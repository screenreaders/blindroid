package com.screenreaders.blindroid.data

import android.content.Context

object Prefs {
    private const val NAME = "blindroid_prefs"
    private const val KEY_ANNOUNCE = "announce_caller"
    private const val KEY_SPEAKER = "auto_speaker"
    private const val KEY_VOICE = "tts_voice"
    private const val KEY_SPEECH_RATE = "tts_rate"
    private const val KEY_SPEECH_VOLUME = "tts_volume"
    private const val KEY_REPEAT_COUNT = "tts_repeat"
    private const val KEY_ANNOUNCE_MODE = "announce_mode"
    private const val KEY_READ_SMS = "read_sms"
    private const val KEY_READ_NOTIFICATIONS = "read_notifications"
    private const val KEY_READ_UNLOCKED = "read_unlocked"
    private const val KEY_AUTO_UPDATE = "auto_update"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    private const val KEY_UPDATE_DOWNLOAD_ID = "update_download_id"
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val KEY_CHIME_INTERVAL = "chime_interval"
    private const val KEY_CHIME_START = "chime_start"
    private const val KEY_CHIME_END = "chime_end"

    const val MODE_RING_AND_SPEECH = 0
    const val MODE_SPEECH_ONLY = 1
    const val MODE_SPEECH_THEN_RING = 2

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isAnnounceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ANNOUNCE, true)

    fun setAnnounceEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ANNOUNCE, value).apply()
    }

    fun isAutoSpeakerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SPEAKER, true)

    fun setAutoSpeakerEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SPEAKER, value).apply()
    }

    fun getVoiceName(context: Context): String? =
        prefs(context).getString(KEY_VOICE, null)

    fun setVoiceName(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_VOICE, value).apply()
    }

    fun getSpeechRate(context: Context): Float =
        prefs(context).getFloat(KEY_SPEECH_RATE, 1.0f)

    fun setSpeechRate(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_SPEECH_RATE, value).apply()
    }

    fun getSpeechVolume(context: Context): Float =
        prefs(context).getFloat(KEY_SPEECH_VOLUME, 1.0f)

    fun setSpeechVolume(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_SPEECH_VOLUME, value).apply()
    }

    fun getRepeatCount(context: Context): Int =
        prefs(context).getInt(KEY_REPEAT_COUNT, 1).coerceIn(1, 3)

    fun setRepeatCount(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_REPEAT_COUNT, value).apply()
    }

    fun getAnnounceMode(context: Context): Int =
        prefs(context).getInt(KEY_ANNOUNCE_MODE, MODE_RING_AND_SPEECH)

    fun setAnnounceMode(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_ANNOUNCE_MODE, value).apply()
    }

    fun isSmsReadEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_READ_SMS, false)

    fun setSmsReadEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_READ_SMS, value).apply()
    }

    fun isNotificationsReadEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_READ_NOTIFICATIONS, false)

    fun setNotificationsReadEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_READ_NOTIFICATIONS, value).apply()
    }

    fun isReadWhenUnlockedEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_READ_UNLOCKED, false)

    fun setReadWhenUnlockedEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_READ_UNLOCKED, value).apply()
    }

    fun isAutoUpdateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_UPDATE, false)

    fun setAutoUpdateEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_UPDATE, value).apply()
    }

    fun getLastUpdateCheck(context: Context): Long =
        prefs(context).getLong(KEY_LAST_UPDATE_CHECK, 0L)

    fun setLastUpdateCheck(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()
    }

    fun getUpdateDownloadId(context: Context): Long =
        prefs(context).getLong(KEY_UPDATE_DOWNLOAD_ID, 0L)

    fun setUpdateDownloadId(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_UPDATE_DOWNLOAD_ID, value).apply()
    }

    fun isChimeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CHIME_ENABLED, false)

    fun setChimeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CHIME_ENABLED, value).apply()
    }

    fun getChimeInterval(context: Context): Int =
        prefs(context).getInt(KEY_CHIME_INTERVAL, 60).coerceIn(15, 60)

    fun setChimeInterval(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_CHIME_INTERVAL, value).apply()
    }

    fun getChimeStartMinutes(context: Context): Int =
        prefs(context).getInt(KEY_CHIME_START, 6 * 60).coerceIn(0, 24 * 60)

    fun setChimeStartMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_CHIME_START, value.coerceIn(0, 24 * 60)).apply()
    }

    fun getChimeEndMinutes(context: Context): Int =
        prefs(context).getInt(KEY_CHIME_END, 0).coerceIn(0, 24 * 60)

    fun setChimeEndMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_CHIME_END, value.coerceIn(0, 24 * 60)).apply()
    }
}
