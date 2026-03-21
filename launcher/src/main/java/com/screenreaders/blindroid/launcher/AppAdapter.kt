package com.screenreaders.blindroid.launcher

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private var items: List<AppEntry>,
    private var config: LauncherUiConfig,
    private val onClick: (AppEntry) -> Unit,
    private val onLongClick: (AppEntry) -> Unit,
    private val onLongClickView: ((AppEntry, View) -> Unit)? = null
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = formatLabel(holder.itemView.context, item.label)
        holder.icon.setImageDrawable(item.icon)
        holder.icon.contentDescription = item.label
        applySizing(holder)
        holder.label.visibility = if (config.showLabels) View.VISIBLE else View.GONE
        applyTheme(holder, position)
        applyIconStyle(holder)
        applyIconTint(holder)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            if (onLongClickView != null) {
                onLongClickView.invoke(item, holder.itemView)
            } else {
                onLongClick(item)
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<AppEntry>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
    }

    private fun applySizing(holder: AppViewHolder) {
        val iconLp = holder.icon.layoutParams
        iconLp.width = config.iconSizePx
        iconLp.height = config.iconSizePx
        holder.icon.layoutParams = iconLp

        holder.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, config.labelSizeSp)

        val itemLp = holder.itemView.layoutParams
        if (itemLp != null && config.itemHeightPx > 0) {
            itemLp.height = config.itemHeightPx
            holder.itemView.layoutParams = itemLp
        }
    }

    private fun applyTheme(holder: AppViewHolder, position: Int) {
        val colors = LauncherPrefs.getThemeColors(holder.itemView.context)
        holder.label.setTextColor(colors.text)
        val context = holder.itemView.context
        if (LauncherPrefs.isLabelBackgroundEnabled(context)) {
            ThemeUtils.applyLabelPill(holder.label, colors, position % 2 == 1)
        } else {
            ThemeUtils.clearLabelBackground(holder.label)
        }
    }

    private fun applyIconStyle(holder: AppViewHolder) {
        val context = holder.itemView.context
        if (LauncherPrefs.getIconStyle(context) == 1) {
            holder.icon.setBackgroundResource(R.drawable.icon_bg_circle)
            val pad = (config.iconSizePx * 0.12f).toInt().coerceAtLeast(6)
            holder.icon.setPadding(pad, pad, pad, pad)
        } else {
            holder.icon.background = null
            holder.icon.setPadding(0, 0, 0, 0)
        }
    }

    private fun applyIconTint(holder: AppViewHolder) {
        val context = holder.itemView.context
        when (LauncherPrefs.getIconTint(context)) {
            1 -> {
                val matrix = android.graphics.ColorMatrix().apply { setSaturation(0f) }
                holder.icon.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
            }
            2 -> {
                val matrix = android.graphics.ColorMatrix().apply {
                    setSaturation(0f)
                    val contrast = 1.4f
                    val scale = contrast
                    val translate = (-0.5f * scale + 0.5f) * 255f
                    postConcat(
                        android.graphics.ColorMatrix(
                            floatArrayOf(
                                scale, 0f, 0f, 0f, translate,
                                0f, scale, 0f, 0f, translate,
                                0f, 0f, scale, 0f, translate,
                                0f, 0f, 0f, 1f, 0f
                            )
                        )
                    )
                }
                holder.icon.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
            }
            else -> holder.icon.colorFilter = null
        }
    }

    private fun formatLabel(context: android.content.Context, label: String): String {
        return when (LauncherPrefs.getLabelStyle(context)) {
            1 -> label.uppercase(java.util.Locale("pl", "PL"))
            else -> label
        }
    }

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }
}
