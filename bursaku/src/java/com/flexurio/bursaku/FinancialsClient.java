package com.flexurio.bursaku;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Client data keuangan via Yahoo Finance quoteSummary (tanpa API key).
 *
 * Terverifikasi 2026-10-07 untuk emiten IDX (BBCA.JK): modul
 * financialData, incomeStatementHistory, defaultKeyStatistics
 * mengembalikan data tidak kosong. Modul earningsHistory kosong
 * untuk IDX → tidak dipakai.
 *
 *   GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{SIMBOL}
 *       ?modules=financialData,incomeStatementHistory,defaultKeyStatistics&crumb=...
 */
public class FinancialsClient {

    /** Satu periode laporan tahunan. */
    public static class YearRow {
        public String endDate = ""; // "2025-12-31"
        public double revenue = Double.NaN;
        public double netIncome = Double.NaN;
    }

    /** Metrik keuangan kunci. Semua fraksi 0..1; NaN bila tak tersedia. */
    public static class FinData {
        public double revenue = Double.NaN;      // TTM
        public double netIncome = Double.NaN;   // netIncomeToCommon
        public double eps = Double.NaN;         // trailing
        public double roe = Double.NaN;
        public double roa = Double.NaN;
        public double profitMargin = Double.NaN;
        public double revenueGrowth = Double.NaN;
        public double earningsGrowth = Double.NaN;
        public double priceToBook = Double.NaN;
        // --- Neraca & arus kas (financialData) ---
        public double totalDebt = Double.NaN;
        public double operatingCashflow = Double.NaN;
        public double freeCashflow = Double.NaN;
        // --- Valuasi (defaultKeyStatistics) ---
        public double bookValue = Double.NaN;       // per saham (Rp)
        public double sharesOutstanding = Double.NaN;
        public double targetMean = Double.NaN;
        public double targetHigh = Double.NaN;
        public double targetLow = Double.NaN;
        public String recommendation = "";          // strong_buy dsb.
        public int analystCount = 0;
        public String fiscalYearEnd = "";           // "2025-12-31"
        public List<YearRow> history = new ArrayList<>(); // tahunan, terbaru dulu
    }

    // ---------- perhitungan client-side (murni, unit-testable) ----------

    /** Estimasi total ekuitas = book value per saham x saham beredar. */
    public static double equityEst(double bookValue, double shares) {
        if (Double.isNaN(bookValue) || Double.isNaN(shares) || shares <= 0) return Double.NaN;
        return bookValue * shares;
    }

    /** DER = total utang / total ekuitas (estimasi). */
    public static double der(double totalDebt, double bookValue, double shares) {
        double eq = equityEst(bookValue, shares);
        if (Double.isNaN(totalDebt) || Double.isNaN(eq) || eq <= 0) return Double.NaN;
        return totalDebt / eq;
    }

    /** P/E = harga kini / EPS trailing. */
    public static double pe(double price, double eps) {
        if (Double.isNaN(price) || Double.isNaN(eps) || eps <= 0) return Double.NaN;
        return price / eps;
    }

    /** P/BV = harga kini / book value per saham. */
    public static double pbv(double price, double bookValue) {
        if (Double.isNaN(price) || Double.isNaN(bookValue) || bookValue <= 0) return Double.NaN;
        return price / bookValue;
    }

    /** Pertumbuhan YoY = (kini - lalu) / |lalu|. */
    public static double yoy(double cur, double prev) {
        if (Double.isNaN(cur) || Double.isNaN(prev) || prev == 0) return Double.NaN;
        return (cur - prev) / Math.abs(prev);
    }

    /** Ambil data keuangan. Melempar Exception bila gagal. */
    public static FinData fetch(String symbol) throws Exception {
        try {
            return doFetch(symbol, YahooCrumb.get());
        } catch (Exception first) {
            YahooCrumb.invalidate();
            return doFetch(symbol, YahooCrumb.get());
        }
    }

    private static FinData doFetch(String symbol, String crumb) throws Exception {
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                + "?modules=financialData,incomeStatementHistory,defaultKeyStatistics"
                + "&crumb=" + crumb;
        return parse(YahooClient.get(url));
    }

    static FinData parse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject qs = root.getJSONObject("quoteSummary");
        if (qs.has("error") && !qs.isNull("error"))
            throw new Exception("Data keuangan tidak tersedia");
        JSONObject res = qs.getJSONArray("result").getJSONObject(0);
        FinData f = new FinData();

