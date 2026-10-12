package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Pengumuman — Admin/Leader kirim, semua anggota baca. Badge "baru" untuk belum dibaca. */
public class PengumumanActivity extends Activity {

    private DbHelper db;
    private LinearLayout listWrap;
    private boolean bisaKirim;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        bisaKirim = FirebaseBridge.canApprove(Prefs.getUserRole(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Pengumuman"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        w.addView(listWrap);

        if (bisaKirim) {
            TextView add = Ui.outlineButton(this, "+ Buat Pengumuman");
            add.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showAddDialog(); }
            });
            w.addView(add);
        }

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
        List<DbHelper.Pengumuman> list = db.getPengumuman();
        if (list.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada pengumuman.", 14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
            return;
        }
        for (final DbHelper.Pengumuman p : list) {
            LinearLayout c = Ui.card(this);

            LinearLayout head = new LinearLayout(this);
            head.setOrientation(LinearLayout.HORIZONTAL);
            head.setGravity(Gravity.CENTER_VERTICAL);
            TextView t = Ui.text(this, p.title, 16, Ui.TEXT, true);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            t.setLayoutParams(tlp);
            head.addView(t);
            if (!p.read) {
                TextView badge = Ui.text(this, " BARU ", 11, 0xFFFFFFFF, true);
                badge.setBackground(Ui.roundBg(Ui.RED, 8, this));
                badge.setPadding(Ui.dp(this, 8), Ui.dp(this, 3),
                        Ui.dp(this, 8), Ui.dp(this, 3));
                head.addView(badge);
            }
            c.addView(head);

            TextView b = Ui.text(this, p.body, 14, Ui.TEXT, false);
            b.setPadding(0, Ui.dp(this, 6), 0, 0);
            c.addView(b);
            TextView dt = Ui.text(this, ExportHelper.tanggalID(p.date), 12, Ui.TEXT_DIM, false);
            dt.setPadding(0, Ui.dp(this, 6), 0, 0);
            c.addView(dt);

            final long pid = p.id;
            c.setClickable(true);
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.markPengumumanRead(pid);
                    render();
                }
            });

            if (bisaKirim) {
                TextView del = Ui.text(this, "Hapus", 13, Ui.RED, true);
                del.setPadding(0, Ui.dp(this, 6), 0, 0);
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.deletePengumuman(pid); render(); }
                });
                c.addView(del);
            }
            listWrap.addView(c);
        }
    }

    private void showAddDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Buat Pengumuman", 18, Ui.TEXT, true));

        final android.widget.EditText etTitle = new android.widget.EditText(this);
        etTitle.setHint("Judul pengumuman");
        w.addView(etTitle);
        final android.widget.EditText etBody = new android.widget.EditText(this);
        etBody.setHint("Isi pengumuman");
        etBody.setMinLines(3);
        w.addView(etBody);

        TextView save = Ui.primaryButton(this, "Kirim");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String t = etTitle.getText().toString().trim();
                String b = etBody.getText().toString().trim();
                if (t.isEmpty() || b.isEmpty()) {
                    Toast.makeText(PengumumanActivity.this,
                            "Judul dan isi wajib diisi.", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.addPengumuman(t, b, DateUtil.todayYmd());
                d.dismiss();
                render();
                Toast.makeText(PengumumanActivity.this,
                        "Pengumuman terkirim.", Toast.LENGTH_SHORT).show();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }
}
