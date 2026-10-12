package com.flexurio.bursaku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/** Adapter watchlist (dengan tombol hapus). */
public class WatchAdapter extends BaseAdapter {
    public interface OnDelete { void onDelete(String symbol); }

    private final List<WatchRow> rows;
    private final LayoutInflater inf;
    private final OnDelete onDelete;

    public static class WatchRow {
        public String symbol, name;
        public double price = Double.NaN, chg = Double.NaN;
    }

    public WatchAdapter(Context c, List<WatchRow> rows, OnDelete onDelete) {
        this.rows = rows;
        this.inf = LayoutInflater.from(c);
        this.onDelete = onDelete;
    }

    public int getCount() { return rows.size(); }
    public Object getItem(int p) { return rows.get(p); }
    public long getItemId(int p) { return p; }

    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) cv = inf.inflate(R.layout.item_watch, parent, false);
        final WatchRow r = rows.get(pos);
        // Logo emiten (atau badge inisial bila tidak ada)
        ImageView ivLogo = cv.findViewById(R.id.ivWLogo);
        TextView tvInit = cv.findViewById(R.id.tvWLogoInit);
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
        ((TextView) cv.findViewById(R.id.tvWSym)).setText(r.symbol.replace(".JK", ""));
        TextView badge = cv.findViewById(R.id.tvWSharia);
        badge.setVisibility(Sharia.isSharia(cv.getContext(), r.symbol)
                ? View.VISIBLE : View.GONE);
        String nm = r.name.isEmpty() ? Lq45.nameOf(r.symbol) : r.name;
        ((TextView) cv.findViewById(R.id.tvWName)).setText(nm.isEmpty() ? "—" : nm);
        TextView tvPrice = cv.findViewById(R.id.tvWPrice);
        TextView chg = cv.findViewById(R.id.tvWChg);
        if (Double.isNaN(r.price)) {
            // Harga belum tiba (ticker baru ditambahkan): status abu, bukan "—".
            tvPrice.setText("Memuat harga…");
            tvPrice.setTextColor(0xFF8B93A7);
            chg.setText("");
            chg.setBackgroundResource(0);
        } else {
            tvPrice.setText(Fmt.rp(r.price));
            tvPrice.setTextColor(0xFFFFFFFF);
            chg.setText(Fmt.pct(r.chg));
            boolean up = r.chg >= 0;
            chg.setTextColor(up ? 0xFF16C784 : 0xFFEA3943);
            chg.setBackgroundResource(up ? R.drawable.pill_up : R.drawable.pill_down);
        }
        Button del = cv.findViewById(R.id.btnWDel);
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { onDelete.onDelete(r.symbol); }
        });
        return cv;
    }
}
