package com.flexurio.bursaku;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferensi pengguna + penanda waktu refresh terakhir.
 * Aplikasi dirancang otomatis penuh: tidak ada tombol refresh manual.
 */
public class Prefs {
    private static final String NAME = "bursaku_prefs";
    public static final String KEY_AUTO = "auto_refresh";
    public static final String TS_MARKET = "ts_market";
    public static final String TS_WATCH = "ts_watch";
    public static final String TS_NEWS = "ts_news";

    /** Default ON: background refresh otomatis aktif. */
    public static boolean autoRefreshOn(Context ctx) {
        return prefs(ctx).getBoolean(KEY_AUTO, true);
    }

    public static void setAutoRefresh(Context ctx, boolean on) {
        prefs(ctx).edit().putBoolean(KEY_AUTO, on).apply();
    }

    public static long lastRefresh(Context ctx, String key) {
        return prefs(ctx).getLong(key, 0);
    }

    public static void markRefreshed(Context ctx, String key) {
        prefs(ctx).edit().putLong(key, System.currentTimeMillis()).apply();
    }

    /**
     * Murni (bisa di-unit-test): true bila cache belum pernah ada
     * atau umurnya sudah >= maxAgeMs.
     */
    public static boolean isStale(long lastMs, long nowMs, long maxAgeMs) {
        if (lastMs <= 0) return true;
        return (nowMs - lastMs) >= maxAgeMs;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }
}
