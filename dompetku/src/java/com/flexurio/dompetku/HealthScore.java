package com.flexurio.dompetku;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Skor Kesehatan Keuangan 0-100 per bulan. 100% offline.
 * Komponen: tabungan 30% | pengendalian 25% | konsistensi 20% | target 15% | darurat 10%.
 * Top-level class agar aman untuk d8.
 */
public final class HealthScore {
    private HealthScore() {}

    public static class Component {
        public String name;
        public int score;
        public int weight;
        public String note;
        public Component(String n, int s, int w, String note) {
            this.name = n; this.score = s; this.weight = w; this.note = note;
        }
    }

    public static class Score {
        public int total;
        public String label;
        public String emoji;
        public String color;
        public List<Component> components = new ArrayList<Component>();
        public int delta; // vs bulan lalu
        public List<String> suggestions = new ArrayList<String>();
        public boolean hasData;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    /** Hitung skor untuk bulan (year, month1). */
    public static Score compute(DbHelper db, int year, int month1) {
        Score sc = new Score();
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = String.format(Locale.US, "%04d-%02d-01", year, month1);
            Calendar c = Calendar.getInstance();
            c.set(year, month1 - 1, 1);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            c.set(Calendar.DAY_OF_MONTH, daysInMonth);
            String to = f.format(c.getTime());

            Calendar now = Calendar.getInstance();
            boolean isCurrent = (now.get(Calendar.YEAR) == year
                    && now.get(Calendar.MONTH) + 1 == month1);
            int dayNow = isCurrent ? now.get(Calendar.DAY_OF_MONTH) : daysInMonth;

            long inCur = db.sumByTipe("in", from, to);
            long outCur = db.sumByTipe("out", from, to);
            if (inCur == 0 && outCur == 0) { sc.hasData = false; return sc; }
            sc.hasData = true;

            // ---- 1) rasio tabungan (30)
            int tabScore;
            String tabNote;
            if (inCur > 0) {
                double rate = (inCur - outCur) * 1.0 / inCur;
                tabScore = clamp((int) Math.round(rate / 0.25 * 100));
                tabNote = "Menabung " + Math.round(rate * 100) + "% dari pemasukan"
                        + (rate >= 0.2 ? " — ideal!" : " (target 20%)");
            } else {
                tabScore = outCur > 0 ? 0 : 50;
                tabNote = "Belum ada pemasukan tercatat";
            }
            sc.components.add(new Component("Rasio Tabungan", tabScore, 30, tabNote));

            // ---- 2) pengendalian pengeluaran (25): vs bulan lalu
            Calendar prev = (Calendar) c.clone();
            prev.add(Calendar.MONTH, -1);
            int py = prev.get(Calendar.YEAR), pm = prev.get(Calendar.MONTH) + 1;
            String pFrom = String.format(Locale.US, "%04d-%02d-01", py, pm);
            prev.set(Calendar.DAY_OF_MONTH, prev.getActualMaximum(Calendar.DAY_OF_MONTH));
            String pTo = f.format(prev.getTime());
            long outPrev = db.sumByTipe("out", pFrom, pTo);
            int kendScore;
            String kendNote;
            if (outPrev <= 0) {
                kendScore = 70; kendNote = "Belum ada pembanding bulan lalu";
            } else {
                double chg = (outCur - outPrev) * 1.0 / outPrev;
                kendScore = chg <= 0 ? 100 : clamp((int) Math.round(100 - chg * 200));
                kendNote = chg < -0.005
                        ? "Pengeluaran turun " + Math.round(-chg * 100) + "% vs bulan lalu — mantap!"
                        : (chg > 0.005
                            ? "Pengeluaran naik " + Math.round(chg * 100) + "% vs bulan lalu"
                            : "Pengeluaran stabil vs bulan lalu");
            }
            sc.components.add(new Component("Pengendalian", kendScore, 25, kendNote));

            // ---- 3) konsistensi mencatat (20)
            int activeDays = db.distinctActiveDays(from, to);
            int konsScore = dayNow > 0 ? clamp((int) Math.round(activeDays * 100.0 / dayNow)) : 0;
            sc.components.add(new Component("Konsistensi", konsScore, 20,
                    "Mencatat " + activeDays + " dari " + dayNow + " hari"));

            // ---- 4) target tabungan (15)
            int tgtScore;
            String tgtNote;
            BudgetPlanner.GoalProgress gp = BudgetPlanner.progress(db);
            if (gp != null && gp.target > 0) {
                tgtScore = clamp(gp.pct);
                tgtNote = "Target " + gp.pct + "% tercapai";
            } else {
                tgtScore = 50; tgtNote = "Belum pasang target tabungan";
            }
            sc.components.add(new Component("Target", tgtScore, 15, tgtNote));

            // ---- 5) dana darurat (10): saldo = berapa x pengeluaran bulanan
            long saldo = db.sumByTipe("in", "0000-01-01", to)
                    - db.sumByTipe("out", "0000-01-01", to);
            long avgOut = db.avgMonthlyOut(3);
            int darScore;
            String darNote;
            if (avgOut > 0 && saldo > 0) {
                double x = saldo * 1.0 / avgOut;
                darScore = clamp((int) Math.round(x / 6.0 * 100));
                darNote = "Dana darurat ±" + String.format(Locale.US, "%.1f", x)
                        + "x pengeluaran bulanan (ideal 6x)";
            } else if (saldo <= 0) {
                darScore = 0; darNote = "Saldo menipis — waspada!";
            } else {
                darScore = 50; darNote = "Belum ada pola pengeluaran";
            }
            sc.components.add(new Component("Dana Darurat", darScore, 10, darNote));

            // ---- total
            double tot = 0;
            for (Component cp : sc.components) tot += cp.score * cp.weight / 100.0;
            sc.total = clamp((int) Math.round(tot));
            if (sc.total >= 80) { sc.label = "Sehat"; sc.emoji = "💚"; sc.color = "#22B573"; }
            else if (sc.total >= 60) { sc.label = "Cukup"; sc.emoji = "💛"; sc.color = "#D97706"; }
            else if (sc.total >= 40) { sc.label = "Waspada"; sc.emoji = "🟠"; sc.color = "#F59E0B"; }
            else { sc.label = "Kritis"; sc.emoji = "🔴"; sc.color = "#F45050"; }

            // ---- tren vs bulan lalu
            try {
                Score prevSc = compute(db, py, pm);
                sc.delta = prevSc.hasData ? sc.total - prevSc.total : 0;
            } catch (Throwable ignored) { sc.delta = 0; }

            // ---- saran konkret: 2 komponen terendah
            List<Component> sorted = new ArrayList<Component>(sc.components);
            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (sorted.get(j).score < sorted.get(i).score) {
                        Component t = sorted.get(i); sorted.set(i, sorted.get(j)); sorted.set(j, t);
                    }
                }
            }
            for (Component cp : sorted) {
                if (sc.suggestions.size() >= 2) break;
                if (cp.score >= 80) continue;
                String s = suggestionFor(db, cp, inCur, outCur, avgOut, saldo);
                if (s != null) sc.suggestions.add(s);
            }
        } catch (Throwable ignored) {}
        return sc;
    }

    private static String suggestionFor(DbHelper db, Component cp, long inCur,
                                       long outCur, long avgOut, long saldo) {
        try {
            int gain = (int) Math.round((100 - cp.score) * cp.weight / 100.0);
            if (gain <= 0) return null;
            if ("Rasio Tabungan".equals(cp.name)) {
                long need = inCur > 0 ? Math.max(0, (long) (inCur * 0.25) - (inCur - outCur)) : 0;
                if (need <= 0) need = Math.max(50000, inCur / 20);
                return "💰 Sisihkan " + AiInsight.rp(need) + "/bulan lagi untuk nabung → skor +"
                        + gain + ".";
            }
            if ("Pengendalian".equals(cp.name)) {
                Map<String, Long> cats = topOutCat(db);
                String top = cats.isEmpty() ? "pengeluaran terbesarmu" : cats.keySet().iterator().next();
                long v = cats.isEmpty() ? Math.max(100000, outCur / 10) : cats.get(top);
                long cut = Math.max(50000, v / 4);
                return "✂️ Pangkas " + top + " " + AiInsight.rp(cut) + "/bulan → skor +"
                        + gain + ".";
            }
            if ("Konsistensi".equals(cp.name)) {
                return "📝 Catat setiap hari — konsistensi penuh bisa +"
                        + gain + " poin.";
            }
            if ("Target".equals(cp.name)) {
                return "🎯 Buat target tabungan di kartu Target Tabungan → skor +"
                        + gain + ".";
            }
            if ("Dana Darurat".equals(cp.name)) {
                long need = avgOut > 0 ? Math.max(0, avgOut * 3 - Math.max(0, saldo)) : 0;
                if (need <= 0) need = 500000;
                return "🛟 Kumpulkan dana darurat " + AiInsight.rp(need)
                        + " lagi → skor +" + gain + ".";
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static Map<String, Long> topOutCat(DbHelper db) {
        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = String.format(Locale.US, "%04d-%02d-01",
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
            String to = f.format(now.getTime());
            Map<String, Long> cats = db.sumOutByCat(from, to);
            Map<String, Long> one = new java.util.LinkedHashMap<String, Long>();
            for (Map.Entry<String, Long> e : cats.entrySet()) {
                if (!"Tabungan".equals(e.getKey()) && !"Darurat".equals(e.getKey())) {
                    one.put(e.getKey(), e.getValue());
                    break;
                }
            }
            return one;
        } catch (Throwable t) {
            return new java.util.LinkedHashMap<String, Long>();
        }
    }
}
