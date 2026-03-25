package com.screenreaders.blindroid.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.screenreaders.blindroid.MainActivity
import com.screenreaders.blindroid.currency.CurrencyActivity
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.document.DocumentAssistActivity
import com.screenreaders.blindroid.face.FaceAssistActivity
import com.screenreaders.blindroid.light.LightActivity
import com.screenreaders.blindroid.navigation.NavigationAssistActivity
import com.screenreaders.blindroid.obstacle.ObstacleAssistActivity
import com.screenreaders.blindroid.scan.ScanHubActivity
import java.util.Calendar

object ScheduledActionScheduler {
    const val ACTION_RUN = "com.screenreaders.blindroid.action.SCHEDULED_ACTION"
    const val TARGET_NONE = "none"
    const val TARGET_MAIN = "main"
    const val TARGET_NAVIGATION = "navigation"
    const val TARGET_DOCUMENTS = "documents"
    const val TARGET_FACE = "face"
    const val TARGET_OBSTACLE = "obstacle"
    const val TARGET_CURRENCY = "currency"
    const val TARGET_LIGHT = "light"
    const val TARGET_SCAN = "scan"

    fun schedule(context: Context) {
        val enabled = Prefs.isScheduledActionEnabled(context)
        val target = Prefs.getScheduledActionTarget(context)
        if (!enabled || target.isNullOrBlank() || target == TARGET_NONE) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val timeMinutes = Prefs.getScheduledActionTime(context)
        val hour = (timeMinutes / 60) % 24
        val minute = timeMinutes % 60
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val pendingIntent = buildPendingIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                trigger.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(buildPendingIntent(context))
    }

    fun buildIntentForTarget(context: Context, target: String?): Intent? {
        return when (target) {
            TARGET_MAIN -> Intent(context, MainActivity::class.java)
            TARGET_NAVIGATION -> Intent(context, NavigationAssistActivity::class.java)
            TARGET_DOCUMENTS -> Intent(context, DocumentAssistActivity::class.java)
            TARGET_FACE -> Intent(context, FaceAssistActivity::class.java)
            TARGET_OBSTACLE -> Intent(context, ObstacleAssistActivity::class.java)
            TARGET_CURRENCY -> Intent(context, CurrencyActivity::class.java)
            TARGET_LIGHT -> Intent(context, LightActivity::class.java)
            TARGET_SCAN -> Intent(context, ScanHubActivity::class.java)
            else -> null
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ScheduledActionReceiver::class.java).setAction(ACTION_RUN)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}
