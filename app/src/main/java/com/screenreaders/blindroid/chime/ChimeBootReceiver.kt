package com.screenreaders.blindroid.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenreaders.blindroid.data.Prefs

class ChimeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (Prefs.isChimeEnabled(context)) {
            ChimeScheduler.schedule(context)
        }
    }
}
