package com.screenreaders.blindroid.util

import android.content.Context
import com.screenreaders.blindroid.data.Prefs
import java.util.Calendar

object QuietHours {
    fun isActive(context: Context, nowMinutes: Int = currentMinutes()): Boolean {
        if (!Prefs.isQuietEnabled(context)) return false
        val start = Prefs.getQuietStartMinutes(context)
        val end = Prefs.getQuietEndMinutes(context)
        return if (start == end) {
            true
        } else if (start < end) {
            nowMinutes in start until end
        } else {
            nowMinutes >= start || nowMinutes < end
        }
    }

    private fun currentMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }
}
