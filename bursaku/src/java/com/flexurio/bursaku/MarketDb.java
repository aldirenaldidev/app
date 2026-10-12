package com.flexurio.bursaku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** SQLite lokal: watchlist, cache ranking, alert harga, transaksi portofolio. */
public class MarketDb extends SQLiteOpenHelper {

    public MarketDb(Context ctx) {
        super(ctx, "bursaku.db", null, 5);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE watchlist(symbol TEXT PRIMARY KEY, name TEXT)");
        db.execSQL("CREATE TABLE rankcache(symbol TEXT PRIMARY KEY, name TEXT, price REAL, chgpct REAL, volume INTEGER, updated INTEGER)");
        db.execSQL("CREATE TABLE alerts(id INTEGER PRIMARY KEY AUTOINCREMENT, symbol TEXT, target REAL, dir TEXT, active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT, symbol TEXT, date TEXT, lot REAL, price REAL, type TEXT)");
        createCacheTables(db);
        createTieredTables(db);
        createDivCache(db);
        createSweepState(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) createCacheTables(db);
        if (o < 3) createTieredTables(db);
        if (o < 4) createDivCache(db);
        if (o < 5) createSweepState(db);
    }

    private void createCacheTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS market_cache(symbol TEXT PRIMARY KEY, price REAL, chgpct REAL, prevclose REAL, w52h REAL, w52l REAL, closes TEXT, updated INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS wl_cache(symbol TEXT PRIMARY KEY, price REAL, chgpct REAL, updated INTEGER)");
    }

