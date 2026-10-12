package com.flexurio.bursaku;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logika murni daftar ticker IDX: parse JSON + filter autocomplete.
 * Sengaja tanpa dependensi Android agar bisa di-unit-test di JVM.
 */
public class TickerFilter {

    public static class Ticker {
        public final String code;
        public final String name;
        public Ticker(String code, String name) {
            this.code = code == null ? "" : code;
            this.name = name == null ? "" : name;
        }
    }

    private static final Pattern ENTRY = Pattern.compile(
            "\\{\\s*\"c\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"n\"\\s*:\\s*\"([^\"]*)\"\\s*\\}");

    /**
     * Parse tickers.json berformat [{"c":"BBCA","n":"Bank Central Asia Tbk."}, ...].
     * Input rusak -> list kosong, tidak melempar.
     */
    public static List<Ticker> parseJson(String json) {
        List<Ticker> out = new ArrayList<Ticker>();
        if (json == null) return out;
        try {
            Matcher m = ENTRY.matcher(json);
            while (m.find()) {
                out.add(new Ticker(m.group(1), m.group(2)));
            }
        } catch (Exception e) { /* abaikan: kembalikan yang terkumpul */ }
        return out;
    }

    /**
     * Filter autocomplete: cocok dengan kode ATAU nama perusahaan,
     * case-insensitive, substring. Query kosong -> seluruh daftar.
     */
    public static List<Ticker> filter(List<Ticker> all, String q) {
        List<Ticker> out = new ArrayList<Ticker>();
        if (all == null) return out;
        String query = q == null ? "" : q.trim().toUpperCase();
        if (query.isEmpty()) {
            out.addAll(all);
            return out;
        }
        for (Ticker t : all) {
            if (t.code.toUpperCase().contains(query)
                    || t.name.toUpperCase().contains(query)) {
                out.add(t);
            }
        }
        return out;
    }
}
