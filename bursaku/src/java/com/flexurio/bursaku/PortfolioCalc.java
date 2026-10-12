package com.flexurio.bursaku;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hitung posisi portofolio dari daftar transaksi (murni Java, bisa di-unit-test).
 * 1 lot = 100 lembar. Jual memakai metode rata-rata (average cost).
 */
public class PortfolioCalc {

    public static class Position {
        public String symbol;
        public double shares;      // lembar
        public double avgPrice;    // harga rata-rata /lembar
        public double cost;        // total biaya (shares * avgPrice)
        public double curPrice = Double.NaN;
        public double marketValue = Double.NaN;
        public double pnl = Double.NaN;
        public double pnlPct = Double.NaN;

        public double lots() { return shares / 100.0; }
    }

    /** Agregasi transaksi menjadi posisi per emiten. Melempar bila jual melebihi milik. */
    public static List<Position> aggregate(List<MarketDb.Tx> txs) throws Exception {
        Map<String, Position> map = new LinkedHashMap<>();
        for (MarketDb.Tx t : txs) {
            Position p = map.get(t.symbol);
            if (p == null) { p = new Position(); p.symbol = t.symbol; map.put(t.symbol, p); }
            double sh = t.lot * 100.0;
            if ("BUY".equals(t.type)) {
                p.cost += sh * t.price;
                p.shares += sh;
            } else { // SELL
                if (sh > p.shares + 1e-9)
                    throw new Exception("Jual " + t.symbol + " melebihi lot dimiliki");
                double avg = p.shares > 0 ? p.cost / p.shares : 0;
                p.cost -= sh * avg;
                p.shares -= sh;
                if (p.shares < 1e-9) { p.shares = 0; p.cost = 0; }
            }
        }
        List<Position> out = new ArrayList<>();
        for (Position p : map.values()) {
            if (p.shares > 0) {
                p.avgPrice = p.cost / p.shares;
                out.add(p);
            }
        }
        return out;
    }

    /** Lengkapi posisi dengan harga kini → nilai pasar & P/L. */
    public static void applyPrices(List<Position> positions, Map<String, Double> prices) {
        for (Position p : positions) {
            Double c = prices.get(p.symbol);
            if (c == null || Double.isNaN(c)) continue;
            p.curPrice = c;
            p.marketValue = p.shares * c;
            p.pnl = p.marketValue - p.cost;
            p.pnlPct = p.cost > 0 ? p.pnl / p.cost * 100.0 : Double.NaN;
        }
    }

    /** Total nilai pasar & total P/L seluruh posisi. */
    public static double[] totals(List<Position> positions) {
        double mv = 0, cost = 0;
        for (Position p : positions) {
            if (!Double.isNaN(p.marketValue)) mv += p.marketValue;
            cost += p.cost;
        }
        return new double[]{mv, mv - cost, cost > 0 ? (mv - cost) / cost * 100.0 : Double.NaN};
    }
}
