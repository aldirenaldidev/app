package com.flexurio.bursaku;

/**
 * Konstituen LQ45 periode 3 Agustus – 30 Oktober 2026
 * (hasil evaluasi mayor BEI; sumber: Wikipedia "Indeks LQ45" & Ajaib).
 */
public class Lq45 {
    public static final String[] CODES = {
        "AADI", "ADMR", "ADRO", "AKRA", "AMMN", "AMRT", "ANTM", "ASII",
        "BBCA", "BBNI", "BBRI", "BBTN", "BMRI", "BRPT", "BUMI", "CPIN",
        "CUAN", "DEWA", "EMTK", "ESSA", "EXCL", "GOTO", "HRTA", "ICBP",
        "INCO", "INDF", "INDY", "INKP", "ISAT", "ITMG", "JPFA", "KLBF",
        "MAPI", "MBMA", "MDKA", "MEDC", "NCKL", "PGAS", "PGEO", "PTBA",
        "SCMA", "TLKM", "UNTR", "UNVR", "WIFI"
    };
    public static final String[] NAMES = {
        "Adaro Andalan Indonesia", "Alamtri Minerals Indonesia", "Alamtri Resources Indonesia",
        "AKR Corporindo", "Amman Mineral Internasional", "Sumber Alfaria Trijaya",
        "Aneka Tambang", "Astra International",
        "Bank Central Asia", "Bank Negara Indonesia", "Bank Rakyat Indonesia", "Bank Tabungan Negara",
        "Bank Mandiri", "Barito Pacific", "Bumi Resources", "Charoen Pokphand Indonesia",
        "Petrindo Jaya Kreasi", "Darma Henwa", "Elang Mahkota Teknologi", "ESSA Industries Indonesia",
        "XLSMART Telecom", "GoTo Gojek Tokopedia", "Hartadinata Abadi", "Indofood CBP Sukses Makmur",
        "Vale Indonesia", "Indofood Sukses Makmur", "Indika Energy", "Indah Kiat Pulp & Paper",
        "Indosat", "Indo Tambangraya Megah", "Japfa Comfeed Indonesia", "Kalbe Farma",
        "Mitra Adiperkasa", "Merdeka Battery Materials", "Merdeka Copper Gold", "Medco Energi Internasional",
        "Trimegah Bangun Persada", "Perusahaan Gas Negara", "Pertamina Geothermal Energy", "Bukit Asam",
        "Surya Citra Media", "Telkom Indonesia", "United Tractors", "Unilever Indonesia",
        "Solusi Sinergi Digital"
    };

    /** Nama perusahaan untuk kode (tanpa .JK); "" bila tidak dikenal. */
    public static String nameOf(String code) {
        String c = code.replace(".JK", "").toUpperCase();
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(c)) return NAMES[i];
        }
        return "";
    }
}
