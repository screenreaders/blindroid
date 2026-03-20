package com.screenreaders.blindroid.senior

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R

class ContactTileAdapter(
    private var items: MutableList<ContactTile?>,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<ContactTileAdapter.TileViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = items.getOrNull(position)
        holder.bind(tile)
        holder.itemView.setOnClickListener { onClick(position) }
        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<ContactTile?>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    class TileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.tileTitle)
        private val subtitle: TextView = view.findViewById(R.id.tileSubtitle)

        fun bind(tile: ContactTile?) {
            if (tile == null) {
                title.setText(R.string.contact_tile_empty)
                subtitle.text = ""
            } else {
                title.text = tile.name.ifBlank { tile.phone }
                subtitle.text = tile.phone
            }
        }
    }
}
