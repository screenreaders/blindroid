package com.screenreaders.blindroid

import android.Manifest
import android.app.AlarmManager
import android.app.DownloadManager
import android.app.role.RoleManager
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.screenreaders.blindroid.BuildConfig
import com.screenreaders.blindroid.accessibility.TalkbackWizardActivity
import com.screenreaders.blindroid.chime.ChimeScheduler
import com.screenreaders.blindroid.currency.CurrencyActivity
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityMainBinding
import com.screenreaders.blindroid.document.DocumentAssistActivity
import com.screenreaders.blindroid.diagnostics.CrashReporter
import com.screenreaders.blindroid.diagnostics.DiagnosticLog
import com.screenreaders.blindroid.face.FaceAssistActivity
import com.screenreaders.blindroid.face.PickupService
import com.screenreaders.blindroid.light.LightActivity
import com.screenreaders.blindroid.obstacle.ObstacleAssistActivity
import com.screenreaders.blindroid.update.UpdateChecker
import com.screenreaders.blindroid.util.LowVisionStyler
import com.screenreaders.blindroid.util.QuietHours
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var pendingCallNumber: String? = null
    private var tts: TextToSpeech? = null
    private var voiceEntries: List<VoiceEntry> = emptyList()
    private val updateExecutor = Executors.newSingleThreadExecutor()
    private val updateHandler = Handler(Looper.getMainLooper())
    private var downloadId: Long = 0L
    private var downloadReceiverRegistered = false
    private var checkingUpdates = false
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id <= 0 || id != downloadId) return
            val dm = getSystemService(DownloadManager::class.java)
            val uri = dm.getUriForDownloadedFile(id) ?: return
            Prefs.setUpdateDownloadId(this@MainActivity, 0L)
            downloadId = 0L
            showInstallPrompt(uri)
        }
    }
    private data class LowVisionOption(val id: Int, val labelRes: Int)
    private val lowVisionOptions = listOf(
        LowVisionOption(LowVisionStyler.STYLE_DEFAULT, R.string.low_vision_theme_default),
        LowVisionOption(LowVisionStyler.STYLE_DARK, R.string.low_vision_theme_dark),
        LowVisionOption(LowVisionStyler.STYLE_LIGHT, R.string.low_vision_theme_light),
        LowVisionOption(LowVisionStyler.STYLE_YELLOW, R.string.low_vision_theme_yellow)
    )
    private data class LowVisionPreset(
        val id: Int,
        val labelRes: Int,
        val enabled: Boolean,
        val style: Int,
        val invert: Boolean,
        val scale: Int
    )
    private val lowVisionPresets = listOf(
        LowVisionPreset(PRESET_CUSTOM, R.string.low_vision_preset_custom, true, LowVisionStyler.STYLE_DEFAULT, false, 100),
        LowVisionPreset(PRESET_LARGE, R.string.low_vision_preset_large, true, LowVisionStyler.STYLE_DEFAULT, false, 130),
        LowVisionPreset(PRESET_CONTRAST, R.string.low_vision_preset_contrast, true, LowVisionStyler.STYLE_DARK, false, 120),
        LowVisionPreset(PRESET_YELLOW, R.string.low_vision_preset_yellow, true, LowVisionStyler.STYLE_YELLOW, false, 140)
    )
    private var updatingLowVisionPreset = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Prefs.isOnboardingDone(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.roleButton.setOnClickListener { requestDialerRole() }
        binding.callButton.setOnClickListener { placeCall() }
        binding.contactsPermissionButton.setOnClickListener { requestContactsPermission() }
        binding.notificationAccessButton.setOnClickListener { openNotificationAccessSettings() }
        binding.launcherSettingsButton.setOnClickListener { openHomeSettings() }
        binding.documentsButton.setOnClickListener { openDocumentModule() }
        binding.faceButton.setOnClickListener { openFaceModule() }
        binding.obstacleButton.setOnClickListener { openObstacleModule() }
        binding.currencyButton.setOnClickListener { openCurrencyModule() }
        binding.lightButton.setOnClickListener { openLightModule() }
        binding.talkbackWizardButton.setOnClickListener { openTalkbackWizard() }

        binding.announceSwitch.isChecked = Prefs.isAnnounceEnabled(this)
        binding.speakerSwitch.isChecked = Prefs.isAutoSpeakerEnabled(this)
        binding.callWaitingSwitch.isChecked = Prefs.isAnnounceDuringCallEnabled(this)
        binding.callStateSwitch.isChecked = Prefs.isCallStateAnnounceEnabled(this)
        binding.callVibrateSwitch.isChecked = Prefs.isCallStateVibrateEnabled(this)
        binding.endCallVibrateSwitch.isChecked = Prefs.isEndCallVibrateEnabled(this)
        binding.voiceCommandsSwitch.isChecked = Prefs.isVoiceCommandsEnabled(this)
        binding.missedCallBackSwitch.isChecked = Prefs.isMissedCallBackEnabled(this)
        binding.smsSwitch.isChecked = Prefs.isSmsReadEnabled(this)
        binding.notificationSwitch.isChecked = Prefs.isNotificationsReadEnabled(this)
        binding.unlockedSwitch.isChecked = Prefs.isReadWhenUnlockedEnabled(this)
        binding.privacySwitch.isChecked = Prefs.isPrivacyModeEnabled(this)
        binding.privacyTitleSwitch.isChecked = Prefs.isPrivacyTitleOnlyEnabled(this)
        binding.updateAutoSwitch.isChecked = Prefs.isAutoUpdateEnabled(this)
        binding.chimeSwitch.isChecked = Prefs.isChimeEnabled(this)
        binding.launcherSwitch.isChecked = isLauncherEnabled()
        binding.moduleShortcutsSwitch.isChecked = Prefs.isModuleShortcutsEnabled(this)
        binding.diagnosticsSwitch.isChecked = Prefs.isDiagnosticsEnabled(this)
        binding.faceSwitch.isChecked = Prefs.isFaceAssistEnabled(this)
        binding.facePickupSwitch.isChecked = Prefs.isFacePickupEnabled(this)
        binding.answerPickupSwitch.isChecked = Prefs.isAnswerPickupEnabled(this)
        binding.faceShortcutSwitch.isChecked = Prefs.isFaceShortcutEnabled(this)
        binding.faceButton.isEnabled = binding.faceSwitch.isChecked
        updateFaceControls()
        initNightUi()
        initSosUi()
        PickupService.sync(this)
        initLowVisionUi()
        binding.diagnosticsViewButton.setOnClickListener {
            startActivity(Intent(this, DiagnosticsActivity::class.java))
        }

        binding.announceSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAnnounceEnabled(this, isChecked)
            logSettingChange("announce", isChecked)
            if (isChecked) {
                requestContactsPermission()
            }
        }

        binding.speakerSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAutoSpeakerEnabled(this, isChecked)
            logSettingChange("speaker", isChecked)
        }

        binding.callWaitingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAnnounceDuringCallEnabled(this, isChecked)
            logSettingChange("call_waiting", isChecked)
        }

        binding.callStateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCallStateAnnounceEnabled(this, isChecked)
            logSettingChange("call_state", isChecked)
        }

        binding.callVibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCallStateVibrateEnabled(this, isChecked)
            logSettingChange("call_vibrate", isChecked)
        }

        binding.endCallVibrateSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEndCallVibrateEnabled(this, isChecked)
            logSettingChange("end_call_vibrate", isChecked)
        }

        binding.voiceCommandsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setVoiceCommandsEnabled(this, isChecked)
            logSettingChange("voice_commands", isChecked)
        }

        binding.missedCallBackSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setMissedCallBackEnabled(this, isChecked)
            logSettingChange("missed_call_back", isChecked)
            PickupService.sync(this)
        }

        binding.smsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setSmsReadEnabled(this, isChecked)
            logSettingChange("sms_read", isChecked)
            if (isChecked) {
                requestSmsPermission()
            }
        }

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNotificationsReadEnabled(this, isChecked)
            logSettingChange("notifications_read", isChecked)
            if (isChecked && !isNotificationAccessEnabled()) {
                openNotificationAccessSettings()
            }
        }

        binding.unlockedSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setReadWhenUnlockedEnabled(this, isChecked)
            logSettingChange("read_unlocked", isChecked)
        }

        binding.privacySwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setPrivacyModeEnabled(this, isChecked)
            logSettingChange("privacy_mode", isChecked)
            binding.privacyTitleSwitch.isEnabled = isChecked
        }

        binding.privacyTitleSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setPrivacyTitleOnlyEnabled(this, isChecked)
            logSettingChange("privacy_title_only", isChecked)
        }

        binding.launcherSwitch.setOnCheckedChangeListener { _, isChecked ->
            setLauncherEnabled(isChecked)
            logSettingChange("launcher", isChecked)
        }

        binding.moduleShortcutsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setModuleShortcutsEnabled(this, isChecked)
            logSettingChange("module_shortcuts", isChecked)
        }

        binding.faceSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setFaceAssistEnabled(this, isChecked)
            logSettingChange("face_assist", isChecked)
            binding.faceButton.isEnabled = isChecked
            if (!isChecked) {
                Prefs.setFacePickupEnabled(this, false)
                Prefs.setFaceShortcutEnabled(this, false)
                binding.facePickupSwitch.isChecked = false
                binding.faceShortcutSwitch.isChecked = false
            }
            updateFaceControls()
            PickupService.sync(this)
        }

        binding.facePickupSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setFacePickupEnabled(this, isChecked)
            logSettingChange("face_pickup", isChecked)
            PickupService.sync(this)
        }

        binding.answerPickupSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAnswerPickupEnabled(this, isChecked)
            logSettingChange("answer_pickup", isChecked)
            PickupService.sync(this)
        }

        binding.faceShortcutSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setFaceShortcutEnabled(this, isChecked)
            logSettingChange("face_shortcut", isChecked)
            PickupService.sync(this)
        }

        binding.updateAutoSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setAutoUpdateEnabled(this, isChecked)
            logSettingChange("auto_update", isChecked)
            if (isChecked) {
                maybeAutoCheckUpdates()
            }
        }

        binding.updateCheckButton.setOnClickListener {
            checkForUpdates(manual = true)
        }

        binding.accessibilitySettingsButton.setOnClickListener {
            openAccessibilitySettings()
        }

        binding.chimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setChimeEnabled(this, isChecked)
            logSettingChange("chime", isChecked)
            if (isChecked) {
                maybeRequestExactAlarmPermission()
                ChimeScheduler.schedule(this)
            } else {
                ChimeScheduler.cancel(this)
            }
            setChimeControlsEnabled(isChecked)
        }

        updateRoleButton()
        updateContactsButton()
        updateSmsSwitch()
        updateNotificationAccessButton()
        updateUnlockedSwitch()
        binding.privacyTitleSwitch.isEnabled = binding.privacySwitch.isChecked
        updateStatusText(getString(R.string.update_status_idle))
        maybeAutoCheckUpdates()
        initEndCallKeyUi()
        initChimeUi()
        initTtsUi()
        initCrashReportUi()
        initSettingsTransferUi()
        applyLowVision()
        handleDialIntent(intent)
        handleSectionIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleDialIntent(intent)
            handleSectionIntent(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        registerDownloadReceiverIfNeeded()
        CrashReporter.uploadPendingReports(this)
    }

    override fun onStop() {
        super.onStop()
        unregisterDownloadReceiver()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationAccessButton()
        updateCrashReportStatus()
        updateCrashReportControls()
        applyLowVision()
        if (Prefs.isChimeEnabled(this) && isExactAlarmAllowed()) {
            ChimeScheduler.schedule(this)
        }
    }

    private fun initCrashReportUi() {
        binding.crashReportSwitch.isChecked = Prefs.isCrashReportingEnabled(this)
        binding.crashReportWifiSwitch.isChecked = Prefs.isCrashWifiOnly(this)
        binding.crashReportForegroundSwitch.isChecked = Prefs.isCrashForegroundOnly(this)
        binding.crashReportChargingSwitch.isChecked = Prefs.isCrashChargingOnly(this)
        binding.crashReportDeviceInfoSwitch.isChecked = Prefs.isCrashDeviceInfoEnabled(this)
        binding.diagnosticsSwitch.isChecked = Prefs.isDiagnosticsEnabled(this)
        binding.crashReportSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCrashReportingEnabled(this, isChecked)
            logSettingChange("crash_reporting", isChecked)
            updateCrashReportControls()
        }
        binding.crashReportWifiSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCrashWifiOnly(this, isChecked)
            logSettingChange("crash_wifi_only", isChecked)
        }
        binding.crashReportForegroundSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCrashForegroundOnly(this, isChecked)
            logSettingChange("crash_foreground_only", isChecked)
        }
        binding.crashReportChargingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCrashChargingOnly(this, isChecked)
            logSettingChange("crash_charging_only", isChecked)
        }
        binding.crashReportDeviceInfoSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setCrashDeviceInfoEnabled(this, isChecked)
            logSettingChange("crash_device_info", isChecked)
        }
        binding.diagnosticsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setDiagnosticsEnabled(this, isChecked)
            logSettingChange("diagnostics", isChecked)
            updateCrashReportControls()
        }

        binding.crashReportShareButton.setOnClickListener {
            shareCrashReport()
        }
        binding.crashReportSendButton.setOnClickListener {
            sendCrashReports()
        }

        binding.crashReportIssueButton.setOnClickListener {
            startActivity(CrashReporter.createIssueIntent(this))
        }

        binding.crashReportClearButton.setOnClickListener {
            CrashReporter.clearReports(this)
            updateCrashReportStatus()
        }

        updateCrashReportControls()
        updateCrashReportStatus()
    }

    private fun updateCrashReportStatus() {
        binding.crashReportStatus.text = CrashReporter.buildReportSummary(this)
    }

    private fun updateCrashReportControls() {
        val enabled = Prefs.isCrashReportingEnabled(this)
        binding.crashReportWifiSwitch.isEnabled = enabled
        binding.crashReportForegroundSwitch.isEnabled = enabled
        binding.crashReportChargingSwitch.isEnabled = enabled
        binding.crashReportDeviceInfoSwitch.isEnabled = enabled
        binding.crashReportShareButton.isEnabled =
            enabled && CrashReporter.getLatestReport(this) != null
        binding.crashReportClearButton.isEnabled =
            enabled && CrashReporter.getLatestReport(this) != null
        binding.crashReportSendButton.isEnabled =
            enabled && CrashReporter.getLatestReport(this) != null && CrashReporter.canUploadNow(this)
        binding.diagnosticsViewButton.isEnabled = Prefs.isDiagnosticsEnabled(this)
    }

    private fun shareCrashReport() {
        val intent = CrashReporter.createShareIntent(this) ?: run {
            updateCrashReportStatus()
            return
        }
        startActivity(Intent.createChooser(intent, getString(R.string.crash_report_share)))
    }

    private fun sendCrashReports() {
        if (!CrashReporter.canUploadNow(this)) {
            Toast.makeText(this, R.string.crash_report_cannot_send, Toast.LENGTH_SHORT).show()
            return
        }
        CrashReporter.uploadPendingReports(this)
        updateCrashReportStatus()
        updateCrashReportControls()
        Handler(Looper.getMainLooper()).postDelayed({
            updateCrashReportStatus()
            updateCrashReportControls()
        }, 1500)
    }

    private fun logSettingChange(name: String, value: Any) {
        DiagnosticLog.log(this, "setting $name=$value")
    }

    private fun initLowVisionUi() {
        binding.lowVisionSwitch.isChecked = Prefs.isLowVisionEnabled(this)
        binding.lowVisionInvertSwitch.isChecked = Prefs.isLowVisionInvert(this)
        binding.lowVisionScaleSeek.progress = Prefs.getLowVisionScale(this)
        binding.lowVisionScaleValue.text = getString(
            R.string.low_vision_scale_value,
            Prefs.getLowVisionScale(this)
        )

        val presetLabels = lowVisionPresets.map { getString(it.labelRes) }
        val presetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetLabels)
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.lowVisionPresetSpinner.adapter = presetAdapter
        val presetIndex = lowVisionPresets.indexOfFirst { it.id == Prefs.getLowVisionPreset(this) }.coerceAtLeast(0)
        binding.lowVisionPresetSpinner.setSelection(presetIndex, false)
        binding.lowVisionPresetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (updatingLowVisionPreset) return
                val preset = lowVisionPresets.getOrNull(position) ?: return
                if (preset.id == PRESET_CUSTOM) {
                    Prefs.setLowVisionPreset(this@MainActivity, PRESET_CUSTOM)
                    return
                }
                applyLowVisionPreset(preset)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        val labels = lowVisionOptions.map { getString(it.labelRes) }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.lowVisionThemeSpinner.adapter = adapter
        val current = Prefs.getLowVisionStyle(this)
        val index = lowVisionOptions.indexOfFirst { it.id == current }.coerceAtLeast(0)
        binding.lowVisionThemeSpinner.setSelection(index)

        binding.lowVisionSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setLowVisionEnabled(this, isChecked)
            markLowVisionPresetCustom()
            applyLowVision()
        }
        binding.lowVisionInvertSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setLowVisionInvert(this, isChecked)
            markLowVisionPresetCustom()
            applyLowVision()
        }
        binding.lowVisionThemeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val option = lowVisionOptions.getOrNull(position) ?: return
                Prefs.setLowVisionStyle(this@MainActivity, option.id)
                markLowVisionPresetCustom()
                applyLowVision()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        binding.lowVisionScaleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceIn(100, 150)
                Prefs.setLowVisionScale(this@MainActivity, value)
                binding.lowVisionScaleValue.text = getString(R.string.low_vision_scale_value, value)
                if (fromUser) {
                    markLowVisionPresetCustom()
                }
                applyLowVision()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateLowVisionControls()
    }

    private fun updateLowVisionControls() {
        val enabled = Prefs.isLowVisionEnabled(this)
        binding.lowVisionThemeSpinner.isEnabled = enabled
        binding.lowVisionInvertSwitch.isEnabled = enabled
        binding.lowVisionScaleSeek.isEnabled = enabled
    }

    private fun applyLowVision() {
        updateLowVisionControls()
        LowVisionStyler.apply(this)
    }

    private fun updateFaceControls() {
        val enabled = Prefs.isFaceAssistEnabled(this)
        binding.facePickupSwitch.isEnabled = enabled
        binding.faceShortcutSwitch.isEnabled = enabled
    }

    private fun initNightUi() {
        binding.nightSwitch.isChecked = Prefs.isQuietEnabled(this)
        binding.nightMuteCallsSwitch.isChecked = Prefs.isQuietMuteCalls(this)
        binding.nightMuteSmsSwitch.isChecked = Prefs.isQuietMuteSms(this)
        binding.nightMuteNotificationsSwitch.isChecked = Prefs.isQuietMuteNotifications(this)
        binding.nightMuteChimeSwitch.isChecked = Prefs.isQuietMuteChime(this)

        binding.nightStartButton.text = formatMinutes(Prefs.getQuietStartMinutes(this))
        binding.nightEndButton.text = formatMinutes(Prefs.getQuietEndMinutes(this))

        binding.nightSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setQuietEnabled(this, isChecked)
            updateNightControls()
        }
        binding.nightMuteCallsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setQuietMuteCalls(this, isChecked)
        }
        binding.nightMuteSmsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setQuietMuteSms(this, isChecked)
        }
        binding.nightMuteNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setQuietMuteNotifications(this, isChecked)
        }
        binding.nightMuteChimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setQuietMuteChime(this, isChecked)
        }

        binding.nightStartButton.setOnClickListener {
            val minutes = Prefs.getQuietStartMinutes(this)
            val hour = minutes / 60
            val minute = minutes % 60
            TimePickerDialog(this, { _, h, m ->
                val value = h * 60 + m
                Prefs.setQuietStartMinutes(this, value)
                binding.nightStartButton.text = formatMinutes(value)
            }, hour, minute, true).show()
        }
        binding.nightEndButton.setOnClickListener {
            val minutes = Prefs.getQuietEndMinutes(this)
            val hour = minutes / 60
            val minute = minutes % 60
            TimePickerDialog(this, { _, h, m ->
                val value = h * 60 + m
                Prefs.setQuietEndMinutes(this, value)
                binding.nightEndButton.text = formatMinutes(value)
            }, hour, minute, true).show()
        }
        updateNightControls()
    }

    private fun updateNightControls() {
        val enabled = Prefs.isQuietEnabled(this)
        binding.nightStartButton.isEnabled = enabled
        binding.nightEndButton.isEnabled = enabled
        binding.nightMuteCallsSwitch.isEnabled = enabled
        binding.nightMuteSmsSwitch.isEnabled = enabled
        binding.nightMuteNotificationsSwitch.isEnabled = enabled
        binding.nightMuteChimeSwitch.isEnabled = enabled
    }

    private fun initSosUi() {
        binding.sosNumberInput.setText(Prefs.getSosNumber(this))
        binding.sosMessageInput.setText(Prefs.getSosMessage(this))
        binding.sosShakeSwitch.isChecked = Prefs.isSosShakeEnabled(this)

        binding.sosNumberInput.setOnFocusChangeListener { _, _ ->
            Prefs.setSosNumber(this, binding.sosNumberInput.text?.toString().orEmpty())
        }
        binding.sosMessageInput.setOnFocusChangeListener { _, _ ->
            Prefs.setSosMessage(this, binding.sosMessageInput.text?.toString().orEmpty())
        }
        binding.sosShakeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setSosShakeEnabled(this, isChecked)
            PickupService.sync(this)
        }
        binding.sosCallButton.setOnClickListener {
            triggerSosCall()
        }
        binding.sosSmsButton.setOnClickListener {
            triggerSosSms()
        }
    }

    private fun triggerSosCall() {
        val number = binding.sosNumberInput.text?.toString()?.trim().orEmpty()
        if (number.isBlank()) {
            Toast.makeText(this, R.string.sos_missing_number, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setSosNumber(this, number)
        val uri = Uri.fromParts("tel", number, null)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
        } else {
            startActivity(Intent(Intent.ACTION_DIAL, uri))
        }
    }

    private fun triggerSosSms() {
        val number = binding.sosNumberInput.text?.toString()?.trim().orEmpty()
        if (number.isBlank()) {
            Toast.makeText(this, R.string.sos_missing_number, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setSosNumber(this, number)
        val message = binding.sosMessageInput.text?.toString().orEmpty()
        Prefs.setSosMessage(this, message)
        val uri = Uri.parse("smsto:$number")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
        }
        startActivity(intent)
    }

    private fun formatMinutes(value: Int): String {
        val hour = value / 60
        val minute = value % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun applyLowVisionPreset(preset: LowVisionPreset) {
        updatingLowVisionPreset = true
        Prefs.setLowVisionPreset(this, preset.id)
        Prefs.setLowVisionEnabled(this, preset.enabled)
        Prefs.setLowVisionStyle(this, preset.style)
        Prefs.setLowVisionInvert(this, preset.invert)
        Prefs.setLowVisionScale(this, preset.scale)
        binding.lowVisionSwitch.isChecked = preset.enabled
        binding.lowVisionInvertSwitch.isChecked = preset.invert
        binding.lowVisionScaleSeek.progress = preset.scale
        binding.lowVisionScaleValue.text = getString(R.string.low_vision_scale_value, preset.scale)
        val themeIndex = lowVisionOptions.indexOfFirst { it.id == preset.style }.coerceAtLeast(0)
        binding.lowVisionThemeSpinner.setSelection(themeIndex, false)
        val presetIndex = lowVisionPresets.indexOfFirst { it.id == preset.id }.coerceAtLeast(0)
        binding.lowVisionPresetSpinner.setSelection(presetIndex, false)
        updatingLowVisionPreset = false
        applyLowVision()
    }

    private fun markLowVisionPresetCustom() {
        if (updatingLowVisionPreset) return
        Prefs.setLowVisionPreset(this, PRESET_CUSTOM)
        val index = lowVisionPresets.indexOfFirst { it.id == PRESET_CUSTOM }.coerceAtLeast(0)
        updatingLowVisionPreset = true
        binding.lowVisionPresetSpinner.setSelection(index, false)
        updatingLowVisionPreset = false
    }

    private fun initSettingsTransferUi() {
        binding.exportSettingsButton.setOnClickListener { exportSettings() }
        binding.importSettingsButton.setOnClickListener { importSettings() }
    }

    private fun exportSettings() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "blindroid-settings.json")
        }
        startActivityForResult(intent, REQ_EXPORT_SETTINGS)
    }

    private fun importSettings() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQ_IMPORT_SETTINGS)
    }

    private fun exportSettingsToUri(uri: Uri) {
        val prefs = getSharedPreferences("blindroid_prefs", Context.MODE_PRIVATE)
        val items = JSONArray()
        for ((key, value) in prefs.all) {
            if (EXPORT_EXCLUDE_KEYS.contains(key)) continue
            val obj = JSONObject()
            obj.put("key", key)
            when (value) {
                is Boolean -> {
                    obj.put("type", "bool")
                    obj.put("value", value)
                }
                is Int -> {
                    obj.put("type", "int")
                    obj.put("value", value)
                }
                is Long -> {
                    obj.put("type", "long")
                    obj.put("value", value)
                }
                is Float -> {
                    obj.put("type", "float")
                    obj.put("value", value.toDouble())
                }
                is String -> {
                    obj.put("type", "string")
                    obj.put("value", value)
                }
                else -> continue
            }
            items.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("items", items)
        try {
            contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString().toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("No output stream")
            Toast.makeText(this, R.string.settings_export_done, Toast.LENGTH_SHORT).show()
            DiagnosticLog.log(this, "settings_export")
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importSettingsFromUri(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("No input stream")
            val root = JSONObject(text)
            val items = root.optJSONArray("items") ?: JSONArray()
            val prefs = getSharedPreferences("blindroid_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            for (i in 0 until items.length()) {
                val obj = items.optJSONObject(i) ?: continue
                val key = obj.optString("key")
                if (key.isBlank() || EXPORT_EXCLUDE_KEYS.contains(key)) continue
                when (obj.optString("type")) {
                    "bool" -> editor.putBoolean(key, obj.optBoolean("value"))
                    "int" -> editor.putInt(key, obj.optInt("value"))
                    "long" -> editor.putLong(key, obj.optLong("value"))
                    "float" -> editor.putFloat(key, obj.optDouble("value").toFloat())
                    "string" -> editor.putString(key, obj.optString("value"))
                }
            }
            editor.apply()
            Toast.makeText(this, R.string.settings_import_done, Toast.LENGTH_SHORT).show()
            DiagnosticLog.log(this, "settings_import")
            recreate()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.settings_import_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDialIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_DIAL) {
            val data = intent.data
            if (data != null && "tel" == data.scheme) {
                val number = data.schemeSpecificPart
                binding.numberInput.setText(number)
            }
        }
    }

    private fun handleSectionIntent(intent: Intent) {
        val section = intent.getStringExtra(EXTRA_SECTION) ?: return
        val target = when (section) {
            SECTION_LAUNCHER -> binding.launcherLabel
            SECTION_CALLS -> binding.callsLabel
            SECTION_NOTIFICATIONS -> binding.notificationsLabel
            SECTION_DOCUMENTS -> binding.documentsLabel
            SECTION_FACE -> binding.faceLabel
            SECTION_CURRENCY -> binding.currencyLabel
            SECTION_LIGHT -> binding.lightLabel
            SECTION_CHIME -> binding.chimeLabel
            SECTION_UPDATES -> binding.updateLabel
            else -> null
        } ?: return
        binding.mainScroll.post {
            binding.mainScroll.smoothScrollTo(0, target.top)
        }
        if (section == SECTION_UPDATES && intent.getBooleanExtra(EXTRA_CHECK_UPDATES, false)) {
            checkForUpdates(manual = true)
        }
    }

    private fun requestDialerRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)
            && !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            startActivityForResult(intent, REQ_ROLE)
        }
    }

    private fun updateRoleButton() {
        val roleManager = getSystemService(RoleManager::class.java)
        val isHeld = roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        binding.roleButton.isEnabled = !isHeld
        binding.roleButton.text = if (isHeld) {
            getString(R.string.role_button_granted)
        } else {
            getString(R.string.role_button_request)
        }
    }

    private fun placeCall() {
        val number = binding.numberInput.text?.toString()?.trim().orEmpty()
        if (number.isBlank()) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingCallNumber = number
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQ_CALL_PHONE
            )
            return
        }

        val telecomManager = getSystemService(TelecomManager::class.java)
        val uri = Uri.fromParts("tel", number, null)
        telecomManager.placeCall(uri, Bundle())
    }

    private fun openCurrencyModule() {
        DiagnosticLog.log(this, "module_currency_open")
        startActivity(Intent(this, CurrencyActivity::class.java))
    }

    private fun openDocumentModule() {
        DiagnosticLog.log(this, "module_documents_open")
        startActivity(Intent(this, DocumentAssistActivity::class.java))
    }

    private fun openFaceModule() {
        if (!Prefs.isFaceAssistEnabled(this)) {
            Toast.makeText(this, R.string.face_enable, Toast.LENGTH_SHORT).show()
            return
        }
        DiagnosticLog.log(this, "module_face_open")
        startActivity(Intent(this, FaceAssistActivity::class.java))
    }

    private fun openObstacleModule() {
        DiagnosticLog.log(this, "module_obstacle_open")
        startActivity(Intent(this, ObstacleAssistActivity::class.java))
    }

    private fun openLightModule() {
        DiagnosticLog.log(this, "module_light_open")
        startActivity(Intent(this, LightActivity::class.java))
    }

    private fun openTalkbackWizard() {
        startActivity(Intent(this, TalkbackWizardActivity::class.java))
    }

    private fun requestContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            updateContactsButton()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_CONTACTS),
            REQ_CONTACTS
        )
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS),
            REQ_SMS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CALL_PHONE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val number = pendingCallNumber
            pendingCallNumber = null
            if (!number.isNullOrBlank()) {
                binding.numberInput.setText(number)
                placeCall()
            }
        }
        if (requestCode == REQ_CONTACTS) {
            updateContactsButton()
        }
        if (requestCode == REQ_SMS) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Prefs.setSmsReadEnabled(this, false)
            }
            updateSmsSwitch()
        }
    }

    private fun updateContactsButton() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        binding.contactsPermissionButton.isEnabled = !granted
        binding.contactsPermissionButton.text = if (granted) {
            getString(R.string.permission_contacts_granted)
        } else {
            getString(R.string.permission_contacts)
        }
    }

    private fun updateSmsSwitch() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted && Prefs.isSmsReadEnabled(this)) {
            Prefs.setSmsReadEnabled(this, false)
        }
        binding.smsSwitch.isChecked = Prefs.isSmsReadEnabled(this)
    }

    private fun updateUnlockedSwitch() {
        binding.unlockedSwitch.isChecked = Prefs.isReadWhenUnlockedEnabled(this)
    }

    private fun isNotificationAccessEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this)
            .contains(packageName)
    }

    private fun updateNotificationAccessButton() {
        val enabled = isNotificationAccessEnabled()
        binding.notificationAccessButton.isEnabled = !enabled
        binding.notificationAccessButton.text = if (enabled) {
            getString(R.string.notification_access_granted)
        } else {
            getString(R.string.notification_access_button)
        }
    }

    private fun openNotificationAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ROLE) {
            updateRoleButton()
        }
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data ?: return
        when (requestCode) {
            REQ_EXPORT_SETTINGS -> exportSettingsToUri(uri)
            REQ_IMPORT_SETTINGS -> importSettingsFromUri(uri)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        tts = null
        updateExecutor.shutdownNow()
    }

    private fun initTtsUi() {
        val currentRate = Prefs.getSpeechRate(this)
        val currentVolume = Prefs.getSpeechVolume(this)
        val currentRepeat = Prefs.getRepeatCount(this)
        val currentMode = Prefs.getAnnounceMode(this)

        binding.ttsRateSeek.progress = (currentRate * 100).toInt().coerceIn(50, 200)
        binding.ttsRateValue.text = String.format(Locale.getDefault(), "%.1fx", currentRate)
        binding.ttsRateValue.contentDescription =
            "${getString(R.string.tts_rate)} ${binding.ttsRateValue.text}"

        binding.ttsVolumeSeek.progress = (currentVolume * 100).toInt().coerceIn(0, 100)
        binding.ttsVolumeValue.text = String.format(
            Locale.getDefault(),
            "%d%%",
            (currentVolume * 100).toInt()
        )
        binding.ttsVolumeValue.contentDescription =
            "${getString(R.string.tts_volume)} ${binding.ttsVolumeValue.text}"

        val repeatDisplay = currentRepeat.coerceIn(1, 3)
        binding.ttsRepeatSeek.progress = repeatDisplay
        binding.ttsRepeatValue.text = String.format(Locale.getDefault(), "%dx", repeatDisplay)
        binding.ttsRepeatValue.contentDescription =
            "${getString(R.string.tts_repeat)} ${binding.ttsRepeatValue.text}"

        when (currentMode) {
            Prefs.MODE_SPEECH_ONLY -> binding.ttsModeSpeechOnly.isChecked = true
            Prefs.MODE_SPEECH_THEN_RING -> binding.ttsModeSpeechThenRing.isChecked = true
            else -> binding.ttsModeRingSpeech.isChecked = true
        }

        binding.ttsModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.ttsModeSpeechOnly -> Prefs.MODE_SPEECH_ONLY
                R.id.ttsModeSpeechThenRing -> Prefs.MODE_SPEECH_THEN_RING
                else -> Prefs.MODE_RING_AND_SPEECH
            }
            Prefs.setAnnounceMode(this, mode)
        }

        binding.ttsRateSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = (progress.coerceIn(50, 200)) / 100f
                binding.ttsRateValue.text = String.format(Locale.getDefault(), "%.1fx", rate)
                binding.ttsRateValue.contentDescription =
                    "${getString(R.string.tts_rate)} ${binding.ttsRateValue.text}"
                Prefs.setSpeechRate(this@MainActivity, rate)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.ttsVolumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress.coerceIn(0, 100)
                val volume = value / 100f
                binding.ttsVolumeValue.text = String.format(Locale.getDefault(), "%d%%", value)
                binding.ttsVolumeValue.contentDescription =
                    "${getString(R.string.tts_volume)} ${binding.ttsVolumeValue.text}"
                Prefs.setSpeechVolume(this@MainActivity, volume)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.ttsRepeatSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val repeat = progress.coerceIn(1, 3)
                binding.ttsRepeatValue.text = String.format(Locale.getDefault(), "%dx", repeat)
                binding.ttsRepeatValue.contentDescription =
                    "${getString(R.string.tts_repeat)} ${binding.ttsRepeatValue.text}"
                Prefs.setRepeatCount(this@MainActivity, repeat)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.ttsVoiceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val entry = voiceEntries.getOrNull(position)
                Prefs.setVoiceName(this@MainActivity, entry?.name)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No-op
            }
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts ?: return@TextToSpeech
                val voices = engine.voices?.toList().orEmpty().sortedWith(
                    compareBy({ it.locale.displayName }, { it.name })
                )
                voiceEntries = voices.map { voice ->
                    VoiceEntry(
                        label = "${voice.locale.displayName} (${voice.name})",
                        name = voice.name
                    )
                }
                if (voiceEntries.isEmpty()) {
                    setVoiceSpinnerUnavailable()
                } else {
                    binding.ttsVoiceSpinner.isEnabled = true
                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_item,
                        voiceEntries.map { it.label }
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    binding.ttsVoiceSpinner.adapter = adapter

                    val saved = Prefs.getVoiceName(this@MainActivity)
                    val defaultVoiceName = engine.defaultVoice?.name
                    val target = saved ?: defaultVoiceName
                    val index = voiceEntries.indexOfFirst { it.name == target }
                    if (index >= 0) {
                        binding.ttsVoiceSpinner.setSelection(index)
                    }
                }
            } else {
                setVoiceSpinnerUnavailable()
            }
        }
    }

    private fun setVoiceSpinnerUnavailable() {
        voiceEntries = emptyList()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listOf(getString(R.string.tts_unavailable))
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.ttsVoiceSpinner.adapter = adapter
        binding.ttsVoiceSpinner.isEnabled = false
    }

    private data class VoiceEntry(val label: String, val name: String)

    private fun initEndCallKeyUi() {
        val options = listOf(
            Prefs.END_CALL_NONE,
            Prefs.END_CALL_VOLUME_UP,
            Prefs.END_CALL_VOLUME_DOWN,
            Prefs.END_CALL_HEADSET,
            Prefs.END_CALL_POWER
        )
        val labels = listOf(
            getString(R.string.end_call_key_none),
            getString(R.string.end_call_key_volume_up),
            getString(R.string.end_call_key_volume_down),
            getString(R.string.end_call_key_headset),
            getString(R.string.end_call_key_power)
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.endCallKeySpinner.adapter = adapter

        val current = Prefs.getEndCallKey(this)
        val index = options.indexOf(current).let { if (it >= 0) it else 0 }
        binding.endCallKeySpinner.setSelection(index)
        updateEndCallPowerHint(current)

        binding.endCallKeySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val value = options.getOrElse(position) { Prefs.END_CALL_NONE }
                Prefs.setEndCallKey(this@MainActivity, value)
                updateEndCallPowerHint(value)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateEndCallPowerHint(value: Int) {
        val show = value == Prefs.END_CALL_POWER
        binding.endCallPowerHint.visibility = if (show) View.VISIBLE else View.GONE
        binding.accessibilitySettingsButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun isLauncherEnabled(): Boolean {
        val component = ComponentName(
            this,
            "com.screenreaders.blindroid.launcher.LauncherActivity"
        )
        val state = packageManager.getComponentEnabledSetting(component)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    private fun setLauncherEnabled(enabled: Boolean) {
        val component = ComponentName(
            this,
            "com.screenreaders.blindroid.launcher.LauncherActivity"
        )
        packageManager.setComponentEnabledSetting(
            component,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun openHomeSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Intent(Settings.ACTION_HOME_SETTINGS)
        } else {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        }
        startActivity(intent)
    }

    private fun isExactAlarmAllowed(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    private fun maybeRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (isExactAlarmAllowed()) return
        AlertDialog.Builder(this)
            .setTitle(R.string.exact_alarm_title)
            .setMessage(R.string.exact_alarm_message)
            .setPositiveButton(R.string.exact_alarm_allow) { _, _ ->
                openExactAlarmSettings()
            }
            .setNegativeButton(R.string.exact_alarm_cancel, null)
            .show()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        startActivity(intent)
    }

    private fun initChimeUi() {
        val intervals = listOf(15, 30, 60)
        val labels = listOf(
            getString(R.string.chime_interval_15),
            getString(R.string.chime_interval_30),
            getString(R.string.chime_interval_60)
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.chimeIntervalSpinner.adapter = adapter

        val currentInterval = Prefs.getChimeInterval(this)
        val index = intervals.indexOf(currentInterval).let { if (it >= 0) it else 2 }
        binding.chimeIntervalSpinner.setSelection(index)

        binding.chimeIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val interval = intervals.getOrElse(position) { 60 }
                Prefs.setChimeInterval(this@MainActivity, interval)
                if (Prefs.isChimeEnabled(this@MainActivity)) {
                    ChimeScheduler.schedule(this@MainActivity)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.chimeStartButton.setOnClickListener { showChimeTimePicker(isStart = true) }
        binding.chimeEndButton.setOnClickListener { showChimeTimePicker(isStart = false) }

        updateChimeTimeButtons()
        setChimeControlsEnabled(Prefs.isChimeEnabled(this))
    }

    private fun setChimeControlsEnabled(enabled: Boolean) {
        binding.chimeIntervalSpinner.isEnabled = enabled
        binding.chimeStartButton.isEnabled = enabled
        binding.chimeEndButton.isEnabled = enabled
    }

    private fun updateChimeTimeButtons() {
        val start = Prefs.getChimeStartMinutes(this)
        val end = Prefs.getChimeEndMinutes(this)
        val startText = minutesToTimeText(start)
        val endText = minutesToTimeText(end)
        binding.chimeStartButton.text = startText
        binding.chimeEndButton.text = endText
        binding.chimeStartButton.contentDescription = "${getString(R.string.chime_start)} $startText"
        binding.chimeEndButton.contentDescription = "${getString(R.string.chime_end)} $endText"
    }

    private fun minutesToTimeText(minutes: Int): String {
        val clamped = minutes.coerceIn(0, 24 * 60)
        val hour = clamped / 60
        val minute = clamped % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun showChimeTimePicker(isStart: Boolean) {
        val current = if (isStart) {
            Prefs.getChimeStartMinutes(this)
        } else {
            Prefs.getChimeEndMinutes(this)
        }
        val hour = current / 60
        val minute = current % 60
        TimePickerDialog(
            this,
            { _, h, m ->
                val value = h * 60 + m
                if (isStart) {
                    Prefs.setChimeStartMinutes(this, value)
                } else {
                    Prefs.setChimeEndMinutes(this, value)
                }
                updateChimeTimeButtons()
                if (Prefs.isChimeEnabled(this)) {
                    ChimeScheduler.schedule(this)
                }
            },
            hour,
            minute,
            true
        ).show()
    }

    private fun updateStatusText(text: String) {
        binding.updateStatusText.text = text
    }

    private fun maybeAutoCheckUpdates() {
        if (!Prefs.isAutoUpdateEnabled(this)) return
        val last = Prefs.getLastUpdateCheck(this)
        val now = System.currentTimeMillis()
        val interval = 24 * 60 * 60 * 1000L
        if (now - last < interval) return
        checkForUpdates(manual = false)
    }

    private fun checkForUpdates(manual: Boolean) {
        if (checkingUpdates) return
        checkingUpdates = true
        DiagnosticLog.log(this, "update_check manual=$manual")
        updateStatusText(getString(R.string.update_status_checking))
        updateExecutor.execute {
            val result = try {
                UpdateChecker.fetchLatest()
            } catch (_: Exception) {
                null
            }
            val now = System.currentTimeMillis()
            Prefs.setLastUpdateCheck(this, now)
            updateHandler.post {
                checkingUpdates = false
                if (result == null) {
                    updateStatusText(getString(R.string.update_status_idle))
                    return@post
                }
                val current = BuildConfig.VERSION_NAME
                val newer = UpdateChecker.isNewer(current, result.version)
                if (newer) {
                    updateStatusText(getString(R.string.update_status_available))
                    showUpdateDialog(result)
                } else {
                    updateStatusText(getString(R.string.update_status_latest))
                    if (manual) {
                        // No dialog needed
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(info: UpdateChecker.UpdateInfo) {
        val message = buildString {
            append("Wersja: ")
            append(info.version)
            if (!info.notes.isNullOrBlank()) {
                append("\n\n")
                append(info.notes)
            }
        }
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_dialog_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.update_dialog_download)) { _, _ ->
                startUpdateDownload(info)
            }
            .setNegativeButton(getString(R.string.update_dialog_later), null)
            .show()
    }

    private fun startUpdateDownload(info: UpdateChecker.UpdateInfo) {
        val url = info.apkUrl
        if (url.isNullOrBlank()) {
            updateStatusText(getString(R.string.update_no_download))
            return
        }
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Blindroid ${info.version}")
            .setDescription(getString(R.string.update_download_started))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "blindroid-${info.version}.apk"
            )
        val dm = getSystemService(DownloadManager::class.java)
        downloadId = dm.enqueue(request)
        Prefs.setUpdateDownloadId(this, downloadId)
        updateStatusText(getString(R.string.update_download_started))
        registerDownloadReceiverIfNeeded()
    }

    private fun registerDownloadReceiverIfNeeded() {
        if (downloadReceiverRegistered) return
        downloadId = Prefs.getUpdateDownloadId(this)
        if (downloadId <= 0L) return
        val filter = android.content.IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(downloadReceiver, filter)
        }
        downloadReceiverRegistered = true
    }

    private fun unregisterDownloadReceiver() {
        if (!downloadReceiverRegistered) return
        try {
            unregisterReceiver(downloadReceiver)
        } catch (_: Exception) {
            // Ignore
        } finally {
            downloadReceiverRegistered = false
        }
    }

    private fun showInstallPrompt(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }
        }
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(installIntent)
        updateStatusText(getString(R.string.update_download_complete))
    }

    companion object {
        private const val REQ_ROLE = 100
        private const val REQ_CALL_PHONE = 101
        private const val REQ_CONTACTS = 102
        private const val REQ_SMS = 103
        private const val REQ_EXPORT_SETTINGS = 301
        private const val REQ_IMPORT_SETTINGS = 302
        const val EXTRA_SECTION = "extra_section"
        const val EXTRA_CHECK_UPDATES = "extra_check_updates"
        const val SECTION_LAUNCHER = "launcher"
        const val SECTION_CALLS = "calls"
        const val SECTION_NOTIFICATIONS = "notifications"
        const val SECTION_DOCUMENTS = "documents"
        const val SECTION_FACE = "face"
        const val SECTION_CURRENCY = "currency"
        const val SECTION_LIGHT = "light"
        const val SECTION_CHIME = "chime"
        const val SECTION_UPDATES = "updates"
        const val PRESET_CUSTOM = 0
        const val PRESET_LARGE = 1
        const val PRESET_CONTRAST = 2
        const val PRESET_YELLOW = 3

        private val EXPORT_EXCLUDE_KEYS = setOf(
            "update_download_id",
            "last_update_check",
            "crash_client_id",
            "onboarding_done",
            "recent_notifications"
        )

        fun createUpdatesIntent(context: Context, checkNow: Boolean): Intent {
            return Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_SECTION, SECTION_UPDATES)
                .putExtra(EXTRA_CHECK_UPDATES, checkNow)
        }
    }
}
