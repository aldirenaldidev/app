package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Absensi — kehadiran dengan selfie (seperti ide Presensi EOS PHE).
 *
 * Alur: pilih tipe (Masuk/Pulang) → ambil selfie (kamera depan, WAJIB,
 * tidak bisa dari galeri) → lokasi GPS + timestamp otomatis →
 * simpan lokal + sinkron metadata ke Firestore (bila login & ada grup).
 *
 * Mode tim: dibuka dengan extra "fbId" oleh Leader/Admin untuk melihat
 * absensi anggota dari Firestore.
 */
public class AbsenActivity extends Activity {

    private static final int REQ_CAMERA = 9001;
    private static final int REQ_PERM = 9002;

    private DbHelper db;
    private String fbId;           // null = mode pribadi
    private String tipe = "Masuk";
    private String photoPath;      // path foto selfie terakhir
    private double lat = 0, lon = 0;
    private String locName = "";   // nama daerah hasil reverse geocoding
    private boolean locDone = false;

    private ImageView ivPreview;
    private TextView tvLoc, tvTime, tvTipeMasuk, tvTipePulang;
    private LinearLayout listWrap;
    private com.google.firebase.firestore.ListenerRegistration absenReg;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        fbId = getIntent().getStringExtra("fbId");
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (absenReg != null) absenReg.remove(); } catch (Throwable ignored) {}
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this,
                fbId != null ? "Absensi Tim" : "Absensi"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));

        if (fbId != null) {
            buildTeamView(w);
        } else {
            buildQuickActions(w);
            buildAbsenForm(w);
            TextView histTitle = Ui.text(this, "Aktivitas Terakhir", 16, Ui.TEXT, true);
            histTitle.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 8));
            w.addView(histTitle);
            listWrap = new LinearLayout(this);
            listWrap.setOrientation(LinearLayout.VERTICAL);
            w.addView(listWrap);
            renderHistory();
        }

        sv.addView(w);
        root.addView(sv);
        setContentView(root);
        fetchLocation();
    }

    // ================= FORM ABSEN =================

    /**
     * 3 tombol lingkaran aksi cepat: Check In (selfie) | Tambah Aktivitas | Check Out (selfie).
     *
     * Logika terkunci:
     * - Check Out NONAKTIF (greyed out) sampai user sudah Check In hari ini;
     *   bila diklik sebelum waktunya -> hint "Absen masuk terlebih dahulu".
     * - Setelah keduanya selesai, tampil ringkasan (jam masuk, jam pulang, durasi).
     * - Reset otomatis keesokan harinya (semua berbasis tanggal hari ini).
     */
    private void buildQuickActions(LinearLayout w) {
        final String today = DateUtil.todayYmd();
        final boolean masukDone = db.hasAbsenToday(today, "Masuk");
        final boolean pulangDone = db.hasAbsenToday(today, "Pulang");

        LinearLayout card = Ui.card(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 2));

        // Check In.
        LinearLayout btnIn = circleAction("\u2713", "Check In",
                masukDone ? 0xFFB0B4C0 : Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) {
                if (masukDone) {
                    Toast.makeText(AbsenActivity.this,
                            "Sudah absen masuk hari ini (" + db.getAbsenTimeToday(today, "Masuk") + ").",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                tipe = "Masuk";
                refreshTipe();
                ambilSelfie();
                Toast.makeText(AbsenActivity.this,
                        "Check In: ambil selfie, lalu Simpan Absen.", Toast.LENGTH_SHORT).show();
            }
        });
        if (masukDone) btnIn.setAlpha(0.55f);
        row.addView(btnIn);

        // Tambah Aktivitas.
        row.addView(circleAction("+", "Tambah\nAktivitas", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { showAddAktivitasDialog(); }
        }));

        // Check Out — terkunci sampai Check In selesai hari ini.
        final boolean outLocked = !masukDone || pulangDone;
        LinearLayout btnOut = circleAction("\u2717", "Check Out",
                outLocked ? 0xFFB0B4C0 : Ui.RED, new View.OnClickListener() {
            public void onClick(View v) {
                if (!masukDone) {
                    Toast.makeText(AbsenActivity.this,
                            "Absen masuk terlebih dahulu sebelum absen pulang.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pulangDone) {
                    Toast.makeText(AbsenActivity.this,
                            "Sudah absen pulang hari ini (" + db.getAbsenTimeToday(today, "Pulang") + ").",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                tipe = "Pulang";
                refreshTipe();
                ambilSelfie();
                Toast.makeText(AbsenActivity.this,
                        "Check Out: ambil selfie, lalu Simpan Absen.", Toast.LENGTH_SHORT).show();
            }
        });
        if (outLocked) btnOut.setAlpha(0.55f);
        row.addView(btnOut);

        card.addView(row);

        // Hint saat Check Out terkunci.
        if (!masukDone) {
            TextView lockHint = Ui.text(this,
                    "\uD83D\uDD12 Check Out aktif setelah Anda absen masuk.",
                    12, Ui.TEXT_DIM, false);
            lockHint.setGravity(Gravity.CENTER);
            lockHint.setPadding(0, Ui.dp(this, 4), 0, 0);
            card.addView(lockHint);
        }

        w.addView(card);

        // Ringkasan bila keduanya sudah selesai hari ini.
        if (masukDone && pulangDone) {
            w.addView(buildRingkasanCard(today));
        }
    }

    /** Kartu ringkasan: jam masuk, jam pulang, durasi kerja hari ini. */
    private LinearLayout buildRingkasanCard(String today) {
        String tIn = db.getAbsenTimeToday(today, "Masuk");
        String tOut = db.getAbsenTimeToday(today, "Pulang");
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Ringkasan Hari Ini", 16, Ui.TEXT, true));
        TextView r = Ui.text(this,
                "Masuk: " + singkat(tIn) + "   •   Pulang: " + singkat(tOut),
                14, Ui.TEXT, false);
        r.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(r);
        String dur = hitungDurasi(tIn, tOut);
        if (!dur.isEmpty()) {
            TextView d = Ui.text(this, "Durasi kerja: " + dur, 14, Ui.PRIMARY, true);
            d.setPadding(0, Ui.dp(this, 4), 0, 0);
            card.addView(d);
        }
        TextView done = Ui.text(this, "Absensi hari ini selesai ✅",
                13, Ui.GREEN, true);
        done.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(done);
        return card;
    }

    /** "08:35:50" -> "08:35". */
    private static String singkat(String t) {
        if (t == null) return "-";
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }

    /** "08:35:50" & "17:05:10" -> "8 jam 30 mnt". */
    private static String hitungDurasi(String tIn, String tOut) {
        try {
            String[] a = tIn.split(":");
            String[] b = tOut.split(":");
            int mIn = Integer.parseInt(a[0]) * 60 + Integer.parseInt(a[1]);
            int mOut = Integer.parseInt(b[0]) * 60 + Integer.parseInt(b[1]);
            int d = mOut - mIn;
            if (d < 0) d = 0;
            int h = d / 60, m = d % 60;
            if (h > 0 && m > 0) return h + " jam " + m + " mnt";
            if (h > 0) return h + " jam";
            return m + " mnt";
        } catch (Throwable ignored) { return ""; }
    }

    /** Satu tombol lingkaran: ikon besar di atas, label di bawah. */
    private LinearLayout circleAction(String icon, String label, int color,
                                      View.OnClickListener onClick) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(clp);

        TextView circ = new TextView(this);
        circ.setText(icon);
        circ.setTextSize(26);
        circ.setTextColor(0xFFFFFFFF);
        circ.setGravity(Gravity.CENTER);
        int s = Ui.dp(this, 64);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(s, s);
        slp.gravity = Gravity.CENTER_HORIZONTAL;
        circ.setLayoutParams(slp);
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(color);
        bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circ.setBackground(bg);
        // Shadow lembut.
        circ.setElevation(Ui.dp(this, 4));
        col.addView(circ);

        TextView lb = Ui.text(this, label, 12, Ui.TEXT, true);
        lb.setGravity(Gravity.CENTER);
        lb.setPadding(0, Ui.dp(this, 6), 0, 0);
        col.addView(lb);

        col.setClickable(true);
        col.setFocusable(true);
        col.setOnClickListener(onClick);
        // Ripple sederhana: tekan -> sedikit transparan.
        col.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent e) {
                if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) v.setAlpha(0.6f);
                else if (e.getAction() == android.view.MotionEvent.ACTION_UP
                        || e.getAction() == android.view.MotionEvent.ACTION_CANCEL) v.setAlpha(1f);
                return false;
            }
        });
        return col;
    }

    /** Dialog catat aktivitas kerja di antara check in/out. */
    private void showAddAktivitasDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Tambah Aktivitas", 18, Ui.TEXT, true));
        TextView hint = Ui.text(this, "Catat aktivitas kerja Anda hari ini.",
                13, Ui.TEXT_DIM, false);
        hint.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        w.addView(hint);

        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("mis. Maintenance PC client A");
        et.setMinLines(2);
        w.addView(et);

        TextView simpan = Ui.primaryButton(this, "Simpan");
        simpan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String t = et.getText().toString().trim();
                if (t.isEmpty()) {
                    Toast.makeText(AbsenActivity.this, "Isi dulu aktivitasnya.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String date = DateUtil.todayYmd();
                String time = new java.text.SimpleDateFormat("HH:mm:ss",
                        Locale.US).format(new java.util.Date());
                db.addAktivitas(date, time, t);
                d.dismiss();
                renderHistory();
                Toast.makeText(AbsenActivity.this, "Aktivitas tersimpan ✅",
                        Toast.LENGTH_SHORT).show();
            }
        });
        w.addView(simpan);
        d.setContentView(w);
        d.show();
    }

    private void buildAbsenForm(LinearLayout w) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Absen Hari Ini", 16, Ui.TEXT, true));

        // Default cerdas: bila sudah masuk tapi belum pulang, pilih Pulang.
        String today0 = DateUtil.todayYmd();
        if (db.hasAbsenToday(today0, "Masuk") && !db.hasAbsenToday(today0, "Pulang")) {
            tipe = "Pulang";
        }

        // Pemilih tipe Masuk/Pulang
        LinearLayout tipeRow = new LinearLayout(this);
        tipeRow.setOrientation(LinearLayout.HORIZONTAL);
        tipeRow.setPadding(0, Ui.dp(this, 8), 0, 0);
        tvTipeMasuk = tipeChip("Masuk");
        tvTipePulang = tipeChip("Pulang");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cp.rightMargin = Ui.dp(this, 8);
        tvTipeMasuk.setLayoutParams(cp);
        tvTipePulang.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tipeRow.addView(tvTipeMasuk);
        tipeRow.addView(tvTipePulang);
        card.addView(tipeRow);
        refreshTipe();

        // Waktu sekarang
        tvTime = Ui.text(this, "", 13, Ui.TEXT_DIM, false);
        tvTime.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(tvTime);
        updateTime();

        // Preview selfie
        ivPreview = new ImageView(this);
        int ps = Ui.dp(this, 180);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ps, ps);
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        plp.topMargin = Ui.dp(this, 10);
        plp.bottomMargin = Ui.dp(this, 6);
        ivPreview.setLayoutParams(plp);
        ivPreview.setBackground(Ui.roundBg(0xFFE8EAF2, 12, this));
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(ivPreview);

        TextView btnCam = Ui.outlineButton(this, "📷 Ambil Selfie");
        btnCam.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { ambilSelfie(); }
        });
        card.addView(btnCam);
        TextView hint = Ui.text(this,
                "Selfie wajib dari kamera depan — tidak bisa dari galeri.",
                12, Ui.TEXT_DIM, false);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Ui.dp(this, 4), 0, 0);
        card.addView(hint);

        // Lokasi
        tvLoc = Ui.text(this, "📍 Mencari lokasi…", 13, Ui.TEXT_DIM, false);
        tvLoc.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(tvLoc);

        TextView btnSave = Ui.primaryButton(this, "Simpan Absen");
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { simpanAbsen(); }
        });
        card.addView(btnSave);

        // Status absen hari ini
        String today = DateUtil.todayYmd();
        StringBuilder sb = new StringBuilder("Hari ini: ");
        sb.append(db.hasAbsenToday(today, "Masuk") ? "✅ Masuk" : "⬜ Masuk");
        sb.append("   ");
        sb.append(db.hasAbsenToday(today, "Pulang") ? "✅ Pulang" : "⬜ Pulang");
        TextView st = Ui.text(this, sb.toString(), 13, Ui.TEXT_DIM, false);
        st.setPadding(0, Ui.dp(this, 8), 0, 0);
        card.addView(st);

        w.addView(card);
    }

    private TextView tipeChip(final String t) {
        final TextView tv = Ui.text(this, t, 15, Ui.TEXT_DIM, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        tv.setClickable(true);
        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { tipe = t; refreshTipe(); }
        });
        return tv;
    }

    private void refreshTipe() {
        boolean masuk = "Masuk".equals(tipe);
        tvTipeMasuk.setBackground(Ui.roundBg(masuk ? Ui.PRIMARY : 0xFFE8EAF2, 10, this));
        tvTipeMasuk.setTextColor(masuk ? 0xFFFFFFFF : Ui.TEXT_DIM);
        tvTipePulang.setBackground(Ui.roundBg(!masuk ? Ui.PRIMARY : 0xFFE8EAF2, 10, this));
        tvTipePulang.setTextColor(!masuk ? 0xFFFFFFFF : Ui.TEXT_DIM);
    }

    private void updateTime() {
        String now = new SimpleDateFormat("EEEE, d MMM yyyy • HH:mm",
                new Locale("id", "ID")).format(new Date());
        tvTime.setText("🕐 " + now);
    }

    // ================= KAMERA =================

    private void ambilSelfie() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, REQ_PERM);
            return;
        }
        openCamera();
    }

    private void openCamera() {
        try {
            File dir = getExternalCacheDir();
            if (dir == null) dir = getCacheDir();
            String name = "selfie_" + System.currentTimeMillis() + ".jpg";
            File f = new File(dir, name);
            photoPath = f.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this,
                    "com.flexurio.shiftku.fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            // Minta kamera depan bila tersedia.
            i.putExtra("android.intent.extras.CAMERA_FACING", 1);
            i.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Throwable t) {
            Toast.makeText(this, "Kamera tidak tersedia: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_CAMERA && res == RESULT_OK && photoPath != null) {
            File f = new File(photoPath);
            if (f.exists()) {
                Bitmap bm = BitmapFactory.decodeFile(photoPath);
                if (bm != null) {
                    // Thumbnail agar hemat memori.
                    int s = Math.max(1, Math.min(bm.getWidth(), bm.getHeight()) / 360);
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inSampleSize = s;
                    Bitmap th = BitmapFactory.decodeFile(photoPath, o);
                    ivPreview.setImageBitmap(th != null ? th : bm);
                }
                Toast.makeText(this, "Selfie diambil ✅", Toast.LENGTH_SHORT).show();
            } else {
                photoPath = null;
                Toast.makeText(this, "Gagal mengambil foto.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override public void onRequestPermissionsResult(int req, String[] p, int[] g) {
        super.onRequestPermissionsResult(req, p, g);
        if (req == REQ_PERM && g.length > 0
                && g[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }

    // ================= LOKASI =================

    private void fetchLocation() {
        if (fbId != null) return; // mode tim tidak butuh lokasi
        try {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_PERM + 1);
                return;
            }
            startLoc();
        } catch (Throwable t) {
            setLocText("📍 Lokasi tidak tersedia");
        }
    }

    private void startLoc() {
        try {
            final LocationManager lm =
                    (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) { setLocText("📍 Lokasi tidak tersedia"); return; }
            Location last = null;
            try { last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); } catch (Throwable ignored) {}
            if (last == null) {
                try { last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); }
                catch (Throwable ignored) {}
            }
            if (last != null) setLoc(last);
            try {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER,
                        new LocationListener() {
                            public void onLocationChanged(Location l) { setLoc(l); }
                            public void onStatusChanged(String p, int s, Bundle e) {}
                            public void onProviderEnabled(String p) {}
                            public void onProviderDisabled(String p) {}
                        }, null);
            } catch (Throwable ignored) {}
        } catch (Throwable t) {
            setLocText("📍 Lokasi tidak tersedia");
        }
    }

    private void setLoc(Location l) {
        lat = l.getLatitude(); lon = l.getLongitude(); locDone = true;
        // Tampilkan koordinat dulu, lalu resolve nama daerah di background.
        setLocText(String.format(Locale.US, "📍 %.5f, %.5f", lat, lon));
        resolveLocName(lat, lon);
    }

    /**
     * Reverse geocoding: lat/long -> nama daerah yang mudah dibaca
     * (mis. "Jl. MH Thamrin, Jakarta Pusat"). Berjalan di thread background;
     * bila gagal (offline/tidak ada hasil), tetap tampil koordinat.
     */
    private void resolveLocName(final double la, final double lo) {
        new Thread(new Runnable() {
            public void run() {
                String name = "";
                try {
                    android.location.Geocoder g =
                            new android.location.Geocoder(AbsenActivity.this,
                                    new Locale("id", "ID"));
                    if (android.location.Geocoder.isPresent()) {
                        java.util.List<android.location.Address> list =
                                g.getFromLocation(la, lo, 1);
                        if (list != null && !list.isEmpty()) {
                            name = formatAddress(list.get(0));
                        }
                    }
                } catch (Throwable ignored) {}
                if (name != null && !name.isEmpty()) {
                    locName = name;
                    setLocText("📍 " + name);
                }
            }
        }).start();
    }

    /** Format: "Nama Jalan, Kota/Kabupaten" — mis. "Jl. MH Thamrin, Jakarta Pusat". */
    private static String formatAddress(android.location.Address a) {
        StringBuilder sb = new StringBuilder();
        String road = a.getThoroughfare();       // nama jalan
        String sub = a.getSubLocality();          // kecamatan/kelurahan
        String city = a.getSubAdminArea();        // kota/kabupaten
        if (road != null && !road.isEmpty()) sb.append(road);
        else if (sub != null && !sub.isEmpty()) sb.append(sub);
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (sb.length() == 0 && a.getLocality() != null) sb.append(a.getLocality());
        return sb.toString().trim();
    }

    private void setLocText(final String s) {
        runOnUiThread(new Runnable() {
            public void run() { if (tvLoc != null) tvLoc.setText(s); }
        });
    }

    // ================= SIMPAN =================

    private void simpanAbsen() {
        if (photoPath == null || !(new File(photoPath).exists())) {
            Toast.makeText(this, "Ambil selfie dulu sebelum menyimpan.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String date = DateUtil.todayYmd();
        // Cegah duplikat: satu tipe absen sekali sehari.
        if (db.hasAbsenToday(date, tipe)) {
            Toast.makeText(this, "Sudah absen " + tipe.toLowerCase() + " hari ini.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Logika terkunci: tidak bisa absen pulang sebelum absen masuk.
        if ("Pulang".equals(tipe) && !db.hasAbsenToday(date, "Masuk")) {
            Toast.makeText(this, "Absen masuk terlebih dahulu sebelum absen pulang.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        long id = db.addAbsen(date, time, tipe, photoPath, lat, lon, locName);

        // Sinkron metadata ke grup aktif bila login.
        String gid = currentGroupFbId();
        if (gid != null) {
            FirebaseBridge.saveAbsen(this, gid, date, time, tipe, lat, lon, locName,
                    new FirebaseBridge.FbCallback() {
                        public void onOk(String info) {
                            Toast.makeText(AbsenActivity.this,
                                    "Absen tersimpan ✅ (tersinkron)", Toast.LENGTH_SHORT).show();
                        }
                        public void onErr(String msg) {
                            Toast.makeText(AbsenActivity.this,
                                    "Absen tersimpan ✅ (lokal saja)", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "Absen tersimpan ✅", Toast.LENGTH_SHORT).show();
        }
        photoPath = null;
        ivPreview.setImageDrawable(null);
        onCreateSafeRefresh();
    }

    /** Grup pertama yang user ikuti (cache lokal) — untuk sinkron absen. */
    private String currentGroupFbId() {
        try {
            List<DbHelper.Group> gs = db.getGroups();
            for (DbHelper.Group g : gs) {
                if (g.fbId != null && !g.fbId.isEmpty()) return g.fbId;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void onCreateSafeRefresh() {
        // Rebuild penuh agar status tombol (kunci Check Out) & ringkasan ter-refresh.
        // recreate() memicu onDestroy (listener tim dilepas) lalu onCreate lagi.
        try { recreate(); }
        catch (Throwable ignored) { try { renderHistory(); } catch (Throwable ignored2) {} }
    }

    // ================= RIWAYAT =================

    /**
     * Feed "Aktivitas Terakhir" ala MyActio: gabungan check in/out hari ini
     * + catatan aktivitas, diurutkan berdasarkan jam (terbaru dulu).
     */
    private void renderHistory() {
        if (listWrap == null) return;
        listWrap.removeAllViews();
        String today = DateUtil.todayYmd();

        // Kumpulkan entri hari ini: absen + aktivitas.
        java.util.List<FeedItem> feed = new java.util.ArrayList<FeedItem>();
        for (DbHelper.Absen a : db.getAbsen(30)) {
            if (today.equals(a.date)) feed.add(new FeedItem(a.time, true, a));
        }
        for (DbHelper.Aktivitas ak : db.getAktivitas(today)) {
            feed.add(new FeedItem(ak.time, false, ak));
        }
        // Urutkan jam terbaru dulu.
        java.util.Collections.sort(feed, new java.util.Comparator<FeedItem>() {
            public int compare(FeedItem x, FeedItem y) {
                return y.time.compareTo(x.time);
            }
        });

        if (feed.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this,
                    "Belum ada aktivitas hari ini. Mulai dengan Check In di atas.",
                    14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
            return;
        }

        for (final FeedItem f : feed) {
            LinearLayout c = Ui.card(this);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            if (f.isAbsen) {
                DbHelper.Absen a = (DbHelper.Absen) f.obj;
                // Thumbnail foto selfie.
                ImageView th = new ImageView(this);
                int s = Ui.dp(this, 52);
                th.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                th.setScaleType(ImageView.ScaleType.CENTER_CROP);
                th.setBackground(Ui.roundBg(0xFFE8EAF2, 8, this));
                try {
                    Bitmap bm = BitmapFactory.decodeFile(a.photo);
                    if (bm != null) th.setImageBitmap(bm);
                } catch (Throwable ignored) {}
                row.addView(th);

                LinearLayout tx = new LinearLayout(this);
                tx.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tlp.leftMargin = Ui.dp(this, 12);
                tx.setLayoutParams(tlp);
                String label = "Masuk".equals(a.type) ? "Check In" : "Check Out";
                tx.addView(Ui.text(this, label + " — " + a.time, 15, Ui.TEXT, true));
                String locLine = !a.locName.isEmpty() ? a.locName
                        : (a.lat != 0 ? String.format(Locale.US, "%.4f,%.4f", a.lat, a.lon) : "");
                if (!locLine.isEmpty()) {
                    tx.addView(Ui.text(this, "📍 " + locLine, 12, Ui.TEXT_DIM, false));
                }
                row.addView(tx);
                row.addView(Ui.text(this, a.synced ? "☁" : "📱", 16, Ui.TEXT_DIM, false));
            } else {
                final DbHelper.Aktivitas ak = (DbHelper.Aktivitas) f.obj;
                // Ikon bulat untuk aktivitas.
                TextView dot = new TextView(this);
                dot.setText("•");
                dot.setTextSize(30);
                dot.setTextColor(Ui.PRIMARY);
                dot.setGravity(Gravity.CENTER);
                int s = Ui.dp(this, 52);
                dot.setLayoutParams(new LinearLayout.LayoutParams(s, s));
                dot.setBackground(Ui.roundBg(0xFFE8EAF2, 26, this));
                row.addView(dot);

                LinearLayout tx = new LinearLayout(this);
                tx.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tlp.leftMargin = Ui.dp(this, 12);
                tx.setLayoutParams(tlp);
                tx.addView(Ui.text(this, ak.text, 15, Ui.TEXT, true));
                tx.addView(Ui.text(this, "Aktivitas — " + ak.time, 12, Ui.TEXT_DIM, false));
                row.addView(tx);

                // Hapus aktivitas (tekan lama tidak tersedia -> tombol kecil).
                TextView del = Ui.text(this, "✕", 16, Ui.TEXT_DIM, false);
                del.setPadding(Ui.dp(this, 8), Ui.dp(this, 8),
                        Ui.dp(this, 8), Ui.dp(this, 8));
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        db.deleteAktivitas(ak.id);
                        renderHistory();
                    }
                });
                row.addView(del);
            }
            c.addView(row);
            listWrap.addView(c);
        }
    }

    /** Satu baris feed: absen atau aktivitas. */
    private static class FeedItem {
        String time;
        boolean isAbsen;
        Object obj;
        FeedItem(String t, boolean a, Object o) { time = t; isAbsen = a; obj = o; }
    }

    // ================= MODE TIM (Leader/Admin) =================

    private void buildTeamView(LinearLayout w) {
        w.addView(Ui.text(this,
                "Absensi anggota tim (disinkron dari perangkat masing-masing). " +
                "Foto selfie tersimpan di HP anggota.",
                13, Ui.TEXT_DIM, false));
        final LinearLayout tw = new LinearLayout(this);
        tw.setOrientation(LinearLayout.VERTICAL);
        tw.setPadding(0, Ui.dp(this, 10), 0, 0);
        w.addView(tw);

        final Activity act = this;
        absenReg = FirebaseBridge.listenAbsen(this, fbId,
                new FirebaseBridge.AbsenListCallback() {
                    public void onData(final java.util.List<Map<String, Object>> list) {
                        act.runOnUiThread(new Runnable() {
                            public void run() { renderTeam(tw, list); }
                        });
                    }
                    public void onErr(String msg) {
                        final String m = msg;
                        act.runOnUiThread(new Runnable() {
                            public void run() {
                                tw.removeAllViews();
                                tw.addView(Ui.text(act, "Gagal memuat: " + m,
                                        13, Ui.RED, false));
                            }
                        });
                    }
                });
    }

    private void renderTeam(LinearLayout tw, java.util.List<Map<String, Object>> list) {
        tw.removeAllViews();
        if (list.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada absensi tim.", 14, Ui.TEXT_DIM, false));
            tw.addView(c);
            return;
        }
        String lastDate = "";
        for (Map<String, Object> m : list) {
            String d = str(m.get("date"));
            if (!d.equals(lastDate)) {
                lastDate = d;
                TextView h = Ui.text(this, ExportHelper.tanggalID(d), 14, Ui.TEXT, true);
                h.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 4));
                tw.addView(h);
            }
            LinearLayout c = Ui.card(this);
            String nm = str(m.get("name"));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.badge(this,
                    nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(),
                    Ui.PRIMARY, 40));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tlp.leftMargin = Ui.dp(this, 10);
            tx.setLayoutParams(tlp);
            tx.addView(Ui.text(this, nm, 15, Ui.TEXT, true));
            String tp = str(m.get("type"));
            tx.addView(Ui.text(this, tp + " • " + str(m.get("time")), 13, Ui.TEXT_DIM, false));
            double la = num(m.get("lat")), lo = num(m.get("lon"));
            String ln = str(m.get("loc_name"));
            String locLine = !ln.isEmpty() ? ln
                    : (la != 0 ? String.format(Locale.US, "%.4f, %.4f", la, lo) : "");
            if (!locLine.isEmpty()) {
                tx.addView(Ui.text(this, "📍 " + locLine, 12, Ui.TEXT_DIM, false));
            }
            row.addView(tx);
            int col = "Masuk".equals(tp) ? Ui.GREEN : Ui.AMBER;
            TextView chip = Ui.text(this, tp, 12, 0xFFFFFFFF, true);
            chip.setBackground(Ui.roundBg(col, 8, this));
            chip.setPadding(Ui.dp(this, 10), Ui.dp(this, 4),
                    Ui.dp(this, 10), Ui.dp(this, 4));
            row.addView(chip);
            c.addView(row);
            tw.addView(c);
        }
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static double num(Object o) {
        try { return o instanceof Number ? ((Number) o).doubleValue()
                : Double.parseDouble(String.valueOf(o)); }
        catch (Throwable t) { return 0; }
    }
}
