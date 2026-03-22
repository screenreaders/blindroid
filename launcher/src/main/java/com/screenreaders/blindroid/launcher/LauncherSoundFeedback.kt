package com.screenreaders.blindroid.launcher

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class LauncherSoundFeedback(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 80)
    private var currentVolume = 80

    private data class ToneSpec(val tone: Int, val durationMs: Int)

    fun playTap() {
        if (LauncherPrefs.isSoundFeedbackEnabled(context) &&
            LauncherPrefs.getSoundFeedbackVolume(context) > 0
        ) {
            ensureToneGenerator()
            playSequence(selectTapSequence())
        }
        hapticTap()
    }

    fun playAction(action: Int, fromGesture: Boolean = false) {
        if (LauncherPrefs.isSoundFeedbackEnabled(context) &&
            LauncherPrefs.getSoundFeedbackVolume(context) > 0
        ) {
            ensureToneGenerator()
            val sequence = selectActionSequence(action)
            playSequence(sequence)
        }
        hapticAction(action, fromGesture)
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

    private fun hapticTap() {
        if (!LauncherPrefs.isHapticFeedbackEnabled(context)) return
        vibrateOnce(20L)
    }

    private fun hapticAction(action: Int, fromGesture: Boolean) {
        if (!LauncherPrefs.isHapticFeedbackEnabled(context)) return
        if (fromGesture && !LauncherPrefs.isGestureHapticEnabled(context)) return
        val pulses = when (action) {
            LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE -> listOf(30L, 60L, 30L)
            LauncherPrefs.ACTION_TOGGLE_DOCK -> listOf(25L, 40L, 25L)
            LauncherPrefs.ACTION_OPEN_SETTINGS -> listOf(45L)
            LauncherPrefs.ACTION_OPEN_FEED -> listOf(35L, 35L)
            else -> listOf(30L)
        }
        vibrateSequence(pulses)
    }

    private fun vibrateSequence(pulses: List<Long>) {
        var delay = 0L
        pulses.forEach { duration ->
            handler.postDelayed({ vibrateOnce(duration) }, delay)
            delay += duration + 40L
        }
    }

    private fun vibrateOnce(durationMs: Long) {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val amplitude = when (LauncherPrefs.getHapticStrength(context)) {
            LauncherPrefs.HAPTIC_LIGHT -> 60
            LauncherPrefs.HAPTIC_STRONG -> 220
            else -> 140
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun release() {
        toneGenerator.release()
    }

    private fun ensureToneGenerator() {
        val target = LauncherPrefs.getSoundFeedbackVolume(context)
        if (target <= 0) return
        if (target == currentVolume) return
        toneGenerator.release()
        toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, target)
        currentVolume = target
    }

    private fun selectTapSequence(): List<ToneSpec> {
        return when (LauncherPrefs.getSoundFeedbackScheme(context)) {
            LauncherPrefs.SOUND_SCHEME_SOFT -> listOf(ToneSpec(ToneGenerator.TONE_PROP_BEEP, 40))
            LauncherPrefs.SOUND_SCHEME_SHARP -> listOf(ToneSpec(ToneGenerator.TONE_DTMF_2, 50))
            else -> listOf(ToneSpec(ToneGenerator.TONE_PROP_ACK, 60))
        }
    }

    private fun selectActionSequence(action: Int): List<ToneSpec> {
        return when (LauncherPrefs.getSoundFeedbackScheme(context)) {
            LauncherPrefs.SOUND_SCHEME_SOFT -> actionSequenceSoft(action)
            LauncherPrefs.SOUND_SCHEME_SHARP -> actionSequenceSharp(action)
            else -> actionSequenceClassic(action)
        }
    }

    private fun actionSequenceClassic(action: Int): List<ToneSpec> {
        return when (action) {
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
            LauncherPrefs.ACTION_OPEN_GEMINI -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 60),
                ToneSpec(ToneGenerator.TONE_SUP_RADIO_ACK, 80)
            )
            else -> listOf(ToneSpec(ToneGenerator.TONE_PROP_ACK, 60))
        }
    }

    private fun actionSequenceSoft(action: Int): List<ToneSpec> {
        return when (action) {
            LauncherPrefs.ACTION_OPEN_ALL_APPS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 60)
            )
            LauncherPrefs.ACTION_OPEN_WIDGETS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP2, 60)
            )
            LauncherPrefs.ACTION_OPEN_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 80)
            )
            LauncherPrefs.ACTION_OPEN_QUICK_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 50),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 50)
            )
            LauncherPrefs.ACTION_FOCUS_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 60)
            )
            LauncherPrefs.ACTION_OPEN_FEED -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 70)
            )
            LauncherPrefs.ACTION_VOICE_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 50),
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 70)
            )
            LauncherPrefs.ACTION_TOGGLE_DOCK -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 60),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP2, 60)
            )
            LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 60),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 60)
            )
            LauncherPrefs.ACTION_NEXT_PAGE, LauncherPrefs.ACTION_PREV_PAGE -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 40)
            )
            LauncherPrefs.ACTION_FLASHLIGHT -> listOf(
                ToneSpec(ToneGenerator.TONE_PROP_BEEP2, 60)
            )
            LauncherPrefs.ACTION_OPEN_DIALER -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 60)
            )
            LauncherPrefs.ACTION_OPEN_MESSAGES -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 60),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 50)
            )
            LauncherPrefs.ACTION_OPEN_GEMINI -> listOf(
                ToneSpec(ToneGenerator.TONE_SUP_PIP, 70),
                ToneSpec(ToneGenerator.TONE_PROP_BEEP, 70)
            )
            else -> listOf(ToneSpec(ToneGenerator.TONE_PROP_BEEP, 60))
        }
    }

    private fun actionSequenceSharp(action: Int): List<ToneSpec> {
        return when (action) {
            LauncherPrefs.ACTION_OPEN_ALL_APPS -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_2, 60),
                ToneSpec(ToneGenerator.TONE_DTMF_4, 60)
            )
            LauncherPrefs.ACTION_OPEN_WIDGETS -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_1, 60),
                ToneSpec(ToneGenerator.TONE_DTMF_3, 60)
            )
            LauncherPrefs.ACTION_OPEN_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_0, 90)
            )
            LauncherPrefs.ACTION_OPEN_QUICK_SETTINGS -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_6, 50),
                ToneSpec(ToneGenerator.TONE_DTMF_8, 50),
                ToneSpec(ToneGenerator.TONE_DTMF_6, 50)
            )
            LauncherPrefs.ACTION_FOCUS_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_5, 70)
            )
            LauncherPrefs.ACTION_OPEN_FEED -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70)
            )
            LauncherPrefs.ACTION_VOICE_SEARCH -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_7, 50),
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70)
            )
            LauncherPrefs.ACTION_TOGGLE_DOCK -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_2, 60),
                ToneSpec(ToneGenerator.TONE_DTMF_2, 60)
            )
            LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_3, 50),
                ToneSpec(ToneGenerator.TONE_DTMF_5, 50),
                ToneSpec(ToneGenerator.TONE_DTMF_7, 50)
            )
            LauncherPrefs.ACTION_NEXT_PAGE, LauncherPrefs.ACTION_PREV_PAGE -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_1, 50)
            )
            LauncherPrefs.ACTION_FLASHLIGHT -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_9, 80)
            )
            LauncherPrefs.ACTION_OPEN_DIALER -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_9, 70)
            )
            LauncherPrefs.ACTION_OPEN_MESSAGES -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_7, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_5, 70)
            )
            LauncherPrefs.ACTION_OPEN_GEMINI -> listOf(
                ToneSpec(ToneGenerator.TONE_DTMF_8, 70),
                ToneSpec(ToneGenerator.TONE_DTMF_0, 70)
            )
            else -> listOf(ToneSpec(ToneGenerator.TONE_DTMF_2, 60))
        }
    }
}
