package com.flexurio.bursaku;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mesin analisa teknikal rule-based: 13 indikator dievaluasi menjadi
 * skor +1 (bullish) / -1 (bearish) / 0 (netral) beserta ALASAN
 * Bahasa Indonesia yang memuat nilai aktual.
 *
 * Verdict: total >= +3 -> BELI, <= -3 -> JUAL, sisanya TAHAN.
 * Murni (tanpa Android) agar bisa di-unit-test di JVM.
 */
public class SignalEngine {

    public static class Signal {
        public final String indicator; // "RSI(14)"
        public final String value;     // "28,4"
        public final int score;        // +1 / -1 / 0
        public final String reason;    // alasan dgn nilai aktual
        Signal(String indicator, String value, int score, String reason) {
            this.indicator = indicator;
            this.value = value;
            this.score = score;
            this.reason = reason;
        }
    }

    public static class Result {
        public final List<Signal> signals = new ArrayList<Signal>();
        public int total = 0;

        public String verdict() {
            if (total >= 3) return "BELI";
            if (total <= -3) return "JUAL";
            return "TAHAN";
        }

        /** Urutan tampil: bullish dulu, lalu bearish, lalu netral. */
        public List<Signal> sorted() {
            List<Signal> b = new ArrayList<Signal>();
            List<Signal> s = new ArrayList<Signal>();
            List<Signal> n = new ArrayList<Signal>();
            for (Signal x : signals) {
                if (x.score > 0) b.add(x);
                else if (x.score < 0) s.add(x);
                else n.add(x);
            }
            List<Signal> out = new ArrayList<Signal>();
            out.addAll(b); out.addAll(s); out.addAll(n);
            return out;
        }
    }

    static String f1(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(Locale.GERMANY, "%,.1f", v);
    }

    private static double last(double[] a) { return a[a.length - 1]; }
    private static double prev(double[] a) { return a[a.length - 2]; }

    // ================= aturan per indikator (testable) =================

    static Signal rsiSignal(double[] rsi) {
        double r = last(rsi), p = prev(rsi);
        if (Double.isNaN(r)) return new Signal("RSI(14)", "—", 0, "RSI(14): — (data belum cukup)");
        if (r < 30) return new Signal("RSI(14)", f1(r), 1,
                "RSI " + f1(r) + " (oversold) — harga berpotensi rebound");
        if (r > 70) return new Signal("RSI(14)", f1(r), -1,
                "RSI " + f1(r) + " (overbought) — rawan koreksi");
        if (r >= 50 && r <= 70 && !Double.isNaN(p) && r > p)
            return new Signal("RSI(14)", f1(r), 1,
                    "RSI " + f1(r) + " di zona bullish dan menguat — momentum beli terjaga");
        if (r >= 30 && r < 50 && !Double.isNaN(p) && r < p)
            return new Signal("RSI(14)", f1(r), -1,
                    "RSI " + f1(r) + " di bawah 50 dan melemah — tekanan jual");
        return new Signal("RSI(14)", f1(r), 0, "RSI " + f1(r) + " — netral");
    }

    static Signal maTrendSignal(double price, double ma20, double ma50) {
        if (Double.isNaN(price) || Double.isNaN(ma20) || Double.isNaN(ma50))
            return new Signal("Tren MA", "—", 0, "Tren MA: — (data belum cukup)");
        if (price > ma20 && ma20 > ma50)
            return new Signal("Tren MA", f1(price), 1,
                    "Harga di atas MA20 & MA50 — tren jangka pendek-menengah bullish");
        if (price < ma20 && ma20 < ma50)
            return new Signal("Tren MA", f1(price), -1,
                    "Harga di bawah MA20 & MA50 — tren melemah");
        return new Signal("Tren MA", f1(price), 0,
                "Harga di antara MA20/MA50 — tren campur aduk");
    }

    static Signal crossSignal(double[] ma50, double[] ma200) {
        if (ma200 == null || Double.isNaN(last(ma200)) || Double.isNaN(prev(ma200)))
            return new Signal("Cross MA50/200", "—", 0,
                    "Cross MA50/200: — (butuh ±200 hari data)");
        if (Indicators.crossUp(ma50, ma200))
            return new Signal("Cross MA50/200", "GC", 1,
                    "Golden Cross: MA50 baru saja memotong MA200 ke atas — tren jangka panjang membaik");
        if (Indicators.crossDown(ma50, ma200))
            return new Signal("Cross MA50/200", "DC", -1,
                    "Death Cross: MA50 baru saja memotong MA200 ke bawah — tren jangka panjang memburuk");
        return new Signal("Cross MA50/200", "—", 0,
                "Tidak ada cross MA50/MA200 baru-baru ini");
    }

