package com.screenreaders.blindroid.document

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
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityDocumentAssistBinding
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

class DocumentAssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDocumentAssistBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null
    private var processing = false
    private var capturing = false
    private var lastAnalyze = 0L
    private var lastGuidance = ""
    private var lastGuidanceTime = 0L
    private var readyFrames = 0
    private var lastCaptureTime = 0L
    private var tts: TextToSpeech? = null
    private lateinit var textRecognizer: TextRecognizer
    private var autoCapture = true
    private var speakResult = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLockScreen()

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyTtsSettings()
            }
        }

        autoCapture = Prefs.isDocAutoCaptureEnabled(this)
        speakResult = Prefs.isDocSpeakResultEnabled(this)

        binding.documentScanSwitch.isChecked = false
        binding.documentAutoSwitch.isChecked = autoCapture
        binding.documentSpeakSwitch.isChecked = speakResult

        binding.documentAutoSwitch.setOnCheckedChangeListener { _, isChecked ->
            autoCapture = isChecked
            Prefs.setDocAutoCaptureEnabled(this, isChecked)
        }
        binding.documentSpeakSwitch.setOnCheckedChangeListener { _, isChecked ->
            speakResult = isChecked
            Prefs.setDocSpeakResultEnabled(this, isChecked)
        }
        binding.documentScanSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startScanning()
            } else {
                stopScanning()
            }
        }

        updateStatus(getString(R.string.documents_status_idle))
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
                if (binding.documentScanSwitch.isChecked) {
                    startScanning()
                }
            } else {
                binding.documentScanSwitch.isChecked = false
                updateStatus(getString(R.string.documents_status_permission))
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
            binding.documentScanSwitch.isChecked = false
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.documents_status_searching))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(1280, 720))
                .build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                analyzeFrame(imageProxy)
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, analysis, capture)
            cameraProvider = provider
            this.analysis = analysis
            imageCapture = capture
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopScanning() {
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        analysis = null
        imageCapture = null
        cameraProvider = null
        processing = false
        capturing = false
        readyFrames = 0
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.documents_status_idle))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (processing || now - lastAnalyze < ANALYZE_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        processing = true
        lastAnalyze = now

        val plane = imageProxy.planes.firstOrNull()
        if (plane == null) {
            processing = false
            imageProxy.close()
            return
        }
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val step = 4
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0
        var edgeCount = 0
        var gradientSum = 0L
        var samples = 0
        var brightnessSum = 0L

        var y = step
        while (y < height - step) {
            val rowOffset = y * rowStride
            val rowOffsetUp = (y - step) * rowStride
            val rowOffsetDown = (y + step) * rowStride
            var x = step
            while (x < width - step) {
                val index = rowOffset + x * pixelStride
                val value = data[index].toInt() and 0xFF
                brightnessSum += value
                samples += 1

                val left = data[rowOffset + (x - step) * pixelStride].toInt() and 0xFF
                val right = data[rowOffset + (x + step) * pixelStride].toInt() and 0xFF
                val up = data[rowOffsetUp + x * pixelStride].toInt() and 0xFF
                val down = data[rowOffsetDown + x * pixelStride].toInt() and 0xFF

                val dx = abs(right - left)
                val dy = abs(down - up)
                val grad = dx + dy
                gradientSum += grad

                if (grad > EDGE_THRESHOLD) {
                    edgeCount += 1
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }

                x += step
            }
            y += step
        }

        val avgBrightness = if (samples == 0) 0 else (brightnessSum / samples).toInt()
        val avgGradient = if (samples == 0) 0 else (gradientSum / samples).toInt()

        val guidance = evaluateGuidance(
            width = width,
            height = height,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            edgeCount = edgeCount,
            avgBrightness = avgBrightness,
            sharpness = avgGradient
        )

        updateStatus(guidance.message)
        maybeSpeakGuidance(guidance.message)

        if (guidance.ready) {
            readyFrames += 1
        } else {
            readyFrames = 0
        }

        if (autoCapture && guidance.ready && readyFrames >= OK_FRAMES && !capturing) {
            val timeSinceCapture = now - lastCaptureTime
            if (timeSinceCapture > CAPTURE_COOLDOWN_MS) {
                captureDocument()
                readyFrames = 0
            }
        }

        processing = false
        imageProxy.close()
    }

    private data class Guidance(val ready: Boolean, val message: String)

    private fun evaluateGuidance(
        width: Int,
        height: Int,
        minX: Int,
        maxX: Int,
        minY: Int,
        maxY: Int,
        edgeCount: Int,
        avgBrightness: Int,
        sharpness: Int
    ): Guidance {
        if (avgBrightness < MIN_BRIGHTNESS) {
            return Guidance(false, getString(R.string.documents_status_too_dark))
        }
        if (edgeCount < MIN_EDGES) {
            return Guidance(false, getString(R.string.documents_status_no_doc))
        }
        val bboxWidth = (maxX - minX).toFloat().coerceAtLeast(1f)
        val bboxHeight = (maxY - minY).toFloat().coerceAtLeast(1f)
        val coverageW = bboxWidth / width
        val coverageH = bboxHeight / height

        val marginX = width * EDGE_MARGIN
        val marginY = height * EDGE_MARGIN
        val cut = minX < marginX || maxX > width - marginX || minY < marginY || maxY > height - marginY

        if (cut) {
            return Guidance(false, getString(R.string.documents_status_cut))
        }
        if (coverageW < MIN_COVERAGE || coverageH < MIN_COVERAGE) {
            return Guidance(false, getString(R.string.documents_status_too_far))
        }
        if (coverageW > MAX_COVERAGE || coverageH > MAX_COVERAGE) {
            return Guidance(false, getString(R.string.documents_status_too_close))
        }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val offsetX = (centerX - width / 2f) / width
        val offsetY = (centerY - height / 2f) / height

        if (offsetX < -CENTER_TOLERANCE) {
            return Guidance(false, getString(R.string.documents_status_move_right))
        }
        if (offsetX > CENTER_TOLERANCE) {
            return Guidance(false, getString(R.string.documents_status_move_left))
        }
        if (offsetY < -CENTER_TOLERANCE) {
            return Guidance(false, getString(R.string.documents_status_move_down))
        }
        if (offsetY > CENTER_TOLERANCE) {
            return Guidance(false, getString(R.string.documents_status_move_up))
        }

        if (sharpness < MIN_SHARPNESS) {
            return Guidance(false, getString(R.string.documents_status_blurry))
        }

        return Guidance(true, getString(R.string.documents_status_ready))
    }

    private fun captureDocument() {
        val capture = imageCapture ?: return
        capturing = true
        updateStatus(getString(R.string.documents_status_capturing))
        maybeSpeakGuidance(getString(R.string.documents_status_capturing))
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    handleCapturedImage(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    capturing = false
                    updateStatus(getString(R.string.documents_status_failed))
                }
            }
        )
    }

    private fun handleCapturedImage(imageProxy: ImageProxy) {
        updateStatus(getString(R.string.documents_status_ocr))
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            capturing = false
            imageProxy.close()
            updateStatus(getString(R.string.documents_status_failed))
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(input)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                if (text.isBlank()) {
                    updateStatus(getString(R.string.documents_status_empty))
                } else {
                    updateStatus(getString(R.string.documents_status_done))
                }
                updateResult(text.ifBlank { getString(R.string.documents_status_empty) })
                if (speakResult && text.isNotBlank()) {
                    speakText(text)
                }
            }
            .addOnFailureListener {
                updateStatus(getString(R.string.documents_status_failed))
            }
            .addOnCompleteListener {
                lastCaptureTime = System.currentTimeMillis()
                capturing = false
                imageProxy.close()
            }
    }

    private fun updateResult(text: String) {
        mainHandler.post {
            binding.documentResultText.text = text
        }
    }

    private fun updateStatus(text: String) {
        mainHandler.post {
            binding.documentStatusText.text = text
        }
    }

    private fun maybeSpeakGuidance(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastGuidance && now - lastGuidanceTime < GUIDANCE_REPEAT_MS) return
        if (now - lastGuidanceTime < GUIDANCE_MIN_INTERVAL_MS) return
        lastGuidance = message
        lastGuidanceTime = now
        speakText(message)
    }

    private fun speakText(text: String) {
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "doc")
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
        private const val REQ_CAMERA = 610
        private const val ANALYZE_INTERVAL_MS = 600L
        private const val GUIDANCE_MIN_INTERVAL_MS = 1200L
        private const val GUIDANCE_REPEAT_MS = 4000L
        private const val CAPTURE_COOLDOWN_MS = 6000L
        private const val OK_FRAMES = 4
        private const val EDGE_THRESHOLD = 30
        private const val MIN_EDGES = 300
        private const val MIN_BRIGHTNESS = 25
        private const val MIN_SHARPNESS = 18
        private const val MIN_COVERAGE = 0.55f
        private const val MAX_COVERAGE = 0.95f
        private const val CENTER_TOLERANCE = 0.12f
        private const val EDGE_MARGIN = 0.04f
    }
}
