package com.flexurio.bursaku;

/**
 * Pemetaan ticker emiten -> drawable logo resmi (res/drawable-nodpi/logo_*.png).
 * Return 0 bila logo tidak tersedia (pemanggil pakai badge inisial sebagai fallback).
 */
public class LogoUtil {
    public static int resId(String symbol) {
        if (symbol == null) return 0;
        String s = symbol.toUpperCase().replace(".JK", "").trim();
        if (s.equals("BBCA")) return R.drawable.logo_bbca;
        if (s.equals("BBRI")) return R.drawable.logo_bbri;
        if (s.equals("BMRI")) return R.drawable.logo_bmri;
        if (s.equals("BBNI")) return R.drawable.logo_bbni;
        if (s.equals("TLKM")) return R.drawable.logo_tlkm;
        if (s.equals("ASII")) return R.drawable.logo_asii;
        if (s.equals("ADRO")) return R.drawable.logo_adro;
        if (s.equals("UNVR")) return R.drawable.logo_unvr;
        if (s.equals("INDF")) return R.drawable.logo_indf;
        if (s.equals("ICBP")) return R.drawable.logo_icbp;
        if (s.equals("KLBF")) return R.drawable.logo_klbf;
        if (s.equals("SMGR")) return R.drawable.logo_smgr;
        if (s.equals("INTP")) return R.drawable.logo_intp;
        if (s.equals("PTBA")) return R.drawable.logo_ptba;
        if (s.equals("ANTM")) return R.drawable.logo_antm;
        if (s.equals("INCO")) return R.drawable.logo_inco;
        if (s.equals("MDKA")) return R.drawable.logo_mdka;
        if (s.equals("BRIS")) return R.drawable.logo_bris;
        if (s.equals("ARTO")) return R.drawable.logo_arto;
        if (s.equals("GOTO")) return R.drawable.logo_goto;
        return 0;
    }

    /** Inisial 2 huruf untuk badge fallback (mis. BBCA -> "BB"). */
    public static String initials(String symbol) {
        if (symbol == null) return "?";
        String s = symbol.toUpperCase().replace(".JK", "").trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < 2; i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) sb.append(c);
        }
        return sb.length() > 0 ? sb.toString() : "?";
    }
}
