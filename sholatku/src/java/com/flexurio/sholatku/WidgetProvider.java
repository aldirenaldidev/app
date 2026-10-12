package com.flexurio.sholatku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/** Widget home: sholat berikutnya + hitung mundur. Refresh tiap 1 menit. */
public class WidgetProvider extends AppWidgetProvider {

    private static final String ACTION_TICK = "com.flexurio.sholatku.WIDGET_TICK";
    private static final String[] ORDER = {"Subuh", "Terbit", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        updateAll(ctx, awm, ids);
        scheduleTick(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String a = intent.getAction();
        if (ACTION_TICK.equals(a) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(a)) {
            AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
            int[] ids = awm.getAppWidgetIds(new ComponentName(ctx, WidgetProvider.class));
            if (ids != null && ids.length > 0) updateAll(ctx, awm, ids);
        }
    }

    @Override
    public void onEnabled(Context ctx) {
        scheduleTick(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(tickIntent(ctx));
        } catch (Throwable ignored) { }
    }

    private void scheduleTick(Context ctx) {
        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = tickIntent(ctx);
            // Refresh tiap 1 menit agar hitung mundur terlihat hidup
            long interval = 60000L;
            long first = SystemClock.elapsedRealtime() + interval;
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, first, interval, pi);
        } catch (Throwable ignored) { }
    }

    private PendingIntent tickIntent(Context ctx) {
        Intent i = new Intent(ctx, WidgetProvider.class);
        i.setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(ctx, 7777, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateAll(Context ctx, AppWidgetManager awm, int[] ids) {
        try {
            SharedPreferences p = ctx.getSharedPreferences("sholatku_widget", Context.MODE_PRIVATE);
            String city = p.getString("city", "Jakarta");
            String json = p.getString("timings", null);
            String hijri = p.getString("hijri", "");

            String nextName = "—", nextTime = "--:--", countdown = "";
            if (json != null) {
                JSONObject t = new JSONObject(json);
                long now = System.currentTimeMillis();
                Calendar c = Calendar.getInstance();
                for (String n : ORDER) {
                    if (n.equals("Terbit")) continue;
                    String hm = t.optString(n, null);
                    if (hm == null) continue;
                    String[] s = hm.split("\\.");
                    Calendar pc = (Calendar) c.clone();
                    pc.set(Calendar.HOUR_OF_DAY, Integer.parseInt(s[0]));
                    pc.set(Calendar.MINUTE, Integer.parseInt(s[1]));
                    pc.set(Calendar.SECOND, 0);
                    if (pc.getTimeInMillis() > now) {
                        nextName = n;
                        nextTime = String.format(Locale.US, "%02d:%02d", Integer.parseInt(s[0]), Integer.parseInt(s[1]));
                        long diff = pc.getTimeInMillis() - now;
                        countdown = String.format(Locale.US, "%02d:%02d:%02d",
                                diff / 3600000, (diff / 60000) % 60, (diff / 1000) % 60);
                        break;
                    }
                }
                if ("—".equals(nextName)) {
                    nextName = "Subuh";
                    String hm = t.optString("Subuh", "04.30");
                    String[] s = hm.split("\\.");
                    nextTime = String.format(Locale.US, "%02d:%02d", Integer.parseInt(s[0]), Integer.parseInt(s[1]));
                    countdown = "Besok";
                }
            }
            String date = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id")).format(Calendar.getInstance().getTime());

            for (int id : ids) {
                RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget_sholatku);
                v.setTextViewText(R.id.w_city, city);
                v.setTextViewText(R.id.w_date, date + (hijri.isEmpty() ? "" : " • " + hijri));
                v.setTextViewText(R.id.w_next, nextName);
                v.setTextViewText(R.id.w_time, nextTime);
                v.setTextViewText(R.id.w_count, countdown.isEmpty() ? "" : "Hitung mundur " + countdown);
                Intent open = new Intent(ctx, MainActivity.class);
                PendingIntent pi = PendingIntent.getActivity(ctx, id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                v.setOnClickPendingIntent(R.id.w_root, pi);
                awm.updateAppWidget(id, v);
            }
        } catch (Throwable ignored) { }
    }
}
