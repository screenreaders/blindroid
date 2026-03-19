package com.screenreaders.blindroid.audio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.screenreaders.blindroid.data.Prefs

class ProximitySpeakerController(context: Context) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val proximitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var running = false
    private var lastNear: Boolean? = null

    fun start() {
        if (running) return
        if (proximitySensor == null) return
        running = true
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        if (!running) return
        running = false
        lastNear = null
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!Prefs.isAutoSpeakerEnabled(appContext)) return
        if (hasExternalOutput()) return
        when (Prefs.getSpeakerOverride(appContext)) {
            Prefs.SPEAKER_OVERRIDE_SPEAKER -> {
                routeToSpeaker()
                return
            }
            Prefs.SPEAKER_OVERRIDE_EARPIECE -> {
                routeToEarpiece()
                return
            }
        }
        val sensor = proximitySensor ?: return
        val distance = event.values[0]
        val near = distance < sensor.maximumRange
        if (lastNear == near) return
        lastNear = near

        if (near) {
            routeToEarpiece()
        } else {
            routeToSpeaker()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun routeToSpeaker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
        }
        audioManager.isSpeakerphoneOn = true
    }

    private fun routeToEarpiece() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = audioManager.availableCommunicationDevices
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
            if (device != null) {
                audioManager.setCommunicationDevice(device)
                return
            }
            audioManager.clearCommunicationDevice()
            return
        }
        audioManager.isSpeakerphoneOn = false
    }

    private fun hasExternalOutput(): Boolean {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> true
                else -> false
            }
        }
    }
}
