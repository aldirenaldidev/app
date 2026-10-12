package com.flexurio.shiftku;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

/** Membangunkan notifikasi saat alarm shift tiba. */
public class AlarmReceiver extends BroadcastReceiver {
    public static final String CH = "shift_alarm";
    public static final String EXTRA_LABEL = "label";

    @Override public void onReceive(Context c, Intent i) {
        String label = i.getStringExtra(EXTRA_LABEL);
        if (label == null) label = "Shift";

        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Alarm Shift",
                    NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Pengingat jadwal shift");
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CH)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle("ShiftKu — Waktunya bersiap")
                .setContentText("Alarm: " + label)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
