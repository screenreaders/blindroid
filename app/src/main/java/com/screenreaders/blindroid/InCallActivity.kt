package com.screenreaders.blindroid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telecom.Call
import android.telecom.VideoProfile
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.call.CallerInfoResolver
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityInCallBinding
import java.text.Normalizer
import java.util.Locale

class InCallActivity : AppCompatActivity(), CallManager.Listener {
    companion object {
        private const val REQ_RECORD_AUDIO = 4001
        private const val VOICE_TIMEOUT_MS = 7000L
    }

    private lateinit var binding: ActivityInCallBinding
    private var currentCall: Call? = null
    private var lastEndKeyTime: Long = 0L
    private var lastEndKeyCode: Int = 0
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private var stopListeningRunnable: Runnable? = null
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }

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
        binding.voiceCommandButton.setOnClickListener {
            startVoiceCommand()
        }
        binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
    }

    override fun onStart() {
        super.onStart()
        CallManager.addListener(this)
        updateVoiceCommandVisibility()
    }

    override fun onStop() {
        super.onStop()
        CallManager.removeListener(this)
        stopListening()
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
            lastEndKeyTime = 0L
            lastEndKeyCode = 0
            return true
        }
        return false
    }

    private fun endCurrentCall() {
        val active = CallManager.getActiveCall()
        val target = active ?: CallManager.getCall()
        target?.disconnect()
    }

    private fun updateVoiceCommandVisibility() {
        val enabled = Prefs.isVoiceCommandsEnabled(this) &&
            SpeechRecognizer.isRecognitionAvailable(this)
        binding.voiceCommandButton.visibility = if (enabled) View.VISIBLE else View.GONE
        binding.voiceCommandStatus.visibility = if (enabled) View.VISIBLE else View.GONE
        if (!enabled) {
            binding.voiceCommandStatus.text = getString(R.string.voice_command_status_unavailable)
        }
    }

    private fun startVoiceCommand() {
        if (!Prefs.isVoiceCommandsEnabled(this)) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.voiceCommandStatus.text = getString(R.string.voice_command_status_unavailable)
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQ_RECORD_AUDIO
            )
            return
        }
        if (isListening) return
        ensureSpeechRecognizer()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        isListening = true
        binding.voiceCommandStatus.text = getString(R.string.voice_command_status_listening)
        speechRecognizer?.startListening(intent)
        scheduleStopListening()
    }

    private fun ensureSpeechRecognizer() {
        if (speechRecognizer != null) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_listening)
                }

                override fun onBeginningOfSpeech() {
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_processing)
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    stopListening()
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_error)
                }

                override fun onResults(results: Bundle) {
                    stopListening()
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull().orEmpty()
                    handleVoiceCommand(text)
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun scheduleStopListening() {
        stopListeningRunnable?.let { handler.removeCallbacks(it) }
        val runnable = Runnable { stopListening() }
        stopListeningRunnable = runnable
        handler.postDelayed(runnable, VOICE_TIMEOUT_MS)
    }

    private fun stopListening() {
        if (!isListening) return
        isListening = false
        stopListeningRunnable?.let { handler.removeCallbacks(it) }
        stopListeningRunnable = null
        speechRecognizer?.stopListening()
        binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
    }

    private fun handleVoiceCommand(text: String) {
        val normalized = normalize(text)
        when {
            containsAny(normalized, "rozlacz", "zakoncz", "koniec", "koniec rozmowy") -> {
                endCurrentCall()
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "odbierz", "odebierz") -> {
                val call = CallManager.getRingingCall()
                call?.answer(VideoProfile.STATE_AUDIO_ONLY)
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "odrzuc", "odrzucic", "odmow", "odrzucenie") -> {
                val call = CallManager.getRingingCall()
                call?.reject(false, null)
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "glosnik", "glosnomowiacy", "glosnomow") -> {
                if (containsAny(normalized, "wylacz", "wyłącz", "off", "wylaczyc", "wylaczic")) {
                    setSpeakerOverride(Prefs.SPEAKER_OVERRIDE_EARPIECE)
                } else if (containsAny(normalized, "wlacz", "włącz", "on", "uruchom")) {
                    setSpeakerOverride(Prefs.SPEAKER_OVERRIDE_SPEAKER)
                } else {
                    toggleSpeakerOverride()
                }
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "sluchawka", "do ucha") -> {
                setSpeakerOverride(Prefs.SPEAKER_OVERRIDE_EARPIECE)
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "mikrofon") -> {
                if (containsAny(normalized, "wylacz", "wyłącz", "wycisz", "mute", "wylaczyc", "wylaczic")) {
                    audioManager.isMicrophoneMute = true
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
                } else if (containsAny(normalized, "wlacz", "włącz", "odcisz", "unmute", "uruchom")) {
                    audioManager.isMicrophoneMute = false
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
                } else {
                    binding.voiceCommandStatus.text = getString(R.string.voice_command_status_error)
                }
            }
            containsAny(normalized, "glosniej", "glosnosc w gore", "glosnosc wyzej") -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.ADJUST_RAISE,
                    0
                )
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            containsAny(normalized, "ciszej", "glosnosc w dol", "glosnosc nizej") -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.ADJUST_LOWER,
                    0
                )
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_idle)
            }
            else -> {
                binding.voiceCommandStatus.text = getString(R.string.voice_command_status_error)
            }
        }
    }

    private fun normalize(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        return normalized.replace("\\p{M}+".toRegex(), "")
    }

    private fun containsAny(text: String, vararg candidates: String): Boolean {
        return candidates.any { text.contains(it) }
    }

    private fun toggleSpeakerOverride() {
        val current = Prefs.getSpeakerOverride(this)
        val next = if (current == Prefs.SPEAKER_OVERRIDE_SPEAKER) {
            Prefs.SPEAKER_OVERRIDE_EARPIECE
        } else {
            Prefs.SPEAKER_OVERRIDE_SPEAKER
        }
        setSpeakerOverride(next)
    }

    private fun setSpeakerOverride(value: Int) {
        if (hasExternalOutput()) return
        Prefs.setSpeakerOverride(this, value)
        when (value) {
            Prefs.SPEAKER_OVERRIDE_SPEAKER -> routeToSpeaker()
            Prefs.SPEAKER_OVERRIDE_EARPIECE -> routeToEarpiece()
            else -> Unit
        }
    }

    private fun routeToSpeaker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
        }
        audioManager.isSpeakerphoneOn = true
    }

    private fun routeToEarpiece() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
            audioManager.clearCommunicationDevice()
            return
        }
        audioManager.isSpeakerphoneOn = false
    }

    private fun hasExternalOutput(): Boolean {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> true
                else -> false
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceCommand()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
