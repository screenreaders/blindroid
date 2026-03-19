package com.screenreaders.blindroid.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import java.text.Collator
import java.util.UUID

data class FolderInfo(val id: String, val label: String)

object LauncherStore {
    private const val PREFS_NAME = "blindroid_launcher"
    private const val KEY_PAGES = "pages"
    private const val KEY_HOTSEAT = "hotseat"
    private const val KEY_WIDGETS = "widgets"
    private const val FOLDER_PREFIX = "folder_"
    private const val FOLDER_LABEL_SUFFIX = "_label"
    private const val PAGE_COUNT = 3

    private const val TYPE_APP = "a"
    private const val TYPE_FOLDER = "f"

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
}
