package com.flexurio.shiftku;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget: shift hari ini + jam. */
public class ShiftWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager awm, int[] ids) {
        update(c, awm, ids);
    }

    public static void refreshAll(Context c) {
        Ui.safe("widget", new Runnable() {
            public void run() {
                AppWidgetManager awm = AppWidgetManager.getInstance(c);
                int[] ids = awm.getAppWidgetIds(
                        new ComponentName(c, ShiftWidget.class));
                if (ids != null && ids.length > 0) update(c, awm, ids);
            }
        });
    }

    private static void update(Context c, AppWidgetManager awm, int[] ids) {
        DbHelper db = new DbHelper(c);
        String today = DateUtil.todayYmd();
        long sid = db.getScheduleShift(today);
        String label = "-";
        String jam = "";
        if (sid > 0) {
            Shift s = db.getShift(sid);
            if (s != null) { label = s.name; jam = s.jam(); }
        }
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget_shift);
            rv.setTextViewText(R.id.w_shift, label);
            rv.setTextViewText(R.id.w_time, jam);
            Intent open = new Intent(c, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.w_shift, pi);
            awm.updateAppWidget(id, rv);
        }
    }
}
