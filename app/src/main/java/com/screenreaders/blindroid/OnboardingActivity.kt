package com.screenreaders.blindroid

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog
import com.screenreaders.blindroid.util.LowVisionStyler
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class OnboardingActivity : AppCompatActivity() {
    private lateinit var dialerStatus: TextView
    private lateinit var contactsStatus: TextView
    private lateinit var smsStatus: TextView
    private lateinit var notificationsStatus: TextView
    private lateinit var accessibilityStatus: TextView
    private lateinit var exactAlarmStatus: TextView
    private lateinit var updatesStatus: TextView
    private lateinit var ttsStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        LowVisionStyler.apply(this)

        dialerStatus = findViewById(R.id.onboardDialerStatus)
        contactsStatus = findViewById(R.id.onboardContactsStatus)
        smsStatus = findViewById(R.id.onboardSmsStatus)
        notificationsStatus = findViewById(R.id.onboardNotificationsStatus)
        accessibilityStatus = findViewById(R.id.onboardAccessibilityStatus)
        exactAlarmStatus = findViewById(R.id.onboardExactAlarmStatus)
        ttsStatus = findViewById(R.id.onboardTtsStatus)
        updatesStatus = findViewById(R.id.onboardUpdatesStatus)

        findViewById<Button>(R.id.onboardDialerButton).setOnClickListener {
            requestDialerRole()
            DiagnosticLog.log(this, "onboarding_dialer_request")
        }
        findViewById<Button>(R.id.onboardContactsButton).setOnClickListener {
            requestContactsPermission()
            DiagnosticLog.log(this, "onboarding_contacts_request")
        }
        findViewById<Button>(R.id.onboardSmsButton).setOnClickListener {
            requestSmsPermission()
            DiagnosticLog.log(this, "onboarding_sms_request")
        }
        findViewById<Button>(R.id.onboardNotificationsButton).setOnClickListener {
            openNotificationAccessSettings()
            DiagnosticLog.log(this, "onboarding_notifications_request")
        }
        findViewById<Button>(R.id.onboardAccessibilityButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            DiagnosticLog.log(this, "onboarding_accessibility_open")
        }
        findViewById<Button>(R.id.onboardExactAlarmButton).setOnClickListener {
            openExactAlarmSettings()
            DiagnosticLog.log(this, "onboarding_exact_alarm_request")
        }
        findViewById<Button>(R.id.onboardTtsSettingsButton).setOnClickListener {
            openTtsSettings()
            DiagnosticLog.log(this, "onboarding_tts_settings")
        }
        findViewById<Button>(R.id.onboardTtsInstallButton).setOnClickListener {
            installRhvoiceIfNeeded()
            DiagnosticLog.log(this, "onboarding_tts_rhvoice_install")
        }
        findViewById<Button>(R.id.onboardUpdatesButton).setOnClickListener {
            openUpdates()
            DiagnosticLog.log(this, "onboarding_updates_check")
        }
        findViewById<Button>(R.id.onboardFinishButton).setOnClickListener {
            Prefs.setOnboardingDone(this, true)
            DiagnosticLog.log(this, "onboarding_finish")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.onboardSkipButton).setOnClickListener {
            Prefs.setOnboardingDone(this, true)
            DiagnosticLog.log(this, "onboarding_skip")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        LowVisionStyler.apply(this)
        updateStatuses()
    }

    private fun updateStatuses() {
        dialerStatus.text = if (isDialerRoleHeld()) {
            getString(R.string.onboard_status_ok)
        } else {
            getString(R.string.onboard_status_missing)
        }
        contactsStatus.text = if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            getString(R.string.onboard_status_ok)
        } else {
            getString(R.string.onboard_status_missing)
        }
        smsStatus.text = if (hasPermission(Manifest.permission.RECEIVE_SMS)) {
            getString(R.string.onboard_status_ok)
        } else {
            getString(R.string.onboard_status_missing)
        }
        notificationsStatus.text = if (isNotificationAccessEnabled()) {
            getString(R.string.onboard_status_ok)
        } else {
            getString(R.string.onboard_status_missing)
        }
        accessibilityStatus.text = getString(R.string.onboard_status_manual)
        exactAlarmStatus.text = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> getString(R.string.onboard_status_not_needed)
            isExactAlarmAllowed() -> getString(R.string.onboard_status_ok)
            else -> getString(R.string.onboard_status_missing)
        }
        val ttsEngines = getTtsEngines()
        if (ttsEngines.isEmpty()) {
            ttsStatus.text = getString(R.string.onboard_tts_status_missing)
            findViewById<Button>(R.id.onboardTtsInstallButton).isEnabled = true
        } else {
            ttsStatus.text = getString(R.string.onboard_tts_status_ok, ttsEngines.size)
            findViewById<Button>(R.id.onboardTtsInstallButton).isEnabled = !isRhvoiceCoreInstalled() || !isRhvoiceVoiceInstalled()
        }
        updatesStatus.text = getString(R.string.onboard_updates_hint)
    }

    private fun isDialerRoleHeld(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        return roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    }

    private fun requestDialerRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
            && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            startActivity(intent)
        }
    }

    private fun requestContactsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQ_CONTACTS
        )
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS),
            REQ_SMS
        )
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }

    private fun isExactAlarmAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }

    private fun openTtsSettings() {
        try {
            startActivity(Intent(Settings.ACTION_TTS_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun getTtsEngines(): List<String> {
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        val services = packageManager.queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return services.mapNotNull { it.serviceInfo?.packageName }.distinct()
    }

    private fun isRhvoiceCoreInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(RHVOICE_CORE_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isRhvoiceVoiceInstalled(): Boolean {
        val packages = packageManager.getInstalledPackages(0)
        return packages.any { it.packageName.startsWith(RHVOICE_VOICE_PREFIX) }
    }

    private fun installRhvoiceIfNeeded() {
        val coreInstalled = isRhvoiceCoreInstalled()
        val voiceInstalled = isRhvoiceVoiceInstalled()
        val target = when {
            !coreInstalled -> RHVOICE_CORE_ASSET
            !voiceInstalled -> RHVOICE_VOICE_ASSET
            else -> null
        }
        if (target == null) {
            openTtsSettings()
            return
        }
        val apk = copyApkFromAssets(target) ?: run {
            Toast.makeText(this, R.string.onboard_tts_install_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        Toast.makeText(this, R.string.onboard_tts_install_started, Toast.LENGTH_SHORT).show()
        startActivity(intent)
    }

    private fun copyApkFromAssets(assetName: String): File? {
        val outDir = File(filesDir, "tts_installer")
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

    private fun openUpdates() {
        val intent = MainActivity.createUpdatesIntent(this, checkNow = true)
        startActivity(intent)
    }

    companion object {
        private const val REQ_CONTACTS = 201
        private const val REQ_SMS = 202
        private const val RHVOICE_CORE_PACKAGE = "com.github.olga_yakovleva.rhvoice.android"
        private const val RHVOICE_VOICE_PREFIX = "com.github.olga_yakovleva.rhvoice.android.voice"
        private const val RHVOICE_CORE_ASSET = "rhvoice-core.apk"
        private const val RHVOICE_VOICE_ASSET = "rhvoice-voice-pl-magda.apk"
    }
}
