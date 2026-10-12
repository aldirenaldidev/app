package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

/**
 * Detail emiten: grafik + pemilih indikator (chips), info fundamental,
 * komposisi pemegang saham (agregat), dan data keuangan.
 */
public class DetailActivity extends Activity {

    private WebView wv;
    private TextView tvSym, tvPrice, tvChange, tvOhlc, tvFund, tvInfo,
            tvHolders, tvKeuLr, tvKeuNeraca, tvKeuKas, tvKeuVal, tvDividen, tvStatus,
            tvShariaStatus;
    private LinearLayout chipOverlayRow, chipOscRow;
    private String symbol;
    private String chartJson;
    private boolean pageReady = false;
    private MarketDb db;
    private double currentPrice = Double.NaN;
    private final ChartJson.Sel sel = new ChartJson.Sel();
    /** Seleksi kosong untuk intraday (candlestick + volume saja, ringan). */
    private final ChartJson.Sel intraSel = new ChartJson.Sel();
    private List<Candle> candles;
    private List<Candle> intraCandles;
    private boolean intraday = false;
    private Button btnTfDaily, btnTfIntra;

    /** Auto-refresh quote ringan tiap 60 detik selama layar terbuka. */
    private final Handler quoteHandler = new Handler();
    private final Runnable quoteTick = new Runnable() {
        public void run() {
            refreshQuote();
            quoteHandler.postDelayed(this, 60_000);
        }
    };

