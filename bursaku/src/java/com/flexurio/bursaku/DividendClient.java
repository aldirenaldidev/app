package com.flexurio.bursaku;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Client info dividen via Yahoo Finance (tanpa API key).
 *
 * Terverifikasi 2026-10-07 untuk BBCA.JK:
 * - Riwayat: v8 chart ?events=div → chart.result[0].events.dividends
 *   = {timestamp: {amount, date}} (12 dividen dalam 5 tahun untuk BBCA)
 * - Metrik: quoteSummary summaryDetail (dividendRate, dividendYield,
 *   exDividendDate, payoutRatio, fiveYearAvgDividendYield,
 *   trailingAnnualDividendRate/Yield) + defaultKeyStatistics
 *   (lastDividendValue, lastDividendDate) via alur crumb (YahooCrumb).
 *
 * Semua field defensif: kosong → NaN/-1, tidak pernah crash.
 */
public class DividendClient {

    /** Satu pembayaran dividen. */
    public static class DivEntry {
        public long date = -1;          // epoch detik
        public double amount = Double.NaN; // Rp/saham
    }

    /** Data dividen emiten. Fraksi 0..1 untuk yield/ratio; NaN bila tak tersedia. */
    public static class DivData {
        public double annualRate = Double.NaN;   // dividen tahunan Rp/saham (TTM)
        public double dividendYield = Double.NaN;
        public long exDivDate = -1;             // epoch detik
        public double payoutRatio = Double.NaN;
        public double fiveYearAvgYield = Double.NaN;
        public double price = Double.NaN;       // harga kini (modul price)
        public List<DivEntry> history = new ArrayList<>(); // terbaru dulu
    }

    /** Ambil metrik + riwayat. Melempar Exception bila metrik gagal. */
    public static DivData fetch(String symbol) throws Exception {
        DivData d = fetchMetrics(symbol);
        try {
            d.history = fetchHistory(symbol);
        } catch (Exception ignored) { /* riwayat opsional */ }
        return d;
    }

    /** Ambil HANYA metrik (lebih cepat; untuk tab Top Dividen). */
    public static DivData fetchMetrics(String symbol) throws Exception {
        try {
            return doFetchMetrics(symbol, YahooCrumb.get());
        } catch (Exception first) {
            YahooCrumb.invalidate();
            return doFetchMetrics(symbol, YahooCrumb.get());
        }
    }

    private static DivData doFetchMetrics(String symbol, String crumb) throws Exception {
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                + "?modules=summaryDetail,defaultKeyStatistics,price"
                + "&crumb=" + crumb;
        return parseMetrics(YahooClient.get(url));
    }

