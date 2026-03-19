package com.screenreaders.blindroid.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.screenreaders.blindroid.data.Prefs
import java.util.Calendar

object ChimeScheduler {
    private const val REQUEST_CODE = 2001

    fun schedule(context: Context) {
        if (!Prefs.isChimeEnabled(context)) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intervalMinutes = Prefs.getChimeInterval(context)
        val intervalMillis = intervalMinutes * 60_000L
        val triggerAt = computeNextTrigger(intervalMinutes)
        val pendingIntent = buildIntent(context)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(buildIntent(context))
    }

    private fun buildIntent(context: Context): PendingIntent {
        val intent = Intent(context, ChimeReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTrigger(intervalMinutes: Int): Long {
        val cal = Calendar.getInstance()
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val millis = cal.get(Calendar.MILLISECOND)

        val remainder = minute % intervalMinutes
        var addMinutes = if (remainder == 0) 0 else intervalMinutes - remainder
        if (addMinutes == 0 && (second > 0 || millis > 0)) {
            addMinutes = intervalMinutes
        }

        cal.add(Calendar.MINUTE, addMinutes)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
