package com.screenreaders.blindroid.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
    private var editingEnabled: Boolean,
    private val onOpenExternalFeed: () -> Unit,
    private val onOpenAlarms: () -> Unit,
    private val onOpenCalendar: () -> Unit,
    private val onRequestCalendarPermission: () -> Unit,
    private val onOpenWeather: () -> Unit,
    private val onOpenBluetoothSettings: () -> Unit,
    private val onRequestBluetoothPermission: () -> Unit,
    private val onOpenLocationSettings: () -> Unit,
    private val onRequestLocationPermission: () -> Unit,
    private val onOpenNetworkSettings: () -> Unit,
    private val onOpenStorageSettings: () -> Unit,
    private val onOpenScreenTime: () -> Unit,
    private val onOpenUsageAccess: () -> Unit,
    private val onOpenDisplaySettings: () -> Unit,
    private val onOpenBatterySettings: () -> Unit,
    private val onOpenDndSettings: () -> Unit,
    private val onOpenSoundSettings: () -> Unit,
    private val onOpenQuickSettings: () -> Unit,
    private val onToggleWifi: () -> Unit,
    private val onToggleBluetooth: () -> Unit,
    private val onToggleDnd: () -> Unit,
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
                onRequestBluetoothPermission,
                onOpenLocationSettings,
                onRequestLocationPermission,
                onOpenNetworkSettings,
                onOpenStorageSettings,
                onOpenScreenTime,
                onOpenUsageAccess,
                onOpenDisplaySettings,
                onOpenBatterySettings,
                onOpenDndSettings,
                onOpenSoundSettings,
                onOpenQuickSettings,
                onToggleWifi,
                onToggleBluetooth,
                onToggleDnd,
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

    fun setEditingEnabled(enabled: Boolean) {
        if (editingEnabled == enabled) return
        editingEnabled = enabled
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
        private val touchHelper: ItemTouchHelper
        private var touchAttached = false

        init {
            grid.layoutManager = gridLayoutManager
            adapter = HomeItemAdapter(
                mutableListOf(),
                config,
                { item -> onClick(adapterPageIndex, item) },
                { item -> onLongClick(adapterPageIndex, item) },
                editingEnabled
            )
            grid.adapter = adapter

            touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
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
        }

        fun bind(pageIndex: Int, items: List<HomeItem>, newConfig: LauncherUiConfig) {
            adapterPageIndex = pageIndex
            gridLayoutManager.spanCount = newConfig.columns
            adapter.updateConfig(newConfig)
            adapter.submit(items)
            setEditingEnabled(editingEnabled)
        }

        private fun setEditingEnabled(enabled: Boolean) {
            adapter.setEditingEnabled(enabled)
            if (enabled && !touchAttached) {
                touchHelper.attachToRecyclerView(grid)
                touchAttached = true
            } else if (!enabled && touchAttached) {
                touchHelper.attachToRecyclerView(null)
                touchAttached = false
            }
        }
    }

    class FeedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val root: View = view.findViewById(R.id.feedRoot)
        private val title: TextView = view.findViewById(R.id.feedTitle)
        private val time: TextView = view.findViewById(R.id.feedTime)
        private val date: TextView = view.findViewById(R.id.feedDate)
        private val battery: TextView = view.findViewById(R.id.feedBattery)
        private val atGlanceCard: LinearLayout = view.findViewById(R.id.feedAtGlanceCard)
        private val atGlanceTitle: TextView = view.findViewById(R.id.feedAtGlanceTitle)
        private val atGlanceText: TextView = view.findViewById(R.id.feedAtGlanceText)
        private val webHint: TextView = view.findViewById(R.id.feedWebHint)
        private val webView: android.webkit.WebView = view.findViewById(R.id.feedWebView)
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
        private val cardBattery: LinearLayout = view.findViewById(R.id.cardBattery)
        private val cardBatteryTitle: TextView = view.findViewById(R.id.cardBatteryTitle)
        private val cardBatteryText: TextView = view.findViewById(R.id.cardBatteryText)
        private val cardReminders: LinearLayout = view.findViewById(R.id.cardReminders)
        private val cardRemindersTitle: TextView = view.findViewById(R.id.cardRemindersTitle)
        private val cardRemindersText: TextView = view.findViewById(R.id.cardRemindersText)
        private val cardHeadphones: LinearLayout = view.findViewById(R.id.cardHeadphones)
        private val cardHeadphonesTitle: TextView = view.findViewById(R.id.cardHeadphonesTitle)
        private val cardHeadphonesText: TextView = view.findViewById(R.id.cardHeadphonesText)
        private val cardLocation: LinearLayout = view.findViewById(R.id.cardLocation)
        private val cardLocationTitle: TextView = view.findViewById(R.id.cardLocationTitle)
        private val cardLocationText: TextView = view.findViewById(R.id.cardLocationText)
        private val cardNetwork: LinearLayout = view.findViewById(R.id.cardNetwork)
        private val cardNetworkTitle: TextView = view.findViewById(R.id.cardNetworkTitle)
        private val cardNetworkText: TextView = view.findViewById(R.id.cardNetworkText)
        private val cardStorage: LinearLayout = view.findViewById(R.id.cardStorage)
        private val cardStorageTitle: TextView = view.findViewById(R.id.cardStorageTitle)
        private val cardStorageText: TextView = view.findViewById(R.id.cardStorageText)
        private val cardScreenTime: LinearLayout = view.findViewById(R.id.cardScreenTime)
        private val cardScreenTimeTitle: TextView = view.findViewById(R.id.cardScreenTimeTitle)
        private val cardScreenTimeText: TextView = view.findViewById(R.id.cardScreenTimeText)
        private val cardBluetooth: LinearLayout = view.findViewById(R.id.cardBluetooth)
        private val cardBluetoothTitle: TextView = view.findViewById(R.id.cardBluetoothTitle)
        private val cardBluetoothText: TextView = view.findViewById(R.id.cardBluetoothText)
        private val cardBrightness: LinearLayout = view.findViewById(R.id.cardBrightness)
        private val cardBrightnessTitle: TextView = view.findViewById(R.id.cardBrightnessTitle)
        private val cardBrightnessText: TextView = view.findViewById(R.id.cardBrightnessText)
        private val cardVolume: LinearLayout = view.findViewById(R.id.cardVolume)
        private val cardVolumeTitle: TextView = view.findViewById(R.id.cardVolumeTitle)
        private val cardVolumeText: TextView = view.findViewById(R.id.cardVolumeText)
        private val cardPower: LinearLayout = view.findViewById(R.id.cardPower)
        private val cardPowerTitle: TextView = view.findViewById(R.id.cardPowerTitle)
        private val cardPowerText: TextView = view.findViewById(R.id.cardPowerText)
        private val cardTopApps: LinearLayout = view.findViewById(R.id.cardTopApps)
        private val cardTopAppsTitle: TextView = view.findViewById(R.id.cardTopAppsTitle)
        private val cardTopAppsText: TextView = view.findViewById(R.id.cardTopAppsText)
        private val cardAirplane: LinearLayout = view.findViewById(R.id.cardAirplane)
        private val cardAirplaneTitle: TextView = view.findViewById(R.id.cardAirplaneTitle)
        private val cardAirplaneText: TextView = view.findViewById(R.id.cardAirplaneText)
        private val cardRam: LinearLayout = view.findViewById(R.id.cardRam)
        private val cardRamTitle: TextView = view.findViewById(R.id.cardRamTitle)
        private val cardRamText: TextView = view.findViewById(R.id.cardRamText)
        private val cardDevice: LinearLayout = view.findViewById(R.id.cardDevice)
        private val cardDeviceTitle: TextView = view.findViewById(R.id.cardDeviceTitle)
        private val cardDeviceText: TextView = view.findViewById(R.id.cardDeviceText)
        private val cardRotation: LinearLayout = view.findViewById(R.id.cardRotation)
        private val cardRotationTitle: TextView = view.findViewById(R.id.cardRotationTitle)
        private val cardRotationText: TextView = view.findViewById(R.id.cardRotationText)
        private val cardNfc: LinearLayout = view.findViewById(R.id.cardNfc)
        private val cardNfcTitle: TextView = view.findViewById(R.id.cardNfcTitle)
        private val cardNfcText: TextView = view.findViewById(R.id.cardNfcText)
        private val cardDnd: LinearLayout = view.findViewById(R.id.cardDnd)
        private val cardDndTitle: TextView = view.findViewById(R.id.cardDndTitle)
        private val cardDndText: TextView = view.findViewById(R.id.cardDndText)
        private val cardRinger: LinearLayout = view.findViewById(R.id.cardRinger)
        private val cardRingerTitle: TextView = view.findViewById(R.id.cardRingerTitle)
        private val cardRingerText: TextView = view.findViewById(R.id.cardRingerText)
        private val notificationsLabel: TextView = view.findViewById(R.id.feedNotificationsLabel)
        private val container: LinearLayout = view.findViewById(R.id.notificationsContainer)
        private val openHint: TextView = view.findViewById(R.id.feedOpenHint)
        private val openButton: android.widget.Button = view.findViewById(R.id.feedOpenButton)
        private val quickActionsRow: LinearLayout = view.findViewById(R.id.feedQuickActions)
        private val quickSettingsButton: Button = view.findViewById(R.id.feedQuickSettingsButton)
        private val wifiButton: Button = view.findViewById(R.id.feedWifiButton)
        private val bluetoothButton: Button = view.findViewById(R.id.feedBluetoothButton)
        private val dndButton: Button = view.findViewById(R.id.feedDndButton)

        fun bind(
            data: FeedData?,
            colors: LauncherPrefs.ThemeColors,
            onOpenExternalFeed: () -> Unit,
            onOpenAlarms: () -> Unit,
            onOpenCalendar: () -> Unit,
            onRequestCalendarPermission: () -> Unit,
            onOpenWeather: () -> Unit,
            onOpenBluetoothSettings: () -> Unit,
            onRequestBluetoothPermission: () -> Unit,
            onOpenLocationSettings: () -> Unit,
            onRequestLocationPermission: () -> Unit,
            onOpenNetworkSettings: () -> Unit,
            onOpenStorageSettings: () -> Unit,
            onOpenScreenTime: () -> Unit,
            onOpenUsageAccess: () -> Unit,
            onOpenDisplaySettings: () -> Unit,
            onOpenBatterySettings: () -> Unit,
            onOpenDndSettings: () -> Unit,
            onOpenSoundSettings: () -> Unit,
            onOpenQuickSettings: () -> Unit,
            onToggleWifi: () -> Unit,
            onToggleBluetooth: () -> Unit,
            onToggleDnd: () -> Unit,
            onOpenAllApps: () -> Unit
        ) {
            root.setBackgroundColor(colors.background)
            title.setTextColor(colors.text)
            time.setTextColor(colors.text)
            date.setTextColor(colors.muted)
            battery.setTextColor(colors.muted)
            atGlanceTitle.setTextColor(colors.text)
            atGlanceText.setTextColor(colors.muted)
            cardAlarmTitle.setTextColor(colors.text)
            cardAlarmText.setTextColor(colors.muted)
            cardCalendarTitle.setTextColor(colors.text)
            cardCalendarText.setTextColor(colors.muted)
            cardWeatherTitle.setTextColor(colors.text)
            cardWeatherText.setTextColor(colors.muted)
            cardBatteryTitle.setTextColor(colors.text)
            cardBatteryText.setTextColor(colors.muted)
            cardRemindersTitle.setTextColor(colors.text)
            cardRemindersText.setTextColor(colors.muted)
            cardHeadphonesTitle.setTextColor(colors.text)
            cardHeadphonesText.setTextColor(colors.muted)
            cardLocationTitle.setTextColor(colors.text)
            cardLocationText.setTextColor(colors.muted)
            cardNetworkTitle.setTextColor(colors.text)
            cardNetworkText.setTextColor(colors.muted)
            cardStorageTitle.setTextColor(colors.text)
            cardStorageText.setTextColor(colors.muted)
            cardScreenTimeTitle.setTextColor(colors.text)
            cardScreenTimeText.setTextColor(colors.muted)
            cardBluetoothTitle.setTextColor(colors.text)
            cardBluetoothText.setTextColor(colors.muted)
            cardBrightnessTitle.setTextColor(colors.text)
            cardBrightnessText.setTextColor(colors.muted)
            cardVolumeTitle.setTextColor(colors.text)
            cardVolumeText.setTextColor(colors.muted)
            cardPowerTitle.setTextColor(colors.text)
            cardPowerText.setTextColor(colors.muted)
            cardTopAppsTitle.setTextColor(colors.text)
            cardTopAppsText.setTextColor(colors.muted)
            cardAirplaneTitle.setTextColor(colors.text)
            cardAirplaneText.setTextColor(colors.muted)
            cardRamTitle.setTextColor(colors.text)
            cardRamText.setTextColor(colors.muted)
            cardDeviceTitle.setTextColor(colors.text)
            cardDeviceText.setTextColor(colors.muted)
            cardRotationTitle.setTextColor(colors.text)
            cardRotationText.setTextColor(colors.muted)
            cardNfcTitle.setTextColor(colors.text)
            cardNfcText.setTextColor(colors.muted)
            cardDndTitle.setTextColor(colors.text)
            cardDndText.setTextColor(colors.muted)
            cardRingerTitle.setTextColor(colors.text)
            cardRingerText.setTextColor(colors.muted)
            notificationsLabel.setTextColor(colors.text)
            openHint.setTextColor(colors.muted)
            ThemeUtils.tintButton(openButton, colors, false)
            ThemeUtils.applySurface(quickActionsRow, colors)
            ThemeUtils.tintButton(quickSettingsButton, colors, false)
            ThemeUtils.tintButton(wifiButton, colors, true)
            ThemeUtils.tintButton(bluetoothButton, colors, false)
            ThemeUtils.tintButton(dndButton, colors, true)

            quickSettingsButton.setOnClickListener { onOpenQuickSettings() }
            wifiButton.setOnClickListener { onToggleWifi() }
            bluetoothButton.setOnClickListener { onToggleBluetooth() }
            dndButton.setOnClickListener { onToggleDnd() }

            if (data == null) return
            title.setText(if (data.externalMode) R.string.launcher_feed_title_google else R.string.launcher_feed_title)
            time.text = data.time
            date.text = data.date
            battery.text = data.battery
            atGlanceText.text = data.atGlanceText ?: itemView.context.getString(R.string.launcher_at_glance_empty)
            time.setOnClickListener {
                if (data.externalMode && data.externalAvailable) onOpenExternalFeed() else onOpenAlarms()
            }
            date.setOnClickListener {
                if (data.externalMode && data.externalAvailable) onOpenExternalFeed() else onOpenCalendar()
            }
            atGlanceCard.setOnClickListener {
                when {
                    data.externalMode && data.externalAvailable -> onOpenExternalFeed()
                    data.showWeather -> onOpenWeather()
                    data.calendarPermissionGranted -> onOpenCalendar()
                    else -> onOpenAlarms()
                }
            }
            atGlanceCard.setOnLongClickListener {
                onOpenAllApps()
                true
            }

            if (data.externalMode) {
                openHint.visibility = View.VISIBLE
                openButton.visibility = View.VISIBLE
                openButton.isEnabled = data.externalAvailable
                openButton.alpha = if (data.externalAvailable) 1.0f else 0.5f
                openButton.setOnClickListener { onOpenExternalFeed() }
                quickActionsRow.visibility = if (data.quickActionsEnabled) View.VISIBLE else View.GONE
                webHint.visibility = View.GONE
                webView.visibility = View.GONE
                notificationsLabel.visibility = View.GONE
                container.visibility = View.GONE
                nowCards.visibility = View.GONE
                root.setOnClickListener { if (data.externalAvailable) onOpenExternalFeed() }
                atGlanceCard.visibility = View.VISIBLE
            } else if (data.embeddedMode) {
                openHint.visibility = View.GONE
                openButton.visibility = View.GONE
                openButton.setOnClickListener(null)
                root.setOnClickListener(null)
                quickActionsRow.visibility = if (data.quickActionsEnabled) View.VISIBLE else View.GONE
                webHint.visibility = View.VISIBLE
                webView.visibility = View.VISIBLE
                notificationsLabel.visibility = View.GONE
                container.visibility = View.GONE
                nowCards.visibility = View.GONE
                atGlanceCard.visibility = View.VISIBLE
                val url = data.embeddedUrl ?: "https://www.google.com"
                if (webView.tag != url) {
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.loadsImagesAutomatically = true
                    webView.settings.useWideViewPort = true
                    webView.settings.loadWithOverviewMode = true
                    webView.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    webView.settings.userAgentString = webView.settings.userAgentString + " BlindroidLauncher Mobile"
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                    webView.webChromeClient = android.webkit.WebChromeClient()
                    webView.webViewClient = android.webkit.WebViewClient()
                    webView.loadUrl(url)
                    webView.tag = url
                }
            } else {
                openHint.visibility = View.GONE
                openButton.visibility = View.GONE
                openButton.setOnClickListener(null)
                root.setOnClickListener(null)
                quickActionsRow.visibility = if (data.quickActionsEnabled) View.VISIBLE else View.GONE
                webHint.visibility = View.GONE
                webView.visibility = View.GONE
                notificationsLabel.visibility = View.VISIBLE
                container.visibility = View.VISIBLE
                nowCards.visibility = View.VISIBLE
                atGlanceCard.visibility = View.VISIBLE
            }

            container.removeAllViews()
            if (data.externalMode || data.embeddedMode) {
                return
            }

            wifiButton.text = if (data.wifiEnabled) {
                itemView.context.getString(R.string.launcher_feed_quick_wifi_on)
            } else {
                itemView.context.getString(R.string.launcher_feed_quick_wifi_off)
            }
            bluetoothButton.text = if (data.bluetoothEnabled) {
                itemView.context.getString(R.string.launcher_feed_quick_bluetooth_on)
            } else {
                itemView.context.getString(R.string.launcher_feed_quick_bluetooth_off)
            }
            dndButton.text = if (data.dndEnabled) {
                itemView.context.getString(R.string.launcher_feed_quick_dnd_on)
            } else {
                itemView.context.getString(R.string.launcher_feed_quick_dnd_off)
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

            if (data.showBattery) {
                cardBattery.visibility = View.VISIBLE
                cardBatteryText.text = data.batteryDetailsText
                    ?: itemView.context.getString(R.string.launcher_battery_unknown)
                cardBattery.setOnClickListener { onOpenBatterySettings() }
            } else {
                cardBattery.visibility = View.GONE
                cardBattery.setOnClickListener(null)
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

            if (data.showLocation) {
                cardLocation.visibility = View.VISIBLE
                cardLocationText.text = data.locationText
                    ?: itemView.context.getString(R.string.launcher_feed_location_unknown)
                cardLocation.setOnClickListener {
                    if (!data.locationPermissionGranted) {
                        onRequestLocationPermission()
                    } else {
                        onOpenLocationSettings()
                    }
                }
            } else {
                cardLocation.visibility = View.GONE
                cardLocation.setOnClickListener(null)
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

            if (data.showScreenTime) {
                cardScreenTime.visibility = View.VISIBLE
                cardScreenTimeText.text = data.screenTimeText
                    ?: itemView.context.getString(R.string.launcher_feed_screen_time_none)
                cardScreenTime.setOnClickListener {
                    if (!data.usagePermissionGranted) {
                        onOpenUsageAccess()
                    } else {
                        onOpenScreenTime()
                    }
                }
            } else {
                cardScreenTime.visibility = View.GONE
                cardScreenTime.setOnClickListener(null)
            }

            if (data.showBluetooth) {
                cardBluetooth.visibility = View.VISIBLE
                cardBluetoothText.text = data.bluetoothText
                    ?: itemView.context.getString(R.string.launcher_feed_bluetooth_off)
                cardBluetooth.setOnClickListener {
                    if (data.bluetoothPermissionGranted) {
                        onOpenBluetoothSettings()
                    } else {
                        onRequestBluetoothPermission()
                    }
                }
            } else {
                cardBluetooth.visibility = View.GONE
                cardBluetooth.setOnClickListener(null)
            }

            if (data.showBrightness) {
                cardBrightness.visibility = View.VISIBLE
                cardBrightnessText.text = data.brightnessText
                    ?: itemView.context.getString(R.string.launcher_feed_brightness_auto)
                cardBrightness.setOnClickListener { onOpenDisplaySettings() }
            } else {
                cardBrightness.visibility = View.GONE
                cardBrightness.setOnClickListener(null)
            }

            if (data.showVolume) {
                cardVolume.visibility = View.VISIBLE
                cardVolumeText.text = data.volumeText
                    ?: itemView.context.getString(R.string.launcher_feed_volume_text, 0)
                cardVolume.setOnClickListener { onOpenSoundSettings() }
            } else {
                cardVolume.visibility = View.GONE
                cardVolume.setOnClickListener(null)
            }

            if (data.showPower) {
                cardPower.visibility = View.VISIBLE
                cardPowerText.text = data.powerText
                    ?: itemView.context.getString(R.string.launcher_feed_power_off)
                cardPower.setOnClickListener { onOpenBatterySettings() }
            } else {
                cardPower.visibility = View.GONE
                cardPower.setOnClickListener(null)
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

            if (data.showDevice) {
                cardDevice.visibility = View.VISIBLE
                cardDeviceText.text = data.deviceText
                    ?: itemView.context.getString(R.string.launcher_feed_device_title)
                cardDevice.setOnClickListener { onOpenAllApps() }
            } else {
                cardDevice.visibility = View.GONE
                cardDevice.setOnClickListener(null)
            }

            if (data.showRotation) {
                cardRotation.visibility = View.VISIBLE
                cardRotationText.text = data.rotationText
                    ?: itemView.context.getString(R.string.launcher_feed_rotation_locked)
                cardRotation.setOnClickListener { onOpenDisplaySettings() }
            } else {
                cardRotation.visibility = View.GONE
                cardRotation.setOnClickListener(null)
            }

            if (data.showNfc) {
                cardNfc.visibility = View.VISIBLE
                cardNfcText.text = data.nfcText
                    ?: itemView.context.getString(R.string.launcher_feed_nfc_off)
                cardNfc.setOnClickListener { onOpenNetworkSettings() }
            } else {
                cardNfc.visibility = View.GONE
                cardNfc.setOnClickListener(null)
            }

            if (data.showDnd) {
                cardDnd.visibility = View.VISIBLE
                cardDndText.text = data.dndText
                    ?: itemView.context.getString(R.string.launcher_feed_dnd_off)
                cardDnd.setOnClickListener { onOpenDndSettings() }
            } else {
                cardDnd.visibility = View.GONE
                cardDnd.setOnClickListener(null)
            }

            if (data.showRinger) {
                cardRinger.visibility = View.VISIBLE
                cardRingerText.text = data.ringerText
                    ?: itemView.context.getString(R.string.launcher_feed_ringer_sound)
                cardRinger.setOnClickListener { onOpenSoundSettings() }
            } else {
                cardRinger.visibility = View.GONE
                cardRinger.setOnClickListener(null)
            }

            applyCardStyle(cardAlarm, colors, false)
            applyCardStyle(cardCalendar, colors, true)
            applyCardStyle(cardWeather, colors, false)
            applyCardStyle(cardBattery, colors, true)
            applyCardStyle(cardReminders, colors, false)
            applyCardStyle(cardHeadphones, colors, true)
            applyCardStyle(cardLocation, colors, false)
            applyCardStyle(cardNetwork, colors, true)
            applyCardStyle(cardStorage, colors, false)
            applyCardStyle(cardScreenTime, colors, true)
            applyCardStyle(cardBluetooth, colors, false)
            applyCardStyle(cardBrightness, colors, true)
            applyCardStyle(cardVolume, colors, false)
            applyCardStyle(cardPower, colors, true)
            applyCardStyle(cardTopApps, colors, false)
            applyCardStyle(cardAirplane, colors, true)
            applyCardStyle(cardRam, colors, false)
            applyCardStyle(cardDevice, colors, true)
            applyCardStyle(cardRotation, colors, false)
            applyCardStyle(cardNfc, colors, true)
            applyCardStyle(cardDnd, colors, true)
            applyCardStyle(cardRinger, colors, false)
            applyCardStyle(atGlanceCard, colors, true)

            nowCards.visibility = if (
                data.showAlarm ||
                data.showCalendar ||
                data.showWeather ||
                data.showReminders ||
                data.showHeadphones ||
                data.showNetwork ||
                data.showStorage ||
                data.showBluetooth ||
                data.showBrightness ||
                data.showVolume ||
                data.showPower ||
                data.showTopApps ||
                data.showAirplane ||
                data.showRam ||
                data.showDevice ||
                data.showRotation ||
                data.showNfc ||
                data.showDnd ||
                data.showRinger
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

        private fun applyCardStyle(view: View, colors: LauncherPrefs.ThemeColors, alt: Boolean) {
            ThemeUtils.applyCard(view, colors, alt)
        }
    }
}
