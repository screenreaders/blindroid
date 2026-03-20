package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LauncherSettingsActivity : AppCompatActivity() {
    private lateinit var columnsGroup: RadioGroup
    private lateinit var rowsGroup: RadioGroup
    private lateinit var iconSizeGroup: RadioGroup
    private lateinit var labelSizeGroup: RadioGroup
    private lateinit var doubleTapSwitch: Switch
    private lateinit var hideDockLabelsSwitch: Switch
    private lateinit var superSimpleSwitch: Switch
    private lateinit var superSimpleGridSpinner: Spinner
    private lateinit var simpleFavoritesGridSpinner: Spinner
    private lateinit var feedEnabledSwitch: Switch
    private lateinit var feedModeSpinner: Spinner
    private lateinit var feedAutoOpenSwitch: Switch
    private lateinit var searchBarSwitch: Switch
    private lateinit var googleSearchSwitch: Switch
    private lateinit var googleVoiceSwitch: Switch
    private lateinit var nowAlarmSwitch: Switch
    private lateinit var nowCalendarSwitch: Switch
    private lateinit var nowWeatherSwitch: Switch
    private lateinit var nowBatterySwitch: Switch
    private lateinit var nowRemindersSwitch: Switch
    private lateinit var nowHeadphonesSwitch: Switch
    private lateinit var nowLocationSwitch: Switch
    private lateinit var nowNetworkSwitch: Switch
    private lateinit var nowStorageSwitch: Switch
    private lateinit var nowScreenTimeSwitch: Switch
    private lateinit var nowTopAppsSwitch: Switch
    private lateinit var nowAirplaneSwitch: Switch
    private lateinit var nowRamSwitch: Switch
    private lateinit var nowDndSwitch: Switch
    private lateinit var nowRingerSwitch: Switch
    private lateinit var nowBluetoothSwitch: Switch
    private lateinit var nowBrightnessSwitch: Switch
    private lateinit var nowVolumeSwitch: Switch
    private lateinit var nowPowerSwitch: Switch
    private lateinit var gnLayoutSwitch: Switch
    private lateinit var wallpaperParallaxSwitch: Switch
    private lateinit var pageAnimationSpinner: Spinner
    private lateinit var assistantSpinner: Spinner
    private lateinit var dockVisibleSwitch: Switch
    private lateinit var smartHotseatSwitch: Switch
    private lateinit var themeGroup: RadioGroup
    private lateinit var invertColorsSwitch: Switch
    private lateinit var iconStyleGroup: RadioGroup
    private lateinit var wallpaperButton: Button
    private lateinit var closeButton: Button
    private lateinit var soundFeedbackSwitch: Switch
    private lateinit var soundVolumeSeek: android.widget.SeekBar
    private lateinit var soundVolumeValue: TextView
    private lateinit var soundSchemeSpinner: Spinner
    private lateinit var backupButton: Button
    private lateinit var restoreButton: Button
    private lateinit var usageStatsSwitch: Switch
    private lateinit var gestureTwoTapSpinner: Spinner
    private lateinit var gestureTwoUpSpinner: Spinner
    private lateinit var gestureTwoDownSpinner: Spinner
    private lateinit var gestureTwoLeftSpinner: Spinner
    private lateinit var gestureTwoRightSpinner: Spinner
    private lateinit var gestureThreeTapSpinner: Spinner
    private lateinit var gestureThreeUpSpinner: Spinner
    private lateinit var gestureThreeDownSpinner: Spinner
    private lateinit var gestureThreeLeftSpinner: Spinner
    private lateinit var gestureThreeRightSpinner: Spinner

    private data class GestureActionOption(val id: Int, val labelRes: Int)
    private val gestureActions = listOf(
        GestureActionOption(LauncherPrefs.ACTION_NONE, R.string.launcher_gesture_action_none),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_ALL_APPS, R.string.launcher_gesture_action_all_apps),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_WIDGETS, R.string.launcher_gesture_action_widgets),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_SETTINGS, R.string.launcher_gesture_action_settings),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_QUICK_SETTINGS, R.string.launcher_gesture_action_quick_settings),
        GestureActionOption(LauncherPrefs.ACTION_FOCUS_SEARCH, R.string.launcher_gesture_action_search),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_FEED, R.string.launcher_gesture_action_feed),
        GestureActionOption(LauncherPrefs.ACTION_VOICE_SEARCH, R.string.launcher_gesture_action_voice),
        GestureActionOption(LauncherPrefs.ACTION_FLASHLIGHT, R.string.launcher_gesture_action_flashlight),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_DIALER, R.string.launcher_gesture_action_phone),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_MESSAGES, R.string.launcher_gesture_action_messages),
        GestureActionOption(LauncherPrefs.ACTION_OPEN_GEMINI, R.string.launcher_gesture_action_gemini),
        GestureActionOption(LauncherPrefs.ACTION_TOGGLE_DOCK, R.string.launcher_gesture_action_toggle_dock),
        GestureActionOption(LauncherPrefs.ACTION_TOGGLE_SUPER_SIMPLE, R.string.launcher_gesture_action_toggle_simple),
        GestureActionOption(LauncherPrefs.ACTION_NEXT_PAGE, R.string.launcher_gesture_action_next_page),
        GestureActionOption(LauncherPrefs.ACTION_PREV_PAGE, R.string.launcher_gesture_action_prev_page)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_settings)

        columnsGroup = findViewById(R.id.columnsGroup)
        rowsGroup = findViewById(R.id.rowsGroup)
        iconSizeGroup = findViewById(R.id.iconSizeGroup)
        labelSizeGroup = findViewById(R.id.labelSizeGroup)
        doubleTapSwitch = findViewById(R.id.doubleTapLockSwitch)
        hideDockLabelsSwitch = findViewById(R.id.hideDockLabelsSwitch)
        superSimpleSwitch = findViewById(R.id.superSimpleSwitch)
        superSimpleGridSpinner = findViewById(R.id.superSimpleGridSpinner)
        simpleFavoritesGridSpinner = findViewById(R.id.simpleFavoritesGridSpinner)
        feedEnabledSwitch = findViewById(R.id.feedEnabledSwitch)
        feedModeSpinner = findViewById(R.id.feedModeSpinner)
        feedAutoOpenSwitch = findViewById(R.id.feedAutoOpenSwitch)
        searchBarSwitch = findViewById(R.id.searchBarSwitch)
        googleSearchSwitch = findViewById(R.id.googleSearchSwitch)
        googleVoiceSwitch = findViewById(R.id.googleVoiceSwitch)
        nowAlarmSwitch = findViewById(R.id.nowAlarmSwitch)
        nowCalendarSwitch = findViewById(R.id.nowCalendarSwitch)
        nowWeatherSwitch = findViewById(R.id.nowWeatherSwitch)
        nowBatterySwitch = findViewById(R.id.nowBatterySwitch)
        nowRemindersSwitch = findViewById(R.id.nowRemindersSwitch)
        nowHeadphonesSwitch = findViewById(R.id.nowHeadphonesSwitch)
        nowLocationSwitch = findViewById(R.id.nowLocationSwitch)
        nowNetworkSwitch = findViewById(R.id.nowNetworkSwitch)
        nowStorageSwitch = findViewById(R.id.nowStorageSwitch)
        nowScreenTimeSwitch = findViewById(R.id.nowScreenTimeSwitch)
        nowTopAppsSwitch = findViewById(R.id.nowTopAppsSwitch)
        nowAirplaneSwitch = findViewById(R.id.nowAirplaneSwitch)
        nowRamSwitch = findViewById(R.id.nowRamSwitch)
        nowDndSwitch = findViewById(R.id.nowDndSwitch)
        nowRingerSwitch = findViewById(R.id.nowRingerSwitch)
        nowBluetoothSwitch = findViewById(R.id.nowBluetoothSwitch)
        nowBrightnessSwitch = findViewById(R.id.nowBrightnessSwitch)
        nowVolumeSwitch = findViewById(R.id.nowVolumeSwitch)
        nowPowerSwitch = findViewById(R.id.nowPowerSwitch)
        gnLayoutSwitch = findViewById(R.id.gnLayoutSwitch)
        wallpaperParallaxSwitch = findViewById(R.id.wallpaperParallaxSwitch)
        pageAnimationSpinner = findViewById(R.id.pageAnimationSpinner)
        assistantSpinner = findViewById(R.id.assistantSpinner)
        dockVisibleSwitch = findViewById(R.id.dockVisibleSwitch)
        smartHotseatSwitch = findViewById(R.id.smartHotseatSwitch)
        themeGroup = findViewById(R.id.themeGroup)
        invertColorsSwitch = findViewById(R.id.invertColorsSwitch)
        iconStyleGroup = findViewById(R.id.iconStyleGroup)
        wallpaperButton = findViewById(R.id.wallpaperButton)
        closeButton = findViewById(R.id.closeSettingsButton)
        soundFeedbackSwitch = findViewById(R.id.soundFeedbackSwitch)
        soundVolumeSeek = findViewById(R.id.soundVolumeSeek)
        soundVolumeValue = findViewById(R.id.soundVolumeValue)
        soundSchemeSpinner = findViewById(R.id.soundSchemeSpinner)
        backupButton = findViewById(R.id.backupButton)
        restoreButton = findViewById(R.id.restoreButton)
        usageStatsSwitch = findViewById(R.id.usageStatsSwitch)
        gestureTwoTapSpinner = findViewById(R.id.gestureTwoTapSpinner)
        gestureTwoUpSpinner = findViewById(R.id.gestureTwoUpSpinner)
        gestureTwoDownSpinner = findViewById(R.id.gestureTwoDownSpinner)
        gestureTwoLeftSpinner = findViewById(R.id.gestureTwoLeftSpinner)
        gestureTwoRightSpinner = findViewById(R.id.gestureTwoRightSpinner)
        gestureThreeTapSpinner = findViewById(R.id.gestureThreeTapSpinner)
        gestureThreeUpSpinner = findViewById(R.id.gestureThreeUpSpinner)
        gestureThreeDownSpinner = findViewById(R.id.gestureThreeDownSpinner)
        gestureThreeLeftSpinner = findViewById(R.id.gestureThreeLeftSpinner)
        gestureThreeRightSpinner = findViewById(R.id.gestureThreeRightSpinner)

        bindInitialState()
        bindListeners()

        closeButton.setOnClickListener { finish() }
    }

    private fun bindInitialState() {
        when (LauncherPrefs.getColumns(this)) {
            5 -> columnsGroup.check(R.id.columns5)
            else -> columnsGroup.check(R.id.columns4)
        }
        when (LauncherPrefs.getRows(this)) {
            4 -> rowsGroup.check(R.id.rows4)
            6 -> rowsGroup.check(R.id.rows6)
            else -> rowsGroup.check(R.id.rows5)
        }
        when (LauncherPrefs.getIconSizeDp(this)) {
            in 0..44 -> iconSizeGroup.check(R.id.iconSmall)
            in 45..52 -> iconSizeGroup.check(R.id.iconNormal)
            else -> iconSizeGroup.check(R.id.iconLarge)
        }
        when (LauncherPrefs.getLabelSizeSp(this).toInt()) {
            in 0..12 -> labelSizeGroup.check(R.id.labelSmall)
            13, 14 -> labelSizeGroup.check(R.id.labelNormal)
            else -> labelSizeGroup.check(R.id.labelLarge)
        }
        doubleTapSwitch.isChecked = LauncherPrefs.isDoubleTapLockEnabled(this)
        hideDockLabelsSwitch.isChecked = LauncherPrefs.isDockLabelsHidden(this)
        superSimpleSwitch.isChecked = LauncherPrefs.isSuperSimpleEnabled(this)
        bindSuperSimpleGrid()
        bindSimpleFavoritesGrid()
        feedEnabledSwitch.isChecked = LauncherPrefs.isFeedEnabled(this)
        bindFeedModeSpinner(LauncherPrefs.getFeedMode(this))
        feedAutoOpenSwitch.isChecked = LauncherPrefs.isFeedAutoOpenEnabled(this)
        searchBarSwitch.isChecked = LauncherPrefs.isSearchBarEnabled(this)
        googleSearchSwitch.isChecked = LauncherPrefs.isGoogleSearchEnabled(this)
        googleVoiceSwitch.isChecked = LauncherPrefs.isGoogleVoiceEnabled(this)
        nowAlarmSwitch.isChecked = LauncherPrefs.isNowAlarmEnabled(this)
        nowCalendarSwitch.isChecked = LauncherPrefs.isNowCalendarEnabled(this)
        nowWeatherSwitch.isChecked = LauncherPrefs.isNowWeatherEnabled(this)
        nowBatterySwitch.isChecked = LauncherPrefs.isNowBatteryEnabled(this)
        nowRemindersSwitch.isChecked = LauncherPrefs.isNowRemindersEnabled(this)
        nowHeadphonesSwitch.isChecked = LauncherPrefs.isNowHeadphonesEnabled(this)
        nowLocationSwitch.isChecked = LauncherPrefs.isNowLocationEnabled(this)
        nowNetworkSwitch.isChecked = LauncherPrefs.isNowNetworkEnabled(this)
        nowStorageSwitch.isChecked = LauncherPrefs.isNowStorageEnabled(this)
        nowScreenTimeSwitch.isChecked = LauncherPrefs.isNowScreenTimeEnabled(this)
        nowTopAppsSwitch.isChecked = LauncherPrefs.isNowTopAppsEnabled(this)
        nowAirplaneSwitch.isChecked = LauncherPrefs.isNowAirplaneEnabled(this)
        nowRamSwitch.isChecked = LauncherPrefs.isNowRamEnabled(this)
        nowDndSwitch.isChecked = LauncherPrefs.isNowDndEnabled(this)
        nowRingerSwitch.isChecked = LauncherPrefs.isNowRingerEnabled(this)
        nowBluetoothSwitch.isChecked = LauncherPrefs.isNowBluetoothEnabled(this)
        nowBrightnessSwitch.isChecked = LauncherPrefs.isNowBrightnessEnabled(this)
        nowVolumeSwitch.isChecked = LauncherPrefs.isNowVolumeEnabled(this)
        nowPowerSwitch.isChecked = LauncherPrefs.isNowPowerEnabled(this)
        gnLayoutSwitch.isChecked = LauncherPrefs.isGnLayoutEnabled(this)
        wallpaperParallaxSwitch.isChecked = LauncherPrefs.isWallpaperParallaxEnabled(this)
        bindPageAnimation()
        bindAssistantSpinner()
        dockVisibleSwitch.isChecked = LauncherPrefs.isDockVisible(this)
        smartHotseatSwitch.isChecked = LauncherPrefs.isSmartHotseatEnabled(this)
        when (LauncherPrefs.getTheme(this)) {
            1 -> themeGroup.check(R.id.themeDark)
            2 -> themeGroup.check(R.id.themeBlack)
            3 -> themeGroup.check(R.id.themeBlue)
            4 -> themeGroup.check(R.id.themeHighContrast)
            5 -> themeGroup.check(R.id.themeYellow)
            else -> themeGroup.check(R.id.themeLight)
        }
        invertColorsSwitch.isChecked = LauncherPrefs.isInvertColorsEnabled(this)
        when (LauncherPrefs.getIconStyle(this)) {
            1 -> iconStyleGroup.check(R.id.iconStyleCircle)
            else -> iconStyleGroup.check(R.id.iconStyleNone)
        }
        soundFeedbackSwitch.isChecked = LauncherPrefs.isSoundFeedbackEnabled(this)
        bindSoundVolume()
        bindSoundScheme()
        bindGestureSpinners()
        usageStatsSwitch.isChecked = LauncherPrefs.isUsageSuggestionsEnabled(this)
        applySuperSimpleState(superSimpleSwitch.isChecked)
        applyLayoutModeState()
        feedModeSpinner.isEnabled = feedEnabledSwitch.isChecked && !superSimpleSwitch.isChecked
        googleSearchSwitch.isEnabled = isGoogleAppAvailable()
        googleVoiceSwitch.isEnabled = isGoogleAppAvailable()
        updateFeedAutoOpenState()
    }

    private fun bindListeners() {
        columnsGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.columns5 -> 5
                else -> 4
            }
            LauncherPrefs.setColumns(this, value)
            toastSaved()
        }

        rowsGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.rows4 -> 4
                R.id.rows6 -> 6
                else -> 5
            }
            LauncherPrefs.setRows(this, value)
            toastSaved()
        }

        iconSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.iconSmall -> 40
                R.id.iconLarge -> 56
                else -> 48
            }
            LauncherPrefs.setIconSizeDp(this, value)
            toastSaved()
        }

        labelSizeGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.labelSmall -> 12f
                R.id.labelLarge -> 16f
                else -> 14f
            }
            LauncherPrefs.setLabelSizeSp(this, value)
            toastSaved()
        }

        doubleTapSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setDoubleTapLockEnabled(this, isChecked)
            toastSaved()
        }

        hideDockLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setDockLabelsHidden(this, isChecked)
            toastSaved()
        }

        smartHotseatSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSmartHotseatEnabled(this, isChecked)
            toastSaved()
        }

        superSimpleSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSuperSimpleEnabled(this, isChecked)
            applySuperSimpleState(isChecked)
            applyLayoutModeState()
            updateFeedAutoOpenState()
            toastSaved()
        }

        superSimpleGridSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        LauncherPrefs.setSuperSimpleColumns(this@LauncherSettingsActivity, 2)
                        LauncherPrefs.setSuperSimpleRows(this@LauncherSettingsActivity, 3)
                    }
                    1 -> {
                        LauncherPrefs.setSuperSimpleColumns(this@LauncherSettingsActivity, 3)
                        LauncherPrefs.setSuperSimpleRows(this@LauncherSettingsActivity, 4)
                    }
                    else -> {
                        LauncherPrefs.setSuperSimpleColumns(this@LauncherSettingsActivity, 4)
                        LauncherPrefs.setSuperSimpleRows(this@LauncherSettingsActivity, 5)
                    }
                }
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        simpleFavoritesGridSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                when (position) {
                    0 -> {
                        LauncherPrefs.setSimpleFavoritesColumns(this@LauncherSettingsActivity, 2)
                        LauncherPrefs.setSimpleFavoritesRows(this@LauncherSettingsActivity, 2)
                    }
                    1 -> {
                        LauncherPrefs.setSimpleFavoritesColumns(this@LauncherSettingsActivity, 2)
                        LauncherPrefs.setSimpleFavoritesRows(this@LauncherSettingsActivity, 3)
                    }
                    2 -> {
                        LauncherPrefs.setSimpleFavoritesColumns(this@LauncherSettingsActivity, 3)
                        LauncherPrefs.setSimpleFavoritesRows(this@LauncherSettingsActivity, 3)
                    }
                    else -> {
                        LauncherPrefs.setSimpleFavoritesColumns(this@LauncherSettingsActivity, 3)
                        LauncherPrefs.setSimpleFavoritesRows(this@LauncherSettingsActivity, 4)
                    }
                }
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        feedEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setFeedEnabled(this, isChecked)
            feedModeSpinner.isEnabled = isChecked && !superSimpleSwitch.isChecked
            updateFeedAutoOpenState()
            toastSaved()
        }

        feedAutoOpenSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setFeedAutoOpenEnabled(this, isChecked)
            toastSaved()
        }

        searchBarSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSearchBarEnabled(this, isChecked)
            toastSaved()
        }

        googleSearchSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isGoogleAppAvailable() && isChecked) {
                googleSearchSwitch.isChecked = false
                toastSaved()
                return@setOnCheckedChangeListener
            }
            LauncherPrefs.setGoogleSearchEnabled(this, isChecked)
            toastSaved()
        }

        googleVoiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isGoogleAppAvailable() && isChecked) {
                googleVoiceSwitch.isChecked = false
                toastSaved()
                return@setOnCheckedChangeListener
            }
            LauncherPrefs.setGoogleVoiceEnabled(this, isChecked)
            toastSaved()
        }

        nowAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowAlarmEnabled(this, isChecked)
            toastSaved()
        }

        nowCalendarSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowCalendarEnabled(this, isChecked)
            toastSaved()
        }

        nowWeatherSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowWeatherEnabled(this, isChecked)
            toastSaved()
        }

        nowBatterySwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowBatteryEnabled(this, isChecked)
            toastSaved()
        }

        nowRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowRemindersEnabled(this, isChecked)
            toastSaved()
        }

        nowHeadphonesSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowHeadphonesEnabled(this, isChecked)
            toastSaved()
        }

        nowLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowLocationEnabled(this, isChecked)
            toastSaved()
        }

        nowNetworkSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowNetworkEnabled(this, isChecked)
            toastSaved()
        }

        nowStorageSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowStorageEnabled(this, isChecked)
            toastSaved()
        }

        nowScreenTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowScreenTimeEnabled(this, isChecked)
            if (isChecked && !LauncherStore.hasUsageStatsPermission(this)) {
                Toast.makeText(this, R.string.launcher_usage_access_missing, Toast.LENGTH_LONG).show()
            }
            toastSaved()
        }

        nowTopAppsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowTopAppsEnabled(this, isChecked)
            toastSaved()
        }

        nowAirplaneSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowAirplaneEnabled(this, isChecked)
            toastSaved()
        }

        nowRamSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowRamEnabled(this, isChecked)
            toastSaved()
        }

        nowDndSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowDndEnabled(this, isChecked)
            toastSaved()
        }

        nowRingerSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowRingerEnabled(this, isChecked)
            toastSaved()
        }

        nowBluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowBluetoothEnabled(this, isChecked)
            toastSaved()
        }

        nowBrightnessSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowBrightnessEnabled(this, isChecked)
            toastSaved()
        }

        nowVolumeSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowVolumeEnabled(this, isChecked)
            toastSaved()
        }

        nowPowerSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowPowerEnabled(this, isChecked)
            toastSaved()
        }

        usageStatsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setUsageSuggestionsEnabled(this, isChecked)
            if (isChecked && !LauncherStore.hasUsageStatsPermission(this)) {
                Toast.makeText(this, R.string.launcher_usage_access_missing, Toast.LENGTH_LONG).show()
            }
            toastSaved()
        }

        gnLayoutSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setGnLayoutEnabled(this, isChecked)
            applyLayoutModeState()
            toastSaved()
        }

        wallpaperParallaxSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setWallpaperParallaxEnabled(this, isChecked)
            toastSaved()
        }

        pageAnimationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setPageAnimation(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        dockVisibleSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setDockVisible(this, isChecked)
            toastSaved()
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.themeDark -> 1
                R.id.themeBlack -> 2
                R.id.themeBlue -> 3
                R.id.themeHighContrast -> 4
                R.id.themeYellow -> 5
                else -> 0
            }
            LauncherPrefs.setTheme(this, theme)
            toastSaved()
        }

        invertColorsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setInvertColorsEnabled(this, isChecked)
            toastSaved()
        }

        iconStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.iconStyleCircle -> 1
                else -> 0
            }
            LauncherPrefs.setIconStyle(this, style)
            toastSaved()
        }

        wallpaperButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.launcher_search_missing, Toast.LENGTH_SHORT).show()
            }
        }

        soundFeedbackSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSoundFeedbackEnabled(this, isChecked)
            soundVolumeSeek.isEnabled = isChecked
            soundSchemeSpinner.isEnabled = isChecked
            toastSaved()
        }

        backupButton.setOnClickListener { backupLauncher() }
        restoreButton.setOnClickListener { restoreLauncher() }
    }

    private fun bindPageAnimation() {
        val labels = listOf(
            getString(R.string.launcher_page_anim_default),
            getString(R.string.launcher_page_anim_carousel),
            getString(R.string.launcher_page_anim_depth),
            getString(R.string.launcher_page_anim_stack)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pageAnimationSpinner.adapter = adapter
        pageAnimationSpinner.setSelection(LauncherPrefs.getPageAnimation(this))
    }

    private fun bindSuperSimpleGrid() {
        val labels = listOf(
            getString(R.string.launcher_simple_grid_2x3),
            getString(R.string.launcher_simple_grid_3x4),
            getString(R.string.launcher_simple_grid_4x5)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        superSimpleGridSpinner.adapter = adapter
        val columns = LauncherPrefs.getSuperSimpleColumns(this)
        val rows = LauncherPrefs.getSuperSimpleRows(this)
        val index = when {
            columns <= 2 && rows <= 3 -> 0
            columns >= 4 || rows >= 5 -> 2
            else -> 1
        }
        superSimpleGridSpinner.setSelection(index)
    }

    private fun bindSimpleFavoritesGrid() {
        val labels = listOf(
            getString(R.string.launcher_simple_fav_2x2),
            getString(R.string.launcher_simple_fav_2x3),
            getString(R.string.launcher_simple_fav_3x3),
            getString(R.string.launcher_simple_fav_3x4)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        simpleFavoritesGridSpinner.adapter = adapter
        val columns = LauncherPrefs.getSimpleFavoritesColumns(this)
        val rows = LauncherPrefs.getSimpleFavoritesRows(this)
        val index = when {
            columns == 2 && rows == 2 -> 0
            columns == 2 && rows == 3 -> 1
            columns == 3 && rows == 3 -> 2
            else -> 3
        }
        simpleFavoritesGridSpinner.setSelection(index)
    }

    private fun applySuperSimpleState(enabled: Boolean) {
        setGroupEnabled(columnsGroup, !enabled)
        setGroupEnabled(rowsGroup, !enabled)
        setGroupEnabled(iconSizeGroup, !enabled)
        setGroupEnabled(labelSizeGroup, !enabled)
        feedEnabledSwitch.isEnabled = !enabled
        feedModeSpinner.isEnabled = !enabled && feedEnabledSwitch.isChecked
        dockVisibleSwitch.isEnabled = !enabled
        hideDockLabelsSwitch.isEnabled = !enabled
        setGroupEnabled(themeGroup, !enabled)
        setGroupEnabled(iconStyleGroup, !enabled)
        invertColorsSwitch.isEnabled = !enabled
        gnLayoutSwitch.isEnabled = !enabled
        wallpaperParallaxSwitch.isEnabled = !enabled
        superSimpleGridSpinner.isEnabled = enabled
        simpleFavoritesGridSpinner.isEnabled = enabled
    }

    private fun applyLayoutModeState() {
        val simple = superSimpleSwitch.isChecked
        val gnLayout = gnLayoutSwitch.isChecked
        val allowCustom = !simple && !gnLayout
        setGroupEnabled(columnsGroup, allowCustom)
        setGroupEnabled(rowsGroup, allowCustom)
        setGroupEnabled(iconSizeGroup, allowCustom)
        setGroupEnabled(labelSizeGroup, allowCustom)
    }

    private fun setGroupEnabled(group: RadioGroup, enabled: Boolean) {
        group.isEnabled = enabled
        for (i in 0 until group.childCount) {
            group.getChildAt(i).isEnabled = enabled
        }
    }

    private fun toastSaved() {
        Toast.makeText(this, R.string.launcher_settings_saved, Toast.LENGTH_SHORT).show()
    }

    private fun updateFeedAutoOpenState() {
        val feedEnabled = feedEnabledSwitch.isChecked && !superSimpleSwitch.isChecked
        val googleMode = feedModeSpinner.selectedItemPosition == LauncherPrefs.FEED_MODE_GOOGLE
        feedAutoOpenSwitch.isEnabled = feedEnabled && googleMode
    }

    private fun isGoogleAppAvailable(): Boolean {
        return packageManager.getLaunchIntentForPackage("com.google.android.googlequicksearchbox") != null
    }

    private fun bindFeedModeSpinner(current: Int) {
        val labels = listOf(
            getString(R.string.launcher_feed_mode_local),
            getString(R.string.launcher_feed_mode_google),
            getString(R.string.launcher_feed_mode_embedded)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        feedModeSpinner.adapter = adapter
        feedModeSpinner.setSelection(current.coerceIn(0, 2), false)
        feedModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setFeedMode(this@LauncherSettingsActivity, position)
                updateFeedAutoOpenState()
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindGestureSpinners() {
        bindGestureSpinner(gestureTwoTapSpinner, LauncherPrefs.getGestureTwoFingerTap(this)) {
            LauncherPrefs.setGestureTwoFingerTap(this, it)
        }
        bindGestureSpinner(gestureTwoUpSpinner, LauncherPrefs.getGestureTwoFingerUp(this)) {
            LauncherPrefs.setGestureTwoFingerUp(this, it)
        }
        bindGestureSpinner(gestureTwoDownSpinner, LauncherPrefs.getGestureTwoFingerDown(this)) {
            LauncherPrefs.setGestureTwoFingerDown(this, it)
        }
        bindGestureSpinner(gestureTwoLeftSpinner, LauncherPrefs.getGestureTwoFingerLeft(this)) {
            LauncherPrefs.setGestureTwoFingerLeft(this, it)
        }
        bindGestureSpinner(gestureTwoRightSpinner, LauncherPrefs.getGestureTwoFingerRight(this)) {
            LauncherPrefs.setGestureTwoFingerRight(this, it)
        }
        bindGestureSpinner(gestureThreeTapSpinner, LauncherPrefs.getGestureThreeFingerTap(this)) {
            LauncherPrefs.setGestureThreeFingerTap(this, it)
        }
        bindGestureSpinner(gestureThreeUpSpinner, LauncherPrefs.getGestureThreeFingerUp(this)) {
            LauncherPrefs.setGestureThreeFingerUp(this, it)
        }
        bindGestureSpinner(gestureThreeDownSpinner, LauncherPrefs.getGestureThreeFingerDown(this)) {
            LauncherPrefs.setGestureThreeFingerDown(this, it)
        }
        bindGestureSpinner(gestureThreeLeftSpinner, LauncherPrefs.getGestureThreeFingerLeft(this)) {
            LauncherPrefs.setGestureThreeFingerLeft(this, it)
        }
        bindGestureSpinner(gestureThreeRightSpinner, LauncherPrefs.getGestureThreeFingerRight(this)) {
            LauncherPrefs.setGestureThreeFingerRight(this, it)
        }
    }

    private fun bindGestureSpinner(spinner: Spinner, current: Int, onSelected: (Int) -> Unit) {
        val labels = gestureActions.map { getString(it.labelRes) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        val index = gestureActions.indexOfFirst { it.id == current }.coerceAtLeast(0)
        spinner.setSelection(index, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val action = gestureActions[position].id
                onSelected(action)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindSoundVolume() {
        val current = LauncherPrefs.getSoundFeedbackVolume(this)
        soundVolumeSeek.max = 100
        soundVolumeSeek.progress = current
        soundVolumeValue.text = "${current}%"
        soundVolumeSeek.isEnabled = soundFeedbackSwitch.isChecked
        soundVolumeSeek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPrefs.setSoundFeedbackVolume(this@LauncherSettingsActivity, progress)
                soundVolumeValue.text = "${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                toastSaved()
            }
        })
    }

    private fun bindSoundScheme() {
        val labels = listOf(
            getString(R.string.launcher_sound_scheme_classic),
            getString(R.string.launcher_sound_scheme_soft),
            getString(R.string.launcher_sound_scheme_sharp)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        soundSchemeSpinner.adapter = adapter
        soundSchemeSpinner.setSelection(LauncherPrefs.getSoundFeedbackScheme(this).coerceIn(0, 2), false)
        soundSchemeSpinner.isEnabled = soundFeedbackSwitch.isChecked
        soundSchemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setSoundFeedbackScheme(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindAssistantSpinner() {
        val labels = listOf(
            getString(R.string.launcher_assistant_gemini),
            getString(R.string.launcher_assistant_voice)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        assistantSpinner.adapter = adapter
        val mode = LauncherPrefs.getAssistantMode(this)
        assistantSpinner.setSelection(if (mode == LauncherPrefs.ASSISTANT_GEMINI) 0 else 1, false)
        assistantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selected = if (position == 0) LauncherPrefs.ASSISTANT_GEMINI else LauncherPrefs.ASSISTANT_VOICE
                LauncherPrefs.setAssistantMode(this@LauncherSettingsActivity, selected)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private val backupLauncherFile = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val success = LauncherBackup.writeBackup(this, uri)
        Toast.makeText(
            this,
            if (success) R.string.launcher_backup_ok else R.string.launcher_backup_fail,
            Toast.LENGTH_SHORT
        ).show()
    }

    private val restoreLauncherFile = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        val success = LauncherBackup.readBackup(this, uri)
        Toast.makeText(
            this,
            if (success) R.string.launcher_restore_ok else R.string.launcher_restore_fail,
            Toast.LENGTH_SHORT
        ).show()
        if (success) {
            recreate()
        }
    }

    private fun backupLauncher() {
        backupLauncherFile.launch("blindroid-launcher-backup.json")
    }

    private fun restoreLauncher() {
        restoreLauncherFile.launch(arrayOf("application/json"))
    }
}
