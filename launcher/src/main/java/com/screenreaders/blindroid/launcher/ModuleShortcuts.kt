package com.screenreaders.blindroid.launcher

import android.content.Context

object ModuleShortcuts {
    data class ShortcutInfo(
        val id: String,
        val labelRes: Int,
        val iconRes: Int
    )

    const val ID_LAUNCHER = "launcher"
    const val ID_CALLS = "calls"
    const val ID_NOTIFICATIONS = "notifications"
    const val ID_CURRENCY = "currency"
    const val ID_LIGHT = "light"
    const val ID_CHIME = "chime"
    const val ID_UPDATES = "updates"

    val all = listOf(
        ShortcutInfo(ID_LAUNCHER, R.string.launcher_shortcut_launcher, android.R.drawable.ic_menu_manage),
        ShortcutInfo(ID_CALLS, R.string.launcher_shortcut_calls, android.R.drawable.ic_menu_call),
        ShortcutInfo(ID_NOTIFICATIONS, R.string.launcher_shortcut_notifications, android.R.drawable.ic_dialog_info),
        ShortcutInfo(ID_CURRENCY, R.string.launcher_shortcut_currency, android.R.drawable.ic_menu_camera),
        ShortcutInfo(ID_LIGHT, R.string.launcher_shortcut_light, android.R.drawable.ic_menu_day),
        ShortcutInfo(ID_CHIME, R.string.launcher_shortcut_chime, android.R.drawable.ic_menu_recent_history),
        ShortcutInfo(ID_UPDATES, R.string.launcher_shortcut_updates, android.R.drawable.ic_menu_rotate)
    )

    fun getInfo(id: String): ShortcutInfo? = all.firstOrNull { it.id == id }

    fun shortcutKeys(): List<String> = all.map { "s:${it.id}" }

    fun buildHomeItem(context: Context, info: ShortcutInfo): HomeItem.Shortcut {
        return HomeItem.Shortcut(
            id = info.id,
            label = context.getString(info.labelRes),
            iconRes = info.iconRes
        )
    }
}
