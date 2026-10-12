package com.flexurio.bursaku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/** Adapter daftar alert harga. */
public class AlertAdapter extends BaseAdapter {
    public interface Listener {
        void onToggle(MarketDb.Alert a);
        void onDelete(MarketDb.Alert a);
    }

    private final List<MarketDb.Alert> rows;
    private final LayoutInflater inf;
    private final Listener listener;

    public AlertAdapter(Context c, List<MarketDb.Alert> rows, Listener listener) {
        this.rows = rows;
        this.inf = LayoutInflater.from(c);
        this.listener = listener;
    }

    public int getCount() { return rows.size(); }
    public Object getItem(int p) { return rows.get(p); }
    public long getItemId(int p) { return rows.get(p).id; }

    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) cv = inf.inflate(R.layout.item_alert, parent, false);
        final MarketDb.Alert a = rows.get(pos);
        ((TextView) cv.findViewById(R.id.tvASym)).setText(a.symbol.replace(".JK", ""));
        String dir = "above".equals(a.dir) ? "Naik menyentuh ≥ " : "Turun menyentuh ≤ ";
        ((TextView) cv.findViewById(R.id.tvACond)).setText(dir + Fmt.rp(a.target));
        TextView st = cv.findViewById(R.id.tvAState);
        st.setText(a.active ? "Aktif" : "Nonaktif");
        st.setTextColor(a.active ? 0xFF16C784 : 0xFF8B93A7);
        Button tg = cv.findViewById(R.id.btnAToggle);
        tg.setText(a.active ? "Nonaktifkan" : "Aktifkan");
        tg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onToggle(a); }
        });
        cv.findViewById(R.id.btnADel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { listener.onDelete(a); }
        });
        return cv;
    }
}
