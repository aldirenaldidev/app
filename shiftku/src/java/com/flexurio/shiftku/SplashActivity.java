package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseUser;

/**
 * Splash: logo ShiftKu saat aplikasi dibuka.
 * Alur: Splash -> Login (belum login) / MainActivity (sudah login).
 */
public class SplashActivity extends Activity {

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            buildSplash();
        } catch (Throwable t) {
            goNext();
            return;
        }
        // Seed akun admin default di background (tidak menghambat splash).
        try {
            FirebaseBridge.ensureDefaultAdmin(this,
                    new FirebaseBridge.FbCallback() {
                        public void onOk(String info) {}
                        public void onErr(String msg) {
                            // SEED_SKIP_CONFIG = Email/Password belum aktif di
                            // Console; bukan error fatal, splash tetap lanjut.
                            if ("SEED_SKIP_CONFIG".equals(msg)) {
                                android.util.Log.i("ShiftKu",
                                        "ensureDefaultAdmin dilewati: aktifkan "
                                        + "Email/Password di Firebase Console dulu.");
                                return;
                            }
                            android.util.Log.w("ShiftKu",
                                    "ensureDefaultAdmin: " + msg);
                        }
                    });
        } catch (Throwable t) {
            android.util.Log.w("ShiftKu", "ensureDefaultAdmin throw: " + t);
        }
        // Tahan splash ~1.2 detik agar logo terlihat, lalu lanjut.
        new Handler().postDelayed(new Runnable() {
            public void run() { goNext(); }
        }, 1200);
    }

    private void buildSplash() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF4845D6, 0xFF5B5BF0, 0xFF7C7CF4});
        root.setBackground(gd);
        root.setPadding(Ui.dp(this, 40), Ui.dp(this, 40), Ui.dp(this, 40), Ui.dp(this, 40));

        // Logo aplikasi (Opsi 1 — Kalender + Jam, pilihan user).
        android.widget.ImageView logoIv = new android.widget.ImageView(this);
        int ls = Ui.dp(this, 110);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ls, ls);
        llp.bottomMargin = Ui.dp(this, 26);
        logoIv.setLayoutParams(llp);
        try {
            logoIv.setImageResource(getResources().getIdentifier(
                    "logo_shiftku", "drawable", getPackageName()));
        } catch (Throwable ignored) {}
        root.addView(logoIv);

        TextView t = Ui.text(this, "ShiftKu", 40, 0xFFFFFFFF, true);
        t.setGravity(Gravity.CENTER);
        root.addView(t);

        TextView sub = Ui.text(this,
                "Kelola jadwal shift tim dengan mudah & rapi",
                15, 0xFFFFFFFF, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(this, 10), 0, 0);
        sub.setAlpha(0.88f);
        root.addView(sub);

        // versi + copyright di bawah
        TextView ver = Ui.text(this, "v1.1.8  \u00A9 aldire__", 12, 0xFFFFFFFF, false);
        ver.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = Ui.dp(this, 60);
        ver.setLayoutParams(vlp);
        ver.setAlpha(0.7f);
        root.addView(ver);

        setContentView(root);
    }

    private void goNext() {
        try {
            FirebaseUser u = FirebaseBridge.currentUser(this);
            if (u == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            // Sesi masih aktif: cek wajib ganti password sebelum ke Main.
            FirebaseBridge.mustChangePassword(this,
                    new FirebaseBridge.BoolCallback() {
                        public void onResult(boolean must) {
                            Class<?> next = must ? ChangePasswordActivity.class
                                    : MainActivity.class;
                            startActivity(new Intent(SplashActivity.this, next));
                            finish();
                        }
                    });
        } catch (Throwable t) {
            try {
                startActivity(new Intent(this, LoginActivity.class));
            } catch (Throwable ignored) {}
            finish();
        }
    }
}