    static Signal macdSignal(double line, double signal) {
        if (Double.isNaN(line) || Double.isNaN(signal))
            return new Signal("MACD", "—", 0, "MACD: — (data belum cukup)");
        if (line > signal) return new Signal("MACD", f1(line), 1,
                "MACD di atas garis sinyal — momentum menguat");
        return new Signal("MACD", f1(line), -1,
                "MACD di bawah garis sinyal — momentum melemah");
    }

    static Signal bbSignal(double price, double upper, double lower) {
        if (Double.isNaN(price) || Double.isNaN(upper) || Double.isNaN(lower))
            return new Signal("Bollinger", "—", 0, "Bollinger: — (data belum cukup)");
        if (price <= lower) return new Signal("Bollinger", f1(price), 1,
                "Harga menyentuh lower band (" + f1(lower) + ") — potensi rebound");
        if (price >= upper) return new Signal("Bollinger", f1(price), -1,
                "Harga menyentuh upper band (" + f1(upper) + ") — rawan jenuh beli");
        return new Signal("Bollinger", f1(price), 0, "Harga di dalam Bollinger Bands");
    }

    static Signal stochSignal(double k, double d) {
        if (Double.isNaN(k) || Double.isNaN(d))
            return new Signal("Stochastic", "—", 0, "Stochastic: — (data belum cukup)");
        if (k > d && k < 30) return new Signal("Stochastic", f1(k), 1,
                "Stochastic %K (" + f1(k) + ") memotong %D ke atas dari area oversold — sinyal beli");
        if (k < d && k > 80) return new Signal("Stochastic", f1(k), -1,
                "Stochastic %K (" + f1(k) + ") memotong %D ke bawah dari area overbought — sinyal jual");
        return new Signal("Stochastic", f1(k), 0,
                "Stochastic %K " + f1(k) + " — netral");
    }

    static Signal adxSignal(double adx, double price, double ma50) {
        if (Double.isNaN(adx))
            return new Signal("ADX(14)", "—", 0, "ADX: — (data belum cukup)");
        if (adx > 25 && !Double.isNaN(price) && !Double.isNaN(ma50) && price > ma50)
            return new Signal("ADX(14)", f1(adx), 1,
                    "ADX " + f1(adx) + " (tren kuat) & harga di atas MA50 — tren bullish solid");
        if (adx < 20)
            return new Signal("ADX(14)", f1(adx), 0,
                    "ADX " + f1(adx) + " — tren lemah, sinyal kurang valid");
        return new Signal("ADX(14)", f1(adx), 0, "ADX " + f1(adx) + " — netral");
    }

    static Signal obvSignal(double[] obv) {
        if (obv.length < 6 || Double.isNaN(last(obv)) || Double.isNaN(obv[obv.length - 6]))
            return new Signal("OBV", "—", 0, "OBV: — (data belum cukup)");
        double now = last(obv), past = obv[obv.length - 6];
        if (now > past) return new Signal("OBV", "naik", 1, "OBV naik — ada akumulasi");
        if (now < past) return new Signal("OBV", "turun", -1, "OBV turun — ada distribusi");
        return new Signal("OBV", "datar", 0, "OBV datar");
    }

    static Signal willrSignal(double w) {
        if (Double.isNaN(w))
            return new Signal("Williams %R", "—", 0, "Williams %R: — (data belum cukup)");
        if (w < -80) return new Signal("Williams %R", f1(w), 1,
                "Williams %R " + f1(w) + " (oversold) — potensi rebound");
        if (w > -20) return new Signal("Williams %R", f1(w), -1,
                "Williams %R " + f1(w) + " (overbought) — rawan koreksi");
        return new Signal("Williams %R", f1(w), 0, "Williams %R " + f1(w) + " — netral");
    }

    static Signal cciSignal(double c) {
        if (Double.isNaN(c))
            return new Signal("CCI(20)", "—", 0, "CCI: — (data belum cukup)");
        if (c > 100) return new Signal("CCI(20)", f1(c), 1,
                "CCI " + f1(c) + " — momentum bullish kuat");
        if (c < -100) return new Signal("CCI(20)", f1(c), -1,
                "CCI " + f1(c) + " — momentum bearish");
        return new Signal("CCI(20)", f1(c), 0, "CCI " + f1(c) + " — netral");
    }

