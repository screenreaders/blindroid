package com.screenreaders.blindroid.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object RingerController {
    private var appContext: Context? = null
    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        ensureContext(context)
        if (ringtone == null) {
            ringtone = buildRingtone()
        }
        ringtone?.let {
            if (!it.isPlaying) {
                it.play()
            }
        }
    }

    fun stop() {
        ringtone?.stop()
    }

    private fun ensureContext(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    private fun buildRingtone(): Ringtone {
        val context = appContext
            ?: throw IllegalStateException("RingerController requires context")
        val uri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE
        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val rt = RingtoneManager.getRingtone(context, uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            rt.isLooping = true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rt.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        return rt
    }
}
