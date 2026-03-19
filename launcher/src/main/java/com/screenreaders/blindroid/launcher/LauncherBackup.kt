package com.screenreaders.blindroid.launcher

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object LauncherBackup {
    private const val PREFS_LAUNCHER = "blindroid_launcher"
    private const val PREFS_UI = "blindroid_launcher_prefs"

    fun writeBackup(context: Context, uri: Uri): Boolean {
        return try {
            val json = export(context)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun readBackup(context: Context, uri: Uri): Boolean {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: return false
            import(context, content)
        } catch (_: Exception) {
            false
        }
    }

    fun export(context: Context): String {
        val root = JSONObject()
        val prefsArray = JSONArray()
        listOf(PREFS_LAUNCHER, PREFS_UI).forEach { name ->
            val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val entries = JSONArray()
            prefs.all.forEach { (key, value) ->
                val entry = JSONObject()
                entry.put("k", key)
                when (value) {
                    is String -> {
                        entry.put("t", "s")
                        entry.put("v", value)
                    }
                    is Int -> {
                        entry.put("t", "i")
                        entry.put("v", value)
                    }
                    is Long -> {
                        entry.put("t", "l")
                        entry.put("v", value)
                    }
                    is Boolean -> {
                        entry.put("t", "b")
                        entry.put("v", value)
                    }
                    is Float -> {
                        entry.put("t", "f")
                        entry.put("v", value)
                    }
                }
                entries.put(entry)
            }
            val obj = JSONObject()
            obj.put("name", name)
            obj.put("entries", entries)
            prefsArray.put(obj)
        }
        root.put("prefs", prefsArray)
        return root.toString()
    }

    fun import(context: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val prefsArray = root.optJSONArray("prefs") ?: return false
            for (i in 0 until prefsArray.length()) {
                val obj = prefsArray.getJSONObject(i)
                val name = obj.getString("name")
                val entries = obj.getJSONArray("entries")
                val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
                val editor = prefs.edit().clear()
                for (j in 0 until entries.length()) {
                    val entry = entries.getJSONObject(j)
                    val key = entry.getString("k")
                    val type = entry.getString("t")
                    when (type) {
                        "s" -> editor.putString(key, entry.optString("v", ""))
                        "i" -> editor.putInt(key, entry.getInt("v"))
                        "l" -> editor.putLong(key, entry.getLong("v"))
                        "b" -> editor.putBoolean(key, entry.getBoolean("v"))
                        "f" -> editor.putFloat(key, entry.getDouble("v").toFloat())
                    }
                }
                editor.apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
