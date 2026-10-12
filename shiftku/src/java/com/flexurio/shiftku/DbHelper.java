package com.flexurio.shiftku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** SQLite lokal: shift, jadwal, alarm, cuti, grup (cache), tukar shift. */
public class DbHelper extends SQLiteOpenHelper {
    private static final String DB = "shiftku.db";
    private static final int VER = 6;

    public DbHelper(Context c) { super(c, DB, null, VER); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE shifts(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT,start TEXT,end TEXT,color INTEGER,dayoff INTEGER)");
        db.execSQL("CREATE TABLE schedule(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT,shift_id INTEGER)");
        db.execSQL("CREATE TABLE alarms(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "shift_id INTEGER,time TEXT,enabled INTEGER)");
        db.execSQL("CREATE TABLE leaves(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "start_date TEXT,end_date TEXT,status TEXT,note TEXT)");
        db.execSQL("CREATE TABLE groups(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT,code TEXT,members TEXT,fb_id TEXT)");
        db.execSQL("CREATE TABLE swaps(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "group_code TEXT,date TEXT,shift_id INTEGER,from_user TEXT,to_user TEXT,status TEXT,reason TEXT)");
        db.execSQL("CREATE TABLE absen(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT,time TEXT,type TEXT,photo TEXT,lat REAL,lon REAL,synced INTEGER DEFAULT 0," +
                "loc_name TEXT)");
        db.execSQL("CREATE TABLE aktivitas(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT,time TEXT,text TEXT)");
        db.execSQL("CREATE TABLE lembur(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date TEXT,start_time TEXT,end_time TEXT,reason TEXT,status TEXT,created_by TEXT)");
        db.execSQL("CREATE TABLE pengumuman(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT,body TEXT,date TEXT,read INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE izin(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT,start_date TEXT,end_date TEXT,reason TEXT,photo TEXT,status TEXT)");
        seed(db);
    }

    private void seed(SQLiteDatabase db) {
        insertShift(db, "Pagi", "07:00", "15:00", 0xFFF87171, 0);
        insertShift(db, "Siang", "15:00", "23:00", 0xFFFBBF24, 0);
        insertShift(db, "Malam", "23:00", "07:00", 0xFF6366F1, 0);
        insertShift(db, "Libur", "", "", 0xFF34D399, 1);
        insertShift(db, "Off", "", "", 0xFF9CA3AF, 1);
    }

