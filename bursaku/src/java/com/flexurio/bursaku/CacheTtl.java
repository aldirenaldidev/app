package com.flexurio.bursaku;

/**
 * Strategi cache bertingkat: TTL per kategori data.
 * Data harga segar; data fundamental (dividen/keuangan/holders)
 * yang jarang berubah di-cache lama agar hemat kuota & baterai.
 * Murni (tanpa Android) agar bisa di-unit-test.
 */
public class CacheTtl {

    public static final String QUOTE = "QUOTE";         // harga/quote, indeks, valas
    public static final String CHART = "CHART";         // OHLCV historis
    public static final String NEWS = "NEWS";           // berita
    public static final String RANKING = "RANKING";     // ranking LQ45
    public static final String DIVIDEND = "DIVIDEND";   // dividen
    public static final String FINANCIAL = "FINANCIAL"; // keuangan
    public static final String HOLDERS = "HOLDERS";     // pemegang saham
    public static final String INFO = "INFO";           // info emiten

    private static final long MIN = 60_000L;
    private static final long HOUR = 60 * MIN;
    private static final long DAY = 24 * HOUR;

    /** TTL dalam milidetik per kategori. */
    public static long ttlMs(String category) {
        if (QUOTE.equals(category)) return 5 * MIN;
        if (CHART.equals(category)) return 1 * DAY;
        if (NEWS.equals(category)) return 30 * MIN;
        if (RANKING.equals(category)) return 15 * MIN;
        if (DIVIDEND.equals(category)) return 30 * DAY;
        if (FINANCIAL.equals(category)) return 30 * DAY;
        if (HOLDERS.equals(category)) return 30 * DAY;
        if (INFO.equals(category)) return 90 * DAY;
        return 5 * MIN;
    }

    /** true bila cache basi / belum pernah ada. */
    public static boolean isStale(long updatedAt, long now, String category) {
        if (updatedAt <= 0) return true;
        return now - updatedAt >= ttlMs(category);
    }

    /**
     * Label umur data Bahasa Indonesia:
     * "baru saja" | "5 menit lalu" | "3 jam lalu" | "3 hari lalu" | "2 bulan lalu".
     */
    public static String ageLabel(long updatedAt, long now) {
        if (updatedAt <= 0) return "belum pernah";
        long d = now - updatedAt;
        if (d < 0) d = 0;
        if (d < MIN) return "baru saja";
        if (d < HOUR) return (d / MIN) + " menit lalu";
        if (d < DAY) return (d / HOUR) + " jam lalu";
        if (d < 30 * DAY) return (d / DAY) + " hari lalu";
        long months = d / (30 * DAY);
        if (months < 12) return months + " bulan lalu";
        return (months / 12) + " tahun lalu";
    }
}
