package com.flexurio.dompetku;

/**
 * ID IKLAN — GANTI DENGAN ID ASLI DARI AKUN ADMOB SAAT SIAP PRODUCTION.
 * Saat ini memakai ID TEST resmi Google (iklan test, tidak menghasilkan uang).
 */
public final class AdsConfig {
    private AdsConfig() {}

    /** App ID — juga harus sama dengan meta-data di AndroidManifest.xml */
    public static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";

    /** Banner bawah layar */
    public static final String BANNER_ID = "ca-app-pub-3940256099942544/6300978111";

    /** Interstitial — tampil maks 1x per 5 menit */
    public static final String INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";
}
