package com.flexurio.bursaku;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Klasifikasi saham syariah vs non-syariah.
 *
 * Sumber: Daftar Efek Syariah (DES) OJK Periode I 2026
 * (KEP-21/D.04/2026, efektif 1 Juni 2026) + tambahan insidentil
 * JECX & JELI (efektif 7 Juli 2026). Daftar di-embed statis di
 * assets/sharia.json; emiten yang tidak ada di daftar dianggap
 * NON-SYARIAH. Meta periode di assets/sharia-meta.json.
 */
public class Sharia {

    private static Set<String> cache;
    private static String periodLabel;

    /** Normalisasi kode: buang ".JK", trim, huruf besar. */
    public static String norm(String symbol) {
        if (symbol == null) return "";
        String s = symbol.trim().toUpperCase();
        if (s.endsWith(".JK")) s = s.substring(0, s.length() - 3);
        return s;
    }

    /** Parse JSON array kode -> Set (murni, untuk unit test). */
    public static Set<String> parseJson(String json) {
        Set<String> out = new HashSet<String>();
        if (json == null || json.isEmpty()) return out;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length(); i++) {
                String c = norm(a.optString(i, ""));
                if (!c.isEmpty()) out.add(c);
            }
        } catch (Exception e) { /* set kosong */ }
        return out;
    }

    /** Murni, untuk unit test: apakah simbol ada di daftar syariah. */
    public static boolean isSharia(Set<String> set, String symbol) {
        if (set == null || set.isEmpty()) return false;
        return set.contains(norm(symbol));
    }

    /** Muat daftar dari assets (sekali, lalu di-cache). */
    public static synchronized Set<String> load(Context ctx) {
        if (cache != null) return cache;
        cache = parseJson(readAsset(ctx, "sharia.json"));
        return cache;
    }

    /** Label periode DES untuk ditampilkan di UI. */
    public static synchronized String periodLabel(Context ctx) {
        if (periodLabel != null) return periodLabel;
        periodLabel = "DES OJK";
        try {
            JSONObject o = new JSONObject(readAsset(ctx, "sharia-meta.json"));
            String p = o.optString("periode", "");
            if (!p.isEmpty()) periodLabel = "DES OJK " + p;
        } catch (Exception e) { /* default */ }
        return periodLabel;
    }

    /** Cek cepat: apakah simbol syariah (memuat daftar bila perlu). */
    public static boolean isSharia(Context ctx, String symbol) {
        return isSharia(load(ctx), symbol);
    }

    private static String readAsset(Context ctx, String name) {
        try {
            InputStream is = ctx.getAssets().open(name);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            return bos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
