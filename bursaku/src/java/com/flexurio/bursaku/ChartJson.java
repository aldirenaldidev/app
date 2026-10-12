package com.flexurio.bursaku;

import java.util.List;
import java.util.Locale;

/**
 * Membangun payload JSON untuk render(payload) di chart.html.
 * Overlay & osilator dipilih via Sel; satu indikator gagal → dilewati ("—"),
 * tidak menggagalkan chart.
 */
public class ChartJson {

    /** Pilihan indikator dari chips. */
    public static class Sel {
        public boolean ma = true;      // MA20 + MA50
        public boolean ema20 = false;
        public boolean bb = false;     // Bollinger (20,2)
        public boolean ichimoku = false;
        public boolean sar = false;    // Parabolic SAR
        // osilator — maksimal 2 aktif
        public boolean rsi = true;
        public boolean macd = false;
        public boolean stoch = false;
        public boolean atr = false;
        public boolean cci = false;
        public boolean willr = false;
        public boolean obv = false;
        public boolean adx = false;
    }

    public static String build(List<Candle> cs, Sel sel) {
        int n = cs.size();
        double[] o = new double[n], h = new double[n], l = new double[n],
                 c = new double[n];
        long[] t = new long[n], v = new long[n];
        for (int i = 0; i < n; i++) {
            Candle k = cs.get(i);
            t[i] = k.time; o[i] = k.open; h[i] = k.high; l[i] = k.low;
            c[i] = k.close; v[i] = k.volume;
        }

        StringBuilder sb = new StringBuilder(131072);
        sb.append("{\"candles\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"time\":").append(t[i])
              .append(",\"open\":").append(r2(o[i]))
              .append(",\"high\":").append(r2(h[i]))
              .append(",\"low\":").append(r2(l[i]))
              .append(",\"close\":").append(r2(c[i])).append('}');
        }
        sb.append("],\"volume\":[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append("{\"value\":").append(v[i])
              .append(",\"up\":").append(c[i] >= o[i] ? "true" : "false").append('}');
        }

        // ---------- overlays ----------
        sb.append("],\"overlays\":{\"lines\":[");
        boolean first = true;
        try {
            if (sel.ma) {
                first = line(sb, first, t, Indicators.sma(c, 20), "#E84393", 1, 0);
                first = line(sb, first, t, Indicators.sma(c, 50), "#F0B90B", 1, 0);
            }
            if (sel.ema20) first = line(sb, first, t, Indicators.ema(c, 20), "#C084FC", 1, 0);
            if (sel.bb) {
                Indicators.BollingerResult b = Indicators.bollinger(c, 20, 2);
                first = line(sb, first, t, b.upper, "#8B93A7", 1, 2);
                first = line(sb, first, t, b.middle, "#E84393", 1, 2);
                first = line(sb, first, t, b.lower, "#8B93A7", 1, 2);
            }
            if (sel.ichimoku) {
                Indicators.IchimokuResult ic = Indicators.ichimoku(h, l, c, 9, 26, 52);
                first = line(sb, first, t, ic.tenkan, "#E84393", 1, 0);
                first = line(sb, first, t, ic.kijun, "#EA3943", 1, 0);
                first = lineShift(sb, first, t, ic.senkouA, 26, "#16C784", 1);
                first = lineShift(sb, first, t, ic.senkouB, 26, "#EA3943", 1);
                first = lineShift(sb, first, t, ic.chikou, -26, "#8B93A7", 1);
            }
        } catch (Exception e) { /* overlay gagal → lewati */ }
        sb.append("],\"sarMarkers\":[");
        try {
            if (sel.sar) {
                double[] sar = Indicators.parabolicSar(h, l);
                boolean f2 = true;
                for (int i = 0; i < n; i++) {
                    if (Double.isNaN(sar[i])) continue;
                    if (!f2) sb.append(',');
                    f2 = false;
                    sb.append("{\"time\":").append(t[i]).append(",\"position\":\"")
                      .append(sar[i] > c[i] ? "aboveBar" : "belowBar").append("\"}");
                }
            }
        } catch (Exception e) { /* abaikan */ }
        sb.append("]}");

