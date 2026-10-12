package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detail grup: kalender grup sinkron Firestore + tukar shift + rekap.
 * Tema gelap (mockup grup_calendar.png).
 */
public class GroupDetailActivity extends Activity {

    private DbHelper db;
    private String name, code, fbId;
    private String[] members;
    private int[] memberColors;
    private Map<String, Integer> uidColor = new HashMap<String, Integer>();
    private Map<String, String> uidName = new HashMap<String, String>();
    private LinearLayout body;
    private int year, month;

    // data firestore: date -> (uid -> entry{shiftName, shiftColor, name})
    private Map<String, Map<String, Object>> fsSchedule = new HashMap<String, Map<String, Object>>();
    private List<Map<String, Object>> fsSwaps = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> fbMembers = new ArrayList<Map<String, Object>>();
    private String myRole = null; // role saya di grup ini (user/leader/admin)
    private ListenerRegistration regSchedule, regSwaps, regMembers;

    private static final int[] PALETTE = {0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B,
            0xFFEF4444, 0xFF8B5CF6, 0xFF06B6D4, 0xFFEC4899, 0xFF14B8A6};

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    private void onCreateSafe() {
        db = new DbHelper(this);
        FirebaseBridge.init(this);
        name = getIntent().getStringExtra("name");
        code = getIntent().getStringExtra("code");
        fbId = getIntent().getStringExtra("fb_id");
        String m = getIntent().getStringExtra("members");
        members = (m == null || m.isEmpty()) ? new String[]{"Saya"} : m.split(",");
        for (int i = 0; i < members.length; i++) members[i] = members[i].trim();
        memberColors = new int[members.length];
        for (int i = 0; i < members.length; i++) memberColors[i] = PALETTE[i % PALETTE.length];

        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.DARK_BG);
        root.addView(Ui.darkHeader(this, this, name,
                DateUtil.BULAN[month] + " " + year + (fbId != null ? "  •  tersinkron" : "")));

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 14));
        sv.addView(body);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);
        setContentView(root);

        if (fbId != null) attachListeners();
        render();
    }

    private void attachListeners() {
        regSchedule = FirebaseBridge.listenSchedule(this, fbId,
                new FirebaseBridge.ScheduleCallback() {
                    public void onData(Map<String, Map<String, Object>> byDate) {
                        fsSchedule = byDate;
                        // kumpulkan uid->nama/warna
                        for (Map<String, Object> day : byDate.values()) {
                            for (Map.Entry<String, Object> e : day.entrySet()) {
                                if (!uidName.containsKey(e.getKey()) && e.getValue() instanceof Map) {
                                    Map<String, Object> en = (Map<String, Object>) e.getValue();
                                    Object nm = en.get("name");
                                    uidName.put(e.getKey(), nm == null ? "?" : String.valueOf(nm));
                                    int ci = uidColor.size() % PALETTE.length;
                                    uidColor.put(e.getKey(), PALETTE[ci]);
                                }
                            }
                        }
                        render();
                    }
                    public void onErr(String msg) {
                        Toast.makeText(GroupDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
        regSwaps = FirebaseBridge.listenSwaps(this, fbId,
                new FirebaseBridge.SwapListCallback() {
                    public void onData(List<Map<String, Object>> swaps) {
                        fsSwaps = swaps;
                        render();
                    }
                    public void onErr(String msg) {}
                });
        // Role saya + daftar anggota (untuk badge & pembatasan aksi).
        FirebaseBridge.getMyRole(this, fbId, new FirebaseBridge.RoleCallback() {
            public void onRole(String role) {
                myRole = role;
                render();
            }
        });
        regMembers = FirebaseBridge.listenMembers(this, fbId,
                new FirebaseBridge.MemberListCallback() {
                    public void onData(List<Map<String, Object>> members) {
                        fbMembers = members;
                        render();
                    }
                    public void onErr(String msg) {}
                });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (regSchedule != null) regSchedule.remove();
        if (regSwaps != null) regSwaps.remove();
        if (regMembers != null) regMembers.remove();
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.safe("onResume", new Runnable() { public void run() { render(); } });
    }

    private void render() {
        body.removeAllViews();
        renderLegend();
        renderCalendar();
        renderToday();
        renderSwaps();
        renderRecap();
        renderMembers();
        renderExport();
        if (fbId != null) {
            TextView hint = Ui.text(this,
                    "Ketuk tanggal untuk mengisi shift Anda. Perubahan tersinkron otomatis ke semua anggota.",
                    12, Ui.DARK_DIM, false);
            hint.setPadding(0, Ui.dp(this, 10), 0, 0);
            body.addView(hint);
        }
    }

    private void renderLegend() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, Ui.dp(this, 10));
        if (fbId != null && !uidName.isEmpty()) {
            for (Map.Entry<String, String> e : uidName.entrySet()) {
                row.addView(legendItem(e.getValue(), uidColor.get(e.getKey())));
            }
        } else {
            for (int i = 0; i < members.length; i++) {
                row.addView(legendItem(members[i], memberColors[i]));
            }
        }
        body.addView(row);
    }

    private LinearLayout legendItem(String nm, int color) {
        LinearLayout it = new LinearLayout(this);
        it.setOrientation(LinearLayout.HORIZONTAL);
        it.setGravity(Gravity.CENTER_VERTICAL);
        it.setPadding(0, 0, Ui.dp(this, 14), 0);
        it.addView(Ui.badge(this, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(), color, 22));
        it.addView(Ui.text(this, " " + nm, 13, Ui.DARK_DIM, false));
        return it;
    }

    // ---------- kalender ----------
    private void renderCalendar() {
        LinearLayout hw = new LinearLayout(this);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        for (String d : DateUtil.HARI_SHORT) {
            TextView tv = Ui.text(this, d, 12, Ui.DARK_DIM, false);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            hw.addView(tv);
        }
        body.addView(hw);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK);
        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = DateUtil.todayYmd();

        int cell = 0;
        LinearLayout row = null;
        for (int i = 1; i < firstDow; i++) {
            if (row == null) { row = calRow(); body.addView(row); }
            row.addView(calEmpty());
            cell++;
        }
        for (int d = 1; d <= days; d++) {
            if (cell % 7 == 0) { row = calRow(); body.addView(row); }
            final String date = DateUtil.ymd(year, month, d);
            row.addView(calDay(d, date, date.equals(today)));
            cell++;
        }
        while (cell % 7 != 0) { row.addView(calEmpty()); cell++; }

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, Ui.dp(this, 8), 0, 0);
        TextView prev = Ui.text(this, "< Bulan lalu", 14, Ui.DARK_ACCENT, true);
        prev.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        prev.setClickable(true);
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                month--; if (month < 0) { month = 11; year--; }
                render();
            }
        });
        nav.addView(prev);
        TextView next = Ui.text(this, "Bulan depan >", 14, Ui.DARK_ACCENT, true);
        next.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        next.setClickable(true);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                month++; if (month > 11) { month = 0; year++; }
                render();
            }
        });
        nav.addView(next);
        body.addView(nav);
    }

    private LinearLayout calRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
        return r;
    }

    private View calEmpty() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f));
        return v;
    }

    private View calDay(int day, final String date, boolean isToday) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1f);
        cell.setLayoutParams(lp);
        if (isToday) cell.setBackground(Ui.roundBg(0xFF1E2A45, 10, this));
        TextView tv = Ui.text(this, String.valueOf(day), 13,
                isToday ? Ui.DARK_ACCENT : Ui.DARK_TEXT, isToday);
        tv.setGravity(Gravity.CENTER);
        cell.addView(tv);
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        Map<String, Object> dayMap = fsSchedule.get(date);
        if (dayMap != null) {
            for (Map.Entry<String, Object> e : dayMap.entrySet()) {
                Integer col = uidColor.get(e.getKey());
                if (col == null) col = 0xFF3B82F6;
                dots.addView(dot(col));
            }
        } else if (fbId == null) {
            // mode lokal: simulasi deterministik
            for (int i = 0; i < members.length; i++) {
                if ((day + i * 2) % 7 >= 5) continue;
                dots.addView(dot(memberColors[i]));
            }
        }
        cell.addView(dots);
        if (fbId != null) {
            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showSetShiftDialog(date); }
            });
        }
        return cell;
    }

    private View dot(int color) {
        View d = new View(this);
        android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        gd.setColor(color);
        d.setBackground(gd);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                Ui.dp(this, 7), Ui.dp(this, 7));
        dlp.leftMargin = Ui.dp(this, 2);
        dlp.rightMargin = Ui.dp(this, 2);
        dlp.topMargin = Ui.dp(this, 3);
        d.setLayoutParams(dlp);
        return d;
    }

    private void showSetShiftDialog(final String date) {
        final List<Shift> shifts = db.getShifts();
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.DARK_CARD);
        w.addView(Ui.text(this, "Shift saya — " + DateUtil.pretty(date), 16, Ui.DARK_TEXT, true));
        for (final Shift s : shifts) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            row.addView(Ui.badge(this, s.initial(), s.color, 32));
            TextView lb = Ui.text(this, "  " + s.name + "  (" + s.jam() + ")",
                    15, Ui.DARK_TEXT, false);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lb.setLayoutParams(tlp);
            row.addView(lb);
            row.setClickable(true);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FirebaseBridge.setGroupSchedule(GroupDetailActivity.this,
                            fbId, date, s.name, s.color);
                    d.dismiss();
                    Toast.makeText(GroupDetailActivity.this,
                            "Jadwal tersimpan & tersinkron.", Toast.LENGTH_SHORT).show();
                }
            });
            w.addView(row);
        }
        d.setContentView(w);
        d.show();
    }

    // ---------- ringkasan hari ini ----------
    private void renderToday() {
        String today = DateUtil.todayYmd();
        Map<String, Object> dayMap = fsSchedule.get(today);
        LinearLayout card = Ui.card(this, true);
        if (dayMap != null && !dayMap.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> e : dayMap.entrySet()) {
                String nm = uidName.get(e.getKey());
                if (nm == null) nm = "?";
                if (sb.length() > 0) sb.append(", ");
                sb.append(nm);
            }
            card.addView(Ui.text(this, "Hari ini: " + dayMap.size() + " anggota terjadwal",
                    15, Ui.DARK_TEXT, true));
            TextView t = Ui.text(this, sb.toString(), 13, Ui.DARK_DIM, false);
            t.setPadding(0, Ui.dp(this, 4), 0, 0);
            card.addView(t);
        } else {
            card.addView(Ui.text(this, "Hari ini: belum ada jadwal terisi",
                    15, Ui.DARK_TEXT, true));
            TextView t = Ui.text(this, "Ketuk tanggal di kalender untuk mengisi shift Anda.",
                    13, Ui.DARK_DIM, false);
            t.setPadding(0, Ui.dp(this, 4), 0, 0);
            card.addView(t);
        }
        LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) card.getLayoutParams();
        clp.topMargin = Ui.dp(this, 10);
        card.setLayoutParams(clp);
        body.addView(card);
    }

    // ---------- tukar shift ----------
    private void renderSwaps() {
        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.HORIZONTAL);
        sec.setGravity(Gravity.CENTER_VERTICAL);
        sec.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        TextView title = Ui.text(this, "Tukar Shift", 17, Ui.DARK_TEXT, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        sec.addView(title);
        TextView add = Ui.text(this, "+ Minta", 14, Ui.DARK_ACCENT, true);
        add.setClickable(true);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showSwapDialog(); }
        });
        sec.addView(add);
        body.addView(sec);

        if (fbId != null) {
            if (fsSwaps.isEmpty()) {
                body.addView(Ui.text(this, "Belum ada permintaan tukar shift.",
                        13, Ui.DARK_DIM, false));
            }
            for (final Map<String, Object> s : fsSwaps) {
                LinearLayout card = Ui.card(this, true);
                card.addView(Ui.text(this,
                        String.valueOf(s.get("date")) + " — " +
                        String.valueOf(s.get("fromName")) + " → " +
                        String.valueOf(s.get("toName")),
                        14, Ui.DARK_TEXT, true));
                Object rsn = s.get("reason");
                String reason = rsn == null ? "" : String.valueOf(rsn).trim();
                if (!reason.isEmpty()) {
                    TextView tvR = Ui.text(this, "Alasan: " + reason, 13, Ui.DARK_DIM, false);
                    tvR.setPadding(0, Ui.dp(this, 4), 0, 0);
                    card.addView(tvR);
                }
                String st = String.valueOf(s.get("status"));
                int col = st.equals("Disetujui") ? Ui.GREEN
                        : st.equals("Ditolak") ? Ui.RED : Ui.AMBER;
                card.addView(Ui.text(this, st, 13, col, false));
                // Hanya Leader/Admin yang boleh menyetujui/menolak.
                boolean bolehApprove = fbId == null || FirebaseBridge.canApprove(myRole);
                if (st.equals("Menunggu") && bolehApprove) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, Ui.dp(this, 8), 0, 0);
                    final String sid = String.valueOf(s.get("_id"));
                    TextView ok = Ui.text(this, "Setujui", 14, Ui.GREEN, true);
                    ok.setPadding(0, 0, Ui.dp(this, 20), 0);
                    ok.setClickable(true);
                    ok.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            FirebaseBridge.setSwapStatus(GroupDetailActivity.this,
                                    fbId, sid, "Disetujui");
                        }
                    });
                    row.addView(ok);
                    TextView no = Ui.text(this, "Tolak", 14, Ui.RED, true);
                    no.setClickable(true);
                    no.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            FirebaseBridge.setSwapStatus(GroupDetailActivity.this,
                                    fbId, sid, "Ditolak");
                        }
                    });
                    row.addView(no);
                    card.addView(row);
                }
                body.addView(card);
            }
        } else {
            // mode lokal via SQLite
            List<DbHelper.Swap> swaps = db.getSwaps(code);
            if (swaps.isEmpty()) {
                body.addView(Ui.text(this, "Belum ada permintaan tukar shift.",
                        13, Ui.DARK_DIM, false));
            }
            for (final DbHelper.Swap s : swaps) {
                LinearLayout card = Ui.card(this, true);
                card.addView(Ui.text(this, s.date + " — " + s.fromUser + " → " + s.toUser,
                        14, Ui.DARK_TEXT, true));
                if (s.reason != null && !s.reason.isEmpty()) {
                    TextView tvR = Ui.text(this, "Alasan: " + s.reason, 13, Ui.DARK_DIM, false);
                    tvR.setPadding(0, Ui.dp(this, 4), 0, 0);
                    card.addView(tvR);
                }
                int col = s.status.equals("Disetujui") ? Ui.GREEN
                        : s.status.equals("Ditolak") ? Ui.RED : Ui.AMBER;
                card.addView(Ui.text(this, s.status, 13, col, false));
                if (s.status.equals("Menunggu")) {
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(0, Ui.dp(this, 8), 0, 0);
                    TextView ok = Ui.text(this, "Setujui", 14, Ui.GREEN, true);
                    ok.setPadding(0, 0, Ui.dp(this, 20), 0);
                    ok.setClickable(true);
                    ok.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            db.setSwapStatus(s.id, "Disetujui");
                            render();
                        }
                    });
                    row.addView(ok);
                    TextView no = Ui.text(this, "Tolak", 14, Ui.RED, true);
                    no.setClickable(true);
                    no.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            db.setSwapStatus(s.id, "Ditolak");
                            render();
                        }
                    });
                    row.addView(no);
                    card.addView(row);
                }
                body.addView(card);
            }
        }
    }

    // ---------- anggota + role ----------

    private void renderMembers() {
        if (fbId == null || fbMembers.isEmpty()) return;
        body.addView(Ui.text(this, "Anggota (" + fbMembers.size() + ")", 15, Ui.DARK_TEXT, true));
        final boolean sayaAdmin = FirebaseBridge.isAdmin(myRole);
        final boolean bisaLihatAbsen = FirebaseBridge.canApprove(myRole);
        final String myUid = FirebaseBridge.currentUid(this);
        // Tombol Tambah Anggota (khusus Admin): akun dibuat oleh admin, bukan daftar mandiri.
        if (sayaAdmin) {
            TextView btnAdd = Ui.text(this, "+ Tambah Anggota", 14, Ui.DARK_ACCENT, true);
            btnAdd.setGravity(Gravity.CENTER);
            btnAdd.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
            btnAdd.setClickable(true);
            btnAdd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showAddMemberDialog(); }
            });
            body.addView(btnAdd);
        }
        // Tombol Absensi Tim untuk Leader/Admin.
        if (bisaLihatAbsen) {
            TextView btnAbsen = Ui.text(this, "📷 Lihat Absensi Tim", 14, Ui.DARK_ACCENT, true);
            btnAbsen.setGravity(Gravity.CENTER);
            btnAbsen.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
            btnAbsen.setClickable(true);
            final String gid = fbId;
            btnAbsen.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    android.content.Intent i = new android.content.Intent(
                            GroupDetailActivity.this, AbsenActivity.class);
                    i.putExtra("fbId", gid);
                    startActivity(i);
                }
            });
            body.addView(btnAbsen);
        }
        for (final Map<String, Object> m : fbMembers) {
            final String uid = String.valueOf(m.get("_uid"));
            String nm = String.valueOf(m.get("name"));
            String role = String.valueOf(m.get("role"));
            if (role == null || role.equals("null")) role = FirebaseBridge.ROLE_USER;

            LinearLayout card = Ui.card(this, true);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            Object colObj = m.get("color");
            int col = (colObj instanceof Number)
                    ? ((Number) colObj).intValue() : 0xFF3B82F6;
            row.addView(Ui.badge(this, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(),
                    col, 32));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(Ui.dp(this, 10), 0, 0, 0);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tx.setLayoutParams(tlp);
            tx.addView(Ui.text(this, nm, 14, Ui.DARK_TEXT, true));
            int rc = FirebaseBridge.ROLE_ADMIN.equals(role) ? Ui.RED
                    : FirebaseBridge.ROLE_LEADER.equals(role) ? Ui.AMBER : Ui.DARK_DIM;
            tx.addView(Ui.text(this, FirebaseBridge.roleLabel(role), 12, rc, true));
            row.addView(tx);

            // Admin bisa ubah role / hapus anggota (kecuali dirinya sendiri).
            if (sayaAdmin && myUid != null && !myUid.equals(uid)
                    && !String.valueOf(uid).startsWith("local_")) {
                final String fRole = role;
                TextView kelola = Ui.text(this, "Kelola", 13, Ui.PRIMARY, true);
                kelola.setClickable(true);
                kelola.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { showMemberDialog(m, fRole); }
                });
                row.addView(kelola);
            }
            card.addView(row);
            body.addView(card);
        }
    }

    /**
     * Admin membuat akun anggota baru.
     * Alur: createUser -> set nama -> tambah ke members -> signOut ->
     * kembali ke Login (admin login ulang dengan akunnya sendiri).
     */
    private void showAddMemberDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.DARK_CARD);
        w.addView(Ui.text(this, "Tambah Anggota Baru", 16, Ui.DARK_TEXT, true));
        TextView hint = Ui.text(this,
                "Akun dibuat oleh Admin. Setelah dibuat, Anda akan keluar dan perlu login kembali.",
                12, Ui.DARK_DIM, false);
        hint.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        w.addView(hint);

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Nama lengkap");
        etName.setTextColor(Ui.DARK_TEXT);
        etName.setHintTextColor(Ui.DARK_DIM);
        w.addView(etName);
        final android.widget.EditText etEmail = new android.widget.EditText(this);
        etEmail.setHint("Email");
        etEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        etEmail.setTextColor(Ui.DARK_TEXT);
        etEmail.setHintTextColor(Ui.DARK_DIM);
        w.addView(etEmail);
        final android.widget.EditText etPass = new android.widget.EditText(this);
        etPass.setHint("Kata sandi awal (min. 6 karakter)");
        etPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etPass.setTextColor(Ui.DARK_TEXT);
        etPass.setHintTextColor(Ui.DARK_DIM);
        w.addView(etPass);

        final String[] roles = {FirebaseBridge.ROLE_USER, FirebaseBridge.ROLE_LEADER,
                FirebaseBridge.ROLE_ADMIN};
        final String[] labels = {"User — anggota biasa", "Leader — ketua tim",
                "Admin — pengelola grup"};
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
                    Toast.makeText(GroupDetailActivity.this, "Isi nama anggota.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!em.contains("@")) {
                    Toast.makeText(GroupDetailActivity.this, "Email tidak valid.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (pw.length() < 6) {
                    Toast.makeText(GroupDetailActivity.this,
                            "Kata sandi minimal 6 karakter.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int pos = sp.getSelectedItemPosition();
                final String role = (pos >= 0 && pos < roles.length)
                        ? roles[pos] : FirebaseBridge.ROLE_USER;
                Toast.makeText(GroupDetailActivity.this, "Membuat akun…",
                        Toast.LENGTH_SHORT).show();
                FirebaseBridge.createAccount(GroupDetailActivity.this, em, pw,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(final String newUid) {
                                // Set nama tampilan, lalu daftarkan ke grup.
                                FirebaseBridge.setDisplayName(GroupDetailActivity.this, nm,
                                        new FirebaseBridge.FbCallback() {
                                            public void onOk(String i) { addToGroup(newUid, nm, em, role, d); }
                                            public void onErr(String m) { addToGroup(newUid, nm, em, role, d); }
                                        });
                            }
                            public void onErr(String msg) {
                                Toast.makeText(GroupDetailActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(buat);
        d.setContentView(w);
        d.show();
    }

    private void addToGroup(String newUid, String nm, String em, String role,
                            final android.app.Dialog d) {
        // Tulis dokumen global users/{uid}: role + wajib ganti password
        // saat login pertama (best-effort; kegagalan tidak menghentikan alur).
        try {
            FirebaseBridge.writeUserDoc(GroupDetailActivity.this, newUid, nm, em, role,
                    new FirebaseBridge.FbCallback() {
                        public void onOk(String i) {}
                        public void onErr(String m) {}
                    });
        } catch (Throwable ignored) {}
        FirebaseBridge.addMemberDoc(GroupDetailActivity.this, fbId, newUid, nm, em, role,
                new FirebaseBridge.FbCallback() {
                    public void onOk(String info) {
                        d.dismiss();
                        // Firebase otomatis login sebagai user baru -> keluarkan,
                        // admin login ulang dengan akunnya sendiri.
                        FirebaseBridge.signOut(GroupDetailActivity.this);
                        Toast.makeText(GroupDetailActivity.this,
                                "Akun anggota dibuat. Silakan login kembali sebagai Admin.",
                                Toast.LENGTH_LONG).show();
                        android.content.Intent i = new android.content.Intent(
                                GroupDetailActivity.this, LoginActivity.class);
                        i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                    public void onErr(String msg) {
                        Toast.makeText(GroupDetailActivity.this, msg,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showMemberDialog(final Map<String, Object> m, String curRole) {
        final String uid = String.valueOf(m.get("_uid"));
        final String nm = String.valueOf(m.get("name"));
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.DARK_CARD);
        w.addView(Ui.text(this, "Kelola: " + nm, 16, Ui.DARK_TEXT, true));

        final String[] roles = {FirebaseBridge.ROLE_USER, FirebaseBridge.ROLE_LEADER,
                FirebaseBridge.ROLE_ADMIN};
        final String[] labels = {"User — anggota biasa", "Leader — ketua tim",
                "Admin — pengelola grup"};
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
        simpan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int pos = sp.getSelectedItemPosition();
                final String nr = (pos >= 0 && pos < roles.length) ? roles[pos]
                        : FirebaseBridge.ROLE_USER;
                FirebaseBridge.setMemberRole(GroupDetailActivity.this, fbId, uid, nr,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(String info) {
                                Toast.makeText(GroupDetailActivity.this, info,
                                        Toast.LENGTH_SHORT).show();
                                d.dismiss();
                            }
                            public void onErr(String msg) {
                                Toast.makeText(GroupDetailActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(simpan);

        // Admin bisa mengirim email reset kata sandi ke anggota.
        final Object emObj = m.get("email");
        final String memberEmail = (emObj == null) ? "" : String.valueOf(emObj);
        if (!memberEmail.isEmpty() && !memberEmail.equals("null")) {
            TextView reset = Ui.text(this, "Kirim email reset kata sandi", 14,
                    Ui.DARK_ACCENT, true);
            reset.setGravity(Gravity.CENTER);
            reset.setPadding(0, Ui.dp(this, 12), 0, 0);
            reset.setClickable(true);
            reset.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    FirebaseBridge.sendPasswordReset(GroupDetailActivity.this, memberEmail,
                            new FirebaseBridge.FbCallback() {
                                public void onOk(String info) {
                                    Toast.makeText(GroupDetailActivity.this, info,
                                            Toast.LENGTH_LONG).show();
                                    d.dismiss();
                                }
                                public void onErr(String msg) {
                                    Toast.makeText(GroupDetailActivity.this, msg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            });
            w.addView(reset);
        }

        TextView hapus = Ui.text(this, "Hapus dari grup", 14, Ui.RED, true);
        hapus.setGravity(Gravity.CENTER);
        hapus.setPadding(0, Ui.dp(this, 12), 0, 0);
        hapus.setClickable(true);
        hapus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FirebaseBridge.removeMember(GroupDetailActivity.this, fbId, uid,
                        new FirebaseBridge.FbCallback() {
                            public void onOk(String info) {
                                Toast.makeText(GroupDetailActivity.this, info,
                                        Toast.LENGTH_SHORT).show();
                                d.dismiss();
                            }
                            public void onErr(String msg) {
                                Toast.makeText(GroupDetailActivity.this, msg,
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
        w.addView(hapus);

        d.setContentView(w);
        d.show();
    }

    // ---------- export jadwal ----------

    private void renderExport() {
        LinearLayout sec = new LinearLayout(this);
        sec.setOrientation(LinearLayout.HORIZONTAL);
        sec.setPadding(0, Ui.dp(this, 12), 0, 0);

        TextView csv = Ui.outlineButton(this, "⬇ Excel (CSV)");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Ui.dp(this, 8);
        csv.setLayoutParams(lp);
        csv.setClickable(true);
        csv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Ui.safe("exportCsv", new Runnable() { public void run() {
                    ExportHelper.exportScheduleCsv(GroupDetailActivity.this,
                            name, year, month, fbId, fsSchedule, uidName,
                            db, code);
                }});
            }
        });
        sec.addView(csv);

        TextView pdf = Ui.outlineButton(this, "⬇ PDF");
        pdf.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        pdf.setClickable(true);
        pdf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Ui.safe("exportPdf", new Runnable() { public void run() {
                    ExportHelper.exportSchedulePdf(GroupDetailActivity.this,
                            name, year, month, fbId, fsSchedule, uidName,
                            db, code);
                }});
            }
        });
        sec.addView(pdf);
        body.addView(sec);
    }

    private void showSwapDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.DARK_CARD);
        w.addView(Ui.text(this, "Minta Tukar Shift", 18, Ui.DARK_TEXT, true));

        final android.widget.EditText etDate = new android.widget.EditText(this);
        etDate.setHint("Tanggal (2026-10-20)");
        etDate.setText(DateUtil.todayYmd());
        etDate.setTextColor(Ui.DARK_TEXT);
        etDate.setHintTextColor(Ui.DARK_DIM);
        w.addView(etDate);

        // Pilih anggota dari daftar (Spinner), bukan ketik manual.
        TextView lblTo = Ui.text(this, "Tukar dengan:", 13, Ui.DARK_DIM, false);
        w.addView(lblTo);
        final android.widget.Spinner spTo = new android.widget.Spinner(this);
        final java.util.List<String> memberNames = new java.util.ArrayList<String>();
        memberNames.add("Siapa saja");
        String meName = FirebaseBridge.currentUserName(GroupDetailActivity.this);
        if (fbId != null) {
            for (java.util.Map<String, Object> m : fbMembers) {
                Object nm = m.get("name");
                String n = nm == null ? "" : String.valueOf(nm).trim();
                if (!n.isEmpty() && !n.equals(meName)) memberNames.add(n);
            }
        } else {
            for (String mn : members) {
                if (!mn.equals(meName)) memberNames.add(mn);
            }
        }
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, memberNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(adapter);
        w.addView(spTo);

        TextView lblReason = Ui.text(this, "Alasan (wajib):", 13, Ui.DARK_DIM, false);
        w.addView(lblReason);
        final android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("cth: ada acara keluarga, sakit, dll.");
        etReason.setTextColor(Ui.DARK_TEXT);
        etReason.setHintTextColor(Ui.DARK_DIM);
        w.addView(etReason);

        TextView save = Ui.text(this, "Kirim Permintaan", 16, 0xFFFFFFFF, true);
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
                String to = String.valueOf(spTo.getSelectedItem());
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(GroupDetailActivity.this,
                            "Alasan wajib diisi.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (fbId != null) {
                    FirebaseBridge.requestSwap(GroupDetailActivity.this, fbId,
                            etDate.getText().toString().trim(), to, reason,
                            new FirebaseBridge.FbCallback() {
                                public void onOk(String info) {
                                    d.dismiss();
                                    Toast.makeText(GroupDetailActivity.this, info,
                                            Toast.LENGTH_SHORT).show();
                                }
                                public void onErr(String msg) {
                                    Toast.makeText(GroupDetailActivity.this, msg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    String me = FirebaseBridge.currentUserName(GroupDetailActivity.this);
                    if (me == null) me = "Saya";
                    db.addSwap(code, etDate.getText().toString().trim(), 0, me, to, reason);
                    d.dismiss();
                    render();
                }
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }

    // ---------- rekap ----------
    private void renderRecap() {
        TextView title = Ui.text(this, "Rekap Bulan Ini", 17, Ui.DARK_TEXT, true);
        title.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 6));
        body.addView(title);

        Map<String, Integer> kerja = new HashMap<String, Integer>();
        if (fbId != null) {
            for (Map.Entry<String, Map<String, Object>> de : fsSchedule.entrySet()) {
                if (!de.getKey().startsWith(String.format("%04d-%02d", year, month + 1))) continue;
                for (String uid : de.getValue().keySet()) {
                    Integer n = kerja.get(uid);
                    kerja.put(uid, (n == null ? 1 : n + 1));
                }
            }
            if (kerja.isEmpty()) {
                body.addView(Ui.text(this, "Belum ada data bulan ini.",
                        13, Ui.DARK_DIM, false));
            }
            for (Map.Entry<String, Integer> e : kerja.entrySet()) {
                String nm = uidName.get(e.getKey());
                if (nm == null) nm = "?";
                Integer col = uidColor.get(e.getKey());
                body.addView(recapCard(nm, col == null ? 0xFF3B82F6 : col,
                        e.getValue()));
            }
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, 1);
            int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 0; i < members.length; i++) {
                int k = 0;
                for (int day = 1; day <= days; day++) {
                    if ((day + i * 2) % 7 < 5) k++;
                }
                body.addView(recapCard(members[i], memberColors[i], k));
            }
        }
    }

    private LinearLayout recapCard(String nm, int color, int shiftCount) {
        int jam = shiftCount * 8;
        int lembur = Math.max(0, jam - 160);
        LinearLayout card = Ui.card(this, true);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(Ui.badge(this, nm.isEmpty() ? "?" : nm.substring(0, 1).toUpperCase(),
                color, 36));
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(Ui.dp(this, 10), 0, 0, 0);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tx.setLayoutParams(tlp);
        tx.addView(Ui.text(this, nm, 15, Ui.DARK_TEXT, true));
        tx.addView(Ui.text(this, shiftCount + " shift • " + jam + " jam" +
                (lembur > 0 ? " • lembur " + lembur + " jam" : ""),
                13, Ui.DARK_DIM, false));
        row.addView(tx);
        card.addView(row);
        return card;
    }
}
