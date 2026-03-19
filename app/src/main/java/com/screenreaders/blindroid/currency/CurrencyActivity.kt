package com.screenreaders.blindroid.currency

import android.Manifest
import android.content.pm.PackageManager
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityCurrencyBinding
import java.util.Locale
import java.util.concurrent.Executors

class CurrencyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCurrencyBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var processing = false
    private var lastAnalyze = 0L
    private var lastSpokenValue: String? = null
    private var lastSpeakTime = 0L
    private var tts: TextToSpeech? = null
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrencyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLockScreen()

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyTtsSettings()
            }
        }

        binding.currencyScanSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startScanning()
            } else {
                stopScanning()
            }
        }

        updateStatus(getString(R.string.currency_status_idle))
    }

    override fun onStop() {
        super.onStop()
        stopScanning()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        textRecognizer.close()
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
                if (binding.currencyScanSwitch.isChecked) {
                    startScanning()
                }
            } else {
                binding.currencyScanSwitch.isChecked = false
                updateStatus(getString(R.string.currency_status_permission))
            }
        }
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

    private fun startScanning() {
        if (!ensureCameraPermission()) {
            binding.currencyScanSwitch.isChecked = false
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.currency_status_starting))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val now = System.currentTimeMillis()
                if (processing || now - lastAnalyze < ANALYZE_INTERVAL_MS) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                processing = true
                lastAnalyze = now
                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { result ->
                        handleText(result.text)
                    }
                    .addOnFailureListener {
                        updateStatus(getString(R.string.currency_status_failed))
                    }
                    .addOnCompleteListener {
                        processing = false
                        imageProxy.close()
                    }
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
            cameraProvider = provider
            this.analysis = analysis
            updateStatus(getString(R.string.currency_status_running))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopScanning() {
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        analysis = null
        cameraProvider = null
        processing = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.currency_status_idle))
    }

    private fun handleText(text: String) {
        val value = extractLargestNumber(text)
        if (value == null) {
            updateStatus(getString(R.string.currency_status_not_found))
            return
        }
        val cleanValue = value.replace(',', '.')
        updateStatus(getString(R.string.currency_status_found, cleanValue))
        speakCurrency(cleanValue)
    }

    private fun extractLargestNumber(text: String): String? {
        val regex = Regex("\\d{1,4}(?:[.,]\\d{1,2})?")
        val candidates = regex.findAll(text).map { it.value }.toList()
        if (candidates.isEmpty()) return null
        return candidates.maxByOrNull { candidate ->
            candidate.replace(',', '.').toDoubleOrNull() ?: 0.0
        }
    }

    private fun speakCurrency(value: String) {
        val now = System.currentTimeMillis()
        if (value == lastSpokenValue && now - lastSpeakTime < SPEAK_INTERVAL_MS) return
        lastSpokenValue = value
        lastSpeakTime = now
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(
            getString(R.string.currency_spoken, value),
            TextToSpeech.QUEUE_FLUSH,
            params,
            "currency"
        )
    }

    private fun updateStatus(text: String) {
        mainHandler.post {
            binding.currencyStatusText.text = text
        }
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
        private const val REQ_CAMERA = 601
        private const val ANALYZE_INTERVAL_MS = 700L
        private const val SPEAK_INTERVAL_MS = 2500L
    }
}
