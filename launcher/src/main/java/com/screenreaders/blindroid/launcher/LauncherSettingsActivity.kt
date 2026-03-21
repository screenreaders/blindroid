package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class LauncherSettingsActivity : AppCompatActivity() {
    private lateinit var columnsGroup: RadioGroup
    private lateinit var rowsGroup: RadioGroup
    private lateinit var pageCountSpinner: Spinner
    private lateinit var iconSizeGroup: RadioGroup
    private lateinit var labelSizeGroup: RadioGroup
    private lateinit var doubleTapSwitch: Switch
    private lateinit var homeEditLockSwitch: Switch
    private lateinit var hideHomeLabelsSwitch: Switch
    private lateinit var showPageIndicatorSwitch: Switch
    private lateinit var defaultHomePageSpinner: Spinner
    private lateinit var resetLayoutButton: Button
    private lateinit var clearFavoritesButton: Button
    private lateinit var clearHiddenButton: Button
    private lateinit var clearCustomButton: Button
    private lateinit var hideDockLabelsSwitch: Switch
    private lateinit var superSimpleSwitch: Switch
    private lateinit var superSimpleGridSpinner: Spinner
    private lateinit var simpleFavoritesGridSpinner: Spinner
    private lateinit var feedEnabledSwitch: Switch
    private lateinit var feedModeSpinner: Spinner
    private lateinit var feedAutoOpenSwitch: Switch
    private lateinit var feedQuickActionsSwitch: Switch
    private lateinit var searchBarSwitch: Switch
    private lateinit var googleSearchSwitch: Switch
    private lateinit var googleVoiceSwitch: Switch
    private lateinit var allAppsLabelsSwitch: Switch
    private lateinit var showFavoritesSectionSwitch: Switch
    private lateinit var showSuggestedNowSectionSwitch: Switch
    private lateinit var showSuggestedSectionSwitch: Switch
    private lateinit var showRecentSectionSwitch: Switch
    private lateinit var showCategoriesRowSwitch: Switch
    private lateinit var showScrollButtonsSwitch: Switch
    private lateinit var showVoiceSearchSwitch: Switch
    private lateinit var showResultsCountSwitch: Switch
    private lateinit var showSystemAppsSwitch: Switch
    private lateinit var sortDescendingSwitch: Switch
    private lateinit var suggestionModeSpinner: Spinner
    private lateinit var allAppsColumnsSpinner: Spinner
    private lateinit var allAppsDefaultTabSpinner: Spinner
    private lateinit var favoritesCountSpinner: Spinner
    private lateinit var suggestedNowCountSpinner: Spinner
    private lateinit var suggestedCountSpinner: Spinner
    private lateinit var recentCountSpinner: Spinner
    private lateinit var categoryRankSpinner: Spinner
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
    private lateinit var nowDeviceSwitch: Switch
    private lateinit var nowRotationSwitch: Switch
    private lateinit var nowNfcSwitch: Switch
    private lateinit var nowDndSwitch: Switch
    private lateinit var nowRingerSwitch: Switch
    private lateinit var nowBluetoothSwitch: Switch
    private lateinit var nowBrightnessSwitch: Switch
    private lateinit var nowVolumeSwitch: Switch
    private lateinit var nowPowerSwitch: Switch
    private lateinit var gnLayoutSwitch: Switch
    private lateinit var wallpaperParallaxSwitch: Switch
    private lateinit var pageAnimationSpinner: Spinner
    private lateinit var pageAnimIntensitySeek: android.widget.SeekBar
    private lateinit var pageAnimIntensityValue: TextView
    private lateinit var assistantSpinner: Spinner
    private lateinit var voiceCommandHelpButton: Button
    private lateinit var dockVisibleSwitch: Switch
    private lateinit var smartHotseatSwitch: Switch
    private lateinit var themeGroup: RadioGroup
    private lateinit var invertColorsSwitch: Switch
    private lateinit var themePresetSpinner: Spinner
    private lateinit var themeOverrideSwitch: Switch
    private lateinit var themeBgInput: EditText
    private lateinit var themeTextInput: EditText
    private lateinit var themeMutedInput: EditText
    private lateinit var themeAccentInput: EditText
    private lateinit var themeAccent2Input: EditText
    private lateinit var iconStyleGroup: RadioGroup
    private lateinit var iconTintSpinner: Spinner
    private lateinit var labelStyleSpinner: Spinner
    private lateinit var labelBackgroundSwitch: Switch
    private lateinit var iconPackSwitch: Switch
    private lateinit var iconPackButton: Button
    private lateinit var iconPackPath: TextView
    private lateinit var widgetSnapSwitch: Switch
    private lateinit var widgetStepSpinner: Spinner
    private lateinit var widgetSizeSwitch: Switch
    private lateinit var wallpaperButton: Button
    private lateinit var closeButton: Button
    private lateinit var soundFeedbackSwitch: Switch
    private lateinit var soundVolumeSeek: android.widget.SeekBar
    private lateinit var soundVolumeValue: TextView
    private lateinit var soundSchemeSpinner: Spinner
    private lateinit var hapticFeedbackSwitch: Switch
    private lateinit var hapticStrengthSpinner: Spinner
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
        pageCountSpinner = findViewById(R.id.pageCountSpinner)
        iconSizeGroup = findViewById(R.id.iconSizeGroup)
        labelSizeGroup = findViewById(R.id.labelSizeGroup)
        doubleTapSwitch = findViewById(R.id.doubleTapLockSwitch)
        homeEditLockSwitch = findViewById(R.id.homeEditLockSwitch)
        hideHomeLabelsSwitch = findViewById(R.id.hideHomeLabelsSwitch)
        showPageIndicatorSwitch = findViewById(R.id.showPageIndicatorSwitch)
        defaultHomePageSpinner = findViewById(R.id.defaultHomePageSpinner)
        resetLayoutButton = findViewById(R.id.resetLayoutButton)
        clearFavoritesButton = findViewById(R.id.clearFavoritesButton)
        clearHiddenButton = findViewById(R.id.clearHiddenButton)
        clearCustomButton = findViewById(R.id.clearCustomButton)
        hideDockLabelsSwitch = findViewById(R.id.hideDockLabelsSwitch)
        superSimpleSwitch = findViewById(R.id.superSimpleSwitch)
        superSimpleGridSpinner = findViewById(R.id.superSimpleGridSpinner)
        simpleFavoritesGridSpinner = findViewById(R.id.simpleFavoritesGridSpinner)
        feedEnabledSwitch = findViewById(R.id.feedEnabledSwitch)
        feedModeSpinner = findViewById(R.id.feedModeSpinner)
        feedAutoOpenSwitch = findViewById(R.id.feedAutoOpenSwitch)
        feedQuickActionsSwitch = findViewById(R.id.feedQuickActionsSwitch)
        searchBarSwitch = findViewById(R.id.searchBarSwitch)
        googleSearchSwitch = findViewById(R.id.googleSearchSwitch)
        googleVoiceSwitch = findViewById(R.id.googleVoiceSwitch)
        allAppsLabelsSwitch = findViewById(R.id.allAppsLabelsSwitch)
        showFavoritesSectionSwitch = findViewById(R.id.showFavoritesSectionSwitch)
        showSuggestedNowSectionSwitch = findViewById(R.id.showSuggestedNowSectionSwitch)
        showSuggestedSectionSwitch = findViewById(R.id.showSuggestedSectionSwitch)
        showRecentSectionSwitch = findViewById(R.id.showRecentSectionSwitch)
        showCategoriesRowSwitch = findViewById(R.id.showCategoriesRowSwitch)
        showScrollButtonsSwitch = findViewById(R.id.showScrollButtonsSwitch)
        showVoiceSearchSwitch = findViewById(R.id.showVoiceSearchSwitch)
        showResultsCountSwitch = findViewById(R.id.showResultsCountSwitch)
        showSystemAppsSwitch = findViewById(R.id.showSystemAppsSwitch)
        sortDescendingSwitch = findViewById(R.id.sortDescendingSwitch)
        suggestionModeSpinner = findViewById(R.id.suggestionModeSpinner)
        allAppsColumnsSpinner = findViewById(R.id.allAppsColumnsSpinner)
        allAppsDefaultTabSpinner = findViewById(R.id.allAppsDefaultTabSpinner)
        favoritesCountSpinner = findViewById(R.id.favoritesCountSpinner)
        suggestedNowCountSpinner = findViewById(R.id.suggestedNowCountSpinner)
        suggestedCountSpinner = findViewById(R.id.suggestedCountSpinner)
        recentCountSpinner = findViewById(R.id.recentCountSpinner)
        categoryRankSpinner = findViewById(R.id.categoryRankSpinner)
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
        nowDeviceSwitch = findViewById(R.id.nowDeviceSwitch)
        nowRotationSwitch = findViewById(R.id.nowRotationSwitch)
        nowNfcSwitch = findViewById(R.id.nowNfcSwitch)
        nowDndSwitch = findViewById(R.id.nowDndSwitch)
        nowRingerSwitch = findViewById(R.id.nowRingerSwitch)
        nowBluetoothSwitch = findViewById(R.id.nowBluetoothSwitch)
        nowBrightnessSwitch = findViewById(R.id.nowBrightnessSwitch)
        nowVolumeSwitch = findViewById(R.id.nowVolumeSwitch)
        nowPowerSwitch = findViewById(R.id.nowPowerSwitch)
        gnLayoutSwitch = findViewById(R.id.gnLayoutSwitch)
        wallpaperParallaxSwitch = findViewById(R.id.wallpaperParallaxSwitch)
        pageAnimationSpinner = findViewById(R.id.pageAnimationSpinner)
        pageAnimIntensitySeek = findViewById(R.id.pageAnimIntensitySeek)
        pageAnimIntensityValue = findViewById(R.id.pageAnimIntensityValue)
        assistantSpinner = findViewById(R.id.assistantSpinner)
        voiceCommandHelpButton = findViewById(R.id.voiceCommandHelpButton)
        dockVisibleSwitch = findViewById(R.id.dockVisibleSwitch)
        smartHotseatSwitch = findViewById(R.id.smartHotseatSwitch)
        themeGroup = findViewById(R.id.themeGroup)
        invertColorsSwitch = findViewById(R.id.invertColorsSwitch)
        themePresetSpinner = findViewById(R.id.themePresetSpinner)
        themeOverrideSwitch = findViewById(R.id.themeOverrideSwitch)
        themeBgInput = findViewById(R.id.themeBgInput)
        themeTextInput = findViewById(R.id.themeTextInput)
        themeMutedInput = findViewById(R.id.themeMutedInput)
        themeAccentInput = findViewById(R.id.themeAccentInput)
        themeAccent2Input = findViewById(R.id.themeAccent2Input)
        iconStyleGroup = findViewById(R.id.iconStyleGroup)
        iconTintSpinner = findViewById(R.id.iconTintSpinner)
        labelStyleSpinner = findViewById(R.id.labelStyleSpinner)
        labelBackgroundSwitch = findViewById(R.id.labelBackgroundSwitch)
        iconPackSwitch = findViewById(R.id.iconPackSwitch)
        iconPackButton = findViewById(R.id.iconPackButton)
        iconPackPath = findViewById(R.id.iconPackPath)
        widgetSnapSwitch = findViewById(R.id.widgetSnapSwitch)
        widgetStepSpinner = findViewById(R.id.widgetStepSpinner)
        widgetSizeSwitch = findViewById(R.id.widgetSizeSwitch)
        wallpaperButton = findViewById(R.id.wallpaperButton)
        closeButton = findViewById(R.id.closeSettingsButton)
        soundFeedbackSwitch = findViewById(R.id.soundFeedbackSwitch)
        soundVolumeSeek = findViewById(R.id.soundVolumeSeek)
        soundVolumeValue = findViewById(R.id.soundVolumeValue)
        soundSchemeSpinner = findViewById(R.id.soundSchemeSpinner)
        hapticFeedbackSwitch = findViewById(R.id.hapticFeedbackSwitch)
        hapticStrengthSpinner = findViewById(R.id.hapticStrengthSpinner)
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
        applyThemeColors()

        closeButton.setOnClickListener { finish() }
        voiceCommandHelpButton.setOnClickListener { showVoiceCommandsHelp() }
    }

    override fun onResume() {
        super.onResume()
        applyThemeColors()
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
        bindPageCount()
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
        homeEditLockSwitch.isChecked = LauncherPrefs.isHomeEditLocked(this)
        hideHomeLabelsSwitch.isChecked = LauncherPrefs.isHomeLabelsHidden(this)
        showPageIndicatorSwitch.isChecked = LauncherPrefs.isPageIndicatorShown(this)
        bindDefaultHomePage()
        hideDockLabelsSwitch.isChecked = LauncherPrefs.isDockLabelsHidden(this)
        superSimpleSwitch.isChecked = LauncherPrefs.isSuperSimpleEnabled(this)
        bindSuperSimpleGrid()
        bindSimpleFavoritesGrid()
        feedEnabledSwitch.isChecked = LauncherPrefs.isFeedEnabled(this)
        bindFeedModeSpinner(LauncherPrefs.getFeedMode(this))
        feedAutoOpenSwitch.isChecked = LauncherPrefs.isFeedAutoOpenEnabled(this)
        feedQuickActionsSwitch.isChecked = LauncherPrefs.isFeedQuickActionsEnabled(this)
        searchBarSwitch.isChecked = LauncherPrefs.isSearchBarEnabled(this)
        googleSearchSwitch.isChecked = LauncherPrefs.isGoogleSearchEnabled(this)
        googleVoiceSwitch.isChecked = LauncherPrefs.isGoogleVoiceEnabled(this)
        allAppsLabelsSwitch.isChecked = LauncherPrefs.isAllAppsLabelsShown(this)
        showFavoritesSectionSwitch.isChecked = LauncherPrefs.isFavoritesSectionShown(this)
        showSuggestedNowSectionSwitch.isChecked = LauncherPrefs.isSuggestedNowSectionShown(this)
        showSuggestedSectionSwitch.isChecked = LauncherPrefs.isSuggestedSectionShown(this)
        showRecentSectionSwitch.isChecked = LauncherPrefs.isRecentSectionShown(this)
        showCategoriesRowSwitch.isChecked = LauncherPrefs.isCategoriesRowShown(this)
        showScrollButtonsSwitch.isChecked = LauncherPrefs.isScrollButtonsShown(this)
        showVoiceSearchSwitch.isChecked = LauncherPrefs.isVoiceSearchShown(this)
        showResultsCountSwitch.isChecked = LauncherPrefs.isResultsCountShown(this)
        showSystemAppsSwitch.isChecked = LauncherPrefs.isShowSystemApps(this)
        sortDescendingSwitch.isChecked = LauncherPrefs.isSortDescending(this)
        bindSuggestionMode()
        bindAllAppsColumns()
        bindAllAppsDefaultTab()
        bindAllAppsCounts()
        bindCategoryRanking()
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
        nowDeviceSwitch.isChecked = LauncherPrefs.isNowDeviceEnabled(this)
        nowRotationSwitch.isChecked = LauncherPrefs.isNowRotationEnabled(this)
        nowNfcSwitch.isChecked = LauncherPrefs.isNowNfcEnabled(this)
        nowDndSwitch.isChecked = LauncherPrefs.isNowDndEnabled(this)
        nowRingerSwitch.isChecked = LauncherPrefs.isNowRingerEnabled(this)
        nowBluetoothSwitch.isChecked = LauncherPrefs.isNowBluetoothEnabled(this)
        nowBrightnessSwitch.isChecked = LauncherPrefs.isNowBrightnessEnabled(this)
        nowVolumeSwitch.isChecked = LauncherPrefs.isNowVolumeEnabled(this)
        nowPowerSwitch.isChecked = LauncherPrefs.isNowPowerEnabled(this)
        gnLayoutSwitch.isChecked = LauncherPrefs.isGnLayoutEnabled(this)
        wallpaperParallaxSwitch.isChecked = LauncherPrefs.isWallpaperParallaxEnabled(this)
        bindPageAnimation()
        bindPageAnimIntensity()
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
        bindThemePreset()
        bindThemeOverride()
        invertColorsSwitch.isChecked = LauncherPrefs.isInvertColorsEnabled(this)
        when (LauncherPrefs.getIconStyle(this)) {
            1 -> iconStyleGroup.check(R.id.iconStyleCircle)
            else -> iconStyleGroup.check(R.id.iconStyleNone)
        }
        bindIconTint()
        bindLabelStyle()
        labelBackgroundSwitch.isChecked = LauncherPrefs.isLabelBackgroundEnabled(this)
        bindIconPack()
        soundFeedbackSwitch.isChecked = LauncherPrefs.isSoundFeedbackEnabled(this)
        bindSoundVolume()
        bindSoundScheme()
        hapticFeedbackSwitch.isChecked = LauncherPrefs.isHapticFeedbackEnabled(this)
        bindHapticStrength()
        bindGestureSpinners()
        usageStatsSwitch.isChecked = LauncherPrefs.isUsageSuggestionsEnabled(this)
        widgetSnapSwitch.isChecked = LauncherPrefs.isWidgetSnapEnabled(this)
        widgetSizeSwitch.isChecked = LauncherPrefs.isWidgetSizeShown(this)
        bindWidgetStep()
        applySuperSimpleState(superSimpleSwitch.isChecked)
        applyLayoutModeState()
        feedModeSpinner.isEnabled = feedEnabledSwitch.isChecked && !superSimpleSwitch.isChecked
        feedQuickActionsSwitch.isEnabled = feedEnabledSwitch.isChecked && !superSimpleSwitch.isChecked
        googleSearchSwitch.isEnabled = isGoogleAppAvailable()
        googleVoiceSwitch.isEnabled = isGoogleAppAvailable()
        updateFeedAutoOpenState()
    }

    private fun bindPageCount() {
        val labels = (1..5).map { it.toString() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pageCountSpinner.adapter = adapter
        val current = LauncherPrefs.getPageCount(this).coerceIn(1, 5)
        pageCountSpinner.setSelection(current - 1)
        updatePageDependentState(current)
    }

    private fun bindAllAppsColumns() {
        val labels = mutableListOf(getString(R.string.launcher_all_apps_columns_auto))
        labels.addAll(listOf("3", "4", "5", "6"))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        allAppsColumnsSpinner.adapter = adapter
        val current = LauncherPrefs.getAllAppsColumns(this)
        val index = if (current <= 0) 0 else (current - 2).coerceIn(1, labels.size - 1)
        allAppsColumnsSpinner.setSelection(index, false)
        allAppsColumnsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = if (position == 0) 0 else (position + 2)
                LauncherPrefs.setAllAppsColumns(this@LauncherSettingsActivity, value)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindAllAppsDefaultTab() {
        val labels = listOf(getString(R.string.launcher_tab_apps), getString(R.string.launcher_tab_widgets))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        allAppsDefaultTabSpinner.adapter = adapter
        allAppsDefaultTabSpinner.setSelection(LauncherPrefs.getAllAppsDefaultTab(this), false)
        allAppsDefaultTabSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setAllAppsDefaultTab(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindAllAppsCounts() {
        val labels = listOf("4", "6", "8", "12", getString(R.string.launcher_count_all))
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        favoritesCountSpinner.adapter = adapter
        suggestedNowCountSpinner.adapter = adapter
        suggestedCountSpinner.adapter = adapter
        recentCountSpinner.adapter = adapter

        fun selectionFor(value: Int): Int {
            return when (value) {
                4 -> 0
                6 -> 1
                8 -> 2
                12 -> 3
                else -> 4
            }
        }

        favoritesCountSpinner.setSelection(selectionFor(LauncherPrefs.getFavoritesCount(this)), false)
        suggestedNowCountSpinner.setSelection(selectionFor(LauncherPrefs.getSuggestedNowCount(this)), false)
        suggestedCountSpinner.setSelection(selectionFor(LauncherPrefs.getSuggestedCount(this)), false)
        recentCountSpinner.setSelection(selectionFor(LauncherPrefs.getRecentCount(this)), false)

        fun valueFor(position: Int): Int {
            return when (position) {
                0 -> 4
                1 -> 6
                2 -> 8
                3 -> 12
                else -> 0
            }
        }

        favoritesCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setFavoritesCount(this@LauncherSettingsActivity, valueFor(position))
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        suggestedNowCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setSuggestedNowCount(this@LauncherSettingsActivity, valueFor(position))
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        suggestedCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setSuggestedCount(this@LauncherSettingsActivity, valueFor(position))
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        recentCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setRecentCount(this@LauncherSettingsActivity, valueFor(position))
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindCategoryRanking() {
        val labels = listOf(
            getString(R.string.launcher_category_rank_count),
            getString(R.string.launcher_category_rank_usage),
            getString(R.string.launcher_category_rank_alpha)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryRankSpinner.adapter = adapter
        categoryRankSpinner.setSelection(LauncherPrefs.getCategoryRanking(this), false)
        categoryRankSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setCategoryRanking(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindSuggestionMode() {
        val labels = listOf(
            getString(R.string.launcher_suggestion_balanced),
            getString(R.string.launcher_suggestion_recent),
            getString(R.string.launcher_suggestion_frequent),
            getString(R.string.launcher_suggestion_time)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        suggestionModeSpinner.adapter = adapter
        suggestionModeSpinner.setSelection(LauncherPrefs.getSuggestionMode(this), false)
        suggestionModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setSuggestionMode(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindDefaultHomePage(maxPages: Int = LauncherPrefs.getPageCount(this)) {
        val boundedMax = maxPages.coerceAtLeast(1)
        val labels = (1..boundedMax).map { getString(R.string.launcher_default_home_page_label, it) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        defaultHomePageSpinner.adapter = adapter
        val current = LauncherPrefs.getDefaultHomePage(this).coerceIn(1, boundedMax)
        if (current != LauncherPrefs.getDefaultHomePage(this)) {
            LauncherPrefs.setDefaultHomePage(this, current)
        }
        defaultHomePageSpinner.setSelection(current - 1, false)
        defaultHomePageSpinner.isEnabled = !superSimpleSwitch.isChecked && boundedMax > 1
        defaultHomePageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setDefaultHomePage(this@LauncherSettingsActivity, position + 1)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
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

        pageCountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = position + 1
                LauncherPrefs.setPageCount(this@LauncherSettingsActivity, value)
                bindDefaultHomePage(value)
                updatePageDependentState(value)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
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

        homeEditLockSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setHomeEditLocked(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) R.string.launcher_edit_locked else R.string.launcher_edit_unlocked,
                Toast.LENGTH_SHORT
            ).show()
        }

        hideHomeLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setHomeLabelsHidden(this, isChecked)
            toastSaved()
        }

        showPageIndicatorSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setPageIndicatorShown(this, isChecked)
            toastSaved()
        }

        hideDockLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setDockLabelsHidden(this, isChecked)
            toastSaved()
        }

        labelBackgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setLabelBackgroundEnabled(this, isChecked)
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
            updatePageDependentState(LauncherPrefs.getPageCount(this))
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

        feedQuickActionsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setFeedQuickActionsEnabled(this, isChecked)
            toastSaved()
        }

        searchBarSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSearchBarEnabled(this, isChecked)
            toastSaved()
        }

        allAppsLabelsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setAllAppsLabelsShown(this, isChecked)
            toastSaved()
        }

        showFavoritesSectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setFavoritesSectionShown(this, isChecked)
            toastSaved()
        }

        showSuggestedNowSectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSuggestedNowSectionShown(this, isChecked)
            toastSaved()
        }

        showSuggestedSectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSuggestedSectionShown(this, isChecked)
            toastSaved()
        }

        showRecentSectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setRecentSectionShown(this, isChecked)
            toastSaved()
        }

        showCategoriesRowSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setCategoriesRowShown(this, isChecked)
            toastSaved()
        }

        showScrollButtonsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setScrollButtonsShown(this, isChecked)
            toastSaved()
        }

        showVoiceSearchSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setVoiceSearchShown(this, isChecked)
            toastSaved()
        }

        showResultsCountSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setResultsCountShown(this, isChecked)
            toastSaved()
        }

        showSystemAppsSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setShowSystemApps(this, isChecked)
            toastSaved()
        }

        sortDescendingSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSortDescending(this, isChecked)
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

        nowDeviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowDeviceEnabled(this, isChecked)
            toastSaved()
        }

        nowRotationSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowRotationEnabled(this, isChecked)
            toastSaved()
        }

        nowNfcSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setNowNfcEnabled(this, isChecked)
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

        hapticFeedbackSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setHapticFeedbackEnabled(this, isChecked)
            hapticStrengthSpinner.isEnabled = isChecked
            toastSaved()
        }

        hapticStrengthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setHapticStrength(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        widgetSnapSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setWidgetSnapEnabled(this, isChecked)
            toastSaved()
        }

        widgetSizeSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setWidgetSizeShown(this, isChecked)
            toastSaved()
        }

        backupButton.setOnClickListener { backupLauncher() }
        restoreButton.setOnClickListener { restoreLauncher() }

        resetLayoutButton.setOnClickListener { confirmResetLayout() }
        clearFavoritesButton.setOnClickListener {
            LauncherStore.clearFavorites(this)
            Toast.makeText(this, R.string.launcher_clear_favorites_done, Toast.LENGTH_SHORT).show()
        }
        clearHiddenButton.setOnClickListener {
            LauncherStore.clearHiddenApps(this)
            Toast.makeText(this, R.string.launcher_clear_hidden_done, Toast.LENGTH_SHORT).show()
        }
        clearCustomButton.setOnClickListener {
            LauncherStore.clearCustomizations(this)
            Toast.makeText(this, R.string.launcher_clear_customizations_done, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindPageAnimation() {
        val labels = listOf(
            getString(R.string.launcher_page_anim_default),
            getString(R.string.launcher_page_anim_carousel),
            getString(R.string.launcher_page_anim_depth),
            getString(R.string.launcher_page_anim_stack),
            getString(R.string.launcher_page_anim_zoom),
            getString(R.string.launcher_page_anim_flip),
            getString(R.string.launcher_page_anim_cube),
            getString(R.string.launcher_page_anim_parallax)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        pageAnimationSpinner.adapter = adapter
        pageAnimationSpinner.setSelection(LauncherPrefs.getPageAnimation(this))
    }

    private fun bindPageAnimIntensity() {
        val current = LauncherPrefs.getPageAnimationIntensity(this)
        pageAnimIntensitySeek.max = 100
        pageAnimIntensitySeek.progress = current
        pageAnimIntensityValue.text = "${current}%"
        pageAnimIntensitySeek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                LauncherPrefs.setPageAnimationIntensity(this@LauncherSettingsActivity, progress)
                pageAnimIntensityValue.text = "${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                toastSaved()
            }
        })
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
        feedQuickActionsSwitch.isEnabled = !enabled && feedEnabledSwitch.isChecked
        searchBarSwitch.isEnabled = !enabled
        googleSearchSwitch.isEnabled = !enabled && isGoogleAppAvailable()
        googleVoiceSwitch.isEnabled = !enabled && isGoogleAppAvailable()
        dockVisibleSwitch.isEnabled = !enabled
        hideDockLabelsSwitch.isEnabled = !enabled
        hideHomeLabelsSwitch.isEnabled = !enabled
        showPageIndicatorSwitch.isEnabled = !enabled
        defaultHomePageSpinner.isEnabled = !enabled
        setGroupEnabled(themeGroup, !enabled)
        setGroupEnabled(iconStyleGroup, !enabled)
        invertColorsSwitch.isEnabled = !enabled
        gnLayoutSwitch.isEnabled = !enabled
        wallpaperParallaxSwitch.isEnabled = !enabled
        superSimpleGridSpinner.isEnabled = enabled
        simpleFavoritesGridSpinner.isEnabled = enabled
        pageAnimIntensitySeek.isEnabled = !enabled
        iconTintSpinner.isEnabled = !enabled
        labelStyleSpinner.isEnabled = !enabled
        labelBackgroundSwitch.isEnabled = !enabled
        themePresetSpinner.isEnabled = !enabled
        themeOverrideSwitch.isEnabled = !enabled
        themeBgInput.isEnabled = !enabled && themeOverrideSwitch.isChecked
        themeTextInput.isEnabled = !enabled && themeOverrideSwitch.isChecked
        themeMutedInput.isEnabled = !enabled && themeOverrideSwitch.isChecked
        themeAccentInput.isEnabled = !enabled && themeOverrideSwitch.isChecked
        themeAccent2Input.isEnabled = !enabled && themeOverrideSwitch.isChecked
        iconPackSwitch.isEnabled = !enabled
        iconPackButton.isEnabled = !enabled
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

    private fun applyThemeColors() {
        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(colors.background)
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        applyThemeRecursive(root, colors)
    }

    private fun applyThemeRecursive(view: android.view.View, colors: LauncherPrefs.ThemeColors) {
        when (view) {
            is Button -> {
                val useAlt = view.id % 2 != 0
                ThemeUtils.tintButton(view, colors, useAlt)
            }
            is Switch -> ThemeUtils.tintSwitch(view, colors)
            is androidx.appcompat.widget.SwitchCompat -> ThemeUtils.tintSwitchCompat(view, colors)
            is android.widget.RadioButton -> {
                view.setTextColor(colors.text)
                view.buttonTintList = android.content.res.ColorStateList.valueOf(colors.accent)
            }
            is android.widget.CheckBox -> {
                view.setTextColor(colors.text)
                view.buttonTintList = android.content.res.ColorStateList.valueOf(colors.accent)
            }
            is android.widget.Spinner -> ThemeUtils.tintSpinner(view, colors)
            is TextView -> view.setTextColor(colors.text)
        }
        if (view is EditText) {
            ThemeUtils.tintEditText(view, colors)
        }
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                applyThemeRecursive(view.getChildAt(i), colors)
            }
        }
    }

    private fun updateFeedAutoOpenState() {
        val feedEnabled = feedEnabledSwitch.isChecked && !superSimpleSwitch.isChecked
        val googleMode = feedModeSpinner.selectedItemPosition == LauncherPrefs.FEED_MODE_GOOGLE
        feedAutoOpenSwitch.isEnabled = feedEnabled && googleMode
        feedQuickActionsSwitch.isEnabled = feedEnabled
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

    private fun bindHapticStrength() {
        val labels = listOf(
            getString(R.string.launcher_haptic_light),
            getString(R.string.launcher_haptic_medium),
            getString(R.string.launcher_haptic_strong)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        hapticStrengthSpinner.adapter = adapter
        hapticStrengthSpinner.setSelection(LauncherPrefs.getHapticStrength(this).coerceIn(0, 2), false)
        hapticStrengthSpinner.isEnabled = hapticFeedbackSwitch.isChecked
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

    private fun showVoiceCommandsHelp() {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_voice_commands_title)
            .setMessage(R.string.launcher_voice_commands_body)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun bindIconTint() {
        val labels = listOf(
            getString(R.string.launcher_icon_tint_none),
            getString(R.string.launcher_icon_tint_gray),
            getString(R.string.launcher_icon_tint_contrast)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        iconTintSpinner.adapter = adapter
        iconTintSpinner.setSelection(LauncherPrefs.getIconTint(this), false)
        iconTintSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setIconTint(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindLabelStyle() {
        val labels = listOf(
            getString(R.string.launcher_label_style_normal),
            getString(R.string.launcher_label_style_upper)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        labelStyleSpinner.adapter = adapter
        labelStyleSpinner.setSelection(LauncherPrefs.getLabelStyle(this), false)
        labelStyleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setLabelStyle(this@LauncherSettingsActivity, position)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindIconPack() {
        iconPackSwitch.isChecked = LauncherPrefs.isIconPackEnabled(this)
        updateIconPackSummary()
        iconPackSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherStore.setIconPack(this, isChecked, LauncherPrefs.getIconPackUri(this))
            updateIconPackSummary()
            toastSaved()
        }
        iconPackButton.setOnClickListener {
            pickIconPackFolder.launch(null)
        }
    }

    private fun updateIconPackSummary() {
        val uri = LauncherPrefs.getIconPackUri(this)
        if (uri.isNullOrBlank()) {
            iconPackPath.text = getString(R.string.launcher_settings_icon_pack_none)
            return
        }
        val name = try {
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(this, android.net.Uri.parse(uri))
            doc?.name
        } catch (_: Exception) {
            null
        }
        iconPackPath.text = name ?: uri
    }

    private fun bindThemePreset() {
        val labels = listOf(
            getString(R.string.launcher_theme_preset_default),
            getString(R.string.launcher_theme_preset_contrast),
            getString(R.string.launcher_theme_preset_soft),
            getString(R.string.launcher_theme_preset_solar)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themePresetSpinner.adapter = adapter
        themePresetSpinner.setSelection(LauncherPrefs.getThemePreset(this), false)
        themePresetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                LauncherPrefs.setThemePreset(this@LauncherSettingsActivity, position)
                refreshThemeInputs()
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun bindThemeOverride() {
        themeOverrideSwitch.isChecked = LauncherPrefs.isThemeOverrideEnabled(this)
        refreshThemeInputs()
        themeOverrideSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setThemeOverrideEnabled(this, isChecked)
            refreshThemeInputs()
            toastSaved()
        }
        bindThemeInput(themeBgInput, LauncherPrefs.KEY_THEME_BG)
        bindThemeInput(themeTextInput, LauncherPrefs.KEY_THEME_TEXT)
        bindThemeInput(themeMutedInput, LauncherPrefs.KEY_THEME_MUTED)
        bindThemeInput(themeAccentInput, LauncherPrefs.KEY_THEME_ACCENT)
        bindThemeInput(themeAccent2Input, LauncherPrefs.KEY_THEME_ACCENT2)
    }

    private fun bindThemeInput(input: EditText, key: String) {
        val colors = LauncherPrefs.getThemeColors(this)
        val fallback = when (key) {
            LauncherPrefs.KEY_THEME_BG -> colors.background
            LauncherPrefs.KEY_THEME_TEXT -> colors.text
            LauncherPrefs.KEY_THEME_MUTED -> colors.muted
            LauncherPrefs.KEY_THEME_ACCENT -> colors.accent
            else -> colors.accentAlt
        }
        input.setText(LauncherPrefs.getThemeColorHex(this, key, fallback))
        input.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                val value = s?.toString().orEmpty()
                LauncherPrefs.setThemeColorHex(this@LauncherSettingsActivity, key, value)
            }
        })
        input.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) toastSaved()
        }
    }

    private fun refreshThemeInputs() {
        val enabled = themeOverrideSwitch.isChecked
        themeBgInput.isEnabled = enabled
        themeTextInput.isEnabled = enabled
        themeMutedInput.isEnabled = enabled
        themeAccentInput.isEnabled = enabled
        themeAccent2Input.isEnabled = enabled
        val colors = LauncherPrefs.getThemeColors(this)
        updateThemeInput(themeBgInput, LauncherPrefs.KEY_THEME_BG, colors.background)
        updateThemeInput(themeTextInput, LauncherPrefs.KEY_THEME_TEXT, colors.text)
        updateThemeInput(themeMutedInput, LauncherPrefs.KEY_THEME_MUTED, colors.muted)
        updateThemeInput(themeAccentInput, LauncherPrefs.KEY_THEME_ACCENT, colors.accent)
        updateThemeInput(themeAccent2Input, LauncherPrefs.KEY_THEME_ACCENT2, colors.accentAlt)
    }

    private fun updateThemeInput(input: EditText, key: String, fallback: Int) {
        val value = LauncherPrefs.getThemeColorHex(this, key, fallback)
        if (input.text?.toString() != value) {
            input.setText(value)
        }
    }

    private fun bindWidgetStep() {
        val labels = listOf(
            getString(R.string.launcher_widget_step_10),
            getString(R.string.launcher_widget_step_20),
            getString(R.string.launcher_widget_step_30),
            getString(R.string.launcher_widget_step_40)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        widgetStepSpinner.adapter = adapter
        val current = LauncherPrefs.getWidgetResizeStepDp(this)
        val index = when (current) {
            10 -> 0
            20 -> 1
            30 -> 2
            else -> 3
        }
        widgetStepSpinner.setSelection(index, false)
        widgetStepSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = when (position) {
                    0 -> 10
                    1 -> 20
                    2 -> 30
                    else -> 40
                }
                LauncherPrefs.setWidgetResizeStepDp(this@LauncherSettingsActivity, value)
                toastSaved()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun confirmResetLayout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_settings_reset_layout)
            .setMessage(R.string.launcher_reset_layout_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                LauncherStore.resetLayout(this)
                Toast.makeText(this, R.string.launcher_reset_layout_done, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updatePageDependentState(pageCount: Int) {
        val multiple = pageCount > 1
        showPageIndicatorSwitch.isEnabled = !superSimpleSwitch.isChecked && multiple
        if (!multiple && showPageIndicatorSwitch.isChecked) {
            showPageIndicatorSwitch.isChecked = false
            LauncherPrefs.setPageIndicatorShown(this, false)
        }
        defaultHomePageSpinner.isEnabled = !superSimpleSwitch.isChecked && multiple
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

    private val pickIconPackFolder = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Ignore
        }
        LauncherStore.setIconPack(this, iconPackSwitch.isChecked, uri.toString())
        updateIconPackSummary()
        toastSaved()
    }

    private fun backupLauncher() {
        backupLauncherFile.launch("blindroid-launcher-backup.json")
    }

    private fun restoreLauncher() {
        restoreLauncherFile.launch(arrayOf("application/json"))
    }
}
