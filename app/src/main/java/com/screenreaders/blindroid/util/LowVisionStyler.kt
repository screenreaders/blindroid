package com.screenreaders.blindroid.util

import android.app.Activity
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs

@Suppress("DEPRECATION")
object LowVisionStyler {
    const val STYLE_DEFAULT = 0
    const val STYLE_DARK = 1
    const val STYLE_LIGHT = 2
    const val STYLE_YELLOW = 3

    data class StyleColors(@ColorInt val background: Int, @ColorInt val text: Int, @ColorInt val muted: Int)

    fun apply(activity: Activity) {
        val enabled = Prefs.isLowVisionEnabled(activity)
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        if (!enabled) return
        val style = getStyleColors(activity)
        val scale = Prefs.getLowVisionScale(activity) / 100f
        root.setBackgroundColor(style.background)
        applyToView(root, style, scale)
    }

    private fun getStyleColors(activity: Activity): StyleColors {
        val style = Prefs.getLowVisionStyle(activity)
        val invert = Prefs.isLowVisionInvert(activity)
        val base = when (style) {
            STYLE_DARK -> StyleColors(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFBDBDBD.toInt())
            STYLE_LIGHT -> StyleColors(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF616161.toInt())
            STYLE_YELLOW -> StyleColors(0xFF000000.toInt(), 0xFFFFEB3B.toInt(), 0xFFFFF59D.toInt())
            else -> StyleColors(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF616161.toInt())
        }
        return if (invert) {
            StyleColors(base.text, base.background, base.muted)
        } else {
            base
        }
    }

    private fun applyToView(view: View, colors: StyleColors, scale: Float) {
        if (view is TextView) {
            view.setTextColor(colors.text)
            if (view is EditText) {
                view.setHintTextColor(colors.muted)
            }
            val tagId = R.id.tag_original_text_size
            val original = (view.getTag(tagId) as? Float) ?: run {
                val sizeSp = view.textSize / view.resources.displayMetrics.scaledDensity
                view.setTag(tagId, sizeSp)
                sizeSp
            }
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, original * scale)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToView(view.getChildAt(i), colors, scale)
            }
        }
    }
}
