package com.flexurio.dompetku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SQLite lokal DompetKu v2. v2: + ai_words, settings, budget_items, savings_goal. */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DB = "dompetku_v2.db";
    private static final int VER = 2;
    static final String T = "transaksi";

    public DbHelper(Context ctx) {
        super(ctx, DB, null, VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE " + T + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "tipe TEXT NOT NULL, "
                    + "nominal INTEGER NOT NULL, "
                    + "kategori TEXT NOT NULL, "
                    + "tanggal TEXT NOT NULL, "
                    + "catatan TEXT, "
                    + "created_at INTEGER NOT NULL)");
            db.execSQL("CREATE INDEX idx_tgl ON " + T + "(tanggal)");
        } catch (Throwable ignored) {}
        createV2(db);
    }

    private void createV2(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS ai_words ("
                    + "word TEXT PRIMARY KEY, tipe TEXT NOT NULL, "
                    + "kategori TEXT NOT NULL, weight INTEGER NOT NULL DEFAULT 1)");
            db.execSQL("CREATE TABLE IF NOT EXISTS settings ("
                    + "k TEXT PRIMARY KEY, v TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS budget_items ("
                    + "kategori TEXT PRIMARY KEY, persen REAL NOT NULL DEFAULT 0)");
            db.execSQL("CREATE TABLE IF NOT EXISTS savings_goal ("
                    + "id INTEGER PRIMARY KEY CHECK (id=1), "
                    + "target INTEGER NOT NULL, deadline TEXT NOT NULL, "
                    + "start TEXT NOT NULL, salary INTEGER NOT NULL, "
                    + "created_at INTEGER NOT NULL)");
        } catch (Throwable ignored) {}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        try {
            if (oldV < 2) createV2(db);
        } catch (Throwable ignored) {}
    }

    // ============ transaksi (v1, tak berubah) ============

    public long addTx(String tipe, long nominal, String kategori, String tanggal, String catatan) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues v = new ContentValues();
            v.put("tipe", tipe);
            v.put("nominal", nominal);
            v.put("kategori", kategori);
            v.put("tanggal", tanggal);
            v.put("catatan", catatan == null ? "" : catatan);
            v.put("created_at", System.currentTimeMillis());
            return db.insert(T, null, v);
        } catch (Throwable t) {
            return -1;
        }
    }

    public boolean deleteTx(long id) {
        try {
            return getWritableDatabase().delete(T, "id=?", new String[]{String.valueOf(id)}) > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public long sumByTipe(String tipe, String from, String to) {
        long out = 0;
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(nominal),0) FROM " + T
                    + " WHERE tipe=? AND tanggal>=? AND tanggal<=?",
                    new String[]{tipe, from, to});
            if (c.moveToFirst()) out = c.getLong(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    public Map<String, Long> sumOutByCat(String from, String to) {
        Map<String, Long> m = new LinkedHashMap<String, Long>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT kategori, COALESCE(SUM(nominal),0) FROM " + T
                    + " WHERE tipe='out' AND tanggal>=? AND tanggal<=?"
                    + " GROUP BY kategori ORDER BY 2 DESC",
                    new String[]{from, to});
            while (c.moveToNext()) m.put(c.getString(0), c.getLong(1));
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return m;
    }

    public Map<String, Long> sumOutByDay(String from, String to) {
        Map<String, Long> m = new LinkedHashMap<String, Long>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT tanggal, COALESCE(SUM(nominal),0) FROM " + T
                    + " WHERE tipe='out' AND tanggal>=? AND tanggal<=?"
                    + " GROUP BY tanggal",
                    new String[]{from, to});
            while (c.moveToNext()) m.put(c.getString(0), c.getLong(1));
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return m;
    }

    public int countTx(String from, String to) {
        int n = 0;
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM " + T + " WHERE tanggal>=? AND tanggal<=?",
                    new String[]{from, to});
            if (c.moveToFirst()) n = c.getInt(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return n;
    }

    /** Daftar transaksi terbaru (atau rentang), desc by tanggal. */
    public List<Tx> listTx(String from, String to, int limit) {
        List<Tx> out = new ArrayList<Tx>();
        Cursor c = null;
        try {
            String sql = "SELECT id, tipe, nominal, kategori, tanggal, catatan FROM " + T
                    + " WHERE tanggal>=? AND tanggal<=?"
                    + " ORDER BY tanggal DESC, id DESC"
                    + (limit > 0 ? " LIMIT " + limit : "");
            c = getReadableDatabase().rawQuery(sql, new String[]{from, to});
            while (c.moveToNext()) {
                Tx t = new Tx();
                t.id = c.getLong(0);
                t.tipe = c.getString(1);
                t.nominal = c.getLong(2);
                t.kategori = c.getString(3);
                t.tanggal = c.getString(4);
                t.catatan = c.getString(5);
                out.add(t);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    // ============ agregasi per kategori+tipe (profil: ringkasan tabungan) ============

    public long sumByCatTipe(String tipe, String kategori, String from, String to) {
        long out = 0;
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(nominal),0) FROM " + T
                    + " WHERE tipe=? AND kategori=? AND tanggal>=? AND tanggal<=?",
                    new String[]{tipe, kategori, from, to});
            if (c.moveToFirst()) out = c.getLong(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    /** Semua transaksi (untuk backup). */
    public List<Tx> listAllTx() {
        return listTx("0000-01-01", "9999-12-31", 0);
    }

    /** Semua settings (untuk backup). */
    public Map<String, String> getAllSettings() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery("SELECT k, v FROM settings", null);
            while (c.moveToNext()) m.put(c.getString(0), c.getString(1));
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return m;
    }

    /**
     * Restore penuh dari JSON backup. Format:
     * {app:"dompetku", transaksi:[...], ai_words:[...], settings:[...],
     *  budget_items:[...], savings_goal:{...}|null}
     * @return null bila sukses, pesan error bila gagal.
     */
    public String restoreFromJson(org.json.JSONObject root) {
        try {
            if (root == null || !"dompetku".equals(root.optString("app")))
                return "File bukan backup DompetKu";
            org.json.JSONArray txs = root.optJSONArray("transaksi");
            if (txs == null) return "Format backup tidak valid";
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                db.delete(T, null, null);
                db.delete("ai_words", null, null);
                db.delete("settings", null, null);
                db.delete("budget_items", null, null);
                db.delete("savings_goal", null, null);
                long now = System.currentTimeMillis();
                for (int i = 0; i < txs.length(); i++) {
                    org.json.JSONObject o = txs.optJSONObject(i);
                    if (o == null) continue;
                    ContentValues v = new ContentValues();
                    String tipe = o.optString("tipe", "out");
                    if (!"in".equals(tipe)) tipe = "out";
                    v.put("tipe", tipe);
                    v.put("nominal", o.optLong("nominal", 0));
                    String kat = o.optString("kategori", "Lainnya");
                    if (kat == null || kat.isEmpty()) kat = "Lainnya";
                    v.put("kategori", kat);
                    v.put("tanggal", o.optString("tanggal", "2000-01-01"));
                    v.put("catatan", o.optString("catatan", ""));
                    v.put("created_at", o.optLong("created_at", now));
                    db.insert(T, null, v);
                }
                org.json.JSONArray words = root.optJSONArray("ai_words");
                if (words != null) {
                    for (int i = 0; i < words.length(); i++) {
                        org.json.JSONObject o = words.optJSONObject(i);
                        if (o == null) continue;
                        ContentValues v = new ContentValues();
                        v.put("word", o.optString("word", ""));
                        v.put("tipe", o.optString("tipe", "out"));
                        v.put("kategori", o.optString("kategori", "Lainnya"));
                        v.put("weight", o.optInt("weight", 1));
                        db.insertWithOnConflict("ai_words", null, v,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
                org.json.JSONArray settings = root.optJSONArray("settings");
                if (settings != null) {
                    for (int i = 0; i < settings.length(); i++) {
                        org.json.JSONObject o = settings.optJSONObject(i);
                        if (o == null) continue;
                        ContentValues v = new ContentValues();
                        v.put("k", o.optString("k", ""));
                        v.put("v", o.optString("v", ""));
                        db.insertWithOnConflict("settings", null, v,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
                org.json.JSONArray bi = root.optJSONArray("budget_items");
                if (bi != null) {
                    for (int i = 0; i < bi.length(); i++) {
                        org.json.JSONObject o = bi.optJSONObject(i);
                        if (o == null) continue;
                        ContentValues v = new ContentValues();
                        v.put("kategori", o.optString("kategori", ""));
                        v.put("persen", o.optDouble("persen", 0));
                        db.insertWithOnConflict("budget_items", null, v,
                                SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
                org.json.JSONObject g = root.optJSONObject("savings_goal");
                if (g != null) {
                    ContentValues v = new ContentValues();
                    v.put("id", 1);
                    v.put("target", g.optLong("target", 0));
                    v.put("deadline", g.optString("deadline", ""));
                    v.put("start", g.optString("start", ""));
                    v.put("salary", g.optLong("salary", 0));
                    v.put("created_at", now);
                    db.insertWithOnConflict("savings_goal", null, v,
                            SQLiteDatabase.CONFLICT_REPLACE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return null;
        } catch (Throwable t) {
            return "Gagal restore: " + (t.getMessage() == null ? "error" : t.getMessage());
        }
    }

    // ============ AI: kata yang dipelajari dari koreksi ============

    public static class Word {
        public String word;
        public String tipe;
        public String kategori;
        public int weight;
    }

    /** Simpan/tambah bobot kata hasil koreksi user. */
    public void learnWord(String word, String tipe, String kategori) {
        Cursor c = null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            c = db.rawQuery("SELECT weight FROM ai_words WHERE word=?", new String[]{word});
            ContentValues v = new ContentValues();
            v.put("word", word);
            v.put("tipe", tipe);
            v.put("kategori", kategori);
            if (c.moveToFirst()) {
                int w = Math.min(10, c.getInt(0) + 1);
                v.put("weight", w);
                db.update("ai_words", v, "word=?", new String[]{word});
            } else {
                v.put("weight", 1);
                db.insert("ai_words", null, v);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
    }

    public List<Word> getLearnedWords() {
        List<Word> out = new ArrayList<Word>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT word, tipe, kategori, weight FROM ai_words", null);
            while (c.moveToNext()) {
                Word w = new Word();
                w.word = c.getString(0);
                w.tipe = c.getString(1);
                w.kategori = c.getString(2);
                w.weight = c.getInt(3);
                out.add(w);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    // ============ settings generik ============

    public void setSetting(String k, String v) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("k", k);
            cv.put("v", v);
            db.insertWithOnConflict("settings", null, cv,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable ignored) {}
    }

    public String getSetting(String k, String def) {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT v FROM settings WHERE k=?", new String[]{k});
            if (c.moveToFirst()) return c.getString(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return def;
    }

    // ============ budget plan ============

    public void setBudgetItem(String kategori, double persen) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("kategori", kategori);
            cv.put("persen", persen);
            db.insertWithOnConflict("budget_items", null, cv,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable ignored) {}
    }

    public Map<String, Double> getBudgetItems() {
        Map<String, Double> m = new LinkedHashMap<String, Double>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT kategori, persen FROM budget_items", null);
            while (c.moveToNext()) m.put(c.getString(0), c.getDouble(1));
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return m;
    }

    public void clearBudget() {
        try {
            getWritableDatabase().delete("budget_items", null, null);
        } catch (Throwable ignored) {}
    }

    // ============ savings goal ============

    public void setSavingsGoal(long target, String deadline, String start, long salary) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("id", 1);
            cv.put("target", target);
            cv.put("deadline", deadline);
            cv.put("start", start);
            cv.put("salary", salary);
            cv.put("created_at", System.currentTimeMillis());
            db.insertWithOnConflict("savings_goal", null, cv,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable ignored) {}
    }

    /** @return map target/deadline/start/salary atau null bila belum ada */
    public Map<String, String> getSavingsGoal() {
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT target, deadline, start, salary FROM savings_goal WHERE id=1", null);
            if (c.moveToFirst()) {
                Map<String, String> m = new LinkedHashMap<String, String>();
                m.put("target", c.getString(0));
                m.put("deadline", c.getString(1));
                m.put("start", c.getString(2));
                m.put("salary", c.getString(3));
                return m;
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return null;
    }

    public void clearSavingsGoal() {
        try {
            getWritableDatabase().delete("savings_goal", null, null);
        } catch (Throwable ignored) {}
    }

    /** Rata-rata pengeluaran bulanan selama n bulan terakhir. */
    public long avgMonthlyOut(int months) {
        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long total = 0;
            int n = 0;
            for (int i = 0; i < months; i++) {
                java.util.Calendar c = (java.util.Calendar) now.clone();
                c.add(java.util.Calendar.MONTH, -i);
                int y = c.get(java.util.Calendar.YEAR);
                int m = c.get(java.util.Calendar.MONTH) + 1;
                String from = String.format(java.util.Locale.US, "%04d-%02d-01", y, m);
                c.set(java.util.Calendar.DAY_OF_MONTH,
                        c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                String to = f.format(c.getTime());
                long out = sumByTipe("out", from, to);
                if (out > 0) { total += out; n++; }
            }
            return n > 0 ? total / n : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** Jumlah hari unik yang ada transaksinya dalam rentang. */
    public int distinctActiveDays(String from, String to) {
        int n = 0;
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT COUNT(DISTINCT tanggal) FROM " + T
                    + " WHERE tanggal>=? AND tanggal<=?",
                    new String[]{from, to});
            if (c.moveToFirst()) n = c.getInt(0);
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return n;
    }

    /** Agregasi nominal per token kata pada catatan, utk analisis spesifik. */
    public Map<String, Long> sumOutByToken(String from, String to, String kategori) {
        Map<String, Long> m = new LinkedHashMap<String, Long>();
        Cursor c = null;
        try {
            c = getReadableDatabase().rawQuery(
                    "SELECT catatan, nominal FROM " + T
                    + " WHERE tipe='out' AND kategori=? AND tanggal>=? AND tanggal<=?",
                    new String[]{kategori, from, to});
            while (c.moveToNext()) {
                String note = c.getString(0);
                long nom = c.getLong(1);
                if (note == null) continue;
                for (String w : SmartCategorizer.tokens(note)) {
                    if (w.length() < 3) continue;
                    Long cur = m.get(w);
                    m.put(w, (cur == null ? 0 : cur) + nom);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) try { c.close(); } catch (Throwable ignored) {}
        }
        return m;
    }
    public long avgMonthlySurplus(int months) {
        try {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat f =
                    new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long total = 0;
            int n = 0;
            for (int i = 0; i < months; i++) {
                java.util.Calendar c = (java.util.Calendar) now.clone();
                c.add(java.util.Calendar.MONTH, -i);
                int y = c.get(java.util.Calendar.YEAR);
                int m = c.get(java.util.Calendar.MONTH) + 1;
                String from = String.format(java.util.Locale.US, "%04d-%02d-01", y, m);
                c.set(java.util.Calendar.DAY_OF_MONTH,
                        c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
                String to = f.format(c.getTime());
                long inn = sumByTipe("in", from, to);
                long out = sumByTipe("out", from, to);
                if (inn > 0 || out > 0) {
                    total += (inn - out);
                    n++;
                }
            }
            return n > 0 ? total / n : 0;
        } catch (Throwable t) {
            return 0;
        }
    }
}
