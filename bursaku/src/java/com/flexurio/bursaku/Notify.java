package com.flexurio.bursaku;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/** Notifikasi ringan tanpa library (untuk konfirmasi ekspor dsb). */
public class Notify {

    private static final String CH = "bursaku_info";

    public static void simple(Context ctx, String title, String text) {
        try {
            NotificationManager nm = (NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(
                        CH, "Info BursaKu", NotificationManager.IMPORTANCE_DEFAULT);
                nm.createNotificationChannel(ch);
            }
            Notification.Builder b = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(ctx, CH)
                    : new Notification.Builder(ctx);
            b.setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setAutoCancel(true);
            nm.notify((int) (System.currentTimeMillis() % 100000), b.build());
        } catch (Exception e) { /* notifikasi opsional; jangan crash */ }
    }
}
