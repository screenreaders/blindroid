package com.screenreaders.blindroid

import android.os.Bundle
import android.telecom.Call
import android.telecom.VideoProfile
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.call.CallerInfoResolver
import com.screenreaders.blindroid.databinding.ActivityInCallBinding

class InCallActivity : AppCompatActivity(), CallManager.Listener {
    private lateinit var binding: ActivityInCallBinding
    private var currentCall: Call? = null

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
}
