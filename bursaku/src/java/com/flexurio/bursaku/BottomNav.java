package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Bottom navigation modern bersama untuk layar utama.
 * Item aktif: pill gradient biru elektrik + ikon/label putih.
 * Item nonaktif: ikon/label abu #8B93A7.
 */
public class BottomNav {

    /** Indeks layar: 0 Pasar, 1 Ranking, 2 Watchlist, 3 Alert, 4 Portofolio,
     *  5 Analisa, 6 Heatmap, -1 tanpa aktif. */
    public static void setup(final Activity act, int active) {
        final int[] roots = {
                R.id.navPasar, R.id.navRanking, R.id.navWatchlist,
                R.id.navAlert, R.id.navPortfolio, R.id.navAnalisa,
                R.id.navHeatmap
        };
        final int[] icons = {
                R.id.navPasarIcon, R.id.navRankingIcon, R.id.navWatchlistIcon,
                R.id.navAlertIcon, R.id.navPortfolioIcon, R.id.navAnalisaIcon,
                R.id.navHeatmapIcon
        };
        final int[] labels = {
                R.id.navPasarLabel, R.id.navRankingLabel, R.id.navWatchlistLabel,
                R.id.navAlertLabel, R.id.navPortfolioLabel, R.id.navAnalisaLabel,
                R.id.navHeatmapLabel
        };
        final Class<?>[] targets = {
                MainActivity.class, RankingActivity.class, WatchlistActivity.class,
                AlertActivity.class, PortfolioActivity.class, AnalisaActivity.class,
                HeatmapActivity.class
        };
        for (int i = 0; i < roots.length; i++) {
            View root = act.findViewById(roots[i]);
            if (root == null) continue;
            final int idx = i;
            boolean on = (i == active);
            root.setBackgroundResource(on ? R.drawable.bg_nav_active : 0);
            int color = on ? 0xFFFFFFFF : 0xFF8B93A7;
            View iv = act.findViewById(icons[i]);
            View lv = act.findViewById(labels[i]);
            if (iv instanceof ImageView) {
                ((ImageView) iv).setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
            if (lv instanceof TextView) {
                ((TextView) lv).setTextColor(color);
                ((TextView) lv).setTypeface(null,
                        on ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            }
            if (!on) {
                root.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent it = new Intent(act, targets[idx]);
                        it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        act.startActivity(it);
                    }
                });
            } else {
                root.setOnClickListener(null);
                root.setClickable(false);
            }
        }
    }
}
