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

/** Lembur — ajukan jam lembur, disetujui/ditolak Leader/Admin. */
public class LemburActivity extends Activity {

    private DbHelper db;
    private LinearLayout listWrap;
    private boolean bisaApprove;

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
        root.addView(Ui.header(this, this, "Lembur"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        w.addView(listWrap);

        TextView add = Ui.outlineButton(this, "+ Ajukan Lembur");
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
        List<DbHelper.Lembur> list = db.getLembur();
        if (list.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada pengajuan lembur.", 14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
            return;
        }
        for (final DbHelper.Lembur l : list) {
            LinearLayout c = Ui.card(this);
            String tgl = ExportHelper.tanggalID(l.date);
            c.addView(Ui.text(this, tgl + " • " + l.startTime + "–" + l.endTime
                    + " (" + String.format(java.util.Locale.US, "%.1f", l.hours()) + " jam)",
                    15, Ui.TEXT, true));
            if (l.reason != null && !l.reason.isEmpty()) {
                TextView nt = Ui.text(this, l.reason, 13, Ui.TEXT_DIM, false);
                nt.setPadding(0, Ui.dp(this, 2), 0, 0);
                c.addView(nt);
            }
            int col = "Disetujui".equals(l.status) ? Ui.GREEN
                    : "Ditolak".equals(l.status) ? Ui.RED : Ui.AMBER;
            TextView st = Ui.text(this, l.status, 13, col, true);
            st.setPadding(0, Ui.dp(this, 4), 0, 0);
            c.addView(st);

            if ("Menunggu".equals(l.status) && bisaApprove) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, Ui.dp(this, 8), 0, 0);
                TextView ok = Ui.text(this, "Setujui", 14, Ui.GREEN, true);
                ok.setPadding(0, 0, Ui.dp(this, 20), 0);
                ok.setClickable(true);
                ok.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.setLemburStatus(l.id, "Disetujui"); render(); }
                });
                row.addView(ok);
                TextView no = Ui.text(this, "Tolak", 14, Ui.RED, true);
                no.setPadding(0, 0, Ui.dp(this, 20), 0);
                no.setClickable(true);
                no.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.setLemburStatus(l.id, "Ditolak"); render(); }
                });
                row.addView(no);
                TextView del = Ui.text(this, "Hapus", 14, Ui.TEXT_DIM, true);
                del.setClickable(true);
                del.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { db.deleteLembur(l.id); render(); }
                });
                row.addView(del);
                c.addView(row);
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
        w.addView(Ui.text(this, "Ajukan Lembur", 18, Ui.TEXT, true));

        final android.widget.EditText etDate = new android.widget.EditText(this);
        etDate.setHint("Tanggal (2026-10-20)");
        etDate.setText(DateUtil.todayYmd());
        w.addView(etDate);
        final android.widget.EditText etStart = new android.widget.EditText(this);
        etStart.setHint("Mulai (17:00)");
        etStart.setText("17:00");
        w.addView(etStart);
        final android.widget.EditText etEnd = new android.widget.EditText(this);
        etEnd.setHint("Selesai (20:00)");
        etEnd.setText("20:00");
        w.addView(etEnd);
        final android.widget.EditText etNote = new android.widget.EditText(this);
        etNote.setHint("Alasan lembur");
        w.addView(etNote);

        TextView save = Ui.primaryButton(this, "Ajukan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String by = FirebaseBridge.currentUserName(LemburActivity.this);
                if (by == null || by.isEmpty()) by = Prefs.getUserName(LemburActivity.this);
                db.addLembur(etDate.getText().toString().trim(),
                        etStart.getText().toString().trim(),
                        etEnd.getText().toString().trim(),
                        etNote.getText().toString().trim(), by);
                d.dismiss();
                render();
                Toast.makeText(LemburActivity.this, "Lembur diajukan.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }
}
