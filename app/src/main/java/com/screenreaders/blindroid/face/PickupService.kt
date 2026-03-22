package com.screenreaders.blindroid.face

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Bundle
import android.app.KeyguardManager
import android.telecom.VideoProfile
import android.telecom.TelecomManager
import android.net.Uri
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.call.CallManager
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog

class PickupService : Service() {
    private val sensorManager by lazy { getSystemService(SensorManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private val keyguardManager by lazy { getSystemService(KeyguardManager::class.java) }
    private var announcer: CallAnnouncer? = null
    private var pickupSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var triggerActive = false
    private var accelActive = false
    private var lastFlatTime = 0L
    private var lastAccelTrigger = 0L
    private val tapTimes = ArrayDeque<Long>()
    private val shakeTimes = ArrayDeque<Long>()
    private var lastTapAction = 0L
    private var lastShakeAction = 0L

    private val triggerListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            triggerActive = false
            handlePickup()
            registerPickupTrigger()
        }
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val usePickup = Prefs.isFacePickupEnabled(this@PickupService) || Prefs.isAnswerPickupEnabled(this@PickupService)
            val useTapBack = Prefs.isMissedCallBackEnabled(this@PickupService)
            val useShake = Prefs.isSosShakeEnabled(this@PickupService)
            if (!usePickup && !useTapBack && !useShake) return
            val now = System.currentTimeMillis()
            val z = event.values.getOrNull(2) ?: return
            val x = event.values.getOrNull(0) ?: 0f
            val y = event.values.getOrNull(1) ?: 0f
            val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
            val gForce = magnitude / 9.81f
            if (kotlin.math.abs(z) > 8.5f) {
                lastFlatTime = now
            }
            val moved = magnitude > 11.5f && kotlin.math.abs(z) < 7.0f
            val recentFlat = now - lastFlatTime < 1500L
            if (usePickup && moved && recentFlat && now - lastAccelTrigger > 3000L) {
                lastAccelTrigger = now
                handlePickup()
            }
            if (useTapBack) {
                detectBackTap(now, gForce)
            }
            if (useShake) {
                detectShake(now, gForce)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        pickupSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!shouldRun(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        registerPickupTrigger()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPickupTrigger()
        announcer?.shutdown()
        announcer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePickup() {
        if (Prefs.isAnswerPickupEnabled(this)) {
            val ringing = CallManager.getRingingCall()
            if (ringing != null) {
                DiagnosticLog.log(this, "pickup_answer_call")
                ringing.answer(VideoProfile.STATE_AUDIO_ONLY)
                return
            }
        }
        if (!Prefs.isFaceAssistEnabled(this) || !Prefs.isFacePickupEnabled(this)) return
        val shouldLaunch = isDeviceLockedOrScreenOff()
        if (!shouldLaunch) return
        DiagnosticLog.log(this, "pickup_face_assist")
        val intent = Intent(this, FaceAssistActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun detectBackTap(now: Long, gForce: Float) {
        if (gForce < backTapThreshold()) return
        tapTimes.addLast(now)
        while (tapTimes.size > 3) tapTimes.removeFirst()
        val window = if (tapTimes.size >= 3) now - (tapTimes.firstOrNull() ?: now) else Long.MAX_VALUE
        if (tapTimes.size >= 3 && window < 1200L && now - lastTapAction > 4000L) {
            lastTapAction = now
            tapTimes.clear()
            handleMissedCallBack()
        }
    }

    private fun detectShake(now: Long, gForce: Float) {
        if (gForce < shakeThreshold()) return
        shakeTimes.addLast(now)
        while (shakeTimes.size > 4) shakeTimes.removeFirst()
        val window = if (shakeTimes.size >= 3) now - (shakeTimes.firstOrNull() ?: now) else Long.MAX_VALUE
        if (shakeTimes.size >= 3 && window < 1500L && now - lastShakeAction > 6000L) {
            lastShakeAction = now
            shakeTimes.clear()
            handleSosShake()
        }
    }

    private fun handleMissedCallBack() {
        val number = Prefs.getLastMissedCallNumber(this)
        if (number.isNullOrBlank()) {
            announceMissedCallUnavailable()
            return
        }
        val age = System.currentTimeMillis() - Prefs.getLastMissedCallTime(this)
        if (age > 24 * 60 * 60 * 1000L) {
            announceMissedCallUnavailable()
            return
        }
        val uri = Uri.fromParts("tel", number, null)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            DiagnosticLog.log(this, "missed_call_back")
            getSystemService(TelecomManager::class.java).placeCall(uri, Bundle())
        } else {
            val intent = Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    private fun handleSosShake() {
        val number = Prefs.getSosNumber(this)
        if (number.isBlank()) return
        val uri = Uri.fromParts("tel", number, null)
        DiagnosticLog.log(this, "sos_shake")
        val intent = Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun announceMissedCallUnavailable() {
        if (CallManager.getCall() != null) return
        val engine = announcer ?: CallAnnouncer(this).also { announcer = it }
        engine.speak(
            text = getString(R.string.missed_call_back_missing),
            repeatCount = 1,
            rate = Prefs.getSpeechRate(this),
            volume = Prefs.getSpeechVolume(this),
            voiceName = Prefs.getVoiceName(this)
        )
    }

    private fun backTapThreshold(): Float {
        return when (Prefs.getBackTapSensitivity(this)) {
            0 -> 2.2f
            2 -> 3.2f
            else -> 2.7f
        }
    }

    private fun shakeThreshold(): Float {
        return when (Prefs.getSosShakeSensitivity(this)) {
            0 -> 2.6f
            2 -> 3.8f
            else -> 3.2f
        }
    }

    private fun isDeviceLockedOrScreenOff(): Boolean {
        val locked = keyguardManager.isKeyguardLocked
        val interactive = powerManager.isInteractive
        return locked || !interactive
    }

    private fun registerPickupTrigger() {
        val needsPickup = Prefs.isFacePickupEnabled(this) || Prefs.isAnswerPickupEnabled(this)
        val needsTap = Prefs.isMissedCallBackEnabled(this)
        val needsShake = Prefs.isSosShakeEnabled(this)
        if (!needsPickup && !needsTap && !needsShake) {
            cancelPickupTrigger()
            return
        }
        val sensor = pickupSensor
        if (sensor != null && needsPickup && !needsTap && !needsShake) {
            if (triggerActive) return
            triggerActive = sensorManager.requestTriggerSensor(triggerListener, sensor)
            return
        }
        if (triggerActive && sensor != null) {
            sensorManager.cancelTriggerSensor(triggerListener, sensor)
            triggerActive = false
        }
        val accel = accelSensor ?: return
        if (accelActive) return
        sensorManager.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        accelActive = true
    }

    private fun cancelPickupTrigger() {
        val sensor = pickupSensor
        if (sensor != null && triggerActive) {
            sensorManager.cancelTriggerSensor(triggerListener, sensor)
            triggerActive = false
        }
        if (accelActive) {
            sensorManager.unregisterListener(accelListener)
            accelActive = false
        }
    }

    private fun buildNotification(): Notification {
        createChannel()
        val faceEnabled = Prefs.isFaceAssistEnabled(this)
        val faceShortcut = faceEnabled && Prefs.isFaceShortcutEnabled(this)
        val facePickup = faceEnabled && Prefs.isFacePickupEnabled(this)
        val answerPickup = Prefs.isAnswerPickupEnabled(this)
        val missedCallBack = Prefs.isMissedCallBackEnabled(this)
        val sosShake = Prefs.isSosShakeEnabled(this)
        val targetIntent = if (faceShortcut || facePickup) {
            Intent(this, FaceAssistActivity::class.java)
        } else {
            Intent(this, com.screenreaders.blindroid.MainActivity::class.java)
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = getString(R.string.pickup_notification_title)
        val content = when {
            facePickup -> getString(R.string.face_notification_pickup)
            faceShortcut -> getString(R.string.face_notification_ready)
            answerPickup -> getString(R.string.answer_pickup_notification)
            missedCallBack -> getString(R.string.pickup_notification_missed)
            sosShake -> getString(R.string.pickup_notification_sos)
            else -> getString(R.string.pickup_notification_idle)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.face_notification_channel),
            NotificationManager.IMPORTANCE_MIN
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "blindroid_face_pickup"
        private const val NOTIF_ID = 2104

        fun shouldRun(context: Context): Boolean {
            val faceEnabled = Prefs.isFaceAssistEnabled(context)
            val faceShortcut = faceEnabled && Prefs.isFaceShortcutEnabled(context)
            val facePickup = faceEnabled && Prefs.isFacePickupEnabled(context)
            val answerPickup = Prefs.isAnswerPickupEnabled(context)
            val missedCallBack = Prefs.isMissedCallBackEnabled(context)
            val sosShake = Prefs.isSosShakeEnabled(context)
            return faceShortcut || facePickup || answerPickup || missedCallBack || sosShake
        }

        fun sync(context: Context) {
            if (!shouldRun(context)) {
                context.stopService(Intent(context, PickupService::class.java))
                return
            }
            ContextCompat.startForegroundService(context, Intent(context, PickupService::class.java))
        }
    }
}
