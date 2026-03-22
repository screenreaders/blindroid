package com.screenreaders.blindroid.launcher

import android.content.Context
import android.util.TypedValue
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

object LauncherPrefs {
    private const val PREFS_NAME = "blindroid_launcher_prefs"
    private const val KEY_COLUMNS = "grid_columns"
    private const val KEY_ROWS = "grid_rows"
    private const val KEY_ICON_SIZE_DP = "icon_size_dp"
    private const val KEY_LABEL_SIZE_SP = "label_size_sp"
    private const val KEY_DOUBLE_TAP_LOCK = "double_tap_lock"
    private const val KEY_HOME_EDIT_LOCK = "home_edit_lock"
    private const val KEY_HIDE_HOME_LABELS = "hide_home_labels"
    private const val KEY_HIDE_DOCK_LABELS = "hide_dock_labels"
    private const val KEY_SUPER_SIMPLE = "super_simple_mode"
    private const val KEY_SUPER_SIMPLE_COLUMNS = "super_simple_columns"
    private const val KEY_SUPER_SIMPLE_ROWS = "super_simple_rows"
    private const val KEY_SIMPLE_FAVORITES_COLUMNS = "simple_favorites_columns"
    private const val KEY_SIMPLE_FAVORITES_ROWS = "simple_favorites_rows"
    private const val KEY_DOCK_VISIBLE = "dock_visible"
    private const val KEY_SMART_HOTSEAT = "smart_hotseat"
    private const val KEY_FEED_ENABLED = "feed_enabled"
    private const val KEY_FEED_MODE = "feed_mode"
    private const val KEY_FEED_AUTO_OPEN = "feed_auto_open"
    private const val KEY_FEED_QUICK_ACTIONS = "feed_quick_actions"
    private const val KEY_USAGE_SUGGESTIONS = "usage_suggestions"
    private const val KEY_THEME = "launcher_theme"
    private const val KEY_INVERT_COLORS = "invert_colors"
    private const val KEY_ICON_STYLE = "icon_style"
    private const val KEY_ICON_TINT = "icon_tint"
    private const val KEY_LABEL_STYLE = "label_style"
    private const val KEY_LABEL_BACKGROUND = "label_background"
    private const val KEY_THEME_PRESET = "theme_preset"
    private const val KEY_THEME_OVERRIDE = "theme_override"
    const val KEY_THEME_BG = "theme_bg"
    const val KEY_THEME_TEXT = "theme_text"
    const val KEY_THEME_MUTED = "theme_muted"
    const val KEY_THEME_ACCENT = "theme_accent"
    const val KEY_THEME_ACCENT2 = "theme_accent2"
    private const val KEY_SEARCH_BAR = "search_bar"
    private const val KEY_GOOGLE_SEARCH = "google_search"
    private const val KEY_GOOGLE_VOICE = "google_voice"
    private const val KEY_APP_SORT = "app_sort"
    private const val KEY_SUGGESTION_MODE = "suggestion_mode"
    private const val KEY_SORT_DESC = "sort_desc"
    private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
    private const val KEY_SHOW_ALL_APPS_LABELS = "show_all_apps_labels"
    private const val KEY_SHOW_FAVORITES_SECTION = "show_favorites_section"
    private const val KEY_SHOW_SUGGESTED_SECTION = "show_suggested_section"
    private const val KEY_SHOW_SUGGESTED_NOW_SECTION = "show_suggested_now_section"
    private const val KEY_SHOW_RECENT_SECTION = "show_recent_section"
    private const val KEY_SHOW_CATEGORIES_ROW = "show_categories_row"
    private const val KEY_SHOW_SCROLL_BUTTONS = "show_scroll_buttons"
    private const val KEY_SHOW_VOICE_SEARCH = "show_voice_search"
    private const val KEY_SHOW_RESULTS_COUNT = "show_results_count"
    private const val KEY_SHOW_PAGE_INDICATOR = "show_page_indicator"
    private const val KEY_SHOW_PAGE_NAV = "show_page_nav_buttons"
    private const val KEY_SHOW_SCREEN_SHORTCUTS = "show_screen_shortcuts"
    private const val KEY_PAGE_ANNOUNCE = "page_announce"
    private const val KEY_DEFAULT_HOME_PAGE = "default_home_page"
    private const val KEY_SCREEN_SHORTCUTS = "screen_shortcuts"
    private const val KEY_ALL_APPS_COLUMNS = "all_apps_columns"
    private const val KEY_ALL_APPS_DEFAULT_TAB = "all_apps_default_tab"
    private const val KEY_SUGGESTED_COUNT = "suggested_count"
    private const val KEY_SUGGESTED_NOW_COUNT = "suggested_now_count"
    private const val KEY_RECENT_COUNT = "recent_count"
    private const val KEY_FAVORITES_COUNT = "favorites_count"
    private const val KEY_PAGE_COUNT = "page_count"
    private const val KEY_NOW_ALARM = "now_alarm"
    private const val KEY_NOW_CALENDAR = "now_calendar"
    private const val KEY_NOW_WEATHER = "now_weather"
    private const val KEY_NOW_BATTERY = "now_battery"
    private const val KEY_NOW_REMINDERS = "now_reminders"
    private const val KEY_NOW_HEADPHONES = "now_headphones"
    private const val KEY_NOW_NETWORK = "now_network"
    private const val KEY_NOW_STORAGE = "now_storage"
    private const val KEY_NOW_LOCATION = "now_location"
    private const val KEY_NOW_SCREEN_TIME = "now_screen_time"
    private const val KEY_NOW_TOP_APPS = "now_top_apps"
    private const val KEY_NOW_AIRPLANE = "now_airplane"
    private const val KEY_NOW_RAM = "now_ram"
    private const val KEY_NOW_DEVICE = "now_device"
    private const val KEY_NOW_ROTATION = "now_rotation"
    private const val KEY_NOW_NFC = "now_nfc"
    private const val KEY_NOW_DND = "now_dnd"
    private const val KEY_NOW_RINGER = "now_ringer"
    private const val KEY_NOW_BLUETOOTH = "now_bluetooth"
    private const val KEY_NOW_BRIGHTNESS = "now_brightness"
    private const val KEY_NOW_VOLUME = "now_volume"
    private const val KEY_NOW_POWER = "now_power"
    private const val KEY_SOUND_FEEDBACK = "sound_feedback"
    private const val KEY_SOUND_FEEDBACK_VOLUME = "sound_feedback_volume"
    private const val KEY_SOUND_FEEDBACK_SCHEME = "sound_feedback_scheme"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_HAPTIC_STRENGTH = "haptic_strength"
    private const val KEY_GESTURE_HAPTIC = "gesture_haptic"
    private const val KEY_GN_LAYOUT = "gn_layout"
    private const val KEY_WALLPAPER_PARALLAX = "wallpaper_parallax"
    private const val KEY_PAGE_ANIMATION = "page_animation"
    private const val KEY_PAGE_ANIM_INTENSITY = "page_anim_intensity"
    private const val KEY_WIDGET_SNAP = "widget_snap"
    private const val KEY_WIDGET_STEP = "widget_step"
    private const val KEY_WIDGET_SHOW_SIZE = "widget_show_size"
    private const val KEY_ICON_PACK_ENABLED = "icon_pack_enabled"
    private const val KEY_ICON_PACK_URI = "icon_pack_uri"
    private const val KEY_ASSISTANT_MODE = "assistant_mode"
    private const val KEY_GESTURE_TWO_TAP = "gesture_two_tap"
    private const val KEY_GESTURE_TWO_UP = "gesture_two_up"
    private const val KEY_GESTURE_TWO_DOWN = "gesture_two_down"
    private const val KEY_GESTURE_TWO_LEFT = "gesture_two_left"
    private const val KEY_GESTURE_TWO_RIGHT = "gesture_two_right"
    private const val KEY_GESTURE_THREE_TAP = "gesture_three_tap"
    private const val KEY_GESTURE_THREE_UP = "gesture_three_up"
    private const val KEY_GESTURE_THREE_DOWN = "gesture_three_down"
    private const val KEY_GESTURE_THREE_LEFT = "gesture_three_left"
    private const val KEY_GESTURE_THREE_RIGHT = "gesture_three_right"

