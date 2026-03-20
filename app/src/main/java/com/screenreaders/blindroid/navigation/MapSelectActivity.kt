package com.screenreaders.blindroid.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.databinding.ActivityMapSelectBinding

class MapSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapSelectBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val minLat = intent.getFloatExtra(EXTRA_MIN_LAT, 0f).toDouble()
        val minLon = intent.getFloatExtra(EXTRA_MIN_LON, 0f).toDouble()
        val maxLat = intent.getFloatExtra(EXTRA_MAX_LAT, 0f).toDouble()
        val maxLon = intent.getFloatExtra(EXTRA_MAX_LON, 0f).toDouble()

        binding.mapSelectWeb.settings.javaScriptEnabled = true
        binding.mapSelectWeb.addJavascriptInterface(MapBridge(), "Android")
        val url = buildUrl(minLat, minLon, maxLat, maxLon)
        binding.mapSelectWeb.loadUrl(url)
    }

    private fun buildUrl(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): String {
        val centerLat = if (minLat != 0.0 || maxLat != 0.0) (minLat + maxLat) / 2 else 52.23
        val centerLon = if (minLon != 0.0 || maxLon != 0.0) (minLon + maxLon) / 2 else 21.01
        val query = "minLat=$minLat&minLon=$minLon&maxLat=$maxLat&maxLon=$maxLon&centerLat=$centerLat&centerLon=$centerLon"
        return "file:///android_asset/map_select.html?$query"
    }

    private inner class MapBridge {
        @JavascriptInterface
        fun onBbox(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double) {
            val data = Intent().apply {
                putExtra(EXTRA_MIN_LAT, minLat)
                putExtra(EXTRA_MIN_LON, minLon)
                putExtra(EXTRA_MAX_LAT, maxLat)
                putExtra(EXTRA_MAX_LON, maxLon)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    companion object {
        const val EXTRA_MIN_LAT = "extra_min_lat"
        const val EXTRA_MIN_LON = "extra_min_lon"
        const val EXTRA_MAX_LAT = "extra_max_lat"
        const val EXTRA_MAX_LON = "extra_max_lon"
    }
}