    // id chips overlay & osilator
    private static final String[] OV_IDS = {"ma", "ema20", "bb", "ichimoku", "sar"};
    private static final String[] OV_LABELS = {"MA", "EMA 20", "Bollinger", "Ichimoku", "SAR"};
    private static final String[] OSC_IDS = {"rsi", "macd", "stoch", "atr", "cci", "willr", "obv", "adx"};
    private static final String[] OSC_LABELS = {"RSI", "MACD", "Stochastic", "ATR", "CCI", "W%R", "OBV", "ADX"};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_detail);
        symbol = getIntent().getStringExtra("symbol");
        AdsHelper.init(this);
        AdsHelper.maybeShowInterstitial(this);
        tvSym = findViewById(R.id.tvDtlSymbol);
        tvPrice = findViewById(R.id.tvDtlPrice);
        tvChange = findViewById(R.id.tvDtlChange);
        tvOhlc = findViewById(R.id.tvDtlOhlc);
        db = new MarketDb(this);
        tvFund = findViewById(R.id.tvDtlFund);
        tvInfo = findViewById(R.id.tvInfo);
        tvHolders = findViewById(R.id.tvHolders);
        tvKeuLr = findViewById(R.id.tvKeuLr);
        tvKeuNeraca = findViewById(R.id.tvKeuNeraca);
        tvKeuKas = findViewById(R.id.tvKeuKas);
        tvKeuVal = findViewById(R.id.tvKeuVal);
        tvDividen = findViewById(R.id.tvDividen);
        tvStatus = findViewById(R.id.tvDtlStatus);
        tvShariaStatus = findViewById(R.id.tvShariaStatus);
        bindShariaStatus();
        chipOverlayRow = findViewById(R.id.chipOverlayRow);
        chipOscRow = findViewById(R.id.chipOscRow);
        tvSym.setText(symbol == null ? "—" : symbol.replace(".JK", ""));

        // Logo emiten di header (atau badge inisial bila tidak ada)
        ImageView ivLogo = findViewById(R.id.ivDtlLogo);
        TextView tvLogoInit = findViewById(R.id.tvDtlLogoInit);
        int logoRes = LogoUtil.resId(symbol);
        if (logoRes != 0) {
            ivLogo.setImageResource(logoRes);
            ivLogo.setVisibility(View.VISIBLE);
            tvLogoInit.setVisibility(View.GONE);
        } else {
            ivLogo.setVisibility(View.GONE);
            tvLogoInit.setVisibility(View.VISIBLE);
            tvLogoInit.setText(LogoUtil.initials(symbol));
        }

        // Seleksi intraday: semua indikator mati.
        intraSel.ma = false; intraSel.ema20 = false; intraSel.bb = false;
        intraSel.ichimoku = false; intraSel.sar = false;
        intraSel.rsi = false; intraSel.macd = false; intraSel.stoch = false;
        intraSel.atr = false; intraSel.cci = false; intraSel.willr = false;
        intraSel.obv = false; intraSel.adx = false;

        btnTfDaily = findViewById(R.id.btnTfDaily);
        btnTfIntra = findViewById(R.id.btnTfIntra);
        btnTfDaily.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setTimeframe(false); }
        });
        btnTfIntra.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setTimeframe(true); }
        });

        buildChips();
        wv = findViewById(R.id.wvChart);
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        wv.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                pageReady = true;
                pushData();
            }
        });
        wv.loadUrl("file:///android_asset/chart.html");
        tvStatus.setText("Memuat data " + symbol + "…");
        new Thread(new LoadTask()).start();

        findViewById(R.id.btnSetAlert).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(DetailActivity.this, AlertActivity.class);
                i.putExtra("symbol", symbol);
                startActivity(i);
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        // Mulai auto-refresh quote tiap 60 detik (berhenti saat layar ditutup).
        quoteHandler.removeCallbacks(quoteTick);
        quoteHandler.postDelayed(quoteTick, 60_000);
    }

    @Override protected void onPause() {
        super.onPause();
        quoteHandler.removeCallbacks(quoteTick);
    }

    /**
     * Refresh ringan: hanya quote (harga, % perubahan, OHLC) — bukan chart penuh.
     * Gagal → abaikan diam-diam, data lama tetap tampil.
     */
    private void refreshQuote() {
        if (symbol == null) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    YahooClient.ChartData d = YahooClient.fetchChart(symbol, "5d");
                    final YahooClient.Meta m = d.meta;
                    if (Double.isNaN(m.price)) return;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvPrice.setText(Fmt.rp(m.price));
                            MainActivity.stylePill(tvChange, Fmt.pct(m.changePct),
                                    m.changePct >= 0);
                            tvStatus.setText("Harga diperbarui otomatis • "
                                    + Fmt.fmtHour(System.currentTimeMillis()) + " WIB");
                        }
                    });
                } catch (Exception e) { /* abaikan; coba lagi 60 detik berikutnya */ }
            }
        }).start();
    }

    /** Status syariah di header: jelas + catatan periode DES. */
    private void bindShariaStatus() {
        if (tvShariaStatus == null) return;
        boolean sy = Sharia.isSharia(this, symbol);
        if (sy) {
            tvShariaStatus.setText("✓ Saham Syariah • "
                    + Sharia.periodLabel(this));
            tvShariaStatus.setTextColor(0xFF16C784);
        } else {
            tvShariaStatus.setText("Non-Syariah • "
                    + Sharia.periodLabel(this));
            tvShariaStatus.setTextColor(0xFF8B93A7);
        }
    }

    // ---------- chips ----------
    private void buildChips() {
        for (int i = 0; i < OV_IDS.length; i++) {
            final String id = OV_IDS[i];
            Button c = makeChip(OV_LABELS[i], isOvOn(id));
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    toggleOv(id);
                    ((Button) v).setBackgroundResource(isOvOn(id)
                            ? R.drawable.chip_active : R.drawable.chip_inactive);
                    ((Button) v).setTextColor(isOvOn(id) ? 0xFFFFFFFF : 0xFF8B93A7);
                    rebuildChart();
                }
            });
            chipOverlayRow.addView(c);
        }
        for (int i = 0; i < OSC_IDS.length; i++) {
            final String id = OSC_IDS[i];
            Button c = makeChip(OSC_LABELS[i], isOscOn(id));
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!isOscOn(id) && countOsc() >= 2) {
                        Toast.makeText(DetailActivity.this,
                                "Maksimal 2 osilator aktif", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    toggleOsc(id);
                    ((Button) v).setBackgroundResource(isOscOn(id)
                            ? R.drawable.chip_active : R.drawable.chip_inactive);
                    ((Button) v).setTextColor(isOscOn(id) ? 0xFFFFFFFF : 0xFF8B93A7);
                    rebuildChart();
                }
            });
            chipOscRow.addView(c);
        }
    }

    private Button makeChip(String label, boolean on) {
        float d = getResources().getDisplayMetrics().density;
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setTextColor(on ? 0xFFFFFFFF : 0xFF8B93A7);
        b.setBackgroundResource(on ? R.drawable.chip_active : R.drawable.chip_inactive);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, (int) (12 * d + 0.5f), 0);
        b.setLayoutParams(lp);
        b.setMinHeight((int) (44 * d + 0.5f));
        b.setPadding((int) (18 * d + 0.5f), (int) (10 * d + 0.5f),
                (int) (18 * d + 0.5f), (int) (10 * d + 0.5f));
        return b;
    }

    private boolean isOvOn(String id) {
        if ("ma".equals(id)) return sel.ma;
        if ("ema20".equals(id)) return sel.ema20;
        if ("bb".equals(id)) return sel.bb;
        if ("ichimoku".equals(id)) return sel.ichimoku;
        return sel.sar;
    }

    private void toggleOv(String id) {
        if ("ma".equals(id)) sel.ma = !sel.ma;
        else if ("ema20".equals(id)) sel.ema20 = !sel.ema20;
        else if ("bb".equals(id)) sel.bb = !sel.bb;
        else if ("ichimoku".equals(id)) sel.ichimoku = !sel.ichimoku;
        else sel.sar = !sel.sar;
    }

    private boolean isOscOn(String id) {
        if ("rsi".equals(id)) return sel.rsi;
        if ("macd".equals(id)) return sel.macd;
        if ("stoch".equals(id)) return sel.stoch;
        if ("atr".equals(id)) return sel.atr;
        if ("cci".equals(id)) return sel.cci;
        if ("willr".equals(id)) return sel.willr;
        if ("obv".equals(id)) return sel.obv;
        return sel.adx;
    }

    private void toggleOsc(String id) {
        if ("rsi".equals(id)) sel.rsi = !sel.rsi;
        else if ("macd".equals(id)) sel.macd = !sel.macd;
        else if ("stoch".equals(id)) sel.stoch = !sel.stoch;
        else if ("atr".equals(id)) sel.atr = !sel.atr;
        else if ("cci".equals(id)) sel.cci = !sel.cci;
        else if ("willr".equals(id)) sel.willr = !sel.willr;
        else if ("obv".equals(id)) sel.obv = !sel.obv;
        else sel.adx = !sel.adx;
    }

    private int countOsc() {
        int n = 0;
        for (String id : OSC_IDS) if (isOscOn(id)) n++;
        return n;
    }

    private void rebuildChart() {
        if (intraday) {
            if (intraCandles == null) return;
            try {
                chartJson = ChartJson.build(intraCandles, intraSel);
                pushData();
            } catch (Exception e) { /* abaikan */ }
            return;
        }
        if (candles == null) return;
        try {
            chartJson = ChartJson.build(candles, sel);
            pushData();
        } catch (Exception e) { /* abaikan */ }
    }

    /** Ganti timeframe Harian / Intraday (5 menit, sesi berjalan). */
    private void setTimeframe(boolean intra) {
        if (intraday == intra && (intra ? intraCandles != null : candles != null)) {
            styleTimeframe();
            return;
        }
        intraday = intra;
        styleTimeframe();
        // Sembunyikan pemilih indikator pada mode intraday (ringan: candle + volume).
        int vis = intra ? View.GONE : View.VISIBLE;
        findViewById(R.id.lblOverlay).setVisibility(vis);
        findViewById(R.id.svOverlay).setVisibility(vis);
        findViewById(R.id.lblOsc).setVisibility(vis);
        findViewById(R.id.svOsc).setVisibility(vis);
        if (intra) {
            tvStatus.setText("Memuat data intraday " + symbol + "…");
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final YahooClient.ChartData d =
                                YahooClient.fetchIntraday(symbol);
                        if (d.candles.size() < 2) throw new Exception("kecil");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                intraCandles = d.candles;
                                chartJson = ChartJson.build(intraCandles, intraSel);
                                tvPrice.setText(Fmt.rp(d.meta.price));
                                MainActivity.stylePill(tvChange, Fmt.pct(d.meta.changePct),
                                        d.meta.changePct >= 0);
                                tvStatus.setText("Intraday 5 menit • sesi terakhir bila bursa tutup");
                                pushData();
                            }
                        });
                    } catch (final Exception e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvStatus.setText("Intraday tak tersedia — kembali ke Harian");
                                setTimeframe(false);
                            }
                        });
                    }
                }
            }).start();
        } else {
            // Kembali ke harian: pakai cache/chart yang sudah ada.
            if (candles != null) {
                chartJson = ChartJson.build(candles, sel);
                tvStatus.setText("");
                pushData();
            }
        }
    }

    private void styleTimeframe() {
        btnTfDaily.setBackgroundResource(
                !intraday ? R.drawable.seg_active : R.drawable.chip_inactive);
        btnTfDaily.setTextColor(!intraday ? 0xFFFFFFFF : 0xFF8B93A7);
        btnTfIntra.setBackgroundResource(
                intraday ? R.drawable.seg_active : R.drawable.chip_inactive);
        btnTfIntra.setTextColor(intraday ? 0xFFFFFFFF : 0xFF8B93A7);
    }

    void pushData() {
        if (pageReady && chartJson != null) {
            final String js = "render('" + chartJson + "')";
            runOnUiThread(new Runnable() {
                public void run() { wv.evaluateJavascript(js, null); }
            });
        }
    }

    // ---------- load bertingkat (cache-first + refresh kategori basi) ----------
    class LoadTask implements Runnable {
        public void run() {
            final long now = System.currentTimeMillis();
            // A. Tampilkan SEMUA cache langsung — offline pun bisa dipakai penuh.
            final Snap snap = readCaches(now);
            runOnUiThread(new Runnable() {
                public void run() { bindSnap(snap, now); }
            });
            // B. Refresh HANYA kategori yang basi (hemat kuota & baterai).
            boolean anyOk = snap.hasAny;
            boolean anyFail = false;
            try {
                if (db.cacheStale(symbol, CacheTtl.CHART)) {
                    YahooClient.ChartData d = YahooClient.fetchChart(symbol, "2y");
                    if (d.candles.size() >= 30) {
                        db.textSet(symbol, CacheTtl.CHART, CacheSer.chartToJson(d));
                        final YahooClient.ChartData fd = d;
                        final long t = System.currentTimeMillis();
                        runOnUiThread(new Runnable() {
                            public void run() { bindChart(fd, t); }
                        });
                        anyOk = true;
                    }
                }
                if (db.cacheStale(symbol, CacheTtl.HOLDERS)) {
                    final String t = fetchHolders();
                    final long tm = System.currentTimeMillis();
                    if (t != null) {
                        db.textSet(symbol, CacheTtl.HOLDERS, t);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvHolders.setText(withAge(t, CacheTtl.HOLDERS, tm));
                            }
                        });
                        anyOk = true;
                    } else anyFail = true;
                }
                if (db.cacheStale(symbol, CacheTtl.DIVIDEND)) {
                    final String t = fetchDividen();
                    final long tm = System.currentTimeMillis();
                    if (t != null) {
                        db.textSet(symbol, CacheTtl.DIVIDEND, t);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvDividen.setText(withAge(t, CacheTtl.DIVIDEND, tm));
                            }
                        });
                        anyOk = true;
                    } else anyFail = true;
                }
                if (db.cacheStale(symbol, CacheTtl.FINANCIAL)) {
                    try {
                        FinancialsClient.FinData f = FinancialsClient.fetch(symbol);
                        db.textSet(symbol, CacheTtl.FINANCIAL, CacheSer.finToJson(f));
                        final FinancialsClient.FinData ff = f;
                        final long tm = System.currentTimeMillis();
                        runOnUiThread(new Runnable() {
                            public void run() { bindFinancial(ff, tm); }
                        });
                        anyOk = true;
                    } catch (Exception e) { anyFail = true; }
                }
                if (db.cacheStale(symbol, CacheTtl.INFO)) {
                    final String t = fetchInfo();
                    final long tm = System.currentTimeMillis();
                    if (t != null) {
                        db.textSet(symbol, CacheTtl.INFO, t);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvInfo.setText(withAge(t, CacheTtl.INFO, tm));
                            }
                        });
                        anyOk = true;
                    } else anyFail = true;
                }
            } catch (Exception e) { anyFail = true; }
            final boolean ok = anyOk, fail = anyFail;
            runOnUiThread(new Runnable() {
                public void run() {
                    if (!ok && fail) {
                        tvStatus.setText("Butuh koneksi internet untuk memuat pertama kali");
                    } else if (fail) {
                        tvStatus.setText("Mode offline — menampilkan data tersimpan");
                    } else {
                        tvStatus.setText("");
                    }
                }
            });
        }
    }

    /** Snapshot cache untuk satu simbol. */
    static class Snap {
        YahooClient.ChartData chart = new YahooClient.ChartData();
        String holders, dividen, finJson, info;
        long tChart, tHolders, tDiv, tFin, tInfo;
        boolean hasAny = false;
    }

    private Snap readCaches(long now) {
        Snap s = new Snap();
        s.chart = CacheSer.chartFromJson(db.textGet(symbol, CacheTtl.CHART));
        s.tChart = db.cacheGet(symbol, CacheTtl.CHART);
        s.holders = db.textGet(symbol, CacheTtl.HOLDERS);
        s.tHolders = db.cacheGet(symbol, CacheTtl.HOLDERS);
        s.dividen = db.textGet(symbol, CacheTtl.DIVIDEND);
        s.tDiv = db.cacheGet(symbol, CacheTtl.DIVIDEND);
        s.finJson = db.textGet(symbol, CacheTtl.FINANCIAL);
        s.tFin = db.cacheGet(symbol, CacheTtl.FINANCIAL);
        s.info = db.textGet(symbol, CacheTtl.INFO);
        s.tInfo = db.cacheGet(symbol, CacheTtl.INFO);
        s.hasAny = s.chart.candles.size() >= 30 || s.holders != null
                || s.dividen != null || s.finJson != null || s.info != null;
        return s;
    }

    /** Tampilkan seluruh cache segera. */
    private void bindSnap(Snap s, long now) {
        if (s.chart.candles.size() >= 30) bindChart(s.chart, s.tChart);
        else {
            tvSym.setText(symbol == null ? "—" : symbol.replace(".JK", ""));
            tvStatus.setText("Memuat data " + symbol + "…");
        }
        tvHolders.setText(withAge(s.holders, CacheTtl.HOLDERS, s.tHolders, now,
                "Pemegang Saham: — (belum ada data)"));
        tvDividen.setText(withAge(s.dividen, CacheTtl.DIVIDEND, s.tDiv, now,
                "Dividen: — (belum ada data)"));
        tvInfo.setText(withAge(s.info, CacheTtl.INFO, s.tInfo, now,
                "Info emiten: — (belum ada data)"));
        if (s.finJson != null) {
            bindFinancial(CacheSer.finFromJson(s.finJson), s.tFin);
        } else {
            String empty = "— (belum ada data)";
            tvKeuLr.setText(empty); tvKeuNeraca.setText(empty);
            tvKeuKas.setText(empty); tvKeuVal.setText(empty);
        }
    }

    /** Tampilkan chart + quote dari ChartData (cache maupun fresh). */
    private void bindChart(YahooClient.ChartData d, long updatedAt) {
        final YahooClient.Meta m = d.meta;
        candles = d.candles;
        currentPrice = m.price;
        String nm = m.longName.isEmpty() ? "" : " — " + m.longName;
        tvSym.setText(symbol.replace(".JK", "") + nm);
        tvPrice.setText(Fmt.rp(m.price));
        MainActivity.stylePill(tvChange, Fmt.pct(m.changePct), m.changePct >= 0);
        Candle last = candles.get(candles.size() - 1);
        tvOhlc.setText(String.format(Locale.GERMANY,
                "O %s  H %s  L %s  C %s  •  Vol %s",
                Fmt.rp(last.open), Fmt.rp(last.high),
                Fmt.rp(last.low), Fmt.rp(last.close),
                Fmt.vol(last.volume)));
        StringBuilder f = new StringBuilder();
        f.append("52W Tertinggi: ").append(Fmt.rp(m.w52high));
        f.append("  •  Terendah: ").append(Fmt.rp(m.w52low));
        f.append("\nDiv Yield (TTM): ");
        f.append(Double.isNaN(m.divYieldPct) ? "—"
                : String.format(Locale.GERMANY, "%,.2f%%", m.divYieldPct));
        f.append("\nP/E dihitung di seksi Valuasi & Pertumbuhan di bawah");
        tvFund.setText(f.toString());
        // Jangan timpa chart intraday yang sedang aktif.
        if (!intraday) {
            chartJson = ChartJson.build(candles, sel);
            pushData();
        }
    }

    /** Tampilkan 4 sub-kartu keuangan (valuasi dihitung dari harga kini). */
    private void bindFinancial(FinancialsClient.FinData f, long updatedAt) {
        long now = System.currentTimeMillis();
        tvKeuLr.setText(FinancialsClient.formatLabaRugi(f));
        tvKeuNeraca.setText(FinancialsClient.formatNeraca(f));
        tvKeuKas.setText(FinancialsClient.formatArusKas(f));
        String val = FinancialsClient.formatValuasi(f, currentPrice);
        tvKeuVal.setText(withAge(val, CacheTtl.FINANCIAL, updatedAt, now, null));
    }

    /** Teks + penanda umur ("— diperbarui 3 hari lalu"). */
    private String withAge(String text, String category, long updatedAt) {
        return withAge(text, category, updatedAt, System.currentTimeMillis(), null);
    }

    private String withAge(String text, String category, long updatedAt, long now,
                           String emptyText) {
        if (text == null || text.isEmpty())
            return emptyText != null ? emptyText : "— (belum ada data)";
        if (updatedAt <= 0) return text;
        return text + "\n\n— diperbarui " + CacheTtl.ageLabel(updatedAt, now);
    }

    // ---------- fetchers (null bila gagal; keputusan cache di pemanggil) ----------

    /** Komposisi pemegang saham (agregat). */
    static String fetchHolders(String symbol) {
        try {
            HoldersClient.HoldersData h = HoldersClient.fetchHolders(symbol);
            StringBuilder s = new StringBuilder("Komposisi agregat (sumber: Yahoo Finance):\n");
            s.append("• Insider/pengendali: ").append(pct1(h.insidersPct)).append('\n');
            s.append("• Institusi: ").append(pct1(h.institutionsPct)).append('\n');
            s.append("• Institusi thd saham publik: ").append(pct1(h.floatPct)).append('\n');
            s.append("• Jumlah institusi: ").append(h.institutionsCount < 0 ? "—"
                    : String.valueOf(h.institutionsCount));
            return s.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchHolders() { return fetchHolders(symbol); }

    /** Data dividen. */
    static String fetchDividen(String symbol) {
        try {
            return DividendClient.format(DividendClient.fetch(symbol));
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchDividen() { return fetchDividen(symbol); }

    /** Info emiten (sektor/industri). */
    static String fetchInfo(String symbol) {
        try {
            return InfoClient.format(InfoClient.fetch(symbol));
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchInfo() { return fetchInfo(symbol); }

    static String pct1(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(Locale.GERMANY, "%,.2f%%", v * 100.0);
    }
}
