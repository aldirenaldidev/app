package com.flexurio.bursaku;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * Penyedia crumb Yahoo (alur: cookie dari fc.yahoo.com → crumb).
 * Dipakai bersama oleh HoldersClient & FinancialsClient.
 */
public class YahooCrumb {

    static {
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        }
    }

    private static String cached = null;

    public static synchronized String get() throws Exception {
        if (cached == null) {
            YahooClient.get("https://fc.yahoo.com"); // kumpulkan cookie
            String c = YahooClient.get(
                    "https://query1.finance.yahoo.com/v1/test/getcrumb").trim();
            if (c.isEmpty() || c.charAt(0) == '{')
                throw new Exception("Gagal mendapatkan crumb Yahoo");
            cached = c;
        }
        return cached;
    }

    public static synchronized void invalidate() {
        cached = null;
    }
}
