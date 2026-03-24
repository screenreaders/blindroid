package com.screenreaders.blindroid.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.LruCache
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog
import com.screenreaders.blindroid.util.LockScreenUtils
import com.screenreaders.blindroid.util.QuietHours

@Suppress("DEPRECATION")
class BlindroidNotificationListener : NotificationListenerService() {
    private lateinit var announcer: CallAnnouncer
    private val appNameCache = LruCache<String, String>(50)

    override fun onCreate() {
        super.onCreate()
        announcer = CallAnnouncer(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        announcer.shutdown()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Prefs.isNotificationsReadEnabled(this)) return
        if (QuietHours.isActive(this) && Prefs.isQuietMuteNotifications(this)) return
        val locked = LockScreenUtils.isDeviceLocked(this)
        if (!locked && !Prefs.isReadWhenUnlockedEnabled(this)) return
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        DiagnosticLog.log(this, "notification_posted pkg=${sbn.packageName}")

        val activeCall = CallManager.getCall()
        if (activeCall != null && activeCall.state != android.telecom.Call.STATE_DISCONNECTED) return

        val privacy = Prefs.isPrivacyModeEnabled(this)
        val privacyTitleOnly = Prefs.isPrivacyTitleOnlyEnabled(this)
        val message = if (privacy) {
            if (privacyTitleOnly) {
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                if (title.isNotBlank()) {
                    title
                } else {
                    val appName = getAppName(sbn.packageName)
                    "Powiadomienie od $appName"
                }
            } else {
                val appName = getAppName(sbn.packageName)
                "Powiadomienie od $appName"
            }
        } else {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

            if (title.isBlank() && text.isBlank()) return
            when {
                title.isNotBlank() && text.isNotBlank() -> "$title. $text"
                title.isNotBlank() -> title
                else -> text
            }
        }

        Prefs.addRecentNotification(this, message)
        val useSeparate = Prefs.isNotificationTtsSeparateEnabled(this)
        val rate = if (useSeparate) Prefs.getNotificationSpeechRate(this) else Prefs.getSpeechRate(this)
        val volume =
            if (useSeparate) Prefs.getNotificationSpeechVolume(this) else Prefs.getSpeechVolume(this)
        val voiceName =
            if (useSeparate) Prefs.getNotificationVoiceName(this) else Prefs.getVoiceName(this)
        announcer.speak(
            text = message,
            repeatCount = 1,
            rate = rate,
            volume = volume,
            voiceName = voiceName
        )
    }

    private fun getAppName(packageName: String): String {
        appNameCache.get(packageName)?.let { return it }
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString().also {
                appNameCache.put(packageName, it)
            }
        } catch (_: Exception) {
            packageName
        }
    }
}
