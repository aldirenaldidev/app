package com.flexurio.dompetku;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Jembatan JS <-> native. Semua method defensif (try/catch) agar WebView tak pernah blank. */
public class DompetBridge {

    public static final int REQ_RESTORE = 9001;

    private final Activity act;
    private final DbHelper db;
    private WebView web;

    /** Topik terakhir percakapan asisten (utk resolusi pertanyaan ambigu). */
    private static String lastTopic = "";

    private static final String[] FINANCE_KW = {
        "uang","rp","rupiah","hemat","irit","nabung","tabung","menabung","budget","anggaran",
        "gaji","pengeluaran","pemasukan","saldo","transaksi","kategori","makan","minum","kopi",
        "transport","ojek","tagihan","listrik","belanja","hiburan","kesehatan","skor","sehat",
        "target","tujuan","bulan","minggu","hari","tahun","bayar","beli","cicil","cicilan",
        "utang","hutang","investasi","darurat","dompet","boros","sisa","mahal","murah","diskon",
        "promo","langganan","parkir","bensin","tol","pulsa","internet","wifi","sewa","kos",
        "kontrakan","sekolah","kuliah","sedekah","zakat","hadiah","liburan","travel","kafe",
        "restoran","jajan","masak","krl","bus","taksi","pesawat","tiket","cuan","profit","rugi",
        "emergency","dana","pensiun","nikah","rumah","motor","mobil","lunas","bayaran","thr",
        "bonus","lembur","freelance","usaha","dagang","jualan","omset","modal","aset","kekayaan",
        "miskin","kaya","kantong","saku","jajan","nongkrong","nobar","streaming","netflix",
        "spotify","game","topup","diamond","voucher","belanja","shopee","tokopedia","tiktok"
    };

    private static final String[] BULAN = {"Januari","Februari","Maret","April","Mei","Juni",
            "Juli","Agustus","September","Oktober","November","Desember"};

    public DompetBridge(Activity act, DbHelper db) {
        this.act = act;
        this.db = db;
    }

    /** Dipanggil MainActivity setelah WebView dibuat (untuk callback restore). */
    public void setWebView(WebView w) {
        try { this.web = w; } catch (Throwable ignored) {}
    }

    private static String ym(int y, int m1) {
        return String.format(Locale.US, "%04d-%02d", y, m1);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ");
    }

    /** scope: 0=harian, 1=mingguan(7 hari), 2=bulanan */
    @JavascriptInterface
    public String getData(int scope) {
        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String to = f.format(now.getTime());
            String from;
            String periode;
            if (scope == 0) {
                from = to;
                periode = "Hari ini";
            } else if (scope == 1) {
                Calendar c = (Calendar) now.clone();
                c.add(Calendar.DAY_OF_MONTH, -6);
                from = f.format(c.getTime());
                periode = "7 hari terakhir";
            } else {
                from = ym(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) + "-01";
                periode = BULAN[now.get(Calendar.MONTH)] + " " + now.get(Calendar.YEAR);
            }

            long masuk = db.sumByTipe("in", from, to);
            long keluar = db.sumByTipe("out", from, to);
            long saldo = db.sumByTipe("in", "0000-01-01", to) - db.sumByTipe("out", "0000-01-01", to);

            // % vs bulan lalu (hanya utk scope bulanan; lainnya 0)
            int pct = 0;
            if (scope == 2) {
                Calendar prev = (Calendar) now.clone();
                prev.add(Calendar.MONTH, -1);
                String pf = ym(prev.get(Calendar.YEAR), prev.get(Calendar.MONTH) + 1) + "-01";
                Calendar pe = (Calendar) prev.clone();
                pe.set(Calendar.DAY_OF_MONTH, pe.getActualMaximum(Calendar.DAY_OF_MONTH));
                String pt = f.format(pe.getTime());
                long outPrev = db.sumByTipe("out", pf, pt);
                if (outPrev > 0) pct = (int) Math.round((keluar - outPrev) * 100.0 / outPrev);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"saldo\":").append(saldo).append(",");
            sb.append("\"masuk\":").append(masuk).append(",");
            sb.append("\"keluar\":").append(keluar).append(",");
            sb.append("\"pct\":").append(pct).append(",");
            sb.append("\"periode\":\"").append(esc(periode)).append("\",");

            sb.append("\"insights\":[");
            List<AiInsight.Insight> ins = AiInsight.compute(db);
            // gabung peringatan budget + target tabungan + insight proaktif (maks total 6)
            try {
                for (AiInsight.Insight b : AiInsight.budgetWarnings(db)) {
                    if (ins.size() >= 6) break;
                    ins.add(b);
                }
                AiInsight.Insight kw = AiInsight.keywordInsight(db);
                if (kw != null && ins.size() < 6) ins.add(kw);
                AiInsight.Insight bp = AiInsight.balanceProjection(db);
                if (bp != null && ins.size() < 6) ins.add(bp);
                AiInsight.Insight sv = AiInsight.savingsReminder(db);
                if (sv != null && ins.size() < 6) ins.add(sv);
            } catch (Throwable ignored) {}
            for (int i = 0; i < ins.size(); i++) {
                AiInsight.Insight x = ins.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"icon\":\"").append(esc(x.icon)).append("\",");
                sb.append("\"bg\":\"").append(esc(x.bg)).append("\",");
                sb.append("\"t\":\"").append(esc(x.title)).append("\",");
                sb.append("\"b\":\"").append(esc(x.body)).append("\"}");
            }
            sb.append("],");

