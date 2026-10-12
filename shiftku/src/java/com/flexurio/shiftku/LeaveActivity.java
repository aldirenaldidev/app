package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

/** Kelola Cuti (mockup leave.png). */
public class LeaveActivity extends Activity {

    private DbHelper db;
    private LinearLayout listWrap;
    private TextView tvSisa;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Kelola Cuti"));

        ScrollView sv = new ScrollView(this);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        // kartu sisa cuti
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.PRIMARY, Ui.PRIMARY_DARK});
        gd.setCornerRadius(Ui.dp(this, 14));
        card.setBackground(gd);
        int p = Ui.dp(this, 16);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = Ui.dp(this, 10);
        card.setLayoutParams(clp);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        card.addView(Ui.text(this, "Sisa Cuti " + year, 14, 0xFFE0E0FF, false));
        tvSisa = Ui.text(this, "", 32, 0xFFFFFFFF, true);
        card.addView(tvSisa);
        TextView tvTotal = Ui.text(this, "", 13, 0xFFE0E0FF, false);
        tvTotal.setTag("total");
        card.addView(tvTotal);
        wrap.addView(card);
        // simpan referensi
        card.setTag(tvTotal);

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(listWrap);

        TextView add = Ui.outlineButton(this, "+ Ajukan Cuti");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddDialog(); }
        });
        wrap.addView(add);

        sv.addView(wrap);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);

        setContentView(root);
        render();
    }

    private void render() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int quota = Prefs.getLeaveQuota(this);
        int used = db.usedLeaveDays(year);
        int sisa = Math.max(0, quota - used);
        tvSisa.setText(sisa + " hari");
        // update total label via parent traversal
        LinearLayout card = (LinearLayout) tvSisa.getParent();
        TextView tvTotal = (TextView) card.getTag();
        tvTotal.setText("dari total " + quota + " hari");

        listWrap.removeAllViews();
        List<DbHelper.Leave> leaves = db.getLeaves();
        if (leaves.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada pengajuan cuti.", 14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
        }
        for (final DbHelper.Leave l : leaves) {
            LinearLayout c = Ui.card(this);
            String label = l.start.equals(l.end)
                    ? DateUtil.pretty(l.start)
                    : DateUtil.pretty(l.start) + " – " + DateUtil.pretty(l.end);
            c.addView(Ui.text(this, label, 16, Ui.TEXT, true));
            int col = l.status.equals("Disetujui") ? Ui.GREEN
                    : l.status.equals("Ditolak") ? Ui.RED : Ui.AMBER;
            TextView st = Ui.text(this, l.status, 13, col, false);
            st.setPadding(0, Ui.dp(this, 2), 0, 0);
            c.addView(st);
            if (l.note != null && !l.note.isEmpty()) {
                TextView nt = Ui.text(this, l.note, 13, Ui.TEXT_DIM, false);
                c.addView(nt);
            }
            if (l.status.equals("Menunggu")) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, Ui.dp(this, 8), 0, 0);
                TextView ok = Ui.text(this, "Setujui", 14, Ui.GREEN, true);
                ok.setPadding(0, 0, Ui.dp(this, 20), 0);
                ok.setClickable(true);
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        setStatus(l.id, "Disetujui");
                    }
                });
                row.addView(ok);
                TextView no = Ui.text(this, "Tolak", 14, Ui.RED, true);
                no.setPadding(0, 0, Ui.dp(this, 20), 0);
                no.setClickable(true);
                no.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        setStatus(l.id, "Ditolak");
                    }
                });
                row.addView(no);
                TextView del = Ui.text(this, "Hapus", 14, Ui.TEXT_DIM, true);
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        db.deleteLeave(l.id);
                        render();
                    }
                });
                row.addView(del);
                c.addView(row);
            }
            listWrap.addView(c);
        }
    }

    private void setStatus(long id, String s) {
        android.content.ContentValues v = new android.content.ContentValues();
        v.put("status", s);
        db.getWritableDatabase().update("leaves", v, "id=?",
                new String[]{String.valueOf(id)});
        render();
    }

    private void showAddDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Ajukan Cuti", 18, Ui.TEXT, true));

        final android.widget.EditText etFrom = new android.widget.EditText(this);
        etFrom.setHint("Dari (2026-10-20)");
        etFrom.setText(DateUtil.todayYmd());
        w.addView(etFrom);
        final android.widget.EditText etTo = new android.widget.EditText(this);
        etTo.setHint("Sampai (2026-10-21)");
        etTo.setText(DateUtil.todayYmd());
        w.addView(etTo);
        final android.widget.EditText etNote = new android.widget.EditText(this);
        etNote.setHint("Keterangan (opsional)");
        w.addView(etNote);

        TextView save = Ui.primaryButton(this, "Ajukan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                db.addLeave(etFrom.getText().toString().trim(),
                        etTo.getText().toString().trim(),
                        etNote.getText().toString().trim());
                d.dismiss();
                render();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }
}
