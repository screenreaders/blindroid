package com.screenreaders.blindroid.launcher

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HomeItemAdapter(
    private val context: Context,
    private var items: List<HomeItem>,
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

    class HomeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val label: TextView = view.findViewById(R.id.appLabel)
    }
}