    private void createTieredTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cache_meta(symbol TEXT, category TEXT, updated INTEGER, PRIMARY KEY(symbol, category))");
        db.execSQL("CREATE TABLE IF NOT EXISTS text_cache(symbol TEXT, category TEXT, content TEXT, updated INTEGER, PRIMARY KEY(symbol, category))");
    }

    private void createDivCache(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS divcache(symbol TEXT PRIMARY KEY, name TEXT, price REAL, yieldpct REAL, annual REAL, payout REAL, updated INTEGER)");
    }

    private void createSweepState(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sweep_state(skey TEXT PRIMARY KEY, svalue TEXT)");
    }

    // ---------- sweep_state (watermark pindai dividen penuh) ----------
    public String sweepGet(String key) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT svalue FROM sweep_state WHERE skey=?", new String[]{key});
        String v = null;
        if (c.moveToFirst()) v = c.getString(0);
        c.close();
        return v;
    }

    public void sweepSet(String key, String value) {
        ContentValues v = new ContentValues();
        v.put("skey", key);
        v.put("svalue", value == null ? "" : value);
        getWritableDatabase().insertWithOnConflict("sweep_state", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Timestamp cache dividen per simbol; 0 bila belum pernah dipindai. */
    public long divGetUpdated(String symbol) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT updated FROM divcache WHERE symbol=?", new String[]{symbol});
        long t = 0;
        if (c.moveToFirst()) t = c.getLong(0);
        c.close();
        return t;
    }

    // ---------- divcache (Top Dividen LQ45) ----------
    public void divSave(String symbol, String name, double price,
                        double yieldPct, double annual, double payout) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("name", name);
        v.put("price", price); v.put("yieldpct", yieldPct);
        v.put("annual", annual); v.put("payout", payout);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("divcache", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static class DivRow {
        public String symbol, name;
        public double price, yieldPct, annual, payout;
    }

    public List<DivRow> divAll() {
        List<DivRow> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT symbol, name, price, yieldpct, annual, payout FROM divcache", null);
        while (c.moveToNext()) {
            DivRow r = new DivRow();
            r.symbol = c.getString(0); r.name = c.getString(1);
            r.price = c.getDouble(2); r.yieldPct = c.getDouble(3);
            r.annual = c.getDouble(4); r.payout = c.getDouble(5);
            out.add(r);
        }
        c.close();
        return out;
    }

    public long divUpdated() {
        Cursor c = getReadableDatabase().rawQuery("SELECT MAX(updated) FROM divcache", null);
        long t = 0;
        if (c.moveToFirst()) t = c.getLong(0);
        c.close();
        return t;
    }

    // ---------- cache bertingkat ----------

    /** Timestamp cache per (simbol, kategori); 0 bila belum ada. */
    public long cacheGet(String symbol, String category) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT updated FROM cache_meta WHERE symbol=? AND category=?",
                new String[]{symbol, category});
        long t = 0;
        if (c.moveToFirst()) t = c.getLong(0);
        c.close();
        return t;
    }

    public void cacheSet(String symbol, String category, long now) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("category", category); v.put("updated", now);
        getWritableDatabase().insertWithOnConflict("cache_meta", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void cacheSet(String symbol, String category) {
        cacheSet(symbol, category, System.currentTimeMillis());
    }

    /** true bila kategori basi menurut TTL-nya. */
    public boolean cacheStale(String symbol, String category) {
        return CacheTtl.isStale(cacheGet(symbol, category),
                System.currentTimeMillis(), category);
    }

    /** Isi teks cache (untuk dividen/holders/dll); null bila kosong. */
    public String textGet(String symbol, String category) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT content FROM text_cache WHERE symbol=? AND category=?",
                new String[]{symbol, category});
        String s = null;
        if (c.moveToFirst()) s = c.getString(0);
        c.close();
        return s;
    }

    public void textSet(String symbol, String category, String content) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("category", category);
        v.put("content", content == null ? "" : content); v.put("updated", now);
        getWritableDatabase().insertWithOnConflict("text_cache", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
        cacheSet(symbol, category, now);
    }

    // ---------- watchlist ----------
    public void wlAdd(String symbol, String name) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("name", name);
        getWritableDatabase().insertWithOnConflict("watchlist", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void wlRemove(String symbol) {
        getWritableDatabase().delete("watchlist", "symbol=?", new String[]{symbol});
    }

    public List<String[]> wlAll() {
        List<String[]> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT symbol, name FROM watchlist ORDER BY symbol", null);
        while (c.moveToNext()) out.add(new String[]{c.getString(0), c.getString(1)});
        c.close();
        return out;
    }

    // ---------- rankcache ----------
    public void rankSave(String symbol, String name, double price, double chg, long vol) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("name", name);
        v.put("price", price); v.put("chgpct", chg); v.put("volume", vol);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("rankcache", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static class RankRow {
        public String symbol, name;
        public double price, chg;
        public long vol;
    }

    public List<RankRow> rankAll() {
        List<RankRow> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT symbol, name, price, chgpct, volume FROM rankcache", null);
        while (c.moveToNext()) {
            RankRow r = new RankRow();
            r.symbol = c.getString(0); r.name = c.getString(1);
            r.price = c.getDouble(2); r.chg = c.getDouble(3); r.vol = c.getLong(4);
            out.add(r);
        }
        c.close();
        return out;
    }

    public long rankUpdated() {
        Cursor c = getReadableDatabase().rawQuery("SELECT MAX(updated) FROM rankcache", null);
        long t = 0;
        if (c.moveToFirst()) t = c.getLong(0);
        c.close();
        return t;
    }

    // ---------- market_cache (indeks + valas/komoditas, untuk auto-refresh) ----------
    public static class MktRow {
        public String symbol;
        public double price, chg, prevClose, w52h, w52l;
        public String closesCsv;
        public long updated;
    }

    public void mktSave(String symbol, double price, double chg, double prevClose,
                        double w52h, double w52l, String closesCsv) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("price", price); v.put("chgpct", chg);
        v.put("prevclose", prevClose); v.put("w52h", w52h); v.put("w52l", w52l);
        v.put("closes", closesCsv);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("market_cache", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public MktRow mktGet(String symbol) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT price, chgpct, prevclose, w52h, w52l, closes, updated FROM market_cache WHERE symbol=?",
                new String[]{symbol});
        MktRow r = null;
        if (c.moveToFirst()) {
            r = new MktRow();
            r.symbol = symbol;
            r.price = c.getDouble(0); r.chg = c.getDouble(1);
            r.prevClose = c.getDouble(2); r.w52h = c.getDouble(3); r.w52l = c.getDouble(4);
            r.closesCsv = c.getString(5); r.updated = c.getLong(6);
        }
        c.close();
        return r;
    }

    public long mktUpdated() {
        Cursor c = getReadableDatabase().rawQuery("SELECT MAX(updated) FROM market_cache", null);
        long t = 0;
        if (c.moveToFirst()) t = c.getLong(0);
        c.close();
        return t;
    }

    // ---------- wl_cache (harga watchlist, untuk auto-refresh) ----------
    public void wlPriceSave(String symbol, double price, double chg) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("price", price); v.put("chgpct", chg);
        v.put("updated", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("wl_cache", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** {price, chgpct, updated} atau null bila belum ada cache. */
    public double[] wlPriceGet(String symbol) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT price, chgpct, updated FROM wl_cache WHERE symbol=?",
                new String[]{symbol});
        double[] r = null;
        if (c.moveToFirst()) r = new double[]{c.getDouble(0), c.getDouble(1), c.getDouble(2)};
        c.close();
        return r;
    }

    // ---------- alerts ----------
    public static class Alert {
        public long id;
        public String symbol;
        public double target;
        public String dir;   // "above" | "below"
        public boolean active;
    }

    public long alertAdd(String symbol, double target, String dir) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("target", target); v.put("dir", dir); v.put("active", 1);
        return getWritableDatabase().insert("alerts", null, v);
    }

    public void alertDelete(long id) {
        getWritableDatabase().delete("alerts", "id=?", new String[]{String.valueOf(id)});
    }

    public void alertSetActive(long id, boolean active) {
        ContentValues v = new ContentValues();
        v.put("active", active ? 1 : 0);
        getWritableDatabase().update("alerts", v, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Alert> alertsAll() {
        List<Alert> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, symbol, target, dir, active FROM alerts ORDER BY symbol", null);
        while (c.moveToNext()) {
            Alert a = new Alert();
            a.id = c.getLong(0); a.symbol = c.getString(1);
            a.target = c.getDouble(2); a.dir = c.getString(3);
            a.active = c.getInt(4) == 1;
            out.add(a);
        }
        c.close();
        return out;
    }

    public List<Alert> alertsActive() {
        List<Alert> out = new ArrayList<>();
        for (Alert a : alertsAll()) if (a.active) out.add(a);
        return out;
    }

    // ---------- portfolio ----------
    public static class Tx {
        public long id;
        public String symbol, date, type; // type: BUY | SELL
        public double lot, price;
    }

    public long txAdd(String symbol, String date, double lot, double price, String type) {
        ContentValues v = new ContentValues();
        v.put("symbol", symbol); v.put("date", date);
        v.put("lot", lot); v.put("price", price); v.put("type", type);
        return getWritableDatabase().insert("transactions", null, v);
    }

    public void txDelete(long id) {
        getWritableDatabase().delete("transactions", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Tx> txAll() {
        List<Tx> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, symbol, date, lot, price, type FROM transactions ORDER BY date, id", null);
        while (c.moveToNext()) {
            Tx t = new Tx();
            t.id = c.getLong(0); t.symbol = c.getString(1); t.date = c.getString(2);
            t.lot = c.getDouble(3); t.price = c.getDouble(4); t.type = c.getString(5);
            out.add(t);
        }
        c.close();
        return out;
    }
}
