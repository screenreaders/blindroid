package com.screenreaders.blindroid.launcher

import android.content.Context

object LauncherProfiles {
    private const val PREFS_NAME = "blindroid_launcher_profiles"

    fun listProfiles(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.keys.sorted()
    }

    fun saveProfile(context: Context, name: String): Boolean {
        return try {
            val json = LauncherBackup.export(context)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(name.trim(), json)
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun loadProfile(context: Context, name: String): Boolean {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(name, null) ?: return false
            LauncherBackup.import(context, json)
        } catch (_: Exception) {
            false
        }
    }

    fun deleteProfile(context: Context, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(name)
            .apply()
    }
}
