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
    private val onLongClick: (HomeItem) -> Unit
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
                holder.label.text = item.label
                holder.icon.setImageDrawable(item.icon)
                holder.icon.contentDescription = item.label
            }
            is HomeItem.Folder -> {
                holder.label.text = item.label
                holder.icon.setImageResource(android.R.drawable.ic_menu_agenda)
                holder.icon.contentDescription = item.label
            }
            is HomeItem.Shortcut -> {
                holder.label.text = item.label
                holder.icon.setImageResource(item.iconRes)
                holder.icon.contentDescription = item.label
            }
        }
        applySizing(holder)
        holder.label.visibility = if (config.showLabels) View.VISIBLE else View.GONE
        applyTheme(holder)
        applyIconStyle(holder)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
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

    private fun applyTheme(holder: HomeViewHolder) {
        val colors = LauncherPrefs.getThemeColors(holder.itemView.context)
        holder.label.setTextColor(colors.text)
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

    class HomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }
}
