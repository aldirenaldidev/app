package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Izin / Sakit — terpisah dari Cuti.
 * Sakit bisa melampirkan foto surat dokter (dari galeri).
 * Persetujuan Leader/Admin seperti cuti.
 */
public class IzinActivity extends Activity {

    private static final int REQ_GALLERY = 9101;

    private DbHelper db;
    private LinearLayout listWrap;
    private boolean bisaApprove;

    // state dialog tambah
    private String tipeDlg = "Izin";
    private String photoDlg;
    private ImageView ivDlg;
    private TextView tvTipeIzin, tvTipeSakit;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        bisaApprove = FirebaseBridge.canApprove(Prefs.getUserRole(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Izin / Sakit"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        w.addView(listWrap);

        TextView add = Ui.outlineButton(this, "+ Ajukan Izin / Sakit");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddDialog(); }
        });
        w.addView(add);

        sv.addView(w);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);

        setContentView(root);
        render();
    }

    private void render() {
        listWrap.removeAllViews();
        List<DbHelper.Izin> list = db.getIzin();
        if (list.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada pengajuan izin/sakit.", 14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
            return;
        }
        for (final DbHelper.Izin iz : list) {
            LinearLayout c = Ui.card(this);
            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            int tc = "Sakit".equals(iz.type) ? Ui.RED : Ui.AMBER;
            TextView tp = Ui.text(this, iz.type.toUpperCase(), 13, 0xFFFFFFFF, true);
            tp.setBackground(Ui.roundBg(tc, 8, this));
            tp.setPadding(Ui.dp(this, 10), Ui.dp(this, 4),
                    Ui.dp(this, 10), Ui.dp(this, 4));
            head.addView(tp);
            TextView tgl = Ui.text(this, "  " + ExportHelper.tanggalID(iz.startDate)
                    + (iz.startDate.equals(iz.endDate) ? ""
                    : " – " + ExportHelper.tanggalID(iz.endDate)),
                    14, Ui.TEXT, true);
            head.addView(tgl);
            c.addView(head);

            if (iz.reason != null && !iz.reason.isEmpty()) {
                TextView nt = Ui.text(this, iz.reason, 13, Ui.TEXT_DIM, false);
                nt.setPadding(0, Ui.dp(this, 4), 0, 0);
                c.addView(nt);
            }
            if (iz.photo != null && !iz.photo.isEmpty()
                    && new File(iz.photo).exists()) {
                TextView ph = Ui.text(this, "📎 Surat dokter terlampir", 12, Ui.DARK_ACCENT, false);
                ph.setPadding(0, Ui.dp(this, 4), 0, 0);
                c.addView(ph);
            }
            int col = "Disetujui".equals(iz.status) ? Ui.GREEN
                    : "Ditolak".equals(iz.status) ? Ui.RED : Ui.AMBER;
            TextView st = Ui.text(this, iz.status, 13, col, true);
            st.setPadding(0, Ui.dp(this, 4), 0, 0);
            c.addView(st);

            if ("Menunggu".equals(iz.status) && bisaApprove) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, Ui.dp(this, 8), 0, 0);
                TextView ok = Ui.text(this, "Setujui", 14, Ui.GREEN, true);
                ok.setPadding(0, 0, Ui.dp(this, 20), 0);
                ok.setClickable(true);
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.setIzinStatus(iz.id, "Disetujui"); render(); }
                });
                row.addView(ok);
                TextView no = Ui.text(this, "Tolak", 14, Ui.RED, true);
                no.setPadding(0, 0, Ui.dp(this, 20), 0);
                no.setClickable(true);
                no.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.setIzinStatus(iz.id, "Ditolak"); render(); }
                });
                row.addView(no);
                TextView del = Ui.text(this, "Hapus", 14, Ui.TEXT_DIM, true);
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.deleteIzin(iz.id); render(); }
                });
                row.addView(del);
                c.addView(row);
            }
            listWrap.addView(c);
        }
    }

    private void showAddDialog() {
        tipeDlg = "Izin";
        photoDlg = null;
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Ajukan Izin / Sakit", 18, Ui.TEXT, true));

        // Pilih tipe
        LinearLayout tipeRow = new LinearLayout(this);
        tipeRow.setOrientation(LinearLayout.HORIZONTAL);
        tipeRow.setPadding(0, Ui.dp(this, 10), 0, 0);
        tvTipeIzin = chipTipe("Izin");
        tvTipeSakit = chipTipe("Sakit");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cp.rightMargin = Ui.dp(this, 8);
        tvTipeIzin.setLayoutParams(cp);
        tvTipeSakit.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tipeRow.addView(tvTipeIzin);
        tipeRow.addView(tvTipeSakit);
        w.addView(tipeRow);
        refreshTipeDlg();

        final android.widget.EditText etFrom = new android.widget.EditText(this);
        etFrom.setHint("Dari (2026-10-20)");
        etFrom.setText(DateUtil.todayYmd());
        w.addView(etFrom);
        final android.widget.EditText etTo = new android.widget.EditText(this);
        etTo.setHint("Sampai (2026-10-21)");
        etTo.setText(DateUtil.todayYmd());
        w.addView(etTo);
        final android.widget.EditText etNote = new android.widget.EditText(this);
        etNote.setHint("Alasan");
        w.addView(etNote);

        // Foto surat dokter (khusus Sakit)
        ivDlg = new ImageView(this);
        int ps = Ui.dp(this, 120);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ps, ps);
        plp.gravity = Gravity.CENTER_HORIZONTAL;
        plp.topMargin = Ui.dp(this, 8);
        ivDlg.setLayoutParams(plp);
        ivDlg.setBackground(Ui.roundBg(0xFFE8EAF2, 10, this));
        ivDlg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        w.addView(ivDlg);
        TextView btnPhoto = Ui.outlineButton(this, "📎 Lampirkan Surat Dokter");
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQ_GALLERY);
            }
        });
        w.addView(btnPhoto);
        TextView phHint = Ui.text(this, "Opsional — khusus Sakit.",
                12, Ui.TEXT_DIM, false);
        phHint.setGravity(Gravity.CENTER);
        w.addView(phHint);

        TextView save = Ui.primaryButton(this, "Ajukan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                db.addIzin(tipeDlg,
                        etFrom.getText().toString().trim(),
                        etTo.getText().toString().trim(),
                        etNote.getText().toString().trim(),
                        photoDlg == null ? "" : photoDlg);
                d.dismiss();
                render();
                Toast.makeText(IzinActivity.this, "Pengajuan terkirim.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }

    private TextView chipTipe(final String t) {
        final TextView tv = Ui.text(this, t, 15, Ui.TEXT_DIM, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        tv.setClickable(true);
        tv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { tipeDlg = t; refreshTipeDlg(); }
        });
        return tv;
    }

    private void refreshTipeDlg() {
        boolean izin = "Izin".equals(tipeDlg);
        tvTipeIzin.setBackground(Ui.roundBg(izin ? Ui.PRIMARY : 0xFFE8EAF2, 10, this));
        tvTipeIzin.setTextColor(izin ? 0xFFFFFFFF : Ui.TEXT_DIM);
        tvTipeSakit.setBackground(Ui.roundBg(!izin ? Ui.PRIMARY : 0xFFE8EAF2, 10, this));
        tvTipeSakit.setTextColor(!izin ? 0xFFFFFFFF : Ui.TEXT_DIM);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_GALLERY && res == RESULT_OK && data != null
                && data.getData() != null) {
            try {
                Uri uri = data.getData();
                InputStream in = getContentResolver().openInputStream(uri);
                Bitmap bm = BitmapFactory.decodeStream(in);
                if (in != null) in.close();
                if (bm != null) {
                    // Simpan salinan kecil ke cache internal.
                    File dir = getCacheDir();
                    File f = new File(dir, "surat_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    int sw = Math.max(1, bm.getWidth() / 800);
                    Bitmap small = Bitmap.createScaledBitmap(bm,
                            bm.getWidth() / sw, bm.getHeight() / sw, true);
                    small.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                    fos.close();
                    photoDlg = f.getAbsolutePath();
                    if (ivDlg != null) ivDlg.setImageBitmap(small);
                    Toast.makeText(this, "Surat dokter dilampirkan ✅",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Gagal memuat foto.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
