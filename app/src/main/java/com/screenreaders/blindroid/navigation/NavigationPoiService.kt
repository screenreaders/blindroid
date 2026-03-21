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
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.diagnostics.DiagnosticLog
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class NavigationPoiService : Service(), LocationListener {
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val executor = Executors.newSingleThreadExecutor()
    private val running = AtomicBoolean(false)
    private var lastQueryTime = 0L
    private var lastQueryLocation: Location? = null
    private var lastSpokenTime = 0L
    private var lastSpokenLocation: Location? = null
    private var lastMotionLocation: Location? = null
    private var lastMotionTime = 0L
    private var lastHeading: Float? = null
    private var lastLocation: Location? = null
    private val announced = LinkedHashMap<String, Long>()
    private var tts: TextToSpeech? = null
    private var lastTrackWrite = 0L
    private var trackFile: java.io.File? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.forLanguageTag("pl-PL")
                tts?.setSpeechRate(Prefs.getSpeechRate(this))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Prefs.isNavigationTrackingEnabled(this) && !Prefs.isNavigationTrackLogEnabled(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        startTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTracking()
        executor.shutdown()
        tts?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTracking() {
        if (running.getAndSet(true)) return
        if (!hasLocationPermission()) {
            speakOnce(getString(R.string.navigation_tracking_missing_location))
            stopSelf()
            return
        }
        ensureTrackFile()
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(
                    provider,
                    10_000L,
                    10f,
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
        if (!running.getAndSet(false)) return
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
        }
    }

    override fun onLocationChanged(location: Location) {
        if (!running.get()) return
        if (!Prefs.isNavigationTrackingEnabled(this) && !Prefs.isNavigationTrackLogEnabled(this)) {
            stopSelf()
            return
        }
        updateHeading(location)
        if (Prefs.isNavigationMovingOnly(this) && !isMoving(location)) return
        maybeWriteTrack(location)
        val wantsPoi = Prefs.isNavigationTrackingEnabled(this)
        val source = Prefs.getNavigationPoiSource(this)
        val apiKey = Prefs.getNavigationApiKey(this)
        val categories = Prefs.getNavigationCategories(this)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (!wantsPoi || categories.isEmpty()) return
        if (source == Prefs.NAV_POI_SOURCE_GOOGLE && apiKey.isBlank()) return

        val now = System.currentTimeMillis()
        val minIntervalMs = Prefs.getNavigationSpeakIntervalSec(this).toLong() * 1000L
        val queryIntervalMs = minIntervalMs.coerceAtLeast(15_000L)
        val lastLoc = lastQueryLocation
        val moved = lastLoc == null || lastLoc.distanceTo(location) > 60f
        if (!moved && now - lastQueryTime < queryIntervalMs) return
        lastQueryTime = now
        lastQueryLocation = location
        executor.execute {
            when (source) {
                Prefs.NAV_POI_SOURCE_GOOGLE -> {
                    categories.forEach { type ->
                        fetchPlaces(type, location, apiKey)
                    }
                }
                Prefs.NAV_POI_SOURCE_OFFLINE -> {
                    fetchOfflinePois(location, categories)
                }
                Prefs.NAV_POI_SOURCE_OSM -> {
                    fetchOnlineOsm(location, categories)
                }
                Prefs.NAV_POI_SOURCE_HYBRID -> {
                    val spoke = fetchOfflinePois(location, categories)
                    if (!spoke) {
                        fetchOnlineOsm(location, categories)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun fetchPlaces(type: String, location: Location, apiKey: String) {
        val radius = Prefs.getNavigationRadius(this)
        val url = URL(
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${location.latitude},${location.longitude}" +
                "&radius=$radius&type=$type&key=$apiKey"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 5000
            requestMethod = "GET"
        }
        try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val results = json.optJSONArray("results") ?: return
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val placeId = obj.optString("place_id") ?: continue
                val name = obj.optString("name") ?: continue
                val geom = obj.optJSONObject("geometry")?.optJSONObject("location") ?: continue
                val lat = geom.optDouble("lat")
                val lng = geom.optDouble("lng")
                val distance = distanceMeters(location.latitude, location.longitude, lat, lng)
                val announceDistance = (radius * 0.6).toInt().coerceIn(30, 120)
                if (distance > announceDistance) continue
                if (isRecentlyAnnounced(placeId)) continue
                if (!canSpeakNow()) continue
                if (!passesMinDistance(location)) continue
                markAnnounced(placeId)
                speakOnce(buildPoiAnnouncement(name, type, lat, lng, location))
                break
            }
        } catch (_: Exception) {
            // ignore network errors
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchOfflinePois(location: Location, categories: List<String>): Boolean {
        val radius = Prefs.getNavigationRadius(this)
        val pois = NavigationPoiStore.queryNearby(
            this,
            location.latitude,
            location.longitude,
            radius,
            categories.toSet()
        ).sortedBy { distanceMeters(location.latitude, location.longitude, it.lat, it.lon) }
        if (pois.isEmpty()) return false
        for (poi in pois) {
            val distance = distanceMeters(location.latitude, location.longitude, poi.lat, poi.lon)
            val announceDistance = (radius * 0.6).toInt().coerceIn(30, 120)
            if (distance > announceDistance) continue
            if (isRecentlyAnnounced(poi.id)) continue
            if (!canSpeakNow()) continue
            if (!passesMinDistance(location)) continue
            markAnnounced(poi.id)
            speakOnce(buildPoiAnnouncement(poi.name, poi.type, poi.lat, poi.lon, location))
            return true
        }
        return false
    }

    private fun fetchOnlineOsm(location: Location, categories: List<String>) {
        val radius = Prefs.getNavigationRadius(this)
        val results = try {
            OverpassPoiClient.fetch(location.latitude, location.longitude, radius, categories.toSet())
        } catch (_: Exception) {
            emptyList()
        }
        if (results.isEmpty()) return
        try {
            NavigationPoiStore.upsertAll(this, results)
            Prefs.setNavigationOfflineUpdated(this, System.currentTimeMillis())
            Prefs.setNavigationOfflineCount(this, NavigationPoiStore.count(this))
        } catch (_: Exception) {
        }
        val sorted = results.sortedBy {
            distanceMeters(location.latitude, location.longitude, it.lat, it.lon)
        }
        for (poi in sorted) {
            val distance = distanceMeters(location.latitude, location.longitude, poi.lat, poi.lon)
            val announceDistance = (radius * 0.6).toInt().coerceIn(30, 120)
            if (distance > announceDistance) continue
            if (isRecentlyAnnounced(poi.id)) continue
            if (!canSpeakNow()) continue
            if (!passesMinDistance(location)) continue
            markAnnounced(poi.id)
            speakOnce(buildPoiAnnouncement(poi.name, poi.type, poi.lat, poi.lon, location))
            break
        }
    }

    private fun buildPoiAnnouncement(name: String, type: String, lat: Double, lon: Double, location: Location): String {
        val distance = distanceMeters(location.latitude, location.longitude, lat, lon)
        val distanceText = formatDistance(distance)
        val direction = buildRelativeDirection(location, lat, lon)
        val label = labelForType(type)
        return if (direction.isBlank()) {
            getString(R.string.navigation_poi_announce, name, label, distanceText)
        } else {
            getString(R.string.navigation_poi_announce_direction, name, label, distanceText, direction)
        }
    }

    private fun buildRelativeDirection(location: Location, lat: Double, lon: Double): String {
        val bearingTo = bearingBetween(location.latitude, location.longitude, lat, lon)
        val heading = lastHeading
        return if (heading != null) {
            val diff = normalizeBearing(bearingTo - heading)
            val absDiff = kotlin.math.abs(diff)
            when {
                absDiff < 20 -> getString(R.string.direction_forward)
                absDiff < 50 -> if (diff < 0) getString(R.string.direction_slight_left) else getString(R.string.direction_slight_right)
                absDiff < 130 -> if (diff < 0) getString(R.string.direction_left) else getString(R.string.direction_right)
                else -> getString(R.string.direction_back)
            }
        } else {
            val idx = ((bearingTo + 22.5) / 45.0).toInt() % 8
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
            getString(R.string.navigation_distance_kilometers, String.format(java.util.Locale.US, "%.1f", km))
        }
    }

    private fun labelForType(type: String): String {
        return when (type) {
            "restaurant" -> getString(R.string.navigation_category_restaurant)
            "cafe" -> getString(R.string.navigation_category_cafe)
            "pharmacy" -> getString(R.string.navigation_category_pharmacy)
            "hospital" -> getString(R.string.navigation_category_hospital)
            "bank" -> getString(R.string.navigation_category_bank)
            "atm" -> getString(R.string.navigation_category_atm)
            "gas_station" -> getString(R.string.navigation_category_fuel)
            "movie_theater" -> getString(R.string.navigation_category_cinema)
            "supermarket" -> getString(R.string.navigation_category_grocery)
            "police" -> getString(R.string.navigation_category_police)
            "parking" -> getString(R.string.navigation_category_parking)
            "lodging" -> getString(R.string.navigation_category_hotel)
            "bus_station" -> getString(R.string.navigation_category_bus_station)
            "train_station" -> getString(R.string.navigation_category_train_station)
            "subway_station" -> getString(R.string.navigation_category_subway_station)
            "school" -> getString(R.string.navigation_category_school)
            "university" -> getString(R.string.navigation_category_university)
            "park" -> getString(R.string.navigation_category_park)
            "post_office" -> getString(R.string.navigation_category_post_office)
            "library" -> getString(R.string.navigation_category_library)
            "place_of_worship" -> getString(R.string.navigation_category_worship)
            "shopping_mall" -> getString(R.string.navigation_category_mall)
            "gym" -> getString(R.string.navigation_category_gym)
            "doctor" -> getString(R.string.navigation_category_doctor)
            "dentist" -> getString(R.string.navigation_category_dentist)
            "bakery" -> getString(R.string.navigation_category_bakery)
            "airport" -> getString(R.string.navigation_category_airport)
            "tourist_attraction" -> getString(R.string.navigation_category_attraction)
            else -> type
        }
    }

    private fun isRecentlyAnnounced(placeId: String): Boolean {
        val now = System.currentTimeMillis()
        val last = announced[placeId] ?: return false
        return now - last < 30 * 60 * 1000L
    }

    private fun markAnnounced(placeId: String) {
        announced[placeId] = System.currentTimeMillis()
        if (announced.size > 120) {
            val iterator = announced.entries.iterator()
            repeat(20) {
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
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

    private fun speakOnce(text: String) {
        lastSpokenTime = System.currentTimeMillis()
        lastSpokenLocation = lastQueryLocation
        val volume = Prefs.getSpeechVolume(this)
        val announcer = CallAnnouncer(this)
        announcer.speak(
            text = text,
            repeatCount = 1,
            rate = Prefs.getSpeechRate(this),
            volume = volume,
            voiceName = Prefs.getVoiceName(this),
            onComplete = { announcer.shutdown() }
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun canSpeakNow(): Boolean {
        val now = System.currentTimeMillis()
        val minIntervalMs = Prefs.getNavigationSpeakIntervalSec(this).toLong() * 1000L
        return now - lastSpokenTime >= minIntervalMs
    }

    private fun passesMinDistance(location: Location): Boolean {
        val minDistance = Prefs.getNavigationMinDistance(this)
        val last = lastSpokenLocation ?: return true
        return last.distanceTo(location) >= minDistance
    }

    private fun isMoving(location: Location): Boolean {
        if (location.hasSpeed()) {
            return location.speed >= 0.6f
        }
        val now = SystemClock.elapsedRealtime()
        val prev = lastMotionLocation
        return if (prev == null) {
            lastMotionLocation = location
            lastMotionTime = now
            true
        } else {
            val distance = prev.distanceTo(location)
            val dt = (now - lastMotionTime).coerceAtLeast(1L)
            val speed = distance / (dt / 1000f)
            lastMotionLocation = location
            lastMotionTime = now
            speed >= 0.6f
        }
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

    private fun ensureTrackFile() {
        if (!Prefs.isNavigationTrackLogEnabled(this)) return
        if (trackFile != null) return
        val dir = java.io.File(filesDir, "tracks")
        if (!dir.exists()) dir.mkdirs()
        val name = "track_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.csv"
        val file = java.io.File(dir, name)
        file.writeText("timestamp,lat,lon,accuracy,speed\n")
        trackFile = file
        Prefs.setNavigationLastTrack(this, file.absolutePath)
    }

    private fun maybeWriteTrack(location: Location) {
        if (!Prefs.isNavigationTrackLogEnabled(this)) return
        ensureTrackFile()
        val now = System.currentTimeMillis()
        if (now - lastTrackWrite < 5000L) return
        lastTrackWrite = now
        val file = trackFile ?: return
        val line = buildString {
            append(now)
            append(',')
            append(location.latitude)
            append(',')
            append(location.longitude)
            append(',')
            append(location.accuracy)
            append(',')
            append(location.speed)
            append('\n')
        }
        executor.execute {
            try {
                file.appendText(line)
            } catch (_: Exception) {
            }
        }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        createChannel(nm)
        val stopIntent = Intent(this, NavigationPoiService::class.java).apply {
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
            .setContentTitle(getString(R.string.navigation_tracking_label))
            .setContentText(getString(R.string.navigation_tracking_started))
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStop)
            .build()
    }

    private fun createChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.navigation_tracking_label),
            NotificationManager.IMPORTANCE_MIN
        )
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "blindroid_nav_poi"
        private const val NOTIF_ID = 4201
        private const val ACTION_STOP = "blindroid_nav_stop"

        fun start(context: android.content.Context) {
            DiagnosticLog.log(context, "nav_poi_start")
            ContextCompat.startForegroundService(
                context,
                Intent(context, NavigationPoiService::class.java)
            )
        }

        fun stop(context: android.content.Context) {
            DiagnosticLog.log(context, "nav_poi_stop")
            context.stopService(Intent(context, NavigationPoiService::class.java))
        }
    }
}
