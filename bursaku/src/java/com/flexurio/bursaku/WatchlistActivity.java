package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Watchlist pribadi. Sepenuhnya otomatis: tampilkan cache segera,
 * refresh di latar bila basi (>5 menit). Tidak ada tombol refresh manual.
 */
public class WatchlistActivity extends Activity {

    private static final long STALE_MS = 5 * 60_000;

    private ListView lv;
    private TextView tvStatus;
    private AutoCompleteTextView etSymbol;
    private MarketDb db;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_watchlist);
        BottomNav.setup(this, 2);
        db = new MarketDb(this);
        AdsHelper.init(this);
        AdsHelper.banner(this, R.id.adBanner);
        lv = findViewById(R.id.lvWatch);
        tvStatus = findViewById(R.id.tvWlStatus);
        etSymbol = findViewById(R.id.etSymbol);
        Tickers.attach(this, etSymbol);

        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addSymbol(); }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                WatchAdapter.WatchRow r = (WatchAdapter.WatchRow) lv.getAdapter().getItem(pos);
                Intent i = new Intent(WatchlistActivity.this, DetailActivity.class);
                i.putExtra("symbol", r.symbol);
                startActivity(i);
            }
        });

        autoLoad();
    }

    @Override protected void onResume() {
        super.onResume();
        autoLoad();
    }

    /** Baris yang sedang tampil (untuk update satu baris tanpa reload). */
    private List<WatchAdapter.WatchRow> currentRows = new ArrayList<WatchAdapter.WatchRow>();

    private void addSymbol() {
        final String code = etSymbol.getText().toString().trim().toUpperCase()
                .replace(".JK", "").replaceAll("\\s+", "");
        if (code.isEmpty()) return;
        final String symbol = code + ".JK";
        if (isWatched(symbol)) {
            Toast.makeText(this, code + " sudah ada di watchlist",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Optimistis: baris langsung tampil dengan status "Memuat harga…"
        db.wlAdd(symbol, Lq45.nameOf(code));
        etSymbol.setText("");
        autoLoad();
        tvStatus.setText("Menambahkan " + code + "…");
        // Fetch prioritas khusus ticker baru (tidak menunggu giliran RefreshTask).
        // Respons yang sama dipakai untuk validasi + harga -> cache terisi SEBELUM tampil.
        new Thread(new Runnable() {
            public void run() {
                final YahooClient.Validation v = YahooClient.validateWithPrice(symbol);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (v.ok) {
                            db.wlPriceSave(symbol, v.price, v.chg);
                            updateRowPrice(symbol, v.price, v.chg);
                            tvStatus.setText(code + " ditambahkan.");
                        } else {
                            db.wlRemove(symbol);
                            autoLoad();
                            Toast.makeText(WatchlistActivity.this,
                                    "Kode tidak valid", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private boolean isWatched(String symbol) {
        for (String[] e : db.wlAll()) {
            if (symbol.equals(e[0])) return true;
        }
        return false;
    }

    /** Perbarui satu baris saja saat harga ticker baru tiba (tanpa reload list). */
    private void updateRowPrice(String symbol, double price, double chg) {
        boolean found = false;
        for (WatchAdapter.WatchRow r : currentRows) {
            if (symbol.equals(r.symbol)) {
                r.price = price;
                r.chg = chg;
                found = true;
                break;
            }
        }
        if (found && lv.getAdapter() instanceof android.widget.BaseAdapter) {
            ((android.widget.BaseAdapter) lv.getAdapter()).notifyDataSetChanged();
        } else {
            autoLoad();
        }
    }

    /** Otomatis: cache dulu, fetch latar bila basi. */
    private void autoLoad() {
        final List<String[]> entries = db.wlAll();
        if (entries.isEmpty()) {
            lv.setAdapter(new WatchAdapter(this, new ArrayList<WatchAdapter.WatchRow>(),
                    new WatchAdapter.OnDelete() {
                        public void onDelete(String s) { }
                    }));
            tvStatus.setText("Watchlist kosong — tambahkan kode emiten di atas.");
            return;
        }
        showRows(buildRowsFromCache(entries), cacheStamp(entries));
        if (Prefs.isStale(watchCacheUpdated(entries), System.currentTimeMillis(), STALE_MS)) {
            tvStatus.setText("Memperbarui harga otomatis…");
            new Thread(new RefreshTask(entries)).start();
        }
    }

    private List<WatchAdapter.WatchRow> buildRowsFromCache(List<String[]> entries) {
        List<WatchAdapter.WatchRow> rows = new ArrayList<>();
        for (String[] e : entries) {
            WatchAdapter.WatchRow r = new WatchAdapter.WatchRow();
            r.symbol = e[0]; r.name = e[1];
            double[] c = db.wlPriceGet(e[0]);
            if (c != null) { r.price = c[0]; r.chg = c[1]; }
            rows.add(r);
        }
        return rows;
    }

    private long watchCacheUpdated(List<String[]> entries) {
        long t = 0;
        for (String[] e : entries) {
            double[] c = db.wlPriceGet(e[0]);
            if (c != null && (long) c[2] > t) t = (long) c[2];
        }
        return t;
    }

    private String cacheStamp(List<String[]> entries) {
        long t = watchCacheUpdated(entries);
        return t > 0 ? "Terakhir diperbarui " + Fmt.fmtHour(t) + " WIB • otomatis" : "";
    }

    private void showRows(List<WatchAdapter.WatchRow> rows, final String status) {
        currentRows = rows;
        lv.setAdapter(new WatchAdapter(WatchlistActivity.this, rows,
                new WatchAdapter.OnDelete() {
                    public void onDelete(String s) {
                        db.wlRemove(s);
                        autoLoad();
                    }
                }));
        tvStatus.setText(status);
    }

    class RefreshTask implements Runnable {
        private final List<String[]> entries;
        RefreshTask(List<String[]> entries) { this.entries = entries; }
        public void run() {
            for (String[] e : entries) {
                try {
                    YahooClient.ChartData d = YahooClient.fetchChart(e[0], "5d");
                    db.wlPriceSave(e[0], d.meta.price, d.meta.changePct);
                } catch (Exception ex) { /* lewati; cache lama tetap tampil */ }
            }
            final List<WatchAdapter.WatchRow> rows = buildRowsFromCache(entries);
            final String stamp = cacheStamp(entries);
            runOnUiThread(new Runnable() {
                public void run() { showRows(rows, stamp); }
            });
        }
    }
}
