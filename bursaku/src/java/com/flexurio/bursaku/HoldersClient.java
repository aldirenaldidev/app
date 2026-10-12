package com.flexurio.bursaku;

import org.json.JSONObject;

/**
 * Client komposisi pemegang saham via Yahoo Finance quoteSummary (tanpa API key).
 *
 * Terverifikasi 2026-10-07: alur crumb+cookie berfungsi untuk emiten IDX besar
 * (BBCA.JK, BBRI.JK, TLKM.JK — semuanya mengembalikan data tidak kosong):
 *   1. GET https://fc.yahoo.com            (kumpulkan cookie; isi/status diabaikan)
 *   2. GET https://query1.finance.yahoo.com/v1/test/getcrumb
 *   3. GET https://query2.finance.yahoo.com/v10/finance/quoteSummary/{SIMBOL}
 *          ?modules=majorHoldersBreakdown&crumb=...
 *
 * Modul institutionOwnership untuk emiten IDX praktis kosong, sehingga yang
 * dipakai hanya majorHoldersBreakdown: angka AGREGAT (insider/institusi/float),
 * bukan daftar nama pemegang saham. Daftar nama pemegang >5% dipublikasikan
 * bulanan oleh IDX (register KSEI) dan tidak tersedia via API publik.
 */
public class HoldersClient {

    /** Komposisi agregat. Fraksi 0..1; NaN / -1 bila tidak tersedia. */
    public static class HoldersData {
        public double insidersPct = Double.NaN;
        public double institutionsPct = Double.NaN;
        public double floatPct = Double.NaN;
        public long institutionsCount = -1;
    }

    /** Ambil komposisi pemegang saham. Melempar Exception bila gagal. */
    public static HoldersData fetchHolders(String symbol) throws Exception {
        try {
            return doFetch(symbol, YahooCrumb.get());
        } catch (Exception first) {
            // Kemungkinan crumb kedaluwarsa: buang cache, coba sekali lagi.
            YahooCrumb.invalidate();
            return doFetch(symbol, YahooCrumb.get());
        }
    }

    private static HoldersData doFetch(String symbol, String crumb) throws Exception {
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                + "?modules=majorHoldersBreakdown&crumb=" + crumb;
        return parse(YahooClient.get(url));
    }

    static HoldersData parse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject qs = root.getJSONObject("quoteSummary");
        if (qs.has("error") && !qs.isNull("error"))
            throw new Exception("Data pemegang saham tidak tersedia");
        JSONObject m = qs.getJSONArray("result").getJSONObject(0)
                .getJSONObject("majorHoldersBreakdown");
        HoldersData h = new HoldersData();
        h.insidersPct = frac(m, "insidersPercentHeld");
        h.institutionsPct = frac(m, "institutionsPercentHeld");
        h.floatPct = frac(m, "institutionsFloatPercentHeld");
        try {
            if (m.has("institutionsCount") && !m.isNull("institutionsCount"))
                h.institutionsCount = m.getJSONObject("institutionsCount").getLong("raw");
        } catch (Exception e) { /* abaikan */ }
        return h;
    }

    private static double frac(JSONObject o, String k) {
        try {
            if (!o.has(k) || o.isNull(k)) return Double.NaN;
            return o.getJSONObject(k).getDouble("raw");
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