        // ---------- oscillators ----------
        sb.append(",\"oscillators\":[");
        boolean of = true;
        try {
            if (sel.rsi) {
                double[] r = Indicators.rsi(c, 14);
                of = oscStart(sb, of, "RSI (14)", last2(r));
                band(sb, 70, "rgba(234,57,67,0.7)");
                band(sb, 30, "rgba(22,199,132,0.7)");
                sb.append("],\"lines\":[");
                oscLine(sb, t, r, "#A78BFA", 1);
                sb.append("]}");
            }
            if (sel.macd) {
                Indicators.MacdResult m = Indicators.macd(c, 12, 26, 9);
                of = oscStart(sb, of, "MACD (12,26,9)", last2(m.hist));
                sb.append("],\"lines\":[");
                oscLine(sb, t, m.macd, "#E84393", 1);
                sb.append(',');
                oscLine(sb, t, m.signal, "#F0B90B", 1);
                sb.append("],\"hist\":[");
                histData(sb, t, m.hist);
                sb.append("]}");
            }
            if (sel.stoch) {
                Indicators.StochResult s = Indicators.stochastic(h, l, c, 14, 3);
                of = oscStart(sb, of, "Stochastic (14,3,3)", last2(s.slowD));
                band(sb, 80, "rgba(234,57,67,0.7)");
                band(sb, 20, "rgba(22,199,132,0.7)");
                sb.append("],\"lines\":[");
                oscLine(sb, t, s.k, "#E84393", 1);
                sb.append(',');
                oscLine(sb, t, s.slowD, "#F0B90B", 1);
                sb.append("]}");
            }
            if (sel.atr) {
                double[] a = Indicators.atr(h, l, c, 14);
                of = oscStart(sb, of, "ATR (14)", lastRp(a));
                sb.append("],\"lines\":[");
                oscLine(sb, t, a, "#A78BFA", 1);
                sb.append("]}");
            }
            if (sel.cci) {
                double[] x = Indicators.cci(h, l, c, 20);
                of = oscStart(sb, of, "CCI (20)", last2(x));
                band(sb, 100, "rgba(234,57,67,0.7)");
                band(sb, -100, "rgba(22,199,132,0.7)");
                sb.append("],\"lines\":[");
                oscLine(sb, t, x, "#E84393", 1);
                sb.append("]}");
            }
            if (sel.willr) {
                double[] w = Indicators.williamsR(h, l, c, 14);
                of = oscStart(sb, of, "Williams %R (14)", last2(w));
                band(sb, -20, "rgba(234,57,67,0.7)");
                band(sb, -80, "rgba(22,199,132,0.7)");
                sb.append("],\"lines\":[");
                oscLine(sb, t, w, "#F0B90B", 1);
                sb.append("]}");
            }
            if (sel.obv) {
                double[] b = Indicators.obv(c, v);
                of = oscStart(sb, of, "OBV", lastShort(b));
                sb.append("],\"lines\":[");
                oscLine(sb, t, b, "#16C784", 1);
                sb.append("]}");
            }
            if (sel.adx) {
                Indicators.AdxResult a = Indicators.adx(h, l, c, 14);
                of = oscStart(sb, of, "ADX (14)", last2(a.adx));
                band(sb, 25, "rgba(139,147,167,0.6)");
                sb.append("],\"lines\":[");
                oscLine(sb, t, a.adx, "#A78BFA", 1);
                sb.append(',');
                oscLine(sb, t, a.plusDI, "#16C784", 1);
                sb.append(',');
                oscLine(sb, t, a.minusDI, "#EA3943", 1);
                sb.append("]}");
            }
        } catch (Exception e) { /* osilator gagal → lewati */ }
        sb.append("]}");
        return sb.toString();
    }

    // ---------- helpers ----------
    static String r2(double v) {
        if (Double.isNaN(v)) return "null";
        return String.format(Locale.US, "%.2f", v);
    }

    static boolean line(StringBuilder sb, boolean first, long[] t, double[] v,
                        String color, int width, int style) {
        StringBuilder d = new StringBuilder();
        boolean f = true;
        for (int i = 0; i < t.length; i++) {
            if (Double.isNaN(v[i])) continue;
            if (!f) d.append(',');
            f = false;
            d.append("{\"time\":").append(t[i]).append(",\"value\":").append(r2(v[i])).append('}');
        }
        if (f) return first;
        if (!first) sb.append(',');
        sb.append("{\"color\":\"").append(color).append("\",\"width\":").append(width)
          .append(",\"style\":").append(style).append(",\"data\":[").append(d).append("]}");
        return false;
    }

    /** Garis dengan geser waktu (untuk Ichimoku, dalam hari). */
    static boolean lineShift(StringBuilder sb, boolean first, long[] t, double[] v,
                             int shiftDays, String color, int width) {
        long sh = shiftDays * 86400L;
        StringBuilder d = new StringBuilder();
        boolean f = true;
        for (int i = 0; i < t.length; i++) {
            if (Double.isNaN(v[i])) continue;
            if (!f) d.append(',');
            f = false;
            d.append("{\"time\":").append(t[i] + sh).append(",\"value\":").append(r2(v[i])).append('}');
        }
        if (f) return first;
        if (!first) sb.append(',');
        sb.append("{\"color\":\"").append(color).append("\",\"width\":").append(width)
          .append(",\"style\":0,\"data\":[").append(d).append("]}");
        return false;
    }

    static boolean oscStart(StringBuilder sb, boolean first, String name, String last) {
        if (!first) sb.append(',');
        sb.append("{\"name\":\"").append(name).append("\",\"last\":\"").append(last)
          .append("\",\"bands\":[");
        return false;
    }

    static void band(StringBuilder sb, double value, String color) {
        // dipanggil berurutan; koma ditangani pemanggil via flag sederhana:
        // kita selalu mulai bands kosong lalu tambah — gunakan trik: cek char terakhir
        if (sb.charAt(sb.length() - 1) == ']') sb.append(',');
        else if (sb.charAt(sb.length() - 1) == '}') sb.append(',');
        sb.append("{\"value\":").append(r2(value)).append(",\"color\":\"").append(color).append("\"}");
    }

    static void oscLine(StringBuilder sb, long[] t, double[] v, String color, int width) {
        sb.append("{\"color\":\"").append(color).append("\",\"width\":").append(width)
          .append(",\"data\":[");
        boolean f = true;
        for (int i = 0; i < t.length; i++) {
            if (Double.isNaN(v[i])) continue;
            if (!f) sb.append(',');
            f = false;
            sb.append("{\"time\":").append(t[i]).append(",\"value\":").append(r2(v[i])).append('}');
        }
        sb.append("]}");
    }

    static void histData(StringBuilder sb, long[] t, double[] v) {
        boolean f = true;
        for (int i = 0; i < t.length; i++) {
            if (Double.isNaN(v[i])) continue;
            if (!f) sb.append(',');
            f = false;
            sb.append("{\"time\":").append(t[i]).append(",\"value\":").append(r2(v[i])).append('}');
        }
    }

    static String last2(double[] v) {
        double x = Indicators.lastValid(v);
        return Double.isNaN(x) ? "—" : String.format(Locale.GERMANY, "%,.2f", x);
    }

    static String lastRp(double[] v) {
        double x = Indicators.lastValid(v);
        return Double.isNaN(x) ? "—" : Fmt.rp(x);
    }

    static String lastShort(double[] v) {
        double x = Indicators.lastValid(v);
        return Double.isNaN(x) ? "—" : Fmt.vol((long) x);
    }
}
