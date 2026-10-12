package com.flexurio.bursaku;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Status bursa (Buka/Tutup) berdasarkan jam WIB.
 * Sesi reguler IDX — Senin–Kamis: 09:00–12:00 &amp; 14:00–15:15;
 * Jumat: 09:00–11:30 &amp; 14:00–15:15; Sabtu–Minggu tutup.
 * Murni Java (bisa di-unit-test).
 */
public class MarketStatus {
    public static class Info {
        public boolean open;
        public String label;   // "BUKA" / "TUTUP"
        public String session; // "Sesi 1" / "Sesi 2" / ""
    }

    public static Info now() {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta"));
        return at(c.get(Calendar.DAY_OF_WEEK),
                  c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE));
    }

    static Info at(int dayOfWeek, int minuteOfDay) {
        Info i = new Info();
        i.open = false;
        i.label = "TUTUP";
        i.session = "";
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return i;
        boolean fri = (dayOfWeek == Calendar.FRIDAY);
        int s1start = 9 * 60, s1end = fri ? 11 * 60 + 30 : 12 * 60;
        int s2start = 14 * 60, s2end = 15 * 60 + 15;
        if (minuteOfDay >= s1start && minuteOfDay < s1end) {
            i.open = true; i.label = "BUKA"; i.session = "Sesi 1";
        } else if (minuteOfDay >= s2start && minuteOfDay < s2end) {
            i.open = true; i.label = "BUKA"; i.session = "Sesi 2";
        }
        return i;
    }
}
