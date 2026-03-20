package com.screenreaders.blindroid.navigation

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tan

object GithubTileClient {
    data class Bbox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)

    data class Config(
        val baseUrl: String,
        val zoom: Int,
        val maxTiles: Int = 16
    )

    data class Tile(val x: Int, val y: Int)

    fun listTiles(bbox: Bbox, zoom: Int): List<Tile> {
        return tilesForBbox(bbox, zoom).map { Tile(it.first, it.second) }
    }

    fun fetchTiles(
        bbox: Bbox,
        categories: Set<String>,
        config: Config
    ): List<OfflinePoi> {
        val tiles = listTiles(bbox, config.zoom)
        if (tiles.size > config.maxTiles) {
            throw IllegalArgumentException("too_many_tiles")
        }
        return fetchTilesInternal(tiles, categories, config, null)
    }

    fun fetchTilesWithProgress(
        bbox: Bbox,
        categories: Set<String>,
        config: Config,
        onProgress: (done: Int, total: Int) -> Unit
    ): List<OfflinePoi> {
        val tiles = listTiles(bbox, config.zoom)
        if (tiles.size > config.maxTiles) {
            throw IllegalArgumentException("too_many_tiles")
        }
        return fetchTilesInternal(tiles, categories, config, onProgress)
    }

    fun fetchTile(url: String, categories: Set<String>): List<OfflinePoi>? {
        val text = download(url) ?: return null
        return parsePois(text, categories)
    }

    private fun download(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 9000
            requestMethod = "GET"
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchTilesInternal(
        tiles: List<Tile>,
        categories: Set<String>,
        config: Config,
        onProgress: ((Int, Int) -> Unit)?
    ): List<OfflinePoi> {
        val result = ArrayList<OfflinePoi>()
        var fetchedTiles = 0
        for ((index, tile) in tiles.withIndex()) {
            val url = "${config.baseUrl.trimEnd('/')}/${config.zoom}/${tile.x}/${tile.y}.json"
            val pois = fetchTile(url, categories)
            if (pois != null) {
                fetchedTiles++
                result.addAll(pois)
            }
            onProgress?.invoke(index + 1, tiles.size)
        }
        if (fetchedTiles == 0) {
            throw IllegalArgumentException("no_tiles")
        }
        return result
    }

    private fun parsePois(text: String, categories: Set<String>): List<OfflinePoi> {
        val json = JSONObject(text)
        val result = mutableListOf<OfflinePoi>()
        val features = json.optJSONArray("features")
        if (features != null) {
            result.addAll(parseGeoJson(features, categories))
            return result
        }
        val pois = json.optJSONArray("pois") ?: JSONArray()
        for (i in 0 until pois.length()) {
            val obj = pois.optJSONObject(i) ?: continue
            val id = obj.optString("id").ifBlank { "p_${i}_${obj.optDouble("lat")}_${obj.optDouble("lon")}" }
            val name = obj.optString("name").ifBlank { "Punkt" }
            val type = obj.optString("type").ifBlank { "" }
            if (categories.isNotEmpty() && type.isNotBlank() && !categories.contains(type)) continue
            val lat = obj.optDouble("lat", Double.NaN)
            val lon = obj.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            result.add(OfflinePoi(id, name, type, lat, lon))
        }
        return result
    }

    private fun parseGeoJson(features: JSONArray, categories: Set<String>): List<OfflinePoi> {
        val result = mutableListOf<OfflinePoi>()
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val geometry = feature.optJSONObject("geometry") ?: continue
            if (geometry.optString("type") != "Point") continue
            val coords = geometry.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val lon = coords.optDouble(0, Double.NaN)
            val lat = coords.optDouble(1, Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            val props = feature.optJSONObject("properties") ?: JSONObject()
            val name = props.optString("name").ifBlank { "Punkt" }
            val type = props.optString("type").ifBlank { "" }
            if (categories.isNotEmpty() && type.isNotBlank() && !categories.contains(type)) continue
            val id = feature.optString("id").ifBlank { "g_${i}_${lat}_$lon" }
            result.add(OfflinePoi(id, name, type, lat, lon))
        }
        return result
    }

    private fun tilesForBbox(bbox: Bbox, zoom: Int): List<Pair<Int, Int>> {
        val minLat = bbox.minLat.coerceIn(-85.0, 85.0)
        val maxLat = bbox.maxLat.coerceIn(-85.0, 85.0)
        val minLon = bbox.minLon.coerceIn(-180.0, 180.0)
        val maxLon = bbox.maxLon.coerceIn(-180.0, 180.0)
        val minX = lonToTileX(minLon, zoom)
        val maxX = lonToTileX(maxLon, zoom)
        val minY = latToTileY(maxLat, zoom)
        val maxY = latToTileY(minLat, zoom)
        val tiles = mutableListOf<Pair<Int, Int>>()
        for (x in min(minX, maxX)..max(minX, maxX)) {
            for (y in min(minY, maxY)..max(minY, maxY)) {
                tiles.add(x to y)
            }
        }
        return tiles
    }

    private fun lonToTileX(lon: Double, zoom: Int): Int {
        val n = 2.0.pow(zoom.toDouble())
        return ((lon + 180.0) / 360.0 * n).toInt()
    }

    private fun latToTileY(lat: Double, zoom: Int): Int {
        val n = 2.0.pow(zoom.toDouble())
        val rad = Math.toRadians(lat)
        val y = (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / Math.PI) / 2.0 * n
        return y.toInt()
    }
}
