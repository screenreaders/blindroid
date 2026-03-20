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
    private val onOpenAlarms: () -> Unit,
    private val onOpenCalendar: () -> Unit,
    private val onRequestCalendarPermission: () -> Unit,
    private val onOpenWeather: () -> Unit,
    private val onOpenBluetoothSettings: () -> Unit,
    private val onOpenNetworkSettings: () -> Unit,
    private val onOpenStorageSettings: () -> Unit,
    private val onOpenAllApps: () -> Unit,
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
            holder.bind(
                feedData,
                feedColors,
                onOpenExternalFeed,
                onOpenAlarms,
                onOpenCalendar,
                onRequestCalendarPermission,
                onOpenWeather,
                onOpenBluetoothSettings,
                onOpenNetworkSettings,
                onOpenStorageSettings,
                onOpenAllApps
            )
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
        private val root: View = view.findViewById(R.id.feedRoot)
        private val title: TextView = view.findViewById(R.id.feedTitle)
        private val time: TextView = view.findViewById(R.id.feedTime)
        private val date: TextView = view.findViewById(R.id.feedDate)
        private val battery: TextView = view.findViewById(R.id.feedBattery)
        private val nowCards: LinearLayout = view.findViewById(R.id.nowCardsContainer)
        private val cardAlarm: LinearLayout = view.findViewById(R.id.cardAlarm)
        private val cardAlarmTitle: TextView = view.findViewById(R.id.cardAlarmTitle)
        private val cardAlarmText: TextView = view.findViewById(R.id.cardAlarmText)
        private val cardCalendar: LinearLayout = view.findViewById(R.id.cardCalendar)
        private val cardCalendarTitle: TextView = view.findViewById(R.id.cardCalendarTitle)
        private val cardCalendarText: TextView = view.findViewById(R.id.cardCalendarText)
        private val cardWeather: LinearLayout = view.findViewById(R.id.cardWeather)
        private val cardWeatherTitle: TextView = view.findViewById(R.id.cardWeatherTitle)
        private val cardWeatherText: TextView = view.findViewById(R.id.cardWeatherText)
        private val cardReminders: LinearLayout = view.findViewById(R.id.cardReminders)
        private val cardRemindersTitle: TextView = view.findViewById(R.id.cardRemindersTitle)
        private val cardRemindersText: TextView = view.findViewById(R.id.cardRemindersText)
        private val cardHeadphones: LinearLayout = view.findViewById(R.id.cardHeadphones)
        private val cardHeadphonesTitle: TextView = view.findViewById(R.id.cardHeadphonesTitle)
        private val cardHeadphonesText: TextView = view.findViewById(R.id.cardHeadphonesText)
        private val cardNetwork: LinearLayout = view.findViewById(R.id.cardNetwork)
        private val cardNetworkTitle: TextView = view.findViewById(R.id.cardNetworkTitle)
        private val cardNetworkText: TextView = view.findViewById(R.id.cardNetworkText)
        private val cardStorage: LinearLayout = view.findViewById(R.id.cardStorage)
        private val cardStorageTitle: TextView = view.findViewById(R.id.cardStorageTitle)
        private val cardStorageText: TextView = view.findViewById(R.id.cardStorageText)
        private val cardTopApps: LinearLayout = view.findViewById(R.id.cardTopApps)
        private val cardTopAppsTitle: TextView = view.findViewById(R.id.cardTopAppsTitle)
        private val cardTopAppsText: TextView = view.findViewById(R.id.cardTopAppsText)
        private val cardAirplane: LinearLayout = view.findViewById(R.id.cardAirplane)
        private val cardAirplaneTitle: TextView = view.findViewById(R.id.cardAirplaneTitle)
        private val cardAirplaneText: TextView = view.findViewById(R.id.cardAirplaneText)
        private val cardRam: LinearLayout = view.findViewById(R.id.cardRam)
        private val cardRamTitle: TextView = view.findViewById(R.id.cardRamTitle)
        private val cardRamText: TextView = view.findViewById(R.id.cardRamText)
        private val notificationsLabel: TextView = view.findViewById(R.id.feedNotificationsLabel)
        private val container: LinearLayout = view.findViewById(R.id.notificationsContainer)
        private val openHint: TextView = view.findViewById(R.id.feedOpenHint)
        private val openButton: android.widget.Button = view.findViewById(R.id.feedOpenButton)

        fun bind(
            data: FeedData?,
            colors: LauncherPrefs.ThemeColors,
            onOpenExternalFeed: () -> Unit,
            onOpenAlarms: () -> Unit,
            onOpenCalendar: () -> Unit,
            onRequestCalendarPermission: () -> Unit,
            onOpenWeather: () -> Unit,
            onOpenBluetoothSettings: () -> Unit,
            onOpenNetworkSettings: () -> Unit,
            onOpenStorageSettings: () -> Unit,
            onOpenAllApps: () -> Unit
        ) {
            title.setTextColor(colors.text)
            time.setTextColor(colors.text)
            date.setTextColor(colors.muted)
            battery.setTextColor(colors.muted)
            cardAlarmTitle.setTextColor(colors.text)
            cardAlarmText.setTextColor(colors.muted)
            cardCalendarTitle.setTextColor(colors.text)
            cardCalendarText.setTextColor(colors.muted)
            cardWeatherTitle.setTextColor(colors.text)
            cardWeatherText.setTextColor(colors.muted)
            cardRemindersTitle.setTextColor(colors.text)
            cardRemindersText.setTextColor(colors.muted)
            cardHeadphonesTitle.setTextColor(colors.text)
            cardHeadphonesText.setTextColor(colors.muted)
            cardNetworkTitle.setTextColor(colors.text)
            cardNetworkText.setTextColor(colors.muted)
            cardStorageTitle.setTextColor(colors.text)
            cardStorageText.setTextColor(colors.muted)
            cardTopAppsTitle.setTextColor(colors.text)
            cardTopAppsText.setTextColor(colors.muted)
            cardAirplaneTitle.setTextColor(colors.text)
            cardAirplaneText.setTextColor(colors.muted)
            cardRamTitle.setTextColor(colors.text)
            cardRamText.setTextColor(colors.muted)
            notificationsLabel.setTextColor(colors.text)
            openHint.setTextColor(colors.muted)
            openButton.setTextColor(colors.text)

            if (data == null) return
            title.setText(if (data.externalMode) R.string.launcher_feed_title_google else R.string.launcher_feed_title)
            time.text = data.time
            date.text = data.date
            battery.text = data.battery

            if (data.externalMode) {
                openHint.visibility = View.VISIBLE
                openButton.visibility = View.VISIBLE
                openButton.isEnabled = data.externalAvailable
                openButton.alpha = if (data.externalAvailable) 1.0f else 0.5f
                openButton.setOnClickListener { onOpenExternalFeed() }
                notificationsLabel.visibility = View.GONE
                container.visibility = View.GONE
                nowCards.visibility = View.GONE
                root.setOnClickListener { if (data.externalAvailable) onOpenExternalFeed() }
            } else {
                openHint.visibility = View.GONE
                openButton.visibility = View.GONE
                openButton.setOnClickListener(null)
                root.setOnClickListener(null)
                notificationsLabel.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                nowCards.visibility = View.VISIBLE
            }

            container.removeAllViews()
            if (data.externalMode) {
                return
            }

            if (data.showAlarm) {
                cardAlarm.visibility = View.VISIBLE
                cardAlarmText.text = data.alarmText
                    ?: itemView.context.getString(R.string.launcher_feed_alarm_none)
                cardAlarm.setOnClickListener { onOpenAlarms() }
            } else {
                cardAlarm.visibility = View.GONE
                cardAlarm.setOnClickListener(null)
            }

            if (data.showCalendar) {
                cardCalendar.visibility = View.VISIBLE
                if (data.calendarPermissionGranted) {
                    cardCalendarText.text = data.calendarText
                        ?: itemView.context.getString(R.string.launcher_feed_calendar_none)
                    cardCalendar.setOnClickListener { onOpenCalendar() }
                } else {
                    cardCalendarText.text = itemView.context.getString(R.string.launcher_feed_calendar_permission)
                    cardCalendar.setOnClickListener { onRequestCalendarPermission() }
                }
            } else {
                cardCalendar.visibility = View.GONE
                cardCalendar.setOnClickListener(null)
            }

            if (data.showWeather) {
                cardWeather.visibility = View.VISIBLE
                cardWeatherText.text = data.weatherText
                    ?: itemView.context.getString(R.string.launcher_feed_weather_open)
                cardWeather.setOnClickListener { onOpenWeather() }
            } else {
                cardWeather.visibility = View.GONE
                cardWeather.setOnClickListener(null)
            }

            if (data.showReminders) {
                cardReminders.visibility = View.VISIBLE
                if (data.calendarPermissionGranted) {
                    cardRemindersText.text = data.reminderText
                        ?: itemView.context.getString(R.string.launcher_feed_reminders_none)
                    cardReminders.setOnClickListener { onOpenCalendar() }
                } else {
                    cardRemindersText.text = itemView.context.getString(R.string.launcher_feed_calendar_permission)
                    cardReminders.setOnClickListener { onRequestCalendarPermission() }
                }
            } else {
                cardReminders.visibility = View.GONE
                cardReminders.setOnClickListener(null)
            }

            if (data.showHeadphones) {
                cardHeadphones.visibility = View.VISIBLE
                cardHeadphonesText.text = data.headphonesText
                    ?: itemView.context.getString(R.string.launcher_feed_headphones_none)
                cardHeadphones.setOnClickListener { onOpenBluetoothSettings() }
            } else {
                cardHeadphones.visibility = View.GONE
                cardHeadphones.setOnClickListener(null)
            }

            if (data.showNetwork) {
                cardNetwork.visibility = View.VISIBLE
                cardNetworkText.text = data.networkText
                    ?: itemView.context.getString(R.string.launcher_feed_network_none)
                cardNetwork.setOnClickListener { onOpenNetworkSettings() }
            } else {
                cardNetwork.visibility = View.GONE
                cardNetwork.setOnClickListener(null)
            }

            if (data.showStorage) {
                cardStorage.visibility = View.VISIBLE
                cardStorageText.text = data.storageText
                    ?: itemView.context.getString(R.string.launcher_feed_storage_settings)
                cardStorage.setOnClickListener { onOpenStorageSettings() }
            } else {
                cardStorage.visibility = View.GONE
                cardStorage.setOnClickListener(null)
            }

            if (data.showTopApps) {
                cardTopApps.visibility = View.VISIBLE
                cardTopAppsText.text = if (data.topApps.isNotEmpty()) {
                    data.topApps.joinToString(separator = " • ")
                } else {
                    itemView.context.getString(R.string.launcher_feed_top_apps_none)
                }
                cardTopApps.setOnClickListener { onOpenAllApps() }
            } else {
                cardTopApps.visibility = View.GONE
                cardTopApps.setOnClickListener(null)
            }

            if (data.showAirplane) {
                cardAirplane.visibility = View.VISIBLE
                cardAirplaneText.text = data.airplaneText
                    ?: itemView.context.getString(R.string.launcher_feed_airplane_off)
                cardAirplane.setOnClickListener { onOpenNetworkSettings() }
            } else {
                cardAirplane.visibility = View.GONE
                cardAirplane.setOnClickListener(null)
            }

            if (data.showRam) {
                cardRam.visibility = View.VISIBLE
                cardRamText.text = data.ramText
                    ?: itemView.context.getString(R.string.launcher_feed_ram_title)
                cardRam.setOnClickListener { onOpenNetworkSettings() }
            } else {
                cardRam.visibility = View.GONE
                cardRam.setOnClickListener(null)
            }

            applyCardStyle(cardAlarm, colors)
            applyCardStyle(cardCalendar, colors)
            applyCardStyle(cardWeather, colors)
            applyCardStyle(cardReminders, colors)
            applyCardStyle(cardHeadphones, colors)
            applyCardStyle(cardNetwork, colors)
            applyCardStyle(cardStorage, colors)
            applyCardStyle(cardTopApps, colors)
            applyCardStyle(cardAirplane, colors)
            applyCardStyle(cardRam, colors)

            nowCards.visibility = if (
                data.showAlarm ||
                data.showCalendar ||
                data.showWeather ||
                data.showReminders ||
                data.showHeadphones ||
                data.showNetwork ||
                data.showStorage ||
                data.showTopApps ||
                data.showAirplane ||
                data.showRam
            ) {
                View.VISIBLE
            } else {
                View.GONE
            }

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

        private fun applyCardStyle(view: View, colors: LauncherPrefs.ThemeColors) {
            val background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 24f
                setColor(blend(colors.background, colors.muted, 0.12f))
                setStroke(2, blend(colors.muted, colors.text, 0.12f))
            }
            view.background = background
        }

        private fun blend(base: Int, overlay: Int, alpha: Float): Int {
            val r = ((1 - alpha) * android.graphics.Color.red(base) + alpha * android.graphics.Color.red(overlay)).toInt()
            val g = ((1 - alpha) * android.graphics.Color.green(base) + alpha * android.graphics.Color.green(overlay)).toInt()
            val b = ((1 - alpha) * android.graphics.Color.blue(base) + alpha * android.graphics.Color.blue(overlay)).toInt()
            return android.graphics.Color.rgb(r, g, b)
        }
    }
}
