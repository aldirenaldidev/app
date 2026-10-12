package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dashboard v2 "Senja Berani": sapaan + hero IHSG + ringkasan portofolio +
 * preview watchlist + komoditas. Cache-first, fetch paralel (4 thread):
 * Fase 1 = IHSG + watchlist (prioritas), Fase 2 = komoditas (latar).
 */
public class MainActivity extends Activity {

    private static final long STALE_MS = 5 * 60_000;
    private static final String[] DEFAULT_WATCH = {"BBCA.JK", "BBRI.JK", "TLKM.JK", "ADRO.JK"};
    private static final String[] DEFAULT_WATCH_NAMES = {
            "Bank Central Asia", "Bank Rakyat Indonesia", "Telkom Indonesia", "Alamtri Resources"};

    private TextView tvGreeting, tvIhsgLive, tvIhsgVal, tvIhsgChg, tvIhsgTime;
    private TextView tvPortVal, tvPortPnl, tvUpdated;
    private SparkView sparkIhsgHero;
    private LinearLayout llWatchPreview, llCommodities;
    private View dotMarket;
    private MarketDb db;
    private ExecutorService pool;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        db = new MarketDb(this);
        pool = Executors.newFixedThreadPool(4);
        AdsHelper.init(this);
        AdsHelper.banner(this, R.id.adBanner);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvIhsgLive = findViewById(R.id.tvIhsgLive);
        tvIhsgVal = findViewById(R.id.tvIhsgVal);
        tvIhsgChg = findViewById(R.id.tvIhsgChg);
        tvIhsgTime = findViewById(R.id.tvIhsgTime);
        tvPortVal = findViewById(R.id.tvPortVal);
        tvPortPnl = findViewById(R.id.tvPortPnl);
        tvUpdated = findViewById(R.id.tvUpdated);
        sparkIhsgHero = findViewById(R.id.sparkIhsgHero);
        llWatchPreview = findViewById(R.id.llWatchPreview);
        llCommodities = findViewById(R.id.llCommodities);
        dotMarket = findViewById(R.id.dotMarket);
        sparkIhsgHero.setLineColor(0xFFFF7A1A);

        tvGreeting.setText(greeting());

