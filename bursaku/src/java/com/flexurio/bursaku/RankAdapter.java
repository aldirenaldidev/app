package com.flexurio.bursaku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/** Adapter daftar ranking. */
public class RankAdapter extends BaseAdapter {
    private final List<MarketDb.RankRow> rows;
    private final LayoutInflater inf;

    public RankAdapter(Context c, List<MarketDb.RankRow> rows) {
        this.rows = rows;
        this.inf = LayoutInflater.from(c);
    }

    public int getCount() { return rows.size(); }
    public Object getItem(int p) { return rows.get(p); }
    public long getItemId(int p) { return p; }

    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) cv = inf.inflate(R.layout.item_rank, parent, false);
        MarketDb.RankRow r = rows.get(pos);
        ((TextView) cv.findViewById(R.id.tvRankNo)).setText(String.valueOf(pos + 1));
        // Logo emiten (atau badge inisial bila tidak ada)
        ImageView ivLogo = cv.findViewById(R.id.ivRankLogo);
        TextView tvInit = cv.findViewById(R.id.tvRankLogoInit);
        int logoRes = LogoUtil.resId(r.symbol);
        if (logoRes != 0) {
            ivLogo.setImageResource(logoRes);
            ivLogo.setVisibility(View.VISIBLE);
            tvInit.setVisibility(View.GONE);
        } else {
            ivLogo.setVisibility(View.GONE);
            tvInit.setVisibility(View.VISIBLE);
            tvInit.setText(LogoUtil.initials(r.symbol));
        }
        ((TextView) cv.findViewById(R.id.tvRankSym)).setText(r.symbol.replace(".JK", ""));
        TextView badge = cv.findViewById(R.id.tvRankSharia);
        badge.setVisibility(Sharia.isSharia(cv.getContext(), r.symbol)
                ? View.VISIBLE : View.GONE);
        String nm = r.name.isEmpty() ? Lq45.nameOf(r.symbol) : r.name;
        ((TextView) cv.findViewById(R.id.tvRankName)).setText(nm);
        ((TextView) cv.findViewById(R.id.tvRankPrice)).setText(Fmt.rp(r.price));
        TextView chg = cv.findViewById(R.id.tvRankChg);
        chg.setText(Fmt.pct(r.chg));
        boolean up = r.chg >= 0;
        chg.setTextColor(up ? 0xFF16C784 : 0xFFEA3943);
        chg.setBackgroundResource(up ? R.drawable.pill_up : R.drawable.pill_down);
        return cv;
    }
}
