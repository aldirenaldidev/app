package com.flexurio.bursaku;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Portofolio: catat transaksi & pantau posisi. */
public class PortfolioActivity extends Activity {

    private MarketDb db;
    private TextView tvTotal, tvPnl;
    private LinearLayout listHoldings, listTx;
    private AutoCompleteTextView etSym;
    private EditText etDate, etLot, etPrice;
    private Spinner spType;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_portfolio);
        BottomNav.setup(this, 4);
        db = new MarketDb(this);
        tvTotal = findViewById(R.id.tvPortTotal);
        tvPnl = findViewById(R.id.tvPortPnl);
        listHoldings = findViewById(R.id.listHoldings);
        listTx = findViewById(R.id.listTx);
        etSym = findViewById(R.id.etTxSym);
        Tickers.attach(this, etSym);
        etDate = findViewById(R.id.etTxDate);
        etLot = findViewById(R.id.etTxLot);
        etPrice = findViewById(R.id.etTxPrice);
        spType = findViewById(R.id.spTxType);

        String[] opts = {"Beli", "Jual"};
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opts);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(ad);

        findViewById(R.id.btnTxAdd).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { addTx(); }
        });
        findViewById(R.id.btnExportPort).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { exportCsv(); }
        });

        refresh();
    }

    /** Ekspor riwayat transaksi ke CSV di Downloads. */
    private void exportCsv() {
        final List<MarketDb.Tx> txs = db.txAll();
        if (txs.isEmpty()) {
            Toast.makeText(this, "Belum ada transaksi", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Menyiapkan file CSV…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<String> lines = new ArrayList<>();
                    for (MarketDb.Tx t : txs) {
                        String tipe = "BUY".equals(t.type) ? "BELI" : "JUAL";
                        double nilai = t.lot * 100 * t.price;
                        lines.add(ExportCsv.csvRow(
                                t.date,
                                t.symbol.replace(".JK", ""),
                                tipe,
                                String.format(Locale.GERMANY, "%,.0f", t.lot),
                                String.format(Locale.GERMANY, "%,.0f", t.price),
                                String.format(Locale.GERMANY, "%,.0f", nilai)));
                    }
                    final String fn = ExportCsv.write(
                            PortfolioActivity.this,
                            ExportCsv.portfolioFileName(),
                            ExportCsv.csvRow("tanggal", "kode", "tipe",
                                    "lot", "harga", "nilai"),
                            lines);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(PortfolioActivity.this,
                                    "Tersimpan di Downloads: " + fn,
                                    Toast.LENGTH_LONG).show();
                            Notify.simple(PortfolioActivity.this, "Ekspor Portofolio",
                                    "File " + fn + " tersimpan di Downloads.");
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(PortfolioActivity.this,
                                    "Gagal mengekspor: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void addTx() {
        String code = etSym.getText().toString().trim().toUpperCase()
                .replace(".JK", "").replaceAll("\\s+", "");
        String date = etDate.getText().toString().trim();
        double lot, price;
        try {
            lot = Double.parseDouble(etLot.getText().toString().trim());
            price = Double.parseDouble(etPrice.getText().toString().trim());
        } catch (Exception e) { lot = 0; price = 0; }
        if (code.isEmpty() || lot <= 0 || price <= 0) {
            Toast.makeText(this, "Kode, lot, dan harga harus diisi (> 0)",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Format tanggal: YYYY-MM-DD", Toast.LENGTH_SHORT).show();
            return;
        }
        String symbol = code + ".JK";
        String type = spType.getSelectedItemPosition() == 0 ? "BUY" : "SELL";
        try {
            // validasi jual tidak melebihi milik
            List<PortfolioCalc.Position> cur =
                    PortfolioCalc.aggregate(db.txAll());
            if ("SELL".equals(type)) {
                double have = 0;
                for (PortfolioCalc.Position p : cur)
                    if (p.symbol.equals(symbol)) have = p.shares;
                if (lot * 100 > have + 1e-9) {
                    Toast.makeText(this, "Lot jual melebihi kepemilikan ("
                            + String.format(Locale.GERMANY, "%,.0f", have / 100) + " lot)",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
            db.txAdd(symbol, date, lot, price, type);
            etSym.setText(""); etLot.setText(""); etPrice.setText("");
            refresh();
            Toast.makeText(this, "Transaksi disimpan", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void refresh() {
        final List<MarketDb.Tx> txs = db.txAll();
        try {
            final List<PortfolioCalc.Position> pos = PortfolioCalc.aggregate(txs);
            renderTxHistory(txs);
            if (pos.isEmpty()) {
                tvTotal.setText("Rp —");
                tvPnl.setText("—");
                listHoldings.removeAllViews();
                return;
            }
            // ambil harga kini tiap emiten
            new Thread(new Runnable() {
                public void run() {
                    final Map<String, Double> prices = new HashMap<>();
                    for (PortfolioCalc.Position p : pos) {
                        try {
                            prices.put(p.symbol,
                                    YahooClient.fetchChart(p.symbol, "5d").meta.price);
                        } catch (Exception e) { /* lewati */ }
                    }
                    PortfolioCalc.applyPrices(pos, prices);
                    runOnUiThread(new Runnable() {
                        public void run() { renderHoldings(pos); }
                    });
                }
            }).start();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderHoldings(List<PortfolioCalc.Position> pos) {
        listHoldings.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        double[] tot = PortfolioCalc.totals(pos);
        tvTotal.setText(Fmt.rpShort(tot[0]));
        String pnlTxt = Fmt.rpShort(tot[1]) + "\n" + Fmt.pct(tot[2]);
        tvPnl.setText(pnlTxt);
        boolean up = tot[1] >= 0;
        tvPnl.setTextColor(up ? 0xFF16C784 : 0xFFEA3943);
        tvPnl.setBackgroundResource(up ? R.drawable.pill_up : R.drawable.pill_down);

        for (final PortfolioCalc.Position p : pos) {
            View v = inf.inflate(R.layout.item_port, listHoldings, false);
            // Logo emiten (atau badge inisial bila tidak ada)
            ImageView ivLogo = v.findViewById(R.id.ivPLogo);
            TextView tvInit = v.findViewById(R.id.tvPLogoInit);
            int logoRes = LogoUtil.resId(p.symbol);
            if (logoRes != 0) {
                ivLogo.setImageResource(logoRes);
                ivLogo.setVisibility(View.VISIBLE);
                tvInit.setVisibility(View.GONE);
            } else {
                ivLogo.setVisibility(View.GONE);
                tvInit.setVisibility(View.VISIBLE);
                tvInit.setText(LogoUtil.initials(p.symbol));
            }
            ((TextView) v.findViewById(R.id.tvPSym)).setText(p.symbol.replace(".JK", ""));
            ((TextView) v.findViewById(R.id.tvPDetail)).setText(
                    String.format(Locale.GERMANY, "%,.0f lot • Avg %s",
                            p.lots(), Fmt.rp(p.avgPrice)));
            ((TextView) v.findViewById(R.id.tvPValue)).setText(
                    "Nilai pasar " + Fmt.rpShort(p.marketValue)
                    + " @ " + Fmt.rp(p.curPrice));
            TextView pnl = v.findViewById(R.id.tvPPnl);
            pnl.setText(Fmt.rpShort(p.pnl) + "\n" + Fmt.pct(p.pnlPct));
            boolean pup = p.pnl >= 0;
            pnl.setTextColor(pup ? 0xFF16C784 : 0xFFEA3943);
            pnl.setBackgroundResource(pup ? R.drawable.pill_up : R.drawable.pill_down);
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View vv) {
                    Intent i = new Intent(PortfolioActivity.this, DetailActivity.class);
                    i.putExtra("symbol", p.symbol);
                    startActivity(i);
                }
            });
            listHoldings.addView(v);
        }
    }

    private void renderTxHistory(List<MarketDb.Tx> txs) {
        listTx.removeAllViews();
        for (final MarketDb.Tx t : txs) {
            TextView tv = new TextView(this);
            String type = "BUY".equals(t.type) ? "Beli" : "Jual";
            tv.setText(String.format(Locale.GERMANY, "%s • %s • %s • %,.0f lot @ %s",
                    t.date, type, t.symbol.replace(".JK", ""), t.lot, Fmt.rp(t.price)));
            tv.setTextColor(0xFF8B93A7);
            tv.setTextSize(12);
            tv.setPadding(4, 12, 4, 12);
            tv.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    db.txDelete(t.id);
                    refresh();
                    Toast.makeText(PortfolioActivity.this,
                            "Transaksi dihapus", Toast.LENGTH_SHORT).show();
                }
            });
            listTx.addView(tv);
        }
    }
}
