package com.flexurio.sholatku;

import android.app.Activity;
import android.webkit.JavascriptInterface;

/** Jembatan JS -> tampilkan interstitial (throttle 5 menit di AdsHelper). */
public class AdsBridge {
    private final Activity act;

    public AdsBridge(Activity act) {
        this.act = act;
    }

    @JavascriptInterface
    public void showInterstitial() {
        try {
            act.runOnUiThread(new Runnable() {
                @Override public void run() {
                    AdsHelper.maybeShowInterstitial(act);
                }
            });
        } catch (Throwable ignored) { }
    }
}
