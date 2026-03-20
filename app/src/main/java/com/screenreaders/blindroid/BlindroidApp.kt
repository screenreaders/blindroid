package com.screenreaders.blindroid

import android.app.Application
import android.app.Activity
import android.os.Bundle
import com.screenreaders.blindroid.diagnostics.CrashReporter
import com.screenreaders.blindroid.diagnostics.DiagnosticLog

class BlindroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.init(this)
        DiagnosticLog.log(this, "app_start")
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedCount = 0

            override fun onActivityStarted(activity: Activity) {
                startedCount += 1
                CrashReporter.setAppInForeground(startedCount > 0)
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount -= 1
                if (startedCount < 0) startedCount = 0
                CrashReporter.setAppInForeground(startedCount > 0)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
