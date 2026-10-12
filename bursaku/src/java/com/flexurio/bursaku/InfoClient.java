package com.flexurio.bursaku;

import org.json.JSONObject;

/**
 * Info emiten via Yahoo Finance quoteSummary modul assetProfile.
 * Data jarang berubah -> cache 90 hari (CacheTtl.INFO).
 */
public class InfoClient {

    public static class Info {
        public String sector = "";
        public String industry = "";
        public String summary = "";
        public String website = "";
    }

    /** Ambil info emiten. Melempar Exception bila gagal. */
    public static Info fetch(String symbol) throws Exception {
        try {
            return doFetch(symbol, YahooCrumb.get());
        } catch (Exception first) {
            YahooCrumb.invalidate();
            return doFetch(symbol, YahooCrumb.get());
        }
    }

    private static Info doFetch(String symbol, String crumb) throws Exception {
        String url = "https://query2.finance.yahoo.com/v10/finance/quoteSummary/" + symbol
                + "?modules=assetProfile&crumb=" + crumb;
        return parse(YahooClient.get(url));
    }

    static Info parse(String body) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject qs = root.getJSONObject("quoteSummary");
        if (qs.has("error") && !qs.isNull("error"))
            throw new Exception("Info emiten tidak tersedia");
        JSONObject res = qs.getJSONArray("result").getJSONObject(0);
        JSONObject ap = res.optJSONObject("assetProfile");
        if (ap == null) throw new Exception("Info emiten tidak tersedia");
        Info i = new Info();
        i.sector = ap.optString("sector", "");
        i.industry = ap.optString("industry", "");
        i.summary = ap.optString("longBusinessSummary", "");
        i.website = ap.optString("website", "");
        return i;
    }

    /** Format ringkas untuk TextView. */
    public static String format(Info i) {
        StringBuilder s = new StringBuilder();
        if (!i.sector.isEmpty()) s.append("• Sektor: ").append(i.sector).append('\n');
        if (!i.industry.isEmpty()) s.append("• Industri: ").append(i.industry).append('\n');
        if (!i.website.isEmpty()) s.append("• Web: ").append(i.website).append('\n');
        if (!i.summary.isEmpty()) {
            String sum = i.summary.length() > 400
                    ? i.summary.substring(0, 400).trim() + "…" : i.summary;
            s.append('\n').append(sum);
        }
        String t = s.toString().trim();
        return t.isEmpty() ? "Info emiten: — (data tidak tersedia)" : t;
    }
}
