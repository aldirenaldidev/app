package com.flexurio.bursaku;

/** Satu berita dari RSS. */
public class NewsItem {
    public String title;
    public String link;
    public String source;
    public long pubTime; // epoch ms; 0 bila tak diketahui

    public NewsItem(String title, String link, String source, long pubTime) {
        this.title = title;
        this.link = link;
        this.source = source;
        this.pubTime = pubTime;
    }
}
