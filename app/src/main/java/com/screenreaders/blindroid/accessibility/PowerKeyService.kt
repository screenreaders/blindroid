package com.screenreaders.blindroid.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.SystemClock
import android.view.KeyEvent
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog

class PowerKeyService : AccessibilityService() {
    private var lastPowerTime = 0L

    override fun onServiceConnected() {
        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (Prefs.getEndCallKey(this) != Prefs.END_CALL_POWER) return false
        if (event.keyCode != KeyEvent.KEYCODE_POWER) return false
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val now = SystemClock.uptimeMillis()
        val isDoublePress = now - lastPowerTime <= 800
        lastPowerTime = now
        if (!isDoublePress) return true

        val call = CallManager.getCall() ?: return true
        DiagnosticLog.log(this, "end_call_power")
        call.disconnect()
        lastPowerTime = 0L
        return true
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
