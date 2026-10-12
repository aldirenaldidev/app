package com.flexurio.bursaku;

/**
 * Skala warna heatmap pasar: merah vivid (turun) → netral → hijau vivid (naik).
 * Intensitas penuh pada |perubahan| >= 5%. Murni (bisa di-unit-test).
 */
public class HeatColor {

    static final int RED = 0xFFEA3943;   // turun
    static final int NEU = 0xFF3A4356;   // flat / netral
    static final int GRN = 0xFF16C784;   // naik
    private static final double CLAMP = 5.0;

    /** chgPct dalam persen (mis. 2.5 = +2,5%). NaN → netral. */
    public static int colorFor(double chgPct) {
        if (Double.isNaN(chgPct)) return NEU;
        double t = Math.max(-CLAMP, Math.min(CLAMP, chgPct)) / CLAMP; // -1..1
        if (t >= 0) return mix(NEU, GRN, t);
        return mix(NEU, RED, -t);
    }

    /** Interpolasi linear dua warna ARGB. */
    static int mix(int a, int b, double t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) (ar + (br - ar) * t + 0.5);
        int g = (int) (ag + (bg - ag) * t + 0.5);
        int bl = (int) (ab + (bb - ab) * t + 0.5);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Warna teks yang kontras di atas sel (putih selalu cukup di sini). */
    public static int textFor(double chgPct) {
        return 0xFFFFFFFF;
    }
}
