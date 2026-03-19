package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomePagerAdapter(
    private var pages: List<List<HomeItem>>,
    private var config: LauncherUiConfig,
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
        holder.bind(position, items, config)
    }

    override fun getItemCount(): Int = pages.size

    fun submitPages(newPages: List<List<HomeItem>>) {
        pages = newPages
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
    }

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val grid: RecyclerView = view.findViewById(R.id.pageGrid)
        private val gridLayoutManager = GridLayoutManager(view.context, config.columns)
        val adapter: HomeItemAdapter
        var adapterPageIndex: Int = 0

        init {
            grid.layoutManager = gridLayoutManager
            adapter = HomeItemAdapter(
                emptyList(),
                config,
                { item -> onClick(adapterPageIndex, item) },
                { item -> onLongClick(adapterPageIndex, item) }
            )
            grid.adapter = adapter
        }

        fun bind(pageIndex: Int, items: List<HomeItem>, newConfig: LauncherUiConfig) {
            adapterPageIndex = pageIndex
            gridLayoutManager.spanCount = newConfig.columns
            adapter.updateConfig(newConfig)
            adapter.submit(items)
        }
    }
}
