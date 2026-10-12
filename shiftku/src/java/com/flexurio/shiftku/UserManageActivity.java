package com.flexurio.shiftku;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Kelola Pengguna (khusus Admin).
 * Daftar semua akun dari koleksi Firestore "users": tambah pengguna baru,
 * ubah role, kirim reset kata sandi, hapus pengguna.
 * Tema terang, konsisten dengan tab Lainnya.
 */
public class UserManageActivity extends Activity {

    private LinearLayout body;
    private List<Map<String, Object>> users = new ArrayList<Map<String, Object>>();
    private ListenerRegistration reg;

    private static final int[] PALETTE = {0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B,
            0xFFEF4444, 0xFF8B5CF6, 0xFF06B6D4, 0xFFEC4899, 0xFF14B8D6};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        FirebaseBridge.init(this);
        // Pengaman ganda: hanya admin boleh membuka layar ini.
        if (!FirebaseBridge.isAdmin(Prefs.getUserRole(this))) {
            Toast.makeText(this, "Hanya Admin yang bisa mengelola pengguna.",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Kelola Pengguna"));

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        sv.addView(body);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);
        setContentView(root);

        reg = FirebaseBridge.listenUsers(this, new FirebaseBridge.UserListCallback() {
            public void onData(List<Map<String, Object>> list) {
                users = list;
                render();
            }
            public void onErr(String msg) {
                Toast.makeText(UserManageActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
        render();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }

    private void render() {
        body.removeAllViews();

        TextView add = Ui.primaryButton(this, "+ Tambah Pengguna");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddUserDialog(); }
        });
        body.addView(add);

        TextView hint = Ui.text(this,
                "Semua akun dibuat & dikelola oleh Admin. Tidak ada pendaftaran mandiri.",
                12, Ui.TEXT_DIM, false);
        hint.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 12));
        body.addView(hint);

        body.addView(Ui.text(this, "Pengguna (" + users.size() + ")", 15, Ui.TEXT, true));

        if (users.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada data pengguna.", 14, Ui.TEXT_DIM, false));
            body.addView(c);
            return;
        }