    static Signal emaSignal(double price, double ema20) {
        if (Double.isNaN(price) || Double.isNaN(ema20))
            return new Signal("EMA20", "—", 0, "EMA20: — (data belum cukup)");
        if (price > ema20) return new Signal("EMA20", f1(ema20), 1,
                "Harga di atas EMA20 — tren pendek bullish");
        return new Signal("EMA20", f1(ema20), -1,
                "Harga di bawah EMA20 — tren pendek bearish");
    }

    static Signal ichimokuSignal(double price, double spanA, double spanB) {
        if (Double.isNaN(price) || Double.isNaN(spanA) || Double.isNaN(spanB))
            return new Signal("Ichimoku", "—", 0, "Ichimoku: — (data belum cukup)");
        double top = Math.max(spanA, spanB), bot = Math.min(spanA, spanB);
        if (price > top) return new Signal("Ichimoku", f1(price), 1,
                "Harga di atas awan Ichimoku — tren bullish");
        if (price < bot) return new Signal("Ichimoku", f1(price), -1,
                "Harga di bawah awan Ichimoku — tren bearish");
        return new Signal("Ichimoku", f1(price), 0,
                "Harga di dalam awan Ichimoku — tren tidak jelas");
    }

    static Signal sarSignal(double price, double sar) {
        if (Double.isNaN(price) || Double.isNaN(sar))
            return new Signal("Parabolic SAR", "—", 0, "Parabolic SAR: — (data belum cukup)");
        if (price > sar) return new Signal("Parabolic SAR", f1(sar), 1,
                "Harga di atas Parabolic SAR — tren naik");
        return new Signal("Parabolic SAR", f1(sar), -1,
                "Harga di bawah Parabolic SAR — tren turun");
    }

    // ================= analisa penuh =================

    /**
     * Analisa satu emiten dari daftar candle harian (disarankan >= 60 bar;
     * MA200 butuh ±200 bar — tanpanya sinyal cross dinetralkan).
     */
    public static Result analyze(List<Candle> candles) {
        Result r = new Result();
        if (candles == null || candles.size() < 30) return r;
        int n = candles.size();
        double[] c = new double[n], h = new double[n], l = new double[n];
        long[] v = new long[n];
        for (int i = 0; i < n; i++) {
            Candle k = candles.get(i);
            c[i] = k.close; h[i] = k.high; l[i] = k.low; v[i] = k.volume;
        }
        double price = c[n - 1];

        double[] rsi = Indicators.rsi(c, 14);
        double[] ma20 = Indicators.sma(c, 20);
        double[] ma50 = Indicators.sma(c, 50);
        double[] ma200 = n >= 200 ? Indicators.sma(c, 200) : new double[n];
        double[] ema20 = Indicators.ema(c, 20);
        Indicators.MacdResult macd = Indicators.macd(c, 12, 26, 9);
        Indicators.BollingerResult bb = Indicators.bollinger(c, 20, 2);
        Indicators.StochResult st = Indicators.stochastic(h, l, c, 14, 3);
        Indicators.AdxResult ax = Indicators.adx(h, l, c, 14);
        double[] obv = Indicators.obv(c, v);
        double[] wr = Indicators.williamsR(h, l, c, 14);
        double[] cci = Indicators.cci(h, l, c, 20);
        Indicators.IchimokuResult ich = Indicators.ichimoku(h, l, c, 9, 26, 52);
        double[] sar = Indicators.parabolicSar(h, l);

        add(r, rsiSignal(rsi));
        add(r, maTrendSignal(price, last(ma20), last(ma50)));
        add(r, crossSignal(ma50, ma200));
        add(r, macdSignal(last(macd.macd), last(macd.signal)));
        add(r, bbSignal(price, last(bb.upper), last(bb.lower)));
        add(r, stochSignal(last(st.k), last(st.d)));
        add(r, adxSignal(last(ax.adx), price, last(ma50)));
        add(r, obvSignal(obv));
        add(r, willrSignal(last(wr)));
        add(r, cciSignal(last(cci)));
        add(r, emaSignal(price, last(ema20)));
        add(r, ichimokuSignal(price, last(ich.senkouA), last(ich.senkouB)));
        add(r, sarSignal(price, last(sar)));
        return r;
    }

    private static void add(Result r, Signal s) {
        r.signals.add(s);
        r.total += s.score;
    }
}
