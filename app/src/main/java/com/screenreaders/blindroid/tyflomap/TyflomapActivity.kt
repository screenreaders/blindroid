package com.screenreaders.blindroid.tyflomap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityTyflomapBinding
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class TyflomapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTyflomapBinding
    private val executor = Executors.newSingleThreadExecutor()
    private var lastLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTyflomapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tyflomapCheckButton.setOnClickListener { fetchTyflomap() }
        binding.tyflomapOpenButton.setOnClickListener { openLink() }
        binding.tyflomapShareButton.setOnClickListener { shareLink() }
        lastLink = Prefs.getTyflomapLastLink(this)
        updateButtons()
    }

    private fun fetchTyflomap() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        val location = getLastLocation() ?: run {
            Toast.makeText(this, R.string.navigation_pin_missing, Toast.LENGTH_SHORT).show()
            return
        }
        binding.tyflomapStatus.text = getString(R.string.tyflomap_status_loading)
        executor.execute {
            val link = try {
                TyflomapClient.findMapLink(location.latitude, location.longitude)
            } catch (_: Exception) {
                null
            }
            runOnUiThread {
                if (link.isNullOrBlank()) {
                    binding.tyflomapStatus.text = getString(R.string.tyflomap_status_missing)
                } else {
                    lastLink = link
                    Prefs.setTyflomapLastLink(this, link)
                    binding.tyflomapStatus.text = getString(R.string.tyflomap_status_found)
                }
                updateButtons()
            }
        }
    }

    private fun openLink() {
        val link = lastLink ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    private fun shareLink() {
        val link = lastLink ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.tyflomap_share_link)))
    }

    private fun updateButtons() {
        val has = !lastLink.isNullOrBlank()
        binding.tyflomapOpenButton.isEnabled = has
        binding.tyflomapShareButton.isEnabled = has
    }

    private fun getLastLocation(): android.location.Location? {
        val manager = getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            if (granted) {
                fetchTyflomap()
            } else {
                Toast.makeText(this, R.string.tyflomap_permission_missing, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQ_LOCATION = 910
    }
}

private object TyflomapClient {
    private const val WMS_URL = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/mapy/WMS/SkorowidzMapTyflologicznych"

    fun findMapLink(lat: Double, lon: Double): String? {
        val layer = resolveLayerName() ?: "SkorowidzMapTyflologicznych"
        val d = 0.002
        val minLat = max(-90.0, lat - d)
        val maxLat = min(90.0, lat + d)
        val minLon = max(-180.0, lon - d)
        val maxLon = min(180.0, lon + d)
        val bbox = "$minLat,$minLon,$maxLat,$maxLon"
        val url = "$WMS_URL?SERVICE=WMS&REQUEST=GetFeatureInfo&VERSION=1.3.0" +
            "&CRS=EPSG:4326&BBOX=$bbox&WIDTH=101&HEIGHT=101&I=50&J=50" +
            "&LAYERS=$layer&QUERY_LAYERS=$layer&INFO_FORMAT=text/html"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 7000
        return try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            extractFirstUrl(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveLayerName(): String? {
        val url = "$WMS_URL?SERVICE=WMS&REQUEST=GetCapabilities"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 7000
        return try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val regex = Regex("<Name>([^<]+)</Name>")
            val names = regex.findAll(text).map { it.groupValues[1] }.toList()
            names.firstOrNull { it.contains("SkorowidzMapTyflologicznych", ignoreCase = true) }
                ?: names.firstOrNull()
        } finally {
            conn.disconnect()
        }
    }

    private fun extractFirstUrl(text: String): String? {
        val regex = Regex("https?://[^\\s\"'<>]+")
        return regex.find(text)?.value
    }
}
