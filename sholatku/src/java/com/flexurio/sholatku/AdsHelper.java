package com.flexurio.sholatku;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Helper AdMob terpusat. Semua pemanggilan defensif:
 * bila init/load gagal (offline, dsb), aplikasi tetap jalan
 * normal — banner disembunyikan, interstitial dilewati.
 */
public class AdsHelper {

    private static boolean initialized = false;
    private static InterstitialAd interstitial = null;
    private static long lastInterstitialShown = 0;
    private static final long INTERSTITIAL_MIN_GAP_MS = 5 * 60_000L;
    // Retry banner: jeda 5 dtk, 15 dtk, 60 dtk — maks 3x per sesi (reset saat sukses)
    private static final long[] BANNER_RETRY_DELAYS_MS = {5_000L, 15_000L, 60_000L};
    private static int bannerRetryCount = 0;

    /** Inisialisasi MobileAds sekali per proses. Aman dipanggil berulang. */
    public static synchronized void init(final Activity act) {
        if (initialized) return;
        initialized = true;
        try {
            MobileAds.initialize(act, new com.google.android.gms.ads.initialization.OnInitializationCompleteListener() {
                public void onInitializationComplete(
                        com.google.android.gms.ads.initialization.InitializationStatus status) {
                    preloadInterstitial(act);
                }
            });
        } catch (Throwable t) {
            // init gagal → semua iklan dilewati diam-diam
        }
    }

    /** Pasang banner ke container. Bila load gagal, coba lagi otomatis
     *  3x (jeda 5/15/60 dtk); container disembunyikan hanya bila semua gagal. */
    public static void banner(final Activity act, final int containerId) {
        try {
            final ViewGroup container = act.findViewById(containerId);
            if (container == null) return;
            final AdView adView = new AdView(act);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(AdsConfig.BANNER_ID);
            container.addView(adView);
            final android.os.Handler handler =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            adView.setAdListener(new AdListener() {
                @Override public void onAdLoaded() {
                    bannerRetryCount = 0;
                    container.setVisibility(View.VISIBLE);
                }

                @Override public void onAdFailedToLoad(LoadAdError err) {
                    if (bannerRetryCount < BANNER_RETRY_DELAYS_MS.length) {
                        final long delay = BANNER_RETRY_DELAYS_MS[bannerRetryCount];
                        bannerRetryCount++;
                        handler.postDelayed(new Runnable() {
                            @Override public void run() {
                                try {
                                    if (act.isFinishing() || act.isDestroyed()) return;
                                    adView.loadAd(new AdRequest.Builder().build());
                                } catch (Throwable ignored) { }
                            }
                        }, delay);
                    } else {
                        container.setVisibility(View.GONE);
                    }
                }
            });
            adView.loadAd(new AdRequest.Builder().build());
        } catch (Throwable t) {
            try {
                View c = act.findViewById(containerId);
                if (c != null) c.setVisibility(View.GONE);
            } catch (Throwable ignored) { }
        }
    }

    /** Preload interstitial di background. */
    public static void preloadInterstitial(final Activity act) {
        try {
            InterstitialAd.load(act, AdsConfig.INTERSTITIAL_ID,
                    new AdRequest.Builder().build(),
                    new InterstitialAdLoadCallback() {
                        @Override public void onAdLoaded(InterstitialAd ad) {
                            interstitial = ad;
                        }

                        @Override public void onAdFailedToLoad(LoadAdError err) {
                            interstitial = null;
                        }
                    });
        } catch (Throwable t) {
            interstitial = null;
        }
    }

    /**
     * Tampilkan interstitial bila sudah ter-load DAN sudah >= 5 menit
     * sejak terakhir tampil. Setelah tampil/dibuang, preload lagi.
     */
    public static void maybeShowInterstitial(final Activity act) {
        try {
            long now = System.currentTimeMillis();
            if (interstitial == null) {
                preloadInterstitial(act);
                return;
            }
            if (now - lastInterstitialShown < INTERSTITIAL_MIN_GAP_MS) return;
            lastInterstitialShown = now;
            final InterstitialAd ad = interstitial;
            interstitial = null;
            ad.setFullScreenContentCallback(
                    new com.google.android.gms.ads.FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() {
                            preloadInterstitial(act);
                        }

                        @Override public void onAdFailedToShowFullScreenContent(
                                com.google.android.gms.ads.AdError err) {
                            preloadInterstitial(act);
                        }
                    });
            ad.show(act);
        } catch (Throwable t) {
            try {
                preloadInterstitial(act);
            } catch (Throwable ignored) { }
        }
    }
}
