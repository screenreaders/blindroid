package com.screenreaders.blindroid.call

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.screenreaders.blindroid.InCallActivity
import com.screenreaders.blindroid.audio.RingerController
import com.screenreaders.blindroid.audio.ProximitySpeakerController
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog

class BlindroidInCallService : InCallService() {
    private lateinit var announcer: CallAnnouncer
    private lateinit var proximityController: ProximitySpeakerController
    private val announcedCalls = HashSet<Call>()
    private val callStates = HashMap<Call, Int>()
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

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
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        DiagnosticLog.log(this, "call_added state=${call.state}")
        CallManager.addCall(call)
        callStates[call] = call.state
        call.registerCallback(callCallback)
        startInCallUi()
        handleCallState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        DiagnosticLog.log(this, "call_removed state=${call.state}")
        call.unregisterCallback(callCallback)
        announcedCalls.remove(call)
        callStates.remove(call)
        CallManager.removeCall(call)
        if (CallManager.getCall() == null) {
            Prefs.clearSpeakerOverride(this)
            proximityController.stop()
            announcer.shutdown()
            RingerController.stop()
        }
    }

    private fun handleCallState(call: Call, state: Int) {
        val previousState = callStates[call]
        if (previousState != state) {
            callStates[call] = state
            DiagnosticLog.log(this, "call_state $previousState->$state")
            if (Prefs.isCallStateAnnounceEnabled(this)) {
                announceCallState(call, state)
            }
            if (Prefs.isCallStateVibrateEnabled(this)) {
                vibrateForState(call, state)
            } else if (state == Call.STATE_DISCONNECTED && Prefs.isEndCallVibrateEnabled(this)) {
                vibrateEndCallShort()
            }
        }
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
                        RingerController.stop()
                    } else {
                        RingerController.stop()
                    }
                    announcer.speak(
                        text = "$prefix: $name",
                        repeatCount = Prefs.getRepeatCount(this),
                        rate = Prefs.getSpeechRate(this),
                        volume = Prefs.getSpeechVolume(this),
                        voiceName = Prefs.getVoiceName(this),
                        onComplete = {
                            if (mode == Prefs.MODE_SPEECH_THEN_RING && call.state == Call.STATE_RINGING) {
                                RingerController.start(this)
                            }
                        }
                    )
                }
            }
            Call.STATE_ACTIVE -> {
                RingerController.stop()
                if (Prefs.isAutoSpeakerEnabled(this)) {
                    proximityController.start()
                }
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                proximityController.stop()
                announcer.shutdown()
                RingerController.stop()
            }
        }
    }

    private fun announceCallState(call: Call, state: Int) {
        if (state == Call.STATE_RINGING) return
        val message = when (state) {
            Call.STATE_DIALING -> "Wybieranie"
            Call.STATE_CONNECTING -> "Łączenie"
            Call.STATE_ACTIVE -> "Połączono"
            Call.STATE_HOLDING -> "Zawieszono"
            Call.STATE_DISCONNECTED -> "Rozłączono"
            else -> null
        } ?: return
        announcer.speak(
            text = message,
            repeatCount = 1,
            rate = Prefs.getSpeechRate(this),
            volume = Prefs.getSpeechVolume(this),
            voiceName = Prefs.getVoiceName(this)
        )
    }

    private fun vibrateForState(call: Call, state: Int) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val hasActive = CallManager.hasActiveCall(exclude = call)
        val pattern = when (state) {
            Call.STATE_RINGING -> if (hasActive) {
                longArrayOf(0, 120, 80, 120, 80, 120)
            } else {
                longArrayOf(0, 200, 100, 200)
            }
            Call.STATE_ACTIVE -> longArrayOf(0, 80)
            Call.STATE_DISCONNECTED -> longArrayOf(0, 160, 80, 160)
            Call.STATE_HOLDING -> longArrayOf(0, 80, 60, 80)
            else -> null
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
        }
    }

    private fun vibrateEndCallShort() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val pattern = longArrayOf(0, 120)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(pattern, -1)
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
