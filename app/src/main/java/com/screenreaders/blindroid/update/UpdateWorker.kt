package com.screenreaders.blindroid.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.screenreaders.blindroid.BuildConfig
import com.screenreaders.blindroid.MainActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs

class UpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!Prefs.isAutoUpdateEnabled(applicationContext)) return Result.success()
        val latest = try {
            UpdateChecker.fetchLatest()
        } catch (_: Exception) {
            null
        } ?: return Result.success()

        Prefs.setLastUpdateCheck(applicationContext, System.currentTimeMillis())
        val current = BuildConfig.VERSION_NAME
        if (!UpdateChecker.isNewer(current, latest.version)) return Result.success()
        val lastNotified = Prefs.getLastUpdateNotifiedVersion(applicationContext)
        if (lastNotified == latest.version) return Result.success()

        Prefs.setLastUpdateNotifiedVersion(applicationContext, latest.version)
        showNotification(latest.version)
        return Result.success()
    }

    private fun showNotification(version: String) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        createChannel(nm)
        val intent = MainActivity.createUpdatesIntent(applicationContext, checkNow = true)
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(applicationContext.getString(R.string.update_notification_title))
            .setContentText(applicationContext.getString(R.string.update_notification_text, version))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notif)
    }

    private fun createChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.update_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "blindroid_updates"
        private const val NOTIF_ID = 3101
    }
}
