package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Berita pasar dari RSS. Sepenuhnya otomatis: tampilkan daftar terakhir,
 * muat ulang di latar bila basi (>15 menit). Tidak ada tombol manual.
 */
public class NewsActivity extends Activity {

    private static final long STALE_MS = 30 * 60_000; // TTL berita: 30 menit

    private ListView lv;
    private TextView tvStatus;
    private final List<NewsItem> items = new ArrayList<>();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_news);
        BottomNav.setup(this, -1);
        AdsHelper.init(this);
        AdsHelper.banner(this, R.id.adBanner);
        lv = findViewById(R.id.lvNews);
        tvStatus = findViewById(R.id.tvNewsStatus);
        lv.setAdapter(new NewsAdapter(this, items));

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                NewsItem n = items.get(pos);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(n.link)));
            }
        });

        autoLoad();
    }

    @Override protected void onResume() {
        super.onResume();
        autoLoad();
    }

    private void autoLoad() {
        long last = Prefs.lastRefresh(this, Prefs.TS_NEWS);
        if (!items.isEmpty()) {
            tvStatus.setText(items.size() + " berita • diperbarui "
                    + Fmt.fmtHour(last) + " WIB • otomatis");
        }
        if (items.isEmpty() || Prefs.isStale(last, System.currentTimeMillis(), STALE_MS)) {
            load();
        }
    }

    private void load() {
        if (items.isEmpty()) tvStatus.setText("Memuat berita otomatis…");
        else tvStatus.setText(tvStatus.getText() + " • memperbarui…");
        new Thread(new Runnable() {
            public void run() {
                final List<NewsItem> got = NewsClient.fetchAll();
                final long now = System.currentTimeMillis();
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (!got.isEmpty()) {
                            items.clear();
                            items.addAll(got);
                            ((NewsAdapter) lv.getAdapter()).notifyDataSetChanged();
                            Prefs.markRefreshed(NewsActivity.this, Prefs.TS_NEWS);
                            tvStatus.setText(got.size() + " berita • diperbarui "
                                    + Fmt.fmtHour(now) + " WIB • otomatis");
                        } else if (items.isEmpty()) {
                            tvStatus.setText("Gagal memuat berita — periksa koneksi.");
                        }
                    }
                });
            }
        }).start();
    }
}
