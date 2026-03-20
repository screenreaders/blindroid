package com.screenreaders.blindroid.navigation

import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max
import kotlin.math.min

object TyflomapClient {
    private const val WMS_URL = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/mapy/WMS/SkorowidzMapTyflologicznych"

    fun findMapLink(lat: Double, lon: Double): String? {
        val layer = resolveLayerName() ?: "SkorowidzMapTyflologicznych"
        val d = 0.002
        val minLat = max(-90.0, lat - d)
        val maxLat = min(90.0, lat + d)
        val minLon = max(-180.0, lon - d)
        val maxLon = min(180.0, lon + d)
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"
        val url = "$WMS_URL?SERVICE=WMS&REQUEST=GetFeatureInfo&VERSION=1.3.0" +
            "&CRS=EPSG:4326&BBOX=$bbox&WIDTH=101&HEIGHT=101&I=50&J=50" +
            "&LAYERS=$layer&QUERY_LAYERS=$layer&INFO_FORMAT=text/html"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 7000
        return try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            extractFirstUrl(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveLayerName(): String? {
        val url = "$WMS_URL?SERVICE=WMS&REQUEST=GetCapabilities"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 7000
        return try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val regex = Regex("<Name>([^<]+)</Name>")
            val names = regex.findAll(text).map { it.groupValues[1] }.toList()
            names.firstOrNull { it.contains("SkorowidzMapTyflologicznych", ignoreCase = true) }
                ?: names.firstOrNull()
        } finally {
            conn.disconnect()
        }
    }

    private fun extractFirstUrl(text: String): String? {
        val regex = Regex("https?://[^\\s\"'<>]+")
        return regex.find(text)?.value
    }
}
