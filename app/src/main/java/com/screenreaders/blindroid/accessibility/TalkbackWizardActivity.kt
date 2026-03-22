package com.screenreaders.blindroid.accessibility

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.databinding.ActivityTalkbackWizardBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
        binding.backupGesturesButton.setOnClickListener {
            openBackupGestureSettings()
        }
        binding.backupInstallButton.setOnClickListener {
            installBackupTalkBack()
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
        binding.backupGesturesButton.isEnabled = backupInstalled
        binding.backupInstallButton.isEnabled = !backupInstalled && isBackupInstallerAvailable()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAccessibilityShortcutSettings() {
        val intent = Intent(ACTION_ACCESSIBILITY_SHORTCUT_SETTINGS)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openAccessibilitySettings()
        }
    }

    private fun openBackupTalkBackSettings() {
        val component = TalkbackUtils.getBackupTalkBackComponent()
        val intent = Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
            putExtra(EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME, component.flattenToString())
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openAccessibilitySettings()
        }
    }

    private fun openBackupGestureSettings() {
        if (!TalkbackUtils.isBackupTalkBackInstalled(this)) {
            Toast.makeText(this, R.string.talkback_backup_status_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent().apply {
            setClassName(BACKUP_TALKBACK_PACKAGE, BACKUP_TALKBACK_PREFS_ACTIVITY)
            putExtra(EXTRA_FRAGMENT_NAME, BACKUP_GESTURE_FRAGMENT)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            openBackupTalkBackSettings()
        }
    }

    private fun installBackupTalkBack() {
        if (TalkbackUtils.isBackupTalkBackInstalled(this)) {
            openBackupTalkBackSettings()
            return
        }
        val apk = copyApkFromAssets(BACKUP_TALKBACK_ASSET) ?: run {
            Toast.makeText(this, R.string.talkback_backup_install_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, packageName)
        }
        Toast.makeText(this, R.string.talkback_backup_install_started, Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }

    private fun isBackupInstallerAvailable(): Boolean {
        return try {
            assets.open(BACKUP_TALKBACK_ASSET).use { }
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun copyApkFromAssets(assetName: String): File? {
        val outDir = File(filesDir, "blindreader_installer")
        if (!outDir.exists() && !outDir.mkdirs()) return null
        val outFile = File(outDir, assetName)
        if (outFile.exists()) return outFile
        return try {
            assets.open(assetName).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private const val ACTION_ACCESSIBILITY_SHORTCUT_SETTINGS =
            "android.settings.ACCESSIBILITY_SHORTCUT_SETTINGS"
        private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
        private const val EXTRA_ACCESSIBILITY_SERVICE_COMPONENT_NAME =
            "android.provider.extra.ACCESSIBILITY_SERVICE_COMPONENT_NAME"
        private const val EXTRA_FRAGMENT_NAME = "FragmentName"
        private const val BACKUP_TALKBACK_PACKAGE = "com.screenreaders.blindreader"
        private const val BACKUP_TALKBACK_PREFS_ACTIVITY =
            "com.android.talkback.TalkBackPreferencesActivity"
        private const val BACKUP_GESTURE_FRAGMENT =
            "com.google.android.accessibility.talkback.preference.base.TalkBackGestureShortcutPreferenceFragment"
        private const val BACKUP_TALKBACK_ASSET = "blindreader.apk"
        private const val APK_MIME = "application/vnd.android.package-archive"
    }
}
