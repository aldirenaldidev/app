package com.flexurio.shiftku;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/** Menerima notifikasi FCM (mis. perubahan jadwal grup via Cloud Function). */
public class MyFcmService extends FirebaseMessagingService {
    private static final String TAG = "ShiftKu-FCM";
    private static final String CH = "shiftku_grup";

    @Override public void onNewToken(String token) {
        Log.i(TAG, "FCM token baru diterima.");
    }

    @Override public void onMessageReceived(RemoteMessage msg) {
        String title = "ShiftKu";
        String body = "";
        if (msg.getNotification() != null) {
            if (msg.getNotification().getTitle() != null)
                title = msg.getNotification().getTitle();
            if (msg.getNotification().getBody() != null)
                body = msg.getNotification().getBody();
        }
        if (msg.getData().size() > 0) {
            if (msg.getData().containsKey("title")) title = msg.getData().get("title");
            if (msg.getData().containsKey("body")) body = msg.getData().get("body");
        }
        showNotification(title, body);
    }

    private void showNotification(String title, String body) {
        NotificationManager nm =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CH, "Notifikasi Grup",
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(this, GroupActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }
}
