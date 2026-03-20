package com.screenreaders.blindroid.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllAppsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PAGE_INDEX = "extra_page_index"
        const val EXTRA_FOLDER_ID = "extra_folder_id"
        const val EXTRA_TAB = "extra_tab"
        const val TAB_APPS = "apps"
        const val TAB_WIDGETS = "widgets"
    }

    private lateinit var searchInput: EditText
    private lateinit var clearSearchButton: Button
    private lateinit var voiceSearchButton: Button
    private lateinit var resultsLabel: TextView
    private lateinit var showHiddenSwitch: android.widget.Switch
    private lateinit var showSystemSwitch: android.widget.Switch
    private lateinit var clearHiddenButton: Button
    private lateinit var scrollTopButton: Button
    private lateinit var scrollBottomButton: Button
    private lateinit var sortRow: LinearLayout
    private lateinit var sortLabel: TextView
    private lateinit var sortSpinner: Spinner
    private lateinit var appsGrid: RecyclerView
    private lateinit var suggestedGrid: RecyclerView
    private lateinit var suggestedLabel: TextView
    private lateinit var favoritesGrid: RecyclerView
    private lateinit var favoritesLabel: TextView
    private lateinit var suggestedNowGrid: RecyclerView
    private lateinit var suggestedNowLabel: TextView
    private lateinit var recentGrid: RecyclerView
    private lateinit var recentLabel: TextView
    private lateinit var usageAccessHint: TextView
    private lateinit var usageAccessButton: Button
    private lateinit var categoryRow: LinearLayout
    private lateinit var tabRow: LinearLayout
    private lateinit var appsTabButton: Button
    private lateinit var widgetsTabButton: Button
    private lateinit var appsContainer: LinearLayout
    private lateinit var widgetsContainer: LinearLayout
    private lateinit var addWidgetButton: Button
    private lateinit var listWidgetButton: Button
    private lateinit var gridWidgetsSwitch: android.widget.Switch
    private lateinit var widgetsGrid: GridLayout
    private lateinit var dragTargetsRow: LinearLayout
    private lateinit var dragHomeTarget: Button
    private lateinit var dragDockTarget: Button
    private lateinit var dragOptionsTarget: Button
    private lateinit var adapter: AppAdapter
    private lateinit var suggestedAdapter: AppAdapter
    private lateinit var favoritesAdapter: AppAdapter
    private lateinit var suggestedNowAdapter: AppAdapter
    private lateinit var recentAdapter: AppAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var suggestedLayoutManager: GridLayoutManager
    private lateinit var favoritesLayoutManager: GridLayoutManager
    private lateinit var suggestedNowLayoutManager: GridLayoutManager
    private lateinit var recentLayoutManager: GridLayoutManager
    private var soundFeedback: LauncherSoundFeedback? = null
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager
    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: AppWidgetProviderInfo? = null
    private var allApps: List<AppEntry> = emptyList()
    private var allAppsRaw: List<AppEntry> = emptyList()
    private var filteredApps: List<AppEntry> = emptyList()
    private var appCategories: Map<String, Int?> = emptyMap()
    private var installTimes: Map<String, Long> = emptyMap()
    private var updateTimes: Map<String, Long> = emptyMap()
    private var systemAppKeys: Set<String> = emptySet()
    private var favoriteKeys: Set<String> = emptySet()
    private var categoryCounts: Map<Category, Int> = emptyMap()
    private var targetPageIndex: Int = 0
    private var targetFolderId: String? = null
    private var currentCategory: Category = Category.ALL
    private var availableCategories: List<Category> = listOf(Category.ALL)
    private var currentTab: String = TAB_APPS
    private var draggingEntry: AppEntry? = null
    private val categoryValueCache = mutableMapOf<String, Int?>()

    private val hostId = 2048
    private val requestPickWidget = 2001
    private val requestBindWidget = 2002
    private val requestConfigureWidget = 2003
    private val requestVoiceSearch = 2104

    private enum class Category(val labelRes: Int, val categoryNames: List<String>) {
        ALL(R.string.launcher_category_all, emptyList()),
        FAVORITES(R.string.launcher_category_favorites, emptyList()),
        RECENT(R.string.launcher_category_recent, emptyList()),
        FREQUENT(R.string.launcher_category_frequent, emptyList()),
        MORNING(R.string.launcher_category_morning, emptyList()),
        DAY(R.string.launcher_category_day, emptyList()),
        EVENING(R.string.launcher_category_evening, emptyList()),
        NIGHT(R.string.launcher_category_night, emptyList()),
        NEW(R.string.launcher_category_new, emptyList()),
        UPDATED(R.string.launcher_category_updated, emptyList()),
        COMMUNICATION(R.string.launcher_category_communication, listOf("CATEGORY_COMMUNICATION")),
        SOCIAL(R.string.launcher_category_social, listOf("CATEGORY_SOCIAL")),
        PRODUCTIVITY(R.string.launcher_category_productivity, listOf("CATEGORY_PRODUCTIVITY")),
        MEDIA(R.string.launcher_category_media, listOf("CATEGORY_AUDIO", "CATEGORY_VIDEO", "CATEGORY_IMAGE")),
        TOOLS(R.string.launcher_category_tools, listOf("CATEGORY_TOOLS")),
        MAPS(R.string.launcher_category_maps, listOf("CATEGORY_MAPS")),
        NEWS(R.string.launcher_category_news, listOf("CATEGORY_NEWS")),
        FINANCE(R.string.launcher_category_finance, listOf("CATEGORY_FINANCE")),
        TRAVEL(R.string.launcher_category_travel, listOf("CATEGORY_TRAVEL_AND_LOCAL")),
        EDUCATION(R.string.launcher_category_education, listOf("CATEGORY_EDUCATION")),
        HEALTH(R.string.launcher_category_health, listOf("CATEGORY_HEALTH", "CATEGORY_HEALTH_AND_FITNESS")),
        WEATHER(R.string.launcher_category_weather, listOf("CATEGORY_WEATHER")),
        GAME(R.string.launcher_category_game, listOf("CATEGORY_GAME"))
    }

    private enum class SortMode(val labelRes: Int, val prefValue: Int) {
        ALPHA(R.string.launcher_sort_alpha, LauncherPrefs.SORT_ALPHA),
        RECENT(R.string.launcher_sort_recent, LauncherPrefs.SORT_RECENT),
        USAGE(R.string.launcher_sort_usage, LauncherPrefs.SORT_USAGE),
        INSTALL(R.string.launcher_sort_install, LauncherPrefs.SORT_INSTALL_NEWEST),
        UPDATE(R.string.launcher_sort_update, LauncherPrefs.SORT_UPDATE_NEWEST)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        searchInput = findViewById(R.id.searchInput)
        clearSearchButton = findViewById(R.id.clearSearchButton)
        voiceSearchButton = findViewById(R.id.voiceSearchButton)
        resultsLabel = findViewById(R.id.resultsLabel)
        showHiddenSwitch = findViewById(R.id.showHiddenSwitch)
        showSystemSwitch = findViewById(R.id.showSystemSwitch)
        clearHiddenButton = findViewById(R.id.clearHiddenButton)
        scrollTopButton = findViewById(R.id.scrollTopButton)
        scrollBottomButton = findViewById(R.id.scrollBottomButton)
        sortRow = findViewById(R.id.sortRow)
        sortLabel = findViewById(R.id.sortLabel)
        sortSpinner = findViewById(R.id.sortSpinner)
        appsGrid = findViewById(R.id.appsGrid)
        suggestedGrid = findViewById(R.id.suggestedGrid)
        suggestedLabel = findViewById(R.id.suggestedLabel)
        favoritesGrid = findViewById(R.id.favoritesGrid)
        favoritesLabel = findViewById(R.id.favoritesLabel)
        suggestedNowGrid = findViewById(R.id.suggestedNowGrid)
        suggestedNowLabel = findViewById(R.id.suggestedNowLabel)
        recentGrid = findViewById(R.id.recentGrid)
        recentLabel = findViewById(R.id.recentLabel)
        usageAccessHint = findViewById(R.id.usageAccessHint)
        usageAccessButton = findViewById(R.id.usageAccessButton)
        categoryRow = findViewById(R.id.categoryRow)
        tabRow = findViewById(R.id.tabRow)
        appsTabButton = findViewById(R.id.appsTabButton)
        widgetsTabButton = findViewById(R.id.widgetsTabButton)
        appsContainer = findViewById(R.id.appsContainer)
        widgetsContainer = findViewById(R.id.widgetsContainer)
        addWidgetButton = findViewById(R.id.addWidgetButton)
        listWidgetButton = findViewById(R.id.listWidgetButton)
        gridWidgetsSwitch = findViewById(R.id.gridWidgetsSwitch)
        widgetsGrid = findViewById(R.id.widgetsGrid)
        dragTargetsRow = findViewById(R.id.dragTargetsRow)
        dragHomeTarget = findViewById(R.id.dragHomeTarget)
        dragDockTarget = findViewById(R.id.dragDockTarget)
        dragOptionsTarget = findViewById(R.id.dragOptionsTarget)
        soundFeedback = LauncherSoundFeedback(this)
        setupSortSpinner()

        targetPageIndex = intent.getIntExtra(EXTRA_PAGE_INDEX, 0)
        targetFolderId = intent.getStringExtra(EXTRA_FOLDER_ID)
        currentTab = intent.getStringExtra(EXTRA_TAB) ?: TAB_APPS
        if (targetFolderId != null) {
            tabRow.visibility = android.view.View.GONE
            currentTab = TAB_APPS
            showHiddenSwitch.visibility = android.view.View.GONE
            clearHiddenButton.visibility = android.view.View.GONE
        }

        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager = GridLayoutManager(this, baseConfig.columns)
        suggestedLayoutManager = GridLayoutManager(this, baseConfig.columns)
        favoritesLayoutManager = GridLayoutManager(this, baseConfig.columns)
        suggestedNowLayoutManager = GridLayoutManager(this, baseConfig.columns)
        recentLayoutManager = GridLayoutManager(this, baseConfig.columns)
        appsGrid.layoutManager = gridLayoutManager
        suggestedGrid.layoutManager = suggestedLayoutManager
        favoritesGrid.layoutManager = favoritesLayoutManager
        suggestedNowGrid.layoutManager = suggestedNowLayoutManager
        recentGrid.layoutManager = recentLayoutManager
        adapter = AppAdapter(
            emptyList(),
            baseConfig.copy(showLabels = true),
            ::launchApp,
            ::handleLongPress,
            if (targetFolderId == null) ::startDragAdd else null
        )
        suggestedAdapter = AppAdapter(
            emptyList(),
            baseConfig.copy(showLabels = true),
            ::launchApp,
            ::handleLongPress,
            if (targetFolderId == null) ::startDragAdd else null
        )
        favoritesAdapter = AppAdapter(
            emptyList(),
            baseConfig.copy(showLabels = true),
            ::launchApp,
            ::handleLongPress,
            if (targetFolderId == null) ::startDragAdd else null
        )
        suggestedNowAdapter = AppAdapter(
            emptyList(),
            baseConfig.copy(showLabels = true),
            ::launchApp,
            ::handleLongPress,
            if (targetFolderId == null) ::startDragAdd else null
        )
        recentAdapter = AppAdapter(
            emptyList(),
            baseConfig.copy(showLabels = true),
            ::launchApp,
            ::handleLongPress,
            if (targetFolderId == null) ::startDragAdd else null
        )
        appsGrid.adapter = adapter
        suggestedGrid.adapter = suggestedAdapter
        favoritesGrid.adapter = favoritesAdapter
        suggestedNowGrid.adapter = suggestedNowAdapter
        recentGrid.adapter = recentAdapter

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, hostId)

        addWidgetButton.setOnClickListener {
            soundFeedback?.playTap()
            pickWidget()
        }
        listWidgetButton.setOnClickListener {
            soundFeedback?.playTap()
            showWidgetList()
        }
        gridWidgetsSwitch.setOnCheckedChangeListener { _, isChecked ->
            widgetsGrid.columnCount = if (isChecked) 2 else 1
            reloadWidgets()
        }
        showHiddenSwitch.setOnCheckedChangeListener { _, _ -> refreshAppsForHidden() }
        showSystemSwitch.isChecked = LauncherPrefs.isShowSystemApps(this)
        showSystemSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setShowSystemApps(this, isChecked)
            refreshAppsForHidden()
        }
        clearSearchButton.setOnClickListener {
            searchInput.setText("")
            searchInput.clearFocus()
        }
        clearHiddenButton.setOnClickListener {
            LauncherStore.clearHiddenApps(this)
            refreshAppsForHidden()
            Toast.makeText(this, R.string.launcher_clear_hidden, Toast.LENGTH_SHORT).show()
        }
        voiceSearchButton.setOnClickListener {
            startVoiceSearch()
        }
        scrollTopButton.setOnClickListener { appsGrid.smoothScrollToPosition(0) }
        scrollBottomButton.setOnClickListener {
            val last = (adapter.itemCount - 1).coerceAtLeast(0)
            appsGrid.smoothScrollToPosition(last)
        }
        widgetsGrid.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DROP -> {
                    val widgetId = event.localState as? Int ?: return@setOnDragListener true
                    val list = LauncherStore.getWidgetIds(this)
                    if (list.isNotEmpty()) {
                        LauncherStore.moveWidgetTo(this, widgetId, list.size - 1)
                        reloadWidgets()
                    }
                    true
                }
                else -> true
            }
        }
        usageAccessButton.setOnClickListener {
            soundFeedback?.playTap()
            LauncherStore.openUsageAccessSettings(this)
        }

        appsTabButton.setOnClickListener { switchTab(TAB_APPS) }
        widgetsTabButton.setOnClickListener { switchTab(TAB_WIDGETS) }
        setupDragTargets()

        loadApps()
        setupSearch()
        switchTab(currentTab, playSound = false)
    }

    override fun onResume() {
        super.onResume()
        applyUiConfig()
        applyTheme()
        updateUsageAccessUi()
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundFeedback?.release()
        soundFeedback = null
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            val meta = buildAppMeta(apps)
            runOnUiThread {
                allAppsRaw = apps
                installTimes = meta.installs
                updateTimes = meta.updates
                systemAppKeys = meta.systemKeys
                favoriteKeys = LauncherStore.getFavoriteKeys(this)
                refreshAppsForHidden()
                updateUsageAccessUi()
            }
        }.start()
    }

    private fun refreshAppsForHidden() {
        val hidden = LauncherStore.getHiddenAppKeys(this)
        val showSystem = LauncherPrefs.isShowSystemApps(this)
        allApps = if (showHiddenSwitch.isChecked) {
            allAppsRaw
        } else {
            allAppsRaw.filterNot { hidden.contains(it.component.flattenToString()) }
        }
        if (!showSystem) {
            allApps = allApps.filterNot { systemAppKeys.contains(it.component.flattenToString()) }
        }
        clearHiddenButton.visibility = if (hidden.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        appCategories = allApps.associate { entry ->
            entry.component.flattenToString() to getAppCategory(entry)
        }
        availableCategories = buildAvailableCategories()
        setupCategories()
        applyFilters()
        updateSuggested()
    }

    private fun setupSortSpinner() {
        val labels = SortMode.values().map { getString(it.labelRes) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter
        val current = LauncherPrefs.getAppSortMode(this)
        val index = SortMode.values().indexOfFirst { it.prefValue == current }.coerceAtLeast(0)
        sortSpinner.setSelection(index)
        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = SortMode.values().getOrNull(position) ?: SortMode.ALPHA
                LauncherPrefs.setAppSortMode(this@AllAppsActivity, mode.prefValue)
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager.spanCount = baseConfig.columns
        suggestedLayoutManager.spanCount = baseConfig.columns
        favoritesLayoutManager.spanCount = baseConfig.columns
        suggestedNowLayoutManager.spanCount = baseConfig.columns
        recentLayoutManager.spanCount = baseConfig.columns
        adapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        suggestedAdapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        favoritesAdapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        suggestedNowAdapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        recentAdapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        widgetsGrid.columnCount = if (gridWidgetsSwitch.isChecked) 2 else 1
    }

    private fun applyTheme() {
        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(colors.background)
        searchInput.setTextColor(colors.text)
        searchInput.setHintTextColor(colors.muted)
        clearSearchButton.setTextColor(colors.text)
        voiceSearchButton.setTextColor(colors.text)
        resultsLabel.setTextColor(colors.muted)
        showHiddenSwitch.setTextColor(colors.text)
        showSystemSwitch.setTextColor(colors.text)
        clearHiddenButton.setTextColor(colors.text)
        sortLabel.setTextColor(colors.text)
        suggestedLabel.setTextColor(colors.text)
        favoritesLabel.setTextColor(colors.text)
        suggestedNowLabel.setTextColor(colors.text)
        recentLabel.setTextColor(colors.text)
        scrollTopButton.setTextColor(colors.text)
        scrollBottomButton.setTextColor(colors.text)
        usageAccessHint.setTextColor(colors.muted)
        usageAccessButton.setTextColor(colors.text)
        appsTabButton.setTextColor(colors.text)
        widgetsTabButton.setTextColor(colors.text)
        addWidgetButton.setTextColor(colors.text)
        listWidgetButton.setTextColor(colors.text)
        gridWidgetsSwitch.setTextColor(colors.text)
        dragHomeTarget.setTextColor(colors.text)
        dragDockTarget.setTextColor(colors.text)
        dragOptionsTarget.setTextColor(colors.text)
        for (i in 0 until categoryRow.childCount) {
            val button = categoryRow.getChildAt(i) as android.widget.Button
            button.setTextColor(colors.text)
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                clearSearchButton.visibility = if (s.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
                applyFilters()
            }
        })
        clearSearchButton.visibility = if (searchInput.text.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun setupDragTargets() {
        val listener = android.view.View.OnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DROP -> false
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    hideDragTargets()
                    true
                }
                else -> true
            }
        }
        dragTargetsRow.setOnDragListener(listener)

        dragHomeTarget.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DROP -> {
                    val entry = draggingEntry ?: return@setOnDragListener true
                    val added = LauncherStore.addToPage(this, targetPageIndex, entry.component)
                    Toast.makeText(
                        this,
                        if (added) R.string.launcher_added_to_home else R.string.launcher_already_on_home,
                        Toast.LENGTH_SHORT
                    ).show()
                    hideDragTargets()
                    if (added) finish()
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    hideDragTargets()
                    true
                }
                else -> true
            }
        }

        dragDockTarget.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DROP -> {
                    val entry = draggingEntry ?: return@setOnDragListener true
                    val added = LauncherStore.addToHotseat(this, entry.component)
                    Toast.makeText(
                        this,
                        if (added) R.string.launcher_added_to_hotseat else R.string.launcher_already_in_hotseat,
                        Toast.LENGTH_SHORT
                    ).show()
                    hideDragTargets()
                    if (added) finish()
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    hideDragTargets()
                    true
                }
                else -> true
            }
        }

        dragOptionsTarget.setOnDragListener { _, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DROP -> {
                    val entry = draggingEntry ?: return@setOnDragListener true
                    hideDragTargets()
                    handleLongPress(entry)
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    hideDragTargets()
                    true
                }
                else -> true
            }
        }
    }

    private fun startDragAdd(entry: AppEntry, view: android.view.View) {
        draggingEntry = entry
        dragTargetsRow.visibility = android.view.View.VISIBLE
        val data = android.content.ClipData.newPlainText("app", entry.component.flattenToString())
        view.startDragAndDrop(data, android.view.View.DragShadowBuilder(view), null, 0)
    }

    private fun hideDragTargets() {
        draggingEntry = null
        dragTargetsRow.visibility = android.view.View.GONE
    }

    private fun switchTab(tab: String, playSound: Boolean = true) {
        currentTab = tab
        val showApps = tab == TAB_APPS
        searchInput.visibility = if (showApps) android.view.View.VISIBLE else android.view.View.GONE
        val showControls = showApps && targetFolderId == null
        showHiddenSwitch.visibility = if (showControls) android.view.View.VISIBLE else android.view.View.GONE
        clearHiddenButton.visibility = if (showControls && LauncherStore.getHiddenAppKeys(this).isNotEmpty()) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
        showSystemSwitch.visibility = if (showControls) android.view.View.VISIBLE else android.view.View.GONE
        sortRow.visibility = if (showControls) android.view.View.VISIBLE else android.view.View.GONE
        resultsLabel.visibility = if (showApps) resultsLabel.visibility else android.view.View.GONE
        scrollTopButton.visibility = if (showApps) android.view.View.VISIBLE else android.view.View.GONE
        scrollBottomButton.visibility = if (showApps) android.view.View.VISIBLE else android.view.View.GONE
        appsContainer.visibility = if (showApps) android.view.View.VISIBLE else android.view.View.GONE
        widgetsContainer.visibility = if (showApps) android.view.View.GONE else android.view.View.VISIBLE
        appsTabButton.isEnabled = !showApps
        widgetsTabButton.isEnabled = showApps
        appsTabButton.alpha = if (showApps) 1.0f else 0.5f
        widgetsTabButton.alpha = if (showApps) 0.5f else 1.0f
        if (playSound) {
            soundFeedback?.playAction(
                if (showApps) LauncherPrefs.ACTION_OPEN_ALL_APPS else LauncherPrefs.ACTION_OPEN_WIDGETS
            )
        }
        if (!showApps) {
            reloadWidgets()
        }
    }

    private fun setupCategories() {
        categoryRow.removeAllViews()
        availableCategories.forEach { category ->
            val button = android.widget.Button(this)
            val baseLabel = getString(category.labelRes)
            val count = categoryCounts[category]
            button.text = if (count != null && category != Category.ALL) {
                "$baseLabel ($count)"
            } else {
                baseLabel
            }
            button.setOnClickListener {
                currentCategory = category
                applyFilters()
                updateCategoryButtons()
            }
            categoryRow.addView(button)
        }
        updateCategoryButtons()
    }

    private fun updateCategoryButtons() {
        for (i in 0 until categoryRow.childCount) {
            val button = categoryRow.getChildAt(i) as android.widget.Button
            val category = availableCategories[i]
            val selected = category == currentCategory
            button.alpha = if (selected) 1.0f else 0.5f
            button.isEnabled = !selected
        }
    }

    private fun applyFilters() {
        val query = searchInput.text?.toString()?.trim().orEmpty().lowercase()
        val now = System.currentTimeMillis()
        val newKeys = if (currentCategory == Category.NEW) {
            installTimes.filterValues { now - it <= 7L * 24L * 60L * 60L * 1000L }.keys
        } else {
            emptySet()
        }
        val updatedKeys = if (currentCategory == Category.UPDATED) {
            updateTimes.filterValues { now - it <= 7L * 24L * 60L * 60L * 1000L }.keys
        } else {
            emptySet()
        }
        val favoriteSet = if (currentCategory == Category.FAVORITES) {
            favoriteKeys
        } else {
            emptySet()
        }
        val frequentKeys = if (currentCategory == Category.FREQUENT) {
            LauncherStore.getSuggestedApps(this, allApps, 20)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        val recentKeys = if (currentCategory == Category.RECENT) {
            LauncherStore.getRecentApps(this, allApps, 48, 40)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        val morningKeys = if (currentCategory == Category.MORNING) {
            LauncherStore.getSuggestedAppsForBucket(this, allApps, "morning", 40)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        val dayKeys = if (currentCategory == Category.DAY) {
            LauncherStore.getSuggestedAppsForBucket(this, allApps, "day", 40)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        val eveningKeys = if (currentCategory == Category.EVENING) {
            LauncherStore.getSuggestedAppsForBucket(this, allApps, "evening", 40)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        val nightKeys = if (currentCategory == Category.NIGHT) {
            LauncherStore.getSuggestedAppsForBucket(this, allApps, "night", 40)
                .map { it.component.flattenToString() }
                .toSet()
        } else {
            emptySet()
        }
        filteredApps = allApps.filter { entry ->
            val matchesQuery = query.isBlank() ||
                entry.label.lowercase().contains(query) ||
                entry.component.packageName.lowercase().contains(query)
            val category = appCategories[entry.component.flattenToString()]
            val matchesCategory = when (currentCategory) {
                Category.FAVORITES -> favoriteSet.contains(entry.component.flattenToString())
                Category.NEW -> newKeys.contains(entry.component.flattenToString())
                Category.UPDATED -> updatedKeys.contains(entry.component.flattenToString())
                Category.FREQUENT -> frequentKeys.contains(entry.component.flattenToString())
                Category.RECENT -> recentKeys.contains(entry.component.flattenToString())
                Category.MORNING -> morningKeys.contains(entry.component.flattenToString())
                Category.DAY -> dayKeys.contains(entry.component.flattenToString())
                Category.EVENING -> eveningKeys.contains(entry.component.flattenToString())
                Category.NIGHT -> nightKeys.contains(entry.component.flattenToString())
                else -> matchesCategory(currentCategory, category, entry)
            }
            matchesQuery && matchesCategory
        }
        adapter.submit(sortApps(filteredApps))
        if (query.isNotBlank() || currentCategory != Category.ALL) {
            resultsLabel.text = getString(R.string.launcher_results_count_format, filteredApps.size)
            resultsLabel.visibility = android.view.View.VISIBLE
        } else {
            resultsLabel.visibility = android.view.View.GONE
        }
    }

    private fun startVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pl-PL")
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.launcher_voice_search_prompt))
            }
            startActivityForResult(intent, requestVoiceSearch)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_search_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAppCategory(entry: AppEntry): Int? {
        return try {
            val info = packageManager.getApplicationInfo(entry.component.packageName, 0)
            info.category
        } catch (_: Exception) {
            null
        }
    }

    private fun matchesCategory(category: Category, appCategory: Int?, entry: AppEntry): Boolean {
        if (category == Category.ALL ||
            category == Category.FAVORITES ||
            category == Category.FREQUENT ||
            category == Category.RECENT ||
            category == Category.MORNING ||
            category == Category.DAY ||
            category == Category.EVENING ||
            category == Category.NIGHT ||
            category == Category.NEW ||
            category == Category.UPDATED
        ) return true
        if (appCategory == null) {
            val inferred = inferCategory(entry)
            return inferred == category
        }
        return category.categoryNames.any { name ->
            val value = resolveCategoryValue(name) ?: return@any false
            appCategory == value
        }
    }

    private fun inferCategory(entry: AppEntry): Category? {
        val text = (entry.label + " " + entry.component.packageName).lowercase()
        val rules = listOf(
            Category.COMMUNICATION to listOf("dialer", "phone", "contacts", "sms", "messaging", "whatsapp", "telegram", "signal", "viber", "messenger", "skype", "meet", "zoom"),
            Category.SOCIAL to listOf("facebook", "instagram", "tiktok", "twitter", "x.com", "threads", "snapchat"),
            Category.MEDIA to listOf("music", "spotify", "soundcloud", "radio", "podcast", "video", "youtube", "netflix", "player"),
            Category.MAPS to listOf("maps", "gps", "navigation", "waze", "osm", "mapy"),
            Category.NEWS to listOf("news", "gazeta", "wiadomosci", "rss"),
            Category.FINANCE to listOf("bank", "wallet", "pay", "finance", "blik"),
            Category.TRAVEL to listOf("trip", "booking", "uber", "bolt", "taxi"),
            Category.EDUCATION to listOf("edu", "school", "learn", "duolingo", "kurs"),
            Category.HEALTH to listOf("fit", "health", "fitness", "zdrowie"),
            Category.WEATHER to listOf("weather", "pogoda", "meteo"),
            Category.GAME to listOf("game", "gry"),
            Category.TOOLS to listOf("calculator", "clock", "files", "settings", "notes")
        )
        return rules.firstOrNull { (_, keywords) -> keywords.any { text.contains(it) } }?.first
    }

    private fun resolveCategoryValue(name: String): Int? {
        return categoryValueCache.getOrPut(name) {
            try {
                ApplicationInfo::class.java.getField(name).getInt(null)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun buildAvailableCategories(): List<Category> {
        val present = mutableSetOf<Category>()
        val counts = mutableMapOf<Category, Int>()
        allApps.forEach { entry ->
            val appCategory = appCategories[entry.component.flattenToString()]
            Category.values().forEach { category ->
                if (category != Category.ALL && category != Category.FREQUENT && category != Category.RECENT &&
                    category != Category.NEW && category != Category.UPDATED && category != Category.FAVORITES
                ) {
                    if (matchesCategory(category, appCategory, entry)) {
                        present.add(category)
                        counts[category] = (counts[category] ?: 0) + 1
                    }
                }
            }
        }
        val list = mutableListOf(Category.ALL)
        val favorites = favoriteKeys
        if (favorites.isNotEmpty()) {
            list.add(Category.FAVORITES)
            counts[Category.FAVORITES] = favorites.size
        }
        val newApps = getNewAppKeys()
        if (newApps.isNotEmpty()) {
            list.add(Category.NEW)
            counts[Category.NEW] = newApps.size
        }
        val updatedApps = getUpdatedAppKeys()
        if (updatedApps.isNotEmpty()) {
            list.add(Category.UPDATED)
            counts[Category.UPDATED] = updatedApps.size
        }
        val recent = LauncherStore.getRecentApps(this, allApps, 48, 12)
        if (recent.isNotEmpty()) {
            list.add(Category.RECENT)
            counts[Category.RECENT] = recent.size
        }
        val frequent = LauncherStore.getSuggestedApps(this, allApps, 12)
        if (frequent.isNotEmpty()) {
            list.add(Category.FREQUENT)
            counts[Category.FREQUENT] = frequent.size
        }
        val morning = LauncherStore.getSuggestedAppsForBucket(this, allApps, "morning", 12)
        if (morning.isNotEmpty()) {
            list.add(Category.MORNING)
            counts[Category.MORNING] = morning.size
        }
        val day = LauncherStore.getSuggestedAppsForBucket(this, allApps, "day", 12)
        if (day.isNotEmpty()) {
            list.add(Category.DAY)
            counts[Category.DAY] = day.size
        }
        val evening = LauncherStore.getSuggestedAppsForBucket(this, allApps, "evening", 12)
        if (evening.isNotEmpty()) {
            list.add(Category.EVENING)
            counts[Category.EVENING] = evening.size
        }
        val night = LauncherStore.getSuggestedAppsForBucket(this, allApps, "night", 12)
        if (night.isNotEmpty()) {
            list.add(Category.NIGHT)
            counts[Category.NIGHT] = night.size
        }
        val ranked = Category.values()
            .filter {
                it != Category.ALL &&
                    it != Category.FAVORITES &&
                    it != Category.NEW &&
                    it != Category.UPDATED &&
                    it != Category.FREQUENT &&
                    it != Category.RECENT &&
                    it != Category.MORNING &&
                    it != Category.DAY &&
                    it != Category.EVENING &&
                    it != Category.NIGHT
            }
            .filter { present.contains(it) }
            .sortedByDescending { counts[it] ?: 0 }
        list.addAll(ranked)
        categoryCounts = counts
        if (!list.contains(currentCategory)) {
            currentCategory = Category.ALL
        }
        return list
    }

    private fun getNewAppKeys(): Set<String> {
        if (installTimes.isEmpty()) return emptySet()
        val now = System.currentTimeMillis()
        val cutoff = now - 7L * 24L * 60L * 60L * 1000L
        return installTimes.filterValues { it >= cutoff }.keys
    }

    private fun getUpdatedAppKeys(): Set<String> {
        if (updateTimes.isEmpty()) return emptySet()
        val now = System.currentTimeMillis()
        val cutoff = now - 7L * 24L * 60L * 60L * 1000L
        return updateTimes.filterValues { it >= cutoff }.keys
    }

    private fun sortApps(input: List<AppEntry>): List<AppEntry> {
        return when (LauncherPrefs.getAppSortMode(this)) {
            LauncherPrefs.SORT_RECENT -> sortByOrder(input, LauncherStore.getRecentApps(this, allApps, 168, allApps.size))
            LauncherPrefs.SORT_USAGE -> sortByOrder(input, LauncherStore.getSuggestedApps(this, allApps, allApps.size))
            LauncherPrefs.SORT_INSTALL_NEWEST -> input.sortedWith(
                compareByDescending<AppEntry> { installTimes[it.component.flattenToString()] ?: 0L }
                    .thenBy { it.label.lowercase() }
            )
            LauncherPrefs.SORT_UPDATE_NEWEST -> input.sortedWith(
                compareByDescending<AppEntry> { updateTimes[it.component.flattenToString()] ?: 0L }
                    .thenBy { it.label.lowercase() }
            )
            else -> input.sortedBy { it.label.lowercase() }
        }
    }

    private fun sortByOrder(input: List<AppEntry>, ordered: List<AppEntry>): List<AppEntry> {
        if (input.isEmpty()) return input
        val orderMap = ordered.mapIndexed { index, entry ->
            entry.component.flattenToString() to index
        }.toMap()
        return input.sortedWith(
            compareBy<AppEntry> { orderMap[it.component.flattenToString()] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() }
        )
    }

    private data class AppMeta(
        val installs: Map<String, Long>,
        val updates: Map<String, Long>,
        val systemKeys: Set<String>
    )

    private fun buildAppMeta(apps: List<AppEntry>): AppMeta {
        val installs = mutableMapOf<String, Long>()
        val updates = mutableMapOf<String, Long>()
        val system = mutableSetOf<String>()
        apps.forEach { entry ->
            try {
                val pkg = entry.component.packageName
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(pkg, 0)
                }
                installs[entry.component.flattenToString()] = info.firstInstallTime
                updates[entry.component.flattenToString()] = info.lastUpdateTime
                val appInfo = packageManager.getApplicationInfo(pkg, 0)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (isSystem) {
                    system.add(entry.component.flattenToString())
                }
            } catch (_: Exception) {
                // Ignore missing package
            }
        }
        return AppMeta(installs, updates, system)
    }

    private fun updateSuggested() {
        val favoriteEntries = allApps.filter { favoriteKeys.contains(it.component.flattenToString()) }
        if (favoriteEntries.isEmpty()) {
            favoritesLabel.visibility = android.view.View.GONE
            favoritesGrid.visibility = android.view.View.GONE
        } else {
            favoritesLabel.visibility = android.view.View.VISIBLE
            favoritesGrid.visibility = android.view.View.VISIBLE
            favoritesAdapter.submit(favoriteEntries)
        }
        val suggestedNow = LauncherStore.getSuggestedAppsForBucket(this, allApps, currentBucket(), 4)
        if (suggestedNow.isEmpty()) {
            suggestedNowLabel.visibility = android.view.View.GONE
            suggestedNowGrid.visibility = android.view.View.GONE
        } else {
            suggestedNowLabel.visibility = android.view.View.VISIBLE
            suggestedNowGrid.visibility = android.view.View.VISIBLE
            suggestedNowAdapter.submit(suggestedNow)
        }
        val suggested = LauncherStore.getSuggestedApps(this, allApps, 4)
        if (suggested.isEmpty()) {
            suggestedLabel.visibility = android.view.View.GONE
            suggestedGrid.visibility = android.view.View.GONE
        } else {
            suggestedLabel.visibility = android.view.View.VISIBLE
            suggestedGrid.visibility = android.view.View.VISIBLE
            suggestedAdapter.submit(suggested)
        }
        val recent = LauncherStore.getRecentApps(this, allApps, 48, 8)
        if (recent.isEmpty()) {
            recentLabel.visibility = android.view.View.GONE
            recentGrid.visibility = android.view.View.GONE
        } else {
            recentLabel.visibility = android.view.View.VISIBLE
            recentGrid.visibility = android.view.View.VISIBLE
            recentAdapter.submit(recent)
        }
    }

    private fun currentBucket(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "morning"
            in 11..16 -> "day"
            in 17..21 -> "evening"
            else -> "night"
        }
    }

    private fun updateUsageAccessUi() {
        val wantsUsage = LauncherPrefs.isUsageSuggestionsEnabled(this)
        val hasUsage = LauncherStore.hasUsageStatsPermission(this)
        if (wantsUsage && !hasUsage) {
            usageAccessHint.visibility = android.view.View.VISIBLE
            usageAccessButton.visibility = android.view.View.VISIBLE
        } else {
            usageAccessHint.visibility = android.view.View.GONE
            usageAccessButton.visibility = android.view.View.GONE
        }
    }

    private fun pickWidget() {
        pendingWidgetId = appWidgetHost.allocateAppWidgetId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        startActivityForResult(intent, requestPickWidget)
    }

    private fun showWidgetList() {
        val providers = appWidgetManager.installedProviders
        if (providers.isNullOrEmpty()) {
            Toast.makeText(this, R.string.launcher_no_widgets, Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_widget_list, null)
        val searchInput = dialogView.findViewById<EditText>(R.id.widgetSearchInput)
        val recycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.widgetListRecycler)
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val entries = providers.map { info ->
            val label = info.label ?: info.provider.className
            val sizeText = getString(R.string.launcher_widget_size_format, info.minWidth, info.minHeight)
            val searchText = (label + " " + info.provider.packageName).lowercase()
            WidgetProviderEntry(info, label, sizeText, searchText, loadWidgetPreview(info))
        }
        val adapter = WidgetPreviewAdapter(entries) { entry ->
            pendingProvider = entry.info
            pendingWidgetId = appWidgetHost.allocateAppWidgetId()
            bindWidgetFromProvider(entry.info, pendingWidgetId)
        }
        recycler.adapter = adapter
        val colors = LauncherPrefs.getThemeColors(this)
        searchInput.setTextColor(colors.text)
        searchInput.setHintTextColor(colors.muted)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString().orEmpty())
            }
        })
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_widgets_list)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestVoiceSearch && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = results?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                searchInput.setText(text)
                searchInput.setSelection(text.length)
            }
            return
        }
        val appWidgetId = data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingWidgetId
        ) ?: pendingWidgetId

        if (resultCode != RESULT_OK) {
            cleanupWidgetId(appWidgetId)
            return
        }

        when (requestCode) {
            requestPickWidget -> handleWidgetPicked(appWidgetId)
            requestBindWidget -> handleWidgetBound(appWidgetId)
            requestConfigureWidget -> completeAddWidget(appWidgetId)
        }
    }

    private fun handleWidgetPicked(appWidgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            cleanupWidgetId(appWidgetId)
            return
        }
        bindWidgetFromProvider(info, appWidgetId)
    }

    private fun bindWidgetFromProvider(info: AppWidgetProviderInfo, appWidgetId: Int) {
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        if (!bound) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            startActivityForResult(intent, requestBindWidget)
            return
        }
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = info.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, requestConfigureWidget)
        } else {
            completeAddWidget(appWidgetId)
        }
    }

    private fun handleWidgetBound(appWidgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            cleanupWidgetId(appWidgetId)
            return
        }
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = info.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, requestConfigureWidget)
        } else {
            completeAddWidget(appWidgetId)
        }
    }

    private fun completeAddWidget(appWidgetId: Int) {
        LauncherStore.addWidgetId(this, appWidgetId)
        addWidgetView(appWidgetId)
        Toast.makeText(this, R.string.launcher_widget_added, Toast.LENGTH_SHORT).show()
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    private fun reloadWidgets() {
        widgetsGrid.removeAllViews()
        LauncherStore.getWidgetIds(this).forEach { widgetId ->
            addWidgetView(widgetId)
        }
    }

    private fun addWidgetView(widgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return
        val hostView = appWidgetHost.createView(this, widgetId, info)
        val wrapper = LinearLayout(this)
        wrapper.orientation = LinearLayout.VERTICAL

        val controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        val bigger = Button(this)
        val smaller = Button(this)
        val moveUp = Button(this)
        val moveDown = Button(this)
        bigger.text = getString(R.string.launcher_widget_bigger)
        smaller.text = getString(R.string.launcher_widget_smaller)
        moveUp.text = getString(R.string.launcher_widget_move_up)
        moveDown.text = getString(R.string.launcher_widget_move_down)
        controls.addView(bigger)
        controls.addView(smaller)
        controls.addView(moveUp)
        controls.addView(moveDown)
        wrapper.addView(controls)
        wrapper.addView(hostView)

        val params = GridLayout.LayoutParams()
        params.width = if (widgetsGrid.columnCount > 1) 0 else GridLayout.LayoutParams.MATCH_PARENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.launcher_widget_margin))
        wrapper.layoutParams = params
        wrapper.tag = widgetId

        val size = LauncherStore.getWidgetSize(this, widgetId)
        if (size != null) {
            hostView.layoutParams = LinearLayout.LayoutParams(size.first, size.second)
        } else {
            val minWidth = dpToPx(info.minWidth.toFloat())
            val minHeight = dpToPx(info.minHeight.toFloat())
            hostView.layoutParams = LinearLayout.LayoutParams(minWidth, minHeight)
            LauncherStore.setWidgetSize(this, widgetId, minWidth, minHeight)
        }

        bigger.setOnClickListener { resizeWidget(widgetId, hostView, 30) }
        smaller.setOnClickListener { resizeWidget(widgetId, hostView, -30) }
        moveUp.setOnClickListener {
            LauncherStore.moveWidget(this, widgetId, -1)
            reloadWidgets()
        }
        moveDown.setOnClickListener {
            LauncherStore.moveWidget(this, widgetId, 1)
            reloadWidgets()
        }
        wrapper.setOnLongClickListener { view ->
            val clip = android.content.ClipData.newPlainText("widget", widgetId.toString())
            view.startDragAndDrop(clip, android.view.View.DragShadowBuilder(view), widgetId, 0)
            true
        }
        wrapper.setOnDragListener { view, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DROP -> {
                    val fromId = event.localState as? Int ?: return@setOnDragListener true
                    val toId = view.tag as? Int ?: return@setOnDragListener true
                    if (fromId != toId) {
                        val list = LauncherStore.getWidgetIds(this)
                        val targetIndex = list.indexOf(toId)
                        if (targetIndex >= 0) {
                            LauncherStore.moveWidgetTo(this, fromId, targetIndex)
                            reloadWidgets()
                        }
                    }
                    true
                }
                else -> true
            }
        }
        hostView.setOnLongClickListener {
            confirmRemoveWidget(widgetId, wrapper)
            true
        }

        widgetsGrid.addView(wrapper)
    }

    private fun resizeWidget(widgetId: Int, view: android.view.View, deltaDp: Int) {
        val deltaPx = dpToPx(deltaDp.toFloat())
        val size = LauncherStore.getWidgetSize(this, widgetId)
        val width = (size?.first ?: view.width) + deltaPx
        val height = (size?.second ?: view.height) + deltaPx
        val newW = width.coerceAtLeast(dpToPx(60f))
        val newH = height.coerceAtLeast(dpToPx(60f))
        view.layoutParams = LinearLayout.LayoutParams(newW, newH)
        LauncherStore.setWidgetSize(this, widgetId, newW, newH)
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    private fun confirmRemoveWidget(widgetId: Int, wrapper: android.view.View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_remove_widget)
            .setMessage(R.string.launcher_remove_widget_message)
            .setPositiveButton(R.string.launcher_action_remove_widget) { _, _ ->
                removeWidget(widgetId, wrapper)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removeWidget(widgetId: Int, wrapper: android.view.View) {
        widgetsGrid.removeView(wrapper)
        LauncherStore.removeWidgetId(this, widgetId)
        appWidgetHost.deleteAppWidgetId(widgetId)
        Toast.makeText(this, R.string.launcher_widget_removed, Toast.LENGTH_SHORT).show()
    }

    private fun cleanupWidgetId(widgetId: Int) {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private data class WidgetProviderEntry(
        val info: AppWidgetProviderInfo,
        val label: String,
        val sizeText: String,
        val searchText: String,
        val preview: android.graphics.drawable.Drawable?
    )

    private inner class WidgetPreviewAdapter(
        private val items: List<WidgetProviderEntry>,
        private val onSelect: (WidgetProviderEntry) -> Unit
    ) : RecyclerView.Adapter<WidgetPreviewAdapter.WidgetViewHolder>() {

        private val filtered = items.toMutableList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
            val view = layoutInflater.inflate(R.layout.item_widget_preview, parent, false)
            return WidgetViewHolder(view)
        }

        override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
            val item = filtered[position]
            holder.label.text = item.label
            holder.label.setTextColor(LauncherPrefs.getThemeColors(this@AllAppsActivity).text)
            holder.size.text = item.sizeText
            holder.size.setTextColor(LauncherPrefs.getThemeColors(this@AllAppsActivity).muted)
            holder.preview.setImageDrawable(item.preview)
            holder.itemView.setOnClickListener {
                onSelect(item)
            }
        }

        override fun getItemCount(): Int = filtered.size

        fun filter(query: String) {
            val needle = query.trim().lowercase()
            filtered.clear()
            if (needle.isBlank()) {
                filtered.addAll(items)
            } else {
                filtered.addAll(items.filter { it.searchText.contains(needle) })
            }
            notifyDataSetChanged()
        }

        inner class WidgetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val preview: android.widget.ImageView = view.findViewById(R.id.widgetPreviewImage)
            val label: TextView = view.findViewById(R.id.widgetPreviewLabel)
            val size: TextView = view.findViewById(R.id.widgetPreviewSize)
        }
    }

    private fun loadWidgetPreview(info: AppWidgetProviderInfo): android.graphics.drawable.Drawable? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                info.loadPreviewImage(this, resources.displayMetrics.densityDpi)
            } else {
                val res = packageManager.getResourcesForApplication(info.provider.packageName)
                if (info.previewImage != 0) {
                    res.getDrawable(info.previewImage, theme)
                } else {
                    info.loadIcon(this, resources.displayMetrics.densityDpi)
                }
            }
        } catch (_: Exception) {
            try {
                info.loadIcon(this, resources.displayMetrics.densityDpi)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun handleLongPress(entry: AppEntry) {
        val folderId = targetFolderId
        if (folderId != null) {
            val added = LauncherStore.addToFolder(this, folderId, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                Toast.LENGTH_SHORT
            ).show()
            if (added) {
                finish()
            }
            return
        }

        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_add_to_home) to {
            val added = LauncherStore.addToPage(this, targetPageIndex, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_home else R.string.launcher_already_on_home,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_add_to_hotseat) to {
            val added = LauncherStore.addToHotseat(this, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_hotseat else R.string.launcher_already_in_hotseat,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker(entry)
        }
        val isFavorite = LauncherStore.isFavorite(this, entry.component)
        options += getString(
            if (isFavorite) R.string.launcher_action_favorite_remove else R.string.launcher_action_favorite_add
        ) to {
            LauncherStore.setFavorite(this, entry.component, !isFavorite)
            favoriteKeys = LauncherStore.getFavoriteKeys(this)
            updateSuggested()
            setupCategories()
            applyFilters()
        }
        val isHidden = LauncherStore.isAppHidden(this, entry.component)
        options += getString(if (isHidden) R.string.launcher_action_unhide_app else R.string.launcher_action_hide_app) to {
            LauncherStore.setAppHidden(this, entry.component, !isHidden)
            refreshAppsForHidden()
        }
        options += getString(R.string.launcher_action_app_info) to {
            openAppInfo(entry)
        }
        options += getString(R.string.launcher_action_uninstall) to {
            confirmUninstall(entry)
        }
        showOptionsDialog(getString(R.string.launcher_all_apps), options)
    }

    private fun openAppInfo(entry: AppEntry) {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", entry.component.packageName, null)
            )
            startActivity(intent)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun confirmUninstall(entry: AppEntry) {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_uninstall)
            .setMessage(getString(R.string.launcher_uninstall_confirm, entry.label))
            .setPositiveButton(R.string.launcher_action_uninstall) { _, _ ->
                uninstallApp(entry)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun uninstallApp(entry: AppEntry) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:${entry.component.packageName}")
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_uninstall_not_allowed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFolderPicker(entry: AppEntry) {
        val folders = LauncherStore.listFolders(this)
        if (folders.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folders, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = folders.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder)
            .setItems(labels) { _, which ->
                val folderId = folders[which].id
                val added = LauncherStore.addToFolder(this, folderId, entry.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun showOptionsDialog(title: String, options: List<Pair<String, () -> Unit>>) {
        val labels = options.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels) { _, which ->
                options[which].second.invoke()
            }
            .show()
    }

    private fun launchApp(entry: AppEntry) {
        soundFeedback?.playTap()
        LauncherStore.recordLaunch(this, entry.component)
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
