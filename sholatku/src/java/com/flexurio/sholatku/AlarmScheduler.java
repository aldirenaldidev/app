package com.flexurio.sholatku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/**
 * Penjadwal alarm: adzan fardhu, pengingat 15 menit, Dhuha, Tahajud, puasa sunnah.
 */
public class AlarmScheduler {

    public static final String PREFS = "sholatku_alarms";
    public static final String EXTRA_PRAYER = "prayer";

    private static final String[] PRAYERS =
            {"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    public static int codeFor(String prayer) {
        for (int i = 0; i < PRAYERS.length; i++) {
            if (PRAYERS[i].equals(prayer)) return 101 + i;
        }
        if (prayer != null && prayer.startsWith("pre_")) {
            for (int i = 0; i < PRAYERS.length; i++) {
                if (PRAYERS[i].equals(prayer.substring(4))) return 201 + i;
            }
        }
        if ("dhuha".equals(prayer)) return 301;
        if ("tahajud".equals(prayer)) return 302;
        if ("kahf".equals(prayer)) return 303;
        return 100;
    }

    /** Alarm adzan fardhu (kind=adzan). */
    public static void schedule(Context ctx, String prayer, int hour, int minute) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra(EXTRA_PRAYER, prayer);
        i.putExtra("kind", "adzan");
        i.putExtra("hour", hour);
        i.putExtra("minute", minute);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codeFor(prayer), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long when = nextTimeMillis(hour, minute);
        setExactCompat(am, when, pi);
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean("en_" + prayer, true)
                .putString("cfg_" + prayer, hour + ":" + minute).apply();
    }

    /** Pengingat 15 menit sebelum adzan (kind=pre15). */
    public static void setPreAlarm(Context ctx, String prayer, int hour, int minute, boolean enabled) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra(EXTRA_PRAYER, prayer);
        i.putExtra("kind", "pre15");
        i.putExtra("hour", hour);
        i.putExtra("minute", minute);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codeFor("pre_" + prayer), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (enabled && am != null) {
            setExactCompat(am, nextTimeMillis(hour, minute), pi);
        } else if (am != null) {
            am.cancel(pi);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("pre_" + prayer, enabled).apply();
    }

    /** Pengingat sholat sunnah (kind=dhuha / tahajud). */
    public static void setSunnah(Context ctx, String kind, int hour, int minute, boolean enabled) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("kind", kind);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codeFor(kind), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (enabled && am != null) {
            setExactCompat(am, nextTimeMillis(hour, minute), pi);
        } else if (am != null) {
            am.cancel(pi);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("sunnah_" + kind, hour + ":" + minute)
                .putBoolean("en_" + kind, enabled).apply();
    }

    public static boolean isSunnahEnabled(Context ctx, String kind) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("en_" + kind, false);
    }

    public static boolean isPreEnabled(Context ctx, String prayer) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("pre_" + prayer, false);
    }

    /** Pengingat puasa sunnah (H-1 20:00). Delegasi ke FastingScheduler. */
    public static void setPuasaReminder(Context ctx, boolean enabled) {
        if (enabled) {
            FastingScheduler.scheduleNext(ctx);
        } else {
            FastingScheduler.cancel(ctx);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("en_puasa", enabled).apply();
    }

    public static boolean isPuasaEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("en_puasa", false);
    }

    /** Pengingat Al-Kahf tiap Jumat 08:00. */
    public static void setKahfReminder(Context ctx, boolean enabled) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("kind", "kahf");
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codeFor("kahf"), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (enabled && am != null) {
            setExactCompat(am, nextFridayMillis(8, 0), pi);
        } else if (am != null) {
            am.cancel(pi);
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("en_kahf", enabled).apply();
    }

    public static boolean isKahfEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("en_kahf", false);
    }

    public static void cancel(Context ctx, String prayer) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra(EXTRA_PRAYER, prayer);
        i.putExtra("kind", "adzan");
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codeFor(prayer), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("en_" + prayer, false).apply();
    }

    public static boolean isEnabled(Context ctx, String prayer) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("en_" + prayer, false);
    }

    /** Dipanggil dari BootReceiver: jadwalkan ulang semua alarm yang aktif. */
    public static void rescheduleAll(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String prayer : PRAYERS) {
            if (p.getBoolean("en_" + prayer, false)) {
                String cfg = p.getString("cfg_" + prayer, null);
                if (cfg != null) {
                    String[] hm = cfg.split(":");
                    try { schedule(ctx, prayer, Integer.parseInt(hm[0]), Integer.parseInt(hm[1])); }
                    catch (Throwable ignored) { }
                }
            }
            if (p.getBoolean("pre_" + prayer, false)) {
                // pre-alarm disimpan bersama cfg; dijadwal ulang dengan waktu -15 mnt
                String cfg = p.getString("cfg_" + prayer, null);
                if (cfg != null) {
                    String[] hm = cfg.split(":");
                    try {
                        int h = Integer.parseInt(hm[0]), m = Integer.parseInt(hm[1]) - 15;
                        if (m < 0) { m += 60; h = (h + 23) % 24; }
                        setPreAlarm(ctx, prayer, h, m, true);
                    } catch (Throwable ignored) { }
                }
            }
        }
        for (String kind : new String[]{"dhuha", "tahajud"}) {
            if (p.getBoolean("en_" + kind, false)) {
                String cfg = p.getString("sunnah_" + kind, null);
                if (cfg != null) {
                    String[] hm = cfg.split(":");
                    try { setSunnah(ctx, kind, Integer.parseInt(hm[0]), Integer.parseInt(hm[1]), true); }
                    catch (Throwable ignored) { }
                }
            }
        }
        if (p.getBoolean("en_puasa", false)) {
            try { FastingScheduler.scheduleNext(ctx); } catch (Throwable ignored) { }
        }
        if (p.getBoolean("en_kahf", false)) {
            try { setKahfReminder(ctx, true); } catch (Throwable ignored) { }
        }
    }

    /* ---------- util ---------- */

    static long nextFridayMillis(int hour, int minute) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, hour);
        c.set(java.util.Calendar.MINUTE, minute);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        int today = c.get(java.util.Calendar.DAY_OF_WEEK);
        int delta = (java.util.Calendar.FRIDAY - today + 7) % 7;
        if (delta == 0 && c.getTimeInMillis() <= System.currentTimeMillis()) delta = 7;
        c.add(java.util.Calendar.DAY_OF_YEAR, delta);
        return c.getTimeInMillis();
    }

    static long nextTimeMillis(int hour, int minute) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.HOUR_OF_DAY, hour);
        c.set(java.util.Calendar.MINUTE, minute);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }

    static void setExactCompat(AlarmManager am, long when, PendingIntent pi) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } catch (Throwable t) {
                try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi); } catch (Throwable ignored) { }
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }
}
