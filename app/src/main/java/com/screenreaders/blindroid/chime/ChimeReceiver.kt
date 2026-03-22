package com.screenreaders.blindroid.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.util.QuietHours
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean
import android.os.Handler
import android.os.Looper

@Suppress("DEPRECATION")
class ChimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!Prefs.isChimeEnabled(context)) return
        if (QuietHours.isActive(context) && Prefs.isQuietMuteChime(context)) return

        val nowMinutes = currentMinutes()
        val start = Prefs.getChimeStartMinutes(context)
        val end = Prefs.getChimeEndMinutes(context)
        val withinWindow = isWithinWindow(nowMinutes, start, end)

        val activeCall = CallManager.getCall()
        val canSpeak = withinWindow && (activeCall == null ||
            activeCall.state == android.telecom.Call.STATE_DISCONNECTED)

        if (canSpeak) {
            val hour = nowMinutes / 60
            val minute = nowMinutes % 60
            val timeText = String.format("%02d:%02d", hour, minute)

            val pendingResult = goAsync()
            val finished = AtomicBoolean(false)
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed({
                if (finished.compareAndSet(false, true)) {
                    pendingResult.finish()
                }
            }, 8000)

            val announcer = CallAnnouncer(context)
            announcer.speak(
                text = "Jest godzina $timeText",
                repeatCount = 1,
                rate = Prefs.getSpeechRate(context),
                volume = Prefs.getSpeechVolume(context),
                voiceName = Prefs.getVoiceName(context),
                onComplete = {
                    announcer.shutdown()
                    if (finished.compareAndSet(false, true)) {
                        pendingResult.finish()
                    }
                }
            )
        }

        ChimeScheduler.schedule(context)
    }

    private fun currentMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun isWithinWindow(now: Int, start: Int, end: Int): Boolean {
        return if (start == end) {
            true
        } else if (start < end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }
}
