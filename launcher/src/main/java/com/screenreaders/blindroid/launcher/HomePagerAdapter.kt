package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class HomePagerAdapter(
    private var pages: MutableList<MutableList<HomeItem>>,
    private var config: LauncherUiConfig,
    private val onClick: (Int, HomeItem) -> Unit,
    private val onLongClick: (Int, HomeItem) -> Unit,
    private val onMove: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val items = pages.getOrElse(position) { mutableListOf() }
        holder.bind(position, items, config)
    }

    override fun getItemCount(): Int = pages.size

    fun submitPages(newPages: List<List<HomeItem>>) {
        pages = newPages.map { it.toMutableList() }.toMutableList()
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
    }

    private fun moveItem(pageIndex: Int, from: Int, to: Int, adapter: HomeItemAdapter) {
        if (pageIndex !in pages.indices) return
        val page = pages[pageIndex]
        if (from !in page.indices || to !in page.indices) return
        val item = page.removeAt(from)
        page.add(to, item)
        adapter.moveItem(from, to)
    }

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val grid: RecyclerView = view.findViewById(R.id.pageGrid)
        private val gridLayoutManager = GridLayoutManager(view.context, config.columns)
        val adapter: HomeItemAdapter
        var adapterPageIndex: Int = 0

        init {
            grid.layoutManager = gridLayoutManager
            adapter = HomeItemAdapter(
                mutableListOf(),
                config,
                { item -> onClick(adapterPageIndex, item) },
                { item -> onLongClick(adapterPageIndex, item) }
            )
            grid.adapter = adapter

            val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                0
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                    moveItem(adapterPageIndex, from, to, adapter)
                    onMove(adapterPageIndex, from, to)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            })
            touchHelper.attachToRecyclerView(grid)
        }

        fun bind(pageIndex: Int, items: List<HomeItem>, newConfig: LauncherUiConfig) {
            adapterPageIndex = pageIndex
            gridLayoutManager.spanCount = newConfig.columns
            adapter.updateConfig(newConfig)
            adapter.submit(items)
        }
    }
}
