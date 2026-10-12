package com.flexurio.bursaku;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Menu "Analisa": search emiten (autocomplete) -> analisa teknikal
 * rule-based (SignalEngine) -> verdict BELI/TAHAN/JUAL + alasan
 * Bahasa Indonesia + rincian per indikator.
 */
public class AnalisaActivity extends Activity {

    private AutoCompleteTextView etSym;
    private TextView tvStatus, tvVerdict, tvScore;
    private LinearLayout listAlasan, listRincian;
    private View cardHasil;
    private MarketDb db;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_analisa);
        db = new MarketDb(this);
        BottomNav.setup(this, 5);

        etSym = findViewById(R.id.etAnalisaSym);
        Tickers.attach(this, etSym);
        tvStatus = findViewById(R.id.tvAnalisaStatus);
        tvVerdict = findViewById(R.id.tvVerdict);
        tvScore = findViewById(R.id.tvScore);
        listAlasan = findViewById(R.id.listAlasan);
        listRincian = findViewById(R.id.listRincian);
        cardHasil = findViewById(R.id.cardHasil);

        findViewById(R.id.btnAnalisaGo).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { runAnalisa(); }
        });
    }

    private void runAnalisa() {
        final String code = etSym.getText().toString().trim().toUpperCase()
                .replace(".JK", "").replaceAll("\\s+", "");
        if (code.isEmpty()) {
            Toast.makeText(this, "Ketik kode emiten dulu", Toast.LENGTH_SHORT).show();
            return;
        }
        final String symbol = code + ".JK";
        cardHasil.setVisibility(View.GONE);
        tvStatus.setText("Menganalisa " + code + "…");
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<Candle> candles = loadCandles(symbol);
                    if (candles.size() < 30)
                        throw new Exception("data historis kurang");
                    final SignalEngine.Result r = SignalEngine.analyze(candles);
                    final String name = Lq45.nameOf(code);
                    runOnUiThread(new Runnable() {
                        public void run() { bindResult(code, name, r); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvStatus.setText("Gagal menganalisa " + code
                                    + " — periksa koneksi / kode emiten.");
                        }
                    });
                }
            }
        }).start();
    }

    /** Candles dari cache CHART (TTL 1 hari) bila segar, else fetch & simpan. */
    private List<Candle> loadCandles(String symbol) throws Exception {
        if (!db.cacheStale(symbol, CacheTtl.CHART)) {
            List<Candle> cached = CacheSer.candlesFromJson(
                    db.textGet(symbol, CacheTtl.CHART));
            if (cached.size() >= 30) return cached;
        }
        YahooClient.ChartData d = YahooClient.fetchChart(symbol, "1y");
        if (d.candles.size() >= 30)
            db.textSet(symbol, CacheTtl.CHART, CacheSer.candlesToJson(d.candles));
        return d.candles;
    }

    private void bindResult(String code, String name, SignalEngine.Result r) {
        cardHasil.setVisibility(View.VISIBLE);
        String verdict = r.verdict();
        tvVerdict.setText(verdict);
        int color;
        if ("BELI".equals(verdict)) color = 0xFF16C784;
        else if ("JUAL".equals(verdict)) color = 0xFFEA3943;
        else color = 0xFFFFB020;
        tvVerdict.setTextColor(color);
        String sign = r.total > 0 ? "+" : "";
        tvScore.setText("Skor sinyal: " + sign + r.total
                + " dari " + r.signals.size() + " indikator");
        tvStatus.setText(code + (name.isEmpty() ? "" : " — " + name)
                + " • " + r.signals.size() + " indikator dievaluasi");

        listAlasan.removeAllViews();
        for (SignalEngine.Signal s : r.sorted()) {
            listAlasan.addView(alasanRow(s));
        }
        listRincian.removeAllViews();
        for (SignalEngine.Signal s : r.signals) {
            listRincian.addView(rincianRow(s));
        }
    }

    private View alasanRow(SignalEngine.Signal s) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(7), 0, dp(7));
        TextView bullet = new TextView(this);
        int color;
        if (s.score > 0) { bullet.setText("✓ "); color = 0xFF16C784; }
        else if (s.score < 0) { bullet.setText("✗ "); color = 0xFFEA3943; }
        else { bullet.setText("• "); color = 0xFF8B93A7; }
        bullet.setTextSize(15);
        bullet.setTextColor(color);
        bullet.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView tv = new TextView(this);
        tv.setText(s.reason);
        tv.setTextSize(14);
        tv.setTextColor(0xFFFFFFFF);
        tv.setLineSpacing(dp(3), 1f);
        row.addView(bullet);
        row.addView(tv);
        return row;
    }

    private View rincianRow(SignalEngine.Signal s) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        TextView name = new TextView(this);
        name.setText(s.indicator);
        name.setTextSize(14);
        name.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        name.setLayoutParams(lp);
        TextView val = new TextView(this);
        val.setText(s.value);
        val.setTextSize(13);
        val.setTextColor(0xFF8B93A7);
        val.setPadding(dp(8), 0, dp(8), 0);
        TextView badge = new TextView(this);
        badge.setText(s.score > 0 ? "+1" : s.score < 0 ? "−1" : "0");
        badge.setTextSize(12);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setTextColor(0xFFFFFFFF);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setMinWidth(dp(40));
        badge.setPadding(dp(8), dp(4), dp(8), dp(4));
        badge.setBackgroundResource(s.score > 0 ? R.drawable.pill_up
                : s.score < 0 ? R.drawable.pill_down : R.drawable.pill_neutral);
        row.addView(name);
        row.addView(val);
        row.addView(badge);
        return row;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
