package com.flexurio.dompetku;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Insight otomatis 100% offline berbasis aturan data. Top-level agar aman untuk d8. */
public final class AiInsight {
    private AiInsight() {}

    public static class Insight {
        public String icon;
        public String bg;
        public String title;
        public String body;
        public Insight(String icon, String bg, String title, String body) {
            this.icon = icon; this.bg = bg; this.title = title; this.body = body;
        }
    }

    private static String ym(int year, int month1) {
        return String.format(Locale.US, "%04d-%02d", year, month1);
    }

    public static String rp(long n) {
        boolean neg = n < 0;
        String s = String.valueOf(Math.abs(n));
        StringBuilder r = new StringBuilder();
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            r.insert(0, s.charAt(i));
            c++;
            if (c % 3 == 0 && i > 0) r.insert(0, '.');
        }
        return "Rp " + (neg ? "-" : "") + r.toString();
    }

    public static List<Insight> compute(DbHelper db) {
        List<Insight> out = new ArrayList<Insight>();
        try {
            Calendar now = Calendar.getInstance();
            int y = now.get(Calendar.YEAR);
            int m = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DAY_OF_MONTH);
            int daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            String curFrom = ym(y, m) + "-01";
            String curTo = f.format(now.getTime());

            Calendar prev = (Calendar) now.clone();
            prev.add(Calendar.MONTH, -1);
            int py = prev.get(Calendar.YEAR);
            int pm = prev.get(Calendar.MONTH) + 1;
            String prevFrom = ym(py, pm) + "-01";
            Calendar prevEnd = (Calendar) prev.clone();
            prevEnd.set(Calendar.DAY_OF_MONTH, prevEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
            String prevTo = f.format(prevEnd.getTime());

            long outCur = db.sumByTipe("out", curFrom, curTo);
            long outPrev = db.sumByTipe("out", prevFrom, prevTo);
            long inCur = db.sumByTipe("in", curFrom, curTo);

            if (outCur == 0 && inCur == 0) {
                out.add(new Insight("\uD83D\uDCA1", "rgba(47,124,246,.12)",
                        "Mulai catat keuanganmu",
                        "Belum ada transaksi bulan ini. Tap tombol + untuk mencatat pengeluaran atau pemasukan pertamamu."));
                return out;
            }

            // 1) kategori naik >20% vs bulan lalu
            Map<String, Long> catCur = db.sumOutByCat(curFrom, curTo);
            Map<String, Long> catPrev = db.sumOutByCat(prevFrom, prevTo);
            String topRiser = null;
            int topPct = 0;
            long topDelta = 0;
            for (Map.Entry<String, Long> e : catCur.entrySet()) {
                Long p = catPrev.get(e.getKey());
                if (p != null && p > 0) {
                    int pct = (int) Math.round((e.getValue() - p) * 100.0 / p);
                    if (pct >= 20 && pct > topPct) {
                        topPct = pct; topRiser = e.getKey(); topDelta = e.getValue() - p;
                    }
                }
            }
            if (topRiser != null) {
                out.add(new Insight("⚠️", "rgba(244,80,80,.12)",
                        topRiser + " naik " + topPct + "%",
                        "Pengeluaran " + topRiser + " bulan ini " + rp(topDelta)
                        + " lebih besar dari bulan lalu. Waspadai kebocoran!"));
            } else if (outPrev > 0 && outCur > 0) {
                int pct = (int) Math.round((outCur - outPrev) * 100.0 / outPrev);
                if (pct <= -10) {
                    out.add(new Insight("\uD83C\uDF89", "rgba(34,181,115,.12)",
                            "Hemat " + (-pct) + "% bulan ini!",
                            "Total pengeluaranmu turun " + rp(outPrev - outCur)
                            + " dibanding bulan lalu. Pertahankan!"));
                } else {
                    out.add(new Insight("\uD83D\uDCCA", "rgba(124,108,240,.12)",
                            "Pengeluaran stabil",
                            "Total bulan ini " + rp(outCur) + " vs bulan lalu " + rp(outPrev) + "."));
                }
            }

            // 2) hari paling boros
            Map<String, Long> byDay = db.sumOutByDay(curFrom, curTo);
            String bigDay = null;
            long bigVal = 0;
            for (Map.Entry<String, Long> e : byDay.entrySet()) {
                if (e.getValue() > bigVal) { bigVal = e.getValue(); bigDay = e.getKey(); }
            }
            if (bigDay != null && byDay.size() >= 3) {
                out.add(new Insight("\uD83D\uDCC5", "rgba(245,158,11,.14)",
                        "Hari paling boros",
                        tglIndo(bigDay) + ": " + rp(bigVal) + " dalam sehari. Coba ingat-ingat untuk apa saja."));
            }

            // 3) proyeksi sisa bulan
            if (day >= 2 && outCur > 0) {
                long avg = outCur / day;
                int rest = daysInMonth - day;
                long estTotal = outCur + avg * rest;
                out.add(new Insight("\uD83D\uDD2E", "rgba(124,108,240,.12)",
                        "Proyeksi sisa bulan",
                        "Dengan rata-rata " + rp(avg) + "/hari, estimasi total bulan ini " + rp(estTotal) + "."));
            }

            // 4) rasio tabungan
            if (inCur > 0 && outCur > 0 && out.size() < 4) {
                int pctSave = (int) Math.round((inCur - outCur) * 100.0 / inCur);
                if (pctSave >= 20) {
                    out.add(new Insight("\uD83C\uDFC6", "rgba(34,181,115,.12)",
                            "Tabungan " + pctSave + "% — keren!",
                            "Kamu menyisihkan " + rp(inCur - outCur) + " bulan ini. Idealnya minimal 20%."));
                } else if (pctSave < 10) {
                    out.add(new Insight("\uD83D\uDEA8", "rgba(244,80,80,.12)",
                            "Hampir tidak menabung",
                            "Sisa bulan ini hanya " + rp(inCur - outCur) + " (" + pctSave + "% dari pemasukan)."));
                }
            }
        } catch (Throwable ignored) {}
        if (out.size() > 4) return out.subList(0, 4);
        return out;
    }

    static final String[] BULAN = {"Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"};

    /** Analisis token spesifik: "Kopi menghabiskan Rp 380rb, naik 40%". */
    public static Insight keywordInsight(DbHelper db) {
        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String curFrom = ym(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) + "-01";
            String curTo = f.format(now.getTime());
            Calendar prev = (Calendar) now.clone();
            prev.add(Calendar.MONTH, -1);
            String prevFrom = ym(prev.get(Calendar.YEAR), prev.get(Calendar.MONTH) + 1) + "-01";
            Calendar pe = (Calendar) prev.clone();
            pe.set(Calendar.DAY_OF_MONTH, pe.getActualMaximum(Calendar.DAY_OF_MONTH));
            String prevTo = f.format(pe.getTime());

            // kategori pengeluaran terbesar bulan ini
            Map<String, Long> cats = db.sumOutByCat(curFrom, curTo);
            String topCat = null;
            long topV = 0;
            for (Map.Entry<String, Long> e : cats.entrySet()) {
                if (e.getValue() > topV) { topV = e.getValue(); topCat = e.getKey(); }
            }
            if (topCat == null || topV < 100000) return null;

            // token dominan di kategori tersebut
            Map<String, Long> tokCur = db.sumOutByToken(curFrom, curTo, topCat);
            Map<String, Long> tokPrev = db.sumOutByToken(prevFrom, prevTo, topCat);
            String topTok = null;
            long tokV = 0;
            for (Map.Entry<String, Long> e : tokCur.entrySet()) {
                if (e.getValue() > tokV) { tokV = e.getValue(); topTok = e.getKey(); }
            }
            if (topTok == null || tokV < 50000) return null;
            // abaikan token yang sama dengan nama kategori
            if (topTok.equalsIgnoreCase(topCat)) return null;

            String label = topTok.substring(0, 1).toUpperCase(Locale.US) + topTok.substring(1);
            Long pv = tokPrev.get(topTok);
            if (pv != null && pv > 0) {
                int pct = (int) Math.round((tokV - pv) * 100.0 / pv);
                if (pct >= 25) {
                    return new Insight("☕", "rgba(245,158,11,.14)",
                            label + " menghabiskan " + rp(tokV),
                            "Bulan ini " + rp(tokV) + " untuk " + topTok + " — naik " + pct
                            + "% dari bulan lalu (" + rp(pv) + "). Dikurangi separuh bisa hemat "
                            + rp(tokV / 2) + ".");
                }
            } else if (pv == null) {
                // kebiasaan baru bulan ini
                return new Insight("\uD83D\uDD0D", "rgba(124,108,240,.12)",
                        label + ": pengeluaran baru " + rp(tokV),
                        "Bulan lalu tidak ada pengeluaran untuk " + topTok
                        + ". Pantau agar tidak jadi kebiasaan.");
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /** Proyeksi saldo akhir bulan dari run-rate harian. */
    public static Insight balanceProjection(DbHelper db) {
        try {
            Calendar now = Calendar.getInstance();
            int day = now.get(Calendar.DAY_OF_MONTH);
            if (day < 3) return null;
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = ym(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) + "-01";
            String to = f.format(now.getTime());
            long inCur = db.sumByTipe("in", from, to);
            long outCur = db.sumByTipe("out", from, to);
            if (inCur == 0 && outCur == 0) return null;
            long saldo = db.sumByTipe("in", "0000-01-01", to)
                    - db.sumByTipe("out", "0000-01-01", to);
            double dailyNet = (inCur - outCur) * 1.0 / day;
            int rest = now.getActualMaximum(Calendar.DAY_OF_MONTH) - day;
            long proj = Math.round(saldo + dailyNet * rest);
            String tone = proj >= 0 ? "Aman." : "Waspada, bisa minus!";
            return new Insight("\uD83D\uDD2E", "rgba(47,124,246,.12)",
                    "Proyeksi saldo akhir bulan: " + rp(proj),
                    "Dengan rata-rata harian " + rp(Math.round(dailyNet))
                    + ", saldo akhirmu sekitar " + rp(proj) + ". " + tone);
        } catch (Throwable ignored) {}
        return null;
    }
    public static List<Insight> budgetWarnings(DbHelper db) {
        List<Insight> out = new ArrayList<Insight>();
        try {
            String incomeS = db.getSetting("income", "0");
            long income = 0;
            try { income = Long.parseLong(incomeS); } catch (Throwable ignored) {}
            if (income <= 0) return out;
            if (db.getBudgetItems().isEmpty()) return out;
            List<BudgetPlanner.BudgetRow> rows = BudgetPlanner.currentPlan(db, income);
            for (BudgetPlanner.BudgetRow r : rows) {
                if ("over".equals(r.status)) {
                    out.add(new Insight("\uD83D\uDEA8", "rgba(244,80,80,.12)",
                            r.kategori + " melebihi budget!",
                            "Sudah " + rp(r.spent) + " dari budget " + rp(r.nominal)
                            + " (" + r.pct + "%). Rem dulu ya."));
                } else if ("warn".equals(r.status)) {
                    out.add(new Insight("⚠️", "rgba(245,158,11,.14)",
                            r.kategori + " sudah " + r.pct + "% dari budget",
                            "Terpakai " + rp(r.spent) + " dari " + rp(r.nominal)
                            + ". Sisa " + rp(Math.max(0, r.nominal - r.spent)) + "."));
                }
                if (out.size() >= 2) break;
            }
        } catch (Throwable ignored) {}
        return out;
    }

    /** Pengingat target tabungan bila bulan ini belum sesuai jalur. */
    public static Insight savingsReminder(DbHelper db) {
        try {
            BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
            if (g == null) return null;
            if (g.target > 0 && g.collected >= g.target) {
                return new Insight("\uD83C\uDF89", "rgba(34,181,115,.12)",
                        "Target tabungan tercapai!",
                        "Terkumpul " + rp(g.collected) + " dari " + rp(g.target) + ". Luar biasa!");
            }
            if (!g.onTrack && g.perMonthNeeded > 0) {
                return new Insight("⏰", "rgba(245,158,11,.14)",
                        "Target tabungan: " + g.pct + "%",
                        g.reminder);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    static String tglIndo(String d) {
        try {
            String[] p = d.split("-");
            return Integer.parseInt(p[2]) + " " + BULAN[Integer.parseInt(p[1]) - 1] + " " + p[0];
        } catch (Throwable t) {
            return d;
        }
    }

    // ============ Saran hemat konkret berbasis data ============

    public static class Tip {
        public String icon;
        public String title;
        public String body;
        public long save;
        public Tip(String icon, String title, String body, long save) {
            this.icon = icon; this.title = title; this.body = body; this.save = save;
        }
    }

    /**
     * 3-5 saran hemat konkret dengan estimasi penghematan Rp/bulan.
     * 100% berbasis data aktual user: token dominan, transport, langganan
     * berulang, kategori yang naik, dan pola makan.
     */
    public static List<Tip> savingsTips(DbHelper db) {
        List<Tip> out = new ArrayList<Tip>();
        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            int y = now.get(Calendar.YEAR);
            int m = now.get(Calendar.MONTH) + 1;
            String curFrom = ym(y, m) + "-01";
            String curTo = f.format(now.getTime());
            Calendar prev = (Calendar) now.clone();
            prev.add(Calendar.MONTH, -1);
            String prevFrom = ym(prev.get(Calendar.YEAR), prev.get(Calendar.MONTH) + 1) + "-01";
            Calendar pe = (Calendar) prev.clone();
            pe.set(Calendar.DAY_OF_MONTH, pe.getActualMaximum(Calendar.DAY_OF_MONTH));
            String prevTo = f.format(pe.getTime());

            Map<String, Long> catCur = db.sumOutByCat(curFrom, curTo);
            Map<String, Long> catPrev = db.sumOutByCat(prevFrom, prevTo);
            if (catCur.isEmpty()) return out;

            // 1) token dominan di kategori terbesar -> pangkas separuh
            String topCat = null;
            long topV = 0;
            for (Map.Entry<String, Long> e : catCur.entrySet()) {
                if (e.getValue() > topV) { topV = e.getValue(); topCat = e.getKey(); }
            }
            if (topCat != null) {
                Map<String, Long> tok = db.sumOutByToken(curFrom, curTo, topCat);
                String tk = null;
                long tv = 0;
                for (Map.Entry<String, Long> e : tok.entrySet()) {
                    if (!e.getKey().equalsIgnoreCase(topCat) && e.getValue() > tv) {
                        tv = e.getValue(); tk = e.getKey();
                    }
                }
                if (tk != null && tv >= 50000) {
                    String label = tk.substring(0, 1).toUpperCase(Locale.US) + tk.substring(1);
                    long save = tv / 2;
                    out.add(new Tip("🎯", "Pangkas " + label + ": hemat " + rp(save) + "/bln",
                            "Kamu menghabiskan " + rp(tv) + " untuk " + tk + " bulan ini. "
                            + "Kurangi separuh — batasi frekuensi atau cari alternatif lebih murah — "
                            + "dan alihkan selisihnya ke tabungan.", save));
                }
            }

            // 2) transport -> 2x seminggu ganti moda murah
            Long tr = catCur.get("Transport");
            if (tr != null && tr >= 100000) {
                long save = Math.round(tr * 0.25);
                out.add(new Tip("\uD83D\uDE8C", "Transport: hemat " + rp(save) + "/bln",
                        "Ojek/taksi/bensin " + rp(tr) + " bulan ini. Coba 2x seminggu ganti "
                        + "KRL, bus, atau nebeng teman — potensinya lumayan buat ditabung.", save));
            }

            // 3) langganan/tagihan berulang tiap bulan
            List<String> subs = findSubscriptions(db, curFrom);
            for (String s : subs) {
                int bar = s.lastIndexOf('|');
                String nm = s.substring(0, bar);
                long nom = 0;
                try { nom = Long.parseLong(s.substring(bar + 1)); } catch (Throwable ignored) {}
                if (nom < 50000) continue;
                out.add(new Tip("\uD83D\uDD01", "Langganan '" + nm + "': " + rp(nom) + "/bln",
                        "Tagihan ini muncul tiap bulan dengan nominal sama. Masih kepakai? "
                        + "Kalau jarang, berhenti = hemat " + rp(nom) + " setiap bulan.", nom));
                if (out.size() >= 5) break;
            }

            // 4) kategori yang naik >=20% vs bulan lalu
            for (Map.Entry<String, Long> e : catCur.entrySet()) {
                Long p = catPrev.get(e.getKey());
                if (p != null && p > 0) {
                    long delta = e.getValue() - p;
                    if (delta >= 100000 && delta * 100 / p >= 20) {
                        out.add(new Tip("⚠️", "Kembalikan " + e.getKey()
                                + " ke level bulan lalu: hemat " + rp(delta) + "/bln",
                                e.getKey() + " naik " + rp(delta) + " dibanding bulan lalu. "
                                + "Balik ke pola lama bulan depan dan tabung selisihnya.", delta));
                        break;
                    }
                }
            }

            // 5) makan/jajan besar -> masak 3x seminggu
            Long makan = catCur.get("Makan");
            if (makan != null && makan >= 300000) {
                long save = Math.round(makan * 0.3);
                out.add(new Tip("\uD83C\uDF72", "Masak 3x seminggu: hemat " + rp(save) + "/bln",
                        "Jajan/makan di luar " + rp(makan) + " bulan ini. Masak untuk 3x makan "
                        + "seminggu bisa memangkas sekitar 30%-nya.", save));
            }

            // urutkan dari penghematan terbesar, ambil 5
            for (int i = 0; i < out.size(); i++) {
                for (int j = i + 1; j < out.size(); j++) {
                    if (out.get(j).save > out.get(i).save) {
                        Tip t = out.get(i);
                        out.set(i, out.get(j));
                        out.set(j, t);
                    }
                }
            }
            if (out.size() > 5) return out.subList(0, 5);
        } catch (Throwable ignored) {}
        return out;
    }

    /**
     * Deteksi tagihan berulang: (catatan, nominal) sama muncul di >=2 bulan
     * berbeda termasuk bulan berjalan. @return list "nama|nominal".
     */
    static List<String> findSubscriptions(DbHelper db, String curFrom) {
        List<String> out = new ArrayList<String>();
        try {
            String curYm = curFrom.substring(0, 7);
            Map<String, java.util.Set<String>> seen =
                    new java.util.HashMap<String, java.util.Set<String>>();
            Map<String, Long> nom = new java.util.HashMap<String, Long>();
            for (Tx t : db.listAllTx()) {
                if (!"out".equals(t.tipe)) continue;
                String note = t.catatan == null ? "" : t.catatan.trim().toLowerCase();
                if (note.length() < 3) continue;
                if ("Gaji".equals(t.kategori)) continue;
                String key = note + "|" + t.nominal;
                String yymm = t.tanggal != null && t.tanggal.length() >= 7
                        ? t.tanggal.substring(0, 7) : "";
                if (yymm.isEmpty()) continue;
                java.util.Set<String> s = seen.get(key);
                if (s == null) {
                    s = new java.util.HashSet<String>();
                    seen.put(key, s);
                }
                s.add(yymm);
                nom.put(key, t.nominal);
            }
            for (Map.Entry<String, java.util.Set<String>> e : seen.entrySet()) {
                if (e.getValue().size() >= 2 && e.getValue().contains(curYm)) {
                    String key = e.getKey();
                    int bar = key.lastIndexOf('|');
                    String nm = key.substring(0, bar);
                    // tampilkan nama yang rapi
                    if (nm.length() > 28) nm = nm.substring(0, 28);
                    out.add(nm + "|" + nom.get(key));
                    if (out.size() >= 3) break;
                }
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
