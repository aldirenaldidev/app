package com.flexurio.shiftku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Jadwalkan ulang alarm setelah HP dinyalakan. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            AlarmScheduler.scheduleAll(c);
            ShiftReminder.schedule(c);
        }
    }
}
