package com.flexurio.bursaku;

/**
 * Konfigurasi Google AdMob untuk BursaKu.
 *
 * *** PENTING: GANTI DENGAN ID ASLI DARI AKUN ADMOB ***
 * Saat ini memakai ID TEST resmi Google — iklan test akan tampil
 * dengan label "Test Ad". Setelah punya akun AdMob sendiri:
 *  1. Buat aplikasi di console AdMob → dapatkan App ID (format ca-app-pub-XXXX~YYYY)
 *  2. Buat 2 Ad Unit: Banner + Interstitial → dapatkan Ad Unit ID (format ca-app-pub-XXXX/ZZZZ)
 *  3. Ganti ketiga konstanta di bawah, lalu ganti juga meta-data
 *     com.google.android.gms.ads.APPLICATION_ID di AndroidManifest.xml
 *     dengan App ID yang sama, lalu build ulang.
 * Panduan lengkap: ~/workspace/bursaku/ADMOB.md
 */
public class AdsConfig {

    /** ID TEST resmi Google. GANTI dengan App ID asli (ca-app-pub-XXXX~YYYY). */
    public static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";

    /** ID TEST banner resmi Google. GANTI dengan Banner Ad Unit ID asli. */
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";

    /** ID TEST interstitial resmi Google. GANTI dengan Interstitial Ad Unit ID asli. */
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    /** true = pakai ID test (aman diklik sendiri); false = ID produksi. */
    public static final boolean TEST_MODE =
            APP_ID.contains("3940256099942544");
}
