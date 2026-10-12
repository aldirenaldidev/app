package com.flexurio.bursaku;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/**
 * Dieksekusi AlarmManager tiap 15 menit (hanya saat jam bursa WIB).
 * Tugas: refresh cache indeks/valas-komoditas + cache watchlist,
 * lalu evaluasi alert harga aktif. Dihormati oleh toggle
 * "Auto-refresh background" di Pengaturan (default ON).
 * Di luar jam bursa tidak ada polling (hemat baterai & rate limit).
 */
public class AlertReceiver extends BroadcastReceiver {

    static final String CH = "bursaku_alert";

    private static final String[] MKT_SYMBOLS = {"^JKSE", "^JKLQ45", "USDIDR=X", "GC=F", "BZ=F"};
    private static final String[] MKT_RANGES = {"1mo", "1mo", "5d", "5d", "5d"};

    @Override public void onReceive(final Context ctx, Intent intent) {
        if (!Prefs.autoRefreshOn(ctx)) return;
        if (!MarketStatus.now().open) return;
        // Kerja di thread latar agar tidak memblokir main thread.
        final PendingResult pr = goAsync();
        new Thread(new Runnable() {
            public void run() {
                try {
                    MarketDb db = new MarketDb(ctx);
                    refreshMarketCache(db);
                    refreshWatchlistCache(db);
                    checkAlerts(ctx, db);
                    BursakuWidget.refreshAll(ctx); // widget ikut siklus 15 menit
                } catch (Exception e) { /* jangan crash receiver */ }
                finally { pr.finish(); }
            }
        }).start();
    }

    /** Refresh cache indeks + valas/komoditas (serial, pacing 800ms via YahooClient). */
    static void refreshMarketCache(MarketDb db) {
        for (int i = 0; i < MKT_SYMBOLS.length; i++) {
            try {
                YahooClient.ChartData d = YahooClient.fetchChart(MKT_SYMBOLS[i], MKT_RANGES[i]);
                YahooClient.Meta m = d.meta;
                StringBuilder csv = new StringBuilder();
                int n = d.candles.size(), start = Math.max(0, n - 60);
                for (int j = start; j < n; j++) {
                    if (csv.length() > 0) csv.append(',');
                    csv.append(d.candles.get(j).close);
                }
                db.mktSave(MKT_SYMBOLS[i], m.price, m.changePct, m.prevClose,
                        m.w52high, m.w52low, csv.toString());
            } catch (Exception e) { /* satu simbol gagal → lanjut */ }
        }
    }

    /** Refresh cache harga watchlist. */
    static void refreshWatchlistCache(MarketDb db) {
        for (String[] e : db.wlAll()) {
            try {
                YahooClient.ChartData d = YahooClient.fetchChart(e[0], "5d");
                db.wlPriceSave(e[0], d.meta.price, d.meta.changePct);
            } catch (Exception ex) { /* lewati */ }
        }
    }

    /** Evaluasi alert harga aktif (logika lama, dipindah ke method). */
    static void checkAlerts(Context ctx, MarketDb db) {
        List<MarketDb.Alert> act = db.alertsActive();
        if (act.isEmpty()) return;

        ensureChannel(ctx);
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        for (MarketDb.Alert a : act) {
            try {
                YahooClient.ChartData d = YahooClient.fetchChart(a.symbol, "5d");
                double price = d.meta.price;
                if (Double.isNaN(price)) continue;
                boolean hit = "above".equals(a.dir) ? price >= a.target : price <= a.target;
                if (hit) {
                    db.alertSetActive(a.id, false);
                    String code = a.symbol.replace(".JK", "");
                    String cond = "above".equals(a.dir) ? "≥ " : "≤ ";
                    Notification n;
                    if (Build.VERSION.SDK_INT >= 26) {
                        n = new Notification.Builder(ctx, CH)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("🔔 Alert Harga " + code)
                                .setContentText(code + " menyentuh " + Fmt.rp(price)
                                        + " (target " + cond + Fmt.rp(a.target) + ")")
                                .setAutoCancel(true)
                                .build();
                    } else {
                        n = new Notification.Builder(ctx)
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("Alert Harga " + code)
                                .setContentText(code + " menyentuh " + Fmt.rp(price))
                                .setAutoCancel(true)
                                .build();
                    }
                    nm.notify((int) a.id, n);
                }
            } catch (Exception e) { /* gagal fetch satu emiten → lewati */ }
        }
    }

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm =
                    (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CH) == null) {
                nm.createNotificationChannel(new NotificationChannel(
                        CH, "Alert Harga", NotificationManager.IMPORTANCE_HIGH));
            }
        }
    }
}
