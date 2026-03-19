package com.screenreaders.blindroid.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import java.util.concurrent.atomic.AtomicBoolean
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.util.LockScreenUtils

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Prefs.isSmsReadEnabled(context)) return
        val locked = LockScreenUtils.isDeviceLocked(context)
        if (!locked && !Prefs.isReadWhenUnlockedEnabled(context)) return

        val activeCall = CallManager.getCall()
        if (activeCall != null && activeCall.state != android.telecom.Call.STATE_DISCONNECTED) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val from = messages.firstOrNull()?.originatingAddress ?: "Nieznany nadawca"
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (body.isBlank()) return

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
            text = "SMS od $from. $body",
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
}
