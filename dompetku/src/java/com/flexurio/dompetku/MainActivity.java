package com.flexurio.dompetku;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * DompetKu v2 — WebView wrapper di atas UI CryptoStar-style.
 * Setiap tahap dibungkus try/catch agar satu kegagalan
 * tidak membuat seluruh layar blank.
 */
public class MainActivity extends Activity {

    private static final String TAG = "DompetKu";
    private WebView web;
    private DbHelper db;
    private DompetBridge bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        safe("setContentView", new Step() {
            public void run() { setContentView(R.layout.activity_main); }
        });
        safe("adsInit", new Step() {
            public void run() { AdsHelper.init(MainActivity.this); }
        });
        safe("db", new Step() {
            public void run() { db = new DbHelper(MainActivity.this); }
        });
        safe("webview", new Step() {
            public void run() { setupWebView(); }
        });
        safe("banner", new Step() {
            public void run() { AdsHelper.banner(MainActivity.this, R.id.ad_container); }
        });
    }

    private void setupWebView() {
        web = (WebView) findViewById(R.id.webview);
        if (web == null) return;
        WebSettings s = web.getSettings();
        try { s.setJavaScriptEnabled(true); } catch (Throwable ignored) {}
        try { s.setDomStorageEnabled(true); } catch (Throwable ignored) {}
        try { s.setLoadWithOverviewMode(true); } catch (Throwable ignored) {}
        try { s.setUseWideViewPort(true); } catch (Throwable ignored) {}
        try { web.setWebViewClient(new WebViewClient()); } catch (Throwable ignored) {}
        try {
            bridge = new DompetBridge(this, db);
            web.addJavascriptInterface(bridge, "Android");
            bridge.setWebView(web);
        } catch (Throwable t) {
            Log.e(TAG, "bridge gagal", t);
        }
        try {
            web.loadUrl("file:///android_asset/www/index.html");
        } catch (Throwable t) {
            Log.e(TAG, "loadUrl gagal", t);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        try {
            if (bridge != null && bridge.handleActivityResult(requestCode, resultCode, data)) {
                return;
            }
        } catch (Throwable ignored) {}
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        try {
            if (web != null && web.canGoBack()) {
                web.goBack();
                return;
            }
        } catch (Throwable ignored) {}
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        try {
            if (db != null) db.close();
        } catch (Throwable ignored) {}
        try {
            if (web != null) web.destroy();
        } catch (Throwable ignored) {}
        super.onDestroy();
    }

    /** Interface langkah agar tanpa anonymous inner class yang rawan di d8 lama. */
    interface Step {
        void run();
    }

    private void safe(String name, Step step) {
        try {
            step.run();
        } catch (Throwable t) {
            try { Log.e(TAG, "safe gagal: " + name, t); } catch (Throwable ignored) {}
        }
    }
}
