package com.screenreaders.blindroid.navigation

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TrackExporter {
    fun csvToGpx(context: Context, csvFile: File): File? {
        if (!csvFile.exists()) return null
        val lines = csvFile.readLines()
        if (lines.size <= 1) return null
        val trackDir = File(context.filesDir, "tracks")
        if (!trackDir.exists()) trackDir.mkdirs()
        val out = File(trackDir, csvFile.nameWithoutExtension + ".gpx")
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("""<gpx version="1.1" creator="Blindroid" xmlns="http://www.topografix.com/GPX/1/1">""")
        sb.appendLine("<trk>")
        sb.appendLine("<name>Blindroid track</name>")
        sb.appendLine("<trkseg>")
        for (i in 1 until lines.size) {
            val parts = lines[i].split(',')
            if (parts.size < 5) continue
            val timeMs = parts[0].toLongOrNull() ?: continue
            val lat = parts[1].toDoubleOrNull() ?: continue
            val lon = parts[2].toDoubleOrNull() ?: continue
            val acc = parts[3].toFloatOrNull() ?: 0f
            val speed = parts[4].toFloatOrNull() ?: 0f
            val time = formatTime(timeMs)
            sb.append("""<trkpt lat="$lat" lon="$lon">""").append('\n')
            sb.append("<time>").append(time).append("</time>").append('\n')
            sb.append("<extensions>")
                .append("<accuracy>").append(acc).append("</accuracy>")
                .append("<speed>").append(speed).append("</speed>")
                .append("</extensions>")
                .append('\n')
            sb.append("</trkpt>").append('\n')
        }
        sb.appendLine("</trkseg>")
        sb.appendLine("</trk>")
        sb.appendLine("</gpx>")
        out.writeText(sb.toString())
        return out
    }

    private fun formatTime(ms: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return fmt.format(Date(ms))
    }
}
