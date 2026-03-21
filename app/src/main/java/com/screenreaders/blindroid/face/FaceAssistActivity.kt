package com.screenreaders.blindroid.face

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.WindowManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityFaceAssistBinding
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

class FaceAssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFaceAssistBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private var faceDetector: FaceDetector? = null
    private var processing = false
    private var lastAnalyze = 0L
    private var lastGuidance = ""
    private var lastGuidanceTime = 0L
    private var tts: TextToSpeech? = null
    private var active = false
    private var soundEnabled = false
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.isFaceAssistEnabled(this)) {
            finish()
            return
        }
        binding = ActivityFaceAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupLockScreen()

        faceDetector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
        )

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                applyTtsSettings()
            }
        }
        toneGenerator = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 80)
        soundEnabled = Prefs.isFaceSoundEnabled(this)
        binding.faceSoundSwitch.isChecked = soundEnabled
        binding.faceSoundSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setFaceSoundEnabled(this, isChecked)
            soundEnabled = isChecked
        }

        binding.faceActiveSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAssistant()
            } else {
                stopAssistant()
            }
        }
        updateStatus(getString(R.string.face_status_idle))
    }

    override fun onStop() {
        super.onStop()
        stopAssistant()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        faceDetector?.close()
        toneGenerator?.release()
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
                if (binding.faceActiveSwitch.isChecked) {
                    startAssistant()
                }
            } else {
                binding.faceActiveSwitch.isChecked = false
                updateStatus(getString(R.string.face_permission_needed))
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

    private fun startAssistant() {
        if (!ensureCameraPermission()) {
            binding.faceActiveSwitch.isChecked = false
            return
        }
        if (active) return
        active = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.face_status_searching))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val selector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(640, 480),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(selector)
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
                val image = imageProxy.image
                if (image == null) {
                    imageProxy.close()
                    processing = false
                    return@setAnalyzer
                }
                val brightness = estimateBrightness(imageProxy)
                val input = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                faceDetector?.process(input)
                    ?.addOnSuccessListener { faces ->
                        handleFaces(faces, imageProxy.width, imageProxy.height, brightness)
                    }
                    ?.addOnCompleteListener {
                        imageProxy.close()
                        processing = false
                    }
            }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
            cameraProvider = provider
            this.analysis = analysis
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopAssistant() {
        if (!active) return
        active = false
        analysis?.clearAnalyzer()
        analysis = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.face_status_idle))
    }

    private fun handleFaces(faces: List<Face>, width: Int, height: Int, brightness: Int) {
        val guidance = buildGuidance(faces, width, height, brightness)
        updateStatus(guidance)
        maybeSpeakGuidance(guidance)
    }

    private fun buildGuidance(faces: List<Face>, width: Int, height: Int, brightness: Int): String {
        if (brightness in 0 until MIN_BRIGHTNESS) {
            return getString(R.string.face_guidance_too_dark)
        }
        if (faces.isEmpty()) {
            return getString(R.string.face_guidance_no_face)
        }
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return getString(
            R.string.face_guidance_no_face
        )
        val box = face.boundingBox
        val area = (box.width().toFloat() * box.height().toFloat()) / max(1, width * height).toFloat()
        if (area < MIN_FACE_COVERAGE) {
            return getString(R.string.face_guidance_move_closer)
        }
        if (area > MAX_FACE_COVERAGE) {
            return getString(R.string.face_guidance_move_farther)
        }
        val cx = box.centerX().toFloat() / width.toFloat()
        val cy = box.centerY().toFloat() / height.toFloat()
        val dx = cx - 0.5f
        val dy = cy - 0.5f
        if (abs(dx) > CENTER_TOLERANCE) {
            return if (dx < 0f) {
                getString(R.string.face_guidance_move_left)
            } else {
                getString(R.string.face_guidance_move_right)
            }
        }
        if (abs(dy) > CENTER_TOLERANCE) {
            return if (dy < 0f) {
                getString(R.string.face_guidance_move_up)
            } else {
                getString(R.string.face_guidance_move_down)
            }
        }
        if (abs(face.headEulerAngleY) > 18f || abs(face.headEulerAngleX) > 18f) {
            return getString(R.string.face_guidance_look_center)
        }
        return getString(R.string.face_status_ready)
    }

    private fun updateStatus(message: String) {
        mainHandler.post {
            binding.faceStatusText.text = message
        }
    }

    private fun maybeSpeakGuidance(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastGuidance && now - lastGuidanceTime < GUIDANCE_REPEAT_MS) return
        if (now - lastGuidanceTime < GUIDANCE_MIN_INTERVAL_MS) return
        lastGuidance = message
        lastGuidanceTime = now
        if (soundEnabled) {
            playSoundCue(message)
        }
        speakText(message)
    }

    private fun speakText(text: String) {
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "face")
    }

    private fun playSoundCue(message: String) {
        val tone = when (message) {
            getString(R.string.face_status_ready) -> ToneGenerator.TONE_PROP_BEEP2
            getString(R.string.face_guidance_no_face) -> ToneGenerator.TONE_PROP_NACK
            getString(R.string.face_guidance_too_dark) -> ToneGenerator.TONE_PROP_NACK
            getString(R.string.face_guidance_move_left),
            getString(R.string.face_guidance_move_right),
            getString(R.string.face_guidance_move_up),
            getString(R.string.face_guidance_move_down),
            getString(R.string.face_guidance_move_closer),
            getString(R.string.face_guidance_move_farther),
            getString(R.string.face_guidance_look_center) -> ToneGenerator.TONE_PROP_BEEP
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        toneGenerator?.startTone(tone, 120)
    }

    private fun estimateBrightness(imageProxy: androidx.camera.core.ImageProxy): Int {
        val buffer = imageProxy.planes.firstOrNull()?.buffer ?: return 0
        val size = buffer.remaining()
        if (size <= 0) return 0
        val step = max(1, size / 1200)
        var sum = 0
        var count = 0
        var i = 0
        while (i < size) {
            sum += buffer.get(i).toInt() and 0xFF
            count += 1
            i += step
        }
        return if (count == 0) 0 else sum / count
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
        tts?.language = Locale.forLanguageTag("pl-PL")
    }

    companion object {
        private const val REQ_CAMERA = 640
        private const val ANALYZE_INTERVAL_MS = 260L
        private const val GUIDANCE_MIN_INTERVAL_MS = 900L
        private const val GUIDANCE_REPEAT_MS = 3000L
        private const val MIN_BRIGHTNESS = 28
        private const val MIN_FACE_COVERAGE = 0.12f
        private const val MAX_FACE_COVERAGE = 0.55f
        private const val CENTER_TOLERANCE = 0.18f
    }
}
