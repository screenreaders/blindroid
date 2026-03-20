package com.screenreaders.blindroid.sms

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R

class SimpleMessageAdapter(
    private val onClick: (MessageEntry) -> Unit
) : RecyclerView.Adapter<SimpleMessageAdapter.ViewHolder>() {

    private var items: List<MessageEntry> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simple_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(list: List<MessageEntry>) {
        items = list
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.simpleMessageTitle)
        private val subtitle: TextView = view.findViewById(R.id.simpleMessageSubtitle)

        fun bind(item: MessageEntry) {
            title.text = item.address
            subtitle.text = item.format(itemView.context)
        }
    }
}
