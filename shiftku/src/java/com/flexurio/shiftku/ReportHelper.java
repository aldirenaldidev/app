package com.flexurio.shiftku;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Laporan Kehadiran bulanan.
 *
 * - Lokal: dihitung dari SQLite perangkat (jadwal + absen + izin + cuti + lembur).
 * - Tim (Leader/Admin): diagregasi dari metadata absen Firestore per anggota.
 */
public class ReportHelper {

    /** Toleransi keterlambatan: check-in lewat (jam shift + 15 mnt) = telat. */
    private static final int TELAT_TOLERANSI_MNT = 15;

    public static class Row {
        public String nama = "";
        public int hadir, telat, tidakHadir, izin, sakit, cuti;
        public double totalJam, lemburJam;
    }

    // ================= LAPORAN LOKAL =================

    /** Laporan satu baris untuk pengguna perangkat ini. */
    public static List<Row> buildLocalReport(Context c, int year, int month) {
        DbHelper db = new DbHelper(c);
        Row r = new Row();
        String nm = Prefs.getUserName(c);
        if (nm == null || nm.isEmpty()) nm = FirebaseBridge.currentUserName(c);
        if (nm == null || nm.isEmpty()) nm = "Saya";
        r.nama = nm;

        String ym = String.format(java.util.Locale.US, "%04d-%02d", year, month + 1);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int d = 1; d <= maxDay; d++) {
            String date = String.format(java.util.Locale.US, "%04d-%02d-%02d",
                    year, month + 1, d);
            long sid = db.getScheduleShift(date);
            if (sid <= 0) continue;
            Shift s = db.getShift(sid);
            if (s == null || s.dayOff) continue; // bukan hari kerja

            if (db.hasAbsenToday(date, "Masuk")) {
                r.hadir++;
                // Telat? bandingkan jam check-in dengan jam mulai shift + toleransi.
                String tIn = db.getAbsenTimeToday(date, "Masuk");
                if (isTelat(tIn, s.start)) r.telat++;
                // Durasi: pulang - masuk.
                String tOut = db.getAbsenTimeToday(date, "Pulang");
                r.totalJam += durasiJam(tIn, tOut);
            } else {
                String iz = db.izinTypeOn(date);
                if ("Sakit".equals(iz)) r.sakit++;
                else if ("Izin".equals(iz)) r.izin++;
                else if (db.isLeaveOn(date)) r.cuti++;
                else r.tidakHadir++;
            }
        }
        r.lemburJam = db.lemburJamBulan(ym);

        List<Row> out = new ArrayList<Row>();
        out.add(r);
        return out;
    }

    // ================= LAPORAN TIM (Firestore) =================

    /**
     * Agregasi per anggota dari metadata absen Firestore.
     * @param absenList dari FirebaseBridge.listenAbsen (tiap map: name, date, time, type, lat, lon, loc_name)
     * @param shiftStartByDate peta date -> jam mulai shift "HH:mm" (boleh kosong -> telat dihitung dari 08:15)
     */
    public static List<Row> buildTeamReport(
            List<Map<String, Object>> absenList,
            Map<String, String> shiftStartByDate,
            int year, int month) {
        String ym = String.format(java.util.Locale.US, "%04d-%02d", year, month + 1);
        Map<String, Row> byName = new HashMap<String, Row>();
        // date+name -> sudah dihitung hadir (hindari dobel)
        java.util.Set<String> hadirKey = new java.util.HashSet<String>();

        for (Map<String, Object> m : absenList) {
            String date = str(m.get("date"));
            if (date == null || !date.startsWith(ym)) continue;
            String name = str(m.get("name"));
            if (name.isEmpty()) name = "Tanpa nama";
            String type = str(m.get("type"));
            String time = str(m.get("time"));

            Row r = byName.get(name);
            if (r == null) { r = new Row(); r.nama = name; byName.put(name, r); }

            String key = name + "|" + date;
            if ("Masuk".equals(type)) {
                if (!hadirKey.contains(key)) {
                    hadirKey.add(key);
                    r.hadir++;
                    String start = shiftStartByDate != null
                            ? shiftStartByDate.get(date) : null;
                    if (isTelat(time, start)) r.telat++;
                }
            }
        }
        // Durasi per anggota per tanggal: butuh pasangan Masuk-Pulang.
        Map<String, String> masukTime = new HashMap<String, String>();
        for (Map<String, Object> m : absenList) {
            String date = str(m.get("date"));
            if (date == null || !date.startsWith(ym)) continue;
            String name = str(m.get("name"));
            if (name.isEmpty()) name = "Tanpa nama";
            String type = str(m.get("type"));
            String time = str(m.get("time"));
            String key = name + "|" + date;
            if ("Masuk".equals(type)) {
                if (!masukTime.containsKey(key)) masukTime.put(key, time);
            } else if ("Pulang".equals(type)) {
                String tIn = masukTime.get(key);
                Row r = byName.get(name);
                if (r != null) r.totalJam += durasiJam(tIn == null ? "" : tIn, time);
            }
        }

        List<Row> out = new ArrayList<Row>(byName.values());
        java.util.Collections.sort(out, new java.util.Comparator<Row>() {
            public int compare(Row a, Row b) { return a.nama.compareTo(b.nama); }
        });
        return out;
    }

    // ================= HELPER =================

    /** true bila jam check-in melewati (jamMulai + toleransi). jamMulai null -> patokan 08:15. */
    private static boolean isTelat(String checkIn, String jamMulai) {
        try {
            String[] a = checkIn.split(":");
            int mIn = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
            int batas;
            if (jamMulai != null && !jamMulai.isEmpty()) {
                String[] b = jamMulai.split(":");
                batas = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1])
                        + TELAT_TOLERANSI_MNT;
            } else {
                batas = 8 * 60 + 15;
            }
            return mIn > batas;
        } catch (Exception e) { return false; }
    }

    /** Durasi jam desimal antara dua "HH:mm[:ss]"; 0 bila tidak valid / pulang kosong. */
    private static double durasiJam(String tIn, String tOut) {
        try {
            if (tIn == null || tIn.isEmpty() || tOut == null || tOut.isEmpty()) return 0;
            String[] a = tIn.split(":");
            String[] b = tOut.split(":");
            int s = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
            int e = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
            if (e <= s) return 0;
            return (e - s) / 60.0;
        } catch (Exception ex) { return 0; }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
