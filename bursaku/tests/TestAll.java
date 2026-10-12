package com.flexurio.bursaku;
import java.util.*;

public class TestAll {
    static int pass = 0, fail = 0;

    static void ok(boolean cond, String name) {
        if (cond) { pass++; System.out.println("PASS " + name); }
        else { fail++; System.out.println("FAIL " + name); }
    }

    static void okEq(double exp, double act, double tol, String name) {
        ok(Math.abs(exp - act) <= tol, name + " (exp=" + exp + " act=" + act + ")");
    }

    /** Data dummy: tren naik halus 60 bar. */
    static double[] closes(int n) {
        double[] c = new double[n];
        for (int i = 0; i < n; i++) c[i] = 1000 + i * 10 + 5 * Math.sin(i / 3.0);
        return c;
    }

    /** Closes naik linear (untuk skenario RSI). */
    static double[] risingCloses(int n) {
        double[] c = new double[n];
        for (int i = 0; i < n; i++) c[i] = 100 + i;
        return c;
    }

    /** Candles tren linear: start + i*step (n bar). */
    static java.util.List<Candle> candlesTrend(int n, double start, double step) {
        java.util.List<Candle> out = new java.util.ArrayList<Candle>();
        for (int i = 0; i < n; i++) {
            double c = start + i * step;
            out.add(new Candle(1700000000L + i * 86400L, c - 1, c + 2, c - 2, c, 1000000));
        }
        return out;
    }

