package com.screenreaders.blindroid.obstacle

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityObstacleAssistBinding
import java.util.Locale

class ObstacleAssistActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityObstacleAssistBinding
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private var proximitySensor: Sensor? = null
    private var active = false
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private var toneGenerator: ToneGenerator? = null
    private var soundEnabled = false
    private val handler = Handler(Looper.getMainLooper())
    private val ttsShutdownRunnable = Runnable { shutdownTts() }
    private var lastSpoken = 0L
    private var lastStateNear = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObstacleAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLockScreen()

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 80)
        soundEnabled = Prefs.isObstacleSoundEnabled(this)
        binding.obstacleSoundSwitch.isChecked = soundEnabled
        binding.obstacleSoundSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setObstacleSoundEnabled(this, isChecked)
            soundEnabled = isChecked
        }

        binding.obstacleActiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            setActive(isChecked)
        }
        binding.obstacleActiveSwitch.isChecked = false
        updateStatus(getString(R.string.obstacle_status_idle))
    }

    override fun onStop() {
        super.onStop()
        setActive(false)
        shutdownTts()
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownTts()
        toneGenerator?.release()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active) return
        if (event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values.firstOrNull() ?: return
        val max = proximitySensor?.maximumRange ?: distance
        val near = distance < max * 0.6f
        if (near != lastStateNear) {
            lastStateNear = near
            val message = if (near) {
                getString(R.string.obstacle_status_near)
            } else {
                getString(R.string.obstacle_status_clear)
            }
            updateStatus(message)
            maybeSpeak(message)
            if (soundEnabled) {
                toneGenerator?.startTone(
                    if (near) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                    120
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun setActive(enabled: Boolean) {
        if (active == enabled) return
        active = enabled
        if (enabled) {
            if (proximitySensor == null) {
                updateStatus(getString(R.string.obstacle_status_no_sensor))
                binding.obstacleActiveSwitch.isChecked = false
                return
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
            updateStatus(getString(R.string.obstacle_status_clear))
        } else {
            sensorManager.unregisterListener(this)
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateStatus(getString(R.string.obstacle_status_idle))
        }
    }

    private fun updateStatus(message: String) {
        handler.post { binding.obstacleStatusText.text = message }
    }

    private fun maybeSpeak(message: String) {
        val now = System.currentTimeMillis()
        if (now - lastSpoken < 1500L) return
        lastSpoken = now
        ensureTts()
        if (!ttsReady) {
            pendingSpeech = message
            return
        }
        speakInternal(message)
    }

    private fun speakInternal(message: String) {
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Prefs.getSpeechVolume(this@ObstacleAssistActivity))
        }
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "obstacle")
        scheduleTtsShutdown()
    }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                applyTtsSettings()
                pendingSpeech?.let {
                    pendingSpeech = null
                    speakInternal(it)
                }
            }
        }
    }

    private fun scheduleTtsShutdown() {
        handler.removeCallbacks(ttsShutdownRunnable)
        handler.postDelayed(ttsShutdownRunnable, 30_000)
    }

    private fun shutdownTts() {
        handler.removeCallbacks(ttsShutdownRunnable)
        ttsReady = false
        pendingSpeech = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun applyTtsSettings() {
        tts?.setSpeechRate(Prefs.getSpeechRate(this))
        val voiceName = Prefs.getVoiceName(this)
        if (voiceName != null) {
            val voice = tts?.voices?.firstOrNull { it.name == voiceName }
            if (voice != null) {
                tts?.voice = voice
                return
            }
        }
        tts?.language = Locale.forLanguageTag("pl-PL")
    }

    private fun setupLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}
