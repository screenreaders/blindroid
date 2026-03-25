package com.screenreaders.blindroid.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduledActionBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScheduledActionScheduler.schedule(context)
        }
    }
}