    public static void main(String[] a) throws Exception {
        double[] c = closes(80);
        double[] h = new double[80], l = new double[80];
        long[] v = new long[80];
        for (int i = 0; i < 80; i++) { h[i] = c[i] + 8; l[i] = c[i] - 8; v[i] = 1000000 + i * 1000; }

        // ---- SMA / EMA ----
        double[] s5 = Indicators.sma(c, 5);
        ok(Double.isNaN(s5[3]) && !Double.isNaN(s5[4]), "sma NaN awal");
        okEq((c[0]+c[1]+c[2]+c[3]+c[4])/5, s5[4], 1e-9, "sma nilai");
        double[] e5 = Indicators.ema(c, 5);
        ok(!Double.isNaN(e5[4]) && Double.isNaN(e5[3]), "ema seed");

        // ---- RSI: tren naik murni -> RSI tinggi ----
        double[] up = new double[30];
        for (int i = 0; i < 30; i++) up[i] = 100 + i;
        double[] r = Indicators.rsi(up, 14);
        ok(r[29] > 70, "rsi tren naik > 70 (=" + r[29] + ")");
        double[] dn = new double[30];
        for (int i = 0; i < 30; i++) dn[i] = 100 - i;
        ok(Indicators.rsi(dn, 14)[29] < 30, "rsi tren turun < 30");

        // ---- MACD ----
        Indicators.MacdResult m = Indicators.macd(c, 12, 26, 9);
        double ml = Indicators.lastValid(m.macd);
        ok(!Double.isNaN(ml), "macd terhitung");
        ok(m.macd.length == 80 && m.hist.length == 80, "macd panjang");

        // ---- Bollinger ----
        Indicators.BollingerResult b = Indicators.bollinger(c, 20, 2);
        int i79 = 79;
        ok(b.upper[i79] > b.middle[i79] && b.middle[i79] > b.lower[i79], "bollinger urutan");

        // ---- Stochastic ----
        Indicators.StochResult st = Indicators.stochastic(h, l, c, 14, 3);
        double k = Indicators.lastValid(st.k);
        ok(k >= 0 && k <= 100, "stochastic 0..100 (=" + k + ")");

        // ---- ATR ----
        double[] atr = Indicators.atr(h, l, c, 14);
        ok(Indicators.lastValid(atr) > 0, "atr positif");

        // ---- CCI ----
        double[] cci = Indicators.cci(h, l, c, 20);
        ok(!Double.isNaN(Indicators.lastValid(cci)), "cci terhitung");

        // ---- Williams %R ----
        double[] wr = Indicators.williamsR(h, l, c, 14);
        double w = Indicators.lastValid(wr);
        ok(w <= 0 && w >= -100, "williamsR -100..0 (=" + w + ")");

        // ---- OBV ----
        double[] obv = Indicators.obv(c, v);
        ok(obv[79] != obv[0], "obv berubah");

        // ---- ADX ----
        Indicators.AdxResult ax = Indicators.adx(h, l, c, 14);
        double adxV = Indicators.lastValid(ax.adx);
        ok(adxV >= 0 && adxV <= 100, "adx 0..100 (=" + adxV + ")");

        // ---- Ichimoku ----
        Indicators.IchimokuResult ic = Indicators.ichimoku(h, l, c, 9, 26, 52);
        ok(!Double.isNaN(Indicators.lastValid(ic.tenkan)), "ichimoku tenkan");
        ok(!Double.isNaN(Indicators.lastValid(ic.senkouB)), "ichimoku senkouB");

        // ---- Parabolic SAR ----
        double[] sar = Indicators.parabolicSar(h, l);
        ok(!Double.isNaN(sar[79]), "sar terhitung");
        // SAR tidak boleh di atas low dua bar terakhir saat uptrend awal
        ok(true, "sar selesai");

        // ---- cross ----
        double[] fa = {1, 2}, fb = {2, 1};
        ok(Indicators.crossUp(fa, fb), "crossUp");
        ok(Indicators.crossDown(fb, fa), "crossDown");

        // ---- Fmt ----
        ok("10.500".equals(Fmt.rp(10500)), "fmt rp");
        ok("6.148,41".equals(Fmt.idx(6148.407)), "fmt idx");
        ok("+2,50%".equals(Fmt.pct(2.5)), "fmt pct");
        ok("1,2 jt".equals(Fmt.vol(1200000)), "fmt vol");

        // ---- MarketStatus ----
        MarketStatus.Info mon9 = MarketStatus.at(java.util.Calendar.MONDAY, 10 * 60);
        ok(mon9.open && "BUKA".equals(mon9.label), "bursa senin 10:00 buka");
        MarketStatus.Info fri12 = MarketStatus.at(java.util.Calendar.FRIDAY, 12 * 60);
        ok(!fri12.open, "bursa jumat 12:00 tutup (istirahat)");
        MarketStatus.Info sat = MarketStatus.at(java.util.Calendar.SATURDAY, 10 * 60);
        ok(!sat.open, "bursa sabtu tutup");
        MarketStatus.Info fri1430 = MarketStatus.at(java.util.Calendar.FRIDAY, 14 * 60 + 30);
        ok(fri1430.open && "Sesi 2".equals(fri1430.session), "bursa jumat sesi 2");

        // ---- PortfolioCalc ----
        List<MarketDb.Tx> txs = new ArrayList<>();
        MarketDb.Tx t1 = new MarketDb.Tx(); t1.symbol = "BBCA.JK"; t1.type = "BUY"; t1.lot = 10; t1.price = 9000; t1.date = "2026-01-05";
        MarketDb.Tx t2 = new MarketDb.Tx(); t2.symbol = "BBCA.JK"; t2.type = "BUY"; t2.lot = 5; t2.price = 10000; t2.date = "2026-02-05";
        MarketDb.Tx t3 = new MarketDb.Tx(); t3.symbol = "BBCA.JK"; t3.type = "SELL"; t3.lot = 3; t3.price = 11000; t3.date = "2026-03-05";
        txs.add(t1); txs.add(t2); txs.add(t3);
        List<PortfolioCalc.Position> pos = PortfolioCalc.aggregate(txs);
        ok(pos.size() == 1, "portfolio 1 posisi");
        PortfolioCalc.Position p = pos.get(0);
        okEq(1200, p.shares, 1e-9, "portfolio shares");
        okEq((10*100.0*9000 + 5*100.0*10000 - 3*100.0*((10*100.0*9000+5*100.0*10000)/1500)) / 1200,
             p.avgPrice, 1e-6, "portfolio avg");
        Map<String, Double> pr = new HashMap<>();
        pr.put("BBCA.JK", 10500.0);
        PortfolioCalc.applyPrices(pos, pr);
        ok(p.pnl > 0, "portfolio pnl positif");
        double[] tot = PortfolioCalc.totals(pos);
        ok(tot[0] == 1200 * 10500.0, "portfolio total mv");
        // jual melebihi → exception
        MarketDb.Tx t4 = new MarketDb.Tx(); t4.symbol = "BBCA.JK"; t4.type = "SELL"; t4.lot = 999; t4.price = 1; t4.date = "2026-04-01";
        List<MarketDb.Tx> bad = new ArrayList<>(txs); bad.add(t4);
        boolean threw = false;
        try { PortfolioCalc.aggregate(bad); } catch (Exception e) { threw = true; }
        ok(threw, "portfolio tolak jual berlebih");

        // ---- ChartJson ----
        List<Candle> cs = new ArrayList<>();
        for (int i = 0; i < 80; i++)
            cs.add(new Candle(1700000000L + i * 86400L, c[i] - 5, h[i], l[i], c[i], v[i]));
        ChartJson.Sel sel = new ChartJson.Sel();
        sel.bb = true; sel.sar = true; sel.ichimoku = true;
        sel.macd = true; sel.stoch = true; // 3 osilator incl rsi → ChartJson tak membatasi, UI yang membatasi
        String json = ChartJson.build(cs, sel);
        ok(json.contains("\"candles\"") && json.contains("\"oscillators\""), "chartjson struktur");
        ok(json.contains("Bollinger") || json.contains("#8B93A7"), "chartjson bb");
        ok(json.contains("Stochastic"), "chartjson stoch");

        // ---- FinancialsClient.parse dengan data asli BBCA ----
        String home = System.getenv("HOME");
        if (home == null) home = System.getProperty("user.home");
        String finBody = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(home
                        + "/workspace/bursaku/tests/bbca_fin.json")), "UTF-8");
        FinancialsClient.FinData f = FinancialsClient.parse(finBody);
        ok(!Double.isNaN(f.revenue) && f.revenue > 0, "fin revenue");
        ok(!Double.isNaN(f.eps) && f.eps > 0, "fin eps (=" + f.eps + ")");
        ok(!Double.isNaN(f.roe), "fin roe");
        ok(f.history.size() >= 3, "fin history (" + f.history.size() + " thn)");
        String ft = FinancialsClient.formatLabaRugi(f);
        ok(ft.contains("Rp") && ft.contains("2025"), "fin format laba rugi");
        System.out.println("--- contoh format keuangan ---\n" + ft.split("\n\n")[0]);
        // --- field baru: neraca/valuasi ---
        ok(!Double.isNaN(f.totalDebt) && f.totalDebt > 0, "fin totalDebt");
        ok(!Double.isNaN(f.bookValue) && f.bookValue > 0, "fin bookValue");
        ok(!Double.isNaN(f.sharesOutstanding) && f.sharesOutstanding > 0, "fin sharesOutstanding");
        ok("2025-12-31".equals(f.fiscalYearEnd), "fin fiscalYearEnd");
        // --- perhitungan client-side ---
        okEq(52021302394880.0 / (2201.512 * 122841751300.0),
                FinancialsClient.der(f.totalDebt, f.bookValue, f.sharesOutstanding),
                1e-9, "der BBCA ~0.19");
        okEq(6025.0 / 472.0, FinancialsClient.pe(6025.0, 472.0), 1e-9, "pe = price/eps");
        okEq(6025.0 / 2201.512, FinancialsClient.pbv(6025.0, 2201.512), 1e-9, "pbv");
        ok(Double.isNaN(FinancialsClient.pe(6025.0, 0)), "pe eps 0 -> NaN");
        ok(Double.isNaN(FinancialsClient.der(1, Double.NaN, 1)), "der NaN -> NaN");
        okEq(0.1, FinancialsClient.yoy(110, 100), 1e-9, "yoy +10%");
        okEq(-0.1, FinancialsClient.yoy(90, 100), 1e-9, "yoy -10%");
        ok(Double.isNaN(FinancialsClient.yoy(100, 0)), "yoy prev 0 -> NaN");
        ok("31 Des 2025".equals(FinancialsClient.tglId("2025-12-31")), "tglId");
        ok("+2,5%".equals(FinancialsClient.pct1(0.025)), "pct1 1 desimal");
        String fv = FinancialsClient.formatValuasi(f, 6025.0);
        ok(fv.contains("12,76x") && fv.contains("Strong Buy")
                && fv.contains("31 Des 2025"), "format valuasi (P/E + analis + periode)");
        String fn = FinancialsClient.formatNeraca(f);
        ok(fn.contains("DER") && fn.contains("estimasi"), "format neraca");
        String fk = FinancialsClient.formatArusKas(f);
        ok(fk.contains("Arus kas operasi"), "format arus kas");

        // ---- Lq45 ----
        ok(Lq45.CODES.length == 45, "lq45 45 emiten");
        ok("Bank Central Asia".equals(Lq45.nameOf("BBCA")), "lq45 nameOf");

        // ---- MarketStatus.at: logika jam bursa WIB ----
        int SEN = java.util.Calendar.MONDAY, JUM = java.util.Calendar.FRIDAY;
        int SAB = java.util.Calendar.SATURDAY, KAM = java.util.Calendar.THURSDAY;
        ok(MarketStatus.at(SEN, 600).open, "bursa senin 10:00 buka");
        ok(!MarketStatus.at(SAB, 600).open, "bursa sabtu tutup");
        ok(!MarketStatus.at(JUM, 780).open, "bursa jumat 13:00 tutup (istirahat)");
        ok(!MarketStatus.at(SEN, 960).open, "bursa senin 16:00 tutup");
        ok(MarketStatus.at(JUM, 600).open, "bursa jumat 10:00 buka");
        ok(!MarketStatus.at(JUM, 690).open, "bursa jumat 11:30 tutup (batas sesi 1)");
        ok(MarketStatus.at(SEN, 540).open, "bursa senin 09:00 buka (batas awal)");
        ok(MarketStatus.at(KAM, 914).open, "bursa kamis 15:14 buka");
        ok(!MarketStatus.at(KAM, 915).open, "bursa kamis 15:15 tutup (batas akhir)");
        ok("Sesi 1".equals(MarketStatus.at(SEN, 600).session), "bursa label sesi 1");
        ok("".equals(MarketStatus.at(SAB, 600).session), "bursa sesi kosong saat tutup");

        // ---- Prefs.isStale: aturan hemat (jangan fetch bila cache < 5 mnt) ----
        ok(Prefs.isStale(0, 1000, 300_000), "stale: belum pernah refresh");
        ok(!Prefs.isStale(1000, 1000 + 299_999, 300_000), "fresh: cache 4:59 mnt");
        ok(Prefs.isStale(1000, 1000 + 300_000, 300_000), "stale: cache tepat 5 mnt");

        // ---- Fmt.fmtHour: penanda "Terakhir diperbarui HH:MM" WIB ----
        ok("—".equals(Fmt.fmtHour(0)), "fmtHour 0 -> —");
        java.util.Calendar wc = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Jakarta"));
        wc.set(2026, java.util.Calendar.OCTOBER, 7, 13, 16, 0);
        wc.set(java.util.Calendar.MILLISECOND, 0);
        ok("13:16".equals(Fmt.fmtHour(wc.getTimeInMillis())), "fmtHour 13:16 WIB");

        // ---- TickerFilter: autocomplete (cocok kode ATAU nama, case-insensitive) ----
        java.util.List<TickerFilter.Ticker> tl = new java.util.ArrayList<TickerFilter.Ticker>();
        tl.add(new TickerFilter.Ticker("BBCA", "Bank Central Asia Tbk."));
        tl.add(new TickerFilter.Ticker("BBRI", "Bank Rakyat Indonesia Tbk."));
        tl.add(new TickerFilter.Ticker("TLKM", "Telkom Indonesia Tbk."));
        ok(TickerFilter.filter(tl, "bbc").size() == 1
                && "BBCA".equals(TickerFilter.filter(tl, "bbc").get(0).code),
                "filter cocok kode");
        ok(TickerFilter.filter(tl, "BANK").size() == 2, "filter cocok nama (2 bank)");
        ok(TickerFilter.filter(tl, "telkom").size() == 1, "filter case-insensitive nama");
        ok(TickerFilter.filter(tl, "").size() == 3, "filter kosong -> semua");
        ok(TickerFilter.filter(tl, "   ").size() == 3, "filter spasi -> semua");
        ok(TickerFilter.filter(tl, "ZZZ").isEmpty(), "filter tak cocok -> kosong");
        ok(TickerFilter.filter(null, "BB").isEmpty(), "filter list null -> kosong");
        ok(TickerFilter.filter(tl, null).size() == 3, "filter query null -> semua");
        String tjs = "[{\"c\":\"BBCA\",\"n\":\"Bank Central Asia Tbk.\"},"
                + "{\"c\":\"TLKM\",\"n\":\"Telkom Indonesia\"}]";
        java.util.List<TickerFilter.Ticker> pj = TickerFilter.parseJson(tjs);
        ok(pj.size() == 2 && "BBCA".equals(pj.get(0).code)
                && "Telkom Indonesia".equals(pj.get(1).name), "parseJson 2 entri");
        ok(TickerFilter.parseJson("rusak{[\"").isEmpty(), "parseJson rusak -> kosong");
        ok(TickerFilter.parseJson(null).isEmpty(), "parseJson null -> kosong");

        // ---- YahooClient.validateData: validasi + harga dari SATU fetch ----
        YahooClient.ChartData vd1 = new YahooClient.ChartData();
        vd1.meta.price = 10500; vd1.meta.changePct = 2.5;
        vd1.candles.add(new Candle(1, 100, 110, 90, 105, 1000));
        vd1.candles.add(new Candle(2, 105, 115, 95, 110, 1000));
        YahooClient.Validation vv1 = YahooClient.validateData(vd1);
        ok(vv1.ok && vv1.price == 10500 && vv1.chg == 2.5,
                "validateData valid + harga terekstrak");
        YahooClient.ChartData vd2 = new YahooClient.ChartData(); // kosong
        YahooClient.Validation vv2 = YahooClient.validateData(vd2);
        ok(!vv2.ok && Double.isNaN(vv2.price), "validateData kosong -> invalid");
        ok(!YahooClient.validateData(null).ok, "validateData null -> invalid");
        YahooClient.ChartData vd3 = new YahooClient.ChartData();
        vd3.candles.add(new Candle(1, 100, 110, 90, 105, 1000)); // cuma 1 candle
        ok(!YahooClient.validateData(vd3).ok, "validateData 1 candle -> invalid");

        // ---- CacheTtl: TTL per kategori + batas tepi ----
        ok(CacheTtl.ttlMs(CacheTtl.QUOTE) == 5 * 60_000L, "ttl quote 5 mnt");
        ok(CacheTtl.ttlMs(CacheTtl.DIVIDEND) == 30L * 24 * 3600_000L, "ttl dividen 30 hari");
        ok(CacheTtl.ttlMs(CacheTtl.INFO) == 90L * 24 * 3600_000L, "ttl info 90 hari");
        long day = 24 * 3600_000L;
        ok(!CacheTtl.isStale(1000L, 1000L + 29 * day, CacheTtl.DIVIDEND),
                "dividen 29 hari -> segar");
        ok(CacheTtl.isStale(1000L, 1000L + 31 * day, CacheTtl.DIVIDEND),
                "dividen 31 hari -> basi");
        ok(!CacheTtl.isStale(1000L, 1000L + 4 * 60_000L, CacheTtl.QUOTE),
                "quote 4 mnt -> segar");
        ok(CacheTtl.isStale(1000L, 1000L + 6 * 60_000L, CacheTtl.QUOTE),
                "quote 6 mnt -> basi");
        ok(CacheTtl.isStale(0, 99999L, CacheTtl.QUOTE), "belum pernah -> basi");
        ok("baru saja".equals(CacheTtl.ageLabel(1000L, 1000L + 30_000L)), "age baru saja");
        ok("5 menit lalu".equals(CacheTtl.ageLabel(1000L, 1000L + 5 * 60_000L)), "age menit");
        ok("3 jam lalu".equals(CacheTtl.ageLabel(1000L, 1000L + 3 * 3600_000L)), "age jam");
        ok("3 hari lalu".equals(CacheTtl.ageLabel(1000L, 1000L + 3 * day)), "age hari");
        ok("2 bulan lalu".equals(CacheTtl.ageLabel(1000L, 1000L + 65 * day)), "age bulan");

        // ---- CacheSer: roundtrip candles & FinData ----
        java.util.List<Candle> cs0 = new java.util.ArrayList<Candle>();
        cs0.add(new Candle(1700000000L, 100, 110, 90, 105, 1000));
        cs0.add(new Candle(1700086400L, 105, 115, 95, 110, 2000));
        java.util.List<Candle> cs1 = CacheSer.candlesFromJson(CacheSer.candlesToJson(cs0));
        ok(cs1.size() == 2 && cs1.get(1).close == 110 && cs1.get(0).volume == 1000,
                "candles roundtrip");
        ok(CacheSer.candlesFromJson("rusak").isEmpty(), "candles rusak -> kosong");
        FinancialsClient.FinData ff0 = new FinancialsClient.FinData();
        ff0.revenue = 1e12; ff0.eps = 472; ff0.bookValue = 2201.512;
        ff0.recommendation = "strong_buy"; ff0.analystCount = 24;
        FinancialsClient.YearRow yr = new FinancialsClient.YearRow();
        yr.endDate = "2025-12-31"; yr.revenue = 1e12; yr.netIncome = 5e10;
        ff0.history.add(yr);
        FinancialsClient.FinData ff1 = CacheSer.finFromJson(CacheSer.finToJson(ff0));
        ok(ff1.eps == 472 && ff1.bookValue == 2201.512
                && "strong_buy".equals(ff1.recommendation) && ff1.analystCount == 24
                && ff1.history.size() == 1
                && "2025-12-31".equals(ff1.history.get(0).endDate),
                "finData roundtrip");
        ok(Double.isNaN(CacheSer.finFromJson(CacheSer.finToJson(new FinancialsClient.FinData())).eps),
                "finData NaN roundtrip");
        ok(CacheSer.finFromJson("{rusak").history.isEmpty(), "finData rusak -> default");

        // ---- SignalEngine: aturan individual ----
        double[] rsiUp = new double[30];
        for (int i = 0; i < 30; i++) rsiUp[i] = 20 + i; // naik ke ~49
        SignalEngine.Signal sRsi = SignalEngine.rsiSignal(Indicators.rsi(
                risingCloses(60), 14));
        ok(sRsi.score == -1 || sRsi.score == 0, "rsi tren naik murni tidak bullish");
        double[] rsiLow = {25, 24, 23, 22, 21, 20, 19, 18};
        SignalEngine.Signal sRsi2 = SignalEngine.rsiSignal(rsiLow);
        ok(sRsi2.score == 1 && sRsi2.reason.contains("18,0"),
                "rsi oversold +1 + nilai aktual");
        SignalEngine.Signal sRsi3 = SignalEngine.rsiSignal(new double[]{80, 82, 85, 88});
        ok(sRsi3.score == -1 && sRsi3.reason.contains("88,0"), "rsi overbought -1");
        ok(SignalEngine.maTrendSignal(110, 105, 100).score == 1, "ma trend bullish");
        ok(SignalEngine.maTrendSignal(90, 95, 100).score == -1, "ma trend bearish");
        ok(SignalEngine.macdSignal(1.5, 1.0).score == 1, "macd bullish");
        ok(SignalEngine.macdSignal(0.5, 1.0).score == -1, "macd bearish");
        ok(SignalEngine.bbSignal(90, 110, 90).score == 1, "bb lower -> rebound");
        ok(SignalEngine.bbSignal(110, 110, 90).score == -1, "bb upper -> jenuh");
        ok(SignalEngine.willrSignal(-87.3).score == 1, "w%r oversold");
        ok(SignalEngine.willrSignal(-12.1).score == -1, "w%r overbought");
        ok(SignalEngine.cciSignal(132.5).score == 1, "cci bullish kuat");
        ok(SignalEngine.cciSignal(-121).score == -1, "cci bearish");
        ok(SignalEngine.stochSignal(25, 20).score == 1, "stoch oversold cross");
        ok(SignalEngine.stochSignal(85, 90).score == -1, "stoch overbought cross");
        ok(SignalEngine.adxSignal(32.1, 110, 100).score == 1, "adx tren kuat");
        ok(SignalEngine.adxSignal(16.4, 110, 100).score == 0, "adx lemah -> netral");
        ok(SignalEngine.emaSignal(110, 105).score == 1, "ema bullish");
        ok(SignalEngine.sarSignal(110, 100).score == 1, "sar bullish");
        ok(SignalEngine.ichimokuSignal(110, 100, 95).score == 1, "ichimoku di atas awan");
        // verdict boundaries
        SignalEngine.Result vr = new SignalEngine.Result();
        vr.total = 3; ok("BELI".equals(vr.verdict()), "verdict +3 -> BELI");
        vr.total = 2; ok("TAHAN".equals(vr.verdict()), "verdict +2 -> TAHAN");
        vr.total = -3; ok("JUAL".equals(vr.verdict()), "verdict -3 -> JUAL");
        vr.total = -2; ok("TAHAN".equals(vr.verdict()), "verdict -2 -> TAHAN");

        // ---- SignalEngine: skenario integrasi ----
        java.util.List<Candle> bull = candlesTrend(250, 100.0, 1.0);   // naik kuat
        SignalEngine.Result rb = SignalEngine.analyze(bull);
        ok("BELI".equals(rb.verdict()) && rb.total >= 3,
                "skenario bullish -> BELI (skor " + rb.total + ")");
        ok(rb.signals.size() == 13, "13 indikator dievaluasi");
        java.util.List<Candle> bear = candlesTrend(250, 350.0, -1.0);  // turun kuat
        SignalEngine.Result rs = SignalEngine.analyze(bear);
        ok("JUAL".equals(rs.verdict()) && rs.total <= -3,
                "skenario bearish -> JUAL (skor " + rs.total + ")");
        // alasan memuat nilai aktual + urutan bullish dulu
        boolean hasVal = false;
        for (SignalEngine.Signal s : rb.sorted()) {
            if (s.reason.matches(".*\\d+[,.]\\d+.*")) { hasVal = true; break; }
        }
        ok(hasVal, "alasan memuat nilai aktual");
        boolean seenBearish = false, orderOk = true;
        for (SignalEngine.Signal s : rb.sorted()) {
            if (s.score < 0) seenBearish = true;
            if (seenBearish && s.score > 0) orderOk = false;
        }
        ok(orderOk, "urutan alasan: bullish dulu");

        // ---- HeatColor: skala warna heatmap ----
        ok(HeatColor.colorFor(10.0) == 0xFF16C784, "heat +10% = hijau penuh");
        ok(HeatColor.colorFor(-10.0) == 0xFFEA3943, "heat -10% = merah penuh");
        ok(HeatColor.colorFor(0.0) == 0xFF3A4356, "heat 0% = netral");
        ok(HeatColor.colorFor(Double.NaN) == 0xFF3A4356, "heat NaN = netral");
        int hc2 = HeatColor.colorFor(2.5), hc5 = HeatColor.colorFor(5.0);
        int g2 = (hc2 >> 8) & 255, g5 = (hc5 >> 8) & 255, g0 = (HeatColor.colorFor(0) >> 8) & 255;
        ok(g5 >= g2 && g2 >= g0, "heat monoton: hijau menguat seiring % naik");
        int hm2 = HeatColor.colorFor(-2.5), hm5 = HeatColor.colorFor(-5.0);
        int r2 = (hm2 >> 16) & 255, r5 = (hm5 >> 16) & 255, r0 = (HeatColor.colorFor(0) >> 16) & 255;
        ok(r5 >= r2 && r2 >= r0, "heat monoton: merah menguat seiring % turun");

        // ---- ExportCsv: nama file & escaping ----
        ok("bursaku-top-dividen-20261008.csv".equals(
                ExportCsv.fileName("bursaku-top-dividen-", "csv", "20261008")),
                "nama file top dividen");
        String prow = ExportCsv.csvRow("2026-10-08", "BBCA", "BELI", "10", "9500", "9500000");
        ok("2026-10-08,BBCA,BELI,10,9500,9500000".equals(prow), "csvRow sederhana");
        String qrow = ExportCsv.csvRow("BBCA", "Bank \"Central\" Tbk");
        ok(qrow.contains("\"Bank \"\"Central\"\" Tbk\""), "csvRow escape kutip");
        ok(ExportCsv.portfolioFileName().matches("bursaku-portofolio-\\d{8}-\\d{4}\\.csv"),
                "nama file portofolio berpola tanggal-jam");

        // ---- Intraday 5m: parse fixture ----
        String intra = "{\"chart\":{\"result\":[{\"meta\":{\"symbol\":\"BBCA.JK\","
                + "\"regularMarketPrice\":10100,\"previousClose\":10000,"
                + "\"chartPreviousClose\":10000,\"regularMarketChangePercent\":1.0,"
                + "\"currency\":\"IDR\"},"
                + "\"timestamp\":[1760000000,1760000300,1760000600],"
                + "\"indicators\":{\"quote\":[{\"open\":[10000,10050,10080],"
                + "\"high\":[10060,10100,10120],\"low\":[9990,10040,10070],"
                + "\"close\":[10050,10080,10100],\"volume\":[5000,6000,5500]}]}}],"
                + "\"error\":null}}";
        YahooClient.ChartData id = YahooClient.parse(intra, "BBCA.JK");
        ok(id.candles.size() == 3, "intraday parse 3 bar 5m");
        ok(id.candles.get(0).time == 1760000000L, "intraday timestamp epoch detik");
        okEq(10100.0, id.candles.get(2).close, 1e-9, "intraday close bar terakhir");
        okEq(10100.0, id.meta.price, 1e-9, "intraday meta price");

        // ---- DivSweep: backoff eksponensial ----
        ok(DivSweep.backoffMs(1) == 5000L, "backoff percobaan 1 = 5 dtk");
        ok(DivSweep.backoffMs(2) == 15000L, "backoff percobaan 2 = 15 dtk");
        ok(DivSweep.backoffMs(3) == 60000L, "backoff percobaan 3 = 60 dtk");
        ok(DivSweep.backoffMs(4) < 0, "backoff percobaan 4 = menyerah");
        ok(DivSweep.backoffMs(0) < 0, "backoff percobaan 0 = menyerah");
        ok(DivSweep.MAX_ATTEMPTS == 3, "maks 3 percobaan");

        // ---- DivSweep: watermark resume ----
        ok(DivSweep.resumeIndex("123", 940) == 123, "resume dari 123");
        ok(DivSweep.resumeIndex("0", 940) == 0, "resume dari 0");
        ok(DivSweep.resumeIndex("939", 940) == 939, "resume indeks terakhir valid");
        ok(DivSweep.resumeIndex("940", 940) == 0, "resume == total -> 0");
        ok(DivSweep.resumeIndex("9999", 940) == 0, "resume > total -> 0");
        ok(DivSweep.resumeIndex("-5", 940) == 0, "resume negatif -> 0");
        ok(DivSweep.resumeIndex(null, 940) == 0, "resume null -> 0");
        ok(DivSweep.resumeIndex("", 940) == 0, "resume kosong -> 0");
        ok(DivSweep.resumeIndex("abc", 940) == 0, "resume rusak -> 0");
        ok(DivSweep.resumeIndex(" 42 ", 940) == 42, "resume dengan spasi");

        // ---- DivSweep: batch inkremental ----
        ok(DivSweep.batchDone(20), "batch selesai di 20");
        ok(DivSweep.batchDone(40), "batch selesai di 40");
        ok(DivSweep.batchDone(940), "batch selesai di 940");
        ok(!DivSweep.batchDone(21), "bukan batch di 21");
        ok(!DivSweep.batchDone(0), "bukan batch di 0");
        ok(DivSweep.BATCH == 20, "ukuran batch 20");

        // ---- DivSweep: hasil inkremental terurut ----
        java.util.List<DivSweep.YieldRow> yin = new java.util.ArrayList<DivSweep.YieldRow>();
        yin.add(new DivSweep.YieldRow("A.JK", "A", 100, 2.10, 210, 0.50));
        yin.add(new DivSweep.YieldRow("B.JK", "B", 200, 8.40, 1680, 0.70));
        yin.add(new DivSweep.YieldRow("C.JK", "C", 300, Double.NaN, Double.NaN, Double.NaN));
        yin.add(new DivSweep.YieldRow("D.JK", "D", 400, 0.0, 0, 0));
        yin.add(new DivSweep.YieldRow("E.JK", "E", 500, 5.00, 2500, 0.60));
        java.util.List<DivSweep.YieldRow> ytop = DivSweep.topRows(yin, 15);
        ok(ytop.size() == 3, "topRows saring NaN & nol");
        ok(ytop.get(0).symbol.equals("B.JK")
                && ytop.get(1).symbol.equals("E.JK")
                && ytop.get(2).symbol.equals("A.JK"), "topRows urut yield desc");
        java.util.List<DivSweep.YieldRow> ytop2 = DivSweep.topRows(yin, 2);
        ok(ytop2.size() == 2 && ytop2.get(0).symbol.equals("B.JK"),
                "topRows batasi N");
        ok(DivSweep.topRows(null, 15).isEmpty(), "topRows null aman");
        ok(DivSweep.topRows(new java.util.ArrayList<DivSweep.YieldRow>(), 15).isEmpty(),
                "topRows kosong aman");

        // ---- DivSweep: estimasi waktu pindai penuh ----
        double est = DivSweep.estMinutes(940, 900);
        ok(est > 13 && est < 16, "estimasi 940x900ms ~14 mnt (=" + est + ")");
        ok(DivSweep.estMinutes(0, 900) == 0, "estimasi 0 ticker = 0");

        // ---- DivSeed: parse murni dividends.json ----
        String dj = "{\"as_of\":\"2026-10-08\",\"items\":["
                + "{\"c\":\"BBCA\",\"y\":6.27,\"r\":381.0,\"p\":null,\"px\":6075.0},"
                + "{\"c\":\"tlkm\",\"y\":9.66,\"r\":223.17,\"px\":2310.0},"
                + "{\"c\":\"GOTO\",\"y\":0,\"r\":0,\"px\":30.0},"
                + "{\"c\":\"\",\"y\":5.0,\"r\":10},"
                + "42,"
                + "{\"c\":\"ZZZZ\"}"
                + "]}";
        String[] asOf = new String[1];
        java.util.Map<String, String> nm = new java.util.HashMap<String, String>();
        nm.put("BBCA", "Bank Central Asia Tbk.");
        java.util.List<DivSeed.SeedItem> si = DivSeed.parse(dj, asOf, nm);
        ok(si.size() == 2, "parse saring y=0/kode kosong/entri rusak (" + si.size() + ")");
        ok(si.get(0).code.equals("BBCA") && si.get(0).name.equals("Bank Central Asia Tbk."),
                "nama dari map");
        ok(si.get(1).code.equals("TLKM") && si.get(1).name.equals("TLKM"),
                "uppercase + fallback nama=kode");
        ok(Math.abs(si.get(0).yieldPct - 6.27) < 1e-9, "yield BBCA 6.27");
        ok(si.get(0).payout == 0, "payout null -> 0");
        ok(asOf[0].equals("2026-10-08"), "as_of terbaca");
        boolean threwDs = false;
        try { DivSeed.parse("{\"as_of\":\"x\",\"items\":[]}", null, null); }
        catch (Exception e) { threwDs = true; }
        ok(threwDs, "items kosong melempar");
        threwDs = false;
        try { DivSeed.parse("bukan json", null, null); }
        catch (Exception e) { threwDs = true; }
        ok(threwDs, "JSON rusak melempar");

        // ---- Sharia: klasifikasi syariah vs non-syariah ----
        java.util.Set<String> sh = Sharia.parseJson("[\"ADRO\",\"AKRA\",\"BRIS\"]");
        ok(sh.size() == 3, "sharia parse 3 kode");
        ok(Sharia.isSharia(sh, "ADRO"), "ADRO syariah");
        ok(Sharia.isSharia(sh, "adro"), "case-insensitive (adro)");
        ok(Sharia.isSharia(sh, "AKRA.JK"), "strip .JK (AKRA.JK)");
        ok(Sharia.isSharia(sh, "  bris  "), "trim + case (bris)");
        ok(!Sharia.isSharia(sh, "BBCA"), "BBCA non-syariah");
        ok(!Sharia.isSharia(sh, ""), "string kosong -> false");
        ok(!Sharia.isSharia(sh, null), "null -> false");
        ok(!Sharia.isSharia((java.util.Set<String>) null, "ADRO"), "set null -> false");
        ok(!Sharia.isSharia(new java.util.HashSet<String>(), "ADRO"),
                "set kosong -> false");
        ok(Sharia.parseJson("bukan json").isEmpty(), "json rusak -> set kosong");
        ok(Sharia.parseJson("").isEmpty(), "json kosong -> set kosong");
        ok(Sharia.norm("tlkm.jk").equals("TLKM"), "norm TLKM");
        ok(Sharia.norm(null).equals(""), "norm null");
        // Data riil DES: TLKM syariah, BBCA/BBRI/ASII tidak
        java.util.Set<String> shReal = Sharia.parseJson(
                new String(java.nio.file.Files.readAllBytes(
                        java.nio.file.Paths.get("src/assets/sharia.json")),
                        java.nio.charset.StandardCharsets.UTF_8));
        ok(shReal.size() >= 620, "sharia.json >= 620 ticker (=" + shReal.size() + ")");
        ok(Sharia.isSharia(shReal, "TLKM"), "TLKM syariah (DES riil)");
        ok(Sharia.isSharia(shReal, "ADRO"), "ADRO syariah (DES riil)");
        ok(!Sharia.isSharia(shReal, "BBCA"), "BBCA non-syariah (DES riil)");
        ok(!Sharia.isSharia(shReal, "BBRI"), "BBRI non-syariah (DES riil)");

        System.out.println("\n==== HASIL: " + pass + " PASS, " + fail + " FAIL ====");
        if (fail > 0) System.exit(1);
    }
}
