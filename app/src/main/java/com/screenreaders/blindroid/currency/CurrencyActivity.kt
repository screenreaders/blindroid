package com.screenreaders.blindroid.currency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

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

    private var mode = Prefs.CURRENCY_MODE_OCR
    private var modelSource = Prefs.CURRENCY_MODEL_SOURCE_FILE
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var inputWidth = 0
    private var inputHeight = 0
    private var inputChannels = 3
    private var inputType: DataType = DataType.FLOAT32
    private var outputType: DataType = DataType.FLOAT32
    private var outputLabels = 0
    private var outputScale = 1f
    private var outputZeroPoint = 0
    private var inputBuffer: ByteBuffer? = null
    private var reuseBitmap: Bitmap? = null

    private val modelPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            takePersistablePermission(uri)
            Prefs.setCurrencyModelUri(this, uri.toString())
            prepareModel()
        }
    }

    private val labelsPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            takePersistablePermission(uri)
            Prefs.setCurrencyLabelsUri(this, uri.toString())
            prepareModel()
        }
    }

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

        mode = Prefs.getCurrencyMode(this)
        modelSource = Prefs.getCurrencyModelSource(this)

        binding.currencyModeOcr.isChecked = mode == Prefs.CURRENCY_MODE_OCR
        binding.currencyModeModel.isChecked = mode == Prefs.CURRENCY_MODE_MODEL
        binding.currencyModelSourceFile.isChecked = modelSource == Prefs.CURRENCY_MODEL_SOURCE_FILE
        binding.currencyModelSourceBuiltin.isChecked = modelSource == Prefs.CURRENCY_MODEL_SOURCE_BUILTIN

        binding.currencyModeGroup.setOnCheckedChangeListener { _, checkedId ->
            mode = if (checkedId == binding.currencyModeModel.id) {
                Prefs.CURRENCY_MODE_MODEL
            } else {
                Prefs.CURRENCY_MODE_OCR
            }
            Prefs.setCurrencyMode(this, mode)
            updateModeUi()
            restartIfActive()
        }

        binding.currencyModelSourceGroup.setOnCheckedChangeListener { _, checkedId ->
            modelSource = if (checkedId == binding.currencyModelSourceBuiltin.id) {
                Prefs.CURRENCY_MODEL_SOURCE_BUILTIN
            } else {
                Prefs.CURRENCY_MODEL_SOURCE_FILE
            }
            Prefs.setCurrencyModelSource(this, modelSource)
            prepareModel()
            restartIfActive()
        }

        binding.currencyModelPickButton.setOnClickListener {
            modelPicker.launch(arrayOf("application/octet-stream", "application/x-tflite", "application/tflite"))
        }

        binding.currencyLabelsPickButton.setOnClickListener {
            labelsPicker.launch(arrayOf("text/plain"))
        }

        binding.currencyScanSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startScanning()
            } else {
                stopScanning()
            }
        }

        updateModeUi()
        prepareModel()
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
        interpreter?.close()
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

    private fun updateModeUi() {
        val modelVisible = mode == Prefs.CURRENCY_MODE_MODEL
        val visibility = if (modelVisible) android.view.View.VISIBLE else android.view.View.GONE
        binding.currencyModelLabel.visibility = visibility
        binding.currencyModelSourceGroup.visibility = visibility
        binding.currencyModelPickButton.visibility = visibility
        binding.currencyLabelsPickButton.visibility = visibility
        binding.currencyModelStatus.visibility = visibility
    }

    private fun restartIfActive() {
        if (binding.currencyScanSwitch.isChecked) {
            stopScanning()
            startScanning()
        }
    }

    private fun startScanning() {
        if (!ensureCameraPermission()) {
            binding.currencyScanSwitch.isChecked = false
            return
        }
        if (mode == Prefs.CURRENCY_MODE_MODEL && !isModelReady()) {
            updateStatus(getString(R.string.currency_status_failed))
            binding.currencyScanSwitch.isChecked = false
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        updateStatus(getString(R.string.currency_status_starting))
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val resolution = if (mode == Prefs.CURRENCY_MODE_MODEL) {
                Size(480, 360)
            } else {
                Size(640, 480)
            }
            val selector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        resolution,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()
            val builder = ImageAnalysis.Builder()
                .setResolutionSelector(selector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

            if (mode == Prefs.CURRENCY_MODE_MODEL) {
                builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            } else {
                builder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            }
            val analysis = builder.build()

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                if (mode == Prefs.CURRENCY_MODE_MODEL) {
                    analyzeModel(imageProxy)
                } else {
                    analyzeOcr(imageProxy)
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

    private fun analyzeOcr(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (processing || now - lastAnalyze < ANALYZE_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        processing = true
        lastAnalyze = now
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { result ->
                handleOcrText(result.text)
            }
            .addOnFailureListener {
                updateStatus(getString(R.string.currency_status_failed))
            }
            .addOnCompleteListener {
                processing = false
                imageProxy.close()
            }
    }

    private fun analyzeModel(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (processing || now - lastAnalyze < ANALYZE_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        processing = true
        lastAnalyze = now
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap == null) {
            processing = false
            imageProxy.close()
            return
        }
        val result = runModel(bitmap)
        if (result != null) {
            updateStatus(getString(R.string.currency_status_found_label, result))
            speakLabel(result)
        } else {
            updateStatus(getString(R.string.currency_status_not_found))
        }
        processing = false
        imageProxy.close()
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val plane = imageProxy.planes.firstOrNull() ?: return null
        val width = imageProxy.width
        val height = imageProxy.height
        val buffer = plane.buffer
        buffer.rewind()
        var bitmap = reuseBitmap
        if (bitmap == null || bitmap.width != width || bitmap.height != height) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            reuseBitmap = bitmap
        }
        bitmap.copyPixelsFromBuffer(buffer)
        val rotation = imageProxy.imageInfo.rotationDegrees
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    private fun runModel(bitmap: Bitmap): String? {
        val interpreter = interpreter ?: return null
        val buffer = inputBuffer ?: return null
        val scaled = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
        buffer.rewind()
        for (y in 0 until inputHeight) {
            for (x in 0 until inputWidth) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (inputType == DataType.FLOAT32) {
                    buffer.putFloat(r / 255f)
                    buffer.putFloat(g / 255f)
                    buffer.putFloat(b / 255f)
                } else {
                    buffer.put(r.toByte())
                    buffer.put(g.toByte())
                    buffer.put(b.toByte())
                }
            }
        }

        return when (outputType) {
            DataType.FLOAT32 -> {
                val output = FloatArray(outputLabels)
                interpreter.run(buffer, output)
                val (index, _) = maxIndex(output)
                labelForIndex(index)
            }
            DataType.UINT8 -> {
                val output = ByteArray(outputLabels)
                interpreter.run(buffer, output)
                val scores = FloatArray(outputLabels) { idx ->
                    ((output[idx].toInt() and 0xFF) - outputZeroPoint) * outputScale
                }
                val (index, _) = maxIndex(scores)
                labelForIndex(index)
            }
            else -> null
        }
    }

    private fun maxIndex(array: FloatArray): Pair<Int, Float> {
        var maxIdx = 0
        var maxVal = Float.NEGATIVE_INFINITY
        array.forEachIndexed { index, value ->
            if (value > maxVal) {
                maxVal = value
                maxIdx = index
            }
        }
        return maxIdx to maxVal
    }

    private fun labelForIndex(index: Int): String? {
        if (index < 0) return null
        if (labels.isNotEmpty()) {
            return labels.getOrNull(index)
        }
        return getString(R.string.currency_label_fallback, index + 1)
    }

    private fun handleOcrText(text: String) {
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

    private fun speakLabel(label: String?) {
        val now = System.currentTimeMillis()
        if (label == null) {
            speakUnknown()
            return
        }
        if (label == lastSpokenValue && now - lastSpeakTime < SPEAK_INTERVAL_MS) return
        lastSpokenValue = label
        lastSpeakTime = now
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(
            getString(R.string.currency_spoken_label, label),
            TextToSpeech.QUEUE_FLUSH,
            params,
            "currency"
        )
    }

    private fun speakUnknown() {
        val now = System.currentTimeMillis()
        if (now - lastSpeakTime < SPEAK_INTERVAL_MS) return
        lastSpeakTime = now
        val volume = Prefs.getSpeechVolume(this)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
        }
        tts?.speak(
            getString(R.string.currency_spoken_unknown),
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
        tts?.language = Locale.forLanguageTag("pl-PL")
    }

    private fun prepareModel() {
        if (mode != Prefs.CURRENCY_MODE_MODEL) {
            updateModelStatus(getString(R.string.currency_model_status_empty))
            return
        }
        labels = loadLabels()
        if (modelSource == Prefs.CURRENCY_MODEL_SOURCE_BUILTIN) {
            if (!hasBuiltinModel()) {
                updateModelStatus(getString(R.string.currency_model_status_builtin_missing))
                interpreter?.close()
                interpreter = null
                return
            }
        }
        val modelBuffer = loadModelBuffer() ?: run {
            updateModelStatus(getString(R.string.currency_model_status_missing))
            interpreter?.close()
            interpreter = null
            return
        }
        try {
            interpreter?.close()
            interpreter = Interpreter(modelBuffer, Interpreter.Options().setNumThreads(2))
            val inputTensor = interpreter?.getInputTensor(0)
            val outputTensor = interpreter?.getOutputTensor(0)
            if (inputTensor == null || outputTensor == null) {
                updateModelStatus(getString(R.string.currency_model_status_failed))
                return
            }
            readModelInfo(inputTensor, outputTensor)
            updateModelStatus(
                if (labels.isEmpty()) {
                    getString(R.string.currency_model_status_labels_missing)
                } else {
                    getString(R.string.currency_model_status_ready, labels.size)
                }
            )
        } catch (_: Exception) {
            updateModelStatus(getString(R.string.currency_model_status_failed))
            interpreter?.close()
            interpreter = null
        }
    }

    private fun readModelInfo(input: Tensor, output: Tensor) {
        val shape = input.shape()
        val (height, width, channels) = when (shape.size) {
            4 -> Triple(shape[1], shape[2], shape[3])
            3 -> Triple(shape[0], shape[1], shape[2])
            else -> Triple(224, 224, 3)
        }
        inputWidth = width
        inputHeight = height
        inputChannels = max(1, channels)
        inputType = input.dataType()
        val bytesPerChannel = if (inputType == DataType.FLOAT32) 4 else 1
        inputBuffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * inputChannels * bytesPerChannel)
            .order(ByteOrder.nativeOrder())

        val outputShape = output.shape()
        outputLabels = outputShape.lastOrNull() ?: 0
        outputType = output.dataType()
        val quant = output.quantizationParams()
        outputScale = quant.scale
        outputZeroPoint = quant.zeroPoint
    }

    private fun isModelReady(): Boolean {
        if (interpreter == null) return false
        if (outputLabels <= 0) return false
        return true
    }

    private fun updateModelStatus(text: String) {
        mainHandler.post {
            binding.currencyModelStatus.text = text
        }
    }

    private fun loadLabels(): List<String> {
        return try {
            if (modelSource == Prefs.CURRENCY_MODEL_SOURCE_BUILTIN) {
                assets.open("pln_labels.txt").bufferedReader().readLines().filter { it.isNotBlank() }
            } else {
                val uri = Prefs.getCurrencyLabelsUri(this) ?: return emptyList()
                contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readLines().filter { it.isNotBlank() }
                } ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hasBuiltinModel(): Boolean {
        return try {
            assets.openFd("pln_model.tflite").close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun loadModelBuffer(): MappedByteBuffer? {
        return try {
            if (modelSource == Prefs.CURRENCY_MODEL_SOURCE_BUILTIN) {
                val afd = assets.openFd("pln_model.tflite")
                FileInputStream(afd.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.startOffset,
                    afd.declaredLength
                )
            } else {
                val uri = Prefs.getCurrencyModelUri(this) ?: return null
                contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).channel.map(
                        FileChannel.MapMode.READ_ONLY,
                        0,
                        pfd.statSize
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Ignore
        }
    }

    companion object {
        private const val REQ_CAMERA = 601
        private const val ANALYZE_INTERVAL_MS = 700L
        private const val SPEAK_INTERVAL_MS = 2500L
    }
}
