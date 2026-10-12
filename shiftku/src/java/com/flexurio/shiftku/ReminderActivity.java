package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Pengaturan Pengingat Shift — notifikasi otomatis sebelum shift mulai. */
public class ReminderActivity extends Activity {

    private static final int[] OPTIONS = {15, 30, 60, 120};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Pengingat Shift"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));

        // Kartu on/off
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Pengingat otomatis", 16, Ui.TEXT, true));
        TextView desc = Ui.text(this,
                "Kirim notifikasi sebelum shift Anda mulai, berdasarkan jadwal di kalender.",
                13, Ui.TEXT_DIM, false);
        desc.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        card.addView(desc);

        final TextView toggle = Ui.text(this, "", 15, Ui.TEXT, true);
        toggle.setGravity(Gravity.CENTER);
        toggle.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        toggle.setClickable(true);
        card.addView(toggle);

        final Runnable refreshToggle = new Runnable() {
            public void run() {
                boolean on = Prefs.isShiftReminderOn(ReminderActivity.this);
                toggle.setText(on ? "Aktif ✅" : "Nonaktif ⬜");
                toggle.setBackground(Ui.roundBg(on ? Ui.GREEN : 0xFFE8EAF2, 10,
                        ReminderActivity.this));
                toggle.setTextColor(on ? 0xFFFFFFFF : Ui.TEXT_DIM);
            }
        };
        refreshToggle.run();
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean on = !Prefs.isShiftReminderOn(ReminderActivity.this);
                Prefs.setShiftReminderOn(ReminderActivity.this, on);
                ShiftReminder.schedule(ReminderActivity.this);
                refreshToggle.run();
                Toast.makeText(ReminderActivity.this,
                        on ? "Pengingat diaktifkan." : "Pengingat dimatikan.",
                        Toast.LENGTH_SHORT).show();
            }
        });
        w.addView(card);

        // Kartu pilihan waktu
        LinearLayout card2 = Ui.card(this);
        card2.addView(Ui.text(this, "Ingatkan saya", 16, Ui.TEXT, true));
        TextView d2 = Ui.text(this, "Berapa menit sebelum shift mulai?",
                13, Ui.TEXT_DIM, false);
        d2.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 10));
        card2.addView(d2);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int cur = Prefs.getShiftReminderMinutes(this);
        for (final int m : OPTIONS) {
            TextView chip = Ui.text(this, label(m), 14, Ui.TEXT_DIM, true);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            int mg = Ui.dp(this, 4);
            lp.leftMargin = mg; lp.rightMargin = mg;
            chip.setLayoutParams(lp);
            chip.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
            final boolean sel = (m == cur);
            chip.setBackground(Ui.roundBg(sel ? Ui.PRIMARY : 0xFFE8EAF2, 10, this));
            chip.setTextColor(sel ? 0xFFFFFFFF : Ui.TEXT_DIM);
            chip.setClickable(true);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Prefs.setShiftReminderMinutes(ReminderActivity.this, m);
                    ShiftReminder.schedule(ReminderActivity.this);
                    Toast.makeText(ReminderActivity.this,
                            "Pengingat: " + label(m) + " sebelum shift.",
                            Toast.LENGTH_SHORT).show();
                    recreate();
                }
            });
            row.addView(chip);
        }
        card2.addView(row);
        w.addView(card2);

        TextView note = Ui.text(this,
                "Pengingat dijadwalkan ulang otomatis setiap aplikasi dibuka.",
                12, Ui.TEXT_DIM, false);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, Ui.dp(this, 8), 0, 0);
        w.addView(note);

        sv.addView(w);
        root.addView(sv);
        setContentView(root);
    }

    private static String label(int m) {
        if (m >= 60) return (m / 60) + " jam";
        return m + " mnt";
    }
}
