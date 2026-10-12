package com.flexurio.dompetku;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Perencana anggaran otomatis + analis target tabungan. 100% offline.
 * Top-level class agar aman untuk d8.
 */
public final class BudgetPlanner {
    private BudgetPlanner() {}

    /** Rencana default 50/30/20 yang disesuaikan kategori DompetKu. */
    public static Map<String, Double> defaultPlan() {
        Map<String, Double> m = new LinkedHashMap<String, Double>();
        // kebutuhan pokok ±50%
        m.put("Makan", 18.0);
        m.put("Transport", 10.0);
        m.put("Tagihan", 14.0);
        m.put("Kesehatan", 5.0);
        // tabungan + darurat 28%
        m.put("Tabungan", 20.0);
        m.put("Darurat", 8.0);
        // gaya hidup 18%
        m.put("Belanja", 10.0);
        m.put("Hiburan", 8.0);
        // lain-lain
        m.put("Lainnya", 7.0);
        return m;
    }

    public static class BudgetRow {
        public String kategori;
        public double persen;
        public long nominal;
        public long spent;
        public int pct; // spent/nominal*100
        public String status; // ok | warn | over
    }

    /** Bangun plan dari income; simpan ke DB; kembalikan baris + warning. */
    public static List<BudgetRow> buildPlan(DbHelper db, long income) {
        List<BudgetRow> rows = new ArrayList<BudgetRow>();
        try {
            Map<String, Double> plan = defaultPlan();
            db.clearBudget();
            for (Map.Entry<String, Double> e : plan.entrySet()) {
                db.setBudgetItem(e.getKey(), e.getValue());
            }
            rows = currentPlan(db, income);
        } catch (Throwable ignored) {}
        return rows;
    }

