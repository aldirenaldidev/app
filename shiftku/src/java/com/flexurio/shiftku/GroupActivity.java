package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Daftar Grup Shift (mockup grup_list.png) — tema gelap. Firebase Firestore. */
public class GroupActivity extends Activity {

    private DbHelper db;
    private LinearLayout listWrap;
    private LinearLayout loginRow;
    private static final int[] MEMBER_COLORS =
            {0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B, 0xFFEF4444, 0xFF8B5CF6, 0xFF06B6D4};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        FirebaseBridge.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.DARK_BG);
        root.addView(Ui.darkHeader(this, this, "Grup Shift", "Berbagi jadwal dengan tim"));

        ScrollView sv = new ScrollView(this);
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 14));

        // status login
        loginRow = new LinearLayout(this);
        loginRow.setOrientation(LinearLayout.HORIZONTAL);
        loginRow.setGravity(Gravity.CENTER_VERTICAL);
        loginRow.setPadding(0, 0, 0, Ui.dp(this, 10));
        wrap.addView(loginRow);

        TextView create = Ui.text(this, "+ Buat / Gabung", 16, 0xFFFFFFFF, true);
        create.setGravity(Gravity.CENTER);
        create.setBackground(Ui.roundBg(Ui.DARK_ACCENT, 12, this));
        create.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        create.setClickable(true);
        create.setFocusable(true);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.bottomMargin = Ui.dp(this, 12);
        create.setLayoutParams(clp);
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showCreateJoinDialog(); }
        });
        wrap.addView(create);

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(listWrap);

        sv.addView(wrap);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);

        setContentView(root);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.safe("onResume", new Runnable() { public void run() { render(); } });
    }

    private void render() {
        renderLoginRow();
        renderLocalGroups();
        // sinkron daftar grup Firebase di background
        if (FirebaseBridge.currentUser(this) != null) {
            FirebaseBridge.listenMyGroups(this, new FirebaseBridge.GroupListCallback() {
                public void onOk(java.util.List<java.util.Map<String, Object>> groups) {
                    Ui.safe("mergeGroups", new Runnable() {
                        public void run() {
                            boolean changed = false;
                            for (java.util.Map<String, Object> g : groups) {
                                String fid = String.valueOf(g.get("id"));
                                boolean exists = false;
                                for (DbHelper.Group lg : db.getGroups()) {
                                    if (fid.equals(lg.fbId)) { exists = true; break; }
                                }
                                if (!exists) {
                                    db.addGroup(String.valueOf(g.get("name")),
                                            String.valueOf(g.get("inviteCode")),
                                            "", fid);
                                    changed = true;
                                }
                            }
                            if (changed) renderLocalGroups();
                        }
                    });
                }
                public void onErr(String msg) {}
            });
        }
    }

    private void renderLoginRow() {
        loginRow.removeAllViews();
        final String nm = FirebaseBridge.currentUserName(this);
        TextView t = Ui.text(this,
                nm == null ? "Belum masuk" : "Masuk sebagai " + nm,
                13, Ui.DARK_DIM, false);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        t.setLayoutParams(tlp);
        loginRow.addView(t);
        TextView b = Ui.text(this, nm == null ? "Masuk / Daftar" : "Akun",
                14, Ui.DARK_ACCENT, true);
        b.setClickable(true);
        b.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(GroupActivity.this, LoginActivity.class));
            }
        });
        loginRow.addView(b);
    }

    private void renderLocalGroups() {
        listWrap.removeAllViews();
        List<DbHelper.Group> groups = db.getGroups();
        if (groups.isEmpty()) {
            TextView t = Ui.text(this, "Belum ada grup. Buat grup baru atau gabung dengan kode undangan.",
                    14, Ui.DARK_DIM, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 30), 0, 0);
            listWrap.addView(t);
        }
        for (final DbHelper.Group g : groups) {
            LinearLayout card = Ui.card(this, true);
            card.setClickable(true);
            card.setFocusable(true);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tx.setLayoutParams(tlp);
            tx.addView(Ui.text(this, g.name, 17, Ui.DARK_TEXT, true));
            String[] members = g.members.isEmpty() ? new String[0] : g.members.split(",");
            tx.addView(Ui.text(this, members.length + " anggota", 13, Ui.DARK_DIM, false));
            TextView code = Ui.text(this, "Kode: " + g.code +
                    (g.fbId != null ? "  •  tersinkron" : ""), 12, Ui.DARK_ACCENT, true);
            code.setPadding(0, Ui.dp(this, 6), 0, 0);
            tx.addView(code);
            row.addView(tx);
            LinearLayout av = new LinearLayout(this);
            av.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = 0; i < Math.min(3, members.length); i++) {
                String m = members[i].trim();
                View b = Ui.badge(this, m.isEmpty() ? "?" : m.substring(0, 1).toUpperCase(),
                        MEMBER_COLORS[i % MEMBER_COLORS.length], 32);
                LinearLayout.LayoutParams blp = (LinearLayout.LayoutParams) b.getLayoutParams();
                blp.leftMargin = i == 0 ? 0 : Ui.dp(this, -8);
                b.setLayoutParams(blp);
                av.addView(b);
            }
            row.addView(av);
            card.addView(row);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(GroupActivity.this, GroupDetailActivity.class);
                    i.putExtra("name", g.name);
                    i.putExtra("code", g.code);
                    i.putExtra("members", g.members);
                    if (g.fbId != null) i.putExtra("fb_id", g.fbId);
                    startActivity(i);
                }
            });
            final long gid = g.id;
            card.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    db.deleteGroup(gid);
                    renderLocalGroups();
                    Toast.makeText(GroupActivity.this, "Grup dihapus dari HP.",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            listWrap.addView(card);
        }
    }

    private void showCreateJoinDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.DARK_CARD);
        w.addView(Ui.text(this, "Buat / Gabung Grup", 18, Ui.DARK_TEXT, true));

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Nama grup baru");
        etName.setTextColor(Ui.DARK_TEXT);
        etName.setHintTextColor(Ui.DARK_DIM);
        w.addView(etName);
        final android.widget.EditText etMembers = new android.widget.EditText(this);
        etMembers.setHint("Anggota (pisah koma): Aldi, Budi, Citra");
        etMembers.setTextColor(Ui.DARK_TEXT);
        etMembers.setHintTextColor(Ui.DARK_DIM);
        w.addView(etMembers);

        TextView save = Ui.text(this, "Buat Grup", 16, 0xFFFFFFFF, true);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.roundBg(Ui.DARK_ACCENT, 12, this));
        save.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        save.setClickable(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 8);
        save.setLayoutParams(slp);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String name = etName.getText().toString().trim().isEmpty()
                        ? "Grup Baru" : etName.getText().toString().trim();
                final String members = etMembers.getText().toString().trim();
                if (FirebaseBridge.currentUser(GroupActivity.this) == null) {
                    Toast.makeText(GroupActivity.this,
                            "Masuk dulu untuk membuat grup tersinkron.",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(GroupActivity.this, LoginActivity.class));
                    return;
                }
                save.setText("Membuat...");
                save.setClickable(false);
                FirebaseBridge.createGroup(GroupActivity.this, name, members,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(final String code) {
                                // dapatkan fb id via query ulang
                                FirebaseBridge.listenMyGroups(GroupActivity.this,
                                        new FirebaseBridge.GroupListCallback() {
                                            public void onOk(java.util.List<java.util.Map<String, Object>> gs) {
                                                String fid = null;
                                                for (java.util.Map<String, Object> g : gs) {
                                                    if (code.equals(g.get("inviteCode"))) {
                                                        fid = String.valueOf(g.get("id"));
                                                        break;
                                                    }
                                                }
                                                db.addGroup(name, code, members, fid);
                                                d.dismiss();
                                                render();
                                                Toast.makeText(GroupActivity.this,
                                                        "Grup dibuat. Kode: " + code,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                            public void onErr(String msg) {
                                                db.addGroup(name, code, members, null);
                                                d.dismiss();
                                                render();
                                            }
                                        });
                            }
                            public void onErr(String msg) {
                                save.setText("Buat Grup");
                                save.setClickable(true);
                                Toast.makeText(GroupActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(save);

        w.addView(Ui.text(this, "— atau —", 13, Ui.DARK_DIM, false));
        final android.widget.EditText etCode = new android.widget.EditText(this);
        etCode.setHint("Kode undangan (mis. PAGI-2026)");
        etCode.setTextColor(Ui.DARK_TEXT);
        etCode.setHintTextColor(Ui.DARK_DIM);
        w.addView(etCode);
        TextView join = Ui.text(this, "Gabung", 15, Ui.DARK_ACCENT, true);
        join.setGravity(Gravity.CENTER);
        join.setPadding(0, Ui.dp(this, 10), 0, 0);
        join.setClickable(true);
        join.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String code = etCode.getText().toString().trim().toUpperCase();
                if (code.isEmpty()) return;
                if (FirebaseBridge.currentUser(GroupActivity.this) == null) {
                    Toast.makeText(GroupActivity.this,
                            "Masuk dulu untuk gabung grup.",
                            Toast.LENGTH_LONG).show();
                    startActivity(new Intent(GroupActivity.this, LoginActivity.class));
                    return;
                }
                join.setText("Mencari...");
                join.setClickable(false);
                FirebaseBridge.joinGroup(GroupActivity.this, code,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(final String gname) {
                                FirebaseBridge.listenMyGroups(GroupActivity.this,
                                        new FirebaseBridge.GroupListCallback() {
                                            public void onOk(java.util.List<java.util.Map<String, Object>> gs) {
                                                String fid = null;
                                                for (java.util.Map<String, Object> g : gs) {
                                                    if (code.equals(g.get("inviteCode"))) {
                                                        fid = String.valueOf(g.get("id"));
                                                        break;
                                                    }
                                                }
                                                db.addGroup(gname, code, "", fid);
                                                d.dismiss();
                                                render();
                                                Toast.makeText(GroupActivity.this,
                                                        "Bergabung ke " + gname,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                            public void onErr(String msg) {
                                                db.addGroup(gname, code, "", null);
                                                d.dismiss();
                                                render();
                                            }
                                        });
                            }
                            public void onErr(String msg) {
                                join.setText("Gabung");
                                join.setClickable(true);
                                Toast.makeText(GroupActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(join);

        d.setContentView(w);
        d.show();
    }
}
