package com.flexurio.shiftku;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;

/**
 * Login baru sesuai mockup (TANPA pendaftaran mandiri).
 *
 * - Akun dibuat & diatur oleh Admin. Tidak ada tombol "Daftar".
 * - Field Email + Kata Sandi (ikon + toggle lihat sandi).
 * - "Lupa kata sandi?" -> kirim tautan reset via Firebase Auth.
 * - Setelah login -> MainActivity. Logout dari profil -> kembali ke sini.
 *
 * Anti-blank: layar SELALU di-render; bila Firebase belum siap tampil banner
 * peringatan, tombol nonaktif dengan penjelasan — tidak pernah layar kosong.
 */
public class LoginActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            onCreateSafe();
        } catch (Throwable t) {
            android.util.Log.e("ShiftKu", "LoginActivity fatal: " + t);
            showErrorScreen("Gagal membuka layar Masuk: " + t.getMessage());
        }
    }

    private void onCreateSafe() {
        boolean fbReady = FirebaseBridge.isReady(this);
        FirebaseUser user = null;
        try {
            user = fbReady ? FirebaseBridge.currentUser(this) : null;
        } catch (Throwable ignored) {}
        if (user != null) {
            showProfile(fbReady);
            return;
        }
        buildLoginForm(fbReady);
    }

    /** Layar error informatif — pengganti blank screen. */
    private void showErrorScreen(String msg) {
        try {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(Ui.BG);
            TextView t = Ui.text(this, msg, 14, Ui.TEXT, false);
            t.setPadding(Ui.dp(this, 20), Ui.dp(this, 24),
                    Ui.dp(this, 20), Ui.dp(this, 20));
            root.addView(t);
            setContentView(root);
        } catch (Throwable ignored) {}
    }

    // ================= FORM LOGIN =================

    private void buildLoginForm(final boolean fbReady) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // --- Header gradient melengkung ---
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
                Ui.dp(this, 28), Ui.dp(this, 44));

        android.widget.ImageView logoIv = new android.widget.ImageView(this);
        int ls = Ui.dp(this, 56);
        logoIv.setLayoutParams(new LinearLayout.LayoutParams(ls, ls));
        try {
            logoIv.setImageResource(getResources().getIdentifier(
                    "logo_shiftku", "drawable", getPackageName()));
        } catch (Throwable ignored) {}
        head.addView(logoIv);

        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.setPadding(Ui.dp(this, 14), 0, 0, 0);
        ht.addView(Ui.text(this, "ShiftKu", 24, 0xFFFFFFFF, true));
        TextView hw = Ui.text(this, "Selamat datang kembali \uD83D\uDC4B",
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

        // Banner peringatan bila Firebase belum siap.
        if (!fbReady) {
            TextView warn = Ui.text(this,
                    "\u26A0 Sinkronisasi belum siap. Periksa koneksi internet lalu buka ulang layar ini.",
                    13, 0xFFB45309, false);
            warn.setBackground(Ui.roundBg(0xFFFEF3C7, 10, this));
            warn.setPadding(Ui.dp(this, 12), Ui.dp(this, 10),
                    Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            wlp.bottomMargin = Ui.dp(this, 12);
            warn.setLayoutParams(wlp);
            w.addView(warn);
        }

        final EditText etEmail = inputWithIcon(w, "Email", "\u2709\uFE0F",
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, false);
        final EditText etPass = inputWithIcon(w, "Kata Sandi", "\uD83D\uDD12",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, true);

        // Lupa kata sandi? (rata kanan)
        TextView lupa = Ui.text(this, "Lupa kata sandi?", 13, Ui.PRIMARY, true);
        lupa.setGravity(Gravity.END);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.topMargin = Ui.dp(this, -4);
        llp.bottomMargin = Ui.dp(this, 20);
        lupa.setLayoutParams(llp);
        lupa.setClickable(true);
        lupa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!fbReady) {
                    Toast.makeText(LoginActivity.this,
                            "Sinkronisasi belum siap. Coba lagi nanti.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                showForgotDialog(etEmail.getText().toString().trim());
            }
        });
        w.addView(lupa);

        // Tombol Masuk (gradient)
        TextView btnIn = new TextView(this);
        btnIn.setText("Masuk");
        btnIn.setTextSize(16);
        btnIn.setTypeface(null, android.graphics.Typeface.BOLD);
        btnIn.setTextColor(0xFFFFFFFF);
        btnIn.setGravity(Gravity.CENTER);
        GradientDrawable bbg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4845D6, 0xFF5B5BF0});
        bbg.setCornerRadius(Ui.dp(this, 16));
        btnIn.setBackground(bbg);
        btnIn.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        btnIn.setClickable(true);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            btnIn.setElevation(Ui.dp(this, 6));
        }
        btnIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!fbReady) {
                    Toast.makeText(LoginActivity.this,
                            "Sinkronisasi belum siap. Coba lagi nanti.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                doLogin(etEmail.getText().toString().trim(),
                        etPass.getText().toString());
            }
        });
        w.addView(btnIn);

        // Catatan: akun dibuat oleh Admin (pengganti tombol daftar).
        TextView note = Ui.text(this,
                "\uD83D\uDD10 Akun dibuat & dikelola oleh Admin tim Anda. "
                        + "Hubungi Admin bila belum punya akun.",
                12, Ui.TEXT_DIM, false);
        note.setBackground(Ui.roundBg(0xFFEEF0FF, 14, this));
        note.setPadding(Ui.dp(this, 14), Ui.dp(this, 12),
                Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.topMargin = Ui.dp(this, 20);
        note.setLayoutParams(nlp);
        w.addView(note);

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

    /**
     * Field input: label + kotak putih rounded dengan ikon kiri.
     * Bila withEye: ikon mata kanan untuk tampil/sembunyi sandi.
     */
    private EditText inputWithIcon(LinearLayout parent, String label,
                                   String icon, int inputType, boolean withEye) {
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

        TextView ic = Ui.text(this, icon, 19, Ui.TEXT_DIM, false);
        ic.setPadding(0, 0, Ui.dp(this, 12), 0);
        box.addView(ic);

        final EditText et = new EditText(this);
        et.setBackground(null);
        et.setHint(label.equals("Email") ? "nama@email.com" : "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022");
        et.setHintTextColor(0xFFA6ABBF);
        et.setTextColor(Ui.TEXT);
        et.setTextSize(15);
        et.setInputType(inputType);
        et.setSingleLine(true);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        et.setLayoutParams(elp);
        box.addView(et);

        if (withEye) {
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
        }

        parent.addView(box);
        return et;
    }

    // ================= LUPA KATA SANDI =================

    private void showForgotDialog(String prefillEmail) {
        final Dialog d = new Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setGravity(Gravity.CENTER_HORIZONTAL);
        w.setPadding(Ui.dp(this, 24), Ui.dp(this, 28),
                Ui.dp(this, 24), Ui.dp(this, 24));
        w.setBackgroundColor(0xFFFFFFFF);

        TextView ill = Ui.text(this, "\uD83D\uDD11", 58, Ui.TEXT, false);
        ill.setGravity(Gravity.CENTER);
        ill.setPadding(0, 0, 0, Ui.dp(this, 16));
        w.addView(ill);

        TextView t = Ui.text(this, "Lupa kata sandi?", 20, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        w.addView(t);
        TextView s = Ui.text(this,
                "Masukkan email akun Anda. Tautan reset akan dikirim ke email tersebut.",
                13, Ui.TEXT_DIM, false);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 16));
        w.addView(s);

        final EditText et = new EditText(this);
        et.setHint("nama@email.com");
        et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        et.setText(prefillEmail);
        et.setSingleLine(true);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        elp.bottomMargin = Ui.dp(this, 16);
        et.setLayoutParams(elp);
        w.addView(et);

        TextView kirim = Ui.primaryButton(this, "Kirim Tautan Reset");
        kirim.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String em = et.getText().toString().trim();
                if (!em.contains("@")) {
                    Toast.makeText(LoginActivity.this, "Email tidak valid.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                FirebaseBridge.sendPasswordReset(LoginActivity.this, em,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(String info) {
                                Toast.makeText(LoginActivity.this, info,
                                        Toast.LENGTH_LONG).show();
                                d.dismiss();
                            }
                            public void onErr(String msg) {
                                Toast.makeText(LoginActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(kirim);

        TextView back = Ui.text(this, "Kembali ke Masuk", 13, Ui.TEXT_DIM, false);
        back.setGravity(Gravity.CENTER);
        back.setPadding(0, Ui.dp(this, 16), 0, 0);
        back.setClickable(true);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        w.addView(back);

        d.setContentView(w);
        d.show();
    }

    // ================= AKSI =================

    private void doLogin(String email, String pass) {
        if (!email.contains("@")) {
            Toast.makeText(this, "Email tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pass.length() < 6) {
            Toast.makeText(this, "Kata sandi minimal 6 karakter.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseBridge.signIn(this, email, pass, new FirebaseBridge.FbCallback() {
            public void onOk(String info) {
                Toast.makeText(LoginActivity.this, info, Toast.LENGTH_SHORT).show();
                // Sinkronkan role global dari users/{uid} agar badge role
                // & menu Admin akurat (sebelumnya Prefs role tidak pernah diisi).
                FirebaseBridge.syncGlobalRole(LoginActivity.this);
                // Cek wajib ganti password sebelum masuk aplikasi utama.
                FirebaseBridge.mustChangePassword(LoginActivity.this,
                        new FirebaseBridge.BoolCallback() {
                            public void onResult(boolean must) {
                                routeAfterLogin(must);
                            }
                        });
            }
            public void onErr(String msg) {
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Arahkan ke ChangePasswordActivity bila wajib, atau MainActivity. */
    private void routeAfterLogin(boolean mustChange) {
        Class<?> next = mustChange ? ChangePasswordActivity.class : MainActivity.class;
        Intent i = new Intent(LoginActivity.this, next);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ================= PROFIL (sudah login) =================

    private void showProfile(boolean fbReady) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 40), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setGravity(Gravity.CENTER_HORIZONTAL);

        String nm = "";
        String em = "";
        try {
            nm = FirebaseBridge.currentUserName(this);
            FirebaseUser u = FirebaseBridge.currentUser(this);
            if (u != null && u.getEmail() != null) em = u.getEmail();
        } catch (Throwable ignored) {}
        if (nm == null) nm = "";

        w.addView(Ui.badge(this, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(),
                Ui.PRIMARY, 72));
        TextView t = Ui.text(this, nm.isEmpty() ? "Pengguna" : nm, 20, Ui.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 10), 0, 0);
        w.addView(t);
        if (!em.isEmpty()) {
            TextView e = Ui.text(this, em, 14, Ui.TEXT_DIM, false);
            e.setGravity(Gravity.CENTER);
            w.addView(e);
        }

        String role = Prefs.getUserRole(this);
        TextView badge = Ui.text(this, FirebaseBridge.roleLabel(role), 13, 0xFFFFFFFF, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        int rc = FirebaseBridge.ROLE_ADMIN.equals(role) ? 0xFFEF4444
                : FirebaseBridge.ROLE_LEADER.equals(role) ? 0xFFF59E0B : 0xFF3B82F6;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(rc);
        bg.setCornerRadius(Ui.dp(this, 12));
        badge.setBackground(bg);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(this, 8);
        blp.bottomMargin = Ui.dp(this, 24);
        badge.setLayoutParams(blp);
        w.addView(badge);

        TextView out = Ui.outlineButton(this, "Keluar");
        out.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FirebaseBridge.signOut(LoginActivity.this);
                // Kembali ke layar login, bersihkan stack.
                Intent i = new Intent(LoginActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });
        w.addView(out);
        root.addView(w);
        setContentView(root);
    }
}
