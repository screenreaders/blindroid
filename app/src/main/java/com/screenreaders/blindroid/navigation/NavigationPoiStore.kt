package com.screenreaders.blindroid.navigation

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlin.math.cos

data class OfflinePoi(
    val id: String,
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double
)

object NavigationPoiStore {
    private const val DB_NAME = "blindroid_poi.db"
    private const val DB_VERSION = 1
    private const val TABLE = "poi"

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS $TABLE (" +
                    "osm_id TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "type TEXT," +
                    "lat REAL," +
                    "lon REAL," +
                    "updated INTEGER" +
                    ")"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_poi_lat ON $TABLE (lat)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_poi_lon ON $TABLE (lon)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_poi_type ON $TABLE (type)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    private fun helper(context: Context) = Helper(context.applicationContext)

    fun replaceAll(context: Context, pois: List<OfflinePoi>) {
        val db = helper(context).writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE, null, null)
            val stmt = db.compileStatement(
                "INSERT OR REPLACE INTO $TABLE (osm_id, name, type, lat, lon, updated) VALUES (?, ?, ?, ?, ?, ?)"
            )
            val now = System.currentTimeMillis()
            for (poi in pois) {
                stmt.clearBindings()
                stmt.bindString(1, poi.id)
                stmt.bindString(2, poi.name)
                stmt.bindString(3, poi.type)
                stmt.bindDouble(4, poi.lat)
                stmt.bindDouble(5, poi.lon)
                stmt.bindLong(6, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    fun clear(context: Context) {
        val db = helper(context).writableDatabase
        try {
            db.delete(TABLE, null, null)
        } finally {
            db.close()
        }
    }

    fun count(context: Context): Int {
        val db = helper(context).readableDatabase
        return db.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }.also { db.close() }
    }

    fun queryNearby(
        context: Context,
        lat: Double,
        lon: Double,
        radiusMeters: Int,
        types: Set<String>
    ): List<OfflinePoi> {
        val latDelta = radiusMeters / 111_000.0
        val lonDelta = radiusMeters / (111_000.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.1))
        val minLat = lat - latDelta
        val maxLat = lat + latDelta
        val minLon = lon - lonDelta
        val maxLon = lon + lonDelta
        val selection = StringBuilder("lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?")
        val args = mutableListOf(minLat.toString(), maxLat.toString(), minLon.toString(), maxLon.toString())
        if (types.isNotEmpty()) {
            selection.append(" AND type IN (${types.joinToString(",") { "?" }})")
            args.addAll(types)
        }
        val db = helper(context).readableDatabase
        val result = mutableListOf<OfflinePoi>()
        db.query(
            TABLE,
            arrayOf("osm_id", "name", "type", "lat", "lon"),
            selection.toString(),
            args.toTypedArray(),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val poi = OfflinePoi(
                    id = cursor.getString(0),
                    name = cursor.getString(1) ?: "",
                    type = cursor.getString(2) ?: "",
                    lat = cursor.getDouble(3),
                    lon = cursor.getDouble(4)
                )
                if (distanceMeters(lat, lon, poi.lat, poi.lon) <= radiusMeters) {
                    result.add(poi)
                }
            }
        }
        db.close()
        return result
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (r * c).toInt()
    }
}
