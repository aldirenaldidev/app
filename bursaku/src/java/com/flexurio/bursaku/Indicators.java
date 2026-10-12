package com.flexurio.bursaku;

import java.util.ArrayList;
import java.util.List;

/**
 * Perhitungan indikator teknikal — murni Java, tanpa dependensi Android,
 * sehingga bisa di-unit-test di JVM biasa.
 *
 * Konvensi: semua fungsi mengembalikan array double sepanjang input.
 * Elemen yang belum bisa dihitung diisi Double.NaN.
 */
public class Indicators {

    /** Simple Moving Average. */
    public static double[] sma(double[] v, int period) {
        double[] out = new double[v.length];
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i])) { out[i] = Double.NaN; continue; }
            sum += v[i];
            if (i >= period) sum -= v[i - period];
            out[i] = (i >= period - 1) ? sum / period : Double.NaN;
        }
        return out;
    }

    /** Exponential Moving Average. Seed = SMA dari `period` data pertama. */
    public static double[] ema(double[] v, int period) {
        double[] out = new double[v.length];
        double k = 2.0 / (period + 1);
        double prev = Double.NaN;
        double sum = 0;
        int count = 0;
        for (int i = 0; i < v.length; i++) {
            if (Double.isNaN(v[i])) { out[i] = Double.NaN; continue; }
            count++;
            sum += v[i];
            if (count < period) { out[i] = Double.NaN; continue; }
            if (count == period) {
                prev = sum / period;   // seed = SMA
            } else {
                prev = v[i] * k + prev * (1 - k);
            }
            out[i] = prev;
        }
        return out;
    }

    /** RSI Wilder (default 14). Nilai 0..100. */
    public static double[] rsi(double[] close, int period) {
        double[] out = new double[close.length];
        for (int i = 0; i < out.length; i++) out[i] = Double.NaN;
        if (close.length <= period) return out;
        double gain = 0, loss = 0;
        for (int i = 1; i <= period; i++) {
            double chg = close[i] - close[i - 1];
            if (chg > 0) gain += chg; else loss -= chg;
        }
        gain /= period;
        loss /= period;
        out[period] = loss == 0 ? 100.0 : 100.0 - (100.0 / (1 + gain / loss));
        for (int i = period + 1; i < close.length; i++) {
            double chg = close[i] - close[i - 1];
            double g = chg > 0 ? chg : 0;
            double l = chg < 0 ? -chg : 0;
            gain = (gain * (period - 1) + g) / period;   // Wilder smoothing
            loss = (loss * (period - 1) + l) / period;
            out[i] = loss == 0 ? 100.0 : 100.0 - (100.0 / (1 + gain / loss));
        }
        return out;
    }

    /** Hasil MACD. */
    public static class MacdResult {
        public double[] macd;    // garis MACD (emaFast - emaSlow)
        public double[] signal;  // EMA-9 dari garis MACD
        public double[] hist;    // macd - signal
    }

    /** MACD(fast=12, slow=26, signal=9). */
    public static MacdResult macd(double[] close, int fast, int slow, int signalP) {
        MacdResult r = new MacdResult();
        double[] ef = ema(close, fast);
        double[] es = ema(close, slow);
        double[] m = new double[close.length];
        for (int i = 0; i < m.length; i++) {
            m[i] = (Double.isNaN(ef[i]) || Double.isNaN(es[i])) ? Double.NaN : ef[i] - es[i];
        }
        double[] s = emaSkipNaN(m, signalP);
        double[] h = new double[close.length];
        for (int i = 0; i < h.length; i++) {
            h[i] = (Double.isNaN(m[i]) || Double.isNaN(s[i])) ? Double.NaN : m[i] - s[i];
        }
        r.macd = m; r.signal = s; r.hist = h;
        return r;
    }

    /** EMA yang melewati NaN awal (untuk deret turunan seperti garis MACD). */
    static double[] emaSkipNaN(double[] v, int period) {
        double[] out = new double[v.length];
        for (int i = 0; i < out.length; i++) out[i] = Double.NaN;
        List<Double> valid = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < v.length; i++) {
            if (!Double.isNaN(v[i])) { valid.add(v[i]); idx.add(i); }
        }
        if (valid.size() < period) return out;
        double[] vv = new double[valid.size()];
        for (int i = 0; i < vv.length; i++) vv[i] = valid.get(i);
        double[] e = ema(vv, period);
        for (int i = 0; i < e.length; i++) out[idx.get(i)] = e[i];
        return out;
    }

    /** Hasil Bollinger Bands. */
    public static class BollingerResult {
        public double[] middle;
        public double[] upper;
        public double[] lower;
    }

    /** Bollinger Bands (default 20, 2). */
    public static BollingerResult bollinger(double[] close, int period, double mult) {
        BollingerResult r = new BollingerResult();
        r.middle = sma(close, period);
        r.upper = new double[close.length];
        r.lower = new double[close.length];
        for (int i = 0; i < close.length; i++) {
            if (Double.isNaN(r.middle[i])) {
                r.upper[i] = Double.NaN; r.lower[i] = Double.NaN; continue;
            }
            double sum = 0;
            for (int j = i - period + 1; j <= i; j++) {
                double d = close[j] - r.middle[i];
                sum += d * d;
            }
            double sd = Math.sqrt(sum / period);
            r.upper[i] = r.middle[i] + mult * sd;
            r.lower[i] = r.middle[i] - mult * sd;
        }
        return r;
    }

    // ---------- osilator tambahan ----------

    /** Hasil Stochastic. */
    public static class StochResult {
        public double[] k;      // %K cepat
        public double[] d;      // %D = SMA(%K, slowing)
        public double[] slowD;  // SMA(%D, slowing)
    }

    /** Stochastic (kPeriod=14, slowing=3). Nilai 0..100. */
    public static StochResult stochastic(double[] high, double[] low, double[] close,
                                         int kPeriod, int slowing) {
        int n = close.length;
        StochResult r = new StochResult();
        r.k = new double[n]; r.d = new double[n]; r.slowD = new double[n];
        for (int i = 0; i < n; i++) { r.k[i] = Double.NaN; r.d[i] = Double.NaN; r.slowD[i] = Double.NaN; }
        for (int i = kPeriod - 1; i < n; i++) {
            double hh = high[i], ll = low[i];
            for (int j = i - kPeriod + 1; j <= i; j++) {
                if (high[j] > hh) hh = high[j];
                if (low[j] < ll) ll = low[j];
            }
            r.k[i] = (hh == ll) ? 50.0 : 100.0 * (close[i] - ll) / (hh - ll);
        }
        r.d = smaSkipNaN(r.k, slowing);
        r.slowD = smaSkipNaN(r.d, slowing);
        return r;
    }

    /** SMA yang melewati NaN (untuk deret turunan). */
    static double[] smaSkipNaN(double[] v, int period) {
        double[] out = new double[v.length];
        for (int i = 0; i < out.length; i++) out[i] = Double.NaN;
        List<Double> valid = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < v.length; i++) {
            if (!Double.isNaN(v[i])) { valid.add(v[i]); idx.add(i); }
        }
        if (valid.size() < period) return out;
        double[] vv = new double[valid.size()];
        for (int i = 0; i < vv.length; i++) vv[i] = valid.get(i);
        double[] s = sma(vv, period);
        for (int i = 0; i < s.length; i++) out[idx.get(i)] = s[i];
        return out;
    }

    /** True Range per bar. */
    static double[] trueRange(double[] high, double[] low, double[] close) {
        int n = close.length;
        double[] tr = new double[n];
        tr[0] = high[0] - low[0];
        for (int i = 1; i < n; i++) {
            double a = high[i] - low[i];
            double b = Math.abs(high[i] - close[i - 1]);
            double c = Math.abs(low[i] - close[i - 1]);
            tr[i] = Math.max(a, Math.max(b, c));
        }
        return tr;
    }

    /** Wilder smoothing (dipakai ATR & ADX). */
    static double[] wilder(double[] v, int period) {
        int n = v.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = Double.NaN;
        if (n < period) return out;
        double sum = 0;
        for (int i = 0; i < period; i++) sum += v[i];
        out[period - 1] = sum / period;
        for (int i = period; i < n; i++) {
            out[i] = (out[i - 1] * (period - 1) + v[i]) / period;
        }
        return out;
    }

    /** ATR Wilder (default 14). */
    public static double[] atr(double[] high, double[] low, double[] close, int period) {
        return wilder(trueRange(high, low, close), period);
    }

    /** CCI (default 20). */
    public static double[] cci(double[] high, double[] low, double[] close, int period) {
        int n = close.length;
        double[] tp = new double[n];
        for (int i = 0; i < n; i++) tp[i] = (high[i] + low[i] + close[i]) / 3.0;
        double[] ma = sma(tp, period);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            if (Double.isNaN(ma[i])) { out[i] = Double.NaN; continue; }
            double md = 0;
            for (int j = i - period + 1; j <= i; j++) md += Math.abs(tp[j] - ma[i]);
            md /= period;
            out[i] = (md == 0) ? 0 : (tp[i] - ma[i]) / (0.015 * md);
        }
        return out;
    }

    /** Williams %R (default 14). Rentang -100..0. */
    public static double[] williamsR(double[] high, double[] low, double[] close, int period) {
        int n = close.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = Double.NaN;
        for (int i = period - 1; i < n; i++) {
            double hh = high[i], ll = low[i];
            for (int j = i - period + 1; j <= i; j++) {
                if (high[j] > hh) hh = high[j];
                if (low[j] < ll) ll = low[j];
            }
            out[i] = (hh == ll) ? 0 : -100.0 * (hh - close[i]) / (hh - ll);
        }
        return out;
    }

    /** On-Balance Volume. */
    public static double[] obv(double[] close, long[] volume) {
        int n = close.length;
        double[] out = new double[n];
        out[0] = volume[0];
        for (int i = 1; i < n; i++) {
            if (close[i] > close[i - 1]) out[i] = out[i - 1] + volume[i];
            else if (close[i] < close[i - 1]) out[i] = out[i - 1] - volume[i];
            else out[i] = out[i - 1];
        }
        return out;
    }

    /** Hasil ADX. */
    public static class AdxResult {
        public double[] adx;
        public double[] plusDI;
        public double[] minusDI;
    }

    /** ADX Wilder (default 14). */
    public static AdxResult adx(double[] high, double[] low, double[] close, int period) {
        int n = close.length;
        AdxResult r = new AdxResult();
        r.adx = new double[n]; r.plusDI = new double[n]; r.minusDI = new double[n];
        for (int i = 0; i < n; i++) { r.adx[i] = Double.NaN; r.plusDI[i] = Double.NaN; r.minusDI[i] = Double.NaN; }
        if (n <= period) return r;
        double[] pDM = new double[n], mDM = new double[n];
        for (int i = 1; i < n; i++) {
            double up = high[i] - high[i - 1];
            double dn = low[i - 1] - low[i];
            pDM[i] = (up > dn && up > 0) ? up : 0;
            mDM[i] = (dn > up && dn > 0) ? dn : 0;
        }
        double[] sP = wilder(pDM, period), sM = wilder(mDM, period);
        double[] sTR = wilder(trueRange(high, low, close), period);
        double[] dx = new double[n];
        for (int i = 0; i < n; i++) dx[i] = Double.NaN;
        for (int i = 0; i < n; i++) {
            if (Double.isNaN(sTR[i]) || sTR[i] == 0) continue;
            r.plusDI[i] = 100.0 * sP[i] / sTR[i];
            r.minusDI[i] = 100.0 * sM[i] / sTR[i];
            double sum = r.plusDI[i] + r.minusDI[i];
            dx[i] = (sum == 0) ? 0 : 100.0 * Math.abs(r.plusDI[i] - r.minusDI[i]) / sum;
        }
        // ADX = Wilder smoothing dari DX, seed = SMA DX pertama
        List<Double> vd = new ArrayList<>();
        List<Integer> id = new ArrayList<>();
        for (int i = 0; i < n; i++) if (!Double.isNaN(dx[i])) { vd.add(dx[i]); id.add(i); }
        if (vd.size() >= period) {
            double[] va = new double[vd.size()];
            for (int i = 0; i < va.length; i++) va[i] = vd.get(i);
            double[] wa = wilder(va, period);
            for (int i = 0; i < wa.length; i++) r.adx[id.get(i)] = wa[i];
        }
        return r;
    }

    // ---------- overlay tambahan ----------

    /** Hasil Ichimoku. Nilai senkou di bar i ditujukan untuk bar i+displacement. */
    public static class IchimokuResult {
        public double[] tenkan;   // 9
        public double[] kijun;    // 26
        public double[] senkouA;  // (tenkan+kijun)/2 -> depan 26
        public double[] senkouB;  // (HH52+LL52)/2 -> depan 26
        public double[] chikou;   // close -> belakang 26
    }

    /** Ichimoku (9, 26, 52). */
    public static IchimokuResult ichimoku(double[] high, double[] low, double[] close,
                                          int t, int k, int s) {
        int n = close.length;
        IchimokuResult r = new IchimokuResult();
        r.tenkan = midLine(high, low, t);
        r.kijun = midLine(high, low, k);
        double[] mid52 = midLine(high, low, s);
        r.senkouA = new double[n]; r.senkouB = new double[n]; r.chikou = new double[n];
        for (int i = 0; i < n; i++) {
            r.senkouA[i] = (Double.isNaN(r.tenkan[i]) || Double.isNaN(r.kijun[i]))
                    ? Double.NaN : (r.tenkan[i] + r.kijun[i]) / 2.0;
            r.senkouB[i] = mid52[i];
            r.chikou[i] = close[i];
        }
        return r;
    }

    private static double[] midLine(double[] high, double[] low, int period) {
        int n = high.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = Double.NaN;
        for (int i = period - 1; i < n; i++) {
            double hh = high[i], ll = low[i];
            for (int j = i - period + 1; j <= i; j++) {
                if (high[j] > hh) hh = high[j];
                if (low[j] < ll) ll = low[j];
            }
            out[i] = (hh + ll) / 2.0;
        }
        return out;
    }

    /** Parabolic SAR (step 0.02, maks 0.2). */
    public static double[] parabolicSar(double[] high, double[] low) {
        int n = high.length;
        double[] sar = new double[n];
        if (n == 0) return sar;
        boolean up = true;
        sar[0] = low[0];
        double ep = high[0];
        double af = 0.02;
        final double STEP = 0.02, MAX = 0.2;
        for (int i = 1; i < n; i++) {
            sar[i] = sar[i - 1] + af * (ep - sar[i - 1]);
            if (up) {
                if (i >= 2 && sar[i] > low[i - 2]) sar[i] = low[i - 2];
                if (sar[i] > low[i - 1]) sar[i] = low[i - 1];
                if (low[i] < sar[i]) {          // reversal ke bawah
                    up = false; sar[i] = ep; ep = low[i]; af = STEP;
                } else {
                    if (high[i] > ep) { ep = high[i]; af = Math.min(af + STEP, MAX); }
                }
            } else {
                if (i >= 2 && sar[i] < high[i - 2]) sar[i] = high[i - 2];
                if (sar[i] < high[i - 1]) sar[i] = high[i - 1];
                if (high[i] > sar[i]) {         // reversal ke atas
                    up = true; sar[i] = ep; ep = high[i]; af = STEP;
                } else {
                    if (low[i] < ep) { ep = low[i]; af = Math.min(af + STEP, MAX); }
                }
            }
        }
        return sar;
    }

    // ---------- sinyal ----------

    /** true bila deret a memotong ke ATAS deret b pada bar terakhir (golden cross). */
    public static boolean crossUp(double[] a, double[] b) {
        int n = a.length;
        if (n < 2 || b.length != n) return false;
        double a1 = a[n - 2], a2 = a[n - 1], b1 = b[n - 2], b2 = b[n - 1];
        if (Double.isNaN(a1) || Double.isNaN(a2) || Double.isNaN(b1) || Double.isNaN(b2)) return false;
        return a1 <= b1 && a2 > b2;
    }

    /** true bila deret a memotong ke BAWAH deret b pada bar terakhir (death cross). */
    public static boolean crossDown(double[] a, double[] b) {
        int n = a.length;
        if (n < 2 || b.length != n) return false;
        double a1 = a[n - 2], a2 = a[n - 1], b1 = b[n - 2], b2 = b[n - 1];
        if (Double.isNaN(a1) || Double.isNaN(a2) || Double.isNaN(b1) || Double.isNaN(b2)) return false;
        return a1 >= b1 && a2 < b2;
    }

    /** Nilai valid terakhir dari deret (melewati NaN di ujung). */
    public static double lastValid(double[] v) {
        for (int i = v.length - 1; i >= 0; i--) {
            if (!Double.isNaN(v[i])) return v[i];
        }
        return Double.NaN;
    }
}
