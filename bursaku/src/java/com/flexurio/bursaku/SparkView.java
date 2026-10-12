package com.flexurio.bursaku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/** Mini sparkline untuk kartu indeks. */
public class SparkView extends View {
    private double[] data;
    private boolean up = true;
    private Integer forceColor = null;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SparkView(Context c) { super(c); }
    public SparkView(Context c, AttributeSet a) { super(c, a); }

    public void setData(double[] d) {
        data = d;
        if (d != null && d.length >= 2) up = d[d.length - 1] >= d[0];
        invalidate();
    }

    /** Paksa warna garis tertentu (mis. oranye untuk hero). null = otomatis hijau/merah. */
    public void setLineColor(Integer color) {
        forceColor = color;
        invalidate();
    }

    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        if (data == null || data.length < 2) return;
        double min = data[0], max = data[0];
        for (double v : data) { if (v < min) min = v; if (v > max) max = v; }
        if (max == min) max = min + 1;
        float w = getWidth(), h = getHeight();
        float pad = 6f;
        Path path = new Path();
        for (int i = 0; i < data.length; i++) {
            float x = pad + (w - 2 * pad) * i / (data.length - 1);
            float y = pad + (h - 2 * pad) * (1 - (float) ((data[i] - min) / (max - min)));
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        int col = forceColor != null ? forceColor : (up ? 0xFF16C784 : 0xFFEA3943);
        paint.setColor(col);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        cv.drawPath(path, paint);
        // isi area samar di bawah garis
        Path area = new Path(path);
        area.lineTo(w - pad, h);
        area.lineTo(pad, h);
        area.close();
        fill.setColor(col & 0x33FFFFFF);
        fill.setStyle(Paint.Style.FILL);
        cv.drawPath(area, fill);
    }
}
