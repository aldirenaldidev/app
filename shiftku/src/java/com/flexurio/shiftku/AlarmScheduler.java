package com.flexurio.shiftku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

/** Menjadwalkan semua alarm shift yang aktif. */
public class AlarmScheduler {

    public static void scheduleAll(Context c) {
        Ui.safe("scheduleAll", new Runnable() {
            public void run() { scheduleAllUnsafe(c); }
        });
    }

    private static void scheduleAllUnsafe(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        DbHelper db = new DbHelper(c);
        List<DbHelper.AlarmItem> alarms = db.getAlarms();
        int req = 1000;
        for (DbHelper.AlarmItem a : alarms) {
            if (!a.enabled) continue;
            Intent i = new Intent(c, AlarmReceiver.class);
            String label = (a.shiftName != null ? a.shiftName : "Shift") + " " + a.time;
            i.putExtra(AlarmReceiver.EXTRA_LABEL, label);
            PendingIntent pi = PendingIntent.getBroadcast(c, req++, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long trigger = nextTrigger(a.time);
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

    private static long nextTrigger(String hhmm) {
        try {
            String[] p = hhmm.split(":");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis() + 60000;
        }
    }
}
