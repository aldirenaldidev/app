package com.flexurio.bursaku;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/** Adapter daftar berita. */
public class NewsAdapter extends BaseAdapter {
    private final List<NewsItem> rows;
    private final LayoutInflater inf;

    public NewsAdapter(Context c, List<NewsItem> rows) {
        this.rows = rows;
        this.inf = LayoutInflater.from(c);
    }

    public int getCount() { return rows.size(); }
    public Object getItem(int p) { return rows.get(p); }
    public long getItemId(int p) { return p; }

    public View getView(int pos, View cv, ViewGroup parent) {
        if (cv == null) cv = inf.inflate(R.layout.item_news, parent, false);
        NewsItem n = rows.get(pos);
        ((TextView) cv.findViewById(R.id.tvNTitle)).setText(n.title);
        String meta = n.source;
        String t = NewsClient.fmtTime(n.pubTime);
        if (!t.isEmpty()) meta += " • " + t;
        ((TextView) cv.findViewById(R.id.tvNMeta)).setText(meta);
        return cv;
    }
}
