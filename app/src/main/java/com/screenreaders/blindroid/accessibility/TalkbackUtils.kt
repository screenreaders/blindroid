package com.screenreaders.blindroid.accessibility

import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.provider.Settings

object TalkbackUtils {
    private val TALKBACK_PACKAGES = listOf(
        "com.google.android.marvin.talkback",
        "com.android.talkback"
    )
    private const val BACKUP_TALKBACK_PACKAGE = "com.screenreaders.blindreader"
    private const val BACKUP_TALKBACK_SERVICE = "com.google.android.accessibility.talkback.TalkBackService"

    fun isTalkBackEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return TALKBACK_PACKAGES.any { enabled.contains(it, ignoreCase = true) }
    }

    fun isBackupTalkBackInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(BACKUP_TALKBACK_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isBackupTalkBackEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val component = ComponentName(BACKUP_TALKBACK_PACKAGE, BACKUP_TALKBACK_SERVICE).flattenToString()
        return enabled.contains(component, ignoreCase = true)
    }

    fun getBackupTalkBackComponent(): ComponentName {
        return ComponentName(BACKUP_TALKBACK_PACKAGE, BACKUP_TALKBACK_SERVICE)
    }
}