        JSONObject fd = res.optJSONObject("financialData");
        if (fd != null) {
            f.revenue = raw(fd, "totalRevenue");
            f.profitMargin = raw(fd, "profitMargins");
            f.roe = raw(fd, "returnOnEquity");
            f.roa = raw(fd, "returnOnAssets");
            f.revenueGrowth = raw(fd, "revenueGrowth");
            f.earningsGrowth = raw(fd, "earningsGrowth");
            f.totalDebt = raw(fd, "totalDebt");
            f.operatingCashflow = raw(fd, "operatingCashflow");
            f.freeCashflow = raw(fd, "freeCashflow");
            f.targetMean = raw(fd, "targetMeanPrice");
            f.targetHigh = raw(fd, "targetHighPrice");
            f.targetLow = raw(fd, "targetLowPrice");
            f.recommendation = fd.optString("recommendationKey", "");
            try { f.analystCount = fd.getInt("numberOfAnalystOpinions"); }
            catch (Exception e) { f.analystCount = 0; }
        }
        JSONObject ks = res.optJSONObject("defaultKeyStatistics");
        if (ks != null) {
            f.eps = raw(ks, "trailingEps");
            f.netIncome = raw(ks, "netIncomeToCommon");
            f.priceToBook = raw(ks, "priceToBook");
            f.bookValue = raw(ks, "bookValue");
            f.sharesOutstanding = raw(ks, "sharesOutstanding");
            if (ks.optJSONObject("lastFiscalYearEnd") != null)
                f.fiscalYearEnd = ks.optJSONObject("lastFiscalYearEnd").optString("fmt", "");
            if (Double.isNaN(f.profitMargin)) f.profitMargin = raw(ks, "profitMargins");
        }
        JSONObject ish = res.optJSONObject("incomeStatementHistory");
        if (ish != null) {
            JSONArray arr = ish.optJSONArray("incomeStatementHistory");
            if (arr != null) {
                for (int i = 0; i < arr.length() && i < 4; i++) {
                    JSONObject h = arr.getJSONObject(i);
                    YearRow y = new YearRow();
                    y.endDate = h.optJSONObject("endDate") != null
                            ? h.optJSONObject("endDate").optString("fmt", "") : "";
                    y.revenue = raw(h, "totalRevenue");
                    y.netIncome = raw(h, "netIncome");
                    f.history.add(y);
                }
            }
        }
        return f;
    }

    private static double raw(JSONObject o, String k) {
        try {
            if (!o.has(k) || o.isNull(k)) return Double.NaN;
            return o.getJSONObject(k).getDouble("raw");
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    /** Format Rp ringkas: 111,86 T / 57,54 M / 472. */
    public static String rpBig(double v) {
        if (Double.isNaN(v)) return "—";
        double a = Math.abs(v);
        String sign = v < 0 ? "-" : "";
        if (a >= 1e12) return sign + "Rp " + String.format(java.util.Locale.GERMANY, "%,.2f T", a / 1e12);
        if (a >= 1e9) return sign + "Rp " + String.format(java.util.Locale.GERMANY, "%,.2f M", a / 1e9);
        if (a >= 1e6) return sign + "Rp " + String.format(java.util.Locale.GERMANY, "%,.1f jt", a / 1e6);
        return sign + "Rp " + Fmt.rp(a);
    }

    /** Sub-kartu 1: Laba Rugi. */
    public static String formatLabaRugi(FinData f) {
        StringBuilder s = new StringBuilder();
        s.append("• Pendapatan (TTM): ").append(rpBig(f.revenue)).append('\n');
        s.append("• Laba bersih: ").append(rpBig(f.netIncome)).append('\n');
        s.append("• EPS (trailing): ").append(Double.isNaN(f.eps) ? "—" : "Rp " + Fmt.rp(f.eps)).append('\n');
        s.append("• ROE: ").append(pct(f.roe)).append("   ROA: ").append(pct(f.roa)).append('\n');
        s.append("• Margin laba bersih: ").append(pct(f.profitMargin));
        if (!f.history.isEmpty()) {
            s.append("\n\nRiwayat tahunan:\n");
            for (YearRow y : f.history) {
                String yr = y.endDate.length() >= 4 ? y.endDate.substring(0, 4) : y.endDate;
                s.append("• ").append(yr).append(": ").append(rpBig(y.revenue))
                 .append(" • Laba ").append(rpBig(y.netIncome)).append('\n');
            }
        }
        s.append("\nSumber: Yahoo Finance (tahunan)");
        return s.toString().trim();
    }

    /** Sub-kartu 2: Neraca (ringkas — rincian BS Yahoo kosong untuk IDX). */
    public static String formatNeraca(FinData f) {
        StringBuilder s = new StringBuilder();
        double eq = equityEst(f.bookValue, f.sharesOutstanding);
        s.append("• Total ekuitas (estimasi): ").append(rpBig(eq)).append('\n');
        s.append("• Total utang: ").append(rpBig(f.totalDebt)).append('\n');
        s.append("• DER: ").append(mult(der(f.totalDebt, f.bookValue, f.sharesOutstanding)))
         .append(" (estimasi)").append('\n');
        s.append("• Book value/saham: ")
         .append(Double.isNaN(f.bookValue) ? "—" : "Rp " + Fmt.rp(f.bookValue));
        s.append("\n\nCatatan: rincian neraca per akun tidak disediakan Yahoo untuk emiten IDX; "
                + "ekuitas diestimasi dari book value × saham beredar.");
        return s.toString().trim();
    }

    /** Sub-kartu 3: Arus Kas. */
    public static String formatArusKas(FinData f) {
        StringBuilder s = new StringBuilder();
        s.append("• Arus kas operasi: ").append(rpBig(f.operatingCashflow)).append('\n');
        s.append("• Free cash flow: ").append(rpBig(f.freeCashflow));
        s.append("\n\nCatatan: rincian arus kas per akun tidak disediakan Yahoo untuk emiten IDX.");
        return s.toString().trim();
    }

    /** Sub-kartu 4: Valuasi & Pertumbuhan (dihitung client-side dari harga kini). */
    public static String formatValuasi(FinData f, double price) {
        StringBuilder s = new StringBuilder();
        s.append("• P/E (harga/EPS): ").append(mult(pe(price, f.eps))).append('\n');
        s.append("• P/BV (harga/book value): ").append(mult(pbv(price, f.bookValue))).append('\n');
        s.append("• Pertumbuhan pendapatan YoY: ").append(pct1(yoyOf(f, true))).append('\n');
        s.append("• Pertumbuhan laba YoY: ").append(pct1(yoyOf(f, false)));
        if (!f.recommendation.isEmpty() || !Double.isNaN(f.targetMean)) {
            s.append("\n\nTarget analis (").append(f.analystCount).append(" analis):\n");
            s.append("• Rekomendasi: ").append(recLabel(f.recommendation)).append('\n');
            s.append("• Target rata-rata: ")
             .append(Double.isNaN(f.targetMean) ? "—" : "Rp " + Fmt.rp(f.targetMean));
            if (!Double.isNaN(f.targetLow) && !Double.isNaN(f.targetHigh)) {
                s.append("\n• Rentang: Rp ").append(Fmt.rp(f.targetLow))
                 .append(" – Rp ").append(Fmt.rp(f.targetHigh));
            }
        }
        if (!f.fiscalYearEnd.isEmpty())
            s.append("\n\nTahun fiskal berakhir ").append(tglId(f.fiscalYearEnd)).append('.');
        return s.toString().trim();
    }

    /** YoY dari riwayat tahunan bila ada (>=2 thn), fallback ke angka Yahoo. */
    static double yoyOf(FinData f, boolean revenue) {
        if (f.history.size() >= 2) {
            YearRow a = f.history.get(0), b = f.history.get(1);
            double y = revenue ? yoy(a.revenue, b.revenue) : yoy(a.netIncome, b.netIncome);
            if (!Double.isNaN(y)) return y;
        }
        return revenue ? f.revenueGrowth : f.earningsGrowth;
    }

    private static String recLabel(String key) {
        if ("strong_buy".equals(key)) return "Strong Buy";
        if ("buy".equals(key)) return "Buy";
        if ("hold".equals(key)) return "Hold";
        if ("sell".equals(key)) return "Sell";
        if ("strong_sell".equals(key)) return "Strong Sell";
        return key.isEmpty() ? "—" : key;
    }

    /** "2025-12-31" -> "31 Des 2025". */
    static String tglId(String iso) {
        try {
            String[] p = iso.split("-");
            String[] bln = {"", "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
            int b = Integer.parseInt(p[1]);
            return Integer.parseInt(p[2]) + " " + bln[b] + " " + p[0];
        } catch (Exception e) {
            return iso;
        }
    }

    private static String pct(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(java.util.Locale.GERMANY, "%,.2f%%", v * 100.0);
    }

    /** Persen 1 desimal. */
    static String pct1(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(java.util.Locale.GERMANY, "%+.1f%%", v * 100.0);
    }

    /** Kelipatan 2 desimal: 12,76x. */
    static String mult(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        return String.format(java.util.Locale.GERMANY, "%,.2fx", v);
    }
}
