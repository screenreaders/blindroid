package com.screenreaders.blindroid.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenreaders.blindroid.data.Prefs

class ScheduledActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ScheduledActionScheduler.ACTION_RUN) return
        if (!Prefs.isScheduledActionEnabled(context)) {
            ScheduledActionScheduler.cancel(context)
            return
        }
        val target = Prefs.getScheduledActionTarget(context)
        val launchIntent = ScheduledActionScheduler.buildIntentForTarget(context, target)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
        ScheduledActionScheduler.schedule(context)
    }
}