    /** Baca plan tersimpan + hitung realisasi bulan berjalan. */
    public static List<BudgetRow> currentPlan(DbHelper db, long income) {
        List<BudgetRow> rows = new ArrayList<BudgetRow>();
        try {
            Map<String, Double> plan = db.getBudgetItems();
            if (plan.isEmpty()) plan = defaultPlan();
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = String.format(Locale.US, "%04d-%02d-01",
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
            String to = f.format(now.getTime());
            Map<String, Long> spent = db.sumOutByCat(from, to);
            for (Map.Entry<String, Double> e : plan.entrySet()) {
                BudgetRow r = new BudgetRow();
                r.kategori = e.getKey();
                r.persen = e.getValue();
                r.nominal = Math.round(income * e.getValue() / 100.0);
                Long s = spent.get(e.getKey());
                r.spent = s == null ? 0 : s;
                r.pct = r.nominal > 0 ? (int) Math.round(r.spent * 100.0 / r.nominal) : 0;
                r.status = r.pct >= 100 ? "over" : (r.pct >= 80 ? "warn" : "ok");
                rows.add(r);
            }
        } catch (Throwable ignored) {}
        return rows;
    }

    /** Sesuaikan plan dari pola belanja bulan lalu (rata-rata tertimbang). */
    public static List<BudgetRow> adjustFromLastMonth(DbHelper db, long income) {
        try {
            Calendar prev = Calendar.getInstance();
            prev.add(Calendar.MONTH, -1);
            int y = prev.get(Calendar.YEAR);
            int m = prev.get(Calendar.MONTH) + 1;
            String from = String.format(Locale.US, "%04d-%02d-01", y, m);
            Calendar pe = (Calendar) prev.clone();
            pe.set(Calendar.DAY_OF_MONTH, pe.getActualMaximum(Calendar.DAY_OF_MONTH));
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String to = f.format(pe.getTime());
            Map<String, Long> last = db.sumOutByCat(from, to);
            long total = 0;
            for (Long v : last.values()) total += v;
            if (total <= 0) return buildPlan(db, income);

            Map<String, Double> cur = db.getBudgetItems();
            if (cur.isEmpty()) cur = defaultPlan();
            db.clearBudget();
            // 60% pola bulan lalu + 40% plan lama, lalu normalisasi ke 100%
            Map<String, Double> raw = new LinkedHashMap<String, Double>();
            double sum = 0;
            for (Map.Entry<String, Double> e : cur.entrySet()) {
                Long lv = last.get(e.getKey());
                double lastPct = total > 0 && lv != null ? lv * 100.0 / total : 0;
                double v = lastPct * 0.6 + e.getValue() * 0.4;
                raw.put(e.getKey(), v);
                sum += v;
            }
            for (Map.Entry<String, Double> e : raw.entrySet()) {
                double p = sum > 0 ? Math.round(e.getValue() * 100.0 / sum * 10.0) / 10.0 : 0;
                db.setBudgetItem(e.getKey(), p);
            }
            return currentPlan(db, income);
        } catch (Throwable t) {
            return buildPlan(db, income);
        }
    }

    // ================= savings goal =================

    public static class GoalAnalysis {
        public long perMonth;
        public int pctSalary;
        public int months;
        public boolean feasible;
        public String advice;
        public long altTarget;   // target alternatif realistis
        public int altMonths;    // durasi alternatif
        public String cutCategory;
        public long cutAmount;
    }

    /** Analisis kelayakan target tabungan. */
    public static GoalAnalysis analyze(DbHelper db, long target, String deadlineYmd, long salary) {
        GoalAnalysis a = new GoalAnalysis();
        try {
            int months = monthsBetween(todayYmd(), deadlineYmd);
            if (months < 1) months = 1;
            a.months = months;
            a.perMonth = (long) Math.ceil(target * 1.0 / months);
            a.pctSalary = salary > 0 ? (int) Math.round(a.perMonth * 100.0 / salary) : 0;
            long avgSurplus = db.avgMonthlySurplus(3);

            // kategori pengeluaran terbesar bulan lalu untuk saran potong
            Calendar prev = Calendar.getInstance();
            prev.add(Calendar.MONTH, -1);
            int y = prev.get(Calendar.YEAR);
            int mm = prev.get(Calendar.MONTH) + 1;
            String from = String.format(Locale.US, "%04d-%02d-01", y, mm);
            Calendar pe = (Calendar) prev.clone();
            pe.set(Calendar.DAY_OF_MONTH, pe.getActualMaximum(Calendar.DAY_OF_MONTH));
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Map<String, Long> cats = db.sumOutByCat(from, f.format(pe.getTime()));
            String top = null;
            long topV = 0;
            for (Map.Entry<String, Long> e : cats.entrySet()) {
                if (!"Tabungan".equals(e.getKey()) && !"Darurat".equals(e.getKey())
                        && e.getValue() > topV) {
                    topV = e.getValue(); top = e.getKey();
                }
            }
            a.cutCategory = top;
            a.cutAmount = topV;

            boolean salaryOk = salary <= 0 || a.pctSalary <= 30;
            boolean surplusOk = avgSurplus <= 0 || a.perMonth <= avgSurplus * 1.25;
            a.feasible = salaryOk && surplusOk;

            if (a.feasible) {
                a.advice = "Target realistis! Sisihkan " + AiInsight.rp(a.perMonth)
                        + "/bulan (" + a.pctSalary + "% dari gaji).";
            } else {
                long realistic = avgSurplus > 0 ? (long) (avgSurplus * 0.9) : (long) (salary * 0.2);
                if (realistic < 1) realistic = Math.max(1, target / 24);
                a.altTarget = realistic * months;
                a.altMonths = (int) Math.ceil(target * 1.0 / Math.max(1, realistic));
                StringBuilder sb = new StringBuilder();
                sb.append("Target berat: perlu ").append(AiInsight.rp(a.perMonth))
                  .append("/bulan (").append(a.pctSalary).append("% gaji). ");
                if (avgSurplus > 0 && a.perMonth > avgSurplus) {
                    sb.append("Rata-rata sisamu ").append(AiInsight.rp(avgSurplus)).append("/bulan. ");
                }
                sb.append("Alternatif: target ").append(AiInsight.rp(a.altTarget))
                  .append(" dalam ").append(months).append(" bulan, atau ")
                  .append(AiInsight.rp(target)).append(" dalam ").append(a.altMonths).append(" bulan.");
                if (top != null) {
                    sb.append(" Coba pangkas ").append(top).append(" (")
                      .append(AiInsight.rp(topV)).append("/bulan).");
                }
                a.advice = sb.toString();
            }
        } catch (Throwable ignored) {}
        return a;
    }

    public static class GoalProgress {
        public long target;
        public String deadline;
        public String start;
        public long salary;
        public long collected;
        public int pct;
        public int monthsLeft;
        public long perMonthNeeded;
        public boolean onTrack;
        public String reminder;
        public String projection;
    }

    /** Progress target tabungan: terkumpul = surplus sejak start. */
    public static GoalProgress progress(DbHelper db) {
        GoalProgress g = new GoalProgress();
        try {
            Map<String, String> sg = db.getSavingsGoal();
            if (sg == null) return null;
            g.target = Long.parseLong(sg.get("target"));
            g.deadline = sg.get("deadline");
            g.start = sg.get("start");
            g.salary = Long.parseLong(sg.get("salary"));

            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String today = f.format(Calendar.getInstance().getTime());
            long inn = db.sumByTipe("in", g.start, today);
            long out = db.sumByTipe("out", g.start, today);
            g.collected = Math.max(0, inn - out);
            g.pct = g.target > 0 ? (int) Math.min(100, Math.round(g.collected * 100.0 / g.target)) : 0;
            g.monthsLeft = monthsBetween(today, g.deadline);
            long remain = Math.max(0, g.target - g.collected);
            g.perMonthNeeded = g.monthsLeft > 0
                    ? (long) Math.ceil(remain * 1.0 / g.monthsLeft) : remain;

            // bulan berjalan: surplus vs kebutuhan
            Calendar now = Calendar.getInstance();
            String mFrom = String.format(Locale.US, "%04d-%02d-01",
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
            long mIn = db.sumByTipe("in", mFrom, today);
            long mOut = db.sumByTipe("out", mFrom, today);
            long mSurplus = mIn - mOut;
            long need = g.monthsLeft > 0 ? (long) Math.ceil(remain * 1.0 / g.monthsLeft) : 0;
            g.onTrack = mSurplus >= need && need > 0;
            if (remain <= 0) {
                g.reminder = "🎉 Target tercapai! Terkumpul " + AiInsight.rp(g.collected)
                        + " dari " + AiInsight.rp(g.target) + ".";
            } else if (!g.onTrack) {
                g.reminder = "⏰ Bulan ini baru terkumpul " + AiInsight.rp(Math.max(0, mSurplus))
                        + ", butuh " + AiInsight.rp(need) + " agar target tercapai.";
            } else {
                g.reminder = "✅ Bagus! Bulan ini sudah " + AiInsight.rp(mSurplus)
                        + " (target " + AiInsight.rp(need) + ").";
            }

            long avg = db.avgMonthlySurplus(3);
            if (remain > 0 && avg > 0) {
                int est = (int) Math.ceil(remain * 1.0 / avg);
                Calendar c = Calendar.getInstance();
                c.add(Calendar.MONTH, est);
                g.projection = "Dengan pola nabung saat ini (" + AiInsight.rp(avg)
                        + "/bulan), target tercapai sekitar "
                        + AiInsight.BULAN[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR) + ".";
            } else if (remain > 0) {
                g.projection = "Belum ada pola menabung yang terdeteksi.";
            } else {
                g.projection = "";
            }
        } catch (Throwable t) {
            return null;
        }
        return g;
    }

    static String todayYmd() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());
    }

    /** Jumlah bulan dari a (yyyy-MM-dd) ke b. */
    static int monthsBetween(String a, String b) {
        try {
            String[] pa = a.split("-"), pb = b.split("-");
            int ya = Integer.parseInt(pa[0]), ma = Integer.parseInt(pa[1]);
            int yb = Integer.parseInt(pb[0]), mb = Integer.parseInt(pb[1]);
            return Math.max(1, (yb - ya) * 12 + (mb - ma));
        } catch (Throwable t) {
            return 1;
        }
    }
}
