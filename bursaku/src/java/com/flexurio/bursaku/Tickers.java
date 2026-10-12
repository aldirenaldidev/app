package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Daftar ticker IDX (assets/tickers.json) untuk autocomplete
 * kolom kode emiten. Offline-capable; ketik manual tetap
 * diizinkan dan validasi API tetap berjalan seperti biasa.
 */
public class Tickers {

    private static List<TickerFilter.Ticker> cache;

    /** Muat dari assets/tickers.json (sekali, lalu di-cache). */
    public static synchronized List<TickerFilter.Ticker> load(Context ctx) {
        if (cache != null) return cache;
        String json = "";
        try {
            InputStream is = ctx.getAssets().open("tickers.json");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            json = bos.toString("UTF-8");
        } catch (Exception e) { /* daftar kosong */ }
        cache = TickerFilter.parseJson(json);
        return cache;
    }

    /** Callback opsional saat user memilih ticker dari dropdown. */
    public interface OnTickerSelected {
        void onSelected(TickerFilter.Ticker t);
    }

    /**
     * Pasang autocomplete ke kolom isian kode emiten.
     * Dropdown menampilkan "BBCA — Bank Central Asia Tbk";
     * setelah dipilih, kolom terisi kode saja (tanpa .JK).
     */
    public static void attach(Activity act, AutoCompleteTextView v) {
        attach(act, v, null);
    }

    public static void attach(Activity act, AutoCompleteTextView v,
                              final OnTickerSelected cb) {
        final List<TickerFilter.Ticker> all = load(act);
        final TickerAdapter ad = new TickerAdapter(act, all);
        v.setAdapter(ad);
        v.setThreshold(1);
        v.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View view, int pos, long id) {
                TickerFilter.Ticker t = ad.getFilteredItem(pos);
                if (t != null) {
                    v.setText(t.code);
                    v.setSelection(t.code.length());
                    if (cb != null) cb.onSelected(t);
                }
                v.dismissDropDown();
            }
        });
    }

    /** Adapter dropdown autocomplete. */
    static class TickerAdapter extends ArrayAdapter<TickerFilter.Ticker> {
        private final List<TickerFilter.Ticker> all;
        private List<TickerFilter.Ticker> filtered;
        private final LayoutInflater inf;

        TickerAdapter(Context ctx, List<TickerFilter.Ticker> all) {
            super(ctx, 0, new ArrayList<TickerFilter.Ticker>());
            this.all = all != null ? all : new ArrayList<TickerFilter.Ticker>();
            this.filtered = new ArrayList<TickerFilter.Ticker>(this.all);
            this.inf = LayoutInflater.from(ctx);
        }

        TickerFilter.Ticker getFilteredItem(int pos) {
            return (pos >= 0 && pos < filtered.size()) ? filtered.get(pos) : null;
        }

        @Override public int getCount() { return filtered.size(); }
        @Override public TickerFilter.Ticker getItem(int pos) { return filtered.get(pos); }

        /** Teks di kolom setelah dipilih: hanya kode. */
        @Override public View getView(int pos, View cv, ViewGroup parent) {
            TextView tv = (cv instanceof TextView)
                    ? (TextView) cv : new TextView(getContext());
            tv.setText(filtered.get(pos).code);
            return tv;
        }

        /** Baris dropdown: "BBCA" bold + nama perusahaan abu. */
        @Override public View getDropDownView(int pos, View cv, ViewGroup parent) {
            View v = cv == null ? inf.inflate(R.layout.item_ticker, parent, false) : cv;
            TickerFilter.Ticker t = filtered.get(pos);
            ((TextView) v.findViewById(R.id.tvTickCode)).setText(t.code);
            ((TextView) v.findViewById(R.id.tvTickName)).setText(t.name);
            TextView mk = v.findViewById(R.id.tvTickSharia);
            mk.setVisibility(Sharia.isSharia(getContext(), t.code)
                    ? View.VISIBLE : View.GONE);
            return v;
        }

        @Override public Filter getFilter() {
            return new Filter() {
                @Override protected FilterResults performFiltering(CharSequence c) {
                    List<TickerFilter.Ticker> r =
                            TickerFilter.filter(all, c == null ? "" : c.toString());
                    FilterResults fr = new FilterResults();
                    fr.values = r;
                    fr.count = r.size();
                    return fr;
                }
                @Override @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence c, FilterResults r) {
                    filtered = (List<TickerFilter.Ticker>) r.values;
                    if (filtered == null) filtered = new ArrayList<TickerFilter.Ticker>();
                    if (r.count > 0) notifyDataSetChanged();
                    else notifyDataSetInvalidated();
                }
            };
        }
    }
}
