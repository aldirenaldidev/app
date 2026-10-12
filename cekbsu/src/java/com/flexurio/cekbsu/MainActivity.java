package com.flexurio.cekbsu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

/**
 * Cek BSU — aplikasi panduan BSU (WebView, konten lokal offline).
 * Tautan resmi dibuka di browser eksternal. Banner AdMob (test IDs).
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        web = new WebView(this);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        web.setLayoutParams(wp);
        WebSettings st = web.getSettings();
        st.setJavaScriptEnabled(true);
        st.setDomStorageEnabled(true);
        st.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                return handleUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
            private boolean handleUrl(String url) {
                // file lokal: biarkan WebView; http/https: buka browser eksternal
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }
        });
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void openUrl(final String url) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        } catch (Exception ignored) { }
                    }
                });
            }
        }, "AndroidBridge");
        root.addView(web);

        // container banner AdMob di bawah WebView
        LinearLayout adWrap = new LinearLayout(this);
        adWrap.setOrientation(LinearLayout.VERTICAL);
        adWrap.setId(0x7f0b0001);
        adWrap.setVisibility(android.view.View.GONE);
        root.addView(adWrap);

        setContentView(root);
        web.loadUrl("file:///android_asset/www/index.html");

        AdsHelper.init(this);
        AdsHelper.banner(this, 0x7f0b0001);
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
