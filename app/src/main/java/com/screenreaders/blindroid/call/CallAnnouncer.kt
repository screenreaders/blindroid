package com.screenreaders.blindroid.call

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class CallAnnouncer(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var ready = false
    private val shutdownRunnable = Runnable { shutdown() }
    private val utteranceCounter = AtomicInteger(0)
    private var pending: SpeakRequest? = null
    private var lastUtteranceId: String? = null
    private var onComplete: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // No-op
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null && utteranceId == lastUtteranceId) {
                        mainHandler.post {
                            onComplete?.invoke()
                            onComplete = null
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId != null && utteranceId == lastUtteranceId) {
                        mainHandler.post {
                            onComplete?.invoke()
                            onComplete = null
                        }
                    }
                }
            })
            pending?.let {
                pending = null
                internalSpeak(it)
            }
        } else {
            pending?.let {
                pending = null
                mainHandler.post {
                    it.onComplete?.invoke()
                    onComplete = null
                }
            }
        }
    }

    fun speak(
        text: String,
        repeatCount: Int,
        rate: Float,
        volume: Float,
        voiceName: String?,
        onComplete: (() -> Unit)? = null
    ) {
        if (tts == null) {
            tts = TextToSpeech(appContext, this).apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            }
        }
        val request = SpeakRequest(text, repeatCount, rate, volume, voiceName, onComplete)
        if (ready) {
            internalSpeak(request)
        } else {
            pending = request
        }
    }

    private fun internalSpeak(request: SpeakRequest) {
        val engine = tts ?: return
        val repeatCount = request.repeatCount.coerceIn(1, 3)
        val rate = request.rate.coerceIn(0.5f, 2.0f)
        val volume = request.volume.coerceIn(0.0f, 1.0f)

        engine.setSpeechRate(rate)
        if (!request.voiceName.isNullOrBlank()) {
            val voice = engine.voices?.firstOrNull { it.name == request.voiceName }
            if (voice != null) {
                engine.voice = voice
            }
        }

        onComplete = request.onComplete
        for (i in 0 until repeatCount) {
            val utteranceId = "blindroid_call_${utteranceCounter.incrementAndGet()}"
            if (i == repeatCount - 1) {
                lastUtteranceId = utteranceId
            }
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume)
            }
            engine.speak(
                request.text,
                if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,
                params,
                utteranceId
            )
            if (i < repeatCount - 1) {
                engine.playSilentUtterance(400, TextToSpeech.QUEUE_ADD, "silent_$utteranceId")
            }
        }
        scheduleShutdown()
    }

    private fun scheduleShutdown() {
        mainHandler.removeCallbacks(shutdownRunnable)
        mainHandler.postDelayed(shutdownRunnable, 30_000)
    }

    fun shutdown() {
        ready = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private data class SpeakRequest(
        val text: String,
        val repeatCount: Int,
        val rate: Float,
        val volume: Float,
        val voiceName: String?,
        val onComplete: (() -> Unit)?
    )
}