    /** Riwayat dividen dari v8 chart events=div. */
    public static List<DivEntry> fetchHistory(String symbol) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                + "?interval=1d&range=5y&events=div";
        return parseHistory(YahooClient.get(url));
    }

    /** Parse metrik dari quoteSummary. Murni (bisa di-unit-test). */
    static DivData parseMetrics(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject qs = root.getJSONObject("quoteSummary");
        if (qs.has("error") && !qs.isNull("error"))
            throw new Exception("Data dividen tidak tersedia");
        JSONObject res = qs.getJSONArray("result").getJSONObject(0);
        DivData d = new DivData();

        JSONObject sd = res.optJSONObject("summaryDetail");
        if (sd != null) {
            d.annualRate = raw(sd, "trailingAnnualDividendRate");
            if (Double.isNaN(d.annualRate)) d.annualRate = raw(sd, "dividendRate");
            d.dividendYield = raw(sd, "trailingAnnualDividendYield");
            if (Double.isNaN(d.dividendYield)) d.dividendYield = raw(sd, "dividendYield");
            d.payoutRatio = raw(sd, "payoutRatio");
            d.fiveYearAvgYield = raw(sd, "fiveYearAvgDividendYield");
            JSONObject ex = sd.optJSONObject("exDividendDate");
            if (ex != null && !ex.isNull("raw")) d.exDivDate = ex.optLong("raw", -1);
        }
        JSONObject ks = res.optJSONObject("defaultKeyStatistics");
        if (ks != null && d.exDivDate < 0) {
            JSONObject ld = ks.optJSONObject("lastDividendDate");
            if (ld != null && !ld.isNull("raw")) d.exDivDate = ld.optLong("raw", -1);
        }
        JSONObject pr = res.optJSONObject("price");
        if (pr != null) d.price = raw(pr, "regularMarketPrice");
        return d;
    }

    /** Parse riwayat dari v8 chart events=div. Murni (bisa di-unit-test). */
    static List<DivEntry> parseHistory(String body) {
        List<DivEntry> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(body);
            JSONObject result = root.getJSONObject("chart")
                    .getJSONArray("result").getJSONObject(0);
            JSONObject events = result.optJSONObject("events");
            if (events == null) return out;
            JSONObject divs = events.optJSONObject("dividends");
            if (divs == null) return out;
            JSONArray names = divs.names();
            if (names == null) return out;
            for (int i = 0; i < names.length(); i++) {
                JSONObject o = divs.optJSONObject(names.getString(i));
                if (o == null) continue;
                DivEntry e = new DivEntry();
                e.date = o.optLong("date", -1);
                e.amount = o.optDouble("amount", Double.NaN);
                if (e.date > 0 && !Double.isNaN(e.amount)) out.add(e);
            }
            Collections.sort(out, new Comparator<DivEntry>() {
                public int compare(DivEntry a, DivEntry b) {
                    return Long.compare(b.date, a.date); // terbaru dulu
                }
            });
        } catch (Exception ignored) { /* kembalikan yang terkumpul */ }
        return out;
    }

    private static double raw(JSONObject o, String k) {
        try {
            if (!o.has(k) || o.isNull(k)) return Double.NaN;
            return o.getJSONObject(k).getDouble("raw");
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static final Locale ID = new Locale("id", "ID");

    /** Format tanggal Indonesia: 21 Mar 2025. */
    public static String fmtDate(long epochSec) {
        if (epochSec <= 0) return "—";
        SimpleDateFormat f = new SimpleDateFormat("d MMM yyyy", ID);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        return f.format(new java.util.Date(epochSec * 1000L));
    }

    /** Format teks section Dividen untuk TextView. Tidak pernah melempar. */
    public static String format(DivData d) {
        try {
            StringBuilder s = new StringBuilder();
            s.append("Sumber: Yahoo Finance\n");
            s.append("• Dividend yield: ")
             .append(Double.isNaN(d.dividendYield) ? "—"
                     : String.format(Locale.GERMANY, "%,.2f%%", d.dividendYield * 100.0))
             .append('\n');
            s.append("• Dividen tahunan: ")
             .append(Double.isNaN(d.annualRate) ? "—" : "Rp " + Fmt.rp(d.annualRate) + "/saham")
             .append('\n');
            s.append("• Ex-dividen terakhir: ").append(fmtDate(d.exDivDate)).append('\n');
            s.append("• Payout ratio: ")
             .append(Double.isNaN(d.payoutRatio) ? "—"
                     : String.format(Locale.GERMANY, "%,.2f%%", d.payoutRatio * 100.0))
             .append('\n');
            s.append("• Rata-rata yield 5 thn: ")
             .append(Double.isNaN(d.fiveYearAvgYield) ? "—"
                     : String.format(Locale.GERMANY, "%,.2f%%", d.fiveYearAvgYield));
            if (!d.history.isEmpty()) {
                s.append("\n\nRiwayat dividen:\n");
                int n = Math.min(d.history.size(), 10);
                for (int i = 0; i < n; i++) {
                    DivEntry e = d.history.get(i);
                    s.append("• ").append(fmtDate(e.date))
                     .append(" — Rp ").append(Fmt.rp(e.amount)).append("/saham\n");
                }
            }
            return s.toString().trim();
        } catch (Exception e) {
            return "Dividen: — (data tidak tersedia)";
        }
    }
}
