package com.flexurio.bursaku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Jadwalkan sinkronisasi background tiap 15 menit via AlarmManager
 * (tanpa WorkManager). Receiver sendiri yang memutuskan: hanya jalan
 * bila toggle auto-refresh ON dan jam bursa sedang buka.
 */
public class AlertScheduler {
    public static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pending(ctx);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 60_000, 15 * 60_000, pi);
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pending(ctx));
    }

    private static PendingIntent pending(Context ctx) {
        Intent i = new Intent(ctx, AlertReceiver.class);
        return PendingIntent.getBroadcast(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
