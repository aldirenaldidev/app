package com.flexurio.shiftku;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Util tanggal. */
public class DateUtil {
    public static final String[] BULAN = {"Januari", "Februari", "Maret", "April",
            "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    public static final String[] HARI = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
    // Header kalender mockup: S S R K J S M
    public static final String[] HARI_SHORT = {"S", "S", "R", "K", "J", "S", "M"};

    public static String ymd(int y, int m, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
    }

    public static String todayYmd() {
        Calendar c = Calendar.getInstance();
        return ymd(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    public static String pretty(String ymd) {
        try {
            String[] p = ymd.split("-");
            int d = Integer.parseInt(p[2]);
            int m = Integer.parseInt(p[1]);
            return d + " " + BULAN[m - 1].substring(0, 3) + " " + p[0];
        } catch (Exception e) { return ymd; }
    }

    public static int daysBetween(String from, String to) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long diff = f.parse(to).getTime() - f.parse(from).getTime();
            return (int) (diff / 86400000L) + 1;
        } catch (Exception e) { return 1; }
    }

    public static String hariNama(int dayOfWeek) {
        // Calendar.SUNDAY=1
        String[] n = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        return n[dayOfWeek - 1];
    }
}
