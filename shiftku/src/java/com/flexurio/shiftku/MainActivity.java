package com.flexurio.shiftku;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Beranda: tab Kalender | Shift | Alarm | Lainnya. */
public class MainActivity extends Activity {

    private DbHelper db;
    private LinearLayout content;
    private LinearLayout tabBar;
    private int tab = 0; // 0 kalender, 1 shift, 2 alarm, 3 lainnya
    private int year, month; // bulan tampil kalender

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(b); } });
    }

    private void onCreateSafe(Bundle b) {
        db = new DbHelper(this);
        FirebaseBridge.init(this);
        AdsHelper.init(this);

        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // header ungu
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(Ui.PRIMARY);
        header.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        TextView t = Ui.text(this, "ShiftKu", 22, 0xFFFFFFFF, true);
        header.addView(t);
        root.addView(header);

        ScrollView sv = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));
        sv.addView(content);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(slp);
        root.addView(sv);

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setBackgroundColor(Ui.CARD);
        tabBar.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        // garis divider di atas navbar
        View navDiv = new View(this);
        navDiv.setBackgroundColor(Ui.DIVIDER);
        navDiv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 1)));
        root.addView(navDiv);
        root.addView(tabBar);

        LinearLayout ad = new LinearLayout(this);
        ad.setId(View.generateViewId());
        ad.setOrientation(LinearLayout.HORIZONTAL);
        ad.setGravity(Gravity.CENTER);
        root.addView(ad);
        ad.setTag("ad_container");

        setContentView(root);
        buildTabs();
        render();
        showLastCrashIfAny();

        // Jadwalkan pengingat shift (otomatis, idempotent).
        Ui.safe("reminder", new Runnable() {
            public void run() { ShiftReminder.schedule(MainActivity.this); }
        });

        // pasang banner AdMob
        Ui.safe("ads", new Runnable() {
            public void run() {
                View av = (View) ad.getTag();
                AdsHelper.banner(MainActivity.this, ad.getId());
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.safe("onResume", new Runnable() {
            public void run() {
                db = new DbHelper(MainActivity.this);
                render();
                ShiftWidget.refreshAll(MainActivity.this);
                // Sinkronkan role global (best-effort) agar menu Admin
                // (Kelola Pengguna) muncul tepat setelah login.
                try {
                    FirebaseBridge.syncGlobalRole(MainActivity.this);
                } catch (Throwable ignored) {}
            }
        });
    }

    /** Dialog diagnostik: bila ada crash log dari sesi sebelumnya, tampilkan
     *  agar user bisa screenshot dan kirim ke developer. */
    private void showLastCrashIfAny() {
        Ui.safe("crashlog", new Runnable() { public void run() {
            final String log = ShiftKuApp.consumeCrashLog(getApplication());
            if (log == null) return;
            android.app.AlertDialog.Builder b =
                    new android.app.AlertDialog.Builder(MainActivity.this);
            b.setTitle("Info Diagnostik");
            TextView tv = new TextView(MainActivity.this);
            tv.setText("Aplikasi sempat menutup tak terduga. Berikut info teknisnya "
                    + "(screenshot lalu kirim ke developer):\n\n" + log);
            tv.setTextSize(11);
            tv.setPadding(Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 12),
                    Ui.dp(MainActivity.this, 16), Ui.dp(MainActivity.this, 12));
            ScrollView sv = new ScrollView(MainActivity.this);
            sv.addView(tv);
            b.setView(sv);
            b.setPositiveButton("Tutup", null);
            b.show();
        }});
    }

    private void buildTabs() {
        tabBar.removeAllViews();
        tabBar.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
        String[] labels = {"Kalender", "Shift", "Absensi", "Alarm", "Lainnya"};
        int[] icons = {R.drawable.ic_cal, R.drawable.ic_shift, R.drawable.ic_camera,
                R.drawable.ic_alarm, R.drawable.ic_more};
        // visual index -> content tab: 0->0, 1->1, 3->2, 4->3. Index 2 = Absensi (launcher).
        final int[] contentTab = {0, 1, -1, 2, 3};
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            item.setClickable(true);
            item.setFocusable(true);
            int ct = contentTab[i];
            boolean active = (ct >= 0 && tab == ct);
            ImageView iv = Ui.icon(this, icons[i], active ? Ui.PRIMARY : Ui.TEXT_DIM, 24);
            item.addView(iv);
            TextView lb = Ui.text(this, labels[i], 11, active ? Ui.PRIMARY : Ui.TEXT_DIM, active);
            lb.setGravity(Gravity.CENTER);
            lb.setSingleLine(true);
            lb.setPadding(0, Ui.dp(this, 2), 0, 0);
            item.addView(lb);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (idx == 2) {
                        // Absensi = launcher ke AbsenActivity, tab tidak berubah.
                        startActivity(new Intent(MainActivity.this, AbsenActivity.class));
                    } else {
                        tab = contentTab[idx];
                        buildTabs();
                        render();
                    }
                }
            });
            tabBar.addView(item);
        }
    }

    private void render() {
        content.removeAllViews();
        if (tab == 0) renderCalendar();
        else if (tab == 1) renderShiftList();
        else if (tab == 2) renderAlarms();
        else renderMore();
    }

    // ================= KALENDER =================
    private void renderCalendar() {
        // Banner pengumuman terbaru (bila ada yang belum dibaca) — di Beranda.
        try {
            final java.util.List<DbHelper.Pengumuman> pg = db.getPengumuman();
            if (pg != null && !pg.isEmpty()) {
                final DbHelper.Pengumuman latest = pg.get(0);
                LinearLayout banner = Ui.card(this);
                banner.setBackground(Ui.roundBg(0xFFEDEEFF, 12, this));
                LinearLayout brow = new LinearLayout(this);
                brow.setOrientation(LinearLayout.HORIZONTAL);
                brow.setGravity(Gravity.CENTER_VERTICAL);
                TextView btx = Ui.text(this, "📢 " + latest.title, 14, Ui.PRIMARY, true);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                btx.setLayoutParams(blp);
                brow.addView(btx);
                if (!latest.read) {
                    TextView nb = Ui.text(this, " BARU ", 11, 0xFFFFFFFF, true);
                    nb.setBackground(Ui.roundBg(Ui.RED, 8, this));
                    nb.setPadding(Ui.dp(this, 8), Ui.dp(this, 3),
                            Ui.dp(this, 8), Ui.dp(this, 3));
                    brow.addView(nb);
                }
                banner.addView(brow);
                banner.setClickable(true);
                banner.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this,
                                PengumumanActivity.class));
                    }
                });
                content.addView(banner);
            }
        } catch (Throwable ignored) {}

        // judul bulan + navigasi
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setPadding(0, 0, 0, Ui.dp(this, 10));
        TextView prev = navArrow("‹");
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                month--; if (month < 0) { month = 11; year--; }
                render();
            }
        });
        nav.addView(prev);
        TextView title = Ui.text(this, DateUtil.BULAN[month] + " " + year, 20, Ui.TEXT, true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(tlp);
        title.setGravity(Gravity.CENTER);
        nav.addView(title);
        TextView next = navArrow("›");
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                month++; if (month > 11) { month = 0; year++; }
                render();
            }
        });
        nav.addView(next);
        content.addView(nav);

        // header hari
        LinearLayout hw = new LinearLayout(this);
        hw.setOrientation(LinearLayout.HORIZONTAL);
        for (String d : DateUtil.HARI_SHORT) {
            TextView tv = Ui.text(this, d, 13, Ui.TEXT_DIM, false);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            hw.addView(tv);
        }
        hw.setPadding(0, 0, 0, Ui.dp(this, 6));
        content.addView(hw);

        // grid tanggal
        List<Shift> shifts = db.getShifts();
        Map<Long, Shift> map = new HashMap<Long, Shift>();
        for (Shift s : shifts) map.put(s.id, s);

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int firstDow = cal.get(Calendar.DAY_OF_WEEK); // 1=Min
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        String today = DateUtil.todayYmd();

        Map<String, Integer> count = new HashMap<String, Integer>();
        double totalHours = 0;

        int cell = 0;
        LinearLayout row = null;
        for (int i = 1; i < firstDow; i++) {
            if (row == null) { row = newRow(); content.addView(row); }
            row.addView(emptyCell());
            cell++;
        }
        for (int d = 1; d <= daysInMonth; d++) {
            if (cell % 7 == 0) { row = newRow(); content.addView(row); }
            final String date = DateUtil.ymd(year, month, d);
            long sid = db.getScheduleShift(date);
            final Shift sh = map.get(sid);
            row.addView(dayCell(d, sh, date.equals(today), date));
            if (sh != null) {
                Integer n = count.get(sh.name);
                count.put(sh.name, (n == null ? 1 : n + 1));
                totalHours += sh.hours();
            }
            cell++;
        }
        while (cell % 7 != 0) { row.addView(emptyCell()); cell++; }

        // legenda (3 kolom; baris terakhir tidak meregang)
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int totalShifts = shifts.size();
        int li = 0;
        LinearLayout legend = newLegendRow(llp);
        for (Shift s : shifts) {
            li++;
            boolean lastRow = (li > totalShifts - (totalShifts % 3 == 0 ? 3 : totalShifts % 3));
            boolean incompleteRow = (totalShifts % 3 != 0) && lastRow;
            LinearLayout it = new LinearLayout(this);
            it.setOrientation(LinearLayout.HORIZONTAL);
            it.setGravity(Gravity.CENTER_VERTICAL);
            if (incompleteRow) {
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                ip.rightMargin = Ui.dp(this, 16);
                it.setLayoutParams(ip);
            } else {
                it.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            }
            it.addView(Ui.badge(this, s.initial(), s.color, 22));
            TextView lb = Ui.text(this, " " + s.name, 13, Ui.TEXT, false);
            it.addView(lb);
            legend.addView(it);
            if (li % 3 == 0 && li < totalShifts) {
                content.addView(legend);
                legend = newLegendRow(llp);
            }
        }
        content.addView(legend);

        // ringkasan
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "Ringkasan " + DateUtil.BULAN[month], 16, Ui.TEXT, true));
        StringBuilder sb = new StringBuilder();
        for (Shift s : shifts) {
            Integer n = count.get(s.name);
            if (n != null && n > 0) {
                if (sb.length() > 0) sb.append("   ");
                sb.append(s.name).append(": ").append(n);
            }
        }
        TextView sum = Ui.text(this, sb.length() == 0 ? "Belum ada jadwal bulan ini." : sb.toString(),
                14, Ui.TEXT_DIM, false);
        sum.setPadding(0, Ui.dp(this, 6), 0, 0);
        card.addView(sum);
        TextView hrs = Ui.text(this, "Total jam kerja: ± " + (int) totalHours + " jam",
                14, Ui.TEXT_DIM, false);
        card.addView(hrs);
        LinearLayout.LayoutParams clp = (LinearLayout.LayoutParams) card.getLayoutParams();
        clp.topMargin = Ui.dp(this, 10);
        card.setLayoutParams(clp);
        content.addView(card);

        // Export jadwal pribadi
        final int fYear = year, fMonth = month;
        TextView expTitle = Ui.text(this, "Export Jadwal", 14, Ui.TEXT_DIM, true);
        expTitle.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 6));
        content.addView(expTitle);
        LinearLayout expRow = new LinearLayout(this);
        expRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView csvBtn = Ui.outlineButton(this, "⬇ Excel (CSV)");
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        elp.rightMargin = Ui.dp(this, 8);
        csvBtn.setLayoutParams(elp);
        csvBtn.setClickable(true);
        csvBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Ui.safe("exportCsv", new Runnable() { public void run() {
                    ExportHelper.exportScheduleCsv(MainActivity.this,
                            "Jadwal Pribadi", fYear, fMonth, null, null, null, db, null);
                }});
            }
        });
        expRow.addView(csvBtn);
        TextView pdfBtn = Ui.outlineButton(this, "⬇ PDF");
        pdfBtn.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        pdfBtn.setClickable(true);
        pdfBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Ui.safe("exportPdf", new Runnable() { public void run() {
                    ExportHelper.exportSchedulePdf(MainActivity.this,
                            "Jadwal Pribadi", fYear, fMonth, null, null, null, db, null);
                }});
            }
        });
        expRow.addView(pdfBtn);
        content.addView(expRow);
    }

    /** Tombol panah navigasi bulan: lingkaran ungu muda dengan chevron rapi. */
    private TextView navArrow(String symbol) {
        TextView t = Ui.text(this, symbol, 22, Ui.PRIMARY, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.roundBg(0xFFE8E9FF, 20, this));
        int s = Ui.dp(this, 40);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    private LinearLayout newLegendRow(LinearLayout.LayoutParams llp) {
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.HORIZONTAL);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        legend.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        legend.setLayoutParams(llp);
        return legend;
    }

    private LinearLayout newRow() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private View emptyCell() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
        int m = Ui.dp(this, 2);
        lp.setMargins(m, m, m, m);
        v.setLayoutParams(lp);
        return v;
    }

    private View dayCell(int day, final Shift sh, boolean isToday, final String date) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        // Kartu putih rounded seperti mockup; hari ini highlight ungu muda.
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isToday ? 0xFFE8E9FF : 0xFFFFFFFF);
        bg.setCornerRadius(Ui.dp(this, 8));
        bg.setStroke(Ui.dp(this, 1), isToday ? Ui.PRIMARY : 0xFFE4E7F0);
        cell.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 54), 1f);
        int m = Ui.dp(this, 2);
        lp.setMargins(m, m, m, m);
        cell.setLayoutParams(lp);
        cell.setClickable(true);
        cell.setFocusable(true);
        int p = Ui.dp(this, 5);
        cell.setPadding(p, Ui.dp(this, 3), p, Ui.dp(this, 3));

        // Angka tanggal kiri-atas.
        TextView tv = Ui.text(this, String.valueOf(day), 13,
                isToday ? Ui.PRIMARY : Ui.TEXT, isToday);
        cell.addView(tv);

        // Spacer: dorong badge ke bawah.
        View spacer = new View(this);
        cell.addView(spacer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        // Badge shift kanan-bawah.
        if (sh != null) {
            LinearLayout badgeRow = new LinearLayout(this);
            badgeRow.setOrientation(LinearLayout.HORIZONTAL);
            badgeRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            badgeRow.addView(Ui.badge(this, sh.initial(), sh.color, 22));
            cell.addView(badgeRow);
        }
        cell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showDayPopup(date, sh); }
        });
        return cell;
    }

    /** Popup info tanggal: shift, tombol Tukar Shift, Ajukan Cuti, Tutup. */
    private void showDayPopup(final String date, final Shift sh) {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackground(Ui.roundBg(Ui.CARD, 16, this));

        // Info tanggal
        w.addView(Ui.text(this, ExportHelper.tanggalID(date), 17, Ui.TEXT, true));

        // Info shift
        LinearLayout infoRow = new LinearLayout(this);
        infoRow.setOrientation(LinearLayout.HORIZONTAL);
        infoRow.setGravity(Gravity.CENTER_VERTICAL);
        infoRow.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        if (sh != null) {
            infoRow.addView(Ui.badge(this, sh.initial(), sh.color, 36));
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            tx.setPadding(Ui.dp(this, 12), 0, 0, 0);
            tx.addView(Ui.text(this, sh.name, 15, Ui.TEXT, true));
            tx.addView(Ui.text(this, sh.jam(), 13, Ui.TEXT_DIM, false));
            infoRow.addView(tx);
        } else {
            infoRow.addView(Ui.text(this, "Belum ada jadwal shift.", 14, Ui.TEXT_DIM, false));
        }
        w.addView(infoRow);

        // Tombol Tukar Shift
        TextView btnSwap = Ui.outlineButton(this, "🔄 Tukar Shift");
        btnSwap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); showSwapDialog(date, sh); }
        });
        w.addView(btnSwap);

        // Tombol Ajukan Cuti
        TextView btnLeave = Ui.outlineButton(this, "📝 Ajukan Cuti");
        btnLeave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); showLeaveDialog(date); }
        });
        w.addView(btnLeave);

        // Tombol Tutup
        TextView btnClose = Ui.text(this, "Tutup", 15, Ui.TEXT_DIM, true);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
        btnClose.setClickable(true);
        btnClose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d.dismiss(); }
        });
        w.addView(btnClose);

        d.setContentView(w);
        // Lebar dialog proporsional.
        android.view.Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            win.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    /** Dialog tukar shift dengan tanggal terisi otomatis. */
    private void showSwapDialog(final String date, final Shift sh) {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackground(Ui.roundBg(Ui.CARD, 16, this));
        w.addView(Ui.text(this, "Minta Tukar Shift", 18, Ui.TEXT, true));

        TextView info = Ui.text(this,
                ExportHelper.tanggalID(date)
                        + (sh != null ? " • Shift " + sh.name + " (" + sh.jam() + ")" : ""),
                13, Ui.TEXT_DIM, false);
        info.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        w.addView(info);

        // Pilih anggota dari daftar (Spinner), bukan ketik manual.
        w.addView(Ui.text(this, "Tukar dengan:", 13, Ui.TEXT_DIM, false));
        final android.widget.Spinner spTo = new android.widget.Spinner(this);
        final java.util.List<String> memberNames = new java.util.ArrayList<String>();
        memberNames.add("Siapa saja");
        final android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, memberNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(adapter);
        w.addView(spTo);
        // Muat daftar anggota grup dari Firestore (async).
        final String gidForMembers = currentGroupFbId();
        if (gidForMembers != null) {
            try {
                final com.google.firebase.firestore.ListenerRegistration reg =
                        FirebaseBridge.listenMembers(this, gidForMembers,
                                new FirebaseBridge.MemberListCallback() {
                                    public void onData(java.util.List<java.util.Map<String, Object>> members) {
                                        String me = Prefs.getUserName(MainActivity.this);
                                        memberNames.clear();
                                        memberNames.add("Siapa saja");
                                        for (java.util.Map<String, Object> m : members) {
                                            Object nm = m.get("name");
                                            String n = nm == null ? "" : String.valueOf(nm).trim();
                                            if (!n.isEmpty() && !n.equals(me)) memberNames.add(n);
                                        }
                                        adapter.notifyDataSetChanged();
                                    }
                                    public void onErr(String msg) {}
                                });
                // Listener dibiarkan aktif selama dialog terbuka; ditutup saat dialog tutup.
                d.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                    public void onDismiss(android.content.DialogInterface di) {
                        try { if (reg != null) reg.remove(); } catch (Throwable ignored) {}
                    }
                });
            } catch (Throwable ignored) {}
        }

        w.addView(Ui.text(this, "Alasan (wajib):", 13, Ui.TEXT_DIM, false));
        final android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("cth: ada acara keluarga, sakit, dll.");
        w.addView(etReason);

        TextView save = Ui.primaryButton(this, "Kirim Permintaan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String to = String.valueOf(spTo.getSelectedItem());
                String reason = etReason.getText().toString().trim();
                if (reason.isEmpty()) {
                    android.widget.Toast.makeText(MainActivity.this,
                            "Alasan wajib diisi.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                String me = Prefs.getUserName(MainActivity.this);
                if (me == null || me.isEmpty()) me = "Saya";
                long shiftId = (sh != null) ? sh.id : 0;
                // Simpan lokal; sinkron ke grup bila tersedia.
                String gid = currentGroupFbId();
                if (gid != null) {
                    FirebaseBridge.requestSwap(MainActivity.this, gid, date, to, reason,
                            new FirebaseBridge.FbCallback() {
                                public void onOk(String info) {
                                    d.dismiss();
                                    android.widget.Toast.makeText(MainActivity.this,
                                            info, android.widget.Toast.LENGTH_SHORT).show();
                                }
                                public void onErr(String msg) {
                                    android.widget.Toast.makeText(MainActivity.this,
                                            msg, android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    db.addSwap("pribadi", date, shiftId, me, to, reason);
                    d.dismiss();
                    android.widget.Toast.makeText(MainActivity.this,
                            "Permintaan tukar shift disimpan ✅", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        w.addView(save);
        d.setContentView(w);
        android.view.Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            win.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    /** Dialog ajukan cuti dengan tanggal terisi otomatis. */
    private void showLeaveDialog(final String date) {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackground(Ui.roundBg(Ui.CARD, 16, this));
        w.addView(Ui.text(this, "Ajukan Cuti", 18, Ui.TEXT, true));

        TextView info = Ui.text(this, "Mulai: " + ExportHelper.tanggalID(date),
                13, Ui.TEXT_DIM, false);
        info.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 10));
        w.addView(info);

        final android.widget.EditText etTo = new android.widget.EditText(this);
        etTo.setHint("Sampai (yyyy-MM-dd, opsional)");
        w.addView(etTo);
        final android.widget.EditText etNote = new android.widget.EditText(this);
        etNote.setHint("Keterangan (opsional)");
        w.addView(etNote);

        TextView save = Ui.primaryButton(this, "Ajukan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String to = etTo.getText().toString().trim();
                if (to.isEmpty()) to = date;
                db.addLeave(date, to, etNote.getText().toString().trim());
                d.dismiss();
                android.widget.Toast.makeText(MainActivity.this,
                        "Pengajuan cuti disimpan ✅", android.widget.Toast.LENGTH_SHORT).show();
                render();
            }
        });
        w.addView(save);
        d.setContentView(w);
        android.view.Window win = d.getWindow();
        if (win != null) {
            win.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
            win.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.85),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
        d.show();
    }

    /** Grup pertama yang user ikuti (cache lokal) — untuk sinkron tukar shift. */
    private String currentGroupFbId() {
        try {
            java.util.List<DbHelper.Group> gs = db.getGroups();
            for (DbHelper.Group g : gs) {
                if (g.fbId != null && !g.fbId.isEmpty()) return g.fbId;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    // ================= SHIFT (kelola template) =================
    private void renderShiftList() {
        content.addView(Ui.text(this, "Shift Kerja", 20, Ui.TEXT, true));
        TextView sub = Ui.text(this, "Kelola jenis shift. Ketuk tanggal di kalender untuk mengatur jadwal.",
                13, Ui.TEXT_DIM, false);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        content.addView(sub);

        for (final Shift s : db.getShifts()) {
            LinearLayout card = Ui.card(this);
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
            ImageView del = Ui.icon(this, R.drawable.ic_delete, Ui.RED, 40);
            del.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.deleteShift(s.id);
                    AlarmScheduler.scheduleAll(MainActivity.this);
                    render();
                }
            });
            row.addView(del);
            card.addView(row);
            content.addView(card);
        }

        TextView add = Ui.outlineButton(this, "+ Tambah Shift Baru");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddShiftDialog(); }
        });
        content.addView(add);
    }

    private void showAddShiftDialog() {
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Shift Baru", 18, Ui.TEXT, true));

        final android.widget.EditText etName = new android.widget.EditText(this);
        etName.setHint("Nama shift (mis. Pagi)");
        w.addView(etName);
        final android.widget.EditText etStart = new android.widget.EditText(this);
        etStart.setHint("Jam mulai (07:00)");
        w.addView(etStart);
        final android.widget.EditText etEnd = new android.widget.EditText(this);
        etEnd.setHint("Jam selesai (15:00)");
        w.addView(etEnd);

        // pilih warna
        final int[] colors = {0xFFF87171, 0xFFFBBF24, 0xFF6366F1, 0xFF34D399, 0xFF9CA3AF, 0xFF3B82F6};
        final int[] sel = {0};
        final LinearLayout[] colorRow = new LinearLayout[1];
        colorRow[0] = new LinearLayout(this);
        colorRow[0].setOrientation(LinearLayout.HORIZONTAL);
        colorRow[0].setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        final View[] dots = new View[colors.length];
        for (int i = 0; i < colors.length; i++) {
            final int idx = i;
            View dot = Ui.badge(this, "", colors[i], 36);
            LinearLayout.LayoutParams dlp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            dlp.rightMargin = Ui.dp(this, 8);
            dot.setLayoutParams(dlp);
            dot.setClickable(true);
            dot.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    sel[0] = idx;
                    for (int j = 0; j < dots.length; j++) {
                        dots[j].setAlpha(j == idx ? 1f : 0.35f);
                    }
                }
            });
            if (i > 0) dot.setAlpha(0.35f);
            dots[i] = dot;
            colorRow[0].addView(dot);
        }
        w.addView(colorRow[0]);

        TextView save = Ui.primaryButton(this, "Simpan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String n = etName.getText().toString().trim();
                if (n.isEmpty()) n = "Shift Baru";
                db.addShift(n, etStart.getText().toString().trim(),
                        etEnd.getText().toString().trim(), colors[sel[0]], false);
                d.dismiss();
                render();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }

    // ================= ALARM =================
    private void renderAlarms() {
        content.addView(Ui.text(this, "Alarm Shift", 20, Ui.TEXT, true));
        TextView sub = Ui.text(this, "Alarm berbunyi setiap hari pada jam yang ditentukan.",
                13, Ui.TEXT_DIM, false);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 12));
        content.addView(sub);

        List<DbHelper.AlarmItem> alarms = db.getAlarms();
        if (alarms.isEmpty()) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.text(this, "Belum ada alarm. Tambahkan alarm untuk tiap shift.",
                    14, Ui.TEXT_DIM, false));
            content.addView(card);
        }
        for (final DbHelper.AlarmItem a : alarms) {
            LinearLayout card = Ui.card(this);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout tx = new LinearLayout(this);
            tx.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tx.setLayoutParams(tlp);
            String nm = a.shiftName != null ? a.shiftName : "Shift";
            tx.addView(Ui.text(this, "Shift " + nm, 16, Ui.TEXT, true));
            tx.addView(Ui.text(this, a.time, 13, Ui.TEXT_DIM, false));
            row.addView(tx);

            final TextView toggle = new TextView(this);
            toggle.setText(a.enabled ? "ON" : "OFF");
            toggle.setTextColor(0xFFFFFFFF);
            toggle.setGravity(Gravity.CENTER);
            toggle.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
            toggle.setBackground(Ui.roundBg(a.enabled ? Ui.PRIMARY : 0xFFB0B5C3, 20, this));
            toggle.setClickable(true);
            toggle.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.setAlarmEnabled(a.id, !a.enabled);
                    AlarmScheduler.scheduleAll(MainActivity.this);
                    render();
                }
            });
            row.addView(toggle);

            ImageView del = Ui.icon(this, R.drawable.ic_delete, Ui.RED, 40);
            del.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
            del.setClickable(true);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.deleteAlarm(a.id);
                    AlarmScheduler.scheduleAll(MainActivity.this);
                    render();
                }
            });
            row.addView(del);
            card.addView(row);
            content.addView(card);
        }

        TextView add = Ui.text(this, "+ Tambah alarm", 15, Ui.PRIMARY, true);
        add.setPadding(0, Ui.dp(this, 8), 0, 0);
        add.setClickable(true);
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAddAlarmDialog(); }
        });
        content.addView(add);
    }

    private void showAddAlarmDialog() {
        final List<Shift> shifts = db.getShifts();
        if (shifts.isEmpty()) return;
        final android.app.Dialog d = new android.app.Dialog(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20), Ui.dp(this, 20));
        w.setBackgroundColor(Ui.CARD);
        w.addView(Ui.text(this, "Alarm Baru", 18, Ui.TEXT, true));

        final String[] names = new String[shifts.size()];
        for (int i = 0; i < shifts.size(); i++) names[i] = shifts.get(i).name;
        final android.widget.Spinner sp = new android.widget.Spinner(this);
        android.widget.ArrayAdapter<String> ad = new android.widget.ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, names);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(ad);
        w.addView(sp);

        final android.widget.EditText etTime = new android.widget.EditText(this);
        etTime.setHint("Jam (05:30)");
        w.addView(etTime);

        TextView save = Ui.primaryButton(this, "Simpan");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String t = etTime.getText().toString().trim();
                if (!t.matches("\\d{1,2}:\\d{2}")) t = "05:30";
                db.addAlarm(shifts.get(sp.getSelectedItemPosition()).id, t);
                AlarmScheduler.scheduleAll(MainActivity.this);
                d.dismiss();
                render();
            }
        });
        w.addView(save);
        d.setContentView(w);
        d.show();
    }

    // ================= LAINNYA =================
    private void renderMore() {
        content.addView(Ui.text(this, "Lainnya", 20, Ui.TEXT, true));
        // Menu Kelola Pengguna & Laporan hanya untuk Admin/Leader (role global).
        final boolean sayaAdmin = FirebaseBridge.isAdmin(Prefs.getUserRole(this));
        final boolean bisaApprove = FirebaseBridge.canApprove(Prefs.getUserRole(this));
        final int unread = new DbHelper(this).countPengumumanUnread();
        final String lblPengumuman = "Pengumuman" + (unread > 0 ? " (" + unread + " baru)" : "");

        java.util.List<String[]> items = new java.util.ArrayList<String[]>();
        items.add(new String[]{lblPengumuman, "ic_info"});
        items.add(new String[]{"Lembur", "ic_shift"});
        items.add(new String[]{"Izin / Sakit", "ic_cuti"});
        items.add(new String[]{"Kelola Cuti", "ic_cuti"});
        items.add(new String[]{"Pengingat Shift", "ic_alarm"});
        if (bisaApprove) items.add(new String[]{"Laporan Kehadiran", "ic_group"});
        items.add(new String[]{"Grup Shift", "ic_group"});
        if (sayaAdmin) items.add(new String[]{"Kelola Pengguna", "ic_group"});
        items.add(new String[]{"Akun", "ic_person"});
        items.add(new String[]{"Tentang ShiftKu", "ic_info"});

        for (String[] it : items) {
            LinearLayout card = Ui.card(this);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int res = getResources().getIdentifier(it[1], "drawable", getPackageName());
            row.addView(Ui.icon(this, res, Ui.PRIMARY, 30));
            TextView lb = Ui.text(this, "  " + it[0], 16, Ui.TEXT, false);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lb.setLayoutParams(tlp);
            row.addView(lb);
            row.addView(Ui.text(this, ">", 20, Ui.TEXT_DIM, true));
            row.setClickable(true);
            row.setFocusable(true);
            final String name = it[0];
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { openMore(name); }
            });
            card.addView(row);
            content.addView(card);
        }
    }

    private void openMore(String name) {
        if (name.startsWith("Pengumuman")) {
            startActivity(new Intent(this, PengumumanActivity.class));
        } else if (name.equals("Lembur")) {
            startActivity(new Intent(this, LemburActivity.class));
        } else if (name.equals("Izin / Sakit")) {
            startActivity(new Intent(this, IzinActivity.class));
        } else if (name.equals("Kelola Cuti")) {
            startActivity(new Intent(this, LeaveActivity.class));
        } else if (name.equals("Pengingat Shift")) {
            startActivity(new Intent(this, ReminderActivity.class));
        } else if (name.equals("Laporan Kehadiran")) {
            if (FirebaseBridge.canApprove(Prefs.getUserRole(this))) {
                startActivity(new Intent(this, ReportActivity.class));
            }
        } else if (name.equals("Grup Shift")) {
            startActivity(new Intent(this, GroupActivity.class));
        } else if (name.equals("Kelola Pengguna")) {
            // Pengaman: pastikan hanya admin yang bisa membuka.
            if (FirebaseBridge.isAdmin(Prefs.getUserRole(this))) {
                startActivity(new Intent(this, UserManageActivity.class));
            }
        } else if (name.equals("Akun")) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, AboutActivity.class));
        }
    }
}
