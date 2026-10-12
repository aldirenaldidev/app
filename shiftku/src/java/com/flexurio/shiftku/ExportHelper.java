package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.core.content.FileProvider;

/**
 * Export jadwal ke CSV (dibuka di Excel) dan PDF.
 * Format rapi & profesional, konsisten dengan data di kalender.
 */
public class ExportHelper {

    /** Satu baris jadwal untuk export. */
    public static class Row {
        public String date;      // yyyy-MM-dd
        public String dayName;   // Senin, ...
        public String person;    // nama anggota (grup) atau "-" (pribadi)
        public String shiftName; // Pagi / Libur / -
        public String start;     // 07:00
        public String end;       // 15:00
        public double hours;
        public boolean isWeekend;
        public boolean isOff;
    }

    // ---------------- KUMPUL DATA ----------------

    /**
     * Bangun daftar baris dari data lokal (pribadi) atau Firestore (grup).
     * Untuk grup: satu baris per (tanggal, anggota).
     */
    public static List<Row> buildRows(Context c, int year, int month, String fbId,
                                      Map<String, Map<String, Object>> fsSchedule,
                                      Map<String, String> uidName,
                                      DbHelper db, String groupCode) {
        List<Row> out = new ArrayList<Row>();
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            if (fbId != null && fsSchedule != null) {
                // Mode grup: tiap tanggal x tiap anggota yang terjadwal.
                for (int d = 1; d <= days; d++) {
                    String ymd = DateUtil.ymd(year, month, d);
                    cal.set(year, month, d);
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    Map<String, Object> dayMap = fsSchedule.get(ymd);
                    if (dayMap == null || dayMap.isEmpty()) {
                        Row r = new Row();
                        r.date = ymd;
                        r.dayName = DateUtil.hariNama(dow);
                        r.person = "-";
                        r.shiftName = "-";
                        r.start = "-"; r.end = "-";
                        r.hours = 0;
                        r.isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
                        r.isOff = true;
                        out.add(r);
                    } else {
                        for (Map.Entry<String, Object> e : dayMap.entrySet()) {
                            Row r = new Row();
                            r.date = ymd;
                            r.dayName = DateUtil.hariNama(dow);
                            String nm = uidName != null ? uidName.get(e.getKey()) : null;
                            r.person = nm != null ? nm : "?";
                            if (e.getValue() instanceof Map) {
                                Map<String, Object> en = (Map<String, Object>) e.getValue();
                                r.shiftName = str(en.get("shiftName"), "-");
                                r.isOff = "Libur".equalsIgnoreCase(r.shiftName)
                                        || "Off".equalsIgnoreCase(r.shiftName);
                            } else {
                                r.shiftName = "-";
                                r.isOff = true;
                            }
                            r.start = "-"; r.end = "-";
                            r.hours = r.isOff ? 0 : 8;
                            r.isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
                            out.add(r);
                        }
                    }
                }
            } else if (db != null) {
                // Mode pribadi: jadwal dari SQLite.
                for (int d = 1; d <= days; d++) {
                    String ymd = DateUtil.ymd(year, month, d);
                    cal.set(year, month, d);
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    Row r = new Row();
                    r.date = ymd;
                    r.dayName = DateUtil.hariNama(dow);
                    r.person = "-";
                    long sid = db.getScheduleShift(ymd);
                    if (sid > 0) {
                        Shift s = db.getShift(sid);
                        if (s != null) {
                            r.shiftName = s.name;
                            r.start = s.dayOff ? "-" : s.start;
                            r.end = s.dayOff ? "-" : s.end;
                            r.hours = s.hours();
                            r.isOff = s.dayOff;
                        } else {
                            r.shiftName = "-"; r.start = "-"; r.end = "-";
                            r.hours = 0; r.isOff = true;
                        }
                    } else {
                        r.shiftName = "-"; r.start = "-"; r.end = "-";
                        r.hours = 0; r.isOff = true;
                    }
                    r.isWeekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY);
                    out.add(r);
                }
            }
            // Urutkan: tanggal, lalu nama.
            Collections.sort(out, new Comparator<Row>() {
                public int compare(Row a, Row b) {
                    int c = a.date.compareTo(b.date);
                    return c != 0 ? c : a.person.compareTo(b.person);
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("ShiftKu", "buildRows: " + t);
        }
        return out;
    }

    private static String str(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }

    /** "Senin, 12 Okt 2026". */
    public static String tanggalID(String ymd) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat out = new SimpleDateFormat("EEEE, d MMM yyyy",
                    new Locale("id", "ID"));
            return out.format(in.parse(ymd));
        } catch (Exception e) {
            return ymd;
        }
    }

    // ---------------- CSV ----------------

    public static void exportScheduleCsv(Activity a, String title, int year, int month,
                                         String fbId,
                                         Map<String, Map<String, Object>> fsSchedule,
                                         Map<String, String> uidName,
                                         DbHelper db, String groupCode) {
        try {
            List<Row> rows = buildRows(a, year, month, fbId, fsSchedule, uidName, db, groupCode);
            if (rows.isEmpty()) {
                Toast.makeText(a, "Tidak ada data.", Toast.LENGTH_SHORT).show();
                return;
            }
            String periode = DateUtil.BULAN[month] + " " + year;
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF"); // BOM agar Excel baca UTF-8 dengan benar
            sb.append("Jadwal Shift — ").append(csv(title)).append("\n");
            sb.append("Periode,").append(csv(periode)).append("\n\n");
            boolean isGroup = fbId != null;
            if (isGroup) {
                sb.append("Tanggal,Hari,Anggota,Shift,Keterangan\n");
            } else {
                sb.append("Tanggal,Hari,Shift,Jam Masuk,Jam Keluar,Durasi (jam),Keterangan\n");
            }
            double totalJam = 0;
            int totalShift = 0;
            for (Row r : rows) {
                String ket = r.isOff ? "Libur" : (r.isWeekend ? "Weekend" : "");
                if (isGroup) {
                    sb.append(csv(tanggalID(r.date))).append(",")
                            .append(csv(r.dayName)).append(",")
                            .append(csv(r.person)).append(",")
                            .append(csv(r.shiftName)).append(",")
                            .append(csv(ket)).append("\n");
                } else {
                    sb.append(csv(tanggalID(r.date))).append(",")
                            .append(csv(r.dayName)).append(",")
                            .append(csv(r.shiftName)).append(",")
                            .append(csv(r.start)).append(",")
                            .append(csv(r.end)).append(",")
                            .append(String.format(Locale.US, "%.1f", r.hours).replace('.', ',')).append(",")
                            .append(csv(ket)).append("\n");
                }
                if (!r.isOff) { totalShift++; totalJam += r.hours; }
            }
            sb.append("\n");
            sb.append("Total shift,").append(totalShift).append("\n");
            if (!isGroup) {
                sb.append("Total jam kerja,")
                        .append(String.format(Locale.US, "%.1f", totalJam).replace('.', ','))
                        .append("\n");
            }
            sb.append("Dicetak,").append(csv(tanggalID(DateUtil.todayYmd()))).append("\n");

            File f = writeDownload(a, "jadwal-" + year + "-" + (month + 1) + ".csv",
                    sb.toString().getBytes("UTF-8"));
            shareFile(a, f, "text/csv");
            Toast.makeText(a, "CSV tersimpan & dibagikan.", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "Gagal export: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private static String csv(String s) {
        if (s == null) s = "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ---------------- PDF ----------------

    public static void exportSchedulePdf(Activity a, String title, int year, int month,
                                         String fbId,
                                         Map<String, Map<String, Object>> fsSchedule,
                                         Map<String, String> uidName,
                                         DbHelper db, String groupCode) {
        try {
            List<Row> rows = buildRows(a, year, month, fbId, fsSchedule, uidName, db, groupCode);
            if (rows.isEmpty()) {
                Toast.makeText(a, "Tidak ada data.", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isGroup = fbId != null;
            double totalJam = 0;
            int totalShift = 0;
            for (Row r : rows) {
                if (!r.isOff) { totalShift++; totalJam += r.hours; }
            }
            String periode = DateUtil.BULAN[month] + " " + year;

            MiniPdf pdf = new MiniPdf();
            String[] cols = isGroup
                    ? new String[]{"Tanggal", "Hari", "Anggota", "Shift", "Ket."}
                    : new String[]{"Tanggal", "Hari", "Shift", "Jam", "Ket."};
            float[] widths = isGroup
                    ? new float[]{118, 62, 120, 110, 105}
                    : new float[]{118, 62, 110, 105, 120};

            int perPage = 30;
            int pages = (rows.size() + perPage - 1) / perPage;
            for (int p = 0; p < pages; p++) {
                int from = p * perPage;
                int to = Math.min(rows.size(), from + perPage);
                pdf.startPage();
                float y = 800;
                // Judul
                pdf.text(40, y, 16, true, 0.15f, 0.2f, 0.45f,
                        "Jadwal Shift — " + title);
                y -= 22;
                pdf.text(40, y, 11, false, 0.4f, 0.4f, 0.4f,
                        "Periode: " + periode + "    •    Halaman " + (p + 1) + "/" + pages);
                y -= 26;
                // Header tabel
                float x = 40;
                float rh = 22;
                for (int i = 0; i < cols.length; i++) {
                    pdf.cell(x, y - rh, widths[i], rh, cols[i], 10, true,
                            0.15f, 0.25f, 0.55f, 1, 1, 1);
                    x += widths[i];
                }
                y -= rh;
                // Baris data
                for (int i = from; i < to; i++) {
                    Row r = rows.get(i);
                    String ket = r.isOff ? "Libur" : (r.isWeekend ? "Weekend" : "-");
                    String jam = r.isOff ? "-" : (r.start + "-" + r.end);
                    String[] vals = isGroup
                            ? new String[]{tanggalID(r.date), r.dayName, r.person,
                            r.shiftName, ket}
                            : new String[]{tanggalID(r.date), r.dayName, r.shiftName,
                            jam, ket};
                    x = 40;
                    boolean hl = r.isWeekend || r.isOff;
                    for (int j = 0; j < vals.length; j++) {
                        pdf.cell(x, y - rh, widths[j], rh, vals[j], 9, false,
                                hl ? 0.96f : 1, hl ? 0.96f : 1, hl ? 0.96f : 1,
                                0.2f, 0.2f, 0.2f);
                        x += widths[j];
                    }
                    y -= rh;
                }
                // Footer di halaman terakhir
                if (p == pages - 1) {
                    y -= 14;
                    pdf.text(40, y, 11, true, 0.2f, 0.2f, 0.2f,
                            "Total shift: " + totalShift
                                    + (isGroup ? "" : "    •    Total jam kerja: "
                                    + String.format(Locale.US, "%.1f", totalJam)));
                    y -= 18;
                    pdf.text(40, y, 9, false, 0.5f, 0.5f, 0.5f,
                            "Dicetak: " + tanggalID(DateUtil.todayYmd())
                                    + " — ShiftKu © aldire__");
                }
                pdf.endPage();
            }

            byte[] data = pdf.build();
            File f = writeDownload(a, "jadwal-" + year + "-" + (month + 1) + ".pdf", data);
            shareFile(a, f, "application/pdf");
            Toast.makeText(a, "PDF tersimpan & dibagikan.", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "Gagal export: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- LAPORAN KEHADIRAN ----------------

    /**
     * Export laporan kehadiran bulanan ke CSV (dibuka di Excel).
     * Format rapi: judul, header jelas, baris ringkasan total, tanda untuk yang bermasalah.
     */
    public static void exportReportCsv(Activity a, String groupName,
                                       List<ReportHelper.Row> rows,
                                       int year, int month) {
        try {
            if (rows == null || rows.isEmpty()) {
                Toast.makeText(a, "Tidak ada data.", Toast.LENGTH_SHORT).show();
                return;
            }
            String periode = DateUtil.BULAN[month] + " " + year;
            StringBuilder sb = new StringBuilder();
            sb.append("\uFEFF"); // BOM agar Excel baca UTF-8 dengan benar
            sb.append("Laporan Kehadiran — ").append(csv(groupName))
                    .append(" — ").append(csv(periode)).append("\n\n");
            sb.append("No,Nama,Hadir,Telat,Tidak Hadir,Izin,Sakit,Cuti,"
                    + "Total Jam Kerja,Lembur (jam)\n");

            int no = 1;
            int tHadir = 0, tTelat = 0, tTdk = 0, tIzin = 0, tSakit = 0, tCuti = 0;
            double tJam = 0, tLembur = 0;
            for (ReportHelper.Row r : rows) {
                String flag = r.tidakHadir > 3 ? " ⚠️" : "";
                sb.append(no).append(",")
                        .append(csv(r.nama + flag)).append(",")
                        .append(r.hadir).append(",")
                        .append(r.telat).append(",")
                        .append(r.tidakHadir).append(",")
                        .append(r.izin).append(",")
                        .append(r.sakit).append(",")
                        .append(r.cuti).append(",")
                        .append(String.format(Locale.US, "%.1f", r.totalJam).replace('.', ',')).append(",")
                        .append(String.format(Locale.US, "%.1f", r.lemburJam).replace('.', ','))
                        .append("\n");
                no++;
                tHadir += r.hadir; tTelat += r.telat; tTdk += r.tidakHadir;
                tIzin += r.izin; tSakit += r.sakit; tCuti += r.cuti;
                tJam += r.totalJam; tLembur += r.lemburJam;
            }
            sb.append("\n");
            sb.append("TOTAL,,")
                    .append(tHadir).append(",")
                    .append(tTelat).append(",")
                    .append(tTdk).append(",")
                    .append(tIzin).append(",")
                    .append(tSakit).append(",")
                    .append(tCuti).append(",")
                    .append(String.format(Locale.US, "%.1f", tJam).replace('.', ',')).append(",")
                    .append(String.format(Locale.US, "%.1f", tLembur).replace('.', ','))
                    .append("\n");
            sb.append("Dicetak,").append(csv(tanggalID(DateUtil.todayYmd()))).append("\n");
            sb.append("Keterangan,⚠️ = Tidak hadir lebih dari 3x\n");

            File f = writeDownload(a, "laporan-kehadiran-" + year + "-" + (month + 1) + ".csv",
                    sb.toString().getBytes("UTF-8"));
            shareFile(a, f, "text/csv");
            Toast.makeText(a, "CSV tersimpan & dibagikan.", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "Gagal export: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Export laporan kehadiran bulanan ke PDF.
     * Kop + tabel ber-border dengan header berwarna + footer + kolom tanda tangan.
     */
    public static void exportReportPdf(Activity a, String groupName,
                                       List<ReportHelper.Row> rows,
                                       int year, int month) {
        try {
            if (rows == null || rows.isEmpty()) {
                Toast.makeText(a, "Tidak ada data.", Toast.LENGTH_SHORT).show();
                return;
            }
            String periode = DateUtil.BULAN[month] + " " + year;

            MiniPdf pdf = new MiniPdf();
            String[] cols = {"No", "Nama", "Hadir", "Telat", "Tdk Hadir",
                    "Izin", "Sakit", "Cuti", "Jam Kerja", "Lembur"};
            float[] widths = {28, 110, 40, 36, 52, 36, 40, 36, 52, 52};

            int perPage = 28;
            int pages = (rows.size() + perPage - 1) / perPage;
            for (int p = 0; p < pages; p++) {
                int from = p * perPage;
                int to = Math.min(rows.size(), from + perPage);
                pdf.startPage();
                float y = 800;

                // Kop: badge ShiftKu + judul
                pdf.text(40, y, 18, true, 0.36f, 0.36f, 0.94f, "ShiftKu");
                y -= 20;
                pdf.text(40, y, 13, true, 0.15f, 0.15f, 0.15f,
                        "Laporan Kehadiran — " + groupName);
                y -= 18;
                pdf.text(40, y, 11, false, 0.4f, 0.4f, 0.4f,
                        "Periode: " + periode + "    •    Halaman " + (p + 1) + "/" + pages);
                y -= 26;

                // Header tabel (biru)
                float x = 40;
                float rh = 22;
                for (int i = 0; i < cols.length; i++) {
                    pdf.cell(x, y - rh, widths[i], rh, cols[i], 8, true,
                            0.15f, 0.25f, 0.55f, 1, 1, 1);
                    x += widths[i];
                }
                y -= rh;

                // Baris data
                int no = from + 1;
                for (int i = from; i < to; i++) {
                    ReportHelper.Row r = rows.get(i);
                    boolean bermasalah = r.tidakHadir > 3;
                    String[] vals = {
                            String.valueOf(no),
                            r.nama + (bermasalah ? " * " : ""),
                            String.valueOf(r.hadir),
                            String.valueOf(r.telat),
                            String.valueOf(r.tidakHadir),
                            String.valueOf(r.izin),
                            String.valueOf(r.sakit),
                            String.valueOf(r.cuti),
                            String.format(Locale.US, "%.1f", r.totalJam),
                            String.format(Locale.US, "%.1f", r.lemburJam)};
                    x = 40;
                    for (int j = 0; j < vals.length; j++) {
                        pdf.cell(x, y - rh, widths[j], rh, vals[j], 8, false,
                                bermasalah ? 1f : 1, bermasalah ? 0.93f : 1,
                                bermasalah ? 0.93f : 1,
                                0.2f, 0.2f, 0.2f);
                        x += widths[j];
                    }
                    y -= rh;
                    no++;
                }

                // Footer di halaman terakhir
                if (p == pages - 1) {
                    y -= 12;
                    int tHadir = 0, tTdk = 0;
                    double tJam = 0;
                    for (ReportHelper.Row r : rows) {
                        tHadir += r.hadir; tTdk += r.tidakHadir; tJam += r.totalJam;
                    }
                    pdf.text(40, y, 10, true, 0.2f, 0.2f, 0.2f,
                            "Total hadir: " + tHadir + "    •    Total tidak hadir: " + tTdk
                                    + "    •    Total jam kerja: "
                                    + String.format(Locale.US, "%.1f", tJam));
                    y -= 16;
                    pdf.text(40, y, 9, false, 0.5f, 0.5f, 0.5f,
                            "* = Tidak hadir lebih dari 3x    •    Dicetak: "
                                    + tanggalID(DateUtil.todayYmd()) + " — ShiftKu © aldire__");
                    // Kolom tanda tangan
                    y -= 34;
                    pdf.text(380, y, 10, false, 0.3f, 0.3f, 0.3f, "Mengetahui,");
                    y -= 16;
                    pdf.text(380, y, 10, false, 0.3f, 0.3f, 0.3f, "Leader");
                    y -= 40;
                    pdf.text(380, y, 10, false, 0.3f, 0.3f, 0.3f,
                            "( ........................................ )");
                }
                pdf.endPage();
            }

            byte[] data = pdf.build();
            File f = writeDownload(a, "laporan-kehadiran-" + year + "-" + (month + 1) + ".pdf", data);
            shareFile(a, f, "application/pdf");
            Toast.makeText(a, "PDF tersimpan & dibagikan.", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "Gagal export: " + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- FILE & SHARE ----------------

    private static File writeDownload(Context c, String name, byte[] data) throws Exception {
        File dir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = c.getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, "ShiftKu-" + name);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(data);
        fos.flush();
        fos.close();
        return f;
    }

    private static void shareFile(Activity a, File f, String mime) {
        try {
            Uri uri = FileProvider.getUriForFile(a,
                    a.getPackageName() + ".fileprovider", f);
            Intent it = new Intent(Intent.ACTION_SEND);
            it.setType(mime);
            it.putExtra(Intent.EXTRA_STREAM, uri);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            a.startActivity(Intent.createChooser(it, "Bagikan jadwal"));
        } catch (Throwable t) {
            Toast.makeText(a, "File tersimpan: " + f.getAbsolutePath(),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- MINI PDF WRITER ----------------
    // Generator PDF 1.4 minimal tanpa dependensi: teks Helvetica,
    // cell tabel dengan border + warna, multi-halaman.

    private static class MiniPdf {
        private final List<byte[]> pages = new ArrayList<byte[]>();
        private ByteArrayOutputStream cur;
        private static final float PAGE_W = 595, PAGE_H = 842;

        void startPage() {
            cur = new ByteArrayOutputStream();
        }

        void endPage() {
            pages.add(cur.toByteArray());
            cur = null;
        }

        /** Teks pada posisi tertentu. */
        void text(float x, float y, int size, boolean bold,
                  float r, float g, float b, String s) {
            String font = bold ? "/F2" : "/F1";
            emit(String.format(Locale.US, "BT %s %d Tf %.2f %.2f %.2f rg %.1f %.1f Td ",
                    font, size, r, g, b, x, y));
            emit("(" + pdfEscape(winAnsi(s)) + ") Tj ET\n");
        }

        /** Cell tabel: persegi + teks di dalamnya. */
        void cell(float x, float y, float w, float h, String s, int size, boolean bold,
                  float fr, float fg, float fb, float tr, float tg, float tb) {
            // fill
            emit(String.format(Locale.US, "%.2f %.2f %.2f rg %.1f %.1f %.1f %.1f re f\n",
                    fr, fg, fb, x, y, w, h));
            // border
            emit(String.format(Locale.US, "%.2f %.2f %.2f RG 0.5 w %.1f %.1f %.1f %.1f re S\n",
                    0.75f, 0.75f, 0.75f, x, y, w, h));
            // teks (potong bila terlalu panjang)
            String t = s == null ? "" : s;
            if (t.length() > 34) t = t.substring(0, 32) + "..";
            text(x + 4, y + 6, size, bold, tr, tg, tb, t);
        }

        private void emit(String s) {
            try {
                cur.write(s.getBytes("ISO-8859-1"));
            } catch (Exception ignored) {}
        }

        /** Bangun dokumen PDF lengkap dengan xref yang valid. */
        byte[] build() throws Exception {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<Integer> offsets = new ArrayList<Integer>();
            // objek: 1=catalog, 2=pages, 3..=page, lalu font, lalu contents
            int nPages = pages.size();
            int nObj = 2 + nPages * 2 + 2; // catalog+pages, (page+content)*n, font F1+F2

            out.write("%PDF-1.4\n".getBytes("ISO-8859-1"));

            // 1: catalog
            offsets.add(out.size());
            out.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                    .getBytes("ISO-8859-1"));
            // 2: pages
            StringBuilder kids = new StringBuilder("[");
            for (int i = 0; i < nPages; i++) kids.append((3 + i * 2)).append(" 0 R ");
            kids.append("]");
            offsets.add(out.size());
            out.write(("2 0 obj\n<< /Type /Pages /Kids " + kids + " /Count "
                    + nPages + " >>\nendobj\n").getBytes("ISO-8859-1"));

            int fontF1 = 3 + nPages * 2;
            int fontF2 = fontF1 + 1;
            // pages + contents
            for (int i = 0; i < nPages; i++) {
                int pageObj = 3 + i * 2;
                int contObj = pageObj + 1;
                offsets.add(out.size());
                out.write((pageObj + " 0 obj\n<< /Type /Page /Parent 2 0 R "
                        + "/MediaBox [0 0 " + (int) PAGE_W + " " + (int) PAGE_H + "] "
                        + "/Resources << /Font << /F1 " + fontF1 + " 0 R /F2 "
                        + fontF2 + " 0 R >> >> "
                        + "/Contents " + contObj + " 0 R >>\nendobj\n")
                        .getBytes("ISO-8859-1"));
                byte[] content = pages.get(i);
                offsets.add(out.size());
                out.write((contObj + " 0 obj\n<< /Length " + content.length
                        + " >>\nstream\n").getBytes("ISO-8859-1"));
                out.write(content);
                out.write("\nendstream\nendobj\n".getBytes("ISO-8859-1"));
            }
            // fonts
            offsets.add(out.size());
            out.write((fontF1 + " 0 obj\n<< /Type /Font /Subtype /Type1 "
                    + "/BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")
                    .getBytes("ISO-8859-1"));
            offsets.add(out.size());
            out.write((fontF2 + " 0 obj\n<< /Type /Font /Subtype /Type1 "
                    + "/BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")
                    .getBytes("ISO-8859-1"));

            int xrefPos = out.size();
            out.write(("xref\n0 " + (nObj + 1) + "\n").getBytes("ISO-8859-1"));
            out.write("0000000000 65535 f \n".getBytes("ISO-8859-1"));
            for (int off : offsets) {
                out.write(String.format(Locale.US, "%010d 00000 n \n", off)
                        .getBytes("ISO-8859-1"));
            }
            out.write(("trailer\n<< /Size " + (nObj + 1) + " /Root 1 0 R >>\n"
                    + "startxref\n" + xrefPos + "\n%%EOF")
                    .getBytes("ISO-8859-1"));
            return out.toByteArray();
        }

        /** Escape karakter khusus string PDF. */
        private static String pdfEscape(String s) {
            return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }

        /** Map ke WinAnsi (Latin-1); karakter tak terpetakan jadi '?'. */
        private static String winAnsi(String s) {
            if (s == null) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                // Karakter Indonesia umum aman di Latin-1; lainnya -> '?'
                if (ch < 256) sb.append(ch);
                else sb.append('?');
            }
            return sb.toString();
        }
    }
}
