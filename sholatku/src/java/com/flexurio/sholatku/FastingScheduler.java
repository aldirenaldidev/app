package com.flexurio.sholatku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

/**
 * Penjadwal pengingat puasa sunnah: Senin-Kamis & Ayyamul Bidh (13-15 Hijriah).
 * Pengingat dibunyikan H-1 pukul 20:00 WIB.
 */
public class FastingScheduler {

    private static final String PREFS = "sholatku_fasting";
    private static final int CODE_SENIN = 401, CODE_KAMIS = 402, CODE_BIDH = 403;

    /* ---------- konversi Hijriah (algoritma Kuwaiti) ---------- */

    /** [tahun, bulan(1-12), tanggal] Hijriah untuk tanggal masehi. */
    public static int[] toHijri(int y, int m, int d) {
        int jd = (int) ((1461 * (y + 4800 + (m - 14) / 12)) / 4
                + (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12
                - (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4
                + d - 32075);
        int l = jd - 1948440 + 10632;
        int n = (int) ((l - 1) / 10631);
        l = l - 10631 * n + 354;
        int j = (int) ((10985 - l) / 5316) * ((50 * l) / 17719)
                + ((l / 5670) * (43 * l / 15238));
        l = l - ((30 - j) / 15) * ((17719 * j) / 50)
                - ((j / 16) * ((15238 * j) / 43)) + 29;
        int month = (int) ((24 * l) / 709);
        int day = l - (709 * month) / 24;
        int year = 30 * n + j - 30;
        return new int[]{year, month, day};
    }

    /** [y,m,d] masehi dari tanggal Hijriah (pendekatan via iterasi dari hari ini). */
    public static long gregorianFromHijri(int hy, int hm, int hd) {
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 400; i++) {
            int[] h = toHijri(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
            if (h[0] == hy && h[1] == hm && h[2] == hd) return c.getTimeInMillis();
            if (h[0] > hy || (h[0] == hy && (h[1] > hm || (h[1] == hm && h[2] > hd)))) break;
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return -1;
    }

    /* ---------- penjadwalan ---------- */

    /** Jadwalkan pengingat puasa berikutnya (dipanggil saat enable & tiap alarm bunyi). */
    public static void scheduleNext(Context ctx) {
        cancelAlarms(ctx);
        Calendar now = Calendar.getInstance();

        // 1. Senin / Kamis terdekat
        Calendar target = (Calendar) now.clone();
        target.add(Calendar.DAY_OF_YEAR, 1); // mulai dari besok
        int guard = 0;
        while (guard++ < 14) {
            int dow = target.get(Calendar.DAY_OF_WEEK);
            if (dow == Calendar.MONDAY || dow == Calendar.THURSDAY) break;
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        long reminderMs = reminderTime(target, 20, 0);
        if (reminderMs > System.currentTimeMillis()) {
            String ftype = target.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY ? "senin" : "kamis";
            setFastingAlarm(ctx, ftype.equals("senin") ? CODE_SENIN : CODE_KAMIS,
                    ftype, reminderMs);
            saveNext(ctx, ftype, target.getTimeInMillis());
        }

        // 2. Ayyamul Bidh (13 Hijriah) berikutnya
        int[] h = toHijri(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        int th = h[0], tm = h[1];
        if (h[2] >= 13) { tm++; if (tm > 12) { tm = 1; th++; } }
        long greg13 = gregorianFromHijri(th, tm, 13);
        if (greg13 > 0) {
            Calendar c13 = Calendar.getInstance();
            c13.setTimeInMillis(greg13);
            long rBidh = reminderTime(c13, 20, 0);
            if (rBidh > System.currentTimeMillis()) {
                setFastingAlarm(ctx, CODE_BIDH, "bidh", rBidh);
            }
        }
    }

    /** Waktu pengingat = hari H-1 pukul jam:menit. */
    private static long reminderTime(Calendar targetDay, int hour, int minute) {
        Calendar r = (Calendar) targetDay.clone();
        r.add(Calendar.DAY_OF_YEAR, -1);
        r.set(Calendar.HOUR_OF_DAY, hour);
        r.set(Calendar.MINUTE, minute);
        r.set(Calendar.SECOND, 0);
        r.set(Calendar.MILLISECOND, 0);
        return r.getTimeInMillis();
    }

    private static void setFastingAlarm(Context ctx, int code, String ftype, long when) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("kind", "fasting");
        i.putExtra("ftype", ftype);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmScheduler.setExactCompat(am, when, pi);
    }

    private static void saveNext(Context ctx, String ftype, long dayMs) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("next_type", ftype).putLong("next_day", dayMs).apply();
    }

    public static void cancel(Context ctx) {
        cancelAlarms(ctx);
    }

    private static void cancelAlarms(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            for (int code : new int[]{CODE_SENIN, CODE_KAMIS, CODE_BIDH}) {
                Intent i = new Intent(ctx, AlarmReceiver.class);
                PendingIntent pi = PendingIntent.getBroadcast(ctx, code, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                am.cancel(pi);
            }
        } catch (Throwable ignored) { }
    }

    /** Info untuk kartu puasa di aplikasi: "Senin, 2 hari lagi". */
    public static String nextInfo(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String t = p.getString("next_type", null);
            long d = p.getLong("next_day", 0);
            if (t == null || d <= 0) return null;
            long days = (d - System.currentTimeMillis()) / 86400000L + 1;
            String nm = "senin".equals(t) ? "Senin" : "kamis".equals(t) ? "Kamis" : "Ayyamul Bidh";
            return "Puasa " + nm + " " + Math.max(1, days) + " hari lagi • ketuk untuk dalil & kisah";
        } catch (Throwable t) {
            return null;
        }
    }
}