        AlertScheduler.schedule(this);
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        BottomNav.setup(this, 0);
        findViewById(R.id.btnNewsTop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, NewsActivity.class)); }
        });
        findViewById(R.id.btnSettingsTop).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
        findViewById(R.id.btnWatchAll).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, WatchlistActivity.class)); }
        });
        findViewById(R.id.cardPortfolio).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, PortfolioActivity.class)); }
        });

        autoLoad();
    }

    @Override protected void onResume() {
        super.onResume();
        autoLoad();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (pool != null) pool.shutdownNow();
    }

    private static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 11) return "Selamat pagi,";
        if (h < 15) return "Selamat siang,";
        if (h < 19) return "Selamat sore,";
        return "Selamat malam,";
    }

    private void updateMarketStatus() {
        MarketStatus.Info i = MarketStatus.now();
        if (i.open) {
            tvIhsgLive.setText(" • LIVE");
            Animation pulse = new AlphaAnimation(1f, 0.3f);
            pulse.setDuration(800);
            pulse.setRepeatMode(Animation.REVERSE);
            pulse.setRepeatCount(Animation.INFINITE);
            dotMarket.startAnimation(pulse);
        } else {
            tvIhsgLive.setText(" • " + i.label);
            dotMarket.clearAnimation();
        }
    }

    /** Cache-first: tampilkan cache segera, fetch paralel bila basi. */
    private void autoLoad() {
        updateMarketStatus();
        bindPortfolio();
        long upd = db.mktUpdated();
        if (upd > 0) {
            bindIhsgFromCache();
            bindWatchFromCache();
            bindCommoditiesFromCache();
            tvUpdated.setText("Terakhir diperbarui " + Fmt.fmtHour(upd) + " WIB • otomatis");
        } else {
            tvUpdated.setText("Memuat data pasar…");
        }
        if (Prefs.isStale(upd, System.currentTimeMillis(), STALE_MS)) {
            loadPhase1();
            loadPhase2();
        }
    }

    // ---------- IHSG ----------

    private void bindIhsgFromCache() {
        MarketDb.MktRow r = db.mktGet("^JKSE");
        if (r == null) return;
        tvIhsgVal.setText(Fmt.idx(r.price));
        double pts = r.price - r.prevClose;
        tvIhsgChg.setText(String.format(Locale.GERMANY, "%+.2f (%+.2f%%)", pts, r.chg));
        tvIhsgChg.setTextColor(r.chg >= 0 ? 0xFF16C784 : 0xFFEA3943);
        setSpark(sparkIhsgHero, r.closesCsv);
    }

    private void setSpark(SparkView spark, String csv) {
        if (csv == null || csv.isEmpty()) return;
        String[] parts = csv.split(",");
        double[] closes = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try { closes[i] = Double.parseDouble(parts[i]); }
            catch (Exception e) { closes[i] = Double.NaN; }
        }
        spark.setData(closes);
    }

    // ---------- Portofolio ----------

    private void bindPortfolio() {
        try {
            List<MarketDb.Tx> txs = db.txAll();
            List<PortfolioCalc.Position> pos = PortfolioCalc.aggregate(txs);
            if (pos.isEmpty()) {
                tvPortVal.setText("Rp —");
                tvPortPnl.setText("Belum ada portofolio");
                tvPortPnl.setTextColor(0xFF9A8FA8);
                return;
            }
            double cost = 0, mv = 0, dayPnl = 0;
            for (PortfolioCalc.Position p : pos) {
                cost += p.cost;
                double px = p.avgPrice;
                double[] cached = db.wlPriceGet(p.symbol);
                if (cached != null && !Double.isNaN(cached[0])) {
                    px = cached[0];
                    dayPnl += p.shares * px * cached[1] / 100.0;
                }
                mv += p.shares * px;
            }
            double pnl = mv - cost;
            double pnlPct = cost > 0 ? pnl / cost * 100 : 0;
            tvPortVal.setText(Fmt.rpShort(mv));
            tvPortPnl.setText(String.format(Locale.GERMANY, "%s (%+.2f%%) • hari ini %s",
                    Fmt.rpShort(pnl), pnlPct, Fmt.rpShort(dayPnl)));
            tvPortPnl.setTextColor(pnl >= 0 ? 0xFF16C784 : 0xFFEA3943);
        } catch (Exception e) {
            tvPortVal.setText("Rp —");
            tvPortPnl.setText("—");
        }
    }

    // ---------- Watchlist preview ----------

    private List<String[]> watchSymbols() {
        List<String[]> wl = db.wlAll();
        if (!wl.isEmpty()) return wl.subList(0, Math.min(4, wl.size()));
        List<String[]> def = new ArrayList<>();
        for (int i = 0; i < DEFAULT_WATCH.length; i++) {
            def.add(new String[]{DEFAULT_WATCH[i], DEFAULT_WATCH_NAMES[i]});
        }
        return def;
    }

    private void bindWatchFromCache() {
        llWatchPreview.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (String[] w : watchSymbols()) {
            View v = inf.inflate(R.layout.item_watch_home, llWatchPreview, false);
            bindWatchCard(v, w[0], w[1], db.wlPriceGet(w[0]), null);
            final String sym = w[0];
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View vv) {
                    Intent it = new Intent(MainActivity.this, DetailActivity.class);
                    it.putExtra("symbol", sym);
                    startActivity(it);
                }
            });
            llWatchPreview.addView(v);
        }
    }

    private void bindWatchCard(View v, String symbol, String name,
                               double[] cached, YahooClient.ChartData chart) {
        TextView tvSym = v.findViewById(R.id.tvHSym);
        TextView tvName = v.findViewById(R.id.tvHName);
        TextView tvPrice = v.findViewById(R.id.tvHPrice);
        TextView tvChg = v.findViewById(R.id.tvHChg);
        ImageView ivLogo = v.findViewById(R.id.ivHLogo);
        TextView tvInit = v.findViewById(R.id.tvHLogoInit);
        SparkView spark = v.findViewById(R.id.sparkH);

        String shortSym = symbol.replace(".JK", "");
        tvSym.setText(shortSym);
        tvName.setText(name != null ? name : shortSym);

        int logoRes = LogoUtil.resId(symbol);
        if (logoRes != 0) {
            ivLogo.setImageResource(logoRes);
            ivLogo.setVisibility(View.VISIBLE);
            tvInit.setVisibility(View.GONE);
        } else {
            ivLogo.setVisibility(View.GONE);
            tvInit.setVisibility(View.VISIBLE);
            tvInit.setText(LogoUtil.initials(symbol));
        }

        double price = Double.NaN, chg = Double.NaN;
        if (chart != null && chart.meta != null) {
            price = chart.meta.price;
            chg = chart.meta.changePct;
            List<Candle> cs = chart.candles;
            double[] closes = new double[cs.size()];
            for (int i = 0; i < closes.length; i++) closes[i] = cs.get(i).close;
            spark.setData(closes);
        } else if (cached != null) {
            price = cached[0];
            chg = cached[1];
        }
        if (!Double.isNaN(price)) {
            tvPrice.setText(Fmt.rp(price));
            tvChg.setText(Fmt.pct(chg));
            tvChg.setTextColor(chg >= 0 ? 0xFF16C784 : 0xFFEA3943);
        }
    }

    // ---------- Komoditas ----------

    private static final String[][] COMMODITIES = {
            {"USD/IDR", "USDIDR=X"}, {"Emas", "GC=F"}, {"Perak", "SI=F"},
            {"Brent", "BZ=F"}, {"WTI", "CL=F"}, {"Tembaga", "HG=F"},
            {"Batubara", "MTF=F"}, {"Nikel", "NICK.L"}, {"Timah", "TINM.L"}
    };

    private void bindCommoditiesFromCache() {
        llCommodities.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (String[] c : COMMODITIES) {
            MarketDb.MktRow r = db.mktGet(c[1]);
            View v = inf.inflate(R.layout.item_commodity, llCommodities, false);
            TextView tvN = v.findViewById(R.id.tvCName);
            TextView tvP = v.findViewById(R.id.tvCPrice);
            TextView tvC = v.findViewById(R.id.tvCChg);
            tvN.setText(c[0]);
            if (r != null) {
                tvP.setText(Fmt.usd(r.price));
                tvC.setText(Fmt.pct(r.chg));
                tvC.setTextColor(r.chg >= 0 ? 0xFF16C784 : 0xFFEA3943);
            }
            llCommodities.addView(v);
        }
    }

    // ---------- Fetch paralel ----------

    /** Fase 1 (prioritas): IHSG + watchlist, paralel. */
    private void loadPhase1() {
        final List<String[]> watch = watchSymbols();
        pool.execute(new Runnable() {
            public void run() {
                try {
                    final YahooClient.ChartData ihsg = YahooClient.fetchChart("^JKSE", "1mo");
                    saveMkt("^JKSE", ihsg);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (isFinishing()) return;
                            bindIhsgFromCache();
                            tvIhsgTime.setText("Hari ini • "
                                    + Fmt.fmtHour(System.currentTimeMillis()) + " WIB");
                        }
                    });
                } catch (Exception e) { /* cache tetap tampil */ }

                // Watchlist paralel (maks 4)
                final List<Object[]> results = new ArrayList<>();
                List<Thread> threads = new ArrayList<>();
                for (final String[] w : watch) {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try {
                                YahooClient.ChartData d = YahooClient.fetchChart(w[0], "5d");
                                db.wlPriceSave(w[0], d.meta.price, d.meta.changePct);
                                synchronized (results) {
                                    results.add(new Object[]{w[0], w[1], d});
                                }
                            } catch (Exception e) { /* abaikan */ }
                        }
                    });
                    threads.add(t);
                    t.start();
                }
                for (Thread t : threads) {
                    try { t.join(12000); } catch (Exception e) { /* abaikan */ }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (isFinishing()) return;
                        if (!results.isEmpty()) {
                            LayoutInflater inf = LayoutInflater.from(MainActivity.this);
                            llWatchPreview.removeAllViews();
                            for (Object[] r : results) {
                                View v = inf.inflate(R.layout.item_watch_home,
                                        llWatchPreview, false);
                                bindWatchCard(v, (String) r[0], (String) r[1], null,
                                        (YahooClient.ChartData) r[2]);
                                final String sym = (String) r[0];
                                v.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View vv) {
                                        Intent it = new Intent(MainActivity.this,
                                                DetailActivity.class);
                                        it.putExtra("symbol", sym);
                                        startActivity(it);
                                    }
                                });
                                llWatchPreview.addView(v);
                            }
                        }
                        bindPortfolio();
                        tvUpdated.setText("Terakhir diperbarui "
                                + Fmt.fmtHour(System.currentTimeMillis()) + " WIB • otomatis");
                    }
                });
            }
        });
    }

    /** Fase 2 (latar): 9 komoditas, paralel. */
    private void loadPhase2() {
        pool.execute(new Runnable() {
            public void run() {
                List<Thread> threads = new ArrayList<>();
                for (final String[] c : COMMODITIES) {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            try {
                                YahooClient.ChartData d = YahooClient.fetchChart(c[1], "5d");
                                saveMkt(c[1], d);
                            } catch (Exception e) { /* abaikan */ }
                        }
                    });
                    threads.add(t);
                    t.start();
                }
                for (Thread t : threads) {
                    try { t.join(15000); } catch (Exception e) { /* abaikan */ }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (isFinishing()) return;
                        bindCommoditiesFromCache();
                    }
                });
            }
        });
    }

    private void saveMkt(String symbol, YahooClient.ChartData d) {
        YahooClient.Meta m = d.meta;
        StringBuilder csv = new StringBuilder();
        List<Candle> cs = d.candles;
        int start = Math.max(0, cs.size() - 60);
        for (int i = start; i < cs.size(); i++) {
            if (csv.length() > 0) csv.append(',');
            csv.append(cs.get(i).close);
        }
        db.mktSave(symbol, m.price, m.changePct, m.prevClose,
                m.w52high, m.w52low, csv.toString());
    }

    /** Helper bersama: pill % hijau/merah (dipakai DetailActivity juga). */
    static void stylePill(TextView tv, String txt, boolean up) {
        tv.setText(txt);
        tv.setTextColor(up ? 0xFF16C784 : 0xFFEA3943);
        tv.setBackgroundResource(up ? R.drawable.pill_up : R.drawable.pill_down);
    }

    static void stylePillText(TextView tv, String txt, boolean up) {
        tv.setText(txt);
        tv.setTextColor(up ? 0xFF16C784 : 0xFFEA3943);
    }
}
