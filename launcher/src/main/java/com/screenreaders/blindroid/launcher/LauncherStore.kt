package com.screenreaders.blindroid.launcher

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import java.text.Collator
import java.util.UUID

data class FolderInfo(val id: String, val label: String)

object LauncherStore {
    private const val PREFS_NAME = "blindroid_launcher"
    private const val KEY_PAGES = "pages"
    private const val KEY_HOTSEAT = "hotseat"
    private const val KEY_WIDGETS = "widgets"
    private const val KEY_HIDDEN_APPS = "hidden_apps"
    private const val KEY_LAUNCH_PREFIX = "launch_"
    private const val KEY_LAUNCH_LAST_PREFIX = "launch_last_"
    private const val KEY_LAUNCH_BUCKET_PREFIX = "launch_bucket_"
    private const val KEY_WIDGET_SIZE_PREFIX = "widget_size_"
    private const val FOLDER_PREFIX = "folder_"
    private const val FOLDER_LABEL_SUFFIX = "_label"
    private const val PAGE_COUNT = 3

    private const val TYPE_APP = "a"
    private const val TYPE_FOLDER = "f"
    private const val TYPE_SHORTCUT = "s"

    private const val MODULE_PREFS_NAME = "blindroid_prefs"
    private const val KEY_MODULE_SHORTCUTS = "module_shortcuts"

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

