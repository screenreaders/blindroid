package com.screenreaders.blindroid.update

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val API_URL = "https://raw.githubusercontent.com/screenreaders/blindroid/main/update.json"

    data class UpdateInfo(
        val version: String,
        val apkUrl: String?,
        val notes: String?,
        val sha256: String?,
        val sizeBytes: Long?
    )

    fun fetchLatest(): UpdateInfo? {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connect()

        return connection.inputStream.bufferedReader().use { reader ->
            val body = reader.readText()
            if (body.isBlank()) return null
            val json = JSONObject(body)
            val version = json.optString("tag_name").ifBlank { json.optString("name") }
            if (version.isBlank()) return null
            val notes = json.optString("body").ifBlank { null }
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            var sha256: String? = null
            var sizeBytes: Long? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    if (name.endsWith(".apk") && url.startsWith("https://")) {
                        apkUrl = url
                        sha256 = asset.optString("sha256").ifBlank { null }
                        val sizeValue = asset.optLong("size", -1L)
                        sizeBytes = if (sizeValue > 0L) sizeValue else null
                        break
                    }
                }
            }
            UpdateInfo(
                version = version,
                apkUrl = apkUrl,
                notes = notes,
                sha256 = sha256,
                sizeBytes = sizeBytes
            )
        }
    }

    fun isNewer(current: String, latest: String): Boolean {
        val currentParts = parseVersionParts(current)
        val latestParts = parseVersionParts(latest)
        val max = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until max) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return current != latest
    }

    private fun parseVersionParts(version: String): List<Int> {
        val digits = version.replace(Regex("[^0-9.]"), "")
        if (digits.isBlank()) return emptyList()
        return digits.split(".").mapNotNull { it.toIntOrNull() }
    }
}
