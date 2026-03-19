package com.screenreaders.blindroid.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        val start = Prefs.getChimeStartMinutes(context)
        val end = Prefs.getChimeEndMinutes(context)
        val triggerAt = computeNextTrigger(intervalMinutes, start, end)
        val pendingIntent = buildIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            // Fallback to inexact if exact alarms are not allowed.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
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

    private fun computeNextTrigger(
        intervalMinutes: Int,
        startMinutes: Int,
        endMinutes: Int
    ): Long {
        val allowed = when (intervalMinutes) {
            60 -> setOf(0)
            30 -> setOf(0, 30)
            else -> setOf(0, 15, 30, 45)
        }

        val now = Calendar.getInstance()
        val nowSecond = now.get(Calendar.SECOND)
        val nowMillis = now.get(Calendar.MILLISECOND)

        val candidate = now.clone() as Calendar
        var firstAllowed: Long? = null
        for (i in 0..(24 * 60)) {
            candidate.timeInMillis = now.timeInMillis
            candidate.add(Calendar.MINUTE, i)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)

            if (i == 0 && (nowSecond > 0 || nowMillis > 0)) {
                continue
            }
            val minute = candidate.get(Calendar.MINUTE)
            if (!allowed.contains(minute)) continue

            if (firstAllowed == null) {
                firstAllowed = candidate.timeInMillis
            }

            val minutesOfDay =
                candidate.get(Calendar.HOUR_OF_DAY) * 60 + candidate.get(Calendar.MINUTE)
            if (isWithinWindow(minutesOfDay, startMinutes, endMinutes)) {
                return candidate.timeInMillis
            }
        }
        if (firstAllowed != null) {
            return firstAllowed
        }
        // Fallback: next minute if nothing matched (should not happen)
        candidate.timeInMillis = now.timeInMillis
        candidate.add(Calendar.MINUTE, 1)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)
        return candidate.timeInMillis
    }

    private fun isWithinWindow(now: Int, start: Int, end: Int): Boolean {
        return if (start == end) {
            true
        } else if (start < end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }
}
