package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Switch
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
    private lateinit var feedEnabledSwitch: Switch
    private lateinit var dockVisibleSwitch: Switch
    private lateinit var themeGroup: RadioGroup
    private lateinit var iconStyleGroup: RadioGroup
    private lateinit var wallpaperButton: Button
    private lateinit var closeButton: Button

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
        feedEnabledSwitch = findViewById(R.id.feedEnabledSwitch)
        dockVisibleSwitch = findViewById(R.id.dockVisibleSwitch)
        themeGroup = findViewById(R.id.themeGroup)
        iconStyleGroup = findViewById(R.id.iconStyleGroup)
        wallpaperButton = findViewById(R.id.wallpaperButton)
        closeButton = findViewById(R.id.closeSettingsButton)

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
        feedEnabledSwitch.isChecked = LauncherPrefs.isFeedEnabled(this)
        dockVisibleSwitch.isChecked = LauncherPrefs.isDockVisible(this)
        when (LauncherPrefs.getTheme(this)) {
            1 -> themeGroup.check(R.id.themeDark)
            2 -> themeGroup.check(R.id.themeBlack)
            3 -> themeGroup.check(R.id.themeBlue)
            else -> themeGroup.check(R.id.themeLight)
        }
        when (LauncherPrefs.getIconStyle(this)) {
            1 -> iconStyleGroup.check(R.id.iconStyleCircle)
            else -> iconStyleGroup.check(R.id.iconStyleNone)
        }
        applySuperSimpleState(superSimpleSwitch.isChecked)
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

        superSimpleSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setSuperSimpleEnabled(this, isChecked)
            applySuperSimpleState(isChecked)
            toastSaved()
        }

        feedEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            LauncherPrefs.setFeedEnabled(this, isChecked)
            toastSaved()
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
                else -> 0
            }
            LauncherPrefs.setTheme(this, theme)
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
    }

    private fun applySuperSimpleState(enabled: Boolean) {
        setGroupEnabled(columnsGroup, !enabled)
        setGroupEnabled(rowsGroup, !enabled)
        setGroupEnabled(iconSizeGroup, !enabled)
        setGroupEnabled(labelSizeGroup, !enabled)
        feedEnabledSwitch.isEnabled = !enabled
        dockVisibleSwitch.isEnabled = !enabled
        hideDockLabelsSwitch.isEnabled = !enabled
        setGroupEnabled(themeGroup, !enabled)
        setGroupEnabled(iconStyleGroup, !enabled)
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
}
