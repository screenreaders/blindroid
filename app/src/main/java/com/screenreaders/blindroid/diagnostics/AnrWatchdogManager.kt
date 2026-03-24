package com.screenreaders.blindroid.diagnostics

import android.content.Context

object AnrWatchdogManager {
    @Volatile private var watchdog: AnrWatchdog? = null

    fun ensureStarted(context: Context) {
        if (watchdog?.isAlive == true) return
        val appContext = context.applicationContext
        watchdog = AnrWatchdog(appContext).also { it.start() }
    }

    fun stop() {
        watchdog?.interrupt()
        watchdog = null
    }
}
