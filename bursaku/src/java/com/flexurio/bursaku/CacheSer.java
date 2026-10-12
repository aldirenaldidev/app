package com.flexurio.bursaku;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Serialisasi data cache ke JSON (candles & FinData).
 * Murni (tanpa Android) agar bisa di-unit-test di JVM.
 */
public class CacheSer {

    // ---------- candles ----------

    /** Candles -> JSON ringkas [[t,o,h,l,c,v],...]. */
    public static String candlesToJson(List<Candle> cs) {
        try {
            JSONArray a = new JSONArray();
            if (cs != null) {
                for (Candle c : cs) {
                    JSONArray r = new JSONArray();
                    r.put(c.time); r.put(c.open); r.put(c.high);
                    r.put(c.low); r.put(c.close); r.put(c.volume);
                    a.put(r);
                }
            }
            return a.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    /** JSON -> candles. Rusak -> list (mungkin parsial), tidak melempar. */
    public static List<Candle> candlesFromJson(String json) {
        List<Candle> out = new ArrayList<Candle>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                JSONArray r = a.getJSONArray(i);
                out.add(new Candle(r.getLong(0), r.getDouble(1), r.getDouble(2),
                        r.getDouble(3), r.getDouble(4), r.getLong(5)));
            }
        } catch (Exception e) { /* kembalikan yang berhasil diparse */ }
        return out;
    }

    // ---------- chart (meta + candles) ----------

    /** ChartData -> JSON (meta ringkas + candles). Untuk cache CHART. */
    public static String chartToJson(YahooClient.ChartData d) {
        try {
            JSONObject o = new JSONObject();
            JSONObject m = new JSONObject();
            putD(m, "price", d.meta.price);
            putD(m, "prevClose", d.meta.prevClose);
            putD(m, "changePct", d.meta.changePct);
            putD(m, "w52high", d.meta.w52high);
            putD(m, "w52low", d.meta.w52low);
            putD(m, "divYieldPct", d.meta.divYieldPct);
            m.put("longName", d.meta.longName == null ? "" : d.meta.longName);
            m.put("dayHigh", d.meta.dayHigh);
            m.put("dayLow", d.meta.dayLow);
            m.put("volume", d.meta.volume);
            o.put("meta", m);
            JSONArray a = new JSONArray();
            if (d.candles != null) {
                for (Candle c : d.candles) {
                    JSONArray r = new JSONArray();
                    r.put(c.time); r.put(c.open); r.put(c.high);
                    r.put(c.low); r.put(c.close); r.put(c.volume);
                    a.put(r);
                }
            }
            o.put("candles", a);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /** JSON -> ChartData. Rusak -> ChartData kosong, tidak melempar. */
    public static YahooClient.ChartData chartFromJson(String json) {
        YahooClient.ChartData d = new YahooClient.ChartData();
        if (json == null || json.isEmpty()) return d;
        try {
            JSONObject o = new JSONObject(json);
            JSONObject m = o.optJSONObject("meta");
            if (m != null) {
                d.meta.price = m.optDouble("price", Double.NaN);
                d.meta.prevClose = m.optDouble("prevClose", Double.NaN);
                d.meta.changePct = m.optDouble("changePct", Double.NaN);
                d.meta.w52high = m.optDouble("w52high", Double.NaN);
                d.meta.w52low = m.optDouble("w52low", Double.NaN);
                d.meta.divYieldPct = m.optDouble("divYieldPct", Double.NaN);
                d.meta.longName = m.optString("longName", "");
                d.meta.dayHigh = m.optDouble("dayHigh", Double.NaN);
                d.meta.dayLow = m.optDouble("dayLow", Double.NaN);
                d.meta.volume = m.optLong("volume", 0);
            }
            JSONArray a = o.optJSONArray("candles");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONArray r = a.getJSONArray(i);
                    d.candles.add(new Candle(r.getLong(0), r.getDouble(1),
                            r.getDouble(2), r.getDouble(3), r.getDouble(4),
                            r.getLong(5)));
                }
            }
        } catch (Exception e) { /* kembalikan yang berhasil */ }
        return d;
    }

    // ---------- FinData ----------

