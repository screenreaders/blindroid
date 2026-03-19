package com.screenreaders.blindroid

import android.os.Bundle
import android.os.SystemClock
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.call.CallerInfoResolver
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityInCallBinding

class InCallActivity : AppCompatActivity(), CallManager.Listener {
    private lateinit var binding: ActivityInCallBinding
    private var currentCall: Call? = null
    private var lastEndKeyTime: Long = 0L
    private var lastEndKeyCode: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.answerButton.setOnClickListener {
            currentCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
        binding.rejectButton.setOnClickListener {
            currentCall?.reject(false, null)
        }
        binding.hangupButton.setOnClickListener {
            currentCall?.disconnect()
        }
    }

    override fun onStart() {
        super.onStart()
        CallManager.addListener(this)
    }

    override fun onStop() {
        super.onStop()
        CallManager.removeListener(this)
    }

    override fun onCallChanged(call: Call?) {
        currentCall = call
        if (call == null) {
            finish()
            return
        }
        val displayName = CallerInfoResolver.displayName(this, call)
        binding.callerText.text = displayName
        binding.stateText.text = stateLabel(call.state)
        updateButtons(call.state)
    }

    private fun updateButtons(state: Int) {
        when (state) {
            Call.STATE_RINGING -> {
                binding.answerButton.isEnabled = true
                binding.answerButton.visibility = android.view.View.VISIBLE
                binding.rejectButton.visibility = android.view.View.VISIBLE
                binding.hangupButton.visibility = android.view.View.GONE
            }
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING -> {
                binding.answerButton.visibility = android.view.View.GONE
                binding.rejectButton.visibility = android.view.View.GONE
                binding.hangupButton.visibility = android.view.View.VISIBLE
                binding.hangupButton.isEnabled = true
            }
            else -> {
                binding.answerButton.visibility = android.view.View.GONE
                binding.rejectButton.visibility = android.view.View.GONE
                binding.hangupButton.visibility = android.view.View.GONE
            }
        }
    }

    private fun stateLabel(state: Int): String {
        return when (state) {
            Call.STATE_RINGING -> "Dzwoni"
            Call.STATE_ACTIVE -> "Połączono"
            Call.STATE_DIALING -> "Wybieranie"
            Call.STATE_CONNECTING -> "Łączenie"
            Call.STATE_DISCONNECTED -> "Zakończono"
            else -> ""
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && handleEndCallKey(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleEndCallKey(event: KeyEvent): Boolean {
        val action = Prefs.getEndCallKey(this)
        if (action == Prefs.END_CALL_NONE || action == Prefs.END_CALL_POWER) return false

        val keyCode = event.keyCode
        val matches = when (action) {
            Prefs.END_CALL_VOLUME_UP -> keyCode == KeyEvent.KEYCODE_VOLUME_UP
            Prefs.END_CALL_VOLUME_DOWN -> keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            Prefs.END_CALL_HEADSET -> keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            else -> false
        }
        if (!matches) return false

        val now = SystemClock.uptimeMillis()
        val isDoublePress = keyCode == lastEndKeyCode && now - lastEndKeyTime <= 800
        lastEndKeyTime = now
        lastEndKeyCode = keyCode
        if (isDoublePress) {
            endCurrentCall()
        }
        return true
    }

    private fun endCurrentCall() {
        val active = CallManager.getActiveCall()
        val target = active ?: CallManager.getCall()
        target?.disconnect()
    }
}
