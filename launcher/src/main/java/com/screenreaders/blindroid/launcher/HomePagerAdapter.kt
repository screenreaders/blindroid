package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class HomePagerAdapter(
    private var pages: MutableList<MutableList<HomeItem>>,
    private var config: LauncherUiConfig,
    private var hasFeed: Boolean,
    private var feedData: FeedData?,
    private var feedColors: LauncherPrefs.ThemeColors,
    private val onOpenExternalFeed: () -> Unit,
    private val onClick: (Int, HomeItem) -> Unit,
    private val onLongClick: (Int, HomeItem) -> Unit,
    private val onMove: (Int, Int, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FEED = 0
        private const val VIEW_TYPE_PAGE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasFeed && position == 0) VIEW_TYPE_FEED else VIEW_TYPE_PAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_FEED) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feed_page, parent, false)
            FeedViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_home_page, parent, false)
            PageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeedViewHolder) {
            holder.bind(feedData, feedColors, onOpenExternalFeed)
            return
        }
        val pageIndex = if (hasFeed) position - 1 else position
        val items = pages.getOrElse(pageIndex) { mutableListOf() }
        (holder as PageViewHolder).bind(pageIndex, items, config)
    }

    override fun getItemCount(): Int = pages.size + if (hasFeed) 1 else 0

    fun submitPages(newPages: List<List<HomeItem>>) {
        pages = newPages.map { it.toMutableList() }.toMutableList()
        notifyDataSetChanged()
    }

    fun updateConfig(newConfig: LauncherUiConfig) {
        config = newConfig
        notifyDataSetChanged()
    }

    fun updateFeed(enabled: Boolean, data: FeedData?, colors: LauncherPrefs.ThemeColors) {
        hasFeed = enabled
        feedData = data
        feedColors = colors
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

    class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.feedTitle)
        private val time: TextView = view.findViewById(R.id.feedTime)
        private val date: TextView = view.findViewById(R.id.feedDate)
        private val battery: TextView = view.findViewById(R.id.feedBattery)
        private val notificationsLabel: TextView = view.findViewById(R.id.feedNotificationsLabel)
        private val container: LinearLayout = view.findViewById(R.id.notificationsContainer)
        private val openHint: TextView = view.findViewById(R.id.feedOpenHint)
        private val openButton: android.widget.Button = view.findViewById(R.id.feedOpenButton)

        fun bind(data: FeedData?, colors: LauncherPrefs.ThemeColors, onOpenExternalFeed: () -> Unit) {
            title.setTextColor(colors.text)
            time.setTextColor(colors.text)
            date.setTextColor(colors.muted)
            battery.setTextColor(colors.muted)
            notificationsLabel.setTextColor(colors.text)
            openHint.setTextColor(colors.muted)
            openButton.setTextColor(colors.text)

            if (data == null) return
            time.text = data.time
            date.text = data.date
            battery.text = data.battery

            if (data.externalMode) {
                openHint.visibility = View.VISIBLE
                openButton.visibility = View.VISIBLE
                openButton.isEnabled = data.externalAvailable
                openButton.alpha = if (data.externalAvailable) 1.0f else 0.5f
                openButton.setOnClickListener { onOpenExternalFeed() }
            } else {
                openHint.visibility = View.GONE
                openButton.visibility = View.GONE
                openButton.setOnClickListener(null)
            }

            container.removeAllViews()
            if (data.notifications.isEmpty()) {
                val empty = TextView(container.context)
                empty.text = container.context.getString(R.string.launcher_feed_no_notifications)
                empty.setTextColor(colors.muted)
                container.addView(empty)
            } else {
                data.notifications.forEach { message ->
                    val item = TextView(container.context)
                    item.text = "• $message"
                    item.setTextColor(colors.text)
                    item.textSize = 14f
                    container.addView(item)
                }
            }
        }
    }
}
