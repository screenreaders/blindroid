package com.screenreaders.blindroid.navigation

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream

data class RoutePoint(val lat: Double, val lon: Double)

object GpxRouteParser {
    fun parse(file: File): List<RoutePoint> {
        if (!file.exists()) return emptyList()
        FileInputStream(file).use { input ->
            return parse(input)
        }
    }

    fun parse(input: java.io.InputStream): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(input, null)
        }
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val name = parser.name ?: ""
                if (name == "trkpt" || name == "rtept") {
                    val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                    val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        points.add(RoutePoint(lat, lon))
                    }
                }
            }
            event = parser.next()
        }
        return points
    }
}
