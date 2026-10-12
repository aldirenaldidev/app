package com.flexurio.dompetku;

/** Satu baris transaksi. Top-level agar aman untuk d8. */
public class Tx {
    public long id;
    public String tipe;      // "in" | "out"
    public long nominal;
    public String kategori;
    public String tanggal;   // yyyy-MM-dd
    public String catatan;
}
