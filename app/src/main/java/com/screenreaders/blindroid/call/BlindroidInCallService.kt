package com.screenreaders.blindroid.call

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import com.screenreaders.blindroid.InCallActivity
import com.screenreaders.blindroid.audio.RingerController
import com.screenreaders.blindroid.audio.ProximitySpeakerController
import com.screenreaders.blindroid.data.Prefs

class BlindroidInCallService : InCallService() {
    private lateinit var announcer: CallAnnouncer
    private lateinit var proximityController: ProximitySpeakerController
    private lateinit var ringerController: RingerController
    private val announcedCalls = HashSet<Call>()

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallManager.updateCall(call)
            handleCallState(call, state)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallManager.updateCall(call)
        }
    }

    override fun onCreate() {
        super.onCreate()
        announcer = CallAnnouncer(this)
        proximityController = ProximitySpeakerController(this)
        ringerController = RingerController(this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.addCall(call)
        call.registerCallback(callCallback)
        startInCallUi()
        handleCallState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        announcedCalls.remove(call)
        CallManager.removeCall(call)
        if (CallManager.getCall() == null) {
            proximityController.stop()
            announcer.shutdown()
            ringerController.stop()
        }
    }

    private fun handleCallState(call: Call, state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                startInCallUi()
                val hasActiveCall = CallManager.hasActiveCall(exclude = call)
                if (!Prefs.isAnnounceEnabled(this)) return
                if (hasActiveCall && !Prefs.isAnnounceDuringCallEnabled(this)) return
                if (announcedCalls.add(call)) {
                    val name = CallerInfoResolver.displayName(this, call)
                    val mode = Prefs.getAnnounceMode(this)
                    val prefix = if (hasActiveCall) "Połączenie oczekujące" else "Dzwoni"
                    if (mode == Prefs.MODE_SPEECH_ONLY || mode == Prefs.MODE_SPEECH_THEN_RING) {
                        val telecomManager = getSystemService(TelecomManager::class.java)
                        telecomManager.silenceRinger()
                        ringerController.stop()
                    } else {
                        ringerController.stop()
                    }
                    announcer.speak(
                        text = "$prefix: $name",
                        repeatCount = Prefs.getRepeatCount(this),
                        rate = Prefs.getSpeechRate(this),
                        volume = Prefs.getSpeechVolume(this),
                        voiceName = Prefs.getVoiceName(this),
                        onComplete = {
                            if (mode == Prefs.MODE_SPEECH_THEN_RING && call.state == Call.STATE_RINGING) {
                                ringerController.start()
                            }
                        }
                    )
                }
            }
            Call.STATE_ACTIVE -> {
                ringerController.stop()
                if (Prefs.isAutoSpeakerEnabled(this)) {
                    proximityController.start()
                }
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                proximityController.stop()
                announcer.shutdown()
                ringerController.stop()
            }
        }
    }

    private fun startInCallUi() {
        val intent = Intent(this, InCallActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        startActivity(intent)
    }
}
