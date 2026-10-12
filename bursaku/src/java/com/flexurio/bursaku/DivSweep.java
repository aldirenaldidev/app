package com.flexurio.bursaku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Logika pindai dividen penuh: SEMUA emiten IDX (~940 ticker),
 * bukan hanya LQ45.
 *
 * - Backoff eksponensial saat rate limit Yahoo (429): 5s → 15s → 60s,
 *   lalu lewati emiten itu dan lanjut (tidak crash).
 * - Watermark: bila terputus (aplikasi ditutup / dibatalkan),
 *   pindai berikutnya lanjut dari indeks terakhir.
 * - Hasil inkremental: UI diperbarui tiap BATCH emiten.
 * - Emiten dengan cache segar (< 30 hari) dilewati.
 *
 * Murni Java (tanpa Android) agar bisa di-unit-test.
 */
public class DivSweep {

    /** Refresh UI inkremental tiap N emiten selesai. */
    public static final int BATCH = 20;

    /** Maksimal percobaan per emiten sebelum dilewati. */
    public static final int MAX_ATTEMPTS = 3;

    /** Kunci watermark di sweep_state. */
    public static final String KEY_IDX = "divsweep_idx";
    public static final String KEY_DONE = "divsweep_done";
    public static final String KEY_FAILED = "divsweep_failed";

    /**
     * Backoff (ms) untuk percobaan ke-n (1-based).
     * Mengembalikan -1 bila sudah harus menyerah.
     */
    public static long backoffMs(int attempt) {
        if (attempt == 1) return 5_000L;
        if (attempt == 2) return 15_000L;
        if (attempt == 3) return 60_000L;
        return -1L;
    }

    /**
     * Indeks mulai yang aman dari watermark tersimpan.
     * Nilai rusak / di luar rentang → mulai dari 0.
     */
    public static int resumeIndex(String saved, int total) {
        try {
            int i = Integer.parseInt(saved.trim());
            if (i >= 0 && i < total) return i;
        } catch (Exception ignored) { /* jatuh ke 0 */ }
        return 0;
    }

    /** true bila jumlah selesai ini menutup satu batch (untuk refresh UI). */
    public static boolean batchDone(int doneCount) {
        return doneCount > 0 && doneCount % BATCH == 0;
    }

    /** Estimasi durasi pindai penuh (menit) untuk komunikasi jujur di UI. */
    public static double estMinutes(int total, long perReqMs) {
        if (total <= 0 || perReqMs <= 0) return 0;
        return total * (double) perReqMs / 60000.0;
    }

    /** Baris yield minimal untuk sorting/pemfilteran murni. */
    public static class YieldRow {
        public String symbol;
        public String name;
        public double price;
        public double yieldPct;
        public double annual;
        public double payout;

        public YieldRow(String s, String n, double p, double y, double a, double pay) {
            symbol = s; name = n; price = p; yieldPct = y; annual = a; payout = pay;
        }
    }

    /**
     * Saring yang valid (yield > 0), urut yield tertinggi dulu,
     * ambil top-N. Dipakai untuk hasil inkremental & ekspor.
     */
    public static List<YieldRow> topRows(List<YieldRow> rows, int n) {
        List<YieldRow> valid = new ArrayList<YieldRow>();
        if (rows != null) {
            for (YieldRow r : rows) {
                if (r != null && !Double.isNaN(r.yieldPct) && r.yieldPct > 0) valid.add(r);
            }
        }
        Collections.sort(valid, new Comparator<YieldRow>() {
            public int compare(YieldRow a, YieldRow b) {
                return Double.compare(b.yieldPct, a.yieldPct);
            }
        });
        return valid.subList(0, Math.min(n, valid.size()));
    }
}
