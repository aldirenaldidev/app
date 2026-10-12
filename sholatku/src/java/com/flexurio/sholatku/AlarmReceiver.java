package com.flexurio.sholatku;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

/**
 * Menerima alarm: adzan fardhu (putar MP3 adzan via MediaPlayer),
 * pengingat 15 mnt, Dhuha, Tahajud, dan puasa sunnah (notifikasi).
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String CH_ADZAN = "adzan_channel_v2";
    private static final String CH_INFO = "info_channel_v2";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String kind = intent.getStringExtra("kind");
        if (kind == null) kind = "adzan";
        String prayer = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER);
        if (prayer == null) prayer = "Sholat";
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);

        if ("adzan".equals(kind)) {
            playAdzan(ctx, prayer);
            showNotification(ctx, CH_ADZAN, "Waktu Sholat",
                    "Telah masuk waktu " + prayer + ". Selamat menunaikan ibadah sholat.",
                    AlarmScheduler.codeFor(prayer), true);
            if (hour >= 0 && minute >= 0) {
                try { AlarmScheduler.schedule(ctx, prayer, hour, minute); } catch (Throwable ignored) { }
            }
        } else if ("pre15".equals(kind)) {
            showNotification(ctx, CH_INFO, "Persiapan Sholat",
                    "15 menit lagi masuk waktu " + prayer + ". Bersiaplah.",
                    AlarmScheduler.codeFor("pre_" + prayer), false);
            if (hour >= 0 && minute >= 0) {
                try { AlarmScheduler.setPreAlarm(ctx, prayer, hour, minute, true); } catch (Throwable ignored) { }
            }
        } else if ("dhuha".equals(kind)) {
            showNotification(ctx, CH_INFO, "Pengingat Dhuha",
                    "Waktu Dhuha telah tiba. Sholat Dhuha pembuka rezeki — minimal 2 rakaat.",
                    301, false);
            rescheduleSunnah(ctx, "dhuha");
        } else if ("tahajud".equals(kind)) {
            showNotification(ctx, CH_INFO, "Pengingat Tahajud",
                    "Bangunlah untuk Tahajud — sepertiga malam terakhir, waktu mustajab berdoa.",
                    302, false);
            rescheduleSunnah(ctx, "tahajud");
        } else if ("kahf".equals(kind)) {
            showNotification(ctx, CH_INFO, "Jumat Berkah",
                    "Jangan lupa baca Surah Al-Kahf hari ini.",
                    303, false);
            try { AlarmScheduler.setKahfReminder(ctx, true); } catch (Throwable ignored) { }
        } else if ("fasting".equals(kind)) {
            String ftype = intent.getStringExtra("ftype");
            String title = "Pengingat Puasa Sunnah";
            String msg = "Besok dianjurkan berpuasa sunnah. Niatkan dari malam hari.";
            if ("senin".equals(ftype)) { title = "Puasa Senin besok"; msg = "Besok hari Senin — jangan lupa niat puasa sunnah malam ini."; }
            else if ("kamis".equals(ftype)) { title = "Puasa Kamis besok"; msg = "Besok hari Kamis — jangan lupa niat puasa sunnah malam ini."; }
            else if ("bidh".equals(ftype)) { title = "Ayyamul Bidh besok"; msg = "Besok 13 Hijriah — awal Ayyamul Bidh. Niatkan puasa malam ini."; }
            showNotification(ctx, CH_INFO, title, msg, 401, false);
            try { FastingScheduler.scheduleNext(ctx); } catch (Throwable ignored) { }
        }
    }

    private void rescheduleSunnah(Context ctx, String kind) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(AlarmScheduler.PREFS, Context.MODE_PRIVATE);
            String cfg = p.getString("sunnah_" + kind, null);
            if (cfg != null) {
                String[] hm = cfg.split(":");
                AlarmScheduler.setSunnah(ctx, kind, Integer.parseInt(hm[0]), Integer.parseInt(hm[1]), true);
            }
        } catch (Throwable ignored) { }
    }

    /** Putar file adzan terpilih via MediaPlayer pada stream ALARM. */
    private void playAdzan(final Context ctx, String prayer) {
        try {
            SharedPreferences p = ctx.getSharedPreferences("sholatku_prefs", Context.MODE_PRIVATE);
            String soundId = p.getString("adzan_sound", "default");
            // Subuh otomatis pakai versi Malmö bila diaktifkan
            if ("Subuh".equals(prayer) && p.getBoolean("subuh_auto", true)) {
                soundId = "malmo";
            }
            int resId = R.raw.adzan_default;
            if ("azeez".equals(soundId)) resId = R.raw.adzan_azeez;
            else if ("malmo".equals(soundId)) resId = R.raw.adzan_malmo;
            AssetFileDescriptor afd = ctx.getResources().openRawResourceFd(resId);
            if (afd == null) return;
            final MediaPlayer mp = new MediaPlayer();
            if (Build.VERSION.SDK_INT >= 21) {
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
            } else {
                mp.setAudioStreamType(AudioManager.STREAM_ALARM);
            }
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer m) {
                    try { m.release(); } catch (Throwable ignored) { }
                }
            });
            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer m, int what, int extra) {
                    try { m.release(); } catch (Throwable ignored) { }
                    return true;
                }
            });
            mp.prepare();
            mp.start();
            // Pengaman: hentikan setelah 6 menit bila onCompletion tak terpanggil
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() {
                    try { if (mp.isPlaying()) mp.stop(); mp.release(); } catch (Throwable ignored) { }
                }
            }, 360000);
        } catch (Throwable ignored) { }
    }

    private void showNotification(Context ctx, String channelId, String title, String text, int nid, boolean withSound) {
        try {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel ch = new NotificationChannel(channelId, "Pengingat Ibadah",
                        withSound ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("Notifikasi pengingat ibadah SholatKu");
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
            Intent open = new Intent(ctx, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, nid, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = new Notification.Builder(ctx)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentIntent(pi)
                    .setAutoCancel(true);
            if (withSound) b.setDefaults(Notification.DEFAULT_ALL);
            if (Build.VERSION.SDK_INT >= 26) b.setChannelId(channelId);
            nm.notify(nid, b.build());
        } catch (Throwable ignored) { }
    }
}
