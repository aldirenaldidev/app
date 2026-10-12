package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Layar WAJIB ganti kata sandi.
 *
 * Ditampilkan setelah login bila akun memiliki flag
 * users/{uid}.mustChangePassword = true (mis. admin default atau akun
 * yang dibuatkan Admin). Layar ini TIDAK bisa dilewati: tidak ada tombol
 * tutup/kembali; onBackPressed diabaikan.
 *
 * Setelah password berhasil diganti, flag dihapus dan user diarahkan
 * ke MainActivity.
 */
public class ChangePasswordActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            buildUi();
        } catch (Throwable t) {
            android.util.Log.e("ShiftKu", "ChangePassword fatal: " + t);
            finish();
        }
    }

    /** Cegah user kabur dari layar ini via tombol back. */
    @Override public void onBackPressed() {
        Toast.makeText(this,
                "Anda wajib mengganti kata sandi terlebih dahulu.",
                Toast.LENGTH_SHORT).show();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // --- Header gradient ---
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable hbg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4845D6, 0xFF5B5BF0});
        hbg.setCornerRadii(new float[]{
                0, 0, 0, 0,
                Ui.dp(this, 36), Ui.dp(this, 36),
                Ui.dp(this, 36), Ui.dp(this, 36)});
        head.setBackground(hbg);
        head.setPadding(Ui.dp(this, 28), Ui.dp(this, 28),
                Ui.dp(this, 28), Ui.dp(this, 40));

        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER);
        GradientDrawable lbg = new GradientDrawable();
        lbg.setColor(0xFFFFFFFF);
        lbg.setCornerRadius(Ui.dp(this, 18));
        logo.setBackground(lbg);
        int ls = Ui.dp(this, 56);
        logo.setLayoutParams(new LinearLayout.LayoutParams(ls, ls));
        TextView lic = Ui.text(this, "\uD83D\uDD11", 30, Ui.TEXT, false);
        lic.setGravity(Gravity.CENTER);
        logo.addView(lic);
        head.addView(logo);

        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(Ui.dp(this, 14), 0, 0, 0);
        ht.addView(Ui.text(this, "Ganti Kata Sandi", 24, 0xFFFFFFFF, true));
        TextView hw = Ui.text(this, "Amankan akun Anda \uD83D\uDEE1\uFE0F",
                13, 0xFFFFFFFF, false);
        hw.setAlpha(0.88f);
        ht.addView(hw);
        head.addView(ht);
        root.addView(head);

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 24), Ui.dp(this, 26),
                Ui.dp(this, 24), Ui.dp(this, 40));

        // Info: untuk siapa password ini diganti.
        String em = "";
        try {
            com.google.firebase.auth.FirebaseUser u =
                    FirebaseBridge.currentUser(this);
            if (u != null && u.getEmail() != null) em = u.getEmail();
        } catch (Throwable ignored) {}
        TextView info = Ui.text(this,
                "Anda login sebagai " + (em.isEmpty() ? "pengguna" : em)
                        + ".\nDemi keamanan, ganti kata sandi bawaan ini "
                        + "dengan kata sandi baru pilihan Anda.",
                13, Ui.TEXT, false);
        info.setBackground(Ui.roundBg(0xFFEEF0FF, 14, this));
        info.setPadding(Ui.dp(this, 14), Ui.dp(this, 12),
                Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        ilp.bottomMargin = Ui.dp(this, 20);
        info.setLayoutParams(ilp);
        w.addView(info);

        final EditText etNew = passInput(w, "Kata Sandi Baru");
        final EditText etConfirm = passInput(w, "Konfirmasi Kata Sandi");

        TextView hint = Ui.text(this,
                "Minimal 6 karakter. Gunakan kombinasi huruf & angka agar kuat.",
                12, Ui.TEXT_DIM, false);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = Ui.dp(this, 20);
        hint.setLayoutParams(hlp);
        w.addView(hint);

        final TextView btn = new TextView(this);
        btn.setText("Simpan Kata Sandi Baru");
        btn.setTextSize(16);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(0xFFFFFFFF);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bbg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4845D6, 0xFF5B5BF0});
        bbg.setCornerRadius(Ui.dp(this, 16));
        btn.setBackground(bbg);
        btn.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        btn.setClickable(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            btn.setElevation(Ui.dp(this, 6));
        }
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doChange(etNew.getText().toString(),
                        etConfirm.getText().toString(), btn);
            }
        });
        w.addView(btn);

        TextView foot = Ui.text(this, "\u00A9 aldire__", 11, 0xFFA6ABBF, false);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        flp.topMargin = Ui.dp(this, 26);
        foot.setLayoutParams(flp);
        w.addView(foot);

        sv.addView(w);
        root.addView(sv);
        setContentView(root);
    }

    /** Field password dengan ikon gembok + toggle mata. */
    private EditText passInput(LinearLayout parent, String label) {
        TextView lb = Ui.text(this, label, 13, Ui.TEXT, true);
        lb.setPadding(0, 0, 0, Ui.dp(this, 8));
        parent.addView(lb);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(Ui.dp(this, 14));
        bg.setStroke(Ui.dp(this, 2), 0xFFE4E7F0);
        box.setBackground(bg);
        box.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 54));
        blp.bottomMargin = Ui.dp(this, 16);
        box.setLayoutParams(blp);

        TextView ic = Ui.text(this, "\uD83D\uDD12", 19, Ui.TEXT_DIM, false);
        ic.setPadding(0, 0, Ui.dp(this, 12), 0);
        box.addView(ic);

        final EditText et = new EditText(this);
        et.setBackground(null);
        et.setHint("\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022");
        et.setHintTextColor(0xFFA6ABBF);
        et.setTextColor(Ui.TEXT);
        et.setTextSize(15);
        et.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setSingleLine(true);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        et.setLayoutParams(elp);
        box.addView(et);

        final TextView eye = Ui.text(this, "\uD83D\uDC41\uFE0F", 19, Ui.TEXT_DIM, false);
        eye.setClickable(true);
        eye.setPadding(Ui.dp(this, 8), Ui.dp(this, 8),
                Ui.dp(this, 4), Ui.dp(this, 8));
        eye.setOnClickListener(new View.OnClickListener() {
            private boolean shown = false;
            public void onClick(View v) {
                shown = !shown;
                int sel = et.getSelectionEnd();
                et.setInputType(InputType.TYPE_CLASS_TEXT
                        | (shown ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                 : InputType.TYPE_TEXT_VARIATION_PASSWORD));
                et.setSelection(Math.max(0, sel));
            }
        });
        box.addView(eye);

        parent.addView(box);
        return et;
    }

    private void doChange(String p1, String p2, final TextView btn) {
        if (p1.length() < 6) {
            Toast.makeText(this, "Kata sandi minimal 6 karakter.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!p1.equals(p2)) {
            Toast.makeText(this, "Konfirmasi tidak cocok. Coba lagi.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        btn.setClickable(false);
        btn.setAlpha(0.6f);
        Toast.makeText(this, "Menyimpan…", Toast.LENGTH_SHORT).show();
        FirebaseBridge.changePassword(this, p1, new FirebaseBridge.FbCallback() {
            public void onOk(String info) {
                Toast.makeText(ChangePasswordActivity.this, info,
                        Toast.LENGTH_LONG).show();
                Intent i = new Intent(ChangePasswordActivity.this,
                        MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
            public void onErr(String msg) {
                btn.setClickable(true);
                btn.setAlpha(1f);
                Toast.makeText(ChangePasswordActivity.this, msg,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
