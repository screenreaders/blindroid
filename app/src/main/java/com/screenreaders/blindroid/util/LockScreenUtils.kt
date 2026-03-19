package com.screenreaders.blindroid.util

import android.app.KeyguardManager
import android.content.Context
import android.os.Build

object LockScreenUtils {
    fun isDeviceLocked(context: Context): Boolean {
        val km = context.getSystemService(KeyguardManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km.isDeviceLocked
        } else {
            km.isKeyguardLocked
        }
    }
}
