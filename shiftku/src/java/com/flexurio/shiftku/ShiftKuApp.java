package com.flexurio.shiftku;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Application kustom ShiftKu.
 *
 * Alasan: FirebaseInitProvider (auto-init Firebase sebelum Application.onCreate)
 * dihapus dari manifest karena menjadi penyebab crash "keeps stopping" saat
 * dibuka di HP user — provider berjalan di luar semua try/catch aplikasi.
 * Firebase sekarang di-init manual di sini, terbungkus try/catch(Throwable).
 *
 * Juga memasang UncaughtExceptionHandler global yang menyimpan stack trace
 * ke file, sehingga crash berikutnya bisa didiagnosis (ditampilkan di
 * MainActivity sebagai dialog).
 */
public class ShiftKuApp extends Application {
    private static final String TAG = "ShiftKuApp";
    public static final String CRASH_LOG = "last_crash.log";

    @Override public void onCreate() {
        try {
            superOnCreate();
        } catch (Throwable t) {
            // jangan biarkan Application.onCreate meledak tanpa jejak
            try { saveCrash("APP_ONCREATE", t); } catch (Throwable ignored) {}
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }

    private void superOnCreate() {
        super.onCreate();
        installCrashHandler();
        // Firebase manual init (pengganti FirebaseInitProvider).
        //
        // DUA LAPIS PERTAHANAN:
        // 1. FirebaseOptions.Builder() dengan nilai langsung dari
        //    google-services.json (via FirebaseConfig.java yang di-generate
        //    saat build) — TIDAK membaca R.string sama sekali. Ini menghindari
        //    crash v1.1.1: StringResourceValueReader.<init> melempar
        //    Resources$NotFoundException karena konstanta R.string library
        //    ter-generate sebagai 0x0 (bug gen_lib_r.py, sudah diperbaiki).
        // 2. Fallback: initializeApp(context) biasa bila builder gagal.
        try {
            initFirebaseManual();
            Log.i(TAG, "FirebaseApp manual init OK");
        } catch (Throwable t) {
            Log.e(TAG, "Firebase manual builder gagal, coba fallback: " + t);
            try {
                com.google.firebase.FirebaseApp.initializeApp(getApplicationContext());
                Log.i(TAG, "FirebaseApp fallback init OK");
            } catch (Throwable t2) {
                Log.e(TAG, "Firebase init gagal total (non-fatal): " + t2);
                try { saveCrash("FIREBASE_INIT", t2); } catch (Throwable ignored) {}
            }
        }
    }

    /** Init Firebase tanpa ketergantungan pada resource ID. */
    private void initFirebaseManual() {
        android.content.Context ctx = getApplicationContext();
        // Jangan init dua kali (mis. setelah process restart).
        try {
            if (com.google.firebase.FirebaseApp.getApps(ctx).size() > 0) {
                Log.i(TAG, "FirebaseApp sudah ada, lewati init manual");
                return;
            }
        } catch (Throwable ignored) {}
        com.google.firebase.FirebaseOptions opts =
                new com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId(FirebaseConfig.APP_ID)
                        .setGcmSenderId(FirebaseConfig.SENDER_ID)
                        .setApiKey(FirebaseConfig.API_KEY)
                        .setProjectId(FirebaseConfig.PROJECT_ID)
                        .setStorageBucket(FirebaseConfig.STORAGE_BUCKET)
                        .build();
        com.google.firebase.FirebaseApp.initializeApp(ctx, opts);
    }

    private void installCrashHandler() {
        final Thread.UncaughtExceptionHandler def =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable t) {
                try { saveCrash("UNCAUGHT", t); } catch (Throwable ignored) {}
                if (def != null) {
                    try { def.uncaughtException(th, t); }
                    catch (Throwable ignored) {}
                }
            }
        });
    }

    private void saveCrash(String kind, Throwable t) {
        PrintWriter pw = null;
        try {
            File f = new File(getFilesDir(), CRASH_LOG);
            pw = new PrintWriter(new FileWriter(f, false));
            pw.println("kind=" + kind);
            pw.println("time=" + System.currentTimeMillis());
            t.printStackTrace(pw);
            pw.flush();
            Log.e(TAG, "crash log tersimpan: " + f.getAbsolutePath());
        } catch (Throwable ignored) {
        } finally {
            if (pw != null) { try { pw.close(); } catch (Throwable ignored) {} }
        }
    }

    /** Baca + hapus log crash terakhir. Null bila tidak ada. */
    public static String consumeCrashLog(Application app) {
        BufferedReader br = null;
        try {
            File f = new File(app.getFilesDir(), CRASH_LOG);
            if (!f.exists()) return null;
            StringBuilder sb = new StringBuilder();
            br = new BufferedReader(new FileReader(f));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
                if (sb.length() > 6000) { sb.append("... (dipotong)"); break; }
            }
            try { f.delete(); } catch (Throwable ignored) {}
            return sb.length() == 0 ? null : sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            if (br != null) { try { br.close(); } catch (Throwable ignored) {} }
        }
    }
}
