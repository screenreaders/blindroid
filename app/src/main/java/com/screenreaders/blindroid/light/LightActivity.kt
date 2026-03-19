package com.screenreaders.blindroid.light

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityLightBinding
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

class LightActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityLightBinding
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private var lightSensor: Sensor? = null
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var processing = false
    private var lastAnalyze = 0L

    private var active = false
    private var directionEnabled = false
    private var currentLux = 0f
    private var lastLuxSpoken = 0L
    private var lastLuxValue = 0f
    private var lastDirection: String? = null
    private var lastDirectionSpoken = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLockScreen()

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyTtsSettings()
            }
        }

        binding.lightActiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            setActive(isChecked)
        }
        binding.lightDirectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!active) {
                binding.lightDirectionSwitch.isChecked = false
                return@setOnCheckedChangeListener
            }
            if (isChecked) {
                if (!ensureCameraPermission()) {
                    binding.lightDirectionSwitch.isChecked = false
                    return@setOnCheckedChangeListener
                }
                directionEnabled = true
                startCamera()
            } else {
                directionEnabled = false
                stopCamera()
                updateDirection(getString(R.string.light_direction_idle))
            }
        }

        binding.lightActiveSwitch.isChecked = false
        binding.lightDirectionSwitch.isEnabled = false
        updateLux(0f)
        updateDirection(getString(R.string.light_direction_idle))
    }

    override fun onStop() {
        super.onStop()
        setActive(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                if (binding.lightDirectionSwitch.isChecked) {
                    directionEnabled = true
                    startCamera()
                }
            } else {
                binding.lightDirectionSwitch.isChecked = false
                updateDirection(getString(R.string.light_direction_permission))
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!active) return
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        currentLux = event.values.firstOrNull() ?: 0f
        updateLux(currentLux)
        if (!directionEnabled) {
            maybeSpeakLux()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

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

    private fun setActive(enabled: Boolean) {
        if (active == enabled) return
        active = enabled
        binding.lightDirectionSwitch.isEnabled = enabled
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            registerLightSensor()
            if (binding.lightDirectionSwitch.isChecked) {
                if (ensureCameraPermission()) {
                    directionEnabled = true
                    startCamera()
                } else {
                    binding.lightDirectionSwitch.isChecked = false
                }
            }
        } else {
            directionEnabled = false
            binding.lightDirectionSwitch.isChecked = false
            unregisterLightSensor()
            stopCamera()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun registerLightSensor() {
        val sensor = lightSensor
        if (sensor == null) {
            updateDirection(getString(R.string.light_direction_no_sensor))
            return
        }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun unregisterLightSensor() {
        sensorManager.unregisterListener(this)
    }

    private fun startCamera() {
        updateDirection(getString(R.string.light_direction_starting))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val now = System.currentTimeMillis()
                if (processing || now - lastAnalyze < ANALYZE_INTERVAL_MS) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                processing = true
                lastAnalyze = now
                val plane = imageProxy.planes.firstOrNull()
                if (plane == null) {
                    processing = false
                    imageProxy.close()
                    return@setAnalyzer
                }
                val buffer = plane.buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)
                val width = imageProxy.width
                val height = imageProxy.height
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride
                val sums = LongArray(3)
                val counts = IntArray(3)
                val step = 4
                var y = 0
                while (y < height) {
                    val rowOffset = y * rowStride
                    var x = 0
                    while (x < width) {
                        val index = rowOffset + x * pixelStride
                        if (index in data.indices) {
                            val value = data[index].toInt() and 0xFF
                            val bucket = (x * 3) / width
                            sums[bucket] += value
                            counts[bucket] += 1
                        }
                        x += step
                    }
                    y += step
                }
                val avg = IntArray(3) { idx ->
                    if (counts[idx] == 0) 0 else (sums[idx] / counts[idx]).toInt()
                }
                val dirIndex = avg.indices.maxByOrNull { avg[it] } ?: 1
                val sorted = avg.sortedDescending()
                val diff = if (sorted.size >= 2) sorted[0] - sorted[1] else 0
                val direction = when {
                    diff < 8 -> getString(R.string.light_direction_even)
                    dirIndex == 0 -> getString(R.string.light_direction_left)
                    dirIndex == 1 -> getString(R.string.light_direction_center)
                    else -> getString(R.string.light_direction_right)
                }
                updateDirection(direction)
                maybeSpeakDirection(direction)
                processing = false
                imageProxy.close()
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
            cameraProvider = provider
            this.analysis = analysis
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        analysis = null
        cameraProvider = null
        processing = false
    }

    private fun updateLux(lux: Float) {
        mainHandler.post {
            binding.lightLuxValue.text = getString(R.string.light_lux_value, lux.toInt())
        }
    }

    private fun updateDirection(text: String) {
        mainHandler.post {
            binding.lightDirectionValue.text = text
        }
    }

    private fun maybeSpeakLux() {
        val now = System.currentTimeMillis()
        val delta = abs(currentLux - lastLuxValue)
        val threshold = max(10f, lastLuxValue * 0.2f)
        if (now - lastLuxSpoken < LUX_SPEAK_INTERVAL_MS && delta < threshold) return
        lastLuxSpoken = now
        lastLuxValue = currentLux
        val level = when {
            currentLux < 30f -> getString(R.string.light_level_dark)
            currentLux < 200f -> getString(R.string.light_level_dim)
            else -> getString(R.string.light_level_bright)
        }
        speak(getString(R.string.light_lux_spoken, currentLux.toInt(), level))
    }

    private fun maybeSpeakDirection(direction: String) {
        val now = System.currentTimeMillis()
        if (direction == lastDirection && now - lastDirectionSpoken < DIRECTION_SPEAK_INTERVAL_MS) return
        lastDirection = direction
        lastDirectionSpoken = now
        speak(getString(R.string.light_direction_spoken, direction))
    }

    private fun speak(text: String) {
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "light")
    }

    private fun ensureCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) return true
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        return false
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
        tts?.language = Locale("pl", "PL")
    }

    companion object {
        private const val REQ_CAMERA = 602
        private const val ANALYZE_INTERVAL_MS = 500L
        private const val LUX_SPEAK_INTERVAL_MS = 5000L
        private const val DIRECTION_SPEAK_INTERVAL_MS = 2500L
    }
}
