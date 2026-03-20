package com.screenreaders.blindroid.navigation

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OverpassPoiClient {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    fun fetch(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        selectedTypes: Set<String>
    ): List<OfflinePoi> {
        val query = buildQuery(lat, lon, radiusMeters, selectedTypes)
        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 9000
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
        }
        return try {
            conn.outputStream.use { stream ->
                stream.write("data=${encode(query)}".toByteArray(Charsets.UTF_8))
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            parse(text, selectedTypes)
        } finally {
            conn.disconnect()
        }
    }

    private fun buildQuery(
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        selectedTypes: Set<String>
    ): String {
        val targets = if (selectedTypes.isEmpty()) {
            categoryToTags.keys
        } else {
            selectedTypes
        }
        val body = buildString {
            for (type in targets) {
                val tags = categoryToTags[type] ?: continue
                for ((key, value) in tags) {
                    append("  nwr[\"").append(key).append("\"=\"").append(value).append("\"]")
                    append("(around:").append(radiusMeters).append(",").append(lat).append(",").append(lon).append(");\n")
                }
            }
        }
        return "[out:json][timeout:25];\n(\n$body);\nout center tags;"
    }

    private fun parse(text: String, selectedTypes: Set<String>): List<OfflinePoi> {
        val json = JSONObject(text)
        val elements = json.optJSONArray("elements") ?: return emptyList()
        val result = mutableListOf<OfflinePoi>()
        for (i in 0 until elements.length()) {
            val obj = elements.optJSONObject(i) ?: continue
            val tags = obj.optJSONObject("tags") ?: continue
            val type = mapTagsToType(tags) ?: continue
            if (selectedTypes.isNotEmpty() && !selectedTypes.contains(type)) continue
            val name = tags.optString("name").takeIf { it.isNotBlank() } ?: type
            val lat = obj.optDouble("lat", Double.NaN).let { value ->
                if (!value.isNaN()) value else obj.optJSONObject("center")?.optDouble("lat", Double.NaN) ?: Double.NaN
            }
            val lon = obj.optDouble("lon", Double.NaN).let { value ->
                if (!value.isNaN()) value else obj.optJSONObject("center")?.optDouble("lon", Double.NaN) ?: Double.NaN
            }
            if (lat.isNaN() || lon.isNaN()) continue
            val id = "${obj.optString("type")}_${obj.optLong("id")}"
            result.add(OfflinePoi(id, name, type, lat, lon))
        }
        return result
    }

    private fun mapTagsToType(tags: JSONObject): String? {
        val amenity = tags.optString("amenity")
        val shop = tags.optString("shop")
        val tourism = tags.optString("tourism")
        val railway = tags.optString("railway")
        val leisure = tags.optString("leisure")
        val aeroway = tags.optString("aeroway")
        return when {
            amenity == "restaurant" -> "restaurant"
            amenity == "cafe" -> "cafe"
            amenity == "pharmacy" -> "pharmacy"
            amenity == "hospital" -> "hospital"
            amenity == "bank" -> "bank"
            amenity == "atm" -> "atm"
            amenity == "fuel" -> "gas_station"
            amenity == "cinema" -> "movie_theater"
            shop == "supermarket" -> "supermarket"
            amenity == "police" -> "police"
            amenity == "parking" -> "parking"
            tourism == "hotel" -> "lodging"
            tourism == "guest_house" -> "lodging"
            amenity == "bus_station" -> "bus_station"
            railway == "station" -> "train_station"
            railway == "subway_entrance" -> "subway_station"
            railway == "subway_station" -> "subway_station"
            amenity == "school" -> "school"
            amenity == "university" -> "university"
            leisure == "park" -> "park"
            amenity == "post_office" -> "post_office"
            amenity == "library" -> "library"
            amenity == "place_of_worship" -> "place_of_worship"
            shop == "mall" -> "shopping_mall"
            leisure == "fitness_centre" -> "gym"
            amenity == "doctors" -> "doctor"
            amenity == "dentist" -> "dentist"
            shop == "bakery" -> "bakery"
            aeroway == "aerodrome" -> "airport"
            tourism == "attraction" -> "tourist_attraction"
            else -> null
        }
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8")

    private val categoryToTags: Map<String, List<Pair<String, String>>> = mapOf(
        "restaurant" to listOf("amenity" to "restaurant"),
        "cafe" to listOf("amenity" to "cafe"),
        "pharmacy" to listOf("amenity" to "pharmacy"),
        "hospital" to listOf("amenity" to "hospital"),
        "bank" to listOf("amenity" to "bank"),
        "atm" to listOf("amenity" to "atm"),
        "gas_station" to listOf("amenity" to "fuel"),
        "movie_theater" to listOf("amenity" to "cinema"),
        "supermarket" to listOf("shop" to "supermarket"),
        "police" to listOf("amenity" to "police"),
        "parking" to listOf("amenity" to "parking"),
        "lodging" to listOf("tourism" to "hotel", "tourism" to "guest_house"),
        "bus_station" to listOf("amenity" to "bus_station"),
        "train_station" to listOf("railway" to "station"),
        "subway_station" to listOf("railway" to "subway_entrance", "railway" to "subway_station"),
        "school" to listOf("amenity" to "school"),
        "university" to listOf("amenity" to "university"),
        "park" to listOf("leisure" to "park"),
        "post_office" to listOf("amenity" to "post_office"),
        "library" to listOf("amenity" to "library"),
        "place_of_worship" to listOf("amenity" to "place_of_worship"),
        "shopping_mall" to listOf("shop" to "mall"),
        "gym" to listOf("leisure" to "fitness_centre"),
        "doctor" to listOf("amenity" to "doctors"),
        "dentist" to listOf("amenity" to "dentist"),
        "bakery" to listOf("shop" to "bakery"),
        "airport" to listOf("aeroway" to "aerodrome"),
        "tourist_attraction" to listOf("tourism" to "attraction")
    )
}
