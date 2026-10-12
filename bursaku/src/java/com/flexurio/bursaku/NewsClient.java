package com.flexurio.bursaku;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregator berita pasar via RSS (terverifikasi 2026-10-07):
 *   - CNBC Indonesia : https://www.cnbcindonesia.com/rss (RSS 2.0 valid)
 *   - IDX Channel    : https://www.idxchannel.com/rss (RSS 2.0 valid)
 * Kontan &amp; Bisnis Indonesia diblokir (403 / bukan XML) — tidak dipakai.
 * Parse memakai XmlPullParser bawaan Android.
 */
public class NewsClient {

    static final String[][] FEEDS = {
        {"https://www.cnbcindonesia.com/rss", "CNBC Indonesia"},
        {"https://www.idxchannel.com/rss", "IDX Channel"},
    };

    /** Ambil & gabungkan semua feed, urut terbaru dulu. */
    public static List<NewsItem> fetchAll() {
        List<NewsItem> out = new ArrayList<>();
        for (String[] f : FEEDS) {
            try {
                String xml = YahooClient.get(f[0]);
                out.addAll(parse(xml, f[1]));
            } catch (Exception e) { /* satu feed gagal → lewati */ }
        }
        Collections.sort(out, new Comparator<NewsItem>() {
            public int compare(NewsItem a, NewsItem b) {
                return Long.compare(b.pubTime, a.pubTime);
            }
        });
        return out;
    }

    /** Parse RSS 2.0 → daftar item. */
    public static List<NewsItem> parse(String xml, String source) throws Exception {
        List<NewsItem> out = new ArrayList<>();
        XmlPullParser p = Xml.newPullParser();
        p.setInput(new StringReader(xml));
        String title = null, link = null, pub = null;
        boolean inItem = false;
        int ev = p.getEventType();
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                String n = p.getName();
                if ("item".equals(n)) { inItem = true; title = link = pub = null; }
                else if (inItem && "title".equals(n)) title = p.nextText();
                else if (inItem && "link".equals(n)) link = p.nextText();
                else if (inItem && "pubDate".equals(n)) pub = p.nextText();
            } else if (ev == XmlPullParser.END_TAG && "item".equals(p.getName())) {
                if (inItem && title != null && link != null) {
                    out.add(new NewsItem(title.trim(), link.trim(), source,
                            parseRssDate(pub)));
                }
                inItem = false;
            }
            ev = p.next();
        }
        return out;
    }

    static final Map<String, Integer> MONTHS = new HashMap<>();
    static {
        MONTHS.put("Jan", 1); MONTHS.put("Feb", 2); MONTHS.put("Mar", 3);
        MONTHS.put("Apr", 4); MONTHS.put("May", 5); MONTHS.put("Jun", 6);
        MONTHS.put("Jul", 7); MONTHS.put("Aug", 8); MONTHS.put("Sep", 9);
        MONTHS.put("Oct", 10); MONTHS.put("Nov", 11); MONTHS.put("Dec", 12);
    }

    /** "Wed, 07 Oct 2026 13:16:00 +0700" → epoch ms (pakai offset +0700). */
    static long parseRssDate(String s) {
        if (s == null) return 0;
        try {
            // buang nama hari, ambil "07 Oct 2026 13:16:00"
            String t = s.trim();
            int comma = t.indexOf(',');
            if (comma >= 0) t = t.substring(comma + 1).trim();
            String[] parts = t.split("\\s+");
            int day = Integer.parseInt(parts[0]);
            int mon = MONTHS.get(parts[1]);
            int year = Integer.parseInt(parts[2]);
            String[] hm = parts[3].split(":");
            java.util.Calendar c = java.util.Calendar.getInstance(
                    java.util.TimeZone.getTimeZone("Asia/Jakarta"));
            c.set(year, mon - 1, day, Integer.parseInt(hm[0]),
                    Integer.parseInt(hm[1]), hm.length > 2 ? Integer.parseInt(hm[2]) : 0);
            return c.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Format waktu berita: "07 Okt 2026 13:16". */
    public static String fmtTime(long ms) {
        if (ms <= 0) return "";
        java.util.Calendar c = java.util.Calendar.getInstance(
                java.util.TimeZone.getTimeZone("Asia/Jakarta"));
        c.setTimeInMillis(ms);
        String[] idMon = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                          "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
        return String.format(java.util.Locale.US, "%02d %s %04d %02d:%02d",
                c.get(java.util.Calendar.DAY_OF_MONTH),
                idMon[c.get(java.util.Calendar.MONTH)],
                c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.HOUR_OF_DAY),
                c.get(java.util.Calendar.MINUTE));
    }
}
