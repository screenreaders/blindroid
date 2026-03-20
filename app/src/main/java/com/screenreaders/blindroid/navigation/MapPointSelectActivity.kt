package com.screenreaders.blindroid.navigation

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.databinding.ActivityMapPointSelectBinding

class MapPointSelectActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapPointSelectBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPointSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val lat = intent.getFloatExtra(EXTRA_LAT, 0f).toDouble()
        val lon = intent.getFloatExtra(EXTRA_LON, 0f).toDouble()
        val centerLat = if (lat != 0.0) lat else 52.23
        val centerLon = if (lon != 0.0) lon else 21.01

        binding.mapPointSelectWeb.settings.javaScriptEnabled = true
        binding.mapPointSelectWeb.addJavascriptInterface(MapBridge(), "Android")
        val url = "file:///android_asset/map_point_select.html?lat=$lat&lon=$lon&centerLat=$centerLat&centerLon=$centerLon"
        binding.mapPointSelectWeb.loadUrl(url)
    }

    private inner class MapBridge {
        @JavascriptInterface
        fun onPoint(lat: Double, lon: Double) {
            val data = Intent().apply {
                putExtra(EXTRA_LAT, lat)
                putExtra(EXTRA_LON, lon)
            }
            setResult(RESULT_OK, data)
            finish()
        }
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
    }
}
