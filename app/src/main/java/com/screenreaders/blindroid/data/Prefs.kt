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
    private const val KEY_LAST_UPDATE_NOTIFIED = "last_update_notified"
    private const val KEY_UPDATE_DOWNLOAD_ID = "update_download_id"
    private const val KEY_UPDATE_DOWNLOAD_SHA = "update_download_sha"
    private const val KEY_UPDATE_DOWNLOAD_SIZE = "update_download_size"
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
    private const val KEY_CRASH_CHARGING_ONLY = "crash_charging_only"
    private const val KEY_CRASH_CLIENT_ID = "crash_client_id"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_DIAGNOSTICS = "diagnostics_enabled"
    private const val KEY_LOW_VISION = "low_vision_enabled"
    private const val KEY_LOW_VISION_STYLE = "low_vision_style"
    private const val KEY_LOW_VISION_INVERT = "low_vision_invert"
    private const val KEY_LOW_VISION_SCALE = "low_vision_scale"
    private const val KEY_LOW_VISION_PRESET = "low_vision_preset"
    private const val KEY_FACE_ASSIST = "face_assist_enabled"
    private const val KEY_FACE_PICKUP = "face_pickup_enabled"
    private const val KEY_FACE_SHORTCUT = "face_shortcut_enabled"
    private const val KEY_FACE_SOUND = "face_sound_enabled"
    private const val KEY_ANSWER_PICKUP = "answer_pickup_enabled"
    private const val KEY_ANSWER_SHAKE = "answer_shake_enabled"
    private const val KEY_OBSTACLE_SOUND = "obstacle_sound_enabled"
    private const val KEY_SOS_NUMBER = "sos_number"
    private const val KEY_SOS_MESSAGE = "sos_message"
    private const val KEY_SOS_SHAKE = "sos_shake"
    private const val KEY_SOS_SHAKE_SENS = "sos_shake_sens"
    private const val KEY_MISSED_CALL_NUMBER = "missed_call_number"
    private const val KEY_MISSED_CALL_TIME = "missed_call_time"
    private const val KEY_MISSED_CALL_BACK = "missed_call_back"
    private const val KEY_BACK_TAP_SENS = "back_tap_sens"
    private const val KEY_QUIET_ENABLED = "quiet_enabled"
    private const val KEY_QUIET_START = "quiet_start"
    private const val KEY_QUIET_END = "quiet_end"
    private const val KEY_QUIET_MUTE_CALLS = "quiet_mute_calls"
    private const val KEY_QUIET_MUTE_SMS = "quiet_mute_sms"
    private const val KEY_QUIET_MUTE_NOTIFICATIONS = "quiet_mute_notifications"
    private const val KEY_QUIET_MUTE_CHIME = "quiet_mute_chime"
    private const val KEY_NAV_API_KEY = "nav_api_key"
    private const val KEY_NAV_TRACKING = "nav_tracking"
    private const val KEY_NAV_CATEGORIES = "nav_categories"
    private const val KEY_NAV_RADIUS = "nav_radius"
    private const val KEY_NAV_SPEAK_INTERVAL = "nav_speak_interval"
    private const val KEY_NAV_MOVING_ONLY = "nav_moving_only"
    private const val KEY_NAV_MIN_DISTANCE = "nav_min_distance"
    private const val KEY_NAV_TRACK_LOG = "nav_track_log"
    private const val KEY_NAV_LAST_TRACK = "nav_last_track"
    private const val KEY_NAV_LAST_PLACE = "nav_last_place"
    private const val KEY_NAV_LAST_CITY = "nav_last_city"
    private const val KEY_NAV_LAST_MODE = "nav_last_mode"
    private const val KEY_NAV_POI_SOURCE = "nav_poi_source"
    private const val KEY_NAV_OFFLINE_COUNT = "nav_offline_count"
    private const val KEY_NAV_OFFLINE_UPDATED = "nav_offline_updated"
    private const val KEY_NAV_IMPORT_RADIUS = "nav_import_radius"
    private const val KEY_NAV_IMPORT_MODE = "nav_import_mode"
    private const val KEY_NAV_IMPORT_MIN_LAT = "nav_import_min_lat"
    private const val KEY_NAV_IMPORT_MIN_LON = "nav_import_min_lon"
    private const val KEY_NAV_IMPORT_MAX_LAT = "nav_import_max_lat"
    private const val KEY_NAV_IMPORT_MAX_LON = "nav_import_max_lon"
    private const val KEY_NAV_OFFLINE_BASE_URL = "nav_offline_base_url"
    private const val KEY_NAV_OFFLINE_ZOOM = "nav_offline_zoom"
    private const val KEY_NAV_AUTO_IMPORT = "nav_auto_import"
    private const val KEY_NAV_AUTO_INTERVAL = "nav_auto_interval"
    private const val KEY_NAV_AUTO_WIFI = "nav_auto_wifi"
    private const val KEY_NAV_AUTO_CHARGING = "nav_auto_charging"
    private const val KEY_NAV_ROUTE_ENABLED = "nav_route_enabled"
    private const val KEY_NAV_ROUTE_LAT = "nav_route_lat"
    private const val KEY_NAV_ROUTE_LON = "nav_route_lon"
    private const val KEY_NAV_ROUTE_LABEL = "nav_route_label"
    private const val KEY_NAV_ROUTE_INTERVAL = "nav_route_interval"
    private const val KEY_NAV_ROUTE_MIN_DISTANCE = "nav_route_min_distance"
    private const val KEY_NAV_ROUTE_GPX_PATH = "nav_route_gpx_path"
    private const val KEY_NAV_ROUTE_GPX_ENABLED = "nav_route_gpx_enabled"
    private const val KEY_NAV_ROUTE_GPX_COUNT = "nav_route_gpx_count"
    private const val KEY_NAV_ROUTE_GPX_INDEX = "nav_route_gpx_index"
    private const val KEY_NAV_OFFLINE_GZIP = "nav_offline_gzip"
    private const val KEY_NAV_OFFLINE_SEGMENTED = "nav_offline_segmented"
    private const val KEY_NAV_OFFLINE_SEGMENT_SIZE = "nav_offline_segment_size"
    private const val KEY_TYFLO_LAST_LINK = "tyflo_last_link"

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
    const val NAV_POI_SOURCE_GOOGLE = 0
    const val NAV_POI_SOURCE_OFFLINE = 1
    const val NAV_POI_SOURCE_OSM = 2
    const val NAV_POI_SOURCE_HYBRID = 3
    const val NAV_IMPORT_MODE_RADIUS = 0
    const val NAV_IMPORT_MODE_MANUAL = 1

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

    fun isCrashChargingOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CRASH_CHARGING_ONLY, false)

    fun setCrashChargingOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CRASH_CHARGING_ONLY, value).apply()
    }

    fun getCrashClientId(context: Context): String? =
        prefs(context).getString(KEY_CRASH_CLIENT_ID, null)

    fun setCrashClientId(context: Context, value: String) {
        prefs(context).edit().putString(KEY_CRASH_CLIENT_ID, value).apply()
    }

    fun isOnboardingDone(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
    }

    fun isDiagnosticsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DIAGNOSTICS, false)

    fun setDiagnosticsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIAGNOSTICS, value).apply()
    }

    fun isLowVisionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOW_VISION, false)

    fun setLowVisionEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOW_VISION, value).apply()
    }

    fun getLowVisionStyle(context: Context): Int =
        prefs(context).getInt(KEY_LOW_VISION_STYLE, 0)

    fun setLowVisionStyle(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_LOW_VISION_STYLE, value).apply()
    }

    fun isLowVisionInvert(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOW_VISION_INVERT, false)

    fun setLowVisionInvert(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOW_VISION_INVERT, value).apply()
    }

    fun getLowVisionScale(context: Context): Int =
        prefs(context).getInt(KEY_LOW_VISION_SCALE, 100).coerceIn(100, 150)

    fun setLowVisionScale(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_LOW_VISION_SCALE, value.coerceIn(100, 150)).apply()
    }

    fun getLowVisionPreset(context: Context): Int =
        prefs(context).getInt(KEY_LOW_VISION_PRESET, 0)

    fun setLowVisionPreset(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_LOW_VISION_PRESET, value).apply()
    }

    fun isFaceAssistEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FACE_ASSIST, true)

    fun setFaceAssistEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_FACE_ASSIST, value).apply()
    }

    fun isFacePickupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FACE_PICKUP, false)

    fun setFacePickupEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_FACE_PICKUP, value).apply()
    }

    fun isFaceShortcutEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FACE_SHORTCUT, true)

    fun setFaceShortcutEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_FACE_SHORTCUT, value).apply()
    }

    fun isFaceSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FACE_SOUND, true)

    fun setFaceSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_FACE_SOUND, value).apply()
    }

    fun isAnswerPickupEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ANSWER_PICKUP, false)

    fun setAnswerPickupEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ANSWER_PICKUP, value).apply()
    }

    fun isAnswerShakeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ANSWER_SHAKE, false)

    fun setAnswerShakeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ANSWER_SHAKE, value).apply()
    }

    fun isObstacleSoundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OBSTACLE_SOUND, true)

    fun setObstacleSoundEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_OBSTACLE_SOUND, value).apply()
    }

    fun getSosNumber(context: Context): String =
        prefs(context).getString(KEY_SOS_NUMBER, "") ?: ""

    fun setSosNumber(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SOS_NUMBER, value.trim()).apply()
    }

    fun getSosMessage(context: Context): String =
        prefs(context).getString(KEY_SOS_MESSAGE, "Potrzebuję pomocy.") ?: "Potrzebuję pomocy."

    fun setSosMessage(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SOS_MESSAGE, value).apply()
    }

    fun isSosShakeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOS_SHAKE, false)

    fun setSosShakeEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOS_SHAKE, value).apply()
    }

    fun getSosShakeSensitivity(context: Context): Int =
        prefs(context).getInt(KEY_SOS_SHAKE_SENS, 1).coerceIn(0, 2)

    fun setSosShakeSensitivity(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SOS_SHAKE_SENS, value.coerceIn(0, 2)).apply()
    }

    fun setLastMissedCall(context: Context, number: String?) {
        val safe = number?.trim().orEmpty()
        val editor = prefs(context).edit()
        if (safe.isBlank()) {
            editor.remove(KEY_MISSED_CALL_NUMBER).remove(KEY_MISSED_CALL_TIME).apply()
        } else {
            editor.putString(KEY_MISSED_CALL_NUMBER, safe)
                .putLong(KEY_MISSED_CALL_TIME, System.currentTimeMillis())
                .apply()
        }
    }

    fun getLastMissedCallNumber(context: Context): String? =
        prefs(context).getString(KEY_MISSED_CALL_NUMBER, null)

    fun getLastMissedCallTime(context: Context): Long =
        prefs(context).getLong(KEY_MISSED_CALL_TIME, 0L)

    fun isMissedCallBackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MISSED_CALL_BACK, false)

    fun setMissedCallBackEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_MISSED_CALL_BACK, value).apply()
    }

    fun getBackTapSensitivity(context: Context): Int =
        prefs(context).getInt(KEY_BACK_TAP_SENS, 1).coerceIn(0, 2)

    fun setBackTapSensitivity(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_BACK_TAP_SENS, value.coerceIn(0, 2)).apply()
    }

    fun isQuietEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_ENABLED, false)

    fun setQuietEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUIET_ENABLED, value).apply()
    }

    fun getQuietStartMinutes(context: Context): Int =
        prefs(context).getInt(KEY_QUIET_START, 22 * 60)

    fun setQuietStartMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_QUIET_START, value.coerceIn(0, 24 * 60 - 1)).apply()
    }

    fun getQuietEndMinutes(context: Context): Int =
        prefs(context).getInt(KEY_QUIET_END, 7 * 60)

    fun setQuietEndMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_QUIET_END, value.coerceIn(0, 24 * 60 - 1)).apply()
    }

    fun isQuietMuteCalls(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_MUTE_CALLS, true)

    fun setQuietMuteCalls(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUIET_MUTE_CALLS, value).apply()
    }

    fun isQuietMuteSms(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_MUTE_SMS, true)

    fun setQuietMuteSms(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUIET_MUTE_SMS, value).apply()
    }

    fun isQuietMuteNotifications(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_MUTE_NOTIFICATIONS, true)

    fun setQuietMuteNotifications(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUIET_MUTE_NOTIFICATIONS, value).apply()
    }

    fun isQuietMuteChime(context: Context): Boolean =
        prefs(context).getBoolean(KEY_QUIET_MUTE_CHIME, true)

    fun setQuietMuteChime(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_QUIET_MUTE_CHIME, value).apply()
    }

    fun getNavigationApiKey(context: Context): String =
        prefs(context).getString(KEY_NAV_API_KEY, "") ?: ""

    fun setNavigationApiKey(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_API_KEY, value.trim()).apply()
    }

    fun isNavigationTrackingEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_TRACKING, false)

    fun setNavigationTrackingEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_TRACKING, value).apply()
    }

    fun getNavigationCategories(context: Context): String =
        prefs(context).getString(KEY_NAV_CATEGORIES, "") ?: ""

    fun setNavigationCategories(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_CATEGORIES, value).apply()
    }

    fun getNavigationRadius(context: Context): Int =
        prefs(context).getInt(KEY_NAV_RADIUS, 100).coerceIn(50, 500)

    fun setNavigationRadius(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_RADIUS, value.coerceIn(50, 500)).apply()
    }

    fun getNavigationSpeakIntervalSec(context: Context): Int =
        prefs(context).getInt(KEY_NAV_SPEAK_INTERVAL, 60).coerceIn(10, 600)

    fun setNavigationSpeakIntervalSec(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_SPEAK_INTERVAL, value.coerceIn(10, 600)).apply()
    }

    fun isNavigationMovingOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_MOVING_ONLY, false)

    fun setNavigationMovingOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_MOVING_ONLY, value).apply()
    }

    fun getNavigationMinDistance(context: Context): Int =
        prefs(context).getInt(KEY_NAV_MIN_DISTANCE, 80).coerceIn(30, 300)

    fun setNavigationMinDistance(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_MIN_DISTANCE, value.coerceIn(30, 300)).apply()
    }

    fun isNavigationTrackLogEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_TRACK_LOG, false)

    fun setNavigationTrackLogEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_TRACK_LOG, value).apply()
    }

    fun getNavigationLastTrack(context: Context): String? =
        prefs(context).getString(KEY_NAV_LAST_TRACK, null)

    fun setNavigationLastTrack(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_NAV_LAST_TRACK, value).apply()
    }

    fun getNavigationLastPlace(context: Context): String =
        prefs(context).getString(KEY_NAV_LAST_PLACE, "") ?: ""

    fun setNavigationLastPlace(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_LAST_PLACE, value).apply()
    }

    fun getNavigationLastCity(context: Context): String =
        prefs(context).getString(KEY_NAV_LAST_CITY, "") ?: ""

    fun setNavigationLastCity(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_LAST_CITY, value).apply()
    }

    fun getNavigationLastMode(context: Context): String =
        prefs(context).getString(KEY_NAV_LAST_MODE, "driving") ?: "driving"

    fun setNavigationLastMode(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_LAST_MODE, value).apply()
    }

    fun getNavigationPoiSource(context: Context): Int =
        prefs(context).getInt(KEY_NAV_POI_SOURCE, NAV_POI_SOURCE_OFFLINE)

    fun setNavigationPoiSource(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_POI_SOURCE, value).apply()
    }

    fun getNavigationOfflineCount(context: Context): Int =
        prefs(context).getInt(KEY_NAV_OFFLINE_COUNT, 0)

    fun setNavigationOfflineCount(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_OFFLINE_COUNT, value.coerceAtLeast(0)).apply()
    }

    fun getNavigationOfflineUpdated(context: Context): Long =
        prefs(context).getLong(KEY_NAV_OFFLINE_UPDATED, 0L)

    fun setNavigationOfflineUpdated(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_NAV_OFFLINE_UPDATED, value.coerceAtLeast(0L)).apply()
    }

    fun getNavigationImportRadius(context: Context): Int =
        prefs(context).getInt(KEY_NAV_IMPORT_RADIUS, 1000).coerceIn(200, 20_000)

    fun setNavigationImportRadius(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_IMPORT_RADIUS, value.coerceIn(200, 20_000)).apply()
    }

    fun getNavigationImportMode(context: Context): Int =
        prefs(context).getInt(KEY_NAV_IMPORT_MODE, NAV_IMPORT_MODE_RADIUS)

    fun setNavigationImportMode(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_IMPORT_MODE, value).apply()
    }

    fun getNavigationImportMinLat(context: Context): Float =
        prefs(context).getFloat(KEY_NAV_IMPORT_MIN_LAT, 0f)

    fun setNavigationImportMinLat(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_NAV_IMPORT_MIN_LAT, value).apply()
    }

    fun getNavigationImportMinLon(context: Context): Float =
        prefs(context).getFloat(KEY_NAV_IMPORT_MIN_LON, 0f)

    fun setNavigationImportMinLon(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_NAV_IMPORT_MIN_LON, value).apply()
    }

    fun getNavigationImportMaxLat(context: Context): Float =
        prefs(context).getFloat(KEY_NAV_IMPORT_MAX_LAT, 0f)

    fun setNavigationImportMaxLat(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_NAV_IMPORT_MAX_LAT, value).apply()
    }

    fun getNavigationImportMaxLon(context: Context): Float =
        prefs(context).getFloat(KEY_NAV_IMPORT_MAX_LON, 0f)

    fun setNavigationImportMaxLon(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_NAV_IMPORT_MAX_LON, value).apply()
    }

    fun getNavigationOfflineBaseUrl(context: Context): String =
        prefs(context).getString(KEY_NAV_OFFLINE_BASE_URL, DEFAULT_NAV_OFFLINE_URL) ?: DEFAULT_NAV_OFFLINE_URL

    fun setNavigationOfflineBaseUrl(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_OFFLINE_BASE_URL, value.trim()).apply()
    }

    private const val DEFAULT_NAV_OFFLINE_URL = "https://maps.asteja.eu/tiles"

    fun getNavigationOfflineZoom(context: Context): Int =
        prefs(context).getInt(KEY_NAV_OFFLINE_ZOOM, 15).coerceIn(10, 18)

    fun setNavigationOfflineZoom(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_OFFLINE_ZOOM, value.coerceIn(10, 18)).apply()
    }

    fun isNavigationAutoImportEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_AUTO_IMPORT, false)

    fun setNavigationAutoImportEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_AUTO_IMPORT, value).apply()
    }

    fun getNavigationAutoImportIntervalHours(context: Context): Int =
        prefs(context).getInt(KEY_NAV_AUTO_INTERVAL, 24).coerceIn(6, 48)

    fun setNavigationAutoImportIntervalHours(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_AUTO_INTERVAL, value.coerceIn(6, 48)).apply()
    }

    fun isNavigationAutoImportWifiOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_AUTO_WIFI, true)

    fun setNavigationAutoImportWifiOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_AUTO_WIFI, value).apply()
    }

    fun isNavigationAutoImportChargingOnly(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_AUTO_CHARGING, false)

    fun setNavigationAutoImportChargingOnly(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_AUTO_CHARGING, value).apply()
    }

    fun isNavigationRouteEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_ROUTE_ENABLED, false)

    fun setNavigationRouteEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_ROUTE_ENABLED, value).apply()
    }

    fun getNavigationRouteLat(context: Context): Double =
        prefs(context).getFloat(KEY_NAV_ROUTE_LAT, Float.NaN).toDouble()

    fun setNavigationRouteLat(context: Context, value: Double) {
        prefs(context).edit().putFloat(KEY_NAV_ROUTE_LAT, value.toFloat()).apply()
    }

    fun getNavigationRouteLon(context: Context): Double =
        prefs(context).getFloat(KEY_NAV_ROUTE_LON, Float.NaN).toDouble()

    fun setNavigationRouteLon(context: Context, value: Double) {
        prefs(context).edit().putFloat(KEY_NAV_ROUTE_LON, value.toFloat()).apply()
    }

    fun getNavigationRouteLabel(context: Context): String =
        prefs(context).getString(KEY_NAV_ROUTE_LABEL, "") ?: ""

    fun setNavigationRouteLabel(context: Context, value: String) {
        prefs(context).edit().putString(KEY_NAV_ROUTE_LABEL, value).apply()
    }

    fun getNavigationRouteIntervalSec(context: Context): Int =
        prefs(context).getInt(KEY_NAV_ROUTE_INTERVAL, 20).coerceIn(5, 180)

    fun setNavigationRouteIntervalSec(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_ROUTE_INTERVAL, value.coerceIn(5, 180)).apply()
    }

    fun getNavigationRouteMinDistance(context: Context): Int =
        prefs(context).getInt(KEY_NAV_ROUTE_MIN_DISTANCE, 12).coerceIn(5, 100)

    fun setNavigationRouteMinDistance(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_ROUTE_MIN_DISTANCE, value.coerceIn(5, 100)).apply()
    }

    fun getNavigationRouteGpxPath(context: Context): String? =
        prefs(context).getString(KEY_NAV_ROUTE_GPX_PATH, null)

    fun setNavigationRouteGpxPath(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_NAV_ROUTE_GPX_PATH, value).apply()
    }

    fun isNavigationRouteGpxEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_ROUTE_GPX_ENABLED, false)

    fun setNavigationRouteGpxEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_ROUTE_GPX_ENABLED, value).apply()
    }

    fun getNavigationRouteGpxCount(context: Context): Int =
        prefs(context).getInt(KEY_NAV_ROUTE_GPX_COUNT, 0).coerceAtLeast(0)

    fun setNavigationRouteGpxCount(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_ROUTE_GPX_COUNT, value.coerceAtLeast(0)).apply()
    }

    fun getNavigationRouteGpxIndex(context: Context): Int =
        prefs(context).getInt(KEY_NAV_ROUTE_GPX_INDEX, 0).coerceAtLeast(0)

    fun setNavigationRouteGpxIndex(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_ROUTE_GPX_INDEX, value.coerceAtLeast(0)).apply()
    }

    fun isNavigationOfflineGzipEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_OFFLINE_GZIP, true)

    fun setNavigationOfflineGzipEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_OFFLINE_GZIP, value).apply()
    }

    fun isNavigationOfflineSegmented(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NAV_OFFLINE_SEGMENTED, true)

    fun setNavigationOfflineSegmented(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_NAV_OFFLINE_SEGMENTED, value).apply()
    }

    fun getNavigationOfflineSegmentSize(context: Context): Int =
        prefs(context).getInt(KEY_NAV_OFFLINE_SEGMENT_SIZE, 16).coerceIn(4, 128)

    fun setNavigationOfflineSegmentSize(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_NAV_OFFLINE_SEGMENT_SIZE, value.coerceIn(4, 128)).apply()
    }

    fun getTyflomapLastLink(context: Context): String? =
        prefs(context).getString(KEY_TYFLO_LAST_LINK, null)

    fun setTyflomapLastLink(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_TYFLO_LAST_LINK, value).apply()
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

    fun getLastUpdateNotifiedVersion(context: Context): String? =
        prefs(context).getString(KEY_LAST_UPDATE_NOTIFIED, null)

    fun setLastUpdateNotifiedVersion(context: Context, value: String) {
        prefs(context).edit().putString(KEY_LAST_UPDATE_NOTIFIED, value).apply()
    }

    fun getUpdateDownloadId(context: Context): Long =
        prefs(context).getLong(KEY_UPDATE_DOWNLOAD_ID, 0L)

    fun setUpdateDownloadId(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_UPDATE_DOWNLOAD_ID, value).apply()
    }

    fun getUpdateDownloadSha(context: Context): String? =
        prefs(context).getString(KEY_UPDATE_DOWNLOAD_SHA, null)

    fun setUpdateDownloadSha(context: Context, value: String?) {
        prefs(context).edit().putString(KEY_UPDATE_DOWNLOAD_SHA, value).apply()
    }

    fun getUpdateDownloadSize(context: Context): Long =
        prefs(context).getLong(KEY_UPDATE_DOWNLOAD_SIZE, 0L)

    fun setUpdateDownloadSize(context: Context, value: Long) {
        prefs(context).edit().putLong(KEY_UPDATE_DOWNLOAD_SIZE, value).apply()
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
