package com.flexurio.bursaku;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.List;

/** Kelola alert harga. */
public class AlertActivity extends Activity {

    private MarketDb db;
    private ListView lv;
    private AutoCompleteTextView etSym;
    private EditText etTarget;
    private TextView tvPrice;
    /** Penanda agar respons fetch lama tak menimpa hint yang lebih baru. */
    private int hintSeq = 0;
    private Spinner spDir;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_alert);
        BottomNav.setup(this, 3);
        db = new MarketDb(this);
        lv = findViewById(R.id.lvAlert);
        etSym = findViewById(R.id.etAlertSym);
        tvPrice = findViewById(R.id.tvAlertPrice);
        Tickers.attach(this, etSym, new Tickers.OnTickerSelected() {
            public void onSelected(TickerFilter.Ticker t) {
                fetchPriceHint(t.code);
            }
        });
        // Hint basi bila user mengedit manual -> kembalikan ke "—".
        etSym.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                hintSeq++;
                tvPrice.setText("Harga saat ini: —");
            }
        });
        etTarget = findViewById(R.id.etAlertTarget);
        spDir = findViewById(R.id.spAlertDir);

        String[] opts = {"Naik menyentuh ≥ target", "Turun menyentuh ≤ target"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDir.setAdapter(ad);

        String pre = getIntent().getStringExtra("symbol");
        if (pre != null) etSym.setText(pre.replace(".JK", ""));

        findViewById(R.id.btnAlertAdd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addAlert(); }
        });

        refresh();
    }

    /** Tampilkan harga saat ini di form begitu ticker dipilih dari dropdown. */
    private void fetchPriceHint(String code) {
        final int seq = ++hintSeq;
        final String symbol = code + ".JK";
        tvPrice.setText("Harga saat ini: memuat…");
        new Thread(new Runnable() {
            public void run() {
                double p = Double.NaN, c = Double.NaN;
                try {
                    YahooClient.ChartData d = YahooClient.fetchChart(symbol, "5d");
                    p = d.meta.price;
                    c = d.meta.changePct;
                } catch (Exception e) { /* hint tetap "—" */ }
                final double fp = p, fc = c;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (seq != hintSeq) return; // sudah basi: abaikan
                        if (!Double.isNaN(fp)) {
                            tvPrice.setText("Harga saat ini: Rp " + Fmt.rp(fp)
                                    + " (" + Fmt.pct(fc) + ")");
                        } else {
                            tvPrice.setText("Harga saat ini: —");
                        }
                    }
                });
            }
        }).start();
    }

    private void addAlert() {
        final String code = etSym.getText().toString().trim().toUpperCase()
                .replace(".JK", "").replaceAll("\\s+", "");
        double target;
        try {
            target = Double.parseDouble(etTarget.getText().toString().trim());
        } catch (Exception e) { target = 0; }
        if (code.isEmpty() || target <= 0) {
            Toast.makeText(this, "Isi kode emiten dan target harga (> 0)",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        final String symbol = code + ".JK";
        final double ftarget = target;
        final String dir = spDir.getSelectedItemPosition() == 0 ? "above" : "below";
        Toast.makeText(this, "Memvalidasi " + code + "…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final boolean ok = YahooClient.validateSymbol(symbol);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (ok) {
                            db.alertAdd(symbol, ftarget, dir);
                            etSym.setText("");
                            etTarget.setText("");
                            refresh();
                            Toast.makeText(AlertActivity.this,
                                    "Alert dipasang untuk " + symbol.replace(".JK", ""),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AlertActivity.this,
                                    "Kode tidak valid", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void refresh() {
        final List<MarketDb.Alert> rows = db.alertsAll();
        lv.setAdapter(new AlertAdapter(this, rows, new AlertAdapter.Listener() {
            public void onToggle(MarketDb.Alert a) {
                db.alertSetActive(a.id, !a.active);
                refresh();
            }
            public void onDelete(MarketDb.Alert a) {
                db.alertDelete(a.id);
                refresh();
            }
        }));
    }
}
