package com.flexurio.shiftku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/**
 * Pengingat Shift — notifikasi otomatis X menit sebelum shift mulai.
 *
 * Dijadwalkan untuk shift hari ini (bila belum lewat) dan besok,
 * berdasarkan tabel jadwal lokal. Dipanggil ulang setiap buka aplikasi
 * dan setelah boot (via BootReceiver -> panggil schedule() di sana bila perlu).
 */
public class ShiftReminder {

    private static final int REQ_BASE = 5000;

    public static void schedule(final Context c) {
        Ui.safe("shiftReminder", new Runnable() {
            public void run() { scheduleUnsafe(c); }
        });
    }

    private static void scheduleUnsafe(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (!Prefs.isShiftReminderOn(c)) {
            cancelAll(c, am);
            return;
        }
        int minutesBefore = Prefs.getShiftReminderMinutes(c);
        DbHelper db = new DbHelper(c);

        // Jadwalkan untuk hari ini dan besok.
        for (int d = 0; d < 2; d++) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, d);
            String ymd = DateUtil.ymd(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            long sid = db.getScheduleShift(ymd);
            if (sid <= 0) continue;
            Shift s = db.getShift(sid);
            if (s == null || s.dayOff || s.start == null || s.start.isEmpty()) continue;

            long trigger = shiftStartMillis(ymd, s.start) - minutesBefore * 60000L;
            if (trigger <= System.currentTimeMillis()) continue; // sudah lewat

            String label = "Pengingat: Shift " + s.name + " mulai " + s.start
                    + " (" + minutesBefore + " mnt lagi)";
            Intent i = new Intent(c, AlarmReceiver.class);
            i.putExtra(AlarmReceiver.EXTRA_LABEL, label);
            PendingIntent pi = PendingIntent.getBroadcast(c, REQ_BASE + d, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            try {
                if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                } else {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                }
            } catch (SecurityException se) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            }
        }
    }

    private static void cancelAll(Context c, AlarmManager am) {
        for (int d = 0; d < 2; d++) {
            Intent i = new Intent(c, AlarmReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(c, REQ_BASE + d, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        }
    }

    private static long shiftStartMillis(String ymd, String hhmm) {
        try {
            String[] dp = ymd.split("-");
            String[] tp = hhmm.split(":");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(dp[0]), Integer.parseInt(dp[1]) - 1,
                    Integer.parseInt(dp[2]),
                    Integer.parseInt(tp[0]), Integer.parseInt(tp[1]), 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis() + 60000;
        }
    }
}
