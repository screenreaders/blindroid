package com.screenreaders.blindroid.senior

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ContactTile(
    val name: String,
    val phone: String
)

object ContactTileStore {
    private const val PREFS = "blindroid_contact_tiles"
    private const val KEY_LAYOUT = "layout_preset"
    private const val KEY_TILES = "tiles_json"

    data class LayoutPreset(val id: Int, val labelRes: Int, val count: Int, val columns: Int)

    fun getLayoutPreset(context: Context): Int =
        prefs(context).getInt(KEY_LAYOUT, 1)

    fun setLayoutPreset(context: Context, preset: Int) {
        prefs(context).edit().putInt(KEY_LAYOUT, preset).apply()
    }

    fun loadTiles(context: Context): MutableList<ContactTile?> {
        val raw = prefs(context).getString(KEY_TILES, "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ContactTile?>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj == null) {
                    list.add(null)
                } else {
                    val name = obj.optString("name")
                    val phone = obj.optString("phone")
                    if (name.isBlank() && phone.isBlank()) {
                        list.add(null)
                    } else {
                        list.add(ContactTile(name, phone))
                    }
                }
            }
            list
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveTiles(context: Context, tiles: List<ContactTile?>) {
        val array = JSONArray()
        tiles.forEach { tile ->
            if (tile == null) {
                array.put(JSONObject.NULL)
            } else {
                val obj = JSONObject()
                obj.put("name", tile.name)
                obj.put("phone", tile.phone)
                array.put(obj)
            }
        }
        prefs(context).edit().putString(KEY_TILES, array.toString()).apply()
    }

    fun normalizedTiles(context: Context, count: Int): MutableList<ContactTile?> {
        val tiles = loadTiles(context)
        val list = tiles.toMutableList()
        if (list.size > count) {
            return list.subList(0, count)
        }
        while (list.size < count) {
            list.add(null)
        }
        return list
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