    fun getHiddenAppKeys(context: Context): MutableSet<String> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_HIDDEN_APPS, "") ?: ""
        if (raw.isBlank()) return mutableSetOf()
        return raw.split('|').filter { it.isNotBlank() }.toMutableSet()
    }

    fun isAppHidden(context: Context, component: ComponentName): Boolean {
        return getHiddenAppKeys(context).contains(component.flattenToString())
    }

    fun setAppHidden(context: Context, component: ComponentName, hidden: Boolean) {
        val prefs = prefs(context)
        val set = getHiddenAppKeys(context)
        val key = component.flattenToString()
        if (hidden) {
            set.add(key)
        } else {
            set.remove(key)
        }
        prefs.edit().putString(KEY_HIDDEN_APPS, set.joinToString("|")).apply()
    }

    fun clearHiddenApps(context: Context) {
        prefs(context).edit().remove(KEY_HIDDEN_APPS).apply()
    }

    fun loadPages(context: Context, allApps: List<AppEntry>): List<List<HomeItem>> {
        val pages = ensurePages(context)
        return pages.map { list -> decodeItems(context, list, allApps) }
    }

    fun loadHotseat(context: Context, allApps: List<AppEntry>): List<HomeItem> {
        val prefs = prefs(context)
        var hotseat = decodeList(prefs.getString(KEY_HOTSEAT, "") ?: "")
        if (hotseat.isEmpty()) {
            val defaults = resolveDefaultPinned(context)
            if (defaults.isNotEmpty()) {
                hotseat = defaults.map { "${TYPE_APP}:$it" }.toMutableList()
                saveList(prefs, KEY_HOTSEAT, hotseat)
            }
        }
        return decodeItems(context, hotseat, allApps)
    }

    fun syncModuleShortcuts(context: Context, enabled: Boolean) {
        val pages = ensurePages(context)
        val hotseat = loadHotseatKeys(context)
        val shortcutKeys = ModuleShortcuts.shortcutKeys()
        var changed = false

        if (!enabled) {
            pages.forEach { page ->
                val removed = page.removeAll { it.startsWith("$TYPE_SHORTCUT:") }
                if (removed) changed = true
            }
            val removedHotseat = hotseat.removeAll { it.startsWith("$TYPE_SHORTCUT:") }
            if (removedHotseat) changed = true
            if (changed) {
                savePages(context, pages)
                saveList(prefs(context), KEY_HOTSEAT, hotseat)
            }
            return
        }

        val existing = pages.flatten().toMutableSet()
        existing.addAll(hotseat)
        val target = pages.firstOrNull() ?: return
        var insertIndex = 0
        shortcutKeys.forEach { key ->
            if (!existing.contains(key)) {
                target.add(insertIndex, key)
                insertIndex++
                existing.add(key)
                changed = true
            } else {
                val idx = target.indexOf(key)
                if (idx >= 0) {
                    insertIndex = idx + 1
                }
            }
        }
        if (changed) {
            savePages(context, pages)
        }
    }

    fun addToPage(context: Context, pageIndex: Int, component: ComponentName): Boolean {
        val pages = ensurePages(context)
        val key = "${TYPE_APP}:${component.flattenToString()}"
        if (containsItem(pages, key) || loadHotseatKeys(context).contains(key)) return false
        val target = pages.getOrNull(pageIndex) ?: return false
        target.add(key)
        savePages(context, pages)
        return true
    }

    fun addToHotseat(context: Context, component: ComponentName): Boolean {
        val pages = ensurePages(context)
        val key = "${TYPE_APP}:${component.flattenToString()}"
        if (containsItem(pages, key) || loadHotseatKeys(context).contains(key)) return false
        val prefs = prefs(context)
        val hotseat = loadHotseatKeys(context)
        hotseat.add(key)
        saveList(prefs, KEY_HOTSEAT, hotseat)
        return true
    }

    fun removeFromPage(context: Context, pageIndex: Int, itemKey: String) {
        val pages = ensurePages(context)
        val target = pages.getOrNull(pageIndex) ?: return
        target.remove(itemKey)
        savePages(context, pages)
    }

    fun removeFromHotseat(context: Context, itemKey: String) {
        val prefs = prefs(context)
        val hotseat = loadHotseatKeys(context)
        hotseat.remove(itemKey)
        saveList(prefs, KEY_HOTSEAT, hotseat)
    }

    fun moveItemInPage(context: Context, pageIndex: Int, from: Int, to: Int) {
        val pages = ensurePages(context)
        val target = pages.getOrNull(pageIndex) ?: return
        if (from !in target.indices || to !in target.indices) return
        val item = target.removeAt(from)
        target.add(to, item)
        savePages(context, pages)
    }

    fun moveItemInHotseat(context: Context, from: Int, to: Int) {
        val prefs = prefs(context)
        val hotseat = loadHotseatKeys(context)
        if (from !in hotseat.indices || to !in hotseat.indices) return
        val item = hotseat.removeAt(from)
        hotseat.add(to, item)
        saveList(prefs, KEY_HOTSEAT, hotseat)
    }

    fun createFolderOnPage(
        context: Context,
        pageIndex: Int,
        first: ComponentName,
        second: ComponentName
    ): String {
        val pages = ensurePages(context)
        val target = pages.getOrNull(pageIndex) ?: return ""
        val firstKey = "${TYPE_APP}:${first.flattenToString()}"
        val secondKey = "${TYPE_APP}:${second.flattenToString()}"
        target.remove(firstKey)
        target.remove(secondKey)

        val folderId = UUID.randomUUID().toString()
        val folderKey = "${TYPE_FOLDER}:$folderId"
        target.add(folderKey)

        saveFolderItems(context, folderId, listOf(first.flattenToString(), second.flattenToString()))
        saveFolderLabel(context, folderId, "Folder")
        savePages(context, pages)
        return folderId
    }

    fun addToFolder(context: Context, folderId: String, component: ComponentName): Boolean {
        val items = loadFolderItems(context, folderId)
        val key = component.flattenToString()
        if (items.contains(key)) return false
        items.add(key)
        saveFolderItems(context, folderId, items)
        return true
    }

    fun removeFromFolder(context: Context, folderId: String, component: ComponentName): Boolean {
        val items = loadFolderItems(context, folderId)
        val key = component.flattenToString()
        val removed = items.remove(key)
        if (removed) {
            saveFolderItems(context, folderId, items)
        }
        return removed
    }

    fun getFolderLabel(context: Context, folderId: String): String {
        val prefs = prefs(context)
        return prefs.getString(folderLabelKey(folderId), "Folder") ?: "Folder"
    }

    fun setFolderLabel(context: Context, folderId: String, label: String) {
        val safeLabel = label.trim().ifBlank { "Folder" }
        saveFolderLabel(context, folderId, safeLabel)
    }

    fun listFolders(context: Context): List<FolderInfo> {
        val pages = ensurePages(context)
        val folderIds = pages.flatten().mapNotNull { key ->
            val parts = key.split(":", limit = 2)
            if (parts.size == 2 && parts[0] == TYPE_FOLDER) parts[1] else null
        }.distinct()
        return folderIds.map { FolderInfo(it, getFolderLabel(context, it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun getFolderItems(context: Context, folderId: String, allApps: List<AppEntry>): List<HomeItem> {
        val items = loadFolderItems(context, folderId)
        return decodeItems(context, items.map { "${TYPE_APP}:$it" }.toMutableList(), allApps)
    }

    fun getFolderAppEntries(context: Context, folderId: String, allApps: List<AppEntry>): List<AppEntry> {
        val items = loadFolderItems(context, folderId)
        val appMap = allApps.associateBy { it.component.flattenToString() }
        return items.mapNotNull { appMap[it] }
    }

    fun getFolderComponents(context: Context, folderId: String): List<String> {
        return loadFolderItems(context, folderId)
    }

    fun removeFolderFromPage(
        context: Context,
        pageIndex: Int,
        folderId: String,
        restoreItems: Boolean
    ) {
        val pages = ensurePages(context)
        val target = pages.getOrNull(pageIndex) ?: return
        val folderKey = "${TYPE_FOLDER}:$folderId"
        if (!target.remove(folderKey)) return
        if (restoreItems) {
            val hotseat = loadHotseatKeys(context)
            val existing = pages.flatten().toMutableSet()
            existing.addAll(hotseat)
            val items = loadFolderItems(context, folderId)
            items.forEach { component ->
                val key = "${TYPE_APP}:$component"
                if (!existing.contains(key)) {
                    target.add(key)
                    existing.add(key)
                }
            }
        }
        savePages(context, pages)
        deleteFolder(context, folderId)
    }

    fun deleteFolder(context: Context, folderId: String) {
        val prefs = prefs(context)
        prefs.edit()
            .remove(folderKey(folderId))
            .remove(folderLabelKey(folderId))
            .apply()
    }

    fun getWidgetIds(context: Context): List<Int> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_WIDGETS, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split('|').mapNotNull { it.toIntOrNull() }
    }

    fun addWidgetId(context: Context, widgetId: Int) {
        val prefs = prefs(context)
        val list = getWidgetIds(context).toMutableList()
        if (!list.contains(widgetId)) {
            list.add(widgetId)
            prefs.edit().putString(KEY_WIDGETS, list.joinToString("|")) .apply()
        }
    }

    fun removeWidgetId(context: Context, widgetId: Int) {
        val prefs = prefs(context)
        val list = getWidgetIds(context).toMutableList()
        if (list.remove(widgetId)) {
            prefs.edit().putString(KEY_WIDGETS, list.joinToString("|")) .apply()
        }
    }

    fun moveWidget(context: Context, widgetId: Int, delta: Int) {
        val prefs = prefs(context)
        val list = getWidgetIds(context).toMutableList()
        val index = list.indexOf(widgetId)
        if (index == -1) return
        val newIndex = (index + delta).coerceIn(0, list.size - 1)
        if (newIndex == index) return
        list.removeAt(index)
        list.add(newIndex, widgetId)
        prefs.edit().putString(KEY_WIDGETS, list.joinToString("|")) .apply()
    }

    fun moveWidgetTo(context: Context, widgetId: Int, index: Int) {
        val prefs = prefs(context)
        val list = getWidgetIds(context).toMutableList()
        val current = list.indexOf(widgetId)
        if (current == -1) return
        val target = index.coerceIn(0, list.size - 1)
        if (current == target) return
        list.removeAt(current)
        list.add(target, widgetId)
        prefs.edit().putString(KEY_WIDGETS, list.joinToString("|")) .apply()
    }

    fun setWidgetSize(context: Context, widgetId: Int, width: Int, height: Int) {
        val prefs = prefs(context)
        prefs.edit()
            .putString("$KEY_WIDGET_SIZE_PREFIX$widgetId", "$width,$height")
            .apply()
    }

    fun getWidgetSize(context: Context, widgetId: Int): Pair<Int, Int>? {
        val prefs = prefs(context)
        val raw = prefs.getString("$KEY_WIDGET_SIZE_PREFIX$widgetId", null) ?: return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val w = parts[0].toIntOrNull() ?: return null
        val h = parts[1].toIntOrNull() ?: return null
        return w to h
    }

    fun recordLaunch(context: Context, component: ComponentName) {
        val prefs = prefs(context)
        val componentKey = component.flattenToString()
        val key = "$KEY_LAUNCH_PREFIX$componentKey"
        val lastKey = "$KEY_LAUNCH_LAST_PREFIX$componentKey"
        val bucketKey = "$KEY_LAUNCH_BUCKET_PREFIX${currentBucket()}_$componentKey"
        val current = prefs.getInt(key, 0)
        val bucketCount = prefs.getInt(bucketKey, 0)
        prefs.edit()
            .putInt(key, current + 1)
            .putLong(lastKey, System.currentTimeMillis())
            .putInt(bucketKey, bucketCount + 1)
            .apply()
    }

    fun getSuggestedApps(context: Context, allApps: List<AppEntry>, limit: Int = 4): List<AppEntry> {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val bucket = currentBucket()
        val usageEnabled = LauncherPrefs.isUsageSuggestionsEnabled(context) && hasUsageStatsPermission(context)
        val usageStats = if (usageEnabled) queryUsageStats(context, daysBack = 14) else emptyMap()
        val scored = allApps.map { entry ->
            val componentKey = entry.component.flattenToString()
            val pkg = entry.component.packageName
            val key = "$KEY_LAUNCH_PREFIX$componentKey"
            val lastKey = "$KEY_LAUNCH_LAST_PREFIX$componentKey"
            val bucketKey = "$KEY_LAUNCH_BUCKET_PREFIX${bucket}_$componentKey"
            val count = prefs.getInt(key, 0)
            val last = prefs.getLong(lastKey, 0L)
            val bucketCount = prefs.getInt(bucketKey, 0)
            val minutes = if (last > 0L) (now - last).toFloat() / 60_000f else 999_999f
            val days = minutes / (60f * 24f)
            val recencyScore = (7f - days).coerceAtLeast(0f) / 7f
            val recentBoost = when {
                minutes <= 15f -> 3f
                minutes <= 60f -> 2.2f
                minutes <= 240f -> 1.2f
                minutes <= 1440f -> 0.6f
                else -> 0f
            }
            val usage = usageStats[pkg]
            val usageMinutes = (usage?.totalTime ?: 0L).toFloat() / 60_000f
            val usageDays = (usage?.lastTime ?: 0L).let { lastUsed ->
                if (lastUsed > 0L) (now - lastUsed).toFloat() / (24f * 60f * 60_000f) else 999_999f
            }
            val usageRecency = (7f - usageDays).coerceAtLeast(0f) / 7f
            val usageScore = if (usageEnabled) {
                (kotlin.math.ln(usageMinutes + 1f) * 1.6f) + usageRecency * 2.5f
            } else 0f
            val score = count * 1.1f + bucketCount * 2.0f + recencyScore * 3.0f + recentBoost + usageScore
            entry to score
        }
        return scored.sortedByDescending { it.second }
            .filter { it.second > 0f }
            .map { it.first }
            .take(limit)
    }

    fun getSuggestedAppsForBucket(
        context: Context,
        allApps: List<AppEntry>,
        bucket: String,
        limit: Int = 6
    ): List<AppEntry> {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val scored = allApps.map { entry ->
            val componentKey = entry.component.flattenToString()
            val bucketKey = "$KEY_LAUNCH_BUCKET_PREFIX${bucket}_$componentKey"
            val lastKey = "$KEY_LAUNCH_LAST_PREFIX$componentKey"
            val last = prefs.getLong(lastKey, 0L)
            val minutes = if (last > 0L) (now - last).toFloat() / 60_000f else 999_999f
            val days = minutes / (60f * 24f)
            val recencyScore = (7f - days).coerceAtLeast(0f) / 7f
            val bucketCount = prefs.getInt(bucketKey, 0)
            val score = bucketCount * 2.0f + recencyScore
            entry to score
        }
        return scored.sortedByDescending { it.second }
            .filter { it.second > 0f }
            .map { it.first }
            .take(limit)
    }

    fun getRecentApps(
        context: Context,
        allApps: List<AppEntry>,
        withinHours: Int = 48,
        limit: Int = 20
    ): List<AppEntry> {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val cutoff = now - withinHours * 3_600_000L
        val usageEnabled = LauncherPrefs.isUsageSuggestionsEnabled(context) && hasUsageStatsPermission(context)
        val usageStats = if (usageEnabled) queryUsageStats(context, daysBack = 7) else emptyMap()
        val recent = allApps.mapNotNull { entry ->
            val componentKey = entry.component.flattenToString()
            val lastKey = "$KEY_LAUNCH_LAST_PREFIX$componentKey"
            val localLast = prefs.getLong(lastKey, 0L)
            val usageLast = usageStats[entry.component.packageName]?.lastTime ?: 0L
            val last = maxOf(localLast, usageLast)
            if (last >= cutoff) entry to last else null
        }
        return recent.sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.component.flattenToString() }
            .take(limit)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Ignore
        }
    }

    private data class UsageInfo(val totalTime: Long, val lastTime: Long)

    private fun queryUsageStats(context: Context, daysBack: Int): Map<String, UsageInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return emptyMap()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = end - daysBack * 24L * 60L * 60L * 1000L
        val stats = manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        if (stats.isNullOrEmpty()) return emptyMap()
        val aggregated = mutableMapOf<String, UsageInfo>()
        stats.forEach { usage ->
            val pkg = usage.packageName ?: return@forEach
            val existing = aggregated[pkg]
            val total = (existing?.totalTime ?: 0L) + usage.totalTimeInForeground
            val last = maxOf(existing?.lastTime ?: 0L, usage.lastTimeUsed)
            aggregated[pkg] = UsageInfo(total, last)
        }
        return aggregated
    }

    private fun ensurePages(context: Context): MutableList<MutableList<String>> {
        val prefs = prefs(context)
        val raw = prefs.getString(KEY_PAGES, "") ?: ""
        val pages = if (raw.isBlank()) {
            MutableList(PAGE_COUNT) { mutableListOf<String>() }
        } else {
            val parsed = raw.split(";;").map { decodeList(it) }.toMutableList()
            while (parsed.size < PAGE_COUNT) parsed.add(mutableListOf())
            if (parsed.size > PAGE_COUNT) {
                parsed.subList(PAGE_COUNT, parsed.size).clear()
            }
            parsed
        }
        if (raw.isBlank()) savePages(context, pages)
        return pages
    }

    private fun savePages(context: Context, pages: List<List<String>>) {
        val prefs = prefs(context)
        val raw = pages.joinToString(";;") { encodeList(it) }
        prefs.edit().putString(KEY_PAGES, raw).apply()
    }

    private fun loadHotseatKeys(context: Context): MutableList<String> {
        val prefs = prefs(context)
        return decodeList(prefs.getString(KEY_HOTSEAT, "") ?: "")
    }

    private fun decodeItems(
        context: Context,
        keys: List<String>,
        allApps: List<AppEntry>
    ): List<HomeItem> {
        val appMap = allApps.associateBy { it.component.flattenToString() }
        return keys.mapNotNull { key ->
            val parts = key.split(":", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            when (parts[0]) {
                TYPE_APP -> {
                    val entry = appMap[parts[1]] ?: return@mapNotNull null
                    HomeItem.app(entry)
                }
                TYPE_FOLDER -> {
                    val label = getFolderLabel(context, parts[1])
                    HomeItem.folder(parts[1], label)
                }
                TYPE_SHORTCUT -> {
                    val info = ModuleShortcuts.getInfo(parts[1]) ?: return@mapNotNull null
                    ModuleShortcuts.buildHomeItem(context, info)
                }
                else -> null
            }
        }
    }

    private fun containsItem(pages: List<List<String>>, key: String): Boolean {
        return pages.any { it.contains(key) }
    }

    private fun saveFolderItems(context: Context, folderId: String, items: List<String>) {
        val prefs = prefs(context)
        prefs.edit().putString(folderKey(folderId), items.joinToString("|")) .apply()
    }

    private fun saveFolderLabel(context: Context, folderId: String, label: String) {
        val prefs = prefs(context)
        prefs.edit().putString(folderLabelKey(folderId), label).apply()
    }

    private fun loadFolderItems(context: Context, folderId: String): MutableList<String> {
        val prefs = prefs(context)
        val raw = prefs.getString(folderKey(folderId), "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split('|').filter { it.isNotBlank() }.toMutableList()
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

    fun isModuleShortcutsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(MODULE_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_MODULE_SHORTCUTS, true)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun decodeList(raw: String): MutableList<String> {
        if (raw.isBlank()) return mutableListOf()
        return raw.split('|').filter { it.isNotBlank() }.toMutableList()
    }

    private fun encodeList(list: List<String>): String = list.joinToString("|")

    private fun saveList(prefs: android.content.SharedPreferences, key: String, list: List<String>) {
        prefs.edit().putString(key, encodeList(list)).apply()
    }

    private fun folderKey(folderId: String) = "$FOLDER_PREFIX$folderId"

    private fun folderLabelKey(folderId: String) = "$FOLDER_PREFIX$folderId$FOLDER_LABEL_SUFFIX"

    private fun currentBucket(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "morning"
            in 11..16 -> "day"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
}
