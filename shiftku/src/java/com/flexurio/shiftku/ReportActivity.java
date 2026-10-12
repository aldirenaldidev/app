package com.flexurio.shiftku;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Laporan Kehadiran bulanan.
 * - Pribadi: rekap dari data perangkat ini.
 * - Tim (Leader/Admin): per anggota dari data Firestore.
 * - Export rapi ke Excel (CSV) & PDF.
 */
public class ReportActivity extends Activity {

    private int year, month; // month 0-based
    private boolean modeTim = false;
    private List<ReportHelper.Row> rows;
    private String groupName = "Pribadi";
    private String fbId;

    private LinearLayout listWrap;
    private TextView tvPeriode;
    private com.google.firebase.firestore.ListenerRegistration absenReg;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.safe("onCreate", new Runnable() { public void run() { onCreateSafe(); } });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { if (absenReg != null) absenReg.remove(); } catch (Throwable ignored) {}
    }

    private void onCreateSafe() {
        Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        fbId = getIntent().getStringExtra("fbId");
        if (getIntent().getStringExtra("groupName") != null) {
            groupName = getIntent().getStringExtra("groupName");
        }
        boolean bisaLihatTim = FirebaseBridge.canApprove(Prefs.getUserRole(this))
                && fbId != null;
        modeTim = bisaLihatTim; // default tim bila bisa

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        root.addView(Ui.header(this, this, "Laporan Kehadiran"));

        ScrollView sv = new ScrollView(this);
        LinearLayout w = new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL);
        w.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14));

        // Navigasi bulan
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView prev = navBtn("‹");
        prev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { geser(-1); }
        });
        nav.addView(prev);
        tvPeriode = Ui.text(this, "", 17, Ui.TEXT, true);
        tvPeriode.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvPeriode.setLayoutParams(plp);
        nav.addView(tvPeriode);
        TextView next = navBtn("›");
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { geser(1); }
        });
        nav.addView(next);
        w.addView(nav);

        // Toggle Pribadi/Tim
        if (bisaLihatTim) {
            LinearLayout tg = new LinearLayout(this);
            tg.setOrientation(LinearLayout.HORIZONTAL);
            tg.setPadding(0, Ui.dp(this, 10), 0, 0);
            final TextView bP = toggleBtn("Pribadi");
            final TextView bT = toggleBtn("Tim");
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.rightMargin = Ui.dp(this, 8);
            bP.setLayoutParams(lp);
            bT.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            final Runnable ref = new Runnable() {
                public void run() {
                    bP.setBackground(Ui.roundBg(!modeTim ? Ui.PRIMARY : 0xFFE8EAF2, 10,
                            ReportActivity.this));
                    bP.setTextColor(!modeTim ? 0xFFFFFFFF : Ui.TEXT_DIM);
                    bT.setBackground(Ui.roundBg(modeTim ? Ui.PRIMARY : 0xFFE8EAF2, 10,
                            ReportActivity.this));
                    bT.setTextColor(modeTim ? 0xFFFFFFFF : Ui.TEXT_DIM);
                }
            };
            bP.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { modeTim = false; ref.run(); muat(); }
            });
            bT.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { modeTim = true; ref.run(); muat(); }
            });
            tg.addView(bP);
            tg.addView(bT);
            w.addView(tg);
            ref.run();
        }

        listWrap = new LinearLayout(this);
        listWrap.setOrientation(LinearLayout.VERTICAL);
        listWrap.setPadding(0, Ui.dp(this, 10), 0, 0);
        w.addView(listWrap);

        // Tombol export
        LinearLayout expRow = new LinearLayout(this);
        expRow.setOrientation(LinearLayout.HORIZONTAL);
        expRow.setPadding(0, Ui.dp(this, 6), 0, 0);
        TextView bCsv = Ui.outlineButton(this, "📊 Excel");
        TextView bPdf = Ui.outlineButton(this, "📄 PDF");
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        elp.rightMargin = Ui.dp(this, 8);
        bCsv.setLayoutParams(elp);
        bPdf.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bCsv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (rows != null) {
                    ExportHelper.exportReportCsv(ReportActivity.this,
                            modeTim ? groupName : "Pribadi", rows, year, month);
                }
            }
        });
        bPdf.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (rows != null) {
                    ExportHelper.exportReportPdf(ReportActivity.this,
                            modeTim ? groupName : "Pribadi", rows, year, month);
                }
            }
        });
        expRow.addView(bCsv);
        expRow.addView(bPdf);
        w.addView(expRow);

        sv.addView(w);
        root.addView(sv);
        setContentView(root);
        muat();
    }

    private TextView navBtn(String t) {
        TextView b = Ui.text(this, t, 26, Ui.PRIMARY, true);
        b.setGravity(Gravity.CENTER);
        int s = Ui.dp(this, 44);
        b.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        b.setBackground(Ui.roundBg(0xFFEDEEFF, 22, this));
        b.setClickable(true);
        return b;
    }

    private TextView toggleBtn(String t) {
        TextView b = Ui.text(this, t, 15, Ui.TEXT_DIM, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12));
        b.setClickable(true);
        return b;
    }

    private void geser(int d) {
        month += d;
        if (month < 0) { month = 11; year--; }
        if (month > 11) { month = 0; year++; }
        muat();
    }

    private void muat() {
        tvPeriode.setText(DateUtil.BULAN[month] + " " + year);
        if (modeTim && fbId != null) {
            // Ambil data absen tim dari Firestore.
            try { if (absenReg != null) absenReg.remove(); } catch (Throwable ignored) {}
            absenReg = FirebaseBridge.listenAbsen(this, fbId,
                    new FirebaseBridge.AbsenListCallback() {
                        public void onData(final java.util.List<Map<String, Object>> list) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    rows = ReportHelper.buildTeamReport(list, null, year, month);
                                    render();
                                }
                            });
                        }
                        public void onErr(String msg) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    listWrap.removeAllViews();
                                    LinearLayout card = Ui.card(ReportActivity.this);
                                    card.addView(Ui.text(ReportActivity.this,
                                            "Gagal memuat data tim.", 14, Ui.RED, false));
                                    listWrap.addView(card);
                                }
                            });
                        }
                    });
        } else {
            rows = ReportHelper.buildLocalReport(this, year, month);
            render();
        }
    }

    private void render() {
        listWrap.removeAllViews();
        if (rows == null || rows.isEmpty()) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "Belum ada data kehadiran periode ini.",
                    14, Ui.TEXT_DIM, false));
            listWrap.addView(c);
            return;
        }
        // Header tabel (scroll horizontal)
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout table = new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);

        String[] heads = {"Nama", "Hadir", "Telat", "Tdk Hdr", "Izin", "Sakit",
                "Cuti", "Jam", "Lembur"};
        int[] widths = {150, 62, 62, 78, 62, 62, 62, 70, 76};
        LinearLayout hr = new LinearLayout(this);
        hr.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < heads.length; i++) {
            TextView h = Ui.text(this, heads[i], 12, 0xFFFFFFFF, true);
            h.setGravity(Gravity.CENTER);
            h.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            h.setLayoutParams(new LinearLayout.LayoutParams(
                    Ui.dp(this, widths[i]), LinearLayout.LayoutParams.WRAP_CONTENT));
            h.setBackground(Ui.roundBg(Ui.PRIMARY, 0, this));
            hr.addView(h);
        }
        table.addView(hr);

        for (ReportHelper.Row r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 2));
            boolean flag = r.tidakHadir > 3;
            String[] vals = {
                    r.nama + (flag ? " ⚠️" : ""),
                    String.valueOf(r.hadir), String.valueOf(r.telat),
                    String.valueOf(r.tidakHadir), String.valueOf(r.izin),
                    String.valueOf(r.sakit), String.valueOf(r.cuti),
                    String.format(Locale.US, "%.1f", r.totalJam),
                    String.format(Locale.US, "%.1f", r.lemburJam)};
            for (int i = 0; i < vals.length; i++) {
                TextView c = Ui.text(this, vals[i], 13,
                        flag && i == 3 ? Ui.RED : Ui.TEXT, i == 0);
                c.setGravity(i == 0 ? Gravity.LEFT : Gravity.CENTER);
                c.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6));
                c.setLayoutParams(new LinearLayout.LayoutParams(
                        Ui.dp(this, widths[i]), LinearLayout.LayoutParams.WRAP_CONTENT));
                if (flag) c.setBackground(Ui.roundBg(0xFFFFF1F1, 6, this));
                row.addView(c);
            }
            table.addView(row);
            // divider
            android.view.View dv = new android.view.View(this);
            dv.setBackgroundColor(Ui.DIVIDER);
            dv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            table.addView(dv);
        }
        hsv.addView(table);
        listWrap.addView(hsv);

        if (rows.size() == 1) {
            ReportHelper.Row r = rows.get(0);
            LinearLayout sum = Ui.card(this);
            sum.addView(Ui.text(this, "Ringkasan", 15, Ui.TEXT, true));
            String s = "Hadir " + r.hadir + " hari • Telat " + r.telat + "x • "
                    + "Tidak hadir " + r.tidakHadir + "x\n"
                    + "Izin " + r.izin + " • Sakit " + r.sakit + " • Cuti " + r.cuti + "\n"
                    + "Total jam kerja: " + String.format(Locale.US, "%.1f", r.totalJam)
                    + " jam • Lembur: " + String.format(Locale.US, "%.1f", r.lemburJam) + " jam";
            TextView st = Ui.text(this, s, 13, Ui.TEXT_DIM, false);
            st.setPadding(0, Ui.dp(this, 6), 0, 0);
            sum.addView(st);
            listWrap.addView(sum);
        }
    }
}
