package com.screenreaders.blindroid.launcher

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class LauncherSoundFeedback(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 80)

    private data class ToneSpec(val tone: Int, val durationMs: Int)

    fun playTap() {
        if (!LauncherPrefs.isSoundFeedbackEnabled(context)) return
        playSequence(listOf(ToneSpec(ToneGenerator.TONE_PROP_ACK, 60)))
    }

    fun playAction(action: Int) {
        if (!LauncherPrefs.isSoundFeedbackEnabled(context)) return
        val sequence = when (action) {
            LauncherPrefs.ACTION_OPEN_ALL_APPS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60),
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60)
            )
            LauncherPrefs.ACTION_OPEN_WIDGETS -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_1, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_3, 70)
            )
            LauncherPrefs.ACTION_OPEN_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 120)
            )
            LauncherPrefs.ACTION_OPEN_QUICK_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 50),
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 50),
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 50)
            )
            LauncherPrefs.ACTION_FOCUS_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_RADIO_ACK, 80)
            )
            LauncherPrefs.ACTION_OPEN_FEED -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_0, 80)
            )
            LauncherPrefs.ACTION_VOICE_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 60),
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 80)
            )
            LauncherPrefs.ACTION_TOGGLE_DOCK -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP2, 60)
            )
            LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60),
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60),
                ToneSpec(ToneGenerator.TONE_PROP_ACK, 60)
            )
            LauncherPrefs.ACTION_NEXT_PAGE, LauncherPrefs.ACTION_PREV_PAGE -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_5, 50)
            )
            LauncherPrefs.ACTION_FLASHLIGHT -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP2, 80)
            )
            LauncherPrefs.ACTION_OPEN_DIALER -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70)
            )
            LauncherPrefs.ACTION_OPEN_MESSAGES -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_7, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_5, 70)
            )
            else -> listOf(ToneSpec(ToneGenerator.TONE_PROP_ACK, 60))
        }
        playSequence(sequence)
    }

    private fun playSequence(sequence: List<ToneSpec>) {
        var delay = 0L
        sequence.forEach { spec ->
            handler.postDelayed({
                toneGenerator.startTone(spec.tone, spec.durationMs)
            }, delay)
            delay += spec.durationMs + 40L
        }
    }

    fun release() {
        toneGenerator.release()
    }
}
