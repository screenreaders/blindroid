package com.screenreaders.blindroid.accessibility

import android.content.Context
import android.provider.Settings

object TalkbackUtils {
    private val TALKBACK_PACKAGES = listOf(
        "com.google.android.marvin.talkback",
        "com.android.talkback"
    )

    fun isTalkBackEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return TALKBACK_PACKAGES.any { enabled.contains(it, ignoreCase = true) }
    }
}
