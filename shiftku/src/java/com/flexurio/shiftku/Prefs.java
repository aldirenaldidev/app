package com.flexurio.shiftku;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferensi sederhana. */
public class Prefs {
    private static final String N = "shiftku_prefs";

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(N, Context.MODE_PRIVATE);
    }

    public static int getLeaveQuota(Context c) {
        return p(c).getInt("leave_quota", 12);
    }

    public static void setLeaveQuota(Context c, int v) {
        p(c).edit().putInt("leave_quota", v).apply();
    }

    public static String getUserName(Context c) {
        return p(c).getString("user_name", "");
    }

    public static void setUserName(Context c, String v) {
        p(c).edit().putString("user_name", v).apply();
    }

    public static int getLeaveYear(Context c) {
        return p(c).getInt("leave_year", 2026);
    }

    public static void setLeaveYear(Context c, int y) {
        p(c).edit().putInt("leave_year", y).apply();
    }

    /** Role global user: "user" (default), "leader", atau "admin". */
    public static String getUserRole(Context c) {
        return p(c).getString("user_role", FirebaseBridge.ROLE_USER);
    }

    public static void setUserRole(Context c, String v) {
        p(c).edit().putString("user_role", v).apply();
    }

    /** Penanda: akun admin default sudah di-seed ke Firebase Auth. */
    public static boolean isAdminSeeded(Context c) {
        return p(c).getBoolean("admin_seeded", false);
    }

    public static void setAdminSeeded(Context c, boolean v) {
        p(c).edit().putBoolean("admin_seeded", v).apply();
    }

    /** Pengingat shift: aktif/tidak. */
    public static boolean isShiftReminderOn(Context c) {
        return p(c).getBoolean("shift_reminder_on", true);
    }

    public static void setShiftReminderOn(Context c, boolean v) {
        p(c).edit().putBoolean("shift_reminder_on", v).apply();
    }

    /** Berapa menit sebelum shift mulai notifikasi dikirim (default 60). */
    public static int getShiftReminderMinutes(Context c) {
        return p(c).getInt("shift_reminder_min", 60);
    }

    public static void setShiftReminderMinutes(Context c, int v) {
        p(c).edit().putInt("shift_reminder_min", v).apply();
    }
}
