package com.screenreaders.blindroid.phone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R

class SimpleRecentAdapter(
    private val onClick: (RecentCall) -> Unit
) : RecyclerView.Adapter<SimpleRecentAdapter.ViewHolder>() {

    private var items: List<RecentCall> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<RecentCall>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.simpleItemTitle)
        private val subtitle: TextView = view.findViewById(R.id.simpleItemSubtitle)

        fun bind(item: RecentCall) {
            title.text = item.name.ifBlank { item.number }
            subtitle.text = item.format(itemView.context)
        }
    }
}
