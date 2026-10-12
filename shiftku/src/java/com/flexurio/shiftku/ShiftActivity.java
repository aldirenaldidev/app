package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Tambah Shift: pilih shift untuk tanggal tertentu (mockup add.png). */
public class ShiftActivity extends Activity {

    private DbHelper db;
    private String date;
    private long selected = 0;
    private LinearLayout listWrap;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        date = getIntent().getStringExtra("date");
        if (date == null) date = DateUtil.todayYmd();
        selected = db.getScheduleShift(date);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Tambah Shift"));

        TextView dt = Ui.text(this, DateUtil.pretty(date), 14, Ui.TEXT_DIM, false);
        dt.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), 0);
        root.addView(dt);

        ScrollView sv = new ScrollView(this);
        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        listWrap.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 14));
        sv.addView(listWrap);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);

        LinearLayout foot = new LinearLayout(this);
        foot.setOrientation(LinearLayout.VERTICAL);
        foot.setPadding(Ui.dp(this, 14), 0, Ui.dp(this, 14), Ui.dp(this, 16));
        TextView save = Ui.primaryButton(this, "Simpan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                db.setSchedule(date, selected);
                ShiftWidget.refreshAll(ShiftActivity.this);
                finish();
            }
        });
        foot.addView(save);
        root.addView(foot);

        setContentView(root);
        renderList();
    }

    private void renderList() {
        listWrap.removeAllViews();
        List<Shift> shifts = db.getShifts();
        for (final Shift s : shifts) {
            LinearLayout card = Ui.card(this);
            boolean sel = (s.id == selected);
            if (sel) {
                android.graphics.drawable.GradientDrawable gd =
                        new android.graphics.drawable.GradientDrawable();
                gd.setColor(Ui.CARD);
                gd.setCornerRadius(Ui.dp(this, 14));
                gd.setStroke(Ui.dp(this, 2), Ui.PRIMARY);
                card.setBackground(gd);
            }
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.badge(this, s.initial(), s.color, 40));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(Ui.dp(this, 12), 0, 0, 0);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tx.setLayoutParams(tlp);
            tx.addView(Ui.text(this, s.name, 16, Ui.TEXT, true));
            tx.addView(Ui.text(this, s.jam(), 13, Ui.TEXT_DIM, false));
            row.addView(tx);
            if (sel) {
                row.addView(Ui.text(this, "✓", 22, Ui.PRIMARY, true));
            }
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    selected = s.id;
                    renderList();
                }
            });
            card.addView(row);
            listWrap.addView(card);
        }
        // opsi kosongkan
        TextView clear = Ui.text(this, "Kosongkan jadwal tanggal ini", 14, Ui.RED, false);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(0, Ui.dp(this, 8), 0, 0);
        clear.setClickable(true);
        clear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selected = 0; renderList(); }
        });
        listWrap.addView(clear);
    }
}
