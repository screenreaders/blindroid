package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class ScreenShortcutAdapter(
    private val items: MutableList<ScreenShortcut>,
    private var colors: LauncherPrefs.ThemeColors,
    private val onClick: (ScreenShortcut) -> Unit
) : RecyclerView.Adapter<ScreenShortcutAdapter.Holder>() {

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.screenShortcutButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screen_shortcut, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.button.text = item.label
        holder.button.contentDescription = "${item.label}, strona ${item.page}"
        ThemeUtils.tintButton(holder.button, colors, true)
        holder.button.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<ScreenShortcut>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateColors(newColors: LauncherPrefs.ThemeColors) {
        colors = newColors
        notifyDataSetChanged()
    }
}