    private const val THEME_LIGHT = 0
    private const val THEME_DARK = 1
    private const val THEME_BLACK = 2
    private const val THEME_BLUE = 3
    private const val THEME_HIGH_CONTRAST = 4
    private const val THEME_YELLOW = 5

    const val FEED_MODE_LOCAL = 0
    const val FEED_MODE_GOOGLE = 1
    const val FEED_MODE_EMBEDDED = 2

    private const val ICON_STYLE_NONE = 0
    private const val ICON_STYLE_CIRCLE = 1

    private const val ICON_TINT_NONE = 0
    private const val ICON_TINT_GRAYSCALE = 1
    private const val ICON_TINT_CONTRAST = 2

    private const val LABEL_STYLE_NORMAL = 0
    private const val LABEL_STYLE_UPPER = 1

    private const val THEME_PRESET_DEFAULT = 0
    private const val THEME_PRESET_CONTRAST = 1
    private const val THEME_PRESET_SOFT = 2
    private const val THEME_PRESET_SOLAR = 3

    const val CATEGORY_RANK_COUNT = 0
    const val CATEGORY_RANK_USAGE = 1
    const val CATEGORY_RANK_ALPHA = 2
    private const val KEY_CATEGORY_RANKING = "category_ranking"

    const val ACTION_NONE = 0
    const val ACTION_TOGGLE_DOCK = 1
    const val ACTION_TOGGLE_SUPER_SIMPLE = 2
    const val ACTION_OPEN_ALL_APPS = 3
    const val ACTION_OPEN_WIDGETS = 4
    const val ACTION_OPEN_SETTINGS = 5
    const val ACTION_OPEN_QUICK_SETTINGS = 6
    const val ACTION_FOCUS_SEARCH = 7
    const val ACTION_NEXT_PAGE = 8
    const val ACTION_PREV_PAGE = 9
    const val ACTION_OPEN_FEED = 10
    const val ACTION_VOICE_SEARCH = 11
    const val ACTION_FLASHLIGHT = 12
    const val ACTION_OPEN_DIALER = 13
    const val ACTION_OPEN_MESSAGES = 14
    const val ACTION_OPEN_GEMINI = 15
    const val ACTION_TOGGLE_WIFI = 16
    const val ACTION_TOGGLE_BLUETOOTH = 17
    const val ACTION_TOGGLE_DND = 18
    const val ACTION_OPEN_NOTIFICATIONS = 19
    const val ACTION_OPEN_DISPLAY_SETTINGS = 20
    const val ACTION_OPEN_SOUND_SETTINGS = 21

    const val SOUND_SCHEME_CLASSIC = 0
    const val SOUND_SCHEME_SOFT = 1
    const val SOUND_SCHEME_SHARP = 2

