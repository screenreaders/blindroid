package com.screenreaders.blindroid.navigation

import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.screenreaders.blindroid.data.Prefs
import kotlin.math.cos

class OfflineImportWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val categories = Prefs.getNavigationCategories(applicationContext)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (categories.isEmpty()) return Result.failure()

        val baseUrl = Prefs.getNavigationOfflineBaseUrl(applicationContext).trim()
        if (baseUrl.isBlank()) return Result.failure()

        val bbox = resolveBbox() ?: return Result.retry()
        return try {
            val pois = GithubTileClient.fetchTiles(
                bbox = bbox,
                categories = categories.toSet(),
                config = GithubTileClient.Config(
                    baseUrl = baseUrl,
                    zoom = Prefs.getNavigationOfflineZoom(applicationContext),
                    maxTiles = 256
                )
            )
            NavigationPoiStore.replaceAll(applicationContext, pois)
            Prefs.setNavigationOfflineCount(applicationContext, pois.size)
            Prefs.setNavigationOfflineUpdated(applicationContext, System.currentTimeMillis())
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun resolveBbox(): GithubTileClient.Bbox? {
        return if (Prefs.getNavigationImportMode(applicationContext) == Prefs.NAV_IMPORT_MODE_MANUAL) {
            val minLat = Prefs.getNavigationImportMinLat(applicationContext).toDouble()
            val minLon = Prefs.getNavigationImportMinLon(applicationContext).toDouble()
            val maxLat = Prefs.getNavigationImportMaxLat(applicationContext).toDouble()
            val maxLon = Prefs.getNavigationImportMaxLon(applicationContext).toDouble()
            if (minLat == 0.0 && minLon == 0.0 && maxLat == 0.0 && maxLon == 0.0) {
                null
            } else {
                GithubTileClient.Bbox(
                    minLat = minOf(minLat, maxLat),
                    minLon = minOf(minLon, maxLon),
                    maxLat = maxOf(minLat, maxLat),
                    maxLon = maxOf(minLon, maxLon)
                )
            }
        } else {
            val location = getLastLocation() ?: return null
            val radius = Prefs.getNavigationImportRadius(applicationContext)
            val latDelta = radius / 111_000.0
            val lonDelta = radius / (111_000.0 * cos(Math.toRadians(location.latitude)).coerceAtLeast(0.1))
            GithubTileClient.Bbox(
                minLat = location.latitude - latDelta,
                minLon = location.longitude - lonDelta,
                maxLat = location.latitude + latDelta,
                maxLon = location.longitude + lonDelta
            )
        }
    }

    private fun getLastLocation(): android.location.Location? {
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = applicationContext.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }

    companion object {
        const val WORK_NAME = "blindroid_offline_import"
    }
}
