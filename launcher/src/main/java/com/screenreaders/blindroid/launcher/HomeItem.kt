package com.screenreaders.blindroid.launcher

import android.graphics.drawable.Drawable

sealed class HomeItem {
    data class App(
        val label: String,
        val component: android.content.ComponentName,
        val icon: Drawable
    ) : HomeItem()

    data class Folder(
        val id: String,
        val label: String
    ) : HomeItem()

    companion object {
        fun app(entry: AppEntry) = App(entry.label, entry.component, entry.icon)
        fun folder(id: String, label: String) = Folder(id, label)
    }
}
