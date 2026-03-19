package com.screenreaders.blindroid.launcher

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
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher_settings)

        columnsGroup = findViewById(R.id.columnsGroup)
        rowsGroup = findViewById(R.id.rowsGroup)
        iconSizeGroup = findViewById(R.id.iconSizeGroup)
        labelSizeGroup = findViewById(R.id.labelSizeGroup)
        doubleTapSwitch = findViewById(R.id.doubleTapLockSwitch)
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
    }

    private fun toastSaved() {
        Toast.makeText(this, R.string.launcher_settings_saved, Toast.LENGTH_SHORT).show()
    }
}
