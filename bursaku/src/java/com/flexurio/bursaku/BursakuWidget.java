package com.flexurio.bursaku;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.List;

/**
 * Widget layar utama BursaKu (4x2): IHSG + 3-4 emiten watchlist teratas.
 * Tanpa ContentProvider, tanpa library. Update via siklus AlarmManager
 * (ikut refresh 15 menit jam bursa) + saat widget ditambahkan.
 * RemoteViews terbatas: hanya LinearLayout/TextView.
 */
public class BursakuWidget extends AppWidgetProvider {

    @Override public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        refreshAll(ctx);
    }

    @Override public void onEnabled(Context ctx) {
        refreshAll(ctx);
    }

    /** Perbarui semua instance widget (boleh dipanggil dari receiver). */
    public static void refreshAll(final Context ctx) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    RemoteViews rv = buildViews(ctx);
                    AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
                    int[] ids = awm.getAppWidgetIds(
                            new android.content.ComponentName(ctx, BursakuWidget.class));
                    for (int id : ids) awm.updateAppWidget(id, rv);
                } catch (Exception e) { /* widget opsional; jangan crash */ }
            }
        }).start();
    }

    static RemoteViews buildViews(Context ctx) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_bursaku);
        MarketDb db = new MarketDb(ctx);

        // --- IHSG ---
        MarketDb.MktRow ihsg = db.mktGet("^JKSE");
        if (ihsg != null && !Double.isNaN(ihsg.price)) {
            rv.setTextViewText(R.id.wIhsgVal, Fmt.rp(ihsg.price));
            rv.setTextViewText(R.id.wIhsgChg, Fmt.pct(ihsg.chg));
            boolean up = ihsg.chg >= 0;
            rv.setInt(R.id.wIhsgChg, "setBackgroundResource",
                    up ? R.drawable.pill_up : R.drawable.pill_down);
            rv.setTextColor(R.id.wIhsgChg, up ? 0xFF16C784 : 0xFFEA3943);
        } else {
            rv.setTextViewText(R.id.wIhsgVal, "—");
            rv.setTextViewText(R.id.wIhsgChg, "—");
        }

        // --- Watchlist teratas (maks 4) ---
        int[] rowSyms = {R.id.wSym1, R.id.wSym2, R.id.wSym3, R.id.wSym4};
        int[] rowPrices = {R.id.wPrice1, R.id.wPrice2, R.id.wPrice3, R.id.wPrice4};
        int[] rowChgs = {R.id.wChg1, R.id.wChg2, R.id.wChg3, R.id.wChg4};
        List<String[]> wl = db.wlAll();
        for (int i = 0; i < 4; i++) {
            if (i < wl.size()) {
                String sym = wl.get(i)[0];
                double[] p = db.wlPriceGet(sym);
                rv.setTextViewText(rowSyms[i], sym.replace(".JK", ""));
                if (p != null) {
                    rv.setTextViewText(rowPrices[i], Fmt.rp(p[0]));
                    rv.setTextViewText(rowChgs[i], Fmt.pct(p[1]));
                    boolean up = p[1] >= 0;
                    rv.setTextColor(rowChgs[i], up ? 0xFF16C784 : 0xFFEA3943);
                } else {
                    rv.setTextViewText(rowPrices[i], "—");
                    rv.setTextViewText(rowChgs[i], "—");
                    rv.setTextColor(rowChgs[i], 0xFF8B93A7);
                }
            } else {
                rv.setTextViewText(rowSyms[i], "");
                rv.setTextViewText(rowPrices[i], "");
                rv.setTextViewText(rowChgs[i], "");
            }
        }

        long upd = db.mktUpdated();
        rv.setTextViewText(R.id.wTime, upd > 0
                ? "Diperbarui " + Fmt.fmtHour(upd) + " WIB • BursaKu"
                : "BursaKu");

        // Tap widget → buka aplikasi.
        Intent it = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.wRoot, pi);
        return rv;
    }
}
