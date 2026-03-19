package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomePagerAdapter(
    private var pages: List<List<HomeItem>>,
    private val onClick: (Int, HomeItem) -> Unit,
    private val onLongClick: (Int, HomeItem) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val items = pages.getOrElse(position) { emptyList() }
        holder.adapter.submit(items)
        holder.adapterPageIndex = position
    }

    override fun getItemCount(): Int = pages.size

    fun submitPages(newPages: List<List<HomeItem>>) {
        pages = newPages
        notifyDataSetChanged()
    }

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val grid: RecyclerView = view.findViewById(R.id.pageGrid)
        val adapter: HomeItemAdapter
        var adapterPageIndex: Int = 0

        init {
            grid.layoutManager = GridLayoutManager(view.context, 4)
            adapter = HomeItemAdapter(
                view.context,
                emptyList(),
                { item -> onClick(adapterPageIndex, item) },
                { item -> onLongClick(adapterPageIndex, item) }
            )
            grid.adapter = adapter
        }
    }
}
