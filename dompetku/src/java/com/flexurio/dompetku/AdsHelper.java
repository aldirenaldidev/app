package com.flexurio.dompetku;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Helper AdMob terpusat. Semua pemanggilan defensif:
 * bila init/load gagal, aplikasi tetap jalan normal.
 * Top-level class agar aman untuk d8.
 */
public class AdsHelper {

    private static boolean initialized = false;
    private static InterstitialAd interstitial = null;
    private static long lastShown = 0;
    private static final long GAP_MS = 5 * 60_000L;

    public static synchronized void init(final Activity act) {
        if (initialized) return;
        initialized = true;
        try {
            MobileAds.initialize(act, new OnInitializationCompleteListener() {
                public void onInitializationComplete(InitializationStatus status) {
                    preload(act);
                }
            });
        } catch (Throwable ignored) {}
    }

    public static void banner(final Activity act, final int containerId) {
        try {
            final ViewGroup container = (ViewGroup) act.findViewById(containerId);
            if (container == null) return;
            final AdView adView = new AdView(act);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(AdsConfig.BANNER_ID);
            container.addView(adView);
            adView.setAdListener(new AdListener() {
                @Override public void onAdLoaded() {
                    try { container.setVisibility(View.VISIBLE); } catch (Throwable ignored) {}
                }
                @Override public void onAdFailedToLoad(LoadAdError err) {
                    try { container.setVisibility(View.GONE); } catch (Throwable ignored) {}
                }
            });
            adView.loadAd(new AdRequest.Builder().build());
        } catch (Throwable t) {
            try {
                View c = act.findViewById(containerId);
                if (c != null) c.setVisibility(View.GONE);
            } catch (Throwable ignored) {}
        }
    }

    private static void preload(final Activity act) {
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
        } catch (Throwable ignored) {}
    }

    /** Tampilkan interstitial bila sudah lewat GAP_MS sejak terakhir. */
    public static void maybeShowInterstitial(final Activity act) {
        try {
            long now = System.currentTimeMillis();
            if (now - lastShown < GAP_MS) return;
            if (interstitial == null) {
                preload(act);
                return;
            }
            lastShown = now;
            final InterstitialAd ad = interstitial;
            interstitial = null;
            act.runOnUiThread(new Runnable() {
                public void run() {
                    try { ad.show(act); } catch (Throwable ignored) {}
                }
            });
            preload(act);
        } catch (Throwable ignored) {}
    }
}