    private void insertShift(SQLiteDatabase db, String n, String s, String e, int c, int off) {
        ContentValues v = new ContentValues();
        v.put("name", n); v.put("start", s); v.put("end", e);
        v.put("color", c); v.put("dayoff", off);
        db.insert("shifts", null, v);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) {
            try { db.execSQL("ALTER TABLE groups ADD COLUMN fb_id TEXT"); }
            catch (Exception ignored) {}
        }
        if (o < 3) {
            try { db.execSQL("CREATE TABLE absen(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT,time TEXT,type TEXT,photo TEXT,lat REAL,lon REAL,synced INTEGER DEFAULT 0," +
                    "loc_name TEXT)"); }
            catch (Exception ignored) {}
        }
        if (o < 4) {
            try { db.execSQL("ALTER TABLE swaps ADD COLUMN reason TEXT"); }
            catch (Exception ignored) {}
        }
        if (o < 5) {
            try { db.execSQL("ALTER TABLE absen ADD COLUMN loc_name TEXT"); }
            catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS aktivitas(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT,time TEXT,text TEXT)"); }
            catch (Exception ignored) {}
        }
        if (o < 6) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS lembur(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "date TEXT,start_time TEXT,end_time TEXT,reason TEXT,status TEXT,created_by TEXT)"); }
            catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS pengumuman(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT,body TEXT,date TEXT,read INTEGER DEFAULT 0)"); }
            catch (Exception ignored) {}
            try { db.execSQL("CREATE TABLE IF NOT EXISTS izin(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "type TEXT,start_date TEXT,end_date TEXT,reason TEXT,photo TEXT,status TEXT)"); }
            catch (Exception ignored) {}
        }
    }

    // ---- shift ----
    public List<Shift> getShifts() {
        List<Shift> out = new ArrayList<Shift>();
        Cursor cu = getReadableDatabase().rawQuery("SELECT * FROM shifts ORDER BY id", null);
        while (cu.moveToNext()) {
            out.add(new Shift(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getInt(4), cu.getInt(5) == 1));
        }
        cu.close();
        return out;
    }

    public Shift getShift(long id) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM shifts WHERE id=?", new String[]{String.valueOf(id)});
        Shift s = null;
        if (cu.moveToFirst()) {
            s = new Shift(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getInt(4), cu.getInt(5) == 1);
        }
        cu.close();
        return s;
    }

    public long addShift(String n, String s, String e, int c, boolean off) {
        ContentValues v = new ContentValues();
        v.put("name", n); v.put("start", s); v.put("end", e);
        v.put("color", c); v.put("dayoff", off ? 1 : 0);
        return getWritableDatabase().insert("shifts", null, v);
    }

    public void deleteShift(long id) {
        getWritableDatabase().delete("shifts", "id=?", new String[]{String.valueOf(id)});
    }

    // ---- jadwal ----
    public void setSchedule(String date, long shiftId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("schedule", "date=?", new String[]{date});
        if (shiftId > 0) {
            ContentValues v = new ContentValues();
            v.put("date", date); v.put("shift_id", shiftId);
            db.insert("schedule", null, v);
        }
    }

    public long getScheduleShift(String date) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT shift_id FROM schedule WHERE date=?", new String[]{date});
        long id = 0;
        if (cu.moveToFirst()) id = cu.getLong(0);
        cu.close();
        return id;
    }

    // ---- alarm ----
    public List<AlarmItem> getAlarms() {
        List<AlarmItem> out = new ArrayList<AlarmItem>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT a.id,a.shift_id,a.time,a.enabled,s.name FROM alarms a " +
                "LEFT JOIN shifts s ON s.id=a.shift_id ORDER BY a.time", null);
        while (cu.moveToNext()) {
            out.add(new AlarmItem(cu.getLong(0), cu.getLong(1), cu.getString(2),
                    cu.getInt(3) == 1, cu.getString(4)));
        }
        cu.close();
        return out;
    }

    public long addAlarm(long shiftId, String time) {
        ContentValues v = new ContentValues();
        v.put("shift_id", shiftId); v.put("time", time); v.put("enabled", 1);
        return getWritableDatabase().insert("alarms", null, v);
    }

    public void setAlarmEnabled(long id, boolean en) {
        ContentValues v = new ContentValues();
        v.put("enabled", en ? 1 : 0);
        getWritableDatabase().update("alarms", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAlarm(long id) {
        getWritableDatabase().delete("alarms", "id=?", new String[]{String.valueOf(id)});
    }

    // ---- cuti ----
    public static class Leave {
        public long id; public String start, end, status, note;
        public Leave(long id, String s, String e, String st, String n) {
            this.id = id; start = s; end = e; status = st; note = n;
        }
    }

    public List<Leave> getLeaves() {
        List<Leave> out = new ArrayList<Leave>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM leaves ORDER BY start_date DESC", null);
        while (cu.moveToNext()) {
            out.add(new Leave(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getString(4)));
        }
        cu.close();
        return out;
    }

    public long addLeave(String s, String e, String note) {
        ContentValues v = new ContentValues();
        v.put("start_date", s); v.put("end_date", e);
        v.put("status", "Menunggu"); v.put("note", note);
        return getWritableDatabase().insert("leaves", null, v);
    }

    public void deleteLeave(long id) {
        getWritableDatabase().delete("leaves", "id=?", new String[]{String.valueOf(id)});
    }

    /** Total hari cuti terpakai (Disetujui + Menunggu) pada tahun berjalan. */
    public int usedLeaveDays(int year) {
        int total = 0;
        for (Leave l : getLeaves()) {
            if (l.start.startsWith(String.valueOf(year))) {
                total += DateUtil.daysBetween(l.start, l.end);
            }
        }
        return total;
    }

    /** Apakah tanggal tsb tercakup cuti berstatus Disetujui? */
    public boolean isLeaveOn(String date) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT id FROM leaves WHERE status='Disetujui' AND start_date<=? AND end_date>=? LIMIT 1",
                new String[]{date, date});
        boolean has = cu.moveToFirst();
        cu.close();
        return has;
    }

    // ---- grup (cache lokal) ----
    public static class Group {
        public long id; public String name, code, members, fbId;
        public Group(long id, String n, String c, String m, String f) {
            this.id = id; name = n; code = c; members = m; fbId = f;
        }
    }

    public List<Group> getGroups() {
        List<Group> out = new ArrayList<Group>();
        Cursor cu = getReadableDatabase().rawQuery("SELECT * FROM groups ORDER BY id DESC", null);
        while (cu.moveToNext()) {
            String fb = cu.getColumnCount() > 4 ? cu.getString(4) : null;
            out.add(new Group(cu.getLong(0), cu.getString(1), cu.getString(2), cu.getString(3), fb));
        }
        cu.close();
        return out;
    }

    public long addGroup(String name, String code, String members) {
        return addGroup(name, code, members, null);
    }

    public long addGroup(String name, String code, String members, String fbId) {
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("code", code); v.put("members", members);
        v.put("fb_id", fbId);
        return getWritableDatabase().insert("groups", null, v);
    }

    public void deleteGroup(long id) {
        getWritableDatabase().delete("groups", "id=?", new String[]{String.valueOf(id)});
    }

    // ---- tukar shift ----
    public static class Swap {
        public long id; public String groupCode, date, fromUser, toUser, status, reason;
        public long shiftId;
        public Swap(long id, String g, String d, long s, String f, String t, String st) {
            this(id, g, d, s, f, t, st, "");
        }
        public Swap(long id, String g, String d, long s, String f, String t, String st, String r) {
            this.id = id; groupCode = g; date = d; shiftId = s;
            fromUser = f; toUser = t; status = st; reason = r == null ? "" : r;
        }
    }

    public List<Swap> getSwaps(String groupCode) {
        List<Swap> out = new ArrayList<Swap>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM swaps WHERE group_code=? ORDER BY id DESC",
                new String[]{groupCode});
        while (cu.moveToNext()) {
            String reason = "";
            try { reason = cu.getString(cu.getColumnIndexOrThrow("reason")); }
            catch (Exception ignored) {}
            if (reason == null) reason = "";
            out.add(new Swap(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getLong(3), cu.getString(4), cu.getString(5), cu.getString(6), reason));
        }
        cu.close();
        return out;
    }

    public long addSwap(String g, String date, long shiftId, String from, String to) {
        return addSwap(g, date, shiftId, from, to, "");
    }

    public long addSwap(String g, String date, long shiftId, String from, String to, String reason) {
        ContentValues v = new ContentValues();
        v.put("group_code", g); v.put("date", date); v.put("shift_id", shiftId);
        v.put("from_user", from); v.put("to_user", to); v.put("status", "Menunggu");
        v.put("reason", reason == null ? "" : reason);
        return getWritableDatabase().insert("swaps", null, v);
    }

    public void setSwapStatus(long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("swaps", v, "id=?", new String[]{String.valueOf(id)});
    }

    public static class AlarmItem {
        public long id, shiftId; public String time, shiftName; public boolean enabled;
        public AlarmItem(long id, long sid, String t, boolean e, String n) {
            this.id = id; shiftId = sid; time = t; enabled = e; shiftName = n;
        }
    }

    // ---- absen selfie ----
    public static class Absen {
        public long id;
        public String date;   // yyyy-MM-dd
        public String time;   // HH:mm
        public String type;   // "Masuk" / "Pulang"
        public String photo;  // path file lokal
        public double lat, lon;
        public String locName; // nama daerah hasil reverse geocoding (bisa kosong)
        public boolean synced;
        public Absen(long id, String d, String t, String ty, String p,
                     double la, double lo, boolean s, String ln) {
            this.id = id; date = d; time = t; type = ty; photo = p;
            lat = la; lon = lo; synced = s; locName = (ln == null) ? "" : ln;
        }
    }

    public long addAbsen(String date, String time, String type, String photo,
                         double lat, double lon, String locName) {
        ContentValues v = new ContentValues();
        v.put("date", date); v.put("time", time); v.put("type", type);
        v.put("photo", photo); v.put("lat", lat); v.put("lon", lon);
        v.put("loc_name", locName == null ? "" : locName);
        v.put("synced", 0);
        return getWritableDatabase().insert("absen", null, v);
    }

    public List<Absen> getAbsen(int limit) {
        List<Absen> out = new ArrayList<Absen>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM absen ORDER BY id DESC LIMIT " + limit, null);
        int iLoc = cu.getColumnIndex("loc_name");
        while (cu.moveToNext()) {
            String ln = (iLoc >= 0) ? cu.getString(iLoc) : "";
            out.add(new Absen(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getString(4), cu.getDouble(5),
                    cu.getDouble(6), cu.getInt(7) == 1, ln));
        }
        cu.close();
        return out;
    }

    /** Apakah sudah absen tipe ini hari ini? */
    public boolean hasAbsenToday(String date, String type) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT id FROM absen WHERE date=? AND type=? LIMIT 1",
                new String[]{date, type});
        boolean has = cu.moveToFirst();
        cu.close();
        return has;
    }

    /** Jam (HH:mm:ss) absen pertama tipe ini pada tanggal tsb, "" bila belum ada. */
    public String getAbsenTimeToday(String date, String type) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT time FROM absen WHERE date=? AND type=? ORDER BY id ASC LIMIT 1",
                new String[]{date, type});
        String t = cu.moveToFirst() ? cu.getString(0) : "";
        cu.close();
        return t;
    }

    public void markAbsenSynced(long id) {
        ContentValues v = new ContentValues();
        v.put("synced", 1);
        getWritableDatabase().update("absen", v, "id=?",
                new String[]{String.valueOf(id)});
    }

    // ---- aktivitas kerja (timesheet ala MyActio) ----
    public static class Aktivitas {
        public long id;
        public String date;  // yyyy-MM-dd
        public String time;  // HH:mm:ss
        public String text;
        public Aktivitas(long id, String d, String t, String x) {
            this.id = id; date = d; time = t; text = x;
        }
    }

    public long addAktivitas(String date, String time, String text) {
        ContentValues v = new ContentValues();
        v.put("date", date); v.put("time", time); v.put("text", text);
        return getWritableDatabase().insert("aktivitas", null, v);
    }

    public List<Aktivitas> getAktivitas(String date) {
        List<Aktivitas> out = new ArrayList<Aktivitas>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM aktivitas WHERE date=? ORDER BY time DESC",
                new String[]{date});
        while (cu.moveToNext()) {
            out.add(new Aktivitas(cu.getLong(0), cu.getString(1),
                    cu.getString(2), cu.getString(3)));
        }
        cu.close();
        return out;
    }

    public void deleteAktivitas(long id) {
        getWritableDatabase().delete("aktivitas", "id=?",
                new String[]{String.valueOf(id)});
    }

    // ---- lembur ----
    public static class Lembur {
        public long id;
        public String date, startTime, endTime, reason, status, createdBy;
        public Lembur(long id, String d, String s, String e, String r, String st, String c) {
            this.id = id; date = d; startTime = s; endTime = e;
            reason = r; status = st; createdBy = c;
        }
        /** Durasi dalam jam (desimal). */
        public double hours() {
            try {
                String[] a = startTime.split(":");
                String[] b = endTime.split(":");
                int s = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
                int e = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
                if (e <= s) e += 24 * 60;
                return (e - s) / 60.0;
            } catch (Exception ex) { return 0; }
        }
    }

    public long addLembur(String date, String start, String end, String reason, String by) {
        ContentValues v = new ContentValues();
        v.put("date", date); v.put("start_time", start); v.put("end_time", end);
        v.put("reason", reason); v.put("status", "Menunggu"); v.put("created_by", by);
        return getWritableDatabase().insert("lembur", null, v);
    }

    public List<Lembur> getLembur() {
        List<Lembur> out = new ArrayList<Lembur>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM lembur ORDER BY date DESC, id DESC", null);
        while (cu.moveToNext()) {
            out.add(new Lembur(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getString(4), cu.getString(5), cu.getString(6)));
        }
        cu.close();
        return out;
    }

    public void setLemburStatus(long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("lembur", v, "id=?",
                new String[]{String.valueOf(id)});
    }

    public void deleteLembur(long id) {
        getWritableDatabase().delete("lembur", "id=?",
                new String[]{String.valueOf(id)});
    }

    /** Total jam lembur berstatus Disetujui pada bulan tsb (yyyy-MM). */
    public double lemburJamBulan(String ym) {
        double total = 0;
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT start_time, end_time FROM lembur WHERE substr(date,1,7)=? AND status='Disetujui'",
                new String[]{ym});
        while (cu.moveToNext()) {
            try {
                String[] a = cu.getString(0).split(":");
                String[] b = cu.getString(1).split(":");
                int s = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
                int e = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
                if (e <= s) e += 24 * 60;
                total += (e - s) / 60.0;
            } catch (Exception ignored) {}
        }
        cu.close();
        return total;
    }

    // ---- pengumuman ----
    public static class Pengumuman {
        public long id;
        public String title, body, date;
        public boolean read;
        public Pengumuman(long id, String t, String b, String d, boolean r) {
            this.id = id; title = t; body = b; date = d; read = r;
        }
    }

    public long addPengumuman(String title, String body, String date) {
        ContentValues v = new ContentValues();
        v.put("title", title); v.put("body", body); v.put("date", date); v.put("read", 0);
        return getWritableDatabase().insert("pengumuman", null, v);
    }

    public List<Pengumuman> getPengumuman() {
        List<Pengumuman> out = new ArrayList<Pengumuman>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM pengumuman ORDER BY id DESC", null);
        while (cu.moveToNext()) {
            out.add(new Pengumuman(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getInt(4) == 1));
        }
        cu.close();
        return out;
    }

    public void markPengumumanRead(long id) {
        ContentValues v = new ContentValues();
        v.put("read", 1);
        getWritableDatabase().update("pengumuman", v, "id=?",
                new String[]{String.valueOf(id)});
    }

    public int countPengumumanUnread() {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM pengumuman WHERE read=0", null);
        int n = cu.moveToFirst() ? cu.getInt(0) : 0;
        cu.close();
        return n;
    }

    public void deletePengumuman(long id) {
        getWritableDatabase().delete("pengumuman", "id=?",
                new String[]{String.valueOf(id)});
    }

    // ---- izin / sakit (terpisah dari cuti) ----
    public static class Izin {
        public long id;
        public String type; // "Izin" / "Sakit"
        public String startDate, endDate, reason, photo, status;
        public Izin(long id, String t, String s, String e, String r, String p, String st) {
            this.id = id; type = t; startDate = s; endDate = e;
            reason = r; photo = p; status = st;
        }
    }

    public long addIzin(String type, String start, String end, String reason, String photo) {
        ContentValues v = new ContentValues();
        v.put("type", type); v.put("start_date", start); v.put("end_date", end);
        v.put("reason", reason); v.put("photo", photo == null ? "" : photo);
        v.put("status", "Menunggu");
        return getWritableDatabase().insert("izin", null, v);
    }

    public List<Izin> getIzin() {
        List<Izin> out = new ArrayList<Izin>();
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT * FROM izin ORDER BY id DESC", null);
        while (cu.moveToNext()) {
            out.add(new Izin(cu.getLong(0), cu.getString(1), cu.getString(2),
                    cu.getString(3), cu.getString(4), cu.getString(5), cu.getString(6)));
        }
        cu.close();
        return out;
    }

    /** Apakah tanggal tsb tercakup izin/sakit berstatus Disetujui? Kembalikan tipenya atau "". */
    public String izinTypeOn(String date) {
        Cursor cu = getReadableDatabase().rawQuery(
                "SELECT type FROM izin WHERE status='Disetujui' AND start_date<=? AND end_date>=? LIMIT 1",
                new String[]{date, date});
        String t = cu.moveToFirst() ? cu.getString(0) : "";
        cu.close();
        return t;
    }

    public void setIzinStatus(long id, String status) {
        ContentValues v = new ContentValues();
        v.put("status", status);
        getWritableDatabase().update("izin", v, "id=?",
                new String[]{String.valueOf(id)});
    }

    public void deleteIzin(long id) {
        getWritableDatabase().delete("izin", "id=?",
                new String[]{String.valueOf(id)});
    }
}
