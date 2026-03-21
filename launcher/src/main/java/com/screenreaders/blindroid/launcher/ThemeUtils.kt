package com.screenreaders.blindroid.launcher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Spinner
import android.widget.Switch
import kotlin.math.roundToInt

object ThemeUtils {
    fun tintButton(button: Button, colors: LauncherPrefs.ThemeColors, alt: Boolean = false) {
        val accent = if (alt) colors.accentAlt else colors.accent
        button.setTextColor(colors.text)
        button.backgroundTintList = ColorStateList.valueOf(accent)
    }

    fun tintEditText(input: EditText, colors: LauncherPrefs.ThemeColors) {
        input.setTextColor(colors.text)
        input.setHintTextColor(colors.muted)
        input.backgroundTintList = ColorStateList.valueOf(colors.accent)
    }

    fun tintSwitch(view: Switch, colors: LauncherPrefs.ThemeColors) {
        view.setTextColor(colors.text)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val thumb = intArrayOf(colors.accent, colors.muted)
        val track = intArrayOf(adjustAlpha(colors.accent, 0.4f), adjustAlpha(colors.muted, 0.2f))
        view.thumbTintList = ColorStateList(states, thumb)
        view.trackTintList = ColorStateList(states, track)
    }

    fun tintSwitchCompat(view: androidx.appcompat.widget.SwitchCompat, colors: LauncherPrefs.ThemeColors) {
        view.setTextColor(colors.text)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val thumb = intArrayOf(colors.accent, colors.muted)
        val track = intArrayOf(adjustAlpha(colors.accent, 0.4f), adjustAlpha(colors.muted, 0.2f))
        view.thumbTintList = ColorStateList(states, thumb)
        view.trackTintList = ColorStateList(states, track)
    }

    fun tintSpinner(spinner: Spinner, colors: LauncherPrefs.ThemeColors) {
        spinner.backgroundTintList = ColorStateList.valueOf(colors.accent)
    }

    fun applyCard(view: View, colors: LauncherPrefs.ThemeColors, alt: Boolean = false) {
        val accent = if (alt) colors.accentAlt else colors.accent
        val fill = blendColor(colors.background, accent, 0.08f)
        val stroke = adjustAlpha(accent, 0.7f)
        val shape = GradientDrawable().apply {
            cornerRadius = dp(view.context, 12)
            setColor(fill)
            setStroke(dp(view.context, 1).toInt(), stroke)
        }
        view.background = shape
    }

    fun applySurface(view: View, colors: LauncherPrefs.ThemeColors) {
        val fill = blendColor(colors.background, colors.muted, 0.08f)
        val shape = GradientDrawable().apply {
            cornerRadius = dp(view.context, 8)
            setColor(fill)
        }
        view.background = shape
    }

    fun applyLabelPill(label: TextView, colors: LauncherPrefs.ThemeColors, alt: Boolean = false) {
        val accent = if (alt) colors.accentAlt else colors.accent
        val fill = blendColor(colors.background, accent, 0.18f)
        val stroke = adjustAlpha(accent, 0.7f)
        val shape = GradientDrawable().apply {
            cornerRadius = dp(label.context, 12)
            setColor(fill)
            setStroke(dp(label.context, 1).toInt(), stroke)
        }
        label.background = shape
        val padH = dp(label.context, 8).toInt()
        val padV = dp(label.context, 3).toInt()
        label.setPadding(padH, padV, padH, padV)
    }

    fun clearLabelBackground(label: TextView) {
        label.background = null
        label.setPadding(0, 0, 0, 0)
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1f - ratio
        val r = ((from shr 16 and 0xFF) * inverse + (to shr 16 and 0xFF) * ratio).toInt()
        val g = ((from shr 8 and 0xFF) * inverse + (to shr 8 and 0xFF) * ratio).toInt()
        val b = ((from and 0xFF) * inverse + (to and 0xFF) * ratio).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (android.graphics.Color.alpha(color) * factor).roundToInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    private fun dp(context: Context, value: Int): Float {
        return value * context.resources.displayMetrics.density
    }
}
