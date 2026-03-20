package com.screenreaders.blindroid.diagnostics

import com.screenreaders.blindroid.data.Prefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object DiagnosticLog {
    private const val MAX_EVENTS = 30
    private val events = ConcurrentLinkedDeque<String>()
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale("pl", "PL"))

    fun log(context: android.content.Context, message: String) {
        if (!Prefs.isDiagnosticsEnabled(context)) return
        val stamp = formatter.format(Date())
        val line = "$stamp $message"
        events.addLast(line)
        while (events.size > MAX_EVENTS) {
            events.pollFirst()
        }
    }

    fun dump(context: android.content.Context): List<String> {
        if (!Prefs.isDiagnosticsEnabled(context)) return emptyList()
        return events.toList()
    }

    fun clear() {
        events.clear()
    }
}
