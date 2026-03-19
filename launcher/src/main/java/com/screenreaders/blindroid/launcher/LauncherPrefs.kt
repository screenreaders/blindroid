package com.screenreaders.blindroid.launcher

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

object LauncherPrefs {
    private const val PREFS_NAME = "blindroid_launcher_prefs"
    private const val KEY_COLUMNS = "grid_columns"
    private const val KEY_ROWS = "grid_rows"
    private const val KEY_ICON_SIZE_DP = "icon_size_dp"
    private const val KEY_LABEL_SIZE_SP = "label_size_sp"
    private const val KEY_DOUBLE_TAP_LOCK = "double_tap_lock"
    private const val KEY_HIDE_DOCK_LABELS = "hide_dock_labels"
    private const val KEY_SUPER_SIMPLE = "super_simple_mode"
    private const val KEY_DOCK_VISIBLE = "dock_visible"
    private const val KEY_FEED_ENABLED = "feed_enabled"
    private const val KEY_FEED_MODE = "feed_mode"
    private const val KEY_THEME = "launcher_theme"
    private const val KEY_ICON_STYLE = "icon_style"
    private const val KEY_SEARCH_BAR = "search_bar"
    private const val KEY_SOUND_FEEDBACK = "sound_feedback"
    private const val KEY_GN_LAYOUT = "gn_layout"
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

    const val FEED_MODE_LOCAL = 0
    const val FEED_MODE_GOOGLE = 1

    private const val ICON_STYLE_NONE = 0
    private const val ICON_STYLE_CIRCLE = 1

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

    fun isDockVisible(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DOCK_VISIBLE, true)

    fun setDockVisible(context: Context, visible: Boolean) {
        prefs(context).edit().putBoolean(KEY_DOCK_VISIBLE, visible).apply()
    }

    fun isFeedEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FEED_ENABLED, true)

    fun setFeedEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FEED_ENABLED, enabled).apply()
    }

    fun getFeedMode(context: Context): Int =
        prefs(context).getInt(KEY_FEED_MODE, FEED_MODE_LOCAL).coerceIn(FEED_MODE_LOCAL, FEED_MODE_GOOGLE)

    fun setFeedMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_FEED_MODE, mode.coerceIn(FEED_MODE_LOCAL, FEED_MODE_GOOGLE)).apply()
    }

    fun getTheme(context: Context): Int =
        prefs(context).getInt(KEY_THEME, THEME_LIGHT).coerceIn(THEME_LIGHT, THEME_BLUE)

    fun setTheme(context: Context, theme: Int) {
        prefs(context).edit().putInt(KEY_THEME, theme.coerceIn(THEME_LIGHT, THEME_BLUE)).apply()
    }

    fun getIconStyle(context: Context): Int =
        prefs(context).getInt(KEY_ICON_STYLE, ICON_STYLE_NONE).coerceIn(ICON_STYLE_NONE, ICON_STYLE_CIRCLE)

    fun setIconStyle(context: Context, style: Int) {
        prefs(context).edit().putInt(KEY_ICON_STYLE, style.coerceIn(ICON_STYLE_NONE, ICON_STYLE_CIRCLE)).apply()
    }

    fun isSearchBarEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SEARCH_BAR, true)

    fun setSearchBarEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SEARCH_BAR, enabled).apply()
    }

    fun isSoundFeedbackEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOUND_FEEDBACK, true)

    fun setSoundFeedbackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND_FEEDBACK, enabled).apply()
    }

    fun isGnLayoutEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GN_LAYOUT, false)

    fun setGnLayoutEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GN_LAYOUT, enabled).apply()
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
            superSimple -> 3
            gnLayout -> 4
            else -> getColumns(context)
        }
        val rows = when {
            superSimple -> 4
            gnLayout -> 5
            else -> getRows(context)
        }
        val iconDp = when {
            superSimple -> 64
            gnLayout -> 48
            else -> getIconSizeDp(context)
        }.toFloat()
        val iconPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDp, metrics)
            .roundToInt()
        val labelSize = when {
            superSimple -> 18f
            gnLayout -> 12f
            else -> getLabelSizeSp(context)
        }
        return LauncherUiConfig(
            columns = columns,
            rows = rows,
            iconSizePx = iconPx,
            labelSizeSp = labelSize,
            itemHeightPx = itemHeightPx,
            showLabels = true
        )
    }

    fun getDockConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val base = getUiConfig(context, itemHeightPx)
        val showLabels = !(isDockLabelsHidden(context) || isSuperSimpleEnabled(context))
        return base.copy(showLabels = showLabels)
    }

    fun getSimpleFavoritesConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val metrics = context.resources.displayMetrics
        val iconPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72f, metrics).roundToInt()
        return LauncherUiConfig(
            columns = 2,
            rows = 2,
            iconSizePx = iconPx,
            labelSizeSp = 20f,
            itemHeightPx = itemHeightPx,
            showLabels = true
        )
    }

    data class ThemeColors(val background: Int, val text: Int, val muted: Int)

    fun getThemeColors(context: Context): ThemeColors {
        return when (getTheme(context)) {
            THEME_DARK -> ThemeColors(0xFF202124.toInt(), 0xFFECEFF1.toInt(), 0xFFB0BEC5.toInt())
            THEME_BLACK -> ThemeColors(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB0BEC5.toInt())
            THEME_BLUE -> ThemeColors(0xFFE3F2FD.toInt(), 0xFF0D47A1.toInt(), 0xFF1E88E5.toInt())
            else -> ThemeColors(0xFFF5F5F5.toInt(), 0xFF1F1F1F.toInt(), 0xFF616161.toInt())
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getGestureAction(context: Context, key: String, defaultValue: Int): Int {
        return prefs(context).getInt(key, defaultValue)
    }

    private fun setGestureAction(context: Context, key: String, action: Int) {
        prefs(context).edit().putInt(key, action).apply()
    }
}