        final String myUid = FirebaseBridge.currentUid(this);
        int ci = 0;
        for (final Map<String, Object> m : users) {
            final String uid = String.valueOf(m.get("_uid"));
            final String nm = safe(m.get("name"));
            final String em = safe(m.get("email"));
            final String role = safe(m.get("role"));

            LinearLayout card = Ui.card(this);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(Ui.badge(this, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(),
                    PALETTE[ci % PALETTE.length], 44));
            ci++;

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(ilp);
            info.setPadding(Ui.dp(this, 10), 0, 0, 0);
            info.addView(Ui.text(this, nm.isEmpty() ? "(tanpa nama)" : nm, 16, Ui.TEXT, true));
            if (!em.isEmpty()) info.addView(Ui.text(this, em, 13, Ui.TEXT_DIM, false));
            // Tandai akun sendiri.
            if (uid.equals(myUid)) {
                info.addView(Ui.text(this, "• akun ini", 12, Ui.TEXT_DIM, false));
            }
            row.addView(info);

            TextView badge = Ui.text(this, FirebaseBridge.roleLabel(role), 12, 0xFFFFFFFF, true);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 5));
            int rc = FirebaseBridge.ROLE_ADMIN.equals(role) ? Ui.RED
                    : FirebaseBridge.ROLE_LEADER.equals(role) ? Ui.AMBER : Ui.PRIMARY;
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(rc);
            bg.setCornerRadius(Ui.dp(this, 10));
            badge.setBackground(bg);
            row.addView(badge);

            row.addView(Ui.text(this, "  >", 20, Ui.TEXT_DIM, true));
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showUserDialog(m, uid.equals(myUid)); }
            });
            card.addView(row);
            body.addView(card);
        }
    }

    private static String safe(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        return "null".equals(s) ? "" : s;
    }

    // ================= TAMBAH PENGGUNA =================

    private void showAddUserDialog() {
        final Dialog d = new Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Tambah Pengguna Baru", 18, Ui.TEXT, true));
        TextView hint = Ui.text(this,
                "Akun dibuat oleh Admin. Pengguna wajib ganti kata sandi saat login pertama. "
                + "Setelah dibuat, Anda akan keluar dan perlu login kembali.",
                12, Ui.TEXT_DIM, false);
        hint.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        w.addView(hint);

        final EditText etName = new EditText(this);
        etName.setHint("Nama lengkap");
        w.addView(etName);
        final EditText etEmail = new EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        w.addView(etEmail);
        final EditText etPass = new EditText(this);
        etPass.setHint("Kata sandi awal (min. 6 karakter)");
        etPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        w.addView(etPass);

        w.addView(Ui.text(this, "Role", 14, Ui.TEXT, true));
        final String[] roles = {FirebaseBridge.ROLE_USER, FirebaseBridge.ROLE_LEADER,
                FirebaseBridge.ROLE_ADMIN};
        final String[] labels = {"User — anggota biasa", "Leader — ketua tim",
                "Admin — pengelola penuh"};
        final Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        w.addView(sp);

        TextView buat = Ui.primaryButton(this, "Buat Akun");
        buat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String nm = etName.getText().toString().trim();
                final String em = etEmail.getText().toString().trim();
                final String pw = etPass.getText().toString();
                if (nm.isEmpty()) {
                    Toast.makeText(UserManageActivity.this, "Isi nama pengguna.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!em.contains("@")) {
                    Toast.makeText(UserManageActivity.this, "Email tidak valid.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pw.length() < 6) {
                    Toast.makeText(UserManageActivity.this,
                            "Kata sandi minimal 6 karakter.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int pos = sp.getSelectedItemPosition();
                final String role = (pos >= 0 && pos < roles.length)
                        ? roles[pos] : FirebaseBridge.ROLE_USER;
                Toast.makeText(UserManageActivity.this, "Membuat akun…",
                        Toast.LENGTH_SHORT).show();
                FirebaseBridge.createAccount(UserManageActivity.this, em, pw,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(final String newUid) {
                                FirebaseBridge.writeUserDoc(UserManageActivity.this,
                                        newUid, nm, em, role,
                                        new FirebaseBridge.FbCallback() {
                                            public void onOk(String i) {
                                                afterAccountCreated(d);
                                            }
                                            public void onErr(String m) {
                                                // Dokumen gagal ditulis, tapi akun Auth
                                                // sudah jadi — tetap lanjut.
                                                afterAccountCreated(d);
                                            }
                                        });
                            }
                            public void onErr(String msg) {
                                Toast.makeText(UserManageActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(buat);
        d.setContentView(w);
        d.show();
    }

    /** Firebase otomatis login sebagai user baru -> keluarkan, admin login ulang. */
    private void afterAccountCreated(Dialog d) {
        try { d.dismiss(); } catch (Throwable ignored) {}
        FirebaseBridge.signOut(UserManageActivity.this);
        Toast.makeText(this,
                "Akun pengguna dibuat. Silakan login kembali sebagai Admin.",
                Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ================= KELOLA PENGGUNA =================

    private void showUserDialog(final Map<String, Object> m, final boolean isSelf) {
        final String uid = String.valueOf(m.get("_uid"));
        final String nm = safe(m.get("name"));
        final String em = safe(m.get("email"));
        final String curRole = safe(m.get("role"));

        final Dialog d = new Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Kelola: " + (nm.isEmpty() ? em : nm), 18, Ui.TEXT, true));
        if (!em.isEmpty()) {
            TextView e = Ui.text(this, em, 13, Ui.TEXT_DIM, false);
            e.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 12));
            w.addView(e);
        }

        // Ubah role.
        w.addView(Ui.text(this, "Role", 14, Ui.TEXT, true));
        final String[] roles = {FirebaseBridge.ROLE_USER, FirebaseBridge.ROLE_LEADER,
                FirebaseBridge.ROLE_ADMIN};
        final String[] labels = {"User — anggota biasa", "Leader — ketua tim",
                "Admin — pengelola penuh"};
        final Spinner sp = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        for (int i = 0; i < roles.length; i++) {
            if (roles[i].equals(curRole)) { sp.setSelection(i); break; }
        }
        w.addView(sp);

        TextView simpan = Ui.primaryButton(this, "Simpan Role");
        if (isSelf) {
            simpan.setAlpha(0.4f);
            simpan.setClickable(false);
        }
        simpan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int pos = sp.getSelectedItemPosition();
                final String nr = (pos >= 0 && pos < roles.length) ? roles[pos]
                        : FirebaseBridge.ROLE_USER;
                FirebaseBridge.setGlobalRole(UserManageActivity.this, uid, nr,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(String info) {
                                Toast.makeText(UserManageActivity.this, info,
                                        Toast.LENGTH_SHORT).show();
                                d.dismiss();
                            }
                            public void onErr(String msg) {
                                Toast.makeText(UserManageActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(simpan);
        if (isSelf) {
            TextView note = Ui.text(this, "Tidak bisa mengubah role akun sendiri.",
                    12, Ui.TEXT_DIM, false);
            note.setGravity(Gravity.CENTER);
            note.setPadding(0, Ui.dp(this, 4), 0, 0);
            w.addView(note);
        }

        // Kirim email reset kata sandi.
        if (!em.isEmpty()) {
            TextView reset = Ui.text(this, "Kirim email reset kata sandi", 14,
                    Ui.PRIMARY, true);
            reset.setGravity(Gravity.CENTER);
            reset.setPadding(0, Ui.dp(this, 14), 0, 0);
            reset.setClickable(true);
            reset.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FirebaseBridge.sendPasswordReset(UserManageActivity.this, em,
                            new FirebaseBridge.FbCallback() {
                                public void onOk(String info) {
                                    Toast.makeText(UserManageActivity.this, info,
                                            Toast.LENGTH_LONG).show();
                                    d.dismiss();
                                }
                                public void onErr(String msg) {
                                    Toast.makeText(UserManageActivity.this, msg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });
            w.addView(reset);
        }

        // Hapus pengguna.
        TextView hapus = Ui.text(this, "Hapus pengguna", 14, Ui.RED, true);
        hapus.setGravity(Gravity.CENTER);
        hapus.setPadding(0, Ui.dp(this, 14), 0, 0);
        if (isSelf) {
            hapus.setAlpha(0.4f);
            hapus.setClickable(false);
        } else {
            hapus.setClickable(true);
            hapus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FirebaseBridge.deleteUserDoc(UserManageActivity.this, uid,
                            new FirebaseBridge.FbCallback() {
                                public void onOk(String info) {
                                    Toast.makeText(UserManageActivity.this,
                                            info + " (Akun login dihapus via Firebase Console bila perlu.)",
                                            Toast.LENGTH_LONG).show();
                                    d.dismiss();
                                }
                                public void onErr(String msg) {
                                    Toast.makeText(UserManageActivity.this, msg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });
        }
        w.addView(hapus);

        d.setContentView(w);
        d.show();
    }
}
