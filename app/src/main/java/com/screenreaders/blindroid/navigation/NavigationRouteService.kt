package com.screenreaders.blindroid.navigation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationRouteService : Service(), LocationListener {
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private var announcer: CallAnnouncer? = null
    private var running = false
    private var lastLocation: Location? = null
    private var lastHeading: Float? = null
    private var lastSpeakTime = 0L
    private var lastDistance = Double.MAX_VALUE
    private var routePoints: List<RoutePoint> = emptyList()
    private var routeIndex = 0
    private var routePath: String? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Prefs.isNavigationRouteEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        if (Prefs.isNavigationRouteGpxEnabled(this) && !ensureGpxRouteLoaded(true)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        Prefs.setNavigationRouteEnabled(this, false)
        announcer?.shutdown()
        announcer = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        if (running) return
        running = true
        if (!hasLocationPermission()) {
            speakOnce(getString(R.string.navigation_tracking_missing_location))
            stopSelf()
            return
        }
        val intervalMs = Prefs.getNavigationRouteIntervalSec(this).toLong() * 1000L
        val minDistance = Prefs.getNavigationRouteMinDistance(this).toFloat()
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(
                    provider,
                    intervalMs,
                    minDistance,
                    this,
                    Looper.getMainLooper()
                )
            }
        }
        val lastKnown = providers.mapNotNull { locationManager.getLastKnownLocation(it) }
            .maxByOrNull { it.time }
        if (lastKnown != null) {
            onLocationChanged(lastKnown)
        }
    }

    private fun stopTracking() {
        if (!running) return
        running = false
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!Prefs.isNavigationRouteEnabled(this)) {
            stopSelf()
            return
        }
        if (Prefs.isNavigationRouteGpxEnabled(this) && ensureGpxRouteLoaded(false)) {
            handleGpxGuidance(location)
            return
        }
        val destLat = Prefs.getNavigationRouteLat(this)
        val destLon = Prefs.getNavigationRouteLon(this)
        if (destLat.isNaN() || destLon.isNaN()) {
            stopSelf()
            return
        }
        updateHeading(location)
        val distance = distanceMeters(location.latitude, location.longitude, destLat, destLon).toDouble()
        if (distance <= ARRIVAL_DISTANCE_METERS) {
            speakOnce(getString(R.string.navigation_offline_guidance_arrived))
            Prefs.setNavigationRouteEnabled(this, false)
            stopSelf()
            return
        }
        val now = System.currentTimeMillis()
        val intervalMs = Prefs.getNavigationRouteIntervalSec(this).toLong() * 1000L
        val distanceDelta = abs(lastDistance - distance)
        if (now - lastSpeakTime < intervalMs && distanceDelta < 20) return
        lastSpeakTime = now
        lastDistance = distance
        val direction = buildDirectionMessage(location, destLat, destLon)
        val label = Prefs.getNavigationRouteLabel(this).ifBlank {
            getString(R.string.navigation_offline_guidance_label)
        }
        val message = if (direction.isBlank()) {
            getString(R.string.navigation_offline_guidance_distance, formatDistance(distance.toInt()), label)
        } else {
            getString(
                R.string.navigation_offline_guidance_direction,
                formatDistance(distance.toInt()),
                direction,
                label
            )
        }
        speakOnce(message)
    }

    private fun handleGpxGuidance(location: Location) {
        if (routePoints.isEmpty()) return
        updateHeading(location)
        var idx = routeIndex.coerceIn(0, routePoints.lastIndex)
        var target = routePoints[idx]
        var distance = distanceMeters(location.latitude, location.longitude, target.lat, target.lon).toDouble()
        val threshold = GPX_POINT_REACHED_METERS
        while (idx < routePoints.lastIndex && distance <= threshold) {
            idx++
            target = routePoints[idx]
            distance = distanceMeters(location.latitude, location.longitude, target.lat, target.lon).toDouble()
            lastSpeakTime = 0L
        }
        if (idx != routeIndex) {
            routeIndex = idx
            Prefs.setNavigationRouteGpxIndex(this, idx)
        }
        if (routeIndex >= routePoints.lastIndex && distance <= threshold) {
            speakOnce(getString(R.string.navigation_offline_guidance_arrived))
            Prefs.setNavigationRouteEnabled(this, false)
            stopSelf()
            return
        }
        val now = System.currentTimeMillis()
        val intervalMs = Prefs.getNavigationRouteIntervalSec(this).toLong() * 1000L
        val distanceDelta = abs(lastDistance - distance)
        if (now - lastSpeakTime < intervalMs && distanceDelta < 20) return
        lastSpeakTime = now
        lastDistance = distance
        val direction = buildDirectionMessage(location, target.lat, target.lon)
        val count = routePoints.size
        val message = if (direction.isBlank()) {
            getString(
                R.string.navigation_offline_gpx_guidance_simple,
                routeIndex + 1,
                count,
                formatDistance(distance.toInt())
            )
        } else {
            getString(
                R.string.navigation_offline_gpx_guidance_direction,
                routeIndex + 1,
                count,
                formatDistance(distance.toInt()),
                direction
            )
        }
        speakOnce(message)
    }

    private fun ensureGpxRouteLoaded(forceSpeak: Boolean): Boolean {
        val path = Prefs.getNavigationRouteGpxPath(this)
        if (path.isNullOrBlank()) {
            if (forceSpeak) speakOnce(getString(R.string.navigation_offline_gpx_missing))
            return false
        }
        if (path != routePath || routePoints.isEmpty()) {
            routePath = path
            routePoints = try {
                GpxRouteParser.parse(File(path))
            } catch (_: Exception) {
                emptyList()
            }
            routeIndex = Prefs.getNavigationRouteGpxIndex(this)
        }
        if (routePoints.size < 2) {
            if (forceSpeak) speakOnce(getString(R.string.navigation_offline_gpx_failed))
            return false
        }
        if (routeIndex >= routePoints.size) {
            routeIndex = 0
            Prefs.setNavigationRouteGpxIndex(this, 0)
        }
        return true
    }

    private fun updateHeading(location: Location) {
        val prev = lastLocation
        if (prev != null && prev.distanceTo(location) >= 3f) {
            lastHeading = bearingBetween(prev.latitude, prev.longitude, location.latitude, location.longitude)
        } else if (location.hasBearing()) {
            lastHeading = location.bearing
        }
        lastLocation = location
    }

    private fun buildDirectionMessage(location: Location, destLat: Double, destLon: Double): String {
        val bearingToDest = bearingBetween(location.latitude, location.longitude, destLat, destLon)
        val heading = lastHeading
        return if (heading != null) {
            val diff = normalizeBearing(bearingToDest - heading)
            val absDiff = abs(diff)
            when {
                absDiff < 20 -> getString(R.string.direction_forward)
                absDiff < 50 -> if (diff < 0) getString(R.string.direction_slight_left) else getString(R.string.direction_slight_right)
                absDiff < 130 -> if (diff < 0) getString(R.string.direction_left) else getString(R.string.direction_right)
                else -> getString(R.string.direction_back)
            }
        } else {
            val idx = ((bearingToDest + 22.5) / 45.0).toInt() % 8
            when (idx) {
                0 -> getString(R.string.direction_north)
                1 -> getString(R.string.direction_north_east)
                2 -> getString(R.string.direction_east)
                3 -> getString(R.string.direction_south_east)
                4 -> getString(R.string.direction_south)
                5 -> getString(R.string.direction_south_west)
                6 -> getString(R.string.direction_west)
                else -> getString(R.string.direction_north_west)
            }
        }
    }

    private fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters < 1000) {
            getString(R.string.navigation_distance_meters, distanceMeters)
        } else {
            val km = distanceMeters / 1000.0
            getString(R.string.navigation_distance_kilometers, String.format(LocaleRoot.LOCALE, "%.1f", km))
        }
    }

    private fun speakOnce(text: String) {
        val engine = announcer ?: CallAnnouncer(this).also { announcer = it }
        engine.speak(
            text = text,
            repeatCount = 1,
            rate = Prefs.getSpeechRate(this),
            volume = Prefs.getSpeechVolume(this),
            voiceName = Prefs.getVoiceName(this)
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt()
    }

    private fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    private fun normalizeBearing(value: Float): Float {
        var v = value % 360f
        if (v > 180f) v -= 360f
        if (v < -180f) v += 360f
        return v
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        createChannel(nm)
        val stopIntent = Intent(this, NavigationRouteService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.navigation_offline_guidance_label))
            .setContentText(getString(R.string.navigation_offline_guidance_started))
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private fun createChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.navigation_offline_guidance_label),
            NotificationManager.IMPORTANCE_MIN
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "blindroid_nav_route"
        private const val NOTIF_ID = 4202
        private const val ACTION_STOP = "blindroid_nav_route_stop"
        private const val ARRIVAL_DISTANCE_METERS = 20
        private const val GPX_POINT_REACHED_METERS = 15

        fun start(context: android.content.Context) {
            DiagnosticLog.log(context, "nav_route_start")
            ContextCompat.startForegroundService(
                context,
                Intent(context, NavigationRouteService::class.java)
            )
        }

        fun stop(context: android.content.Context) {
            DiagnosticLog.log(context, "nav_route_stop")
            context.stopService(Intent(context, NavigationRouteService::class.java))
        }
    }

    private object LocaleRoot {
        val LOCALE = java.util.Locale.US
    }
}
