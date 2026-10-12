package com.flexurio.bursaku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** Adapter tab Top Dividen: kode, nama, harga, pill yield vivid, dividen tahunan. */
public class DivAdapter extends BaseAdapter {
    private final List<MarketDb.DivRow> rows;
    private final LayoutInflater inf;

    public DivAdapter(Context c, List<MarketDb.DivRow> rows) {
        this.rows = rows;
        this.inf = LayoutInflater.from(c);
    }

    public int getCount() { return rows.size(); }
    public Object getItem(int p) { return rows.get(p); }
    public long getItemId(int p) { return p; }

    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) cv = inf.inflate(R.layout.item_rank, parent, false);
        MarketDb.DivRow r = rows.get(pos);
        ((TextView) cv.findViewById(R.id.tvRankNo)).setText(String.valueOf(pos + 1));
        ((TextView) cv.findViewById(R.id.tvRankSym)).setText(r.symbol.replace(".JK", ""));
        TextView badge = cv.findViewById(R.id.tvRankSharia);
        badge.setVisibility(Sharia.isSharia(cv.getContext(), r.symbol)
                ? View.VISIBLE : View.GONE);
        String nm = (r.name == null || r.name.isEmpty())
                ? r.symbol.replace(".JK", "") : r.name;
        ((TextView) cv.findViewById(R.id.tvRankName)).setText(nm);
        String sub = Fmt.rp(r.price);
        if (!Double.isNaN(r.annual) && r.annual > 0)
            sub += " • Rp " + String.format(Locale.GERMANY, "%,.0f", r.annual) + "/thn";
        ((TextView) cv.findViewById(R.id.tvRankPrice)).setText(sub);
        TextView pill = cv.findViewById(R.id.tvRankChg);
        pill.setText(String.format(Locale.GERMANY, "%,.2f%%", r.yieldPct));
        pill.setTextColor(0xFF16C784);
        pill.setBackgroundResource(R.drawable.pill_up);
        return cv;
    }
}
