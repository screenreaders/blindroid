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
    private const val KEY_ANNOUNCE_DURING_CALL = "announce_during_call"
    private const val KEY_VOICE_COMMANDS = "voice_commands"
    private const val KEY_CALL_STATE_ANNOUNCE = "call_state_announce"
    private const val KEY_CALL_STATE_VIBRATE = "call_state_vibrate"
    private const val KEY_END_CALL_VIBRATE = "end_call_vibrate"
    private const val KEY_PRIVACY_MODE = "privacy_mode"
    private const val KEY_PRIVACY_TITLE_ONLY = "privacy_title_only"
    private const val KEY_READ_SMS = "read_sms"
    private const val KEY_READ_NOTIFICATIONS = "read_notifications"
    private const val KEY_READ_UNLOCKED = "read_unlocked"
    private const val KEY_AUTO_UPDATE = "auto_update"
    private const val KEY_LAST_UPDATE_CHECK = "last_update_check"
    private const val KEY_UPDATE_DOWNLOAD_ID = "update_download_id"
    private const val KEY_SPEAKER_OVERRIDE = "speaker_override"
    private const val KEY_END_CALL_KEY = "end_call_key"
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val KEY_CHIME_INTERVAL = "chime_interval"
    private const val KEY_CHIME_START = "chime_start"
    private const val KEY_CHIME_END = "chime_end"
    private const val KEY_RECENT_NOTIFICATIONS = "recent_notifications"
    private const val KEY_MODULE_SHORTCUTS = "module_shortcuts"
    private const val KEY_CURRENCY_MODE = "currency_mode"
    private const val KEY_CURRENCY_MODEL_URI = "currency_model_uri"
    private const val KEY_CURRENCY_LABELS_URI = "currency_labels_uri"
    private const val KEY_CURRENCY_MODEL_SOURCE = "currency_model_source"
    private const val KEY_LIGHT_SOUND = "light_sound_cues"
    private const val KEY_DOC_AUTO = "doc_auto_capture"
    private const val KEY_DOC_SPEAK = "doc_speak_result"
    private const val KEY_CRASH_REPORTING = "crash_reporting"
    private const val KEY_CRASH_WIFI_ONLY = "crash_wifi_only"
    private const val KEY_CRASH_FOREGROUND_ONLY = "crash_foreground_only"
    private const val KEY_CRASH_DEVICE_INFO = "crash_device_info"

    const val MODE_RING_AND_SPEECH = 0
    const val MODE_SPEECH_ONLY = 1
    const val MODE_SPEECH_THEN_RING = 2
    const val SPEAKER_OVERRIDE_NONE = 0
    const val SPEAKER_OVERRIDE_SPEAKER = 1
    const val SPEAKER_OVERRIDE_EARPIECE = 2
    const val END_CALL_NONE = 0
    const val END_CALL_VOLUME_UP = 1
    const val END_CALL_VOLUME_DOWN = 2
    const val END_CALL_HEADSET = 3
    const val END_CALL_POWER = 4
    const val CURRENCY_MODE_OCR = 0
    const val CURRENCY_MODE_MODEL = 1
    const val CURRENCY_MODEL_SOURCE_FILE = 0
    const val CURRENCY_MODEL_SOURCE_BUILTIN = 1

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

    fun isCrashWifiOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_WIFI_ONLY, true)

    fun setCrashWifiOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_WIFI_ONLY, value).apply()
    }

    fun isCrashForegroundOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_FOREGROUND_ONLY, true)

    fun setCrashForegroundOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_FOREGROUND_ONLY, value).apply()
    }

    fun isCrashDeviceInfoEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_DEVICE_INFO, false)

    fun setCrashDeviceInfoEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_DEVICE_INFO, value).apply()
    }

    fun getAnnounceMode(context: Context): Int =
        prefs(context).getInt(KEY_ANNOUNCE_MODE, MODE_RING_AND_SPEECH)

    fun setAnnounceMode(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_ANNOUNCE_MODE, value).apply()
    }

    fun isAnnounceDuringCallEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ANNOUNCE_DURING_CALL, true)

    fun setAnnounceDuringCallEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ANNOUNCE_DURING_CALL, value).apply()
    }

    fun isVoiceCommandsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VOICE_COMMANDS, false)

    fun setVoiceCommandsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_VOICE_COMMANDS, value).apply()
    }

    fun isCallStateAnnounceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALL_STATE_ANNOUNCE, true)

    fun setCallStateAnnounceEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALL_STATE_ANNOUNCE, value).apply()
    }

    fun isCallStateVibrateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CALL_STATE_VIBRATE, false)

    fun setCallStateVibrateEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CALL_STATE_VIBRATE, value).apply()
    }

    fun isEndCallVibrateEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_END_CALL_VIBRATE, false)

    fun setEndCallVibrateEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_END_CALL_VIBRATE, value).apply()
    }

    fun isPrivacyModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PRIVACY_MODE, false)

    fun setPrivacyModeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRIVACY_MODE, value).apply()
    }

    fun isPrivacyTitleOnlyEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PRIVACY_TITLE_ONLY, false)

    fun setPrivacyTitleOnlyEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_PRIVACY_TITLE_ONLY, value).apply()
    }

    fun getEndCallKey(context: Context): Int =
        prefs(context).getInt(KEY_END_CALL_KEY, END_CALL_NONE)

    fun setEndCallKey(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_END_CALL_KEY, value).apply()
    }

    fun getSpeakerOverride(context: Context): Int =
        prefs(context).getInt(KEY_SPEAKER_OVERRIDE, SPEAKER_OVERRIDE_NONE).coerceIn(0, 2)

    fun setSpeakerOverride(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SPEAKER_OVERRIDE, value.coerceIn(0, 2)).apply()
    }

    fun clearSpeakerOverride(context: Context) {
        prefs(context).edit().remove(KEY_SPEAKER_OVERRIDE).apply()
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

    fun addRecentNotification(context: Context, message: String) {
        val prefs = prefs(context)
        val current = prefs.getString(KEY_RECENT_NOTIFICATIONS, "").orEmpty()
        val list = current.split('|').filter { it.isNotBlank() }.toMutableList()
        list.remove(message)
        list.add(0, message)
        val trimmed = list.take(5)
        prefs.edit().putString(KEY_RECENT_NOTIFICATIONS, trimmed.joinToString("|")).apply()
    }

    fun getRecentNotifications(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_RECENT_NOTIFICATIONS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split('|').filter { it.isNotBlank() }.take(5)
    }

    fun isModuleShortcutsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MODULE_SHORTCUTS, true)

    fun setModuleShortcutsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_MODULE_SHORTCUTS, value).apply()
    }

    fun getCurrencyMode(context: Context): Int =
        prefs(context).getInt(KEY_CURRENCY_MODE, CURRENCY_MODE_OCR).coerceIn(0, 1)

    fun setCurrencyMode(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_CURRENCY_MODE, value.coerceIn(0, 1)).apply()
    }

    fun getCurrencyModelUri(context: Context): String? =
        prefs(context).getString(KEY_CURRENCY_MODEL_URI, null)

    fun setCurrencyModelUri(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_CURRENCY_MODEL_URI, value).apply()
    }

    fun getCurrencyLabelsUri(context: Context): String? =
        prefs(context).getString(KEY_CURRENCY_LABELS_URI, null)

    fun setCurrencyLabelsUri(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_CURRENCY_LABELS_URI, value).apply()
    }

    fun getCurrencyModelSource(context: Context): Int =
        prefs(context).getInt(KEY_CURRENCY_MODEL_SOURCE, CURRENCY_MODEL_SOURCE_FILE).coerceIn(0, 1)

    fun setCurrencyModelSource(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_CURRENCY_MODEL_SOURCE, value.coerceIn(0, 1)).apply()
    }

    fun isLightSoundCuesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LIGHT_SOUND, false)

    fun setLightSoundCuesEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LIGHT_SOUND, value).apply()
    }

    fun isDocAutoCaptureEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOC_AUTO, true)

    fun setDocAutoCaptureEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOC_AUTO, value).apply()
    }

    fun isDocSpeakResultEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOC_SPEAK, true)

    fun setDocSpeakResultEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOC_SPEAK, value).apply()
    }

    fun isCrashReportingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_REPORTING, true)

    fun setCrashReportingEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_REPORTING, value).apply()
    }
}
