package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Tentang ShiftKu. */
public class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Tentang"));

        ScrollView sv = new ScrollView(this);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        wrap.setGravity(Gravity.CENTER_HORIZONTAL);

        ImageView logo = Ui.icon(this, R.mipmap.ic_launcher, 0, 96);
        wrap.addView(logo);
        TextView t = Ui.text(this, "ShiftKu", 24, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 8), 0, 0);
        wrap.addView(t);
        TextView v = Ui.text(this, "Versi 1.0", 14, Ui.TEXT_DIM, false);
        v.setGravity(Gravity.CENTER);
        wrap.addView(v);

        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Kenapa ShiftKu dibuat", 16, Ui.TEXT, true));
        TextView d = Ui.text(this,
                "ShiftKu dibuat untuk membantu pekerja shift mengatur jadwal kerja " +
                "dengan mudah: kalender shift, alarm pengingat, kelola cuti, " +
                "dan berbagi jadwal dengan tim. Semua data pribadi tersimpan " +
                "aman di HP tanpa perlu koneksi internet.",
                14, Ui.TEXT_DIM, false);
        d.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(d);
        LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) card.getLayoutParams();
        clp.topMargin = Ui.dp(this, 14);
        card.setLayoutParams(clp);
        wrap.addView(card);

        TextView cr = Ui.text(this, "© aldire__", 14, Ui.TEXT_DIM, false);
        cr.setGravity(Gravity.CENTER);
        cr.setPadding(0, Ui.dp(this, 16), 0, 0);
        wrap.addView(cr);

        TextView ig = Ui.text(this, "@aldire__  •  Instagram", 15, Ui.PRIMARY, true);
        ig.setGravity(Gravity.CENTER);
        ig.setPadding(0, Ui.dp(this, 8), 0, 0);
        ig.setClickable(true);
        ig.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Ui.openInstagram(AboutActivity.this); }
        });
        wrap.addView(ig);

        sv.addView(wrap);
        root.addView(sv);
        setContentView(root);
    }
}