    const val HAPTIC_LIGHT = 0
    const val HAPTIC_MEDIUM = 1
    const val HAPTIC_STRONG = 2

    const val ASSISTANT_VOICE = 0
    const val ASSISTANT_GEMINI = 1

    const val PAGE_ANIM_DEFAULT = 0
    const val PAGE_ANIM_CAROUSEL = 1
    const val PAGE_ANIM_DEPTH = 2
    const val PAGE_ANIM_STACK = 3
    const val PAGE_ANIM_ZOOM = 4
    const val PAGE_ANIM_FLIP = 5
    const val PAGE_ANIM_CUBE = 6
    const val PAGE_ANIM_PARALLAX = 7

    const val SORT_ALPHA = 0
    const val SORT_RECENT = 1
    const val SORT_USAGE = 2
    const val SORT_INSTALL_NEWEST = 3
    const val SORT_UPDATE_NEWEST = 4

    const val SUGGESTION_BALANCED = 0
    const val SUGGESTION_RECENT = 1
    const val SUGGESTION_FREQUENT = 2
    const val SUGGESTION_TIME = 3

    fun getColumns(context: Context): Int =
        prefs(context).getInt(KEY_COLUMNS, 4).coerceIn(3, 6)

    fun setColumns(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_COLUMNS, value.coerceIn(3, 6)).apply()
    }

    fun getRows(context: Context): Int =
        prefs(context).getInt(KEY_ROWS, 5).coerceIn(3, 7)

    fun setRows(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_ROWS, value.coerceIn(3, 7)).apply()
    }

    fun getIconSizeDp(context: Context): Int =
        prefs(context).getInt(KEY_ICON_SIZE_DP, 48).coerceIn(32, 72)

    fun setIconSizeDp(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_ICON_SIZE_DP, value.coerceIn(32, 72)).apply()
    }

    fun getLabelSizeSp(context: Context): Float =
        prefs(context).getFloat(KEY_LABEL_SIZE_SP, 12f).coerceIn(10f, 20f)

    fun setLabelSizeSp(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_LABEL_SIZE_SP, value.coerceIn(10f, 20f)).apply()
    }

    fun isDoubleTapLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOUBLE_TAP_LOCK, true)

    fun setDoubleTapLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOUBLE_TAP_LOCK, enabled).apply()
    }

    fun isHomeLabelsHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_HOME_LABELS, false)

    fun setHomeLabelsHidden(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_HOME_LABELS, hidden).apply()
    }

    fun isHomeEditLocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_EDIT_LOCK, false)

    fun setHomeEditLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean(KEY_HOME_EDIT_LOCK, locked).apply()
    }

    fun isDockLabelsHidden(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HIDE_DOCK_LABELS, false)

    fun setDockLabelsHidden(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean(KEY_HIDE_DOCK_LABELS, hidden).apply()
    }

    fun isSuperSimpleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SUPER_SIMPLE, false)

    fun setSuperSimpleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SUPER_SIMPLE, enabled).apply()
    }

    fun getSuperSimpleColumns(context: Context): Int =
        prefs(context).getInt(KEY_SUPER_SIMPLE_COLUMNS, 3).coerceIn(2, 4)

    fun setSuperSimpleColumns(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SUPER_SIMPLE_COLUMNS, value.coerceIn(2, 4)).apply()
    }

    fun getSuperSimpleRows(context: Context): Int =
        prefs(context).getInt(KEY_SUPER_SIMPLE_ROWS, 4).coerceIn(2, 5)

    fun setSuperSimpleRows(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SUPER_SIMPLE_ROWS, value.coerceIn(2, 5)).apply()
    }

    fun getSimpleFavoritesColumns(context: Context): Int =
        prefs(context).getInt(KEY_SIMPLE_FAVORITES_COLUMNS, 2).coerceIn(2, 3)

    fun setSimpleFavoritesColumns(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SIMPLE_FAVORITES_COLUMNS, value.coerceIn(2, 3)).apply()
    }

    fun getSimpleFavoritesRows(context: Context): Int =
        prefs(context).getInt(KEY_SIMPLE_FAVORITES_ROWS, 2).coerceIn(2, 4)

    fun setSimpleFavoritesRows(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SIMPLE_FAVORITES_ROWS, value.coerceIn(2, 4)).apply()
    }

    fun isDockVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_VISIBLE, true)

    fun setDockVisible(context: Context, visible: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_VISIBLE, visible).apply()
    }

    fun isSmartHotseatEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SMART_HOTSEAT, false)

    fun setSmartHotseatEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SMART_HOTSEAT, enabled).apply()
    }

    fun isFeedEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FEED_ENABLED, true)

    fun setFeedEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FEED_ENABLED, enabled).apply()
    }

    fun getFeedMode(context: Context): Int =
        prefs(context).getInt(KEY_FEED_MODE, FEED_MODE_LOCAL).coerceIn(FEED_MODE_LOCAL, FEED_MODE_EMBEDDED)

    fun setFeedMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_FEED_MODE, mode.coerceIn(FEED_MODE_LOCAL, FEED_MODE_EMBEDDED)).apply()
    }

    fun isFeedAutoOpenEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FEED_AUTO_OPEN, true)

    fun setFeedAutoOpenEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FEED_AUTO_OPEN, enabled).apply()
    }

    fun isFeedQuickActionsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FEED_QUICK_ACTIONS, true)

    fun setFeedQuickActionsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FEED_QUICK_ACTIONS, enabled).apply()
    }

    fun isUsageSuggestionsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_USAGE_SUGGESTIONS, true)

    fun setUsageSuggestionsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_USAGE_SUGGESTIONS, enabled).apply()
    }

    fun getTheme(context: Context): Int =
        prefs(context).getInt(KEY_THEME, THEME_LIGHT).coerceIn(THEME_LIGHT, THEME_YELLOW)

    fun setTheme(context: Context, theme: Int) {
        prefs(context).edit().putInt(KEY_THEME, theme.coerceIn(THEME_LIGHT, THEME_YELLOW)).apply()
    }

    fun isInvertColorsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_INVERT_COLORS, false)

    fun setInvertColorsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_INVERT_COLORS, enabled).apply()
    }

    fun getIconStyle(context: Context): Int =
        prefs(context).getInt(KEY_ICON_STYLE, ICON_STYLE_NONE).coerceIn(ICON_STYLE_NONE, ICON_STYLE_CIRCLE)

    fun setIconStyle(context: Context, style: Int) {
        prefs(context).edit().putInt(KEY_ICON_STYLE, style.coerceIn(ICON_STYLE_NONE, ICON_STYLE_CIRCLE)).apply()
    }

    fun getIconTint(context: Context): Int =
        prefs(context).getInt(KEY_ICON_TINT, ICON_TINT_NONE).coerceIn(ICON_TINT_NONE, ICON_TINT_CONTRAST)

    fun setIconTint(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_ICON_TINT, mode.coerceIn(ICON_TINT_NONE, ICON_TINT_CONTRAST)).apply()
    }

    fun getLabelStyle(context: Context): Int =
        prefs(context).getInt(KEY_LABEL_STYLE, LABEL_STYLE_NORMAL).coerceIn(LABEL_STYLE_NORMAL, LABEL_STYLE_UPPER)

    fun setLabelStyle(context: Context, style: Int) {
        prefs(context).edit().putInt(KEY_LABEL_STYLE, style.coerceIn(LABEL_STYLE_NORMAL, LABEL_STYLE_UPPER)).apply()
    }

    fun isLabelBackgroundEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LABEL_BACKGROUND, false)

    fun setLabelBackgroundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LABEL_BACKGROUND, enabled).apply()
    }

    fun isSearchBarEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SEARCH_BAR, true)

    fun setSearchBarEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SEARCH_BAR, enabled).apply()
    }

    fun getAppSortMode(context: Context): Int =
        prefs(context).getInt(KEY_APP_SORT, SORT_ALPHA).coerceIn(SORT_ALPHA, SORT_UPDATE_NEWEST)

    fun setAppSortMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_APP_SORT, mode.coerceIn(SORT_ALPHA, SORT_UPDATE_NEWEST)).apply()
    }

    fun getSuggestionMode(context: Context): Int =
        prefs(context).getInt(KEY_SUGGESTION_MODE, SUGGESTION_BALANCED)
            .coerceIn(SUGGESTION_BALANCED, SUGGESTION_TIME)

    fun setSuggestionMode(context: Context, mode: Int) {
        prefs(context).edit()
            .putInt(KEY_SUGGESTION_MODE, mode.coerceIn(SUGGESTION_BALANCED, SUGGESTION_TIME))
            .apply()
    }

    fun isSortDescending(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SORT_DESC, false)

    fun setSortDescending(context: Context, desc: Boolean) {
        prefs(context).edit().putBoolean(KEY_SORT_DESC, desc).apply()
    }

    fun isShowSystemApps(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SYSTEM_APPS, true)

    fun setShowSystemApps(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SYSTEM_APPS, show).apply()
    }

    fun isAllAppsLabelsShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_ALL_APPS_LABELS, true)

    fun setAllAppsLabelsShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_ALL_APPS_LABELS, show).apply()
    }

    fun isFavoritesSectionShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_FAVORITES_SECTION, true)

    fun setFavoritesSectionShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_FAVORITES_SECTION, show).apply()
    }

    fun isSuggestedSectionShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SUGGESTED_SECTION, true)

    fun setSuggestedSectionShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SUGGESTED_SECTION, show).apply()
    }

    fun isSuggestedNowSectionShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SUGGESTED_NOW_SECTION, true)

    fun setSuggestedNowSectionShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SUGGESTED_NOW_SECTION, show).apply()
    }

    fun isRecentSectionShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_RECENT_SECTION, true)

    fun setRecentSectionShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_RECENT_SECTION, show).apply()
    }

    fun isCategoriesRowShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_CATEGORIES_ROW, true)

    fun setCategoriesRowShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_CATEGORIES_ROW, show).apply()
    }

    fun isScrollButtonsShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SCROLL_BUTTONS, true)

    fun setScrollButtonsShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SCROLL_BUTTONS, show).apply()
    }

    fun isVoiceSearchShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_VOICE_SEARCH, true)

    fun setVoiceSearchShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_VOICE_SEARCH, show).apply()
    }

    fun isResultsCountShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_RESULTS_COUNT, true)

    fun setResultsCountShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_RESULTS_COUNT, show).apply()
    }

    fun isPageIndicatorShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_PAGE_INDICATOR, true)

    fun setPageIndicatorShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_PAGE_INDICATOR, show).apply()
    }

    fun isPageNavShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_PAGE_NAV, true)

    fun setPageNavShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_PAGE_NAV, show).apply()
    }

    fun isPageAnnouncementEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PAGE_ANNOUNCE, true)

    fun setPageAnnouncementEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PAGE_ANNOUNCE, enabled).apply()
    }

    fun isScreenShortcutsShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_SCREEN_SHORTCUTS, true)

    fun setScreenShortcutsShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_SCREEN_SHORTCUTS, show).apply()
    }

    fun getScreenShortcuts(context: Context): List<ScreenShortcut> {
        val raw = prefs(context).getString(KEY_SCREEN_SHORTCUTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<ScreenShortcut>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val page = obj.optInt("page", -1)
                val label = obj.optString("label", "").trim()
                if (page > 0 && label.isNotBlank()) {
                    result.add(ScreenShortcut(page, label))
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setScreenShortcuts(context: Context, shortcuts: List<ScreenShortcut>) {
        val unique = LinkedHashMap<String, ScreenShortcut>()
        shortcuts.forEach { item ->
            val page = item.page
            val label = item.label.trim()
            if (page <= 0 || label.isBlank()) return@forEach
            val key = "${page}_${label.lowercase()}"
            if (!unique.containsKey(key)) {
                unique[key] = ScreenShortcut(page, label)
            }
        }
        val array = JSONArray()
        unique.values.take(12).forEach { item ->
            val obj = JSONObject()
            obj.put("page", item.page)
            obj.put("label", item.label)
            array.put(obj)
        }
        prefs(context).edit().putString(KEY_SCREEN_SHORTCUTS, array.toString()).apply()
    }

    fun trimScreenShortcuts(context: Context, pageCount: Int): Boolean {
        val current = getScreenShortcuts(context)
        val filtered = current.filter { it.page in 1..pageCount }
        if (filtered.size == current.size) return false
        setScreenShortcuts(context, filtered)
        return true
    }

    fun getDefaultHomePage(context: Context): Int =
        prefs(context).getInt(KEY_DEFAULT_HOME_PAGE, 1).coerceIn(1, 5)

    fun setDefaultHomePage(context: Context, page: Int) {
        prefs(context).edit().putInt(KEY_DEFAULT_HOME_PAGE, page.coerceIn(1, 5)).apply()
    }

    fun getAllAppsColumns(context: Context): Int =
        prefs(context).getInt(KEY_ALL_APPS_COLUMNS, 0).coerceIn(0, 6)

    fun setAllAppsColumns(context: Context, columns: Int) {
        prefs(context).edit().putInt(KEY_ALL_APPS_COLUMNS, columns.coerceIn(0, 6)).apply()
    }

    fun getAllAppsDefaultTab(context: Context): Int =
        prefs(context).getInt(KEY_ALL_APPS_DEFAULT_TAB, 0).coerceIn(0, 1)

    fun setAllAppsDefaultTab(context: Context, tab: Int) {
        prefs(context).edit().putInt(KEY_ALL_APPS_DEFAULT_TAB, tab.coerceIn(0, 1)).apply()
    }

    fun getSuggestedCount(context: Context): Int =
        prefs(context).getInt(KEY_SUGGESTED_COUNT, 4).coerceIn(0, 24)

    fun setSuggestedCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_SUGGESTED_COUNT, count.coerceIn(0, 24)).apply()
    }

    fun getSuggestedNowCount(context: Context): Int =
        prefs(context).getInt(KEY_SUGGESTED_NOW_COUNT, 4).coerceIn(0, 24)

    fun setSuggestedNowCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_SUGGESTED_NOW_COUNT, count.coerceIn(0, 24)).apply()
    }

    fun getRecentCount(context: Context): Int =
        prefs(context).getInt(KEY_RECENT_COUNT, 8).coerceIn(0, 48)

    fun setRecentCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_RECENT_COUNT, count.coerceIn(0, 48)).apply()
    }

    fun getFavoritesCount(context: Context): Int =
        prefs(context).getInt(KEY_FAVORITES_COUNT, 0).coerceIn(0, 48)

    fun setFavoritesCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_FAVORITES_COUNT, count.coerceIn(0, 48)).apply()
    }

    fun getPageCount(context: Context): Int =
        prefs(context).getInt(KEY_PAGE_COUNT, 3).coerceIn(1, 5)

    fun setPageCount(context: Context, count: Int) {
        prefs(context).edit().putInt(KEY_PAGE_COUNT, count.coerceIn(1, 5)).apply()
    }

    fun isGoogleSearchEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GOOGLE_SEARCH, true)

    fun setGoogleSearchEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GOOGLE_SEARCH, enabled).apply()
    }

    fun isGoogleVoiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GOOGLE_VOICE, true)

    fun setGoogleVoiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GOOGLE_VOICE, enabled).apply()
    }

    fun isNowAlarmEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_ALARM, true)

    fun setNowAlarmEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_ALARM, enabled).apply()
    }

    fun isNowCalendarEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_CALENDAR, true)

    fun setNowCalendarEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_CALENDAR, enabled).apply()
    }

    fun isNowWeatherEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_WEATHER, true)

    fun setNowWeatherEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_WEATHER, enabled).apply()
    }

    fun isNowBatteryEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_BATTERY, true)

    fun setNowBatteryEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_BATTERY, enabled).apply()
    }

    fun isNowRemindersEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_REMINDERS, true)

    fun setNowRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_REMINDERS, enabled).apply()
    }

    fun isNowHeadphonesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_HEADPHONES, true)

    fun setNowHeadphonesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_HEADPHONES, enabled).apply()
    }

    fun isNowLocationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_LOCATION, true)

    fun setNowLocationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_LOCATION, enabled).apply()
    }

    fun isNowNetworkEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_NETWORK, true)

    fun setNowNetworkEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_NETWORK, enabled).apply()
    }

    fun isNowStorageEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_STORAGE, true)

    fun setNowStorageEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_STORAGE, enabled).apply()
    }

    fun isNowScreenTimeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_SCREEN_TIME, true)

    fun setNowScreenTimeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_SCREEN_TIME, enabled).apply()
    }

    fun isNowTopAppsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_TOP_APPS, true)

    fun setNowTopAppsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_TOP_APPS, enabled).apply()
    }

    fun isNowAirplaneEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_AIRPLANE, true)

    fun setNowAirplaneEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_AIRPLANE, enabled).apply()
    }

    fun isNowRamEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_RAM, true)

    fun setNowRamEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_RAM, enabled).apply()
    }

    fun isNowDeviceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_DEVICE, true)

    fun setNowDeviceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_DEVICE, enabled).apply()
    }

    fun isNowRotationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_ROTATION, true)

    fun setNowRotationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_ROTATION, enabled).apply()
    }

    fun isNowNfcEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_NFC, true)

    fun setNowNfcEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_NFC, enabled).apply()
    }

    fun isNowDndEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_DND, true)

    fun setNowDndEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_DND, enabled).apply()
    }

    fun isNowRingerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_RINGER, true)

    fun setNowRingerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_RINGER, enabled).apply()
    }

    fun isNowBluetoothEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_BLUETOOTH, true)

    fun setNowBluetoothEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_BLUETOOTH, enabled).apply()
    }

    fun isNowBrightnessEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_BRIGHTNESS, true)

    fun setNowBrightnessEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_BRIGHTNESS, enabled).apply()
    }

    fun isNowVolumeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_VOLUME, true)

    fun setNowVolumeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_VOLUME, enabled).apply()
    }

    fun isNowPowerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOW_POWER, true)

    fun setNowPowerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOW_POWER, enabled).apply()
    }

    fun isSoundFeedbackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOUND_FEEDBACK, true)

    fun setSoundFeedbackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND_FEEDBACK, enabled).apply()
    }

    fun getSoundFeedbackVolume(context: Context): Int =
        prefs(context).getInt(KEY_SOUND_FEEDBACK_VOLUME, 80).coerceIn(0, 100)

    fun setSoundFeedbackVolume(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_SOUND_FEEDBACK_VOLUME, value.coerceIn(0, 100)).apply()
    }

    fun getSoundFeedbackScheme(context: Context): Int =
        prefs(context).getInt(KEY_SOUND_FEEDBACK_SCHEME, SOUND_SCHEME_CLASSIC)
            .coerceIn(SOUND_SCHEME_CLASSIC, SOUND_SCHEME_SHARP)

    fun setSoundFeedbackScheme(context: Context, scheme: Int) {
        prefs(context).edit().putInt(
            KEY_SOUND_FEEDBACK_SCHEME,
            scheme.coerceIn(SOUND_SCHEME_CLASSIC, SOUND_SCHEME_SHARP)
        ).apply()
    }

    fun isHapticFeedbackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAPTIC_FEEDBACK, true)

    fun setHapticFeedbackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }

    fun isGestureHapticEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GESTURE_HAPTIC, true)

    fun setGestureHapticEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GESTURE_HAPTIC, enabled).apply()
    }

    fun getHapticStrength(context: Context): Int =
        prefs(context).getInt(KEY_HAPTIC_STRENGTH, HAPTIC_MEDIUM)
            .coerceIn(HAPTIC_LIGHT, HAPTIC_STRONG)

    fun setHapticStrength(context: Context, strength: Int) {
        prefs(context).edit()
            .putInt(KEY_HAPTIC_STRENGTH, strength.coerceIn(HAPTIC_LIGHT, HAPTIC_STRONG))
            .apply()
    }

    fun isGnLayoutEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GN_LAYOUT, false)

    fun setGnLayoutEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GN_LAYOUT, enabled).apply()
    }

    fun isWallpaperParallaxEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WALLPAPER_PARALLAX, true)

    fun setWallpaperParallaxEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WALLPAPER_PARALLAX, enabled).apply()
    }

    fun getPageAnimation(context: Context): Int =
        prefs(context).getInt(KEY_PAGE_ANIMATION, PAGE_ANIM_DEFAULT).coerceIn(PAGE_ANIM_DEFAULT, PAGE_ANIM_PARALLAX)

    fun setPageAnimation(context: Context, mode: Int) {
        prefs(context).edit()
            .putInt(KEY_PAGE_ANIMATION, mode.coerceIn(PAGE_ANIM_DEFAULT, PAGE_ANIM_PARALLAX))
            .apply()
    }

    fun getPageAnimationIntensity(context: Context): Int =
        prefs(context).getInt(KEY_PAGE_ANIM_INTENSITY, 70).coerceIn(0, 100)

    fun setPageAnimationIntensity(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_PAGE_ANIM_INTENSITY, value.coerceIn(0, 100)).apply()
    }

    fun isWidgetSnapEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_SNAP, true)

    fun setWidgetSnapEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIDGET_SNAP, enabled).apply()
    }

    fun getWidgetResizeStepDp(context: Context): Int =
        prefs(context).getInt(KEY_WIDGET_STEP, 20).coerceIn(8, 80)

    fun setWidgetResizeStepDp(context: Context, step: Int) {
        prefs(context).edit().putInt(KEY_WIDGET_STEP, step.coerceIn(8, 80)).apply()
    }

    fun isWidgetSizeShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WIDGET_SHOW_SIZE, true)

    fun setWidgetSizeShown(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_WIDGET_SHOW_SIZE, show).apply()
    }

    fun isIconPackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ICON_PACK_ENABLED, false)

    fun setIconPackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ICON_PACK_ENABLED, enabled).apply()
    }

    fun getIconPackUri(context: Context): String? =
        prefs(context).getString(KEY_ICON_PACK_URI, null)

    fun setIconPackUri(context: Context, uri: String?) {
        if (uri.isNullOrBlank()) {
            prefs(context).edit().remove(KEY_ICON_PACK_URI).apply()
        } else {
            prefs(context).edit().putString(KEY_ICON_PACK_URI, uri).apply()
        }
    }

    fun getCategoryRanking(context: Context): Int =
        prefs(context).getInt(KEY_CATEGORY_RANKING, CATEGORY_RANK_COUNT)
            .coerceIn(CATEGORY_RANK_COUNT, CATEGORY_RANK_ALPHA)

    fun setCategoryRanking(context: Context, mode: Int) {
        prefs(context).edit()
            .putInt(KEY_CATEGORY_RANKING, mode.coerceIn(CATEGORY_RANK_COUNT, CATEGORY_RANK_ALPHA))
            .apply()
    }

    fun getAssistantMode(context: Context): Int =
        prefs(context).getInt(KEY_ASSISTANT_MODE, ASSISTANT_GEMINI).coerceIn(ASSISTANT_VOICE, ASSISTANT_GEMINI)

    fun setAssistantMode(context: Context, mode: Int) {
        prefs(context).edit()
            .putInt(KEY_ASSISTANT_MODE, mode.coerceIn(ASSISTANT_VOICE, ASSISTANT_GEMINI))
            .apply()
    }

    fun getGestureTwoFingerTap(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_TWO_TAP, ACTION_TOGGLE_DOCK)

    fun setGestureTwoFingerTap(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_TWO_TAP, action)

    fun getGestureTwoFingerUp(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_TWO_UP, ACTION_OPEN_WIDGETS)

    fun setGestureTwoFingerUp(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_TWO_UP, action)

    fun getGestureTwoFingerDown(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_TWO_DOWN, ACTION_OPEN_SETTINGS)

    fun setGestureTwoFingerDown(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_TWO_DOWN, action)

    fun getGestureTwoFingerLeft(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_TWO_LEFT, ACTION_PREV_PAGE)

    fun setGestureTwoFingerLeft(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_TWO_LEFT, action)

    fun getGestureTwoFingerRight(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_TWO_RIGHT, ACTION_NEXT_PAGE)

    fun setGestureTwoFingerRight(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_TWO_RIGHT, action)

    fun getGestureThreeFingerTap(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_THREE_TAP, ACTION_TOGGLE_SUPER_SIMPLE)

    fun setGestureThreeFingerTap(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_THREE_TAP, action)

    fun getGestureThreeFingerUp(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_THREE_UP, ACTION_OPEN_ALL_APPS)

    fun setGestureThreeFingerUp(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_THREE_UP, action)

    fun getGestureThreeFingerDown(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_THREE_DOWN, ACTION_OPEN_QUICK_SETTINGS)

    fun setGestureThreeFingerDown(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_THREE_DOWN, action)

    fun getGestureThreeFingerLeft(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_THREE_LEFT, ACTION_FOCUS_SEARCH)

    fun setGestureThreeFingerLeft(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_THREE_LEFT, action)

    fun getGestureThreeFingerRight(context: Context): Int =
        getGestureAction(context, KEY_GESTURE_THREE_RIGHT, ACTION_OPEN_QUICK_SETTINGS)

    fun setGestureThreeFingerRight(context: Context, action: Int) =
        setGestureAction(context, KEY_GESTURE_THREE_RIGHT, action)

    fun getUiConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val metrics = context.resources.displayMetrics
        val superSimple = isSuperSimpleEnabled(context)
        val gnLayout = isGnLayoutEnabled(context)
        val columns = when {
            superSimple -> getSuperSimpleColumns(context)
            gnLayout -> 4
            else -> getColumns(context)
        }
        val rows = when {
            superSimple -> getSuperSimpleRows(context)
            gnLayout -> 5
            else -> getRows(context)
        }
        val iconDp = when {
            superSimple -> {
                val densityScale = when (getSuperSimpleColumns(context)) {
                    2 -> 86
                    3 -> 72
                    else -> 60
                }
                densityScale
            }
            gnLayout -> 48
            else -> getIconSizeDp(context)
        }.toFloat()
        val iconPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDp, metrics)
            .roundToInt()
        val labelSize = when {
            superSimple -> 20f
            gnLayout -> 12f
            else -> getLabelSizeSp(context)
        }
        val showLabels = if (superSimple) true else !isHomeLabelsHidden(context)
        return LauncherUiConfig(
            columns = columns,
            rows = rows,
            iconSizePx = iconPx,
            labelSizeSp = labelSize,
            itemHeightPx = itemHeightPx,
            showLabels = showLabels
        )
    }

    fun getDockConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val base = getUiConfig(context, itemHeightPx)
        val showLabels = !(isDockLabelsHidden(context) || isSuperSimpleEnabled(context))
        return base.copy(showLabels = showLabels)
    }

    fun getSimpleFavoritesConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val metrics = context.resources.displayMetrics
        val columns = getSimpleFavoritesColumns(context)
        val rows = getSimpleFavoritesRows(context)
        val iconDp = when (columns) {
            2 -> 84f
            else -> 64f
        }
        val iconPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDp, metrics).roundToInt()
        return LauncherUiConfig(
            columns = columns,
            rows = rows,
            iconSizePx = iconPx,
            labelSizeSp = 20f,
            itemHeightPx = itemHeightPx,
            showLabels = true
        )
    }

    data class ThemeColors(val background: Int, val text: Int, val muted: Int, val accent: Int, val accentAlt: Int)

    fun getThemeColors(context: Context): ThemeColors {
        val preset = getThemePreset(context)
        val base = when (getTheme(context)) {
            THEME_DARK -> ThemeColors(
                0xFF202124.toInt(), 0xFFECEFF1.toInt(), 0xFFB0BEC5.toInt(),
                0xFF64B5F6.toInt(), 0xFF81C784.toInt()
            )
            THEME_BLACK -> ThemeColors(
                0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB0BEC5.toInt(),
                0xFF64B5F6.toInt(), 0xFFFFB74D.toInt()
            )
            THEME_BLUE -> ThemeColors(
                0xFFE3F2FD.toInt(), 0xFF0D47A1.toInt(), 0xFF1E88E5.toInt(),
                0xFF1565C0.toInt(), 0xFF43A047.toInt()
            )
            THEME_HIGH_CONTRAST -> ThemeColors(
                0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF424242.toInt(),
                0xFF000000.toInt(), 0xFF1A237E.toInt()
            )
            THEME_YELLOW -> ThemeColors(
                0xFF000000.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFF59D.toInt(),
                0xFFFFEB3B.toInt(), 0xFFFFC107.toInt()
            )
            else -> ThemeColors(
                0xFFF5F5F5.toInt(), 0xFF1F1F1F.toInt(), 0xFF616161.toInt(),
                0xFF1976D2.toInt(), 0xFF388E3C.toInt()
            )
        }
        val presetAdjusted = when (preset) {
            THEME_PRESET_CONTRAST -> base.copy(
                background = base.background,
                text = base.text,
                muted = blendColor(base.muted, base.text, 0.4f),
                accent = base.text,
                accentAlt = base.accentAlt
            )
            THEME_PRESET_SOFT -> base.copy(
                background = blendColor(base.background, 0xFFFFFFFF.toInt(), 0.2f),
                muted = blendColor(base.muted, base.background, 0.2f),
                accent = blendColor(base.accent, base.background, 0.2f),
                accentAlt = blendColor(base.accentAlt, base.background, 0.2f)
            )
            THEME_PRESET_SOLAR -> base.copy(
                background = 0xFFFDF6E3.toInt(),
                text = 0xFF073642.toInt(),
                muted = 0xFF586E75.toInt(),
                accent = 0xFF268BD2.toInt(),
                accentAlt = 0xFF2AA198.toInt()
            )
            else -> base
        }
        val override = if (isThemeOverrideEnabled(context)) {
            ThemeColors(
                getThemeColor(context, KEY_THEME_BG, presetAdjusted.background),
                getThemeColor(context, KEY_THEME_TEXT, presetAdjusted.text),
                getThemeColor(context, KEY_THEME_MUTED, presetAdjusted.muted),
                getThemeColor(context, KEY_THEME_ACCENT, presetAdjusted.accent),
                getThemeColor(context, KEY_THEME_ACCENT2, presetAdjusted.accentAlt)
            )
        } else {
            presetAdjusted
        }
        return if (isInvertColorsEnabled(context)) {
            ThemeColors(override.text, override.background, override.muted, override.accent, override.accentAlt)
        } else {
            override
        }
    }

    fun getThemePreset(context: Context): Int =
        prefs(context).getInt(KEY_THEME_PRESET, THEME_PRESET_DEFAULT)
            .coerceIn(THEME_PRESET_DEFAULT, THEME_PRESET_SOLAR)

    fun setThemePreset(context: Context, preset: Int) {
        prefs(context).edit()
            .putInt(KEY_THEME_PRESET, preset.coerceIn(THEME_PRESET_DEFAULT, THEME_PRESET_SOLAR))
            .apply()
    }

    fun isThemeOverrideEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_THEME_OVERRIDE, false)

    fun setThemeOverrideEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_THEME_OVERRIDE, enabled).apply()
    }

    fun setThemeColor(context: Context, key: String, color: Int) {
        prefs(context).edit().putInt(key, color).apply()
    }

    fun getThemeColor(context: Context, key: String, fallback: Int): Int {
        return prefs(context).getInt(key, fallback)
    }

    fun getThemeColorHex(context: Context, key: String, fallback: Int): String {
        val color = getThemeColor(context, key, fallback)
        return String.format("#%06X", 0xFFFFFF and color)
    }

    fun setThemeColorHex(context: Context, key: String, hex: String) {
        val cleaned = hex.trim().removePrefix("#")
        if (cleaned.length != 6) return
        val value = cleaned.toIntOrNull(16) ?: return
        setThemeColor(context, key, (0xFF shl 24) or value)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getGestureAction(context: Context, key: String, defaultValue: Int): Int {
        return prefs(context).getInt(key, defaultValue)
    }

    private fun setGestureAction(context: Context, key: String, action: Int) {
        prefs(context).edit().putInt(key, action).apply()
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        val r = ((from shr 16 and 0xFF) * inverse + (to shr 16 and 0xFF) * ratio).toInt()
        val g = ((from shr 8 and 0xFF) * inverse + (to shr 8 and 0xFF) * ratio).toInt()
        val b = ((from and 0xFF) * inverse + (to and 0xFF) * ratio).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
