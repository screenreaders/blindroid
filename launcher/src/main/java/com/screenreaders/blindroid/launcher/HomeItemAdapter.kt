package com.screenreaders.blindroid.launcher

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeItemAdapter(
    private var items: List<HomeItem>,
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
        }
        applySizing(holder)
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<HomeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
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

    class HomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }
}
