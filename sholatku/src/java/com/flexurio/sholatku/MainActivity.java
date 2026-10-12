package com.flexurio.sholatku;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/** SholatKu: WebView + bridge alarm adzan + kompas kiblat + AdMob. */
public class MainActivity extends Activity {

    public static final int AD_CONTAINER_ID = 1002;

    private WebView web;
    private QiblaBridge qibla;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Layout: WebView (bobot 1) + banner AdMob di bawah ---
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        web = new WebView(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new AlarmBridge(this), "AndroidAlarm");
        qibla = new QiblaBridge(this);
        web.addJavascriptInterface(qibla, "AndroidQibla");
        web.addJavascriptInterface(new AdsBridge(this), "AndroidAds");
        web.addJavascriptInterface(new OnboardingBridge(this, web), "AndroidOnboard");
        web.addJavascriptInterface(new AppBridge(this), "AndroidApp");
        root.addView(web, wp);

        FrameLayout adBox = new FrameLayout(this);
        adBox.setId(AD_CONTAINER_ID);
        adBox.setVisibility(View.GONE);
        root.addView(adBox, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        web.loadUrl("file:///android_asset/www/index.html");

        // Izin notifikasi (Android 13+) diminta dari onboarding langkah 1,
        // bukan otomatis di sini agar tidak muncul sebelum waktunya.

        // --- AdMob ---
        AdsHelper.init(this);
        AdsHelper.banner(this, AD_CONTAINER_ID);
    }

    @Override
    public void onBackPressed() {
        // Navigasi SPA: jika di sub-layar, kembali via JS goBack(); jika di tab utama, keluar aplikasi
        if (web != null) {
            web.evaluateJavascript(
                "(function(){ try { return (typeof curMain!=='undefined' && document.querySelector('.screen.active') && !['jadwal','imsakiyah','kiblat','lainnya'].includes(document.querySelector('.screen.active').id.replace('screen-',''))) ? 'back' : 'exit'; } catch(e){ return 'exit'; } })();",
                value -> {
                    if ("back".equals(value != null ? value.replace("\"", "") : "")) {
                        web.evaluateJavascript("try{ goBack(); }catch(e){}", null);
                    } else {
                        MainActivity.super.onBackPressed();
                    }
                });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (qibla != null) {
            try { qibla.stop(); } catch (Throwable ignored) { }
        }
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
