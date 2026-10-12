package com.flexurio.bursaku;

import java.util.Locale;

/** Format angka gaya Indonesia untuk tampilan. Murni Java (bisa di-unit-test). */
public class Fmt {
    static final Locale ID = Locale.GERMANY; // 1.234.567 & koma desimal

    /** Rupiah tanpa desimal: 10.500 */
    public static String rp(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(ID, "%,.0f", v);
    }

    /** Indeks dengan 2 desimal: 6.148,41 */
    public static String idx(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(ID, "%,.2f", v);
    }

    /** Dolar AS: $4,160.30 */
    public static String usd(double v) {
        if (Double.isNaN(v)) return "—";
        return "$" + String.format(Locale.US, "%,.2f", v);
    }

    /** Persen bertanda: +2,50% */
    public static String pct(double v) {
        if (Double.isNaN(v)) return "—";
        return String.format(ID, "%+.2f%%", v);
    }

    /** Volume ringkas: 1,2 jt / 3,4 M */
    public static String vol(long v) {
        if (v >= 1_000_000_000L) return String.format(ID, "%,.1f M", v / 1e9);
        if (v >= 1_000_000L) return String.format(ID, "%,.1f jt", v / 1e6);
        if (v >= 1_000L) return String.format(ID, "%,.0f rb", v / 1e3);
        return String.valueOf(v);
    }

    /** Jam WIB singkat untuk penanda auto-refresh: "13:16". Murni Java. */
    public static String fmtHour(long ms) {
        if (ms <= 0) return "—";
        java.util.Calendar c = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Jakarta"));
        c.setTimeInMillis(ms);
        return String.format(java.util.Locale.US, "%02d:%02d",
                c.get(java.util.Calendar.HOUR_OF_DAY),
                c.get(java.util.Calendar.MINUTE));
    }

    /** Rp ringkas untuk P/L: Rp 1,2 jt */
    public static String rpShort(double v) {
        if (Double.isNaN(v)) return "—";
        String sign = v < 0 ? "-" : "";
        double a = Math.abs(v);
        if (a >= 1e9) return sign + "Rp " + String.format(ID, "%,.2f M", a / 1e9);
        if (a >= 1e6) return sign + "Rp " + String.format(ID, "%,.1f jt", a / 1e6);
        if (a >= 1e3) return sign + "Rp " + String.format(ID, "%,.0f rb", a / 1e3);
        return sign + "Rp " + String.format(ID, "%,.0f", a);
    }
}
