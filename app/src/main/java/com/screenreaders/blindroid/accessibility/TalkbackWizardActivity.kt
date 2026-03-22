package com.screenreaders.blindroid.accessibility

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.databinding.ActivityTalkbackWizardBinding

class TalkbackWizardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTalkbackWizardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkbackWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.talkbackSettingsButton.setOnClickListener {
            openAccessibilitySettings()
        }
        binding.talkbackShortcutButton.setOnClickListener {
            openAccessibilityShortcutSettings()
        }
        binding.backupSettingsButton.setOnClickListener {
            openBackupTalkBackSettings()
        }
        binding.talkbackRefreshButton.setOnClickListener {
            updateStatus()
        }
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = TalkbackUtils.isTalkBackEnabled(this)
        binding.talkbackStatus.text = getString(
            if (enabled) R.string.talkback_status_on else R.string.talkback_status_off
        )
        val backupInstalled = TalkbackUtils.isBackupTalkBackInstalled(this)
        val backupEnabled = backupInstalled && TalkbackUtils.isBackupTalkBackEnabled(this)
        val backupStatusRes = when {
            !backupInstalled -> R.string.talkback_backup_status_missing
            backupEnabled -> R.string.talkback_backup_status_on
            else -> R.string.talkback_backup_status_off
        }
        binding.backupStatus.text = getString(backupStatusRes)
        binding.backupSettingsButton.isEnabled = backupInstalled
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAccessibilityShortcutSettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SHORTCUT_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openAccessibilitySettings()
        }
    }

    private fun openBackupTalkBackSettings() {
        val component = TalkbackUtils.getBackupTalkBackComponent()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(Settings.EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME, component.flattenToString())
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openAccessibilitySettings()
        }
    }
}
