package com.screenreaders.blindroid.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import java.text.Collator

object LauncherStore {
    private const val PREFS_NAME = "blindroid_launcher"
    private const val KEY_PINNED = "pinned"

    fun loadAllApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val currentPackage = context.packageName
        val list = resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            if (packageName == currentPackage) return@mapNotNull null
            val component = ComponentName(packageName, info.activityInfo.name)
            val label = info.loadLabel(pm)?.toString() ?: packageName
            val icon = info.loadIcon(pm)
            AppEntry(label, component, icon)
        }
        val collator = Collator.getInstance()
        return list.sortedWith(compareBy(collator) { it.label })
    }

    fun loadPinned(context: Context, allApps: List<AppEntry>): List<AppEntry> {
        val order = getPinnedOrder(context)
        if (order.isEmpty()) {
            val defaults = resolveDefaultPinned(context)
            if (defaults.isNotEmpty()) {
                savePinnedOrder(context, defaults)
            }
        }
        val set = allApps.associateBy { it.component.flattenToString() }
        return getPinnedOrder(context).mapNotNull { set[it] }
    }

    fun addPinned(context: Context, component: ComponentName): Boolean {
        val order = getPinnedOrder(context)
        val key = component.flattenToString()
        if (order.contains(key)) return false
        order.add(key)
        savePinnedOrder(context, order)
        return true
    }

    fun removePinned(context: Context, component: ComponentName): Boolean {
        val order = getPinnedOrder(context)
        val key = component.flattenToString()
        val removed = order.remove(key)
        if (removed) {
            savePinnedOrder(context, order)
        }
        return removed
    }

    private fun resolveDefaultPinned(context: Context): List<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
            Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        )
        val components = intents.mapNotNull { intent ->
            val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolved?.activityInfo?.let {
                ComponentName(it.packageName, it.name).flattenToString()
            }
        }
        return components.distinct()
    }

    private fun getPinnedOrder(context: Context): MutableList<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_PINNED, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split('|').filter { it.isNotBlank() }.toMutableList()
    }

    private fun savePinnedOrder(context: Context, order: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PINNED, order.joinToString("|")) .apply()
    }
}
