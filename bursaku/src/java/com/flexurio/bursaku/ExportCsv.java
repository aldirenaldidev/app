package com.flexurio.bursaku;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tulis file CSV ke folder Downloads.
 * Android 10+ (API 29): via MediaStore — TIDAK butuh izin tulis.
 * Di bawah itu: fallback ke getExternalFilesDir (area aplikasi).
 * Ditambah BOM agar rapi dibuka di Excel.
 */
public class ExportCsv {

    /** Nama file dengan cap waktu WIB, mis. bursaku-top-dividen-20261008.csv */
    public static String fileName(String prefix, String ext) {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        return fileName(prefix, ext, f.format(new Date()));
    }

    /** Varian murni untuk unit test. */
    static String fileName(String prefix, String ext, String yyyymmdd) {
        return prefix + yyyymmdd + "." + ext;
    }

    /** Nama file portofolio dengan jam: bursaku-portofolio-YYYYMMDD-HHMM.csv */
    public static String portfolioFileName() {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        return "bursaku-portofolio-" + f.format(new Date()) + ".csv";
    }

    /** Satu baris CSV dengan escaping standar (kutip digandakan). Murni. */
    public static String csvRow(String... cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            String c = cols[i] == null ? "" : cols[i];
            boolean need = c.contains(",") || c.contains("\"") || c.contains("\n");
            if (need) sb.append('"').append(c.replace("\"", "\"\"")).append('"');
            else sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Tulis CSV. header = baris judul (sudah diformat), rows = baris data.
     * Kembalikan nama file yang tersimpan.
     */
    public static String write(Context ctx, String fileName, String header,
                               List<String> rows) throws Exception {
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM untuk Excel
        sb.append(header).append("\r\n");
        for (String r : rows) sb.append(r).append("\r\n");
        byte[] bytes = sb.toString().getBytes("UTF-8");

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            v.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver().insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new Exception("MediaStore menolak");
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            if (os == null) throw new Exception("Tidak bisa membuka stream");
            os.write(bytes);
            os.close();
        } else {
            File dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (dir == null) throw new Exception("Penyimpanan tak tersedia");
            File f = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
        }
        return fileName;
    }
}