            sb.append("\"tx\":[");
            List<Tx> txs = db.listTx(from, to, 30);
            for (int i = 0; i < txs.size(); i++) {
                Tx t = txs.get(i);
                if (i > 0) sb.append(",");
                sb.append(txJson(t));
            }
            sb.append("]}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"saldo\":0,\"masuk\":0,\"keluar\":0,\"pct\":0,\"periode\":\"\",\"insights\":[],\"tx\":[]}";
        }
    }

    private static String txJson(Tx t) {
        return "{\"id\":" + t.id
                + ",\"tipe\":\"" + esc(t.tipe) + "\""
                + ",\"nominal\":" + t.nominal
                + ",\"kategori\":\"" + esc(t.kategori) + "\""
                + ",\"tanggal\":\"" + esc(t.tanggal) + "\""
                + ",\"catatan\":\"" + esc(t.catatan) + "\"}";
    }

    @JavascriptInterface
    public long addTx(String tipe, double nominal, String kategori, String tanggal, String catatan) {
        try {
            if (nominal <= 0) return -1;
            if (!"in".equals(tipe)) tipe = "out";
            if (kategori == null || kategori.isEmpty()) kategori = "Lainnya";
            return db.addTx(tipe, (long) nominal, kategori, tanggal, catatan);
        } catch (Throwable t) {
            return -1;
        }
    }

    @JavascriptInterface
    public boolean deleteTx(long id) {
        try {
            return db.deleteTx(id);
        } catch (Throwable t) {
            return false;
        }
    }

    @JavascriptInterface
    public String getRekap(int year, int month1) {
        try {
            String from = ym(year, month1) + "-01";
            Calendar c = Calendar.getInstance();
            c.set(year, month1 - 1, 1);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String to = f.format(c.getTime());

            long masuk = db.sumByTipe("in", from, to);
            long keluar = db.sumByTipe("out", from, to);
            int count = db.countTx(from, to);
            Map<String, Long> cats = db.sumOutByCat(from, to);
            long max = 1;
            for (Long v : cats.values()) if (v > max) max = v;

            StringBuilder sb = new StringBuilder();
            sb.append("{\"masuk\":").append(masuk)
              .append(",\"keluar\":").append(keluar)
              .append(",\"count\":").append(count)
              .append(",\"max\":").append(max)
              .append(",\"cats\":[");
            boolean first = true;
            for (Map.Entry<String, Long> e : cats.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("[\"").append(esc(e.getKey())).append("\",").append(e.getValue()).append("]");
            }
            sb.append("],\"tx\":[");
            List<Tx> txs = db.listTx(from, to, 0);
            for (int i = 0; i < txs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(txJson(txs.get(i)));
            }
            sb.append("]}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"masuk\":0,\"keluar\":0,\"count\":0,\"max\":1,\"cats\":[],\"tx\":[]}";
        }
    }

    @JavascriptInterface
    public String getAsisten() {
        try {
            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = ym(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) + "-01";
            String to = f.format(now.getTime());
            long masuk = db.sumByTipe("in", from, to);
            long keluar = db.sumByTipe("out", from, to);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"msgs\":[");
            if (masuk == 0 && keluar == 0) {
                sb.append("{\"t\":\"📝 Belum ada data bulan ini. Catat pemasukan dan pengeluaran dulu, lalu aku beri analisanya.\"}");
            } else {
                sb.append("{\"t\":\"").append(esc("\uD83D\uDCB0 Bulan ini: pemasukan " + AiInsight.rp(masuk)
                        + ", pengeluaran " + AiInsight.rp(keluar)
                        + ", sisa " + AiInsight.rp(masuk - keluar) + ".")) .append("\"}");
                Map<String, Long> cats = db.sumOutByCat(from, to);
                String top = null;
                long topV = 0;
                for (Map.Entry<String, Long> e : cats.entrySet()) {
                    if (e.getValue() > topV) { topV = e.getValue(); top = e.getKey(); }
                }
                if (top != null) {
                    sb.append(",{\"t\":\"").append(esc("🛍️ Kategori terbesar: " + top + " (" + AiInsight.rp(topV) + ").")).append("\"}");
                }
                // tawaran sesuaikan budget tiap awal bulan
                try {
                    long income = 0;
                    try { income = Long.parseLong(db.getSetting("income", "0")); } catch (Throwable ignored) {}
                    if (income > 0 && !db.getBudgetItems().isEmpty()
                            && now.get(Calendar.DAY_OF_MONTH) <= 7) {
                        sb.append(",{\"t\":\"").append(esc("🔄 Bulan baru! Mau aku sesuaikan budget plan dengan pola belanja bulan lalu? Ketik: sesuaikan budget")).append("\"}");
                    }
                } catch (Throwable ignored) {}
            }
            sb.append("],\"chips\":[\"Berapa sisa uangku?\",\"Kategori paling boros?\",\"💡 Bisa lebih hemat?\",\"💰 Budget-ku\",\"🎯 Target tabunganku\",\"❤️ Skor keuanganku\"]}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"msgs\":[],\"chips\":[]}";
        }
    }

    @JavascriptInterface
    public String askAsisten(String q) {
        try {
            if (q == null) q = "";
            String ql = q.toLowerCase(Locale.US).trim();

            // ---- sapaan ----
            if (isGreeting(ql)) {
                return "\uD83D\uDC4B Halo! Aku Asisten DompetKu. Tanya soal sisa uang, pengeluaran terbesarmu, "
                        + "atau ketik \u00ABbisa lebih hemat?\u00BB biar aku analisis datamu dan kasih saran konkret.";
            }

            // ---- di luar konteks keuangan: jujur, jangan ngarang ----
            if (!ql.isEmpty() && !isFinanceQuestion(ql)) {
                return "Maaf, aku hanya bisa bantu soal keuangan ya \u2014 catatan transaksi, budget, target tabungan, "
                        + "dan analisanya. Coba tanya \u00ABbisa lebih hemat?\u00BB \uD83D\uDCA1";
            }

            Calendar now = Calendar.getInstance();
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String from = ym(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1) + "-01";
            String to = f.format(now.getTime());
            long masuk = db.sumByTipe("in", from, to);
            long keluar = db.sumByTipe("out", from, to);

            // ---- saran hemat konkret berbasis data ----
            if (ql.contains("hemat") || ql.contains("irit")) {
                lastTopic = "tips";
                return formatSavingsTips();
            }
            // ---- skor kesehatan ----
            if (ql.contains("skor") || ql.contains("sehat") || ql.contains("nilai")) {
                lastTopic = "health";
                try {
                    HealthScore.Score s = HealthScore.compute(db,
                            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
                    if (!s.hasData) return "Belum ada cukup data untuk menghitung skor bulan ini.";
                    StringBuilder sb = new StringBuilder();
                    sb.append(s.emoji).append(" Skor keuanganmu bulan ini: ").append(s.total)
                      .append("/100 (").append(s.label).append(").");
                    if (!s.suggestions.isEmpty())
                        sb.append(" Saran tercepat: ").append(s.suggestions.get(0));
                    return sb.toString();
                } catch (Throwable t) {
                    return "Belum ada data skor.";
                }
            }
            // ---- sisa uang ----
            if (ql.contains("sisa")) {
                return "\uD83D\uDCB0 Sisa uangmu bulan ini " + AiInsight.rp(masuk - keluar)
                        + " (pemasukan " + AiInsight.rp(masuk) + " \u2212 pengeluaran "
                        + AiInsight.rp(keluar) + ").";
            }
            // ---- pengeluaran terbesar (data nyata) ----
            if (ql.contains("boros") || ql.contains("terbesar") || ql.contains("paling banyak")) {
                Map<String, Long> cats = db.sumOutByCat(from, to);
                String top = null;
                long topV = 0;
                long total = 0;
                for (Map.Entry<String, Long> e : cats.entrySet()) {
                    total += e.getValue();
                    if (e.getValue() > topV) { topV = e.getValue(); top = e.getKey(); }
                }
                if (top == null) return "Belum ada pengeluaran bulan ini.";
                int pct = total > 0 ? (int) Math.round(topV * 100.0 / total) : 0;
                StringBuilder sb = new StringBuilder();
                sb.append("\uD83D\uDECD\uFE0F Kategori terbesarmu: ").append(top)
                  .append(" (").append(AiInsight.rp(topV)).append(", ").append(pct)
                  .append("% dari total pengeluaran).");
                // contoh transaksi nyata terbesar di kategori itu
                try {
                    Tx big = null;
                    for (Tx t : db.listTx(from, to, 0)) {
                        if ("out".equals(t.tipe) && top.equals(t.kategori)
                                && (big == null || t.nominal > big.nominal)) big = t;
                    }
                    if (big != null && big.catatan != null && !big.catatan.isEmpty())
                        sb.append(" Transaksi terbesarnya: \u00AB").append(big.catatan)
                          .append("\u00BB ").append(AiInsight.rp(big.nominal)).append(".");
                } catch (Throwable ignored) {}
                return sb.toString();
            }
            // ---- pemasukan ----
            if (ql.contains("pemasukan") || ql.contains("masuk") || ql.contains("gaji")
                    || ql.contains("penghasilan")) {
                StringBuilder sb = new StringBuilder();
                sb.append("\uD83D\uDCB5 Pemasukan bulan ini ").append(AiInsight.rp(masuk)).append(".");
                try {
                    Map<String, Long> cats = new java.util.LinkedHashMap<String, Long>();
                    for (Tx t : db.listTx(from, to, 0)) {
                        if (!"in".equals(t.tipe)) continue;
                        Long cur = cats.get(t.kategori);
                        cats.put(t.kategori, (cur == null ? 0 : cur) + t.nominal);
                    }
                    String top = null;
                    long topV = 0;
                    for (Map.Entry<String, Long> e : cats.entrySet())
                        if (e.getValue() > topV) { topV = e.getValue(); top = e.getKey(); }
                    if (top != null)
                        sb.append(" Sumber terbesar: ").append(top)
                          .append(" (").append(AiInsight.rp(topV)).append(").");
                } catch (Throwable ignored) {}
                return sb.toString();
            }
            // ---- saldo ----
            if (ql.contains("saldo")) {
                long saldo = db.sumByTipe("in", "0000-01-01", to)
                        - db.sumByTipe("out", "0000-01-01", to);
                return "\uD83C\uDFE6 Saldo totalmu " + AiInsight.rp(saldo) + ".";
            }
            // ---- tabungan ----
            if (ql.contains("tabungan") || ql.contains("nabung") || ql.contains("menabung")) {
                long tab = db.sumByCatTipe("out", "Tabungan", from, to);
                long tabTotal = db.sumByCatTipe("out", "Tabungan", "0000-01-01", to);
                return "\uD83C\uDFE6 Bulan ini kamu menabung " + AiInsight.rp(tab)
                        + ". Total tabungan terkumpul " + AiInsight.rp(tabTotal) + ".";
            }
            // ---- langganan ----
            if (ql.contains("langganan")) {
                List<String> subs = AiInsight.findSubscriptions(db, from);
                if (subs.isEmpty())
                    return "Tidak terdeteksi tagihan berulang di datamu.";
                StringBuilder sb = new StringBuilder("\uD83D\uDD01 Tagihan berulang terdeteksi: ");
                long tot = 0;
                for (int i = 0; i < subs.size(); i++) {
                    String s = subs.get(i);
                    int bar = s.lastIndexOf('|');
                    long nom = 0;
                    try { nom = Long.parseLong(s.substring(bar + 1)); } catch (Throwable ignored) {}
                    tot += nom;
                    if (i > 0) sb.append("; ");
                    sb.append("\u00AB").append(s.substring(0, bar)).append("\u00BB ")
                      .append(AiInsight.rp(nom)).append("/bln");
                }
                sb.append(". Total ").append(AiInsight.rp(tot)).append("/bln \u2014 masih kepakai semua?");
                return sb.toString();
            }
            // ---- jumlah transaksi ----
            if (ql.contains("transaksi")) {
                int n = db.countTx(from, to);
                return "\uD83D\uDCDD Bulan ini ada " + n + " transaksi tercatat.";
            }
            // ---- sesuaikan budget ----
            if (ql.contains("sesuaikan budget")) {
                try {
                    String j = adjustBudgetPlan();
                    if (j.contains("\"ok\":false")) return "Isi income dulu di tab Budget ya.";
                    return "\uD83D\uDD04 Budget plan sudah kusesuaikan dengan pola belanja bulan lalu. Cek tab Budget.";
                } catch (Throwable t) {
                    return "Gagal menyesuaikan budget.";
                }
            }
            // ---- budget ----
            if (ql.contains("budget") || ql.contains("anggaran")) {
                lastTopic = "budget";
                try {
                    long income = 0;
                    try { income = Long.parseLong(db.getSetting("income", "0")); } catch (Throwable ignored) {}
                    if (income <= 0) return "\uD83D\uDCB0 Kamu belum punya budget plan. Isi gaji/pemasukan bulanan di tab Budget, biar aku buatkan rencananya otomatis.";
                    List<BudgetPlanner.BudgetRow> rows = BudgetPlanner.currentPlan(db, income);
                    int over = 0, warn = 0;
                    String overCat = null;
                    for (BudgetPlanner.BudgetRow r : rows) {
                        if ("over".equals(r.status)) { over++; overCat = r.kategori; }
                        else if ("warn".equals(r.status)) warn++;
                    }
                    if (over == 0 && warn == 0)
                        return "\uD83D\uDCB0 Budget aman! Semua kategori masih di bawah 80% rencana bulan ini.";
                    StringBuilder sb = new StringBuilder("\uD83D\uDCB0 Status budget: ");
                    if (over > 0) sb.append(over).append(" kategori melebihi rencana");
                    if (overCat != null) sb.append(" (").append(overCat).append(" paling parah)");
                    if (over > 0 && warn > 0) sb.append(", ");
                    if (warn > 0) sb.append(warn).append(" hampir habis");
                    sb.append(". Cek tab Budget untuk detailnya.");
                    return sb.toString();
                } catch (Throwable t) {
                    return "Belum ada data budget.";
                }
            }
            // ---- target tabungan ----
            if (ql.contains("target") || ql.contains("tujuan")) {
                lastTopic = "goal";
                try {
                    BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
                    if (g == null) return "\uD83C\uDFAF Kamu belum punya target tabungan. Buat di tab Budget \u2014 misal 50 juta dalam 12 bulan.";
                    return "\uD83C\uDFAF " + g.reminder + (g.projection.isEmpty() ? "" : " " + g.projection);
                } catch (Throwable t) {
                    return "Belum ada data target.";
                }
            }
            // ---- pertanyaan ambigu: resolusi via konteks percakapan ----
            if (ql.contains("tercapai") || ql.contains("terkejar") || ql.contains("kesampaian")) {
                if ("goal".equals(lastTopic)) {
                    try {
                        BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
                        if (g == null) return "Kamu belum punya target tabungan aktif.";
                        return (g.onTrack ? "\u2705 Ya, masih on track! " : "\u26A0\uFE0F Agak berat. ")
                                + g.reminder + (g.projection.isEmpty() ? "" : " " + g.projection);
                    } catch (Throwable t) {
                        return "Belum ada data target.";
                    }
                }
                return "Maksudmu target tabungan? Ceritakan dulu targetnya, atau buat di tab Budget.";
            }
            if (ql.contains("gimana") || ql.contains("bagaimana") || ql.contains("terus")
                    || ql.contains("lanjut")) {
                if ("goal".equals(lastTopic)) {
                    try {
                        BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
                        if (g == null) return "Buat dulu target tabunganmu di tab Budget.";
                        return "\uD83C\uDFAF " + g.reminder + " Fokus ke sana dulu ya.";
                    } catch (Throwable ignored) {}
                }
                if ("tips".equals(lastTopic)) return formatSavingsTips();
                if ("budget".equals(lastTopic)) {
                    return "Cek tab Budget untuk detail per kategorinya. Mau aku sesuaikan dengan pola bulan lalu? Ketik: sesuaikan budget";
                }
            }
            // ---- fallback: tetap relevan, arahkan ke yang bisa dijawab ----
            if (lastTopic.isEmpty())
                return "Aku menganalisis datamu secara offline. Coba tanya soal sisa uang, kategori terbesarmu, tabungan, atau ketik \u00ABbisa lebih hemat?\u00BB.";
            if ("goal".equals(lastTopic))
                return "Masih soal target tabungan ya? Tanya \u00ABbisa tercapai?\u00BB atau ubah targetnya di tab Budget.";
            if ("budget".equals(lastTopic))
                return "Masih soal budget? Tanya statusnya atau ketik \u00ABsesuaikan budget\u00BB.";
            return "Bisa diperjelas maksudnya? Aku bisa bantu soal sisa uang, pengeluaran, tabungan, budget, dan target.";
        } catch (Throwable t) {
            return "Belum ada data.";
        }
    }

    /** Sapaan / basa-basi ringan. */
    private static boolean isGreeting(String ql) {
        if (ql.isEmpty()) return false;
        String[] g = {"halo", "hai", "hi", "pagi", "siang", "sore", "malam",
                "assalamu", "makasih", "terima kasih", "thanks", "thank you"};
        for (String s : g) {
            if (ql.equals(s) || ql.startsWith(s + " ") || ql.endsWith(" " + s)
                    || ql.contains(" " + s + " ")) return true;
        }
        return false;
    }

    /** Heuristik: apakah pertanyaan terkait keuangan? */
    private static boolean isFinanceQuestion(String ql) {
        if (ql.length() < 2) return false;
        for (String k : FINANCE_KW) {
            if (ql.contains(k)) return true;
        }
        return false;
    }

    /** Format jawaban saran hemat konkret: 3-5 saran + total potensi hemat. */
    private String formatSavingsTips() {
        try {
            List<AiInsight.Tip> tips = AiInsight.savingsTips(db);
            if (tips.isEmpty())
                return "\uD83D\uDCA1 Catat dulu pengeluaranmu beberapa hari, lalu tanya lagi \u2014 biar saranku tepat sasaran, bukan asal tebak.";
            StringBuilder sb = new StringBuilder();
            sb.append("\uD83D\uDCA1 Bisa! Ini analisaku dari datamu:\n\n");
            long total = 0;
            for (int i = 0; i < tips.size(); i++) {
                AiInsight.Tip t = tips.get(i);
                total += t.save;
                sb.append(t.icon).append(" ").append(t.title).append("\n")
                  .append(t.body).append("\n\n");
            }
            sb.append("\uD83D\uDCB0 Total potensi hemat: ").append(AiInsight.rp(total))
              .append("/bulan. Kecil-kecil lama-lama jadi bukit \u2014 langsung alihkan ke tabungan ya!");
            return sb.toString();
        } catch (Throwable t) {
            return "Belum ada cukup data untuk analisis.";
        }
    }

    // ================= AI: auto-kategorisasi =================

    /** Kategorisasi otomatis dari teks. -> {tipe,kategori,conf,src,amount} */
    @JavascriptInterface
    public String categorize(String text) {
        try {
            SmartCategorizer.Result r = SmartCategorizer.categorize(db, text);
            return "{\"tipe\":\"" + esc(r.tipe) + "\",\"kategori\":\"" + esc(r.kategori)
                    + "\",\"conf\":" + r.confidence + ",\"src\":\"" + esc(r.src)
                    + "\",\"amount\":" + r.amount + "}";
        } catch (Throwable t) {
            return "{\"tipe\":\"out\",\"kategori\":\"Lainnya\",\"conf\":40,\"src\":\"default\",\"amount\":0}";
        }
    }

    /** AI belajar: user mengoreksi kategori -> kata baru disimpan. */
    @JavascriptInterface
    public void learnCorrection(String text, String tipe, String kategori) {
        try {
            SmartCategorizer.learn(db, text, tipe, kategori);
        } catch (Throwable ignored) {}
    }

    // ================= Budget Plan =================

    /** Simpan income + generate plan otomatis. -> plan JSON */
    @JavascriptInterface
    public String setupBudget(double income) {
        try {
            long inc = (long) income;
            if (inc <= 0) return "{\"ok\":false}";
            db.setSetting("income", String.valueOf(inc));
            BudgetPlanner.buildPlan(db, inc);
            return getBudget();
        } catch (Throwable t) {
            return "{\"ok\":false}";
        }
    }

    /** Plan + realisasi bulan berjalan + warning. */
    @JavascriptInterface
    public String getBudget() {
        try {
            long income = 0;
            try { income = Long.parseLong(db.getSetting("income", "0")); } catch (Throwable ignored) {}
            StringBuilder sb = new StringBuilder();
            sb.append("{\"income\":").append(income).append(",\"items\":[");
            List<BudgetPlanner.BudgetRow> rows = BudgetPlanner.currentPlan(db, income);
            for (int i = 0; i < rows.size(); i++) {
                BudgetPlanner.BudgetRow r = rows.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"kategori\":\"").append(esc(r.kategori)).append("\",");
                sb.append("\"persen\":").append(r.persen).append(",");
                sb.append("\"nominal\":").append(r.nominal).append(",");
                sb.append("\"spent\":").append(r.spent).append(",");
                sb.append("\"pct\":").append(r.pct).append(",");
                sb.append("\"status\":\"").append(r.status).append("\"}");
            }
            sb.append("]}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"income\":0,\"items\":[]}";
        }
    }

    /** Ubah persen satu kategori secara manual. */
    @JavascriptInterface
    public boolean updateBudgetItem(String kategori, double persen) {
        try {
            if (kategori == null || persen < 0 || persen > 100) return false;
            db.setBudgetItem(kategori, persen);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Bangun ulang plan dari pola bulan lalu. */
    @JavascriptInterface
    public String adjustBudgetPlan() {
        try {
            long income = 0;
            try { income = Long.parseLong(db.getSetting("income", "0")); } catch (Throwable ignored) {}
            if (income <= 0) return "{\"ok\":false}";
            BudgetPlanner.adjustFromLastMonth(db, income);
            return getBudget();
        } catch (Throwable t) {
            return "{\"ok\":false}";
        }
    }

    // ================= Savings Goal =================

    /** Buat target tabungan + analisis kelayakan. */
    @JavascriptInterface
    public String setSavingsGoal(double target, String deadline, double salary) {
        try {
            long tg = (long) target;
            long sal = (long) salary;
            if (tg <= 0 || deadline == null || deadline.length() < 7) return "{\"ok\":false}";
            BudgetPlanner.GoalAnalysis a =
                    BudgetPlanner.analyze(db, tg, deadline, sal);
            db.setSavingsGoal(tg, deadline, BudgetPlanner.todayYmd(), sal);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"ok\":true,");
            sb.append("\"perMonth\":").append(a.perMonth).append(",");
            sb.append("\"pctSalary\":").append(a.pctSalary).append(",");
            sb.append("\"months\":").append(a.months).append(",");
            sb.append("\"feasible\":").append(a.feasible).append(",");
            sb.append("\"advice\":\"").append(esc(a.advice)).append("\",");
            sb.append("\"altTarget\":").append(a.altTarget).append(",");
            sb.append("\"altMonths\":").append(a.altMonths).append("}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"ok\":false}";
        }
    }

    /** Progress target tabungan. */
    @JavascriptInterface
    public String getSavingsGoal() {
        try {
            BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
            if (g == null) return "{\"has\":false}";
            StringBuilder sb = new StringBuilder();
            sb.append("{\"has\":true,");
            sb.append("\"target\":").append(g.target).append(",");
            sb.append("\"deadline\":\"").append(esc(g.deadline)).append("\",");
            sb.append("\"collected\":").append(g.collected).append(",");
            sb.append("\"pct\":").append(g.pct).append(",");
            sb.append("\"monthsLeft\":").append(g.monthsLeft).append(",");
            sb.append("\"perMonthNeeded\":").append(g.perMonthNeeded).append(",");
            sb.append("\"onTrack\":").append(g.onTrack).append(",");
            sb.append("\"reminder\":\"").append(esc(g.reminder)).append("\",");
            sb.append("\"projection\":\"").append(esc(g.projection)).append("\"}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"has\":false}";
        }
    }

    @JavascriptInterface
    public void clearSavingsGoal() {
        try { db.clearSavingsGoal(); } catch (Throwable ignored) {}
    }

    // ================= Skor Kesehatan Keuangan =================

    /** Skor 0-100 + breakdown + saran + tren. */
    @JavascriptInterface
    public String getHealthScore() {
        try {
            Calendar now = Calendar.getInstance();
            HealthScore.Score s = HealthScore.compute(db,
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
            if (!s.hasData) return "{\"has\":false}";
            StringBuilder sb = new StringBuilder();
            sb.append("{\"has\":true,");
            sb.append("\"total\":").append(s.total).append(",");
            sb.append("\"label\":\"").append(esc(s.label)).append("\",");
            sb.append("\"emoji\":\"").append(esc(s.emoji)).append("\",");
            sb.append("\"color\":\"").append(esc(s.color)).append("\",");
            sb.append("\"delta\":").append(s.delta).append(",");
            sb.append("\"components\":[");
            for (int i = 0; i < s.components.size(); i++) {
                HealthScore.Component c = s.components.get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(esc(c.name)).append("\",");
                sb.append("\"score\":").append(c.score).append(",");
                sb.append("\"weight\":").append(c.weight).append(",");
                sb.append("\"note\":\"").append(esc(c.note)).append("\"}");
            }
            sb.append("],\"suggestions\":[");
            for (int i = 0; i < s.suggestions.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(esc(s.suggestions.get(i))).append("\"");
            }
            sb.append("]}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"has\":false}";
        }
    }

    // ================= Profile =================

    /** Profil: nama, foto, ringkasan tabungan, progress target. */
    @JavascriptInterface
    public String getProfile() {
        try {
            String name = db.getSetting("profile_name", "");
            String photo = db.getSetting("profile_photo", "");
            long totalSav = db.sumByCatTipe("out", "Tabungan", "0000-01-01", "9999-12-31");

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
            long savCur = db.sumByCatTipe("out", "Tabungan", curFrom, curTo);
            long savPrev = db.sumByCatTipe("out", "Tabungan", prevFrom, prevTo);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"name\":\"").append(esc(name)).append("\",");
            sb.append("\"hasPhoto\":").append(photo != null && photo.length() > 20).append(",");
            if (photo != null && photo.length() > 20 && photo.length() < 800000)
                sb.append("\"photo\":\"").append(esc(photo)).append("\",");
            else
                sb.append("\"photo\":\"\",");
            sb.append("\"totalSavings\":").append(totalSav).append(",");
            sb.append("\"savCur\":").append(savCur).append(",");
            sb.append("\"savPrev\":").append(savPrev).append(",");
            try {
                BudgetPlanner.GoalProgress g = BudgetPlanner.progress(db);
                if (g == null) {
                    sb.append("\"goal\":null");
                } else {
                    sb.append("\"goal\":{");
                    sb.append("\"target\":").append(g.target).append(",");
                    sb.append("\"deadline\":\"").append(esc(g.deadline)).append("\",");
                    sb.append("\"collected\":").append(g.collected).append(",");
                    sb.append("\"pct\":").append(g.pct).append(",");
                    sb.append("\"monthsLeft\":").append(g.monthsLeft).append(",");
                    sb.append("\"onTrack\":").append(g.onTrack);
                    sb.append("}");
                }
            } catch (Throwable ignored) {
                sb.append("\"goal\":null");
            }
            sb.append("}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"name\":\"\",\"hasPhoto\":false,\"photo\":\"\",\"totalSavings\":0,"
                    + "\"savCur\":0,\"savPrev\":0,\"goal\":null}";
        }
    }

    @JavascriptInterface
    public boolean setProfileName(String name) {
        try {
            if (name == null) name = "";
            name = name.trim();
            if (name.length() > 40) name = name.substring(0, 40);
            db.setSetting("profile_name", name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Simpan foto profil (data URI, dibatasi ~600KB). */
    @JavascriptInterface
    public boolean setProfilePhoto(String dataUri) {
        try {
            if (dataUri == null || !dataUri.startsWith("data:image")) return false;
            if (dataUri.length() > 700000) return false;
            db.setSetting("profile_photo", dataUri);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ================= Backup & Restore =================

    /** Export seluruh data ke JSON lalu share (user pilih Google Drive / lainnya). */
    @JavascriptInterface
    public void exportBackup() {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "dompetku");
            root.put("version", 1);
            root.put("exported_at", System.currentTimeMillis());

            JSONArray txs = new JSONArray();
            for (Tx t : db.listAllTx()) {
                JSONObject o = new JSONObject();
                o.put("tipe", t.tipe);
                o.put("nominal", t.nominal);
                o.put("kategori", t.kategori);
                o.put("tanggal", t.tanggal);
                o.put("catatan", t.catatan == null ? "" : t.catatan);
                txs.put(o);
            }
            root.put("transaksi", txs);

            JSONArray words = new JSONArray();
            for (DbHelper.Word w : db.getLearnedWords()) {
                JSONObject o = new JSONObject();
                o.put("word", w.word);
                o.put("tipe", w.tipe);
                o.put("kategori", w.kategori);
                o.put("weight", w.weight);
                words.put(o);
            }
            root.put("ai_words", words);

            JSONArray settings = new JSONArray();
            for (Map.Entry<String, String> e : db.getAllSettings().entrySet()) {
                JSONObject o = new JSONObject();
                o.put("k", e.getKey());
                o.put("v", e.getValue());
                settings.put(o);
            }
            root.put("settings", settings);

            JSONArray bi = new JSONArray();
            for (Map.Entry<String, Double> e : db.getBudgetItems().entrySet()) {
                JSONObject o = new JSONObject();
                o.put("kategori", e.getKey());
                o.put("persen", e.getValue());
                bi.put(o);
            }
            root.put("budget_items", bi);

            Map<String, String> g = db.getSavingsGoal();
            if (g != null) {
                JSONObject o = new JSONObject();
                o.put("target", g.get("target"));
                o.put("deadline", g.get("deadline"));
                o.put("start", g.get("start"));
                o.put("salary", g.get("salary"));
                root.put("savings_goal", o);
            } else {
                root.put("savings_goal", JSONObject.NULL);
            }

            File dir = new File(act.getCacheDir(), "backup");
            if (!dir.exists()) dir.mkdirs();
            String fname = "dompetku-backup-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                    + ".json";
            File out = new File(dir, fname);
            FileWriter fw = new FileWriter(out);
            fw.write(root.toString());
            fw.close();

            Uri uri = FileProvider.getUriForFile(act,
                    "com.flexurio.dompetku.fileprovider", out);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, "Backup DompetKu");
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            act.startActivity(Intent.createChooser(send, "Simpan backup ke..."));
        } catch (Throwable t) {
            toast("Backup gagal");
        }
    }

    /** Buka file picker untuk memilih file backup JSON. */
    @JavascriptInterface
    public void pickRestoreFile() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("application/json");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            act.startActivityForResult(
                    Intent.createChooser(i, "Pilih file backup"), REQ_RESTORE);
        } catch (Throwable t) {
            toast("Tidak bisa membuka file picker");
        }
    }

    /** Dipanggil MainActivity.onActivityResult. */
    public boolean handleActivityResult(int req, int res, Intent data) {
        if (req != REQ_RESTORE) return false;
        try {
            if (res == Activity.RESULT_OK && data != null && data.getData() != null) {
                InputStream in = act.getContentResolver().openInputStream(data.getData());
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    sb.append(new String(buf, 0, n, "UTF-8"));
                    if (sb.length() > 5 * 1024 * 1024) break;
                }
                try { in.close(); } catch (Throwable ignored) {}
                final String quoted = JSONObject.quote(sb.toString());
                final WebView wv = web;
                if (wv != null) {
                    act.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                wv.evaluateJavascript(
                                        "window.__onRestoreFile(" + quoted + ")", null);
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            } else {
                toast("Restore dibatalkan");
            }
        } catch (Throwable t) {
            toast("Gagal membaca file backup");
        }
        return true;
    }

    /** Import JSON backup (JS sudah konfirmasi overwrite dulu). -> "ok" / "err:..." */
    @JavascriptInterface
    public String importBackup(String json) {
        try {
            if (json == null || json.length() < 10) return "err:File kosong";
            String err = db.restoreFromJson(new JSONObject(json));
            return err == null ? "ok" : "err:" + err;
        } catch (Throwable t) {
            return "err:File rusak / bukan JSON valid";
        }
    }

    // ================= Saran hemat konkret =================

    /** 3-5 saran hemat konkret + total potensi hemat. */
    @JavascriptInterface
    public String getSavingsTips() {
        try {
            List<AiInsight.Tip> tips = AiInsight.savingsTips(db);
            StringBuilder sb = new StringBuilder();
            sb.append("{\"tips\":[");
            long total = 0;
            for (int i = 0; i < tips.size(); i++) {
                AiInsight.Tip x = tips.get(i);
                total += x.save;
                if (i > 0) sb.append(",");
                sb.append("{\"icon\":\"").append(esc(x.icon)).append("\",");
                sb.append("\"title\":\"").append(esc(x.title)).append("\",");
                sb.append("\"body\":\"").append(esc(x.body)).append("\",");
                sb.append("\"save\":").append(x.save).append("}");
            }
            sb.append("],\"total\":").append(total).append("}");
            return sb.toString();
        } catch (Throwable t) {
            return "{\"tips\":[],\"total\":0}";
        }
    }

    @JavascriptInterface
    public void showInterstitial() {        try {
            AdsHelper.maybeShowInterstitial(act);
        } catch (Throwable ignored) {}
    }

    @JavascriptInterface
    public void toast(final String msg) {
        try {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }
}
