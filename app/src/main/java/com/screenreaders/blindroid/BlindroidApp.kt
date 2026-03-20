package com.screenreaders.blindroid

import android.app.Application
import com.screenreaders.blindroid.diagnostics.CrashReporter

class BlindroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.init(this)
    }
}
