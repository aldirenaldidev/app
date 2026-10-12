package com.flexurio.sholatku;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.webkit.JavascriptInterface;

/** Jembatan JS -> aplikasi: bagikan, masukan, versi, simpan jadwal untuk widget. */
public class AppBridge {
    private final Activity act;

    public AppBridge(Activity act) {
        this.act = act;
    }

    @JavascriptInterface
    public void shareText(final String text) {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    act.startActivity(Intent.createChooser(i, "Bagikan jadwal sholat"));
                } catch (Throwable ignored) { }
            }
        });
    }

    @JavascriptInterface
    public void sendFeedback() {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_SENDTO);
                    i.setData(Uri.parse("mailto:aldirenaldi.dev@gmail.com"));
                    i.putExtra(Intent.EXTRA_SUBJECT, "[SholatKu] Masukan pengguna");
                    act.startActivity(i);
                } catch (Throwable ignored) { }
            }
        });
    }

    @JavascriptInterface
    public void openPrivacyPolicy() {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://aldire__.github.io/sholatku/privacy-policy.html"));
                    act.startActivity(i);
                } catch (Throwable ignored) { }
            }
        });
    }

    /** Buka navigasi Google Maps ke koordinat tujuan (fitur Masjid Terdekat). */
    @JavascriptInterface
    public void openMap(final String lat, final String lon, final String label) {
        act.runOnUiThread(new Runnable() {
            @Override public void run() {
                try {
                    String q = lat + "," + lon;
                    String uri = "https://www.google.com/maps/dir/?api=1&destination=" + q;
                    if (label != null && !label.isEmpty()) {
                        uri += "&destination_place_id=" + Uri.encode(label);
                    }
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    act.startActivity(i);
                } catch (Throwable ignored) { }
            }
        });
    }

    @JavascriptInterface
    public String getVersionName() {
        try {
            PackageInfo pi = act.getPackageManager().getPackageInfo(act.getPackageName(), 0);
            return pi.versionName != null ? pi.versionName : "1.0";
        } catch (Throwable t) {
            return "2.0";
        }
    }

    /** Simpan jadwal hari ini agar widget home dapat menampilkannya. */
    @JavascriptInterface
    public void saveTimings(final String city, final String json, final String hijri) {
        try {
            SharedPreferences p = act.getSharedPreferences("sholatku_widget", Context.MODE_PRIVATE);
            p.edit().putString("city", city).putString("timings", json)
                    .putString("hijri", hijri).putLong("saved_at", System.currentTimeMillis()).apply();
            // Minta widget me-refresh
            Intent i = new Intent(act, WidgetProvider.class);
            i.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = AppWidgetManager.getInstance(act)
                    .getAppWidgetIds(new ComponentName(act, WidgetProvider.class));
            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            act.sendBroadcast(i);
        } catch (Throwable ignored) { }
    }
}
