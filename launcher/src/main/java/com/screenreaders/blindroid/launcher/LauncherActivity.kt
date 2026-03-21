package com.screenreaders.blindroid.launcher

import android.Manifest
import android.app.AlarmManager
import android.app.SearchManager
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothProfile
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.BatteryManager
import android.app.ActivityManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import android.speech.RecognizerIntent
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class LauncherActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var searchRow: LinearLayout
    private lateinit var homePager: ViewPager2
    private lateinit var hotseatRow: RecyclerView
    private lateinit var allAppsButton: Button
    private lateinit var widgetsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var voiceButton: Button
    private lateinit var simpleFavoritesLabel: TextView
    private lateinit var simpleFavoritesGrid: RecyclerView
    private lateinit var pageIndicator: LinearLayout
    private lateinit var gestureDetector: GestureDetector
    private var twoFingerActive = false
    private var twoFingerStartX = 0f
    private var twoFingerStartY = 0f
    private var twoFingerLastX = 0f
    private var twoFingerLastY = 0f
    private var twoFingerStartTime = 0L
    private var threeFingerActive = false
    private var threeFingerStartX = 0f
    private var threeFingerStartY = 0f
    private var threeFingerLastX = 0f
    private var threeFingerLastY = 0f
    private var threeFingerStartTime = 0L

    private lateinit var homeAdapter: HomePagerAdapter
    private lateinit var hotseatAdapter: HomeItemAdapter
    private lateinit var simpleFavoritesAdapter: AppAdapter
    private var hotseatTouchHelper: ItemTouchHelper? = null

    private var allApps: List<AppEntry> = emptyList()
    private var pages: List<List<HomeItem>> = emptyList()
    private var hotseat: List<HomeItem> = emptyList()
    private var feedData: FeedData? = null
    private var lastExternalFeedLaunchMs = 0L
    private var flashlightOn = false
    private var flashlightCameraId: String? = null
    private var pendingFlashToggle = false
    private var soundFeedback: LauncherSoundFeedback? = null
    private var weatherFetchInProgress = false
    private var lastAppliedHomeTarget: Int? = null
    private var lastCustomVersion: Long = 0L
    private var lastIconPackVersion: Long = 0L

    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        val query = results.firstOrNull().orEmpty()
        if (query.isNotBlank()) {
            if (!handleVoiceCommand(query)) {
                launchSearch(query)
            }
        }
    }

    private val flashPermissionRequestCode = 9201
    private val calendarPermissionRequestCode = 9202
    private val bluetoothPermissionRequestCode = 9203
    private val weatherPermissionRequestCode = 9204


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        LauncherDiagnosticLog.log(this, "launcher_open")

        searchInput = findViewById(R.id.searchInput)
        searchRow = findViewById(R.id.searchRow)
        homePager = findViewById(R.id.homePager)
        hotseatRow = findViewById(R.id.hotseatRow)
        allAppsButton = findViewById(R.id.allAppsButton)
        widgetsButton = findViewById(R.id.widgetsButton)
        settingsButton = findViewById(R.id.settingsButton)
        voiceButton = findViewById(R.id.voiceButton)
        simpleFavoritesLabel = findViewById(R.id.simpleFavoritesLabel)
        simpleFavoritesGrid = findViewById(R.id.simpleFavoritesGrid)
        pageIndicator = findViewById(R.id.pageIndicator)
        soundFeedback = LauncherSoundFeedback(this)
        lastCustomVersion = LauncherStore.getCustomVersion(this)
        lastIconPackVersion = LauncherStore.getIconPackVersion(this)

        val baseConfig = LauncherPrefs.getUiConfig(this)
        feedData = buildFeedData()

        homeAdapter = HomePagerAdapter(
            pages.map { it.toMutableList() }.toMutableList(),
            baseConfig,
            LauncherPrefs.isFeedEnabled(this),
            feedData,
            LauncherPrefs.getThemeColors(this),
            !LauncherPrefs.isHomeEditLocked(this),
            ::openExternalFeed,
            ::openAlarms,
            ::openCalendar,
            ::requestCalendarPermission,
            ::openWeather,
            ::openBluetoothSettings,
            ::requestBluetoothPermission,
            ::openLocationSettings,
            ::requestLocationPermission,
            ::openNetworkSettings,
            ::openStorageSettings,
            ::openScreenTime,
            ::openUsageAccess,
            ::openDisplaySettings,
            ::openBatterySettings,
            ::openDndSettings,
            ::openSoundSettings,
            ::openQuickSettings,
            ::toggleWifiFromFeed,
            ::toggleBluetoothFromFeed,
            ::toggleDndFromFeed,
            ::openNotificationAccessSettings,
            ::openAllApps,
            ::onHomeItemClick,
            ::onHomeItemLongClick,
            ::onHomeItemMoved
        )
        homePager.adapter = homeAdapter
        homePager.offscreenPageLimit = 2
        applyPageTransformer()
        homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator()
                maybeAutoOpenExternalFeed(position)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                updateWallpaperParallax(position, positionOffset)
                updatePageIndicatorProgress(position, positionOffset)
            }
        })

        hotseatAdapter = HomeItemAdapter(
            hotseat.toMutableList(),
            LauncherPrefs.getDockConfig(this, 0),
            ::onHotseatItemClick,
            ::onHotseatItemLongClick,
            !LauncherPrefs.isHomeEditLocked(this)
        )
        hotseatRow.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        hotseatRow.adapter = hotseatAdapter
        setupHotseatDrag()

        simpleFavoritesAdapter = AppAdapter(
            emptyList(),
            LauncherPrefs.getSimpleFavoritesConfig(this, 0),
            { entry -> launchApp(entry.component) },
            ::handleSimpleFavoriteLongPress
        )
        simpleFavoritesGrid.layoutManager = GridLayoutManager(this, LauncherPrefs.getSimpleFavoritesConfig(this, 0).columns)
        simpleFavoritesGrid.adapter = simpleFavoritesAdapter

        allAppsButton.setOnClickListener {
            soundFeedback?.playAction(LauncherPrefs.ACTION_OPEN_ALL_APPS)
            openAllApps()
        }

        widgetsButton.setOnClickListener {
            soundFeedback?.playAction(LauncherPrefs.ACTION_OPEN_WIDGETS)
            openWidgets()
        }

        settingsButton.setOnClickListener {
            soundFeedback?.playAction(LauncherPrefs.ACTION_OPEN_SETTINGS)
            openSettings()
        }
        voiceButton.setOnClickListener {
            openAssistant()
        }
        findViewById<View>(R.id.launcherRoot).setOnLongClickListener {
            showHomeQuickMenu()
            true
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                launchSearch(searchInput.text?.toString())
                true
            } else {
                false
            }
        }

        setupGestureDetector()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        val currentCustom = LauncherStore.getCustomVersion(this)
        val currentIconPack = LauncherStore.getIconPackVersion(this)
        if (currentCustom != lastCustomVersion || currentIconPack != lastIconPackVersion) {
            lastCustomVersion = currentCustom
            lastIconPackVersion = currentIconPack
            loadApps()
            return
        }
        refreshHome()
        applyUiConfig()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundFeedback?.release()
        soundFeedback = null
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                refreshHome()
                applyUiConfig()
            }
        }.start()
    }

    private fun refreshHome() {
        if (allApps.isEmpty()) return
        LauncherStore.syncModuleShortcuts(this, LauncherStore.isModuleShortcutsEnabled(this))
        val allPages = LauncherStore.loadPages(this, allApps)
        pages = if (LauncherPrefs.isSuperSimpleEnabled(this)) {
            listOf(allPages.firstOrNull() ?: emptyList())
        } else {
            allPages
        }
        val baseHotseat = LauncherStore.loadHotseat(this, allApps)
        hotseat = if (LauncherPrefs.isSmartHotseatEnabled(this)) {
            val maxSlots = LauncherPrefs.getColumns(this).coerceAtLeast(3)
            val used = baseHotseat.filterIsInstance<HomeItem.App>()
                .map { it.component.flattenToString() }
                .toMutableSet()
            val suggestions = LauncherStore.getSuggestedApps(this, allApps, maxSlots * 2)
                .filter { used.add(it.component.flattenToString()) }
                .map { HomeItem.app(it) }
            val merged = baseHotseat + suggestions
            if (merged.size > maxSlots) merged.take(maxSlots) else merged
        } else {
            baseHotseat
        }
        homeAdapter.submitPages(pages)
        homePager.offscreenPageLimit = kotlin.math.min(2, homeAdapter.itemCount.coerceAtLeast(1))
        hotseatAdapter.submit(hotseat)
        updateSimpleFavorites()
        updatePageIndicator()
        applyDefaultHomePage()
    }

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        homePager.post {
            val rows = baseConfig.rows
            val itemHeight = if (rows > 0) homePager.height / rows else 0
            val pageConfig = baseConfig.copy(itemHeightPx = itemHeight)
            homeAdapter.updateConfig(pageConfig)
            hotseatAdapter.updateConfig(LauncherPrefs.getDockConfig(this, 0))
            val editingEnabled = !LauncherPrefs.isHomeEditLocked(this)
            homeAdapter.setEditingEnabled(editingEnabled)
            hotseatAdapter.setEditingEnabled(editingEnabled)
            applyHotseatEditingLock()
        }
        val simpleConfig = LauncherPrefs.getSimpleFavoritesConfig(this, 0)
        (simpleFavoritesGrid.layoutManager as? GridLayoutManager)?.spanCount = simpleConfig.columns
        simpleFavoritesAdapter.updateConfig(simpleConfig)
        applyPageTransformer()
        val simple = LauncherPrefs.isSuperSimpleEnabled(this)
        val searchEnabled = LauncherPrefs.isSearchBarEnabled(this)
        searchRow.visibility = if (simple || !searchEnabled) View.GONE else View.VISIBLE
        widgetsButton.visibility = if (simple) View.GONE else View.VISIBLE
        settingsButton.visibility = if (simple) View.GONE else View.VISIBLE
        allAppsButton.visibility = View.VISIBLE
        voiceButton.visibility = if (simple) View.GONE else View.VISIBLE
        hotseatRow.visibility = if (LauncherPrefs.isDockVisible(this)) View.VISIBLE else View.GONE
        simpleFavoritesLabel.visibility = if (simple) View.VISIBLE else View.GONE
        simpleFavoritesGrid.visibility = if (simple) View.VISIBLE else View.GONE

        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<View>(R.id.launcherRoot).setBackgroundColor(colors.background)
        ThemeUtils.tintEditText(searchInput, colors)
        ThemeUtils.tintButton(allAppsButton, colors, false)
        ThemeUtils.tintButton(widgetsButton, colors, true)
        ThemeUtils.tintButton(settingsButton, colors, false)
        ThemeUtils.tintButton(voiceButton, colors, true)
        simpleFavoritesLabel.setTextColor(colors.text)
        ThemeUtils.applySurface(searchRow, colors)
        ThemeUtils.applySurface(pageIndicator, colors)
        ThemeUtils.applySurface(hotseatRow, colors)
        ThemeUtils.applySurface(simpleFavoritesGrid, colors)
        (allAppsButton.parent as? View)?.let { ThemeUtils.applySurface(it, colors) }

        feedData = buildFeedData()
        val feedEnabled = LauncherPrefs.isFeedEnabled(this) && !simple
        homeAdapter.updateFeed(feedEnabled, feedData, colors)
        homePager.isUserInputEnabled = homeAdapter.itemCount > 1
        updatePageIndicator()
    }

    private fun applyPageTransformer() {
        val intensity = (LauncherPrefs.getPageAnimationIntensity(this) / 100f).coerceIn(0.3f, 1.5f)
        when (LauncherPrefs.getPageAnimation(this)) {
            LauncherPrefs.PAGE_ANIM_CAROUSEL -> homePager.setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                val scale = 0.85f + (1f - absPos) * 0.15f
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.5f + (1f - absPos) * 0.5f
                page.translationX = -position * page.width * 0.2f * intensity
                page.rotationY = position * -30f * intensity
            }
            LauncherPrefs.PAGE_ANIM_DEPTH -> homePager.setPageTransformer { page, position ->
                if (position <= 0f) {
                    page.alpha = 1f
                    page.translationX = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f
                } else if (position <= 1f) {
                    page.alpha = 1f - position
                    page.translationX = page.width * -position * intensity
                    val scale = 0.75f + (1f - 0.75f) * (1f - position)
                    page.scaleX = scale
                    page.scaleY = scale
                } else {
                    page.alpha = 0f
                }
                page.rotationY = 0f
            }
            LauncherPrefs.PAGE_ANIM_STACK -> homePager.setPageTransformer { page, position ->
                if (position >= 0f) {
                    page.translationX = -position * page.width * intensity
                    page.scaleX = 1f - 0.08f * position * intensity
                    page.scaleY = 1f - 0.08f * position * intensity
                    page.alpha = 1f - 0.25f * position
                } else {
                    page.translationX = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f
                    page.alpha = 1f
                }
                page.rotationY = 0f
            }
            LauncherPrefs.PAGE_ANIM_ZOOM -> homePager.setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                val scale = 0.85f + (1f - absPos) * 0.15f
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.4f + (1f - absPos) * 0.6f
                page.translationX = -position * page.width * 0.1f * intensity
                page.rotationY = 0f
            }
            LauncherPrefs.PAGE_ANIM_FLIP -> homePager.setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                page.cameraDistance = page.width * 12f
                page.translationX = -position * page.width * intensity
                page.rotationY = -180f * position * intensity
                page.alpha = (1f - absPos * 0.6f).coerceIn(0f, 1f)
                page.scaleX = 0.9f + (1f - absPos) * 0.1f
                page.scaleY = page.scaleX
            }
            LauncherPrefs.PAGE_ANIM_CUBE -> homePager.setPageTransformer { page, position ->
                page.cameraDistance = page.width * 12f
                page.translationX = -position * page.width * intensity
                if (position < 0f) {
                    page.pivotX = page.width.toFloat()
                } else {
                    page.pivotX = 0f
                }
                page.pivotY = page.height * 0.5f
                page.rotationY = 90f * position * intensity
                page.alpha = (1f - kotlin.math.abs(position) * 0.3f).coerceIn(0f, 1f)
                page.scaleX = 1f
                page.scaleY = 1f
            }
            LauncherPrefs.PAGE_ANIM_PARALLAX -> homePager.setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                page.translationX = -position * page.width * 0.35f * intensity
                page.alpha = 0.5f + (1f - absPos) * 0.5f
                page.scaleX = 0.95f + (1f - absPos) * 0.05f
                page.scaleY = page.scaleX
                page.rotationY = 0f
            }
            else -> homePager.setPageTransformer { page, position ->
                val absPos = kotlin.math.abs(position)
                val scale = 0.92f + (1f - absPos) * 0.08f
                page.scaleX = scale
                page.scaleY = scale
                page.alpha = 0.6f + (1f - absPos) * 0.4f
                page.translationX = -position * page.width * 0.08f * intensity
                page.rotationY = 0f
            }
        }
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (LauncherPrefs.isDoubleTapLockEnabled(this@LauncherActivity)) {
                    attemptLock()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                openSettings()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                val absX = kotlin.math.abs(diffX)
                val absY = kotlin.math.abs(diffY)
                if (absY > absX && absY > 120 && kotlin.math.abs(velocityY) > 120) {
                    if (diffY < 0) {
                        openAllApps()
                    } else {
                        focusSearch()
                    }
                    return true
                }
                return false
            }
        })
        val listener = View.OnTouchListener { _, event ->
            handleTwoFingerGesture(event)
            handleThreeFingerGesture(event)
            gestureDetector.onTouchEvent(event)
            false
        }
        findViewById<View>(R.id.launcherRoot).setOnTouchListener(listener)
        homePager.setOnTouchListener(listener)
    }

    private fun openAllApps() {
        LauncherDiagnosticLog.log(this, "launcher_all_apps")
        val intent = Intent(this, AllAppsActivity::class.java)
        intent.putExtra(AllAppsActivity.EXTRA_PAGE_INDEX, currentHomePageIndex())
        startActivity(intent)
    }

    private fun openWidgets() {
        LauncherDiagnosticLog.log(this, "launcher_widgets")
        val intent = Intent(this, AllAppsActivity::class.java)
        intent.putExtra(AllAppsActivity.EXTRA_PAGE_INDEX, currentHomePageIndex())
        intent.putExtra(AllAppsActivity.EXTRA_TAB, AllAppsActivity.TAB_WIDGETS)
        startActivity(intent)
    }

    private fun openSettings() {
        LauncherDiagnosticLog.log(this, "launcher_settings")
        startActivity(Intent(this, LauncherSettingsActivity::class.java))
    }

    private fun openQuickSettings() {
        val intent = Intent("android.settings.QUICK_SETTINGS")
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    private fun openFeed() {
        val simple = LauncherPrefs.isSuperSimpleEnabled(this)
        val enabled = LauncherPrefs.isFeedEnabled(this) && !simple
        if (!enabled) {
            Toast.makeText(this, R.string.launcher_feed_disabled, Toast.LENGTH_SHORT).show()
            return
        }
        if (LauncherPrefs.getFeedMode(this) == LauncherPrefs.FEED_MODE_GOOGLE) {
            openExternalFeed(showError = true)
        } else {
            homePager.currentItem = 0
        }
    }

    private fun focusSearch() {
        searchInput.requestFocus()
        searchInput.setSelection(searchInput.text?.length ?: 0)
    }

    private fun startVoiceSearch() {
        val googleAvailable = isGoogleAppAvailable()
        val preferGoogle = LauncherPrefs.isGoogleVoiceEnabled(this) && googleAvailable
        val intent = if (preferGoogle) {
            Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                setPackage("com.google.android.googlequicksearchbox")
            }
        } else {
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        }
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.launcher_settings_voice_search))
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: Exception) {
            if (preferGoogle) {
                val fallback = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.launcher_settings_voice_search))
                }
                try {
                    voiceSearchLauncher.launch(fallback)
                    return
                } catch (_: Exception) {
                    // fall through
                }
            }
            Toast.makeText(this, R.string.launcher_search_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleVoiceCommand(raw: String): Boolean {
        val text = normalizeCommand(raw)
        if (text.isBlank()) return false
        fun has(vararg keys: String): Boolean = keys.any { text.contains(it) }

        return when {
            has("telefon", "zadzwon", "zadzwonic", "polaczenie", "polaczenia", "call") -> {
                openDialer(); true
            }
            has("wiadomosc", "wiadomosci", "sms", "message") -> {
                openMessages(); true
            }
            has("aplikacje", "aplikacja", "apps", "app") -> {
                openAllApps(); true
            }
            has("ustawienia", "settings") -> {
                openSettings(); true
            }
            has("widzet", "widget") -> {
                openWidgets(); true
            }
            has("feed", "discover", "wiadomosci google", "google now") -> {
                openFeed(); true
            }
            has("gemini", "asystent") -> {
                openGemini(); true
            }
            has("latarka", "flash") -> {
                toggleFlashlight(); true
            }
            has("pogoda", "weather") -> {
                openWeather(); true
            }
            has("nastepna", "dalej", "next") -> {
                goToNextPage(); true
            }
            has("poprzednia", "wstecz", "cofnij", "back") -> {
                goToPrevPage(); true
            }
            has("dok", "dock") -> {
                toggleDockVisibility(); true
            }
            has("super prosty", "prosty", "tryb prosty", "simple mode") -> {
                toggleSuperSimple(); true
            }
            has("zablokuj", "blokada", "lock") -> {
                attemptLock(); true
            }
            has("glosniej", "glośniej", "volume up") -> {
                adjustVolume(AudioManager.ADJUST_RAISE); true
            }
            has("ciszej", "volume down") -> {
                adjustVolume(AudioManager.ADJUST_LOWER); true
            }
            has("wycisz", "mute") -> {
                adjustVolume(AudioManager.ADJUST_MUTE); true
            }
            else -> false
        }
    }

    private fun adjustVolume(direction: Int) {
        val audio = getSystemService(AudioManager::class.java)
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun normalizeCommand(text: String): String {
        val normalized = java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun handleSimpleFavoriteLongPress(entry: AppEntry) {
        val item = HomeItem.App(entry.label, entry.component, entry.icon)
        showHotseatAppOptions(item)
    }

    private fun updateSimpleFavorites() {
        if (!LauncherPrefs.isSuperSimpleEnabled(this)) return
        val slots = LauncherPrefs.getSimpleFavoritesColumns(this) * LauncherPrefs.getSimpleFavoritesRows(this)
        val favorites = hotseat.filterIsInstance<HomeItem.App>()
            .take(slots)
            .map { AppEntry(it.label, it.component, it.icon) }
        val list = if (favorites.isNotEmpty()) {
            favorites
        } else {
            LauncherStore.getSuggestedApps(this, allApps, slots)
        }
        simpleFavoritesAdapter.submit(list)
    }

    private fun toggleDockVisibility() {
        val visible = !LauncherPrefs.isDockVisible(this)
        LauncherPrefs.setDockVisible(this, visible)
        hotseatRow.visibility = if (visible) View.VISIBLE else View.GONE
        Toast.makeText(
            this,
            if (visible) R.string.launcher_dock_shown else R.string.launcher_dock_hidden,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleFeedEnabled() {
        val currentHome = currentHomePageIndex()
        val enabled = !LauncherPrefs.isFeedEnabled(this)
        LauncherPrefs.setFeedEnabled(this, enabled)
        applyUiConfig()
        val feedOffset = if (enabled && !LauncherPrefs.isSuperSimpleEnabled(this)) 1 else 0
        val target = (currentHome + feedOffset).coerceIn(0, homeAdapter.itemCount - 1)
        homePager.setCurrentItem(target, false)
        Toast.makeText(
            this,
            if (enabled) R.string.launcher_feed_enabled else R.string.launcher_feed_disabled,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun toggleSuperSimple() {
        val enabled = !LauncherPrefs.isSuperSimpleEnabled(this)
        LauncherPrefs.setSuperSimpleEnabled(this, enabled)
        if (enabled) {
            LauncherPrefs.setDockVisible(this, false)
        }
        refreshHome()
        applyUiConfig()
        Toast.makeText(
            this,
            if (enabled) R.string.launcher_super_simple_on else R.string.launcher_super_simple_off,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildFeedData(): FeedData {
        val now = java.time.LocalDateTime.now()
        val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern(
            "EEEE, d MMMM",
            java.util.Locale.forLanguageTag("pl-PL")
        )
        val time = now.format(timeFormatter)
        val date = now.format(dateFormatter)
        val batteryLevel = getBatteryLevel()
        val batteryText = getString(R.string.launcher_feed_battery) + ": ${batteryLevel}%"
        val notifications = getRecentNotifications()
        val feedMode = LauncherPrefs.getFeedMode(this)
        val externalAvailable = isGoogleAppAvailable()
        val wantsGoogle = feedMode == LauncherPrefs.FEED_MODE_GOOGLE
        val externalMode = wantsGoogle && externalAvailable
        val embeddedMode = feedMode == LauncherPrefs.FEED_MODE_EMBEDDED || (wantsGoogle && !externalAvailable)
        val quickActionsEnabled = LauncherPrefs.isFeedQuickActionsEnabled(this)
        val wifiEnabled = isWifiEnabled()
        val bluetoothEnabled = isBluetoothEnabled()
        val dndEnabled = isDndEnabled()
        val dndPolicyGranted = isDndPolicyGranted()
        val notificationsAccessGranted = NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
        val showAlarm = LauncherPrefs.isNowAlarmEnabled(this)
        val showCalendar = LauncherPrefs.isNowCalendarEnabled(this)
        val showWeather = LauncherPrefs.isNowWeatherEnabled(this)
        val showBattery = LauncherPrefs.isNowBatteryEnabled(this)
        val showReminders = LauncherPrefs.isNowRemindersEnabled(this)
        val showHeadphones = LauncherPrefs.isNowHeadphonesEnabled(this)
        val showLocation = LauncherPrefs.isNowLocationEnabled(this)
        val showNetwork = LauncherPrefs.isNowNetworkEnabled(this)
        val showStorage = LauncherPrefs.isNowStorageEnabled(this)
        val showScreenTime = LauncherPrefs.isNowScreenTimeEnabled(this)
        val showTopApps = LauncherPrefs.isNowTopAppsEnabled(this)
        val showAirplane = LauncherPrefs.isNowAirplaneEnabled(this)
        val showRam = LauncherPrefs.isNowRamEnabled(this)
        val showDevice = LauncherPrefs.isNowDeviceEnabled(this)
        val showRotation = LauncherPrefs.isNowRotationEnabled(this)
        val showNfc = LauncherPrefs.isNowNfcEnabled(this)
        val showDnd = LauncherPrefs.isNowDndEnabled(this)
        val showRinger = LauncherPrefs.isNowRingerEnabled(this)
        val showBluetooth = LauncherPrefs.isNowBluetoothEnabled(this)
        val showBrightness = LauncherPrefs.isNowBrightnessEnabled(this)
        val showVolume = LauncherPrefs.isNowVolumeEnabled(this)
        val showPower = LauncherPrefs.isNowPowerEnabled(this)
        val calendarPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val bluetoothPermissionGranted = isBluetoothPermissionGranted()
        val alarmText = if (showAlarm) getNextAlarmText() else null
        val calendarText = if (showCalendar && calendarPermissionGranted) getNextCalendarEventText() else null
        val weatherText = if (showWeather) getWeatherText() else null
        val batteryDetailsText = if (showBattery) getBatteryDetailsText() else null
        val reminderText = if (showReminders && calendarPermissionGranted) getNextReminderText() else null
        val headphonesText = if (showHeadphones) getHeadphonesText() else null
        val locationPermissionGranted = hasLocationPermission()
        val locationText = if (showLocation) getLocationText(locationPermissionGranted) else null
        val networkText = if (showNetwork) getNetworkText() else null
        val storageText = if (showStorage) getStorageText() else null
        val usagePermissionGranted = LauncherStore.hasUsageStatsPermission(this)
        val screenTimeText = if (showScreenTime) getScreenTimeText(usagePermissionGranted) else null
        val bluetoothText = if (showBluetooth) getBluetoothText(bluetoothPermissionGranted) else null
        val brightnessText = if (showBrightness) getBrightnessText() else null
        val volumeText = if (showVolume) getVolumeText() else null
        val powerText = if (showPower) getPowerText() else null
        val topApps = if (showTopApps) LauncherStore.getSuggestedApps(this, allApps, 4).map { it.label } else emptyList()
        val airplaneText = if (showAirplane) getAirplaneText() else null
        val ramText = if (showRam) getRamText() else null
        val deviceText = if (showDevice) getDeviceText() else null
        val rotationText = if (showRotation) getRotationText() else null
        val nfcText = if (showNfc) getNfcText() else null
        val dndText = if (showDnd) getDndText() else null
        val ringerText = if (showRinger) getRingerText() else null
        if (showWeather) {
            maybeRefreshWeather()
        }
        val atGlanceText = buildAtGlance(alarmText, calendarText, reminderText, weatherText)
        return FeedData(
            time = time,
            date = date,
            battery = batteryText,
            notifications = notifications,
            externalMode = externalMode,
            embeddedMode = embeddedMode,
            externalAvailable = externalAvailable,
            embeddedUrl = if (embeddedMode) "https://www.google.com/discover?hl=pl&gl=PL" else null,
            quickActionsEnabled = quickActionsEnabled,
            wifiEnabled = wifiEnabled,
            bluetoothEnabled = bluetoothEnabled,
            dndEnabled = dndEnabled,
            dndPolicyGranted = dndPolicyGranted,
            notificationsAccessGranted = notificationsAccessGranted,
            atGlanceText = atGlanceText,
            showAlarm = showAlarm,
            alarmText = alarmText,
            showCalendar = showCalendar,
            calendarText = calendarText,
            calendarPermissionGranted = calendarPermissionGranted,
            showWeather = showWeather,
            weatherText = weatherText,
            showBattery = showBattery,
            batteryDetailsText = batteryDetailsText,
            showReminders = showReminders,
            reminderText = reminderText,
            showHeadphones = showHeadphones,
            headphonesText = headphonesText,
            showLocation = showLocation,
            locationText = locationText,
            locationPermissionGranted = locationPermissionGranted,
            showNetwork = showNetwork,
            networkText = networkText,
            showStorage = showStorage,
            storageText = storageText,
            showScreenTime = showScreenTime,
            screenTimeText = screenTimeText,
            usagePermissionGranted = usagePermissionGranted,
            showBluetooth = showBluetooth,
            bluetoothText = bluetoothText,
            bluetoothPermissionGranted = bluetoothPermissionGranted,
            showBrightness = showBrightness,
            brightnessText = brightnessText,
            showVolume = showVolume,
            volumeText = volumeText,
            showPower = showPower,
            powerText = powerText,
            showTopApps = showTopApps,
            topApps = topApps,
            showAirplane = showAirplane,
            airplaneText = airplaneText,
            showRam = showRam,
            ramText = ramText,
            showDevice = showDevice,
            deviceText = deviceText,
            showRotation = showRotation,
            rotationText = rotationText,
            showNfc = showNfc,
            nfcText = nfcText,
            showDnd = showDnd,
            dndText = dndText,
            showRinger = showRinger,
            ringerText = ringerText
        )
    }

    private fun getDeviceText(): String {
        val maker = android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.titlecase() }
        val model = android.os.Build.MODEL.orEmpty()
        val name = listOf(maker, model).filter { it.isNotBlank() }.joinToString(" ")
        val release = android.os.Build.VERSION.RELEASE ?: "?"
        val sdk = android.os.Build.VERSION.SDK_INT
        val base = getString(R.string.launcher_feed_device_text, name.ifBlank { "Android" }, release, sdk)
        val patch = android.os.Build.VERSION.SECURITY_PATCH
        return if (!patch.isNullOrBlank()) {
            "$base • " + getString(R.string.launcher_feed_device_patch, patch)
        } else {
            base
        }
    }

    private fun getRotationText(): String? {
        return try {
            val enabled = android.provider.Settings.System.getInt(
                contentResolver,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
            if (enabled) {
                getString(R.string.launcher_feed_rotation_auto)
            } else {
                getString(R.string.launcher_feed_rotation_locked)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getNfcText(): String? {
        val adapter = android.nfc.NfcAdapter.getDefaultAdapter(this) ?: return getString(R.string.launcher_feed_nfc_missing)
        return if (adapter.isEnabled) {
            getString(R.string.launcher_feed_nfc_on)
        } else {
            getString(R.string.launcher_feed_nfc_off)
        }
    }

    private fun getBatteryLevel(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    private fun getBatteryDetailsText(): String? {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val tempTenth = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val temp = if (tempTenth > 0) tempTenth / 10f else null

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> getString(R.string.launcher_battery_charging)
            BatteryManager.BATTERY_STATUS_FULL -> getString(R.string.launcher_battery_full)
            BatteryManager.BATTERY_STATUS_DISCHARGING -> getString(R.string.launcher_battery_discharging)
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> getString(R.string.launcher_battery_not_charging)
            else -> getString(R.string.launcher_battery_unknown)
        }
        val plugText = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> getString(R.string.launcher_battery_plug_ac)
            BatteryManager.BATTERY_PLUGGED_USB -> getString(R.string.launcher_battery_plug_usb)
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> getString(R.string.launcher_battery_plug_wireless)
            else -> null
        }
        val parts = mutableListOf<String>()
        parts.add(statusText)
        if (plugText != null) parts.add(plugText)
        if (percent >= 0) parts.add("$percent%")
        if (temp != null) parts.add(String.format(java.util.Locale.US, "%.1f°C", temp))
        return parts.joinToString(" • ").ifBlank { null }
    }

    private fun getRecentNotifications(): List<String> {
        val prefs = getSharedPreferences("blindroid_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString("recent_notifications", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }.take(5)
    }

    private fun isWifiEnabled(): Boolean {
        return try {
            val wifi = applicationContext.getSystemService(WifiManager::class.java) ?: return false
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled
        } catch (_: Exception) {
            false
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        return try {
            val manager = getSystemService(BluetoothManager::class.java)
            manager?.adapter
        } catch (_: Exception) {
            null
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            getBluetoothAdapter()?.isEnabled == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isDndEnabled(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun isDndPolicyGranted(): Boolean {
        val nm = getSystemService(NotificationManager::class.java)
        return nm.isNotificationPolicyAccessGranted
    }

    private fun toggleWifiFromFeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openNetworkSettings()
            return
        }
        try {
            val wifi = applicationContext.getSystemService(WifiManager::class.java) ?: return
            @Suppress("DEPRECATION")
            val target = !wifi.isWifiEnabled
            @Suppress("DEPRECATION")
            wifi.isWifiEnabled = target
            refreshHome()
        } catch (_: Exception) {
            openNetworkSettings()
        }
    }

    private fun toggleBluetoothFromFeed() {
        if (!isBluetoothPermissionGranted()) {
            requestBluetoothPermission()
            return
        }
        val adapter = getBluetoothAdapter()
        if (adapter == null) {
            openBluetoothSettings()
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                openBluetoothSettings()
                return
            }
            val enabled = adapter.isEnabled
            if (enabled) {
                @Suppress("DEPRECATION")
                adapter.disable()
            } else {
                @Suppress("DEPRECATION")
                adapter.enable()
            }
            refreshHome()
        } catch (_: Exception) {
            openBluetoothSettings()
        }
    }

    private fun toggleDndFromFeed() {
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) {
            openDndSettings()
            return
        }
        val enabled = nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        nm.setInterruptionFilter(
            if (enabled) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_NONE
        )
        refreshHome()
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun getNextAlarmText(): String? {
        val alarmManager = getSystemService(AlarmManager::class.java)
        val next = alarmManager.nextAlarmClock ?: return null
        return formatDateTime(next.triggerTime, allowTodayLabel = true)
    }

    private fun getWeatherText(): String? {
        val prefs = getSharedPreferences("blindroid_launcher_weather", Context.MODE_PRIVATE)
        val text = prefs.getString("weather_text", null)
        val ts = prefs.getLong("weather_ts", 0L)
        val ageMs = System.currentTimeMillis() - ts
        if (!text.isNullOrBlank() && ageMs in 0..(60 * 60 * 1000L)) {
            return text
        }
        if (!hasLocationPermission()) {
            return getString(R.string.launcher_feed_weather_permission)
        }
        if (!isLocationEnabled()) {
            return getString(R.string.launcher_feed_weather_location_off)
        }
        if (!isNetworkAvailable()) {
            return getString(R.string.launcher_feed_weather_wait)
        }
        return getString(R.string.launcher_feed_weather_wait)
    }

    private fun maybeRefreshWeather(force: Boolean = false) {
        if (weatherFetchInProgress) return
        if (!hasLocationPermission()) return
        if (!isLocationEnabled()) return
        if (!isNetworkAvailable()) return
        val prefs = getSharedPreferences("blindroid_launcher_weather", Context.MODE_PRIVATE)
        val ts = prefs.getLong("weather_ts", 0L)
        val ageMs = System.currentTimeMillis() - ts
        if (!force && ageMs in 0..(60 * 60 * 1000L)) return
        weatherFetchInProgress = true
        Thread {
            val location = getLastKnownLocationSafe()
            if (location == null) {
                weatherFetchInProgress = false
                return@Thread
            }
            val text = fetchOpenMeteoWeather(location)
            if (!text.isNullOrBlank()) {
                prefs.edit()
                    .putString("weather_text", text)
                    .putLong("weather_ts", System.currentTimeMillis())
                    .apply()
            }
            weatherFetchInProgress = false
            runOnUiThread {
                refreshHome()
                applyUiConfig()
            }
        }.start()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val manager = getSystemService(LocationManager::class.java)
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    @Suppress("MissingPermission")
    private fun getLastKnownLocationSafe(): Location? {
        val manager = getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        var best: Location? = null
        providers.forEach { provider ->
            try {
                val loc = manager.getLastKnownLocation(provider) ?: return@forEach
                if (best == null || (loc.time > (best?.time ?: 0L))) {
                    best = loc
                }
            } catch (_: Exception) {
                // ignore
            }
        }
        return best
    }

    private fun fetchOpenMeteoWeather(location: Location): String? {
        return try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${location.latitude}" +
                "&longitude=${location.longitude}" +
                "&current=temperature_2m,weather_code,wind_speed_10m" +
                "&timezone=auto"
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.use { input ->
                val body = input.bufferedReader().readText()
                val json = JSONObject(body)
                val current = json.optJSONObject("current") ?: return null
                val temp = current.optDouble("temperature_2m", Double.NaN)
                val wind = current.optDouble("wind_speed_10m", Double.NaN)
                val code = current.optInt("weather_code", -1)
                val desc = weatherCodeToText(code)
                val tempText = if (!temp.isNaN()) "${temp.toInt()}°C" else null
                val windText = if (!wind.isNaN()) "${wind.toInt()} km/h" else null
                listOfNotNull(tempText, desc, windText).joinToString(" • ").ifBlank { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun weatherCodeToText(code: Int): String? {
        return when (code) {
            0 -> getString(R.string.launcher_weather_clear)
            1, 2 -> getString(R.string.launcher_weather_partly_cloudy)
            3 -> getString(R.string.launcher_weather_cloudy)
            45, 48 -> getString(R.string.launcher_weather_fog)
            51, 53, 55 -> getString(R.string.launcher_weather_drizzle)
            61, 63, 65 -> getString(R.string.launcher_weather_rain)
            71, 73, 75 -> getString(R.string.launcher_weather_snow)
            80, 81, 82 -> getString(R.string.launcher_weather_showers)
            95, 96, 99 -> getString(R.string.launcher_weather_thunder)
            else -> null
        }
    }

    private fun buildAtGlance(
        alarmText: String?,
        calendarText: String?,
        reminderText: String?,
        weatherText: String?
    ): String? {
        val parts = mutableListOf<String>()
        if (!alarmText.isNullOrBlank()) {
            parts.add(getString(R.string.launcher_at_glance_alarm, alarmText))
        }
        if (!calendarText.isNullOrBlank()) {
            parts.add(getString(R.string.launcher_at_glance_event, calendarText))
        } else if (!reminderText.isNullOrBlank()) {
            parts.add(getString(R.string.launcher_at_glance_reminder, reminderText))
        }
        if (!weatherText.isNullOrBlank()) {
            parts.add(getString(R.string.launcher_at_glance_weather, weatherText))
        }
        return parts.joinToString(" • ").ifBlank { null }
    }

    private fun getNextCalendarEventText(): String? {
        val now = System.currentTimeMillis()
        val weekAhead = now + 7L * 24L * 60L * 60L * 1000L
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, now)
        ContentUris.appendId(uriBuilder, weekAhead)
        val cursor = contentResolver.query(
            uriBuilder.build(),
            projection,
            "${CalendarContract.Instances.BEGIN} >= ?",
            arrayOf(now.toString()),
            "${CalendarContract.Instances.BEGIN} ASC"
        ) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val title = it.getString(0)?.trim().orEmpty()
            val begin = it.getLong(1)
            val allDay = it.getInt(2) == 1
            val whenText = if (allDay) {
                formatDateOnly(begin, allowTodayLabel = true)
            } else {
                formatDateTime(begin, allowTodayLabel = true)
            }
            return if (title.isNotBlank()) {
                "$title — $whenText"
            } else {
                whenText
            }
        }
    }

    private fun getNextReminderText(): String? {
        val now = System.currentTimeMillis()
        val weekAhead = now + 7L * 24L * 60L * 60L * 1000L
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.HAS_ALARM
        )
        val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(uriBuilder, now)
        ContentUris.appendId(uriBuilder, weekAhead)
        val cursor = contentResolver.query(
            uriBuilder.build(),
            projection,
            "${CalendarContract.Instances.BEGIN} >= ?",
            arrayOf(now.toString()),
            "${CalendarContract.Instances.BEGIN} ASC"
        ) ?: return null
        cursor.use {
            while (it.moveToNext()) {
                val hasAlarm = it.getInt(3) == 1
                if (!hasAlarm) continue
                val title = it.getString(0)?.trim().orEmpty()
                val begin = it.getLong(1)
                val allDay = it.getInt(2) == 1
                val whenText = if (allDay) {
                    formatDateOnly(begin, allowTodayLabel = true)
                } else {
                    formatDateTime(begin, allowTodayLabel = true)
                }
                return if (title.isNotBlank()) {
                    "$title — $whenText"
                } else {
                    whenText
                }
            }
        }
        return null
    }

    private fun getHeadphonesText(): String? {
        val audioManager = getSystemService(AudioManager::class.java)
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val labels = mutableSetOf<String>()
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> labels.add("Przewodowe")
                AudioDeviceInfo.TYPE_USB_HEADSET -> labels.add("USB")
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> labels.add("Bluetooth")
            }
        }
        return if (labels.isEmpty()) null else labels.joinToString(", ")
    }

    private fun getLocationText(permissionGranted: Boolean): String? {
        if (!permissionGranted) {
            return getString(R.string.launcher_feed_location_permission)
        }
        if (!isLocationEnabled()) {
            return getString(R.string.launcher_feed_location_off)
        }
        val location = getLastKnownLocationSafe() ?: return getString(R.string.launcher_feed_location_unknown)
        val lat = String.format(java.util.Locale.US, "%.5f", location.latitude)
        val lon = String.format(java.util.Locale.US, "%.5f", location.longitude)
        val accuracy = if (location.hasAccuracy()) " ±${location.accuracy.toInt()}m" else ""
        val age = formatRelativeTime(System.currentTimeMillis() - location.time)
        return getString(R.string.launcher_feed_location_text, lat, lon, accuracy, age)
    }

    private fun getNetworkText(): String? {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val suffix = if (hasInternet) "" else " (bez internetu)"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val ssid = try {
                    val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (caps.transportInfo as? android.net.wifi.WifiInfo)?.ssid
                    } else {
                        @Suppress("DEPRECATION")
                        applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
                            .connectionInfo?.ssid
                    }
                    val cleaned = raw?.trim('"')
                    cleaned?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
                } catch (_: Exception) {
                    null
                }
                if (ssid != null) "Wi‑Fi: $ssid$suffix" else "Wi‑Fi$suffix"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val carrier = try {
                    val tm = getSystemService(android.telephony.TelephonyManager::class.java)
                    tm.networkOperatorName?.takeIf { it.isNotBlank() }
                } catch (_: Exception) {
                    null
                }
                if (carrier != null) "Komórkowa: $carrier$suffix" else "Komórkowa$suffix"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet$suffix"
            else -> "Połączenie$suffix"
        }
    }

    private fun getStorageText(): String? {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val available = stat.availableBytes
            val total = stat.totalBytes
            val availableGb = bytesToHuman(available)
            val totalGb = bytesToHuman(total)
            getString(R.string.launcher_feed_storage_text, availableGb, totalGb)
        } catch (_: Exception) {
            null
        }
    }

    private fun getScreenTimeText(permissionGranted: Boolean): String? {
        if (!permissionGranted) {
            return getString(R.string.launcher_feed_screen_time_permission)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return getString(R.string.launcher_feed_screen_time_unavailable)
        }
        return try {
            val manager = getSystemService(android.app.usage.UsageStatsManager::class.java)
            val end = System.currentTimeMillis()
            val start = end - 24L * 60L * 60L * 1000L
            val stats = manager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                start,
                end
            )
            if (stats.isNullOrEmpty()) {
                getString(R.string.launcher_feed_screen_time_none)
            } else {
                val totalMs = stats.sumOf { it.totalTimeInForeground }
                val top = stats.maxByOrNull { it.totalTimeInForeground }
                val totalText = formatDuration(totalMs)
                val topLabel = top?.packageName?.let { resolveAppLabel(it) }
                if (!topLabel.isNullOrBlank()) {
                    getString(R.string.launcher_feed_screen_time_top, totalText, topLabel)
                } else {
                    getString(R.string.launcher_feed_screen_time_text, totalText)
                }
            }
        } catch (_: Exception) {
            getString(R.string.launcher_feed_screen_time_unavailable)
        }
    }

    private fun resolveAppLabel(packageName: String): String? {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info)?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val totalMinutes = (durationMs / 60000L).toInt().coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    private fun formatRelativeTime(ageMs: Long): String {
        val minutes = (ageMs / 60000L).toInt().coerceAtLeast(0)
        return when {
            minutes < 1 -> getString(R.string.launcher_time_just_now)
            minutes < 60 -> getString(R.string.launcher_time_minutes, minutes)
            else -> {
                val hours = minutes / 60
                getString(R.string.launcher_time_hours, hours)
            }
        }
    }

    private fun getBluetoothText(permissionGranted: Boolean): String? {
        if (!permissionGranted) {
            return getString(R.string.launcher_feed_bluetooth_permission)
        }
        return try {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter = manager.adapter ?: return getString(R.string.launcher_feed_bluetooth_off)
            if (!adapter.isEnabled) return getString(R.string.launcher_feed_bluetooth_off)
            val devices = mutableListOf<android.bluetooth.BluetoothDevice>()
            devices.addAll(manager.getConnectedDevices(BluetoothProfile.HEADSET))
            devices.addAll(manager.getConnectedDevices(BluetoothProfile.A2DP))
            devices.addAll(manager.getConnectedDevices(BluetoothProfile.GATT))
            val names = devices.mapNotNull { device ->
                val label = device.name?.takeIf { name -> name.isNotBlank() } ?: device.address
                val battery = try {
                    val method = device.javaClass.methods.firstOrNull { it.name == "getBatteryLevel" }
                    val value = (method?.invoke(device) as? Int) ?: -1
                    if (value in 0..100) value else -1
                } catch (_: Exception) {
                    -1
                }
                if (battery in 0..100) "$label ($battery%)" else label
            }.distinct()
            if (names.isEmpty()) {
                getString(R.string.launcher_feed_bluetooth_on)
            } else {
                getString(R.string.launcher_feed_bluetooth_connected, names.joinToString(", "))
            }
        } catch (_: Exception) {
            getString(R.string.launcher_feed_bluetooth_off)
        }
    }

    private fun getBrightnessText(): String? {
        return try {
            val mode = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                getString(R.string.launcher_feed_brightness_auto)
            } else {
                val value = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
                val percent = ((value / 255f) * 100f).toInt().coerceIn(0, 100)
                getString(R.string.launcher_feed_brightness_text, percent)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getVolumeText(): String? {
        return try {
            val audio = getSystemService(AudioManager::class.java)
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = if (max > 0) ((current.toFloat() / max.toFloat()) * 100f).toInt() else 0
            getString(R.string.launcher_feed_volume_text, percent)
        } catch (_: Exception) {
            null
        }
    }

    private fun getPowerText(): String? {
        return try {
            val pm = getSystemService(PowerManager::class.java)
            if (pm.isPowerSaveMode) {
                getString(R.string.launcher_feed_power_on)
            } else {
                getString(R.string.launcher_feed_power_off)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun bytesToHuman(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        var value = bytes.toDouble()
        var exp = 0
        while (value >= 1024.0 && exp < 6) {
            value /= 1024.0
            exp += 1
        }
        val pre = "KMGTPE"[exp - 1]
        return String.format(java.util.Locale.forLanguageTag("pl-PL"), "%.1f %sB", value, pre)
    }

    private fun getAirplaneText(): String {
        val enabled = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        return if (enabled) {
            getString(R.string.launcher_feed_airplane_on)
        } else {
            getString(R.string.launcher_feed_airplane_off)
        }
    }

    private fun getRamText(): String? {
        return try {
            val am = getSystemService(ActivityManager::class.java)
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val avail = bytesToHuman(info.availMem)
            val total = bytesToHuman(info.totalMem)
            getString(R.string.launcher_feed_ram_text, avail, total)
        } catch (_: Exception) {
            null
        }
    }

    private fun getDndText(): String? {
        return try {
            val nm = getSystemService(NotificationManager::class.java)
            when (nm.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_NONE ->
                    getString(R.string.launcher_feed_dnd_on)
                NotificationManager.INTERRUPTION_FILTER_PRIORITY ->
                    getString(R.string.launcher_feed_dnd_priority)
                NotificationManager.INTERRUPTION_FILTER_ALARMS ->
                    getString(R.string.launcher_feed_dnd_alarms)
                else -> getString(R.string.launcher_feed_dnd_off)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getRingerText(): String? {
        return try {
            val audio = getSystemService(AudioManager::class.java)
            when (audio.ringerMode) {
                AudioManager.RINGER_MODE_SILENT ->
                    getString(R.string.launcher_feed_ringer_silent)
                AudioManager.RINGER_MODE_VIBRATE ->
                    getString(R.string.launcher_feed_ringer_vibrate)
                else -> getString(R.string.launcher_feed_ringer_sound)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isBluetoothPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun formatDateTime(epochMillis: Long, allowTodayLabel: Boolean): String {
        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val date = dateTime.toLocalDate()
        val today = LocalDate.now(zone)
        return if (allowTodayLabel && date == today) {
            "Dziś ${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
        } else {
            dateTime.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "d MMM, HH:mm",
                    java.util.Locale.forLanguageTag("pl-PL")
                )
            )
        }
    }

    private fun formatDateOnly(epochMillis: Long, allowTodayLabel: Boolean): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return if (allowTodayLabel && date == today) {
            "Dziś"
        } else {
            date.format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "d MMM",
                    java.util.Locale.forLanguageTag("pl-PL")
                )
            )
        }
    }

    private fun updatePageIndicator() {
        if (!LauncherPrefs.isPageIndicatorShown(this)) {
            pageIndicator.visibility = View.GONE
            pageIndicator.removeAllViews()
            return
        }
        pageIndicator.visibility = View.VISIBLE
        val count = homeAdapter.itemCount
        if (count <= 1) {
            pageIndicator.removeAllViews()
            return
        }
        if (pageIndicator.childCount != count) {
            pageIndicator.removeAllViews()
            val height = dpToPx(6f)
            val baseWidth = dpToPx(6f)
            val activeWidth = dpToPx(18f)
            val margin = dpToPx(6f)
            val colors = LauncherPrefs.getThemeColors(this)
            for (i in 0 until count) {
                val dot = View(this)
                val params = LinearLayout.LayoutParams(baseWidth, height)
                params.marginEnd = margin
                dot.layoutParams = params
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = height / 2f
                    setColor(colors.muted)
                }
                dot.background = drawable
                dot.tag = DotMeta(drawable, baseWidth, activeWidth, height)
                pageIndicator.addView(dot)
            }
        }
        updatePageIndicatorProgress(homePager.currentItem, 0f)
    }

    private fun applyDefaultHomePage() {
        if (pages.isEmpty()) return
        val baseIndex = (LauncherPrefs.getDefaultHomePage(this) - 1).coerceIn(0, pages.lastIndex)
        val feedEnabled = LauncherPrefs.isFeedEnabled(this) && !LauncherPrefs.isSuperSimpleEnabled(this)
        val target = if (feedEnabled) baseIndex + 1 else baseIndex
        val bounded = target.coerceIn(0, homeAdapter.itemCount - 1)
        if (homePager.currentItem != bounded || lastAppliedHomeTarget != bounded) {
            homePager.setCurrentItem(bounded, false)
            lastAppliedHomeTarget = bounded
        }
    }

    private fun updatePageIndicatorProgress(position: Int, offset: Float) {
        val count = pageIndicator.childCount
        if (count == 0) return
        val colors = LauncherPrefs.getThemeColors(this)
        val total = (position + offset).coerceIn(0f, (count - 1).toFloat())
        for (i in 0 until count) {
            val dot = pageIndicator.getChildAt(i)
            val distance = kotlin.math.abs(total - i.toFloat()).coerceAtMost(1f)
            val focus = 1f - distance
            val meta = dot.tag as? DotMeta ?: continue
            val width = (meta.base + (meta.active - meta.base) * focus).toInt().coerceAtLeast(meta.base)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            if (params.width != width) {
                params.width = width
                params.height = meta.height
                dot.layoutParams = params
            }
            dot.alpha = 0.55f + focus * 0.45f
            val color = blendColor(colors.muted, colors.accent, focus)
            meta.drawable.setColor(color)
        }
    }

    private data class DotMeta(
        val drawable: android.graphics.drawable.GradientDrawable,
        val base: Int,
        val active: Int,
        val height: Int
    )

    private fun dpToPx(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt().coerceAtLeast(1)
    }

    private fun blendColor(base: Int, overlay: Int, alpha: Float): Int {
        val r = ((1 - alpha) * android.graphics.Color.red(base) + alpha * android.graphics.Color.red(overlay)).toInt()
        val g = ((1 - alpha) * android.graphics.Color.green(base) + alpha * android.graphics.Color.green(overlay)).toInt()
        val b = ((1 - alpha) * android.graphics.Color.blue(base) + alpha * android.graphics.Color.blue(overlay)).toInt()
        return android.graphics.Color.rgb(r, g, b)
    }

    private fun maybeAutoOpenExternalFeed(position: Int) {
        val feedEnabled = LauncherPrefs.isFeedEnabled(this) && !LauncherPrefs.isSuperSimpleEnabled(this)
        if (!feedEnabled) return
        if (LauncherPrefs.getFeedMode(this) != LauncherPrefs.FEED_MODE_GOOGLE) return
        if (!LauncherPrefs.isFeedAutoOpenEnabled(this)) return
        if (!isGoogleAppAvailable()) return
        if (position != 0) return
        val now = System.currentTimeMillis()
        if (now - lastExternalFeedLaunchMs < 1200L) return
        openExternalFeed(showError = false)
    }

    private fun goToNextPage() {
        val next = (homePager.currentItem + 1).coerceAtMost(homeAdapter.itemCount - 1)
        homePager.currentItem = next
    }

    private fun goToPrevPage() {
        val prev = (homePager.currentItem - 1).coerceAtLeast(0)
        homePager.currentItem = prev
    }

    private fun currentHomePageIndex(): Int {
        val feedOffset = if (LauncherPrefs.isFeedEnabled(this) && !LauncherPrefs.isSuperSimpleEnabled(this)) 1 else 0
        return (homePager.currentItem - feedOffset).coerceAtLeast(0)
    }

    private fun handleTwoFingerGesture(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    twoFingerActive = true
                    twoFingerStartX = averageX(event, 2)
                    twoFingerStartY = averageY(event, 2)
                    twoFingerLastX = twoFingerStartX
                    twoFingerLastY = twoFingerStartY
                    twoFingerStartTime = System.currentTimeMillis()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (twoFingerActive && event.pointerCount >= 2) {
                    twoFingerLastX = averageX(event, 2)
                    twoFingerLastY = averageY(event, 2)
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (twoFingerActive) {
                    val deltaX = twoFingerLastX - twoFingerStartX
                    val deltaY = twoFingerLastY - twoFingerStartY
                    val absX = kotlin.math.abs(deltaX)
                    val absY = kotlin.math.abs(deltaY)
                    val duration = System.currentTimeMillis() - twoFingerStartTime
                    if (absX < 40 && absY < 40 && duration < 250) {
                        performGestureAction(LauncherPrefs.getGestureTwoFingerTap(this))
                    } else if (absX > absY && absX > 120) {
                        if (deltaX > 0) {
                            performGestureAction(LauncherPrefs.getGestureTwoFingerRight(this))
                        } else {
                            performGestureAction(LauncherPrefs.getGestureTwoFingerLeft(this))
                        }
                    } else if (absY > 120) {
                        if (deltaY < 0) {
                            performGestureAction(LauncherPrefs.getGestureTwoFingerUp(this))
                        } else {
                            performGestureAction(LauncherPrefs.getGestureTwoFingerDown(this))
                        }
                    }
                }
                twoFingerActive = false
            }
        }
    }

    private fun handleThreeFingerGesture(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 3) {
                    threeFingerActive = true
                    threeFingerStartX = averageX(event, 3)
                    threeFingerStartY = averageY(event, 3)
                    threeFingerLastX = threeFingerStartX
                    threeFingerLastY = threeFingerStartY
                    threeFingerStartTime = System.currentTimeMillis()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (threeFingerActive && event.pointerCount >= 3) {
                    threeFingerLastX = averageX(event, 3)
                    threeFingerLastY = averageY(event, 3)
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (threeFingerActive) {
                    val deltaX = threeFingerLastX - threeFingerStartX
                    val deltaY = threeFingerLastY - threeFingerStartY
                    val absX = kotlin.math.abs(deltaX)
                    val absY = kotlin.math.abs(deltaY)
                    val duration = System.currentTimeMillis() - threeFingerStartTime
                    if (absX < 40 && absY < 40 && duration < 250) {
                        performGestureAction(LauncherPrefs.getGestureThreeFingerTap(this))
                    } else if (absX > absY && absX > 120) {
                        if (deltaX > 0) {
                            performGestureAction(LauncherPrefs.getGestureThreeFingerRight(this))
                        } else {
                            performGestureAction(LauncherPrefs.getGestureThreeFingerLeft(this))
                        }
                    } else if (absY > 120) {
                        if (deltaY < 0) {
                            performGestureAction(LauncherPrefs.getGestureThreeFingerUp(this))
                        } else {
                            performGestureAction(LauncherPrefs.getGestureThreeFingerDown(this))
                        }
                    }
                }
                threeFingerActive = false
            }
        }
    }

    private fun averageX(event: MotionEvent, count: Int): Float {
        val safe = minOf(count, event.pointerCount)
        var sum = 0f
        for (i in 0 until safe) {
            sum += event.getX(i)
        }
        return sum / safe
    }

    private fun averageY(event: MotionEvent, count: Int): Float {
        val safe = minOf(count, event.pointerCount)
        var sum = 0f
        for (i in 0 until safe) {
            sum += event.getY(i)
        }
        return sum / safe
    }

    private fun setupHotseatDrag() {
        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
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
                hotseatAdapter.moveItem(from, to)
                LauncherStore.moveItemInHotseat(this@LauncherActivity, from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
        hotseatTouchHelper = touchHelper
        applyHotseatEditingLock()
    }

    private fun applyHotseatEditingLock() {
        val locked = LauncherPrefs.isHomeEditLocked(this)
        hotseatTouchHelper?.attachToRecyclerView(if (locked) null else hotseatRow)
    }

    private fun attemptLock() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, LauncherAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            Toast.makeText(this, R.string.launcher_lock_enabled, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.launcher_lock_requires_admin, Toast.LENGTH_SHORT).show()
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.launcher_admin_explanation)
            )
            startActivity(intent)
        }
    }

    private fun onHomeItemClick(pageIndex: Int, item: HomeItem) {
        soundFeedback?.playTap()
        when (item) {
            is HomeItem.App -> launchApp(item.component)
            is HomeItem.Folder -> openFolder(item.id)
            is HomeItem.Shortcut -> openModuleShortcut(item.id)
        }
    }

    private fun onHomeItemLongClick(pageIndex: Int, item: HomeItem) {
        if (LauncherPrefs.isHomeEditLocked(this)) {
            Toast.makeText(this, R.string.launcher_edit_locked, Toast.LENGTH_SHORT).show()
            return
        }
        when (item) {
            is HomeItem.App -> showHomeAppOptions(pageIndex, item)
            is HomeItem.Folder -> showFolderOptions(pageIndex, item)
            is HomeItem.Shortcut -> showShortcutHint()
        }
    }

    private fun onHotseatItemClick(item: HomeItem) {
        soundFeedback?.playTap()
        when (item) {
            is HomeItem.App -> launchApp(item.component)
            is HomeItem.Folder -> openFolder(item.id)
            is HomeItem.Shortcut -> openModuleShortcut(item.id)
        }
    }

    private fun onHotseatItemLongClick(item: HomeItem) {
        if (LauncherPrefs.isHomeEditLocked(this)) {
            Toast.makeText(this, R.string.launcher_edit_locked, Toast.LENGTH_SHORT).show()
            return
        }
        when (item) {
            is HomeItem.App -> showHotseatAppOptions(item)
            is HomeItem.Folder -> showFolderOptions(currentHomePageIndex(), item)
            is HomeItem.Shortcut -> showShortcutHint()
        }
    }

    private fun onHomeItemMoved(pageIndex: Int, from: Int, to: Int) {
        LauncherStore.moveItemInPage(this, pageIndex, from, to)
    }

    private fun showHomeAppOptions(pageIndex: Int, app: HomeItem.App) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_remove_from_home) to {
            LauncherStore.removeFromPage(this, pageIndex, appKey(app))
            refreshHome()
        }
        options += getString(R.string.launcher_action_move_to_hotseat) to {
            val added = LauncherStore.addToHotseat(this, app.component)
            if (added) {
                LauncherStore.removeFromPage(this, pageIndex, appKey(app))
                refreshHome()
                Toast.makeText(this, R.string.launcher_added_to_hotseat, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.launcher_already_in_hotseat, Toast.LENGTH_SHORT).show()
            }
        }
        options += getString(R.string.launcher_action_create_folder) to {
            promptCreateFolder(pageIndex, app)
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker { folderId ->
                val added = LauncherStore.addToFolder(this, folderId, app.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val isFavorite = LauncherStore.isFavorite(this, app.component)
        options += getString(
            if (isFavorite) R.string.launcher_action_favorite_remove else R.string.launcher_action_favorite_add
        ) to {
            LauncherStore.setFavorite(this, app.component, !isFavorite)
            Toast.makeText(
                this,
                if (isFavorite) R.string.launcher_action_favorite_remove else R.string.launcher_action_favorite_add,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_app_info) to { openAppInfo(app.component) }
        options += getString(R.string.launcher_action_uninstall) to { confirmUninstall(app.label, app.component) }
        showOptionsDialog(getString(R.string.launcher_home_options), options)
    }

    private fun showHotseatAppOptions(app: HomeItem.App) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_remove_from_hotseat) to {
            LauncherStore.removeFromHotseat(this, appKey(app))
            refreshHome()
        }
        options += getString(R.string.launcher_action_move_to_home) to {
            val pageIndex = currentHomePageIndex()
            val added = LauncherStore.addToPage(this, pageIndex, app.component)
            if (added) {
                LauncherStore.removeFromHotseat(this, appKey(app))
                refreshHome()
                Toast.makeText(this, R.string.launcher_added_to_home, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.launcher_already_on_home, Toast.LENGTH_SHORT).show()
            }
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker { folderId ->
                val added = LauncherStore.addToFolder(this, folderId, app.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        val isFavorite = LauncherStore.isFavorite(this, app.component)
        options += getString(
            if (isFavorite) R.string.launcher_action_favorite_remove else R.string.launcher_action_favorite_add
        ) to {
            LauncherStore.setFavorite(this, app.component, !isFavorite)
            Toast.makeText(
                this,
                if (isFavorite) R.string.launcher_action_favorite_remove else R.string.launcher_action_favorite_add,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_app_info) to { openAppInfo(app.component) }
        options += getString(R.string.launcher_action_uninstall) to { confirmUninstall(app.label, app.component) }
        showOptionsDialog(getString(R.string.launcher_hotseat_options), options)
    }

    private fun showFolderOptions(pageIndex: Int, folder: HomeItem.Folder) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_open_folder) to { openFolder(folder.id) }
        options += getString(R.string.launcher_action_rename_folder) to { promptRenameFolder(folder) }
        options += getString(R.string.launcher_action_delete_folder) to {
            AlertDialog.Builder(this)
                .setTitle(R.string.launcher_action_delete_folder)
                .setMessage(R.string.launcher_delete_folder_message)
                .setPositiveButton(R.string.launcher_action_delete_folder) { _, _ ->
                    LauncherStore.removeFolderFromPage(this, pageIndex, folder.id, true)
                    refreshHome()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        showOptionsDialog(folder.label, options)
    }

    private fun promptCreateFolder(pageIndex: Int, first: HomeItem.App) {
        val candidates = pages.getOrNull(pageIndex)
            ?.filterIsInstance<HomeItem.App>()
            ?.filter { it.component != first.component }
            .orEmpty()
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folder_candidates, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder_app)
            .setItems(labels) { _, which ->
                val second = candidates[which]
                LauncherStore.createFolderOnPage(this, pageIndex, first.component, second.component)
                refreshHome()
            }
            .show()
    }

    private fun promptRenameFolder(folder: HomeItem.Folder) {
        val input = EditText(this)
        input.setText(folder.label)
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_rename_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                LauncherStore.setFolderLabel(this, folder.id, input.text.toString())
                refreshHome()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFolderPicker(onPick: (String) -> Unit) {
        val folders = LauncherStore.listFolders(this)
        if (folders.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folders, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = folders.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder)
            .setItems(labels) { _, which ->
                onPick(folders[which].id)
            }
            .show()
    }

    private fun showHomeQuickMenu() {
        val locked = LauncherPrefs.isHomeEditLocked(this)
        val options = mutableListOf<Pair<String, () -> Unit>>()
        if (!locked) {
            options += getString(R.string.launcher_action_add_app) to { openAllApps() }
            options += getString(R.string.launcher_action_add_widget) to { openWidgets() }
            options += getString(R.string.launcher_action_add_folder) to { promptCreateEmptyFolder() }
        }
        options += getString(
            if (locked) R.string.launcher_action_unlock_edit else R.string.launcher_action_lock_edit
        ) to { toggleHomeEditLock() }
        options += getString(R.string.launcher_action_toggle_feed) to { toggleFeedEnabled() }
        options += getString(R.string.launcher_action_toggle_dock) to { toggleDockVisibility() }
        options += getString(R.string.launcher_action_open_settings) to { openSettings() }
        showOptionsDialog(getString(R.string.launcher_home_quick_menu), options)
    }

    private fun toggleHomeEditLock() {
        val locked = !LauncherPrefs.isHomeEditLocked(this)
        LauncherPrefs.setHomeEditLocked(this, locked)
        applyUiConfig()
        Toast.makeText(
            this,
            if (locked) R.string.launcher_edit_locked else R.string.launcher_edit_unlocked,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun promptCreateEmptyFolder() {
        val input = EditText(this)
        input.hint = getString(R.string.launcher_action_add_folder)
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_add_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val label = input.text?.toString().orEmpty()
                val folderId = LauncherStore.createEmptyFolderOnPage(this, currentHomePageIndex(), label)
                if (folderId.isNotEmpty()) {
                    refreshHome()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
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

    private fun openAppInfo(component: ComponentName) {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", component.packageName, null)
            )
            startActivity(intent)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun confirmUninstall(label: String, component: ComponentName) {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_uninstall)
            .setMessage(getString(R.string.launcher_uninstall_confirm, label))
            .setPositiveButton(R.string.launcher_action_uninstall) { _, _ ->
                uninstallApp(component)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun uninstallApp(component: ComponentName) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = android.net.Uri.parse("package:${component.packageName}")
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_uninstall_not_allowed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFolder(folderId: String) {
        val intent = Intent(this, FolderActivity::class.java)
        intent.putExtra(FolderActivity.EXTRA_FOLDER_ID, folderId)
        startActivity(intent)
    }

    private fun launchApp(component: ComponentName) {
        LauncherStore.recordLaunch(this, component)
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun launchSearch(query: String?) {
        val text = query?.trim().orEmpty()
        val preferGoogle = LauncherPrefs.isGoogleSearchEnabled(this) && isGoogleAppAvailable()
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            if (text.isNotBlank()) {
                putExtra(SearchManager.QUERY, text)
            }
            if (preferGoogle) {
                setPackage("com.google.android.googlequicksearchbox")
            }
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            if (preferGoogle) {
                val fallback = Intent(Intent.ACTION_WEB_SEARCH)
                if (text.isNotBlank()) {
                    fallback.putExtra(SearchManager.QUERY, text)
                }
                try {
                    startActivity(fallback)
                    return
                } catch (_: Exception) {
                    // fall through
                }
            }
            Toast.makeText(this, R.string.launcher_search_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun appKey(app: HomeItem.App): String = "a:${app.component.flattenToString()}"

    private fun performGestureAction(action: Int) {
        when (action) {
            LauncherPrefs.ACTION_NONE -> Unit
            LauncherPrefs.ACTION_TOGGLE_DOCK -> toggleDockVisibility()
            LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE -> toggleSuperSimple()
            LauncherPrefs.ACTION_OPEN_ALL_APPS -> openAllApps()
            LauncherPrefs.ACTION_OPEN_WIDGETS -> openWidgets()
            LauncherPrefs.ACTION_OPEN_SETTINGS -> openSettings()
            LauncherPrefs.ACTION_OPEN_QUICK_SETTINGS -> openQuickSettings()
            LauncherPrefs.ACTION_FOCUS_SEARCH -> focusSearch()
            LauncherPrefs.ACTION_NEXT_PAGE -> goToNextPage()
            LauncherPrefs.ACTION_PREV_PAGE -> goToPrevPage()
            LauncherPrefs.ACTION_OPEN_FEED -> openFeed()
            LauncherPrefs.ACTION_VOICE_SEARCH -> startVoiceSearch()
            LauncherPrefs.ACTION_FLASHLIGHT -> toggleFlashlight()
            LauncherPrefs.ACTION_OPEN_DIALER -> openDialer()
            LauncherPrefs.ACTION_OPEN_MESSAGES -> openMessages()
            LauncherPrefs.ACTION_OPEN_GEMINI -> openGemini()
        }
        if (action != LauncherPrefs.ACTION_NONE) {
            soundFeedback?.playAction(action)
        }
    }

    private fun openExternalFeed(showError: Boolean = true): Boolean {
        LauncherDiagnosticLog.log(this, "launcher_external_feed")
        val packageName = "com.google.android.googlequicksearchbox"
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://discover")).setPackage(packageName),
            Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://feed")).setPackage(packageName),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/discover")).setPackage(packageName),
            packageManager.getLaunchIntentForPackage(packageName),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/discover"))
        ).filterNotNull()
        for (intent in intents) {
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                lastExternalFeedLaunchMs = System.currentTimeMillis()
                return true
            }
        }
        if (showError) {
            Toast.makeText(this, R.string.launcher_feed_google_missing, Toast.LENGTH_SHORT).show()
        }
        return false
    }

    private fun isGoogleAppAvailable(): Boolean {
        return packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox") != null
    }

    private fun updateWallpaperParallax(position: Int, offset: Float) {
        if (!LauncherPrefs.isWallpaperParallaxEnabled(this)) return
        val count = homeAdapter.itemCount
        if (count <= 1) return
        val total = (position + offset).coerceIn(0f, (count - 1).toFloat())
        val progress = total / (count - 1).toFloat()
        try {
            val wm = android.app.WallpaperManager.getInstance(this)
            val token = window.decorView.windowToken ?: return
            wm.setWallpaperOffsetSteps(1f / (count - 1), 0f)
            wm.setWallpaperOffsets(token, progress, 0.5f)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private fun openModuleShortcut(id: String) {
        when (id) {
            ModuleShortcuts.ID_LAUNCHER -> openSettings()
            ModuleShortcuts.ID_CALLS -> openMainSection("calls")
            ModuleShortcuts.ID_NOTIFICATIONS -> openMainSection("notifications")
            ModuleShortcuts.ID_DOCUMENTS -> openMainSection("documents")
            ModuleShortcuts.ID_CURRENCY -> openMainSection("currency")
            ModuleShortcuts.ID_LIGHT -> openMainSection("light")
            ModuleShortcuts.ID_NAVIGATION -> openMainSection("navigation")
            ModuleShortcuts.ID_FACE -> openMainSection("face")
            ModuleShortcuts.ID_CHIME -> openMainSection("chime")
            ModuleShortcuts.ID_UPDATES -> openMainSection("updates")
            ModuleShortcuts.ID_TILES -> openBlindroidComponent("com.screenreaders.blindroid.senior.ContactTilesActivity")
            ModuleShortcuts.ID_SIMPLE_PHONE -> openBlindroidComponent("com.screenreaders.blindroid.phone.SimplePhoneActivity")
            ModuleShortcuts.ID_SIMPLE_MESSAGES -> openBlindroidComponent("com.screenreaders.blindroid.sms.SimpleMessagesActivity")
            ModuleShortcuts.ID_BRAILLE -> openBlindroidComponent("com.screenreaders.blindroid.braille.BrailleSettingsActivity")
            else -> Unit
        }
    }

    private fun openBlindroidComponent(className: String) {
        val intent = Intent().apply {
            component = ComponentName("com.screenreaders.blindroid", className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openMainSection(section: String) {
        val intent = Intent().apply {
            component = ComponentName(
                "com.screenreaders.blindroid",
                "com.screenreaders.blindroid.MainActivity"
            )
            putExtra("extra_section", section)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showShortcutHint() {
        Toast.makeText(this, R.string.launcher_shortcut_hint, Toast.LENGTH_SHORT).show()
    }

    private fun openDialer() {
        val intent = Intent().apply {
            component = ComponentName("com.screenreaders.blindroid", "com.screenreaders.blindroid.phone.SimplePhoneActivity")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_DIAL))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMessages() {
        val intent = Intent().apply {
            component = ComponentName("com.screenreaders.blindroid", "com.screenreaders.blindroid.sms.SimpleMessagesActivity")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAlarms() {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCalendar() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWeather() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                weatherPermissionRequestCode
            )
            return
        }
        if (!isLocationEnabled()) {
            try {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } catch (_: Exception) {
                // ignore
            }
        }
        maybeRefreshWeather(force = true)
        launchSearch(getString(R.string.launcher_feed_weather_query))
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            weatherPermissionRequestCode
        )
    }

    private fun openLocationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun openScreenTime() {
        val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")
        if (intent != null) {
            startActivity(intent)
            return
        }
        openUsageAccess()
    }

    private fun openUsageAccess() {
        LauncherStore.openUsageAccessSettings(this)
    }

    private fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openNetworkSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDisplaySettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openDndSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSoundSettings() {
        val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_shortcut_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            openCalendar()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CALENDAR),
            calendarPermissionRequestCode
        )
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            openBluetoothSettings()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            openBluetoothSettings()
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
            bluetoothPermissionRequestCode
        )
    }

    private fun openAssistant() {
        val mode = LauncherPrefs.getAssistantMode(this)
        if (mode == LauncherPrefs.ASSISTANT_GEMINI) {
            if (openGemini()) {
                soundFeedback?.playAction(LauncherPrefs.ACTION_OPEN_GEMINI)
            } else {
                startVoiceSearch()
            }
        } else {
            soundFeedback?.playAction(LauncherPrefs.ACTION_VOICE_SEARCH)
            startVoiceSearch()
        }
    }

    private fun openGemini(): Boolean {
        val packages = listOf(
            "com.google.android.apps.bard",
            "com.google.android.apps.gemini"
        )
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return true
            }
        }
        val assist = Intent(Intent.ACTION_ASSIST)
        return try {
            startActivity(assist)
            true
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_gemini_missing, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun toggleFlashlight() {
        if (!ensureFlashlightSupport()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingFlashToggle = true
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), flashPermissionRequestCode)
            return
        }
        val manager = getSystemService(CameraManager::class.java)
        val cameraId = flashlightCameraId ?: return
        try {
            flashlightOn = !flashlightOn
            manager.setTorchMode(cameraId, flashlightOn)
            Toast.makeText(
                this,
                if (flashlightOn) R.string.launcher_flashlight_on else R.string.launcher_flashlight_off,
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.launcher_flashlight_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureFlashlightSupport(): Boolean {
        if (flashlightCameraId != null) return true
        val manager = getSystemService(CameraManager::class.java)
        val ids = manager.cameraIdList
        for (id in ids) {
            val characteristics = manager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            if (hasFlash) {
                flashlightCameraId = id
                return true
            }
        }
        Toast.makeText(this, R.string.launcher_flashlight_missing, Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == flashPermissionRequestCode) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted && pendingFlashToggle) {
                pendingFlashToggle = false
                toggleFlashlight()
            } else {
                Toast.makeText(this, R.string.launcher_flashlight_permission, Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == calendarPermissionRequestCode) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                refreshHome()
                applyUiConfig()
                openCalendar()
            }
        } else if (requestCode == bluetoothPermissionRequestCode) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                refreshHome()
                applyUiConfig()
                openBluetoothSettings()
            } else {
                Toast.makeText(this, R.string.launcher_feed_bluetooth_permission, Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == weatherPermissionRequestCode) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                maybeRefreshWeather(force = true)
                refreshHome()
                applyUiConfig()
            } else {
                Toast.makeText(this, R.string.launcher_feed_weather_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
