package com.flexurio.bursaku;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Client Yahoo Finance v8 chart API (tanpa API key).
 * Terbukti berfungsi per 2026-10-07:
 *   GET https://query1.finance.yahoo.com/v8/finance/chart/BBCA.JK?interval=1d&range=1y
 * Endpoint quote v7/v10 mengembalikan 401, sehingga P/E & market cap
 * TIDAK tersedia — hanya data dari blok meta + dividen (events=div).
 */
public class YahooClient {
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static long lastCallMs = 0;

    public static class Meta {
        public String symbol = "";
        public String longName = "";
        public String shortName = "";
        public String currency = "IDR";
        public double price = Double.NaN;
        public double prevClose = Double.NaN;
        public double changePct = Double.NaN;
        public double dayHigh = Double.NaN;
        public double dayLow = Double.NaN;
        public long volume = 0;
        public double w52high = Double.NaN;
        public double w52low = Double.NaN;
        public double divYieldPct = Double.NaN; // trailing 12 bln, %
    }

    public static class ChartData {
        public List<Candle> candles = new ArrayList<>();
        public Meta meta = new Meta();
    }

    /** Ambil data harian. range: "5d" | "1y" | "2y". Termasuk events dividen. */
    public static ChartData fetchChart(String symbol, String range) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                + "?interval=1d&range=" + range + "&events=div";
        String body;
        try {
            body = get(url);
        } catch (RateLimitException e) {
            // fallback ke host query2 sekali
            body = get(url.replace("query1.", "query2."));
        }
        return parse(body, symbol);
    }

    /**
     * Data intraday sesi berjalan (interval 5 menit, range 1 hari).
     * Di luar jam bursa mengembalikan sesi perdagangan terakhir.
     */
    public static ChartData fetchIntraday(String symbol) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                + "?interval=5m&range=1d";
        String body;
        try {
            body = get(url);
        } catch (RateLimitException e) {
            body = get(url.replace("query1.", "query2."));
        }
        return parse(body, symbol);
    }

    /** Hasil validasi + harga dari SATU fetch (agar tak perlu 2x call API). */
    public static class Validation {        public final boolean ok;
        public final double price;
        public final double chg;
        public Validation(boolean ok, double price, double chg) {
            this.ok = ok; this.price = price; this.chg = chg;
        }
    }

    /** Logika murni: tentukan validitas + ekstrak harga dari ChartData. */
    public static Validation validateData(ChartData d) {
        boolean ok = d != null && d.candles != null && d.candles.size() >= 2;
        double p = Double.NaN, c = Double.NaN;
        if (ok && d.meta != null) { p = d.meta.price; c = d.meta.changePct; }
        return new Validation(ok, p, c);
    }

    /** Validasi + harga sekaligus dalam satu fetch. Gagal -> ok=false. */
    public static Validation validateWithPrice(String symbol) {
        try {
            return validateData(fetchChart(symbol, "5d"));
        } catch (Exception e) {
            return new Validation(false, Double.NaN, Double.NaN);
        }
    }

    /** Validasi cepat: true bila simbol dikenali & punya data. */
    public static boolean validateSymbol(String symbol) {
        return validateWithPrice(symbol).ok;
    }

    static class RateLimitException extends Exception {}

    /** GET dengan User-Agent browser + jeda antar request (hindari 429).
     *  v2: jeda dipersingkat (150ms) + timeout dipersingkat (5s/8s) agar
     *  loading jauh lebih cepat; request dijalankan paralel via thread pool. */
    static synchronized String get(String url) throws Exception {
        long now = System.currentTimeMillis();
        long wait = 150 - (now - lastCallMs);
        if (wait > 0) Thread.sleep(wait);
        lastCallMs = System.currentTimeMillis();

        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", UA);
        c.setConnectTimeout(5000);
        c.setReadTimeout(8000);
        int code = c.getResponseCode();
        if (code == 429) {
            Thread.sleep(20000);
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestProperty("User-Agent", UA);
            c.setConnectTimeout(5000);
            c.setReadTimeout(8000);
            code = c.getResponseCode();
            if (code == 429) throw new RateLimitException();
        }
        if (code != 200) throw new Exception("HTTP " + code);
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return sb.toString();
    }

    static double num(JSONObject o, String k) {
        if (!o.has(k) || o.isNull(k)) return Double.NaN;
        try { return o.getDouble(k); } catch (Exception e) { return Double.NaN; }
    }

    static double num(JSONArray a, int i) {
        if (a.isNull(i)) return Double.NaN;
        try { return a.getDouble(i); } catch (Exception e) { return Double.NaN; }
    }

    static ChartData parse(String body, String symbol) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject chart = root.getJSONObject("chart");
        if (chart.has("error") && !chart.isNull("error")) {
            throw new Exception("Simbol tidak ditemukan");
        }
        JSONObject res = chart.getJSONArray("result").getJSONObject(0);
        JSONObject meta = res.getJSONObject("meta");

        ChartData d = new ChartData();
        d.meta.symbol = meta.optString("symbol", symbol);
        d.meta.longName = meta.optString("longName", meta.optString("shortName", symbol));
        d.meta.shortName = meta.optString("shortName", symbol);
        d.meta.currency = meta.optString("currency", "IDR");
        d.meta.price = num(meta, "regularMarketPrice");
        d.meta.prevClose = num(meta, "chartPreviousClose");
        d.meta.changePct = num(meta, "regularMarketChangePercent");
        d.meta.dayHigh = num(meta, "regularMarketDayHigh");
        d.meta.dayLow = num(meta, "regularMarketDayLow");
        d.meta.w52high = num(meta, "fiftyTwoWeekHigh");
        d.meta.w52low = num(meta, "fiftyTwoWeekLow");
        if (!meta.isNull("regularMarketVolume")) {
            try { d.meta.volume = meta.getLong("regularMarketVolume"); } catch (Exception e) { /* abaikan */ }
        }

        // dividen trailing 12 bulan -> yield
        try {
            JSONObject divs = res.optJSONObject("events") != null
                    ? res.getJSONObject("events").optJSONObject("dividends") : null;
            if (divs != null && !Double.isNaN(d.meta.price) && d.meta.price > 0) {
                long nowSec = System.currentTimeMillis() / 1000;
                double tot = 0;
                JSONArray names = divs.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        JSONObject dv = divs.getJSONObject(names.getString(i));
                        long ts = dv.optLong("date", 0);
                        if (nowSec - ts < 365L * 86400) tot += dv.optDouble("amount", 0);
                    }
                }
                if (tot > 0) d.meta.divYieldPct = tot / d.meta.price * 100.0;
            }
        } catch (Exception e) { /* yield opsional */ }

        JSONArray ts = res.getJSONArray("timestamp");
        JSONObject q = res.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);
        JSONArray o = q.getJSONArray("open"), h = q.getJSONArray("high"),
                  l = q.getJSONArray("low"), cl = q.getJSONArray("close"),
                  v = q.optJSONArray("volume");
        for (int i = 0; i < ts.length(); i++) {
            double cc = num(cl, i);
            if (Double.isNaN(cc)) continue; // bar kosong (libur) dilewati
            long vol = 0;
            if (v != null && !v.isNull(i)) {
                try { vol = v.getLong(i); } catch (Exception e) { /* abaikan */ }
            }
            d.candles.add(new Candle(ts.getLong(i), num(o, i), num(h, i), num(l, i), cc, vol));
        }
        if (d.candles.size() < 2) throw new Exception("Data tidak cukup");
        return d;
    }
}
