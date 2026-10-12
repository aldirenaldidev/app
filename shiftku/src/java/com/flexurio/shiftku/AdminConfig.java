package com.flexurio.shiftku;

/**
 * Konfigurasi akun Admin default ShiftKu.
 *
 * ============================================================
 *  PENTING — KEAMANAN
 * ============================================================
 * Email & password di bawah ini adalah KREDENSIAL AWAL yang dibuat
 * otomatis saat aplikasi pertama kali dibuka (lihat
 * FirebaseBridge.ensureDefaultAdmin).
 *
 * Saat admin login PERTAMA KALI dengan kredensial ini, aplikasi
 * WAJIBKAN ganti password (ChangePasswordActivity, tidak bisa di-skip).
 *
 * Setelah password diganti, nilai ADMIN_DEFAULT_PASSWORD di bawah
 * ini TIDAK LAGI BERLAKU — yang berlaku adalah password baru yang
 * disimpan di Firebase Authentication.
 *
 * Disarankan: setelah instalasi & login pertama berhasil, ganti juga
 * password di sini agar build berikutnya tidak memakai password lama
 * sebagai default, atau hapus mekanisme seeding bila tidak dibutuhkan.
 * ============================================================
 */
public final class AdminConfig {

    private AdminConfig() {}

    /** Email akun admin default. */
    public static final String ADMIN_EMAIL = "admin@shiftku.app";

    /**
     * Password AWAL admin default. Hanya berlaku sampai admin mengganti
     * password saat login pertama. GANTI dengan password yang kuat bila
     * membangun ulang aplikasi untuk distribusi baru.
     */
    public static final String ADMIN_DEFAULT_PASSWORD = "ShiftKu2026!";

    /** Nama tampilan akun admin default. */
    public static final String ADMIN_DISPLAY_NAME = "Administrator";

    /** Koleksi Firestore untuk data akun global (di luar grup). */
    public static final String USERS_COLLECTION = "users";

    /** Field penanda "wajib ganti password" pada dokumen users/{uid}. */
    public static final String FIELD_MUST_CHANGE_PASSWORD = "mustChangePassword";
}
