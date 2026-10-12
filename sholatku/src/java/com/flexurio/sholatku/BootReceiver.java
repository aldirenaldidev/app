package com.flexurio.sholatku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Jadwalkan ulang alarm adzan setelah HP di-restart. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            try {
                AlarmScheduler.rescheduleAll(ctx);
            } catch (Throwable ignored) { }
        }
    }
}
