package com.flexurio.sholatku;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.JavascriptInterface;

/** Jembatan JS -> alarm adzan native. */
public class AlarmBridge {
    private final Activity act;

    public AlarmBridge(Activity act) {
        this.act = act;
    }

    /** Aktif/nonaktifkan pengingat untuk satu waktu sholat (jam 0-23, menit 0-59). */
    @JavascriptInterface
    public void setAlarm(final String prayer, final int hour, final int minute, final boolean enabled) {
        try {
            if (enabled) {
                AlarmScheduler.schedule(act.getApplicationContext(), prayer, hour, minute);
            } else {
                AlarmScheduler.cancel(act.getApplicationContext(), prayer);
            }
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean isAlarmEnabled(String prayer) {
        try {
            return AlarmScheduler.isEnabled(act.getApplicationContext(), prayer);
        } catch (Throwable t) {
            return false;
        }
    }

    @JavascriptInterface
    public boolean hasNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT < 33) return true;
            return act.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    @JavascriptInterface
    public void requestNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                act.requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 2001);
            }
        } catch (Throwable ignored) { }
    }

    /* ---------- pengingat 15 menit sebelum adzan ---------- */

    @JavascriptInterface
    public void setPreAlarm(final String prayer, final int hour, final int minute, final boolean enabled) {
        try { AlarmScheduler.setPreAlarm(act, prayer, hour, minute, enabled); } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean isPreAlarmEnabled(String prayer) {
        try { return AlarmScheduler.isPreEnabled(act, prayer); } catch (Throwable t) { return false; }
    }

    /* ---------- pengingat sunnah (dhuha / tahajud) ---------- */

    @JavascriptInterface
    public void setSunnah(final String kind, final int hour, final int minute, final boolean enabled) {
        try { AlarmScheduler.setSunnah(act, kind, hour, minute, enabled); } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean isSunnahEnabled(String kind) {
        try { return AlarmScheduler.isSunnahEnabled(act, kind); } catch (Throwable t) { return false; }
    }

    /* ---------- pengingat puasa sunnah ---------- */

    @JavascriptInterface
    public void setPuasaReminder(final boolean enabled) {
        try { AlarmScheduler.setPuasaReminder(act, enabled); } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean isPuasaReminderEnabled() {
        try { return AlarmScheduler.isPuasaEnabled(act); } catch (Throwable t) { return false; }
    }

    @JavascriptInterface
    public void setKahfReminder(final boolean enabled) {
        try { AlarmScheduler.setKahfReminder(act, enabled); } catch (Throwable ignored) { }
    }

    /* ---------- suara adzan ---------- */

    @JavascriptInterface
    public void setAdzanSound(final String soundId) {
        try {
            act.getSharedPreferences("sholatku_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putString("adzan_sound", soundId).apply();
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public String getAdzanSound() {
        try {
            return act.getSharedPreferences("sholatku_prefs", android.content.Context.MODE_PRIVATE)
                    .getString("adzan_sound", "default");
        } catch (Throwable t) { return "default"; }
    }

    @JavascriptInterface
    public void testAdzan(final String soundId) {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    stopAdzanTest();
                    int resId = com.flexurio.sholatku.R.raw.adzan_default;
                    if ("azeez".equals(soundId)) resId = com.flexurio.sholatku.R.raw.adzan_azeez;
                    else if ("malmo".equals(soundId)) resId = com.flexurio.sholatku.R.raw.adzan_malmo;
                    testPlayer = android.media.MediaPlayer.create(act.getApplicationContext(), resId);
                    if (testPlayer != null) testPlayer.start();
                } catch (Throwable ignored) { }
            }
        });
    }

    @JavascriptInterface
    public void stopAdzanTest() {
        try {
            if (testPlayer != null) {
                if (testPlayer.isPlaying()) testPlayer.stop();
                testPlayer.release();
            }
        } catch (Throwable ignored) { }
        testPlayer = null;
    }

    @JavascriptInterface
    public void setSubuhAuto(final boolean on) {
        try {
            act.getSharedPreferences("sholatku_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("subuh_auto", on).apply();
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean getSubuhAuto() {
        try {
            return act.getSharedPreferences("sholatku_prefs", android.content.Context.MODE_PRIVATE)
                    .getBoolean("subuh_auto", true);
        } catch (Throwable t) { return true; }
    }

    private android.media.MediaPlayer testPlayer;
}

