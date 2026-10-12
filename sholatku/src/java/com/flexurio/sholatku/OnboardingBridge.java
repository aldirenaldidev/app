package com.flexurio.sholatku;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

/** Jembatan JS -> onboarding: izin notifikasi, volume pengingat, lokasi GPS sekali pakai. */
public class OnboardingBridge {
    private static final String PREFS = "sholatku_prefs";
    private static final String KEY_DONE = "onboarding_done";
    private static final int REQ_NOTIF = 2001;
    private static final int REQ_LOCATION = 2002;

    private final Activity act;
    private final WebView web;
    private boolean locating = false;
    private Ringtone testTone;

    public OnboardingBridge(Activity act, WebView web) {
        this.act = act;
        this.web = web;
    }

    private SharedPreferences prefs() {
        return act.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /* ---------- flag onboarding ---------- */

    @JavascriptInterface
    public boolean isOnboardingDone() {
        try {
            return prefs().getBoolean(KEY_DONE, false);
        } catch (Throwable t) {
            return false;
        }
    }

    @JavascriptInterface
    public void finishOnboarding() {
        try {
            prefs().edit().putBoolean(KEY_DONE, true).apply();
        } catch (Throwable ignored) { }
    }

    /* ---------- langkah 1: izin notifikasi ---------- */

    @JavascriptInterface
    public void requestNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 33
                    && act.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                act.requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            }
        } catch (Throwable ignored) { }
    }

    /* ---------- langkah 2: volume pengingat (STREAM_ALARM) ---------- */

    @JavascriptInterface
    public int getAlarmVolume() {
        try {
            AudioManager am = (AudioManager) act.getSystemService(Context.AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int cur = am.getStreamVolume(AudioManager.STREAM_ALARM);
            return max > 0 ? Math.round(cur * 100f / max) : 50;
        } catch (Throwable t) {
            return 50;
        }
    }

    @JavascriptInterface
    public void setAlarmVolume(final int percent) {
        try {
            AudioManager am = (AudioManager) act.getSystemService(Context.AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            int p = Math.max(0, Math.min(100, percent));
            am.setStreamVolume(AudioManager.STREAM_ALARM, Math.round(max * p / 100f), 0);
        } catch (Throwable ignored) { }
    }

    /** Putar bunyi notifikasi default sistem (tanpa file audio bawaan). */
    @JavascriptInterface
    public void testSound() {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    stopTestSound();
                    Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    testTone = RingtoneManager.getRingtone(act.getApplicationContext(), uri);
                    if (testTone != null) testTone.play();
                } catch (Throwable ignored) { }
            }
        });
    }

    @JavascriptInterface
    public void stopTestSound() {
        try {
            if (testTone != null && testTone.isPlaying()) testTone.stop();
        } catch (Throwable ignored) { }
        testTone = null;
    }

    /* ---------- langkah 3: izin lokasi + GPS sekali pakai ---------- */

    @JavascriptInterface
    public void requestLocationPermission() {
        try {
            if (act.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                    && act.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                act.requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            }
        } catch (Throwable ignored) { }
    }

    @JavascriptInterface
    public boolean hasLocationPermission() {
        try {
            return act.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                    || act.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Ambil koordinat GPS sekali saja; hasil dikirim balik ke JS.
     *  Strategi akurat: kumpulkan fix dari GPS+Network selama ~10 detik,
     *  pilih yang akurasinya terbaik. lastKnownLocation HANYA dipakai bila
     *  masih segar (<3 menit) dan akurat (<500m) — lokasi basi penyebab
     *  "wilayah tidak akurat". */
    @JavascriptInterface
    public void fetchLocationOnce() {
        if (locating) return;
        if (!hasLocationPermission()) {
            jsCallback("onLocationFailed()");
            return;
        }
        final LocationManager lm =
                (LocationManager) act.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            jsCallback("onLocationFailed()");
            return;
        }
        locating = true;
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] done = {false};
        final Location[] best = {null};
        final long startMs = System.currentTimeMillis();

        final Runnable finish = new Runnable() {
            @Override public void run() {
                if (done[0]) return;
                done[0] = true;
                locating = false;
                Location b = best[0];
                if (b == null) b = freshLastKnown(lm);
                if (b != null) {
                    jsCallback("onLocationReceived(" + b.getLatitude()
                            + "," + b.getLongitude() + ")");
                } else {
                    jsCallback("onLocationFailed()");
                }
            }
        };

        final LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                if (done[0] || loc == null) return;
                // Abaikan fix basi (>2 menit) atau tanpa akurasi
                long age = System.currentTimeMillis() - loc.getTime();
                if (age > 120000) return;
                if (!loc.hasAccuracy()) return;
                if (best[0] == null || loc.getAccuracy() < best[0].getAccuracy()) {
                    best[0] = loc;
                }
                // Bila sudah sangat akurat (<30m), langsung selesai
                if (loc.getAccuracy() < 30) {
                    try { lm.removeUpdates(this); } catch (Throwable ignored) { }
                    handler.removeCallbacksAndMessages(null);
                    handler.post(finish);
                }
            }
            @Override public void onStatusChanged(String p, int s, Bundle e) { }
            @Override public void onProviderEnabled(String p) { }
            @Override public void onProviderDisabled(String p) { }
        };
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, listener, Looper.getMainLooper()); } catch (Throwable ignored) { }
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, listener, Looper.getMainLooper()); } catch (Throwable ignored) { }
            }
            // Kumpulkan fix terbaik selama 10 detik, lalu pakai yang terbaik
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    try { lm.removeUpdates(listener); } catch (Throwable ignored) { }
                    handler.post(finish);
                }
            }, 10000);
        } catch (SecurityException se) {
            locating = false;
            jsCallback("onLocationFailed()");
        } catch (Throwable t) {
            locating = false;
            jsCallback("onLocationFailed()");
        }
    }

    /** lastKnownLocation hanya bila segar (<3 mnt) & akurat (<500 m). */
    private Location freshLastKnown(LocationManager lm) {
        try {
            long now = System.currentTimeMillis();
            Location[] cands = new Location[2];
            try { cands[0] = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (Throwable ignored) { }
            try { cands[1] = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (Throwable ignored) { }
            Location best = null;
            for (Location l : cands) {
                if (l == null || !l.hasAccuracy()) continue;
                if (now - l.getTime() > 180000) continue;   // basi
                if (l.getAccuracy() > 500) continue;        // tidak akurat
                if (best == null || l.getAccuracy() < best.getAccuracy()) best = l;
            }
            return best;
        } catch (Throwable t) { return null; }
    }

    private void jsCallback(final String js) {
        try {
            web.post(new Runnable() {
                @Override public void run() {
                    try {
                        web.evaluateJavascript(js, null);
                    } catch (Throwable ignored) { }
                }
            });
        } catch (Throwable ignored) { }
    }
}
