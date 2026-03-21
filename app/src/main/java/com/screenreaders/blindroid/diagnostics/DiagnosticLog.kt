package com.screenreaders.blindroid.diagnostics

import android.content.Context
import com.screenreaders.blindroid.data.Prefs
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val PREFS = "blindroid_diag"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 30
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("pl-PL"))

    fun log(context: Context, message: String) {
        if (!Prefs.isDiagnosticsEnabled(context)) return
        val stamp = formatter.format(Date())
        val line = "$stamp $message"
        synchronized(this) {
            val list = readEvents(context).toMutableList()
            list.add(line)
            while (list.size > MAX_EVENTS) {
                list.removeAt(0)
            }
            writeEvents(context, list)
        }
    }

    fun dump(context: Context): List<String> {
        if (!Prefs.isDiagnosticsEnabled(context)) return emptyList()
        return synchronized(this) { readEvents(context) }
    }

    fun clear(context: Context) {
        synchronized(this) { writeEvents(context, emptyList()) }
    }

    private fun readEvents(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<String>(arr.length())
            for (i in 0 until arr.length()) {
                out.add(arr.optString(i))
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writeEvents(context: Context, items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EVENTS, arr.toString()).apply()
    }
}
