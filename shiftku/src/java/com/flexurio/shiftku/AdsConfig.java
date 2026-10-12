package com.flexurio.shiftku;

/**
 * ID IKLAN — GANTI DENGAN ID ASLI DARI AKUN ADMOB SAAT SIAP PRODUCTION.
 * Saat ini memakai ID TEST resmi Google (iklan test, tidak menghasilkan uang).
 * Lihat ADMOB.md untuk cara mendapatkan ID asli.
 */
public final class AdsConfig {
    private AdsConfig() {}

    /** App ID — juga harus sama dengan meta-data di AndroidManifest.xml */
    public static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";

    /** Banner bawah layar */
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";

    /** Interstitial — tampil maks 1x per 5 menit saat membuka jadwal */
    public static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
}
