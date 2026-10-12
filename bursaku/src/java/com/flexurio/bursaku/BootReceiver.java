package com.flexurio.bursaku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Jadwalkan ulang alarm alert setelah HP restart. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AlertScheduler.schedule(ctx);
        }
    }
}
