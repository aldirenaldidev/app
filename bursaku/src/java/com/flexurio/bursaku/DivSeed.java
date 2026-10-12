package com.flexurio.bursaku;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Seed cache dividen (divcache) dari assets/dividends.json — snapshot
 * pre-computed yang di-bundle di APK. Membuat tab Top Dividen langsung
 * tampil penuh tanpa pindai 940 emiten (±15 menit).
 *
 * Dipanggil sekali saat divcache kosong. Bila file hilang/rusak,
 * kembalikan FAILED agar pemanggil fallback ke pindai background.
 */
public class DivSeed {

    public static final int OK = 0;
    public static final int ALREADY = 1;
    public static final int FAILED = 2;

    public static class SeedResult {
        public int status = FAILED;
        public int count = 0;
        /** Tanggal snapshot, mis. "8 Okt 2026". */
        public String asOfDisplay = "";
    }

    /** Item hasil parse murni (bisa di-unit-test tanpa Android). */
    public static class SeedItem {
        public String code, name;
        public double yieldPct, annual, payout, price;
    }

    /**
     * Parse murni dividends.json → daftar item valid.
     * Melempar bila JSON rusak / items kosong. names boleh null.
     */
    public static java.util.List<SeedItem> parse(String json, String[] asOfOut,
            java.util.Map<String, String> names) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONArray items = root.getJSONArray("items");
        if (items.length() == 0) throw new Exception("items kosong");
        if (asOfOut != null && asOfOut.length > 0) asOfOut[0] = root.optString("as_of", "");
        java.util.List<SeedItem> out = new java.util.ArrayList<SeedItem>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue; // entri rusak → lewati
            String code = o.optString("c", "").trim().toUpperCase();
            double y = o.optDouble("y", Double.NaN);
            if (code.isEmpty() || Double.isNaN(y) || y <= 0) continue;
            SeedItem s = new SeedItem();
            s.code = code;
            s.name = (names != null && names.get(code) != null) ? names.get(code) : code;
            s.yieldPct = y;
            s.annual = o.optDouble("r", 0);
            s.payout = (!o.isNull("p")) ? o.optDouble("p", 0) : 0;
            s.price = o.optDouble("px", 0);
            out.add(s);
        }
        if (out.isEmpty()) throw new Exception("tidak ada item valid");
        return out;
    }

    /** Seed bila divcache kosong. Tidak pernah melempar. */
    public static SeedResult seed(Context ctx, MarketDb db) {
        SeedResult r = new SeedResult();
        try {
            if (!db.divAll().isEmpty()) {
                r.status = ALREADY;
                return r;
            }
            InputStream is = ctx.getAssets().open("dividends.json");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            String[] asOf = new String[1];
            java.util.Map<String, String> names = new HashMap<>();
            List<TickerFilter.Ticker> tickers = Tickers.load(ctx);
            if (tickers != null) {
                for (TickerFilter.Ticker t : tickers) names.put(t.code, t.name);
            }
            java.util.List<SeedItem> list = parse(new String(bos.toByteArray(), "UTF-8"), asOf, names);
            for (SeedItem s : list) {
                db.divSave(s.code + ".JK", s.name, s.price, s.yieldPct, s.annual, s.payout);
            }
            r.count = list.size();
            r.status = OK;
            r.asOfDisplay = fmtAsOf(asOf[0]);
            // Tandai "pindai penuh" selesai pada tanggal snapshot agar
            // logika stale 30 hari tetap jalan.
            long asOfMs = parseAsOf(asOf[0]);
            if (asOfMs > 0) {
                db.sweepSet(DivSweep.KEY_DONE, String.valueOf(asOfMs));
                db.sweepSet(DivSweep.KEY_IDX, "0");
            }
        } catch (Exception e) {
            r.status = FAILED;
        }
        return r;
    }

    /** "2026-10-08" → "8 Okt 2026". */
    private static String fmtAsOf(String asOf) {
        long ms = parseAsOf(asOf);
        if (ms <= 0) return asOf;
        return DividendClient.fmtDate(ms / 1000L);
    }

    private static long parseAsOf(String asOf) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            java.util.Date d = f.parse(asOf);
            return d == null ? 0 : d.getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
