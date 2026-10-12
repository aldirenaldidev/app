package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Ranking saham LQ45: Top Gainer / Top Loser / Teraktif.
 * Sepenuhnya otomatis (cache-first): buka layar → cache langsung tampil
 * + "memperbarui…" → pindai di latar → UI diperbarui otomatis.
 * Tidak ada tombol pindai manual.
 */
public class RankingActivity extends Activity {

    /** Ranking berat (±1 menit) → anggap basi setelah 15 menit. */
    private static final long STALE_MS = 15 * 60_000;

    private ListView lv;
    private ProgressBar pb;
    private TextView tvInfo;
    private Button tabGainer, tabLoser, tabActive, tabDiv, btnExportDiv, btnStopDiv, btnRescanDiv;
    private Button chipShariaOnly;
    private MarketDb db;
    private int tab = 0; // 0=gainer 1=loser 2=active 3=dividen
    private boolean shariaOnly = false;
    private boolean scanning = false;
    private boolean divScanning = false;
    private volatile boolean divCancel = false;
    /** true bila user menekan "Pindai ulang penuh": abaikan cache segar. */
    private volatile boolean divForceAll = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_ranking);
        BottomNav.setup(this, 1);
        db = new MarketDb(this);
        AdsHelper.init(this);
        AdsHelper.banner(this, R.id.adBanner);
        lv = findViewById(R.id.lvRank);
        pb = findViewById(R.id.pbScan);
        tvInfo = findViewById(R.id.tvScanInfo);
        tabGainer = findViewById(R.id.tabGainer);
        tabLoser = findViewById(R.id.tabLoser);
        tabActive = findViewById(R.id.tabActive);
        tabDiv = findViewById(R.id.tabDiv);
        btnExportDiv = findViewById(R.id.btnExportDiv);
        btnStopDiv = findViewById(R.id.btnStopDiv);
        btnRescanDiv = findViewById(R.id.btnRescanDiv);
        chipShariaOnly = findViewById(R.id.chipShariaOnly);
        chipShariaOnly.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shariaOnly = !shariaOnly;
                styleShariaChip();
                showTab();
            }
        });

        View.OnClickListener tabClick = new View.OnClickListener() {
            public void onClick(View v) {
                if (v == tabLoser) tab = 1;
                else if (v == tabActive) tab = 2;
                else if (v == tabDiv) tab = 3;
                else tab = 0;
                styleTabs();
                showTab();
                autoLoad();
            }
        };
        tabGainer.setOnClickListener(tabClick);
        tabLoser.setOnClickListener(tabClick);
        tabActive.setOnClickListener(tabClick);
        tabDiv.setOnClickListener(tabClick);

        btnExportDiv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportDividen(); }
        });

        btnStopDiv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                divCancel = true;
                tvInfo.setText("Membatalkan pindai…");
            }
        });

        btnRescanDiv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (divScanning) return;
                divForceAll = true;
                db.sweepSet(DivSweep.KEY_IDX, "0");
                tvInfo.setText("Memulai pindai ulang penuh…");
                startDivScan();
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = lv.getAdapter().getItem(pos);
                String sym = null;
                if (o instanceof MarketDb.RankRow) sym = ((MarketDb.RankRow) o).symbol;
                else if (o instanceof MarketDb.DivRow) sym = ((MarketDb.DivRow) o).symbol;
                if (sym == null) return;
                Intent i = new Intent(RankingActivity.this, DetailActivity.class);
                i.putExtra("symbol", sym);
                startActivity(i);
            }
        });

        styleTabs();
        styleShariaChip();
        autoLoad();
    }

    @Override protected void onResume() {
        super.onResume();
        autoLoad();
    }

    /** Otomatis: cache tampil segera, pindai latar bila basi/kosong. */
    private void autoLoad() {
        if (tab == 3) {
            autoLoadDiv();
            return;
        }
        btnExportDiv.setVisibility(View.GONE);
        btnStopDiv.setVisibility(View.GONE);
        btnRescanDiv.setVisibility(View.GONE);
        showTab();
        long upd = db.rankUpdated();
        if (upd <= 0) {
            tvInfo.setText("Memuat ranking otomatis…");
            startScan();
        } else if (Prefs.isStale(upd, System.currentTimeMillis(), STALE_MS) && !scanning) {
            tvInfo.setText("Data " + Fmt.fmtHour(upd) + " WIB • memperbarui otomatis…");
            startScan();
        } else if (!scanning) {
            tvInfo.setText("Diperbarui " + Fmt.fmtHour(upd) + " WIB • otomatis");
        }
    }

    /** Tab Top Dividen: seed instan dari snapshot bila cache kosong, pindai latar bila basi. */
    private void autoLoadDiv() {
        updateDivButtons();
        // Seed sekali dari dividends.json yang ter-bundle → tampil INSTAN.
        if (db.divAll().isEmpty() && !divScanning) {
            DivSeed.SeedResult sr = DivSeed.seed(this, db);
            if (sr.status == DivSeed.OK) {
                showDivTab();
                String label = "Data per " + sr.asOfDisplay
                        + " • " + sr.count + " emiten berdividen"
                        + " • diperbarui otomatis (30 hari)";
                tvInfo.setText(label);
                return;
            }
            // Seed gagal (file hilang/rusak) → fallback pindai background.
        }
        showDivTab();
        long doneAt = sweepDoneAt();
        long now = System.currentTimeMillis();
        boolean neverDone = doneAt <= 0;
        boolean stale = !neverDone
                && CacheTtl.isStale(doneAt, now, CacheTtl.DIVIDEND);
        if ((neverDone || stale) && !divScanning) {
            tvInfo.setText("Memuat data dividen otomatis…");
            startDivScan();
        } else if (!divScanning) {
            tvInfo.setText("Terakhir dipindai penuh: "
                    + DividendClient.fmtDate(doneAt / 1000)
                    + " • otomatis (30 hari)");
        }
    }

    /** Visibilitas tombol Ekspor / Berhenti / Pindai-ulang di tab dividen. */
    private void updateDivButtons() {
        boolean onDiv = (tab == 3);
        btnExportDiv.setVisibility(onDiv ? View.VISIBLE : View.GONE);
        btnStopDiv.setVisibility(onDiv && divScanning ? View.VISIBLE : View.GONE);
        btnRescanDiv.setVisibility(onDiv && !divScanning ? View.VISIBLE : View.GONE);
    }

    private long sweepDoneAt() {
        try {
            return Long.parseLong(db.sweepGet(DivSweep.KEY_DONE));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override protected void onDestroy() {
        divCancel = true;
        super.onDestroy();
    }

    private void styleTabs() {
        tabGainer.setBackgroundResource(tab == 0 ? R.drawable.seg_active : R.drawable.chip_inactive);
        tabLoser.setBackgroundResource(tab == 1 ? R.drawable.seg_active : R.drawable.chip_inactive);
        tabActive.setBackgroundResource(tab == 2 ? R.drawable.seg_active : R.drawable.chip_inactive);
        tabDiv.setBackgroundResource(tab == 3 ? R.drawable.seg_active : R.drawable.chip_inactive);
        tabGainer.setTextColor(tab == 0 ? 0xFFFFFFFF : 0xFF8B93A7);
        tabLoser.setTextColor(tab == 1 ? 0xFFFFFFFF : 0xFF8B93A7);
        tabActive.setTextColor(tab == 2 ? 0xFFFFFFFF : 0xFF8B93A7);
        tabDiv.setTextColor(tab == 3 ? 0xFFFFFFFF : 0xFF8B93A7);
    }

    /** Gaya chip filter "Syariah saja". */
    private void styleShariaChip() {
        chipShariaOnly.setBackgroundResource(shariaOnly
                ? R.drawable.chip_active : R.drawable.chip_inactive);
        chipShariaOnly.setTextColor(shariaOnly ? 0xFFFFFFFF : 0xFF8B93A7);
    }

    /** Saring daftar ranking bila mode "Syariah saja" aktif. */
    private List<MarketDb.RankRow> filterSharia(List<MarketDb.RankRow> rows) {
        if (!shariaOnly) return rows;
        java.util.Set<String> set = Sharia.load(this);
        List<MarketDb.RankRow> out = new ArrayList<>();
        for (MarketDb.RankRow r : rows) {
            if (Sharia.isSharia(set, r.symbol)) out.add(r);
        }
        return out;
    }

    private void showTab() {
        if (tab == 3) { showDivTab(); return; }
        List<MarketDb.RankRow> rows = filterSharia(db.rankAll());
        if (tab == 0) {
            Collections.sort(rows, new Comparator<MarketDb.RankRow>() {
                public int compare(MarketDb.RankRow a, MarketDb.RankRow b) {
                    return Double.compare(b.chg, a.chg);
                }
            });
        } else if (tab == 1) {
            Collections.sort(rows, new Comparator<MarketDb.RankRow>() {
                public int compare(MarketDb.RankRow a, MarketDb.RankRow b) {
                    return Double.compare(a.chg, b.chg);
                }
            });
        } else {
            Collections.sort(rows, new Comparator<MarketDb.RankRow>() {
                public int compare(MarketDb.RankRow a, MarketDb.RankRow b) {
                    return Long.compare(b.vol, a.vol);
                }
            });
        }
        List<MarketDb.RankRow> top = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < 15; i++) top.add(rows.get(i));
        lv.setAdapter(new RankAdapter(this, top));
    }

    private void startScan() {
        scanning = true;
        pb.setVisibility(View.VISIBLE);
        pb.setMax(Lq45.CODES.length);
        pb.setProgress(0);
        new Thread(new ScanTask()).start();
    }

    // ---------- Top Dividen ----------

    /** Tampilkan cache dividen via DivSweep.topRows (urut yield tertinggi). */
    private void showDivTab() {
        List<DivSweep.YieldRow> all = new ArrayList<DivSweep.YieldRow>();
        java.util.Set<String> shSet = shariaOnly ? Sharia.load(this) : null;
        for (MarketDb.DivRow r : db.divAll()) {
            if (shariaOnly && !Sharia.isSharia(shSet, r.symbol)) continue;
            all.add(new DivSweep.YieldRow(r.symbol, r.name, r.price,
                    r.yieldPct, r.annual, r.payout));
        }
        List<DivSweep.YieldRow> top = DivSweep.topRows(all, 15);
        List<MarketDb.DivRow> rows = new ArrayList<MarketDb.DivRow>();
        for (DivSweep.YieldRow y : top) {
            MarketDb.DivRow r = new MarketDb.DivRow();
            r.symbol = y.symbol; r.name = y.name; r.price = y.price;
            r.yieldPct = y.yieldPct; r.annual = y.annual; r.payout = y.payout;
            rows.add(r);
        }
        lv.setAdapter(new DivAdapter(this, rows));
    }

    private void startDivScan() {
        divScanning = true;
        divCancel = false;
        pb.setVisibility(View.VISIBLE);
        updateDivButtons();
        new Thread(new DivScanTask()).start();
    }

    /**
     * Pindai dividen penuh: SEMUA ticker IDX (~940) dengan:
     * - watermark (lanjut dari indeks terakhir bila terputus),
     * - lewati emiten yang cache-nya masih segar (< 30 hari),
     * - backoff eksponensial saat 429,
     * - hasil inkremental tiap batch ke UI.
     */
    class DivScanTask implements Runnable {
        public void run() {
            List<TickerFilter.Ticker> all = Tickers.load(RankingActivity.this);
            final int total = all.size();
            int startIdx = DivSweep.resumeIndex(db.sweepGet(DivSweep.KEY_IDX), total);
            db.sweepSet(DivSweep.KEY_FAILED, "");
            int ok = 0, skipped = 0, failed = 0;
            StringBuilder failedSyms = new StringBuilder();
            long now0 = System.currentTimeMillis();

            postDivProgress(startIdx, total);

            for (int i = startIdx; i < total && !divCancel; i++) {
                TickerFilter.Ticker t = all.get(i);
                String sym = t.code + ".JK";
                boolean fresh = !divForceAll && !CacheTtl.isStale(
                        db.divGetUpdated(sym), System.currentTimeMillis(),
                        CacheTtl.DIVIDEND);
                if (fresh) {
                    skipped++;
                } else {
                    int res = scanOneWithBackoff(sym, t.name);
                    if (res == 1) ok++;
                    else if (res < 0) {
                        failed++;
                        if (failedSyms.length() > 0) failedSyms.append(",");
                        failedSyms.append(t.code);
                    }
                    // res == 0: tidak berdividen / delisted → dilewati elegan
                }
                db.sweepSet(DivSweep.KEY_IDX, String.valueOf(i + 1));
                final int done = i + 1;
                if (DivSweep.batchDone(done) || done == total) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (tab == 3 && !divCancel) showDivTab();
                        }
                    });
                }
                postDivProgress(done, total);
            }

            db.sweepSet(DivSweep.KEY_FAILED, failedSyms.toString());
            final boolean cancelled = divCancel;
            final long finishedAt = System.currentTimeMillis();
            if (!cancelled) {
                db.sweepSet(DivSweep.KEY_DONE, String.valueOf(finishedAt));
                db.sweepSet(DivSweep.KEY_IDX, "0");
            }
            final int fok = ok, fskip = skipped, ffail = failed;
            final long doneAt = cancelled ? sweepDoneAt() : finishedAt;
            runOnUiThread(new Runnable() {
                public void run() {
                    divScanning = false;
                    divForceAll = false;
                    pb.setVisibility(View.GONE);
                    updateDivButtons();
                    if (tab != 3) return;
                    showDivTab();
                    if (cancelled) {
                        tvInfo.setText("Pindai dibatalkan • hasil sementara tersimpan"
                                + " • lanjut otomatis saat dibuka lagi");
                    } else if (fok == 0 && fskip == 0 && db.divAll().isEmpty()) {
                        tvInfo.setText("Data dividen tak tersedia — periksa koneksi");
                    } else {
                        tvInfo.setText("Terakhir dipindai penuh: "
                                + DividendClient.fmtDate(doneAt / 1000)
                                + " • " + fok + " berdividen"
                                + (ffail > 0 ? " • " + ffail + " gagal, dicoba lagi nanti" : "")
                                + " • otomatis (30 hari)");
                    }
                }
            });
        }

        /**
         * Pindai satu emiten dengan backoff saat rate limit.
         * @return 1 tersimpan, 0 tidak ada data (lewati), -1 gagal setelah backoff.
         */
        private int scanOneWithBackoff(String sym, String name) {
            for (int attempt = 1;
                 attempt <= DivSweep.MAX_ATTEMPTS && !divCancel;
                 attempt++) {
                try {
                    DividendClient.DivData d = DividendClient.fetchMetrics(sym);
                    if (!Double.isNaN(d.dividendYield) && d.dividendYield > 0) {
                        db.divSave(sym, name, d.price,
                                d.dividendYield * 100.0, d.annualRate, d.payoutRatio);
                        return 1;
                    }
                    return 0;
                } catch (YahooClient.RateLimitException rle) {
                    long wait = DivSweep.backoffMs(attempt);
                    if (wait < 0 || !sleepCancellable(wait)) return -1;
                } catch (Exception e) {
                    return 0; // delisted / simbol tak valid → lewati elegan
                }
            }
            return -1;
        }

        /** Tidur yang bisa dibatalkan; false bila dibatalkan. */
        private boolean sleepCancellable(long ms) {
            long end = System.currentTimeMillis() + ms;
            while (System.currentTimeMillis() < end) {
                if (divCancel) return false;
                try {
                    Thread.sleep(400);
                } catch (InterruptedException ie) {
                    return false;
                }
            }
            return !divCancel;
        }

        private void postDivProgress(final int done, final int total) {
            runOnUiThread(new Runnable() {
                public void run() {
                    pb.setMax(total);
                    pb.setProgress(done);
                    if (tab == 3 && divScanning && !divCancel) {
                        tvInfo.setText("Pindai penuh " + total + " emiten • progres "
                                + done + "/" + total + " • hasil sementara di bawah");
                    }
                }
            });
        }
    }

    /** Ekspor Top Dividen ke CSV di Downloads. */
    private void exportDividen() {
        final List<MarketDb.DivRow> rows = db.divAll();
        if (rows.isEmpty()) {
            Toast.makeText(this, "Belum ada data dividen", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Menyiapkan file CSV…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<DivSweep.YieldRow> all = new ArrayList<DivSweep.YieldRow>();
                    for (MarketDb.DivRow r : rows) {
                        all.add(new DivSweep.YieldRow(r.symbol, r.name, r.price,
                                r.yieldPct, r.annual, r.payout));
                    }
                    List<DivSweep.YieldRow> valid =
                            DivSweep.topRows(all, Integer.MAX_VALUE);
                    List<String> lines = new ArrayList<>();
                    for (DivSweep.YieldRow r : valid) {
                        lines.add(ExportCsv.csvRow(
                                r.symbol.replace(".JK", ""),
                                r.name,
                                Double.isNaN(r.price) ? "" : String.valueOf((long) r.price),
                                String.format(Locale.GERMANY, "%.2f", r.yieldPct),
                                Double.isNaN(r.annual) ? "" : String.valueOf((long) r.annual),
                                Double.isNaN(r.payout) ? ""
                                        : String.format(Locale.GERMANY, "%.2f", r.payout * 100)));
                    }
                    final String fn = ExportCsv.write(
                            RankingActivity.this,
                            ExportCsv.fileName("bursaku-top-dividen-", "csv"),
                            ExportCsv.csvRow("kode", "nama", "harga",
                                    "dividend_yield_pct", "dividen_tahunan_rp",
                                    "payout_ratio_pct"),
                            lines);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(RankingActivity.this,
                                    "Tersimpan di Downloads: " + fn,
                                    Toast.LENGTH_LONG).show();
                            Notify.simple(RankingActivity.this, "Ekspor Top Dividen",
                                    "File " + fn + " tersimpan di Downloads.");
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(RankingActivity.this,
                                    "Gagal mengekspor: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    class ScanTask implements Runnable {
        public void run() {
            int ok = 0;
            for (int i = 0; i < Lq45.CODES.length; i++) {
                final int idx = i;
                try {
                    YahooClient.ChartData d = YahooClient.fetchChart(Lq45.CODES[i] + ".JK", "5d");
                    YahooClient.Meta m = d.meta;
                    db.rankSave(Lq45.CODES[i] + ".JK", Lq45.NAMES[i], m.price, m.changePct, m.volume);
                    ok++;
                } catch (Exception e) { /* lewati emiten gagal */ }
                final int done = idx + 1;
                runOnUiThread(new Runnable() {
                    public void run() {
                        pb.setProgress(done);
                        tvInfo.setText("Memperbarui otomatis " + done + "/" + Lq45.CODES.length);
                    }
                });
            }
            final int fok = ok;
            runOnUiThread(new Runnable() {
                public void run() {
                    scanning = false;
                    pb.setVisibility(View.GONE);
                    tvInfo.setText("Diperbarui " + Fmt.fmtHour(System.currentTimeMillis())
                            + " WIB • otomatis");
                    showTab();
                    if (fok == 0) Toast.makeText(RankingActivity.this,
                            "Gagal memperbarui — periksa koneksi", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
