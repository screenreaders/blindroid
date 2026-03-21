package com.screenreaders.blindroid.launcher

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeItemAdapter(
    private var items: MutableList<HomeItem>,
    private var config: LauncherUiConfig,
    private val onClick: (HomeItem) -> Unit,
    private val onLongClick: (HomeItem) -> Unit,
    private var editingEnabled: Boolean = true
) : RecyclerView.Adapter<HomeItemAdapter.HomeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_icon, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        val item = items[position]
        when (item) {
            is HomeItem.App -> {
                holder.label.text = formatLabel(holder.itemView.context, item.label)
                holder.icon.setImageDrawable(item.icon)
                holder.icon.contentDescription = item.label
            }
            is HomeItem.Folder -> {
                holder.label.text = formatLabel(holder.itemView.context, item.label)
                holder.icon.setImageResource(android.R.drawable.ic_menu_agenda)
                holder.icon.contentDescription = item.label
            }
            is HomeItem.Shortcut -> {
                holder.label.text = formatLabel(holder.itemView.context, item.label)
                holder.icon.setImageResource(item.iconRes)
                holder.icon.contentDescription = item.label
            }
        }
        applySizing(holder)
        holder.label.visibility = if (config.showLabels) View.VISIBLE else View.GONE
        applyTheme(holder, position)
        applyIconStyle(holder)
        applyIconTint(holder)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            if (!editingEnabled) return@setOnLongClickListener false
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<HomeItem>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
    }

    fun setEditingEnabled(enabled: Boolean) {
        if (editingEnabled == enabled) return
        editingEnabled = enabled
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        if (from == to) return
        if (from !in items.indices || to !in items.indices) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }

    private fun applySizing(holder: HomeViewHolder) {
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

    private fun applyTheme(holder: HomeViewHolder, position: Int) {
        val colors = LauncherPrefs.getThemeColors(holder.itemView.context)
        holder.label.setTextColor(colors.text)
        val context = holder.itemView.context
        if (LauncherPrefs.isLabelBackgroundEnabled(context)) {
            ThemeUtils.applyLabelPill(holder.label, colors, position % 2 == 1)
        } else {
            ThemeUtils.clearLabelBackground(holder.label)
        }
        if (LauncherPrefs.isSuperSimpleEnabled(context)) {
            ThemeUtils.applyCard(holder.itemView, colors, position % 2 == 1)
        } else {
            holder.itemView.background = null
        }
    }

    private fun applyIconStyle(holder: HomeViewHolder) {
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

    private fun applyIconTint(holder: HomeViewHolder) {
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
            1 -> label.uppercase(java.util.Locale.forLanguageTag("pl-PL"))
            else -> label
        }
    }

    class HomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }
}
