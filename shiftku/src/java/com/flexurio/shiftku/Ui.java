package com.flexurio.shiftku;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Helper UI: tema terang personal + tema gelap grup. */
public class Ui {
    public static final int PRIMARY = 0xFF5B5BF0;
    public static final int PRIMARY_DARK = 0xFF4845D6;
    public static final int BG = 0xFFF7F8FC;
    public static final int CARD = 0xFFFFFFFF;
    public static final int TEXT = 0xFF1A1D29;
    public static final int TEXT_DIM = 0xFF8A8FA3;
    public static final int DIVIDER = 0xFFE8EAF2;

    public static final int DARK_BG = 0xFF0D1424;
    public static final int DARK_CARD = 0xFF182238;
    public static final int DARK_ACCENT = 0xFF3B82F6;
    public static final int DARK_TEXT = 0xFFFFFFFF;
    public static final int DARK_DIM = 0xFF8A94B0;

    public static final int GREEN = 0xFF22C55E;
    public static final int RED = 0xFFEF4444;
    public static final int AMBER = 0xFFF59E0B;

    public static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    public static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    public static LinearLayout card(Context c) {
        return card(c, false);
    }

    public static LinearLayout card(Context c, boolean dark) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(dark ? DARK_CARD : CARD);
        gd.setCornerRadius(dp(c, 14));
        l.setBackground(gd);
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(c, 10);
        l.setLayoutParams(lp);
        return l;
    }

    public static GradientDrawable roundBg(int color, int radiusDp, Context c) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(c, radiusDp));
        return gd;
    }

    public static View badge(Context c, String letter, int color, int sizeDp) {
        TextView t = new TextView(c);
        t.setText(letter);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeDp / 3);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        t.setBackground(gd);
        int s = dp(c, sizeDp);
        t.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return t;
    }

    public static ImageView icon(Context c, int res, int color, int sizeDp) {
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        if (color != 0) iv.setColorFilter(color);
        int s = dp(c, sizeDp);
        iv.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return iv;
    }

    public static TextView primaryButton(Context c, String label) {
        TextView b = text(c, label, 16, 0xFFFFFFFF, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(roundBg(PRIMARY, 12, c));
        b.setPadding(0, dp(c, 14), 0, dp(c, 14));
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        b.setLayoutParams(lp);
        return b;
    }

    public static TextView outlineButton(Context c, String label) {
        TextView b = text(c, label, 15, PRIMARY, true);
        b.setGravity(Gravity.CENTER);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0x00000000);
        gd.setCornerRadius(dp(c, 12));
        gd.setStroke(dp(c, 1), PRIMARY);
        b.setBackground(gd);
        b.setPadding(0, dp(c, 13), 0, dp(c, 13));
        b.setClickable(true);
        b.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        b.setLayoutParams(lp);
        return b;
    }

    /** Header ungu dengan tombol kembali di kiri (standar Android). */
    public static LinearLayout header(Context c, android.app.Activity act, String title) {
        LinearLayout h = new LinearLayout(c);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(PRIMARY);
        h.setPadding(dp(c, 8), dp(c, 14), dp(c, 16), dp(c, 14));
        ImageView back = icon(c, R.drawable.ic_back, 0xFFFFFFFF, 40);
        int p = dp(c, 8);
        back.setPadding(p, p, p, p);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.finish(); }
        });
        h.addView(back);
        TextView t = text(c, title, 20, 0xFFFFFFFF, true);
        t.setPadding(dp(c, 8), 0, 0, 0);
        h.addView(t);
        return h;
    }

    /** Header gelap untuk layar grup. */
    public static LinearLayout darkHeader(Context c, android.app.Activity act, String title, String sub) {
        LinearLayout wrap = new LinearLayout(c);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setBackgroundColor(DARK_BG);
        wrap.setPadding(dp(c, 16), dp(c, 18), dp(c, 16), dp(c, 12));
        LinearLayout h = new LinearLayout(c);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = icon(c, R.drawable.ic_back, DARK_TEXT, 40);
        int p = dp(c, 8);
        back.setPadding(p, p, p, p);
        back.setClickable(true);
        back.setFocusable(true);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { act.finish(); }
        });
        h.addView(back);
        TextView t = text(c, title, 22, DARK_TEXT, true);
        t.setPadding(dp(c, 8), 0, 0, 0);
        h.addView(t);
        wrap.addView(h);
        if (sub != null) {
            TextView s = text(c, sub, 13, DARK_DIM, false);
            s.setPadding(dp(c, 56), dp(c, 2), 0, 0);
            wrap.addView(s);
        }
        return wrap;
    }

    public static void openInstagram(Context c) {
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://instagram.com/aldire__")));
        } catch (Exception ignored) {}
    }

    /** Bungkus aman: satu section gagal tidak menggugurkan layar.
     *  Catch Throwable (bukan cuma Exception) agar Error seperti
     *  NoClassDefFoundError / VerifyError juga tidak meledakkan aplikasi. */
    public static void safe(String tag, Runnable r) {
        try { r.run(); }
        catch (Throwable t) { android.util.Log.e("ShiftKu", tag + ": " + t); }
    }
}
