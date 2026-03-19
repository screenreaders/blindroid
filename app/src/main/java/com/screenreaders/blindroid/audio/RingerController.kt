package com.screenreaders.blindroid.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

class RingerController(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: Ringtone? = null

    fun start() {
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

    private fun buildRingtone(): Ringtone {
        val uri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
            appContext,
            RingtoneManager.TYPE_RINGTONE
        ) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val rt = RingtoneManager.getRingtone(appContext, uri)
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