    private static void putD(JSONObject o, String k, double v) {
        try { o.put(k, Double.isNaN(v) ? JSONObject.NULL : v); }
        catch (Exception e) { /* abaikan */ }
    }

    /** FinData -> JSON (angka mentah; format dilakukan saat tampil). */
    public static String finToJson(FinancialsClient.FinData f) {
        try {
            JSONObject o = new JSONObject();
            putD(o, "revenue", f.revenue);
            putD(o, "netIncome", f.netIncome);
            putD(o, "eps", f.eps);
            putD(o, "roe", f.roe);
            putD(o, "roa", f.roa);
            putD(o, "profitMargin", f.profitMargin);
            putD(o, "revenueGrowth", f.revenueGrowth);
            putD(o, "earningsGrowth", f.earningsGrowth);
            putD(o, "priceToBook", f.priceToBook);
            putD(o, "totalDebt", f.totalDebt);
            putD(o, "operatingCashflow", f.operatingCashflow);
            putD(o, "freeCashflow", f.freeCashflow);
            putD(o, "bookValue", f.bookValue);
            putD(o, "sharesOutstanding", f.sharesOutstanding);
            putD(o, "targetMean", f.targetMean);
            putD(o, "targetHigh", f.targetHigh);
            putD(o, "targetLow", f.targetLow);
            o.put("recommendation", f.recommendation == null ? "" : f.recommendation);
            o.put("analystCount", f.analystCount);
            o.put("fiscalYearEnd", f.fiscalYearEnd == null ? "" : f.fiscalYearEnd);
            JSONArray h = new JSONArray();
            for (FinancialsClient.YearRow y : f.history) {
                JSONObject yo = new JSONObject();
                yo.put("endDate", y.endDate == null ? "" : y.endDate);
                putD(yo, "revenue", y.revenue);
                putD(yo, "netIncome", y.netIncome);
                h.put(yo);
            }
            o.put("history", h);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    /** JSON -> FinData. Rusak -> FinData default (serba NaN), tidak melempar. */
    public static FinancialsClient.FinData finFromJson(String json) {
        FinancialsClient.FinData f = new FinancialsClient.FinData();
        if (json == null || json.isEmpty()) return f;
        try {
            JSONObject o = new JSONObject(json);
            f.revenue = o.optDouble("revenue", Double.NaN);
            f.netIncome = o.optDouble("netIncome", Double.NaN);
            f.eps = o.optDouble("eps", Double.NaN);
            f.roe = o.optDouble("roe", Double.NaN);
            f.roa = o.optDouble("roa", Double.NaN);
            f.profitMargin = o.optDouble("profitMargin", Double.NaN);
            f.revenueGrowth = o.optDouble("revenueGrowth", Double.NaN);
            f.earningsGrowth = o.optDouble("earningsGrowth", Double.NaN);
            f.priceToBook = o.optDouble("priceToBook", Double.NaN);
            f.totalDebt = o.optDouble("totalDebt", Double.NaN);
            f.operatingCashflow = o.optDouble("operatingCashflow", Double.NaN);
            f.freeCashflow = o.optDouble("freeCashflow", Double.NaN);
            f.bookValue = o.optDouble("bookValue", Double.NaN);
            f.sharesOutstanding = o.optDouble("sharesOutstanding", Double.NaN);
            f.targetMean = o.optDouble("targetMean", Double.NaN);
            f.targetHigh = o.optDouble("targetHigh", Double.NaN);
            f.targetLow = o.optDouble("targetLow", Double.NaN);
            f.recommendation = o.optString("recommendation", "");
            f.analystCount = o.optInt("analystCount", 0);
            f.fiscalYearEnd = o.optString("fiscalYearEnd", "");
            JSONArray h = o.optJSONArray("history");
            if (h != null) {
                for (int i = 0; i < h.length(); i++) {
                    JSONObject yo = h.getJSONObject(i);
                    FinancialsClient.YearRow y = new FinancialsClient.YearRow();
                    y.endDate = yo.optString("endDate", "");
                    y.revenue = yo.optDouble("revenue", Double.NaN);
                    y.netIncome = yo.optDouble("netIncome", Double.NaN);
                    f.history.add(y);
                }
            }
        } catch (Exception e) { /* kembalikan default */ }
        return f;
    }
}
