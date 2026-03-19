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

    fun getUiConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val metrics = context.resources.displayMetrics
        val superSimple = isSuperSimpleEnabled(context)
        val columns = if (superSimple) 3 else getColumns(context)
        val rows = if (superSimple) 4 else getRows(context)
        val iconDp = (if (superSimple) 64 else getIconSizeDp(context)).toFloat()
        val iconPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, iconDp, metrics)
            .roundToInt()
        return LauncherUiConfig(
            columns = columns,
            rows = rows,
            iconSizePx = iconPx,
            labelSizeSp = if (superSimple) 18f else getLabelSizeSp(context),
            itemHeightPx = itemHeightPx,
            showLabels = true
        )
    }

    fun getDockConfig(context: Context, itemHeightPx: Int = 0): LauncherUiConfig {
        val base = getUiConfig(context, itemHeightPx)
        val showLabels = !(isDockLabelsHidden(context) || isSuperSimpleEnabled(context))
        return base.copy(showLabels = showLabels)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
