package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Heatmap pasar: grid 45 emiten LQ45, warna sel = % perubahan
 * (merah vivid → netral → hijau vivid). Data dari cache ranking
 * (tidak fetch ulang bila cache < 15 menit). Tap sel → detail emiten.
 */
public class HeatmapActivity extends Activity {

    private static final long STALE_MS = 15 * 60_000;

    private GridView grid;
    private TextView tvInfo;
    private MarketDb db;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_heatmap);
        BottomNav.setup(this, 6);
        db = new MarketDb(this);
        grid = findViewById(R.id.gridHeat);
        tvInfo = findViewById(R.id.tvHeatInfo);

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                HeatCell c = (HeatCell) grid.getAdapter().getItem(pos);
                Intent i = new Intent(HeatmapActivity.this, DetailActivity.class);
                i.putExtra("symbol", c.symbol);
                startActivity(i);
            }
        });
        load();
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        List<MarketDb.RankRow> rows = db.rankAll();
        long upd = db.rankUpdated();
        if (rows.isEmpty()) {
            tvInfo.setText("Belum ada data — buka layar Ranking untuk memindai otomatis.");
            grid.setAdapter(new HeatAdapter(this, new ArrayList<HeatCell>()));
            return;
        }
        List<HeatCell> cells = new ArrayList<>();
        for (MarketDb.RankRow r : rows) {
            HeatCell c = new HeatCell();
            c.symbol = r.symbol;
            c.code = r.symbol.replace(".JK", "");
            c.chg = r.chg;
            cells.add(c);
        }
        // Urutkan: gainers dulu agar hijau di kiri atas.
        Collections.sort(cells, new Comparator<HeatCell>() {
            public int compare(HeatCell a, HeatCell b) {
                return Double.compare(b.chg, a.chg);
            }
        });
        grid.setAdapter(new HeatAdapter(this, cells));
        String age = "Diperbarui " + Fmt.fmtHour(upd) + " WIB • otomatis";
        if (Prefs.isStale(upd, System.currentTimeMillis(), STALE_MS))
            age += " (segera diperbarui di latar)";
        tvInfo.setText(age);
    }

    static class HeatCell {
        String symbol, code;
        double chg;
    }

    /** Adapter grid: sel berwarna sesuai % perubahan. */
    static class HeatAdapter extends BaseAdapter {
        private final Context ctx;
        private final List<HeatCell> cells;
        private final float d;

        HeatAdapter(Context ctx, List<HeatCell> cells) {
            this.ctx = ctx;
            this.cells = cells;
            this.d = ctx.getResources().getDisplayMetrics().density;
        }

        public int getCount() { return cells.size(); }
        public Object getItem(int p) { return cells.get(p); }
        public long getItemId(int p) { return p; }

        public View getView(int pos, View cv, ViewGroup parent) {
            LinearLayout root;
            TextView tvCode, tvChg;
            if (cv == null) {
                root = new LinearLayout(ctx);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setGravity(Gravity.CENTER);
                int pad = (int) (10 * d + 0.5f);
                root.setPadding(pad, pad, pad, pad);
                tvCode = new TextView(ctx);
                tvCode.setTextSize(15);
                tvCode.setTypeface(null, android.graphics.Typeface.BOLD);
                tvChg = new TextView(ctx);
                tvChg.setTextSize(13);
                root.addView(tvCode);
                root.addView(tvChg);
                root.setTag(new TextView[]{tvCode, tvChg});
            } else {
                root = (LinearLayout) cv;
                TextView[] t = (TextView[]) root.getTag();
                tvCode = t[0]; tvChg = t[1];
            }
            HeatCell c = cells.get(pos);
            tvCode.setText(c.code);
            tvCode.setTextColor(0xFFFFFFFF);
            tvChg.setText(Fmt.pct(c.chg));
            tvChg.setTextColor(0xFFFFFFFF);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(HeatColor.colorFor(c.chg));
            bg.setCornerRadius(12 * d);
            root.setBackground(bg);
            return root;
        }
    }
}
