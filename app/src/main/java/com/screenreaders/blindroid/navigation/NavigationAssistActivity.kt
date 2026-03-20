package com.screenreaders.blindroid.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.call.CallAnnouncer
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityNavigationAssistBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

class NavigationAssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNavigationAssistBinding
    private val categories = listOf(
        Category("restaurant", R.string.navigation_category_restaurant),
        Category("cafe", R.string.navigation_category_cafe),
        Category("pharmacy", R.string.navigation_category_pharmacy),
        Category("hospital", R.string.navigation_category_hospital),
        Category("bank", R.string.navigation_category_bank),
        Category("atm", R.string.navigation_category_atm),
        Category("gas_station", R.string.navigation_category_fuel),
        Category("movie_theater", R.string.navigation_category_cinema),
        Category("supermarket", R.string.navigation_category_grocery),
        Category("police", R.string.navigation_category_police),
        Category("parking", R.string.navigation_category_parking),
        Category("lodging", R.string.navigation_category_hotel),
        Category("bus_station", R.string.navigation_category_bus_station),
        Category("train_station", R.string.navigation_category_train_station),
        Category("subway_station", R.string.navigation_category_subway_station),
        Category("school", R.string.navigation_category_school),
        Category("university", R.string.navigation_category_university),
        Category("park", R.string.navigation_category_park),
        Category("post_office", R.string.navigation_category_post_office),
        Category("library", R.string.navigation_category_library),
        Category("place_of_worship", R.string.navigation_category_worship),
        Category("shopping_mall", R.string.navigation_category_mall),
        Category("gym", R.string.navigation_category_gym),
        Category("doctor", R.string.navigation_category_doctor),
        Category("dentist", R.string.navigation_category_dentist),
        Category("bakery", R.string.navigation_category_bakery),
        Category("airport", R.string.navigation_category_airport),
        Category("tourist_attraction", R.string.navigation_category_attraction)
    )
    private val selected = BooleanArray(categories.size)
    private var pendingSharePin = false
    private var pendingTyflomap = false
    private var pendingOfflineImport = false
    private var pendingOfflineCheck = false
    private var pendingRouteStart = false
    private var tyflomapLink: String? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        restoreLastDestination()
        binding.navigationStartButton.setOnClickListener { startNavigation() }
        binding.navigationStartOfflineButton.setOnClickListener { startOfflineNavigation() }
        binding.navigationOfflineGuidanceSwitch.isChecked = Prefs.isNavigationRouteEnabled(this) &&
            !Prefs.isNavigationRouteGpxEnabled(this)
        binding.navigationOfflineGuidanceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startOfflineGuidance()
            } else {
                stopOfflineGuidance()
            }
        }
        binding.navigationMapPointSelectButton.setOnClickListener { openMapPointSelect() }
        binding.navigationOfflineGpxSwitch.isChecked = Prefs.isNavigationRouteGpxEnabled(this)
        binding.navigationOfflineGpxSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationRouteGpxEnabled(this, isChecked)
            if (isChecked) {
                binding.navigationOfflineGuidanceSwitch.isChecked = false
                startGpxGuidance()
            } else {
                stopRouteIfNone()
            }
            updateGpxStatus()
        }
        binding.navigationOfflineGpxImportButton.setOnClickListener { pickGpxFile() }
        binding.navigationTrackingSwitch.isChecked = Prefs.isNavigationTrackingEnabled(this)
        binding.navigationTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationTrackingEnabled(this, isChecked)
            syncTrackingService()
        }
        setupPoiSourceControls()
        setupImportControls()
        setupAutoImportControls()
        binding.navigationCategoriesButton.setOnClickListener { openCategoryDialog() }
        binding.navigationApiKeyInput.setText(Prefs.getNavigationApiKey(this))
        binding.navigationApiKeyInput.setOnFocusChangeListener { _, _ ->
            Prefs.setNavigationApiKey(this, binding.navigationApiKeyInput.text?.toString().orEmpty())
        }
        initPoiControls()
        binding.navigationTrackLogSwitch.isChecked = Prefs.isNavigationTrackLogEnabled(this)
        binding.navigationTrackLogSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationTrackLogEnabled(this, isChecked)
            syncTrackingService()
        }
        binding.navigationShareTrackButton.setOnClickListener { shareLastTrack() }
        binding.navigationShareGpxButton.setOnClickListener { shareLastTrackGpx() }
        binding.navigationSharePinButton.setOnClickListener { sharePin() }
        setupTyflomapControls()
        loadSelectedCategories()
        updateCategorySummary()
        updateOfflineStatus()
        updateGpxStatus()
        updatePoiSourceUi()
    }

    override fun onStart() {
        super.onStart()
        binding.navigationOfflineGuidanceSwitch.isChecked = Prefs.isNavigationRouteEnabled(this) &&
            !Prefs.isNavigationRouteGpxEnabled(this)
        binding.navigationOfflineGpxSwitch.isChecked = Prefs.isNavigationRouteGpxEnabled(this)
        updateGpxStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private fun startNavigation() {
        val place = binding.navigationPlaceInput.text?.toString()?.trim().orEmpty()
        val city = binding.navigationCityInput.text?.toString()?.trim().orEmpty()
        if (city.isBlank()) {
            Toast.makeText(this, R.string.navigation_city_required, Toast.LENGTH_SHORT).show()
            return
        }
        val destination = if (place.isBlank()) {
            city
        } else {
            "$place, $city"
        }
        val mode = when {
            binding.navigationModeDriving.isChecked -> "driving"
            binding.navigationModeWalking.isChecked -> "walking"
            binding.navigationModeBicycling.isChecked -> "bicycling"
            else -> "transit"
        }
        speakConfirmation(destination, mode)
        Prefs.setNavigationLastPlace(this, place)
        Prefs.setNavigationLastCity(this, city)
        Prefs.setNavigationLastMode(this, mode)
        if (binding.navigationTrackingSwitch.isChecked || binding.navigationTrackLogSwitch.isChecked) {
            syncTrackingService()
        }
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}&travelmode=$mode"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun startOfflineNavigation() {
        val place = binding.navigationPlaceInput.text?.toString()?.trim().orEmpty()
        val city = binding.navigationCityInput.text?.toString()?.trim().orEmpty()
        if (city.isBlank()) {
            Toast.makeText(this, R.string.navigation_city_required, Toast.LENGTH_SHORT).show()
            return
        }
        val destination = if (place.isBlank()) {
            city
        } else {
            "$place, $city"
        }
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(destination)}")
        val osmAndPackages = listOf("net.osmand", "net.osmand.plus")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        val resolved = osmAndPackages.firstOrNull { pkg ->
            intent.setPackage(pkg)
            intent.resolveActivity(packageManager) != null
        }
        if (resolved != null) {
            intent.setPackage(resolved)
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.navigation_offline_route_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startOfflineGuidance() {
        if (!hasLocationPermission()) {
            pendingRouteStart = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        Prefs.setNavigationRouteGpxEnabled(this, false)
        if (binding.navigationOfflineGpxSwitch.isChecked) {
            binding.navigationOfflineGpxSwitch.isChecked = false
        }
        val place = binding.navigationPlaceInput.text?.toString()?.trim().orEmpty()
        val city = binding.navigationCityInput.text?.toString()?.trim().orEmpty()
        val coord = parseCoordinates(place) ?: parseCoordinates(city)
        if (coord != null) {
            beginOfflineGuidance(place.ifBlank { city }, coord.first, coord.second)
            return
        }
        val destination = if (place.isBlank()) city else "$place, $city"
        if (destination.isBlank()) {
            Toast.makeText(this, R.string.navigation_offline_guidance_missing, Toast.LENGTH_SHORT).show()
            binding.navigationOfflineGuidanceSwitch.isChecked = false
            return
        }
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, R.string.navigation_offline_guidance_missing, Toast.LENGTH_SHORT).show()
            binding.navigationOfflineGuidanceSwitch.isChecked = false
            return
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        Toast.makeText(this, R.string.navigation_offline_guidance_lookup, Toast.LENGTH_SHORT).show()
        executor.execute {
            val result = try {
                val list = geocoder.getFromLocationName(destination, 1)
                list?.firstOrNull()
            } catch (_: Exception) {
                null
            }
            runOnUiThread {
                if (!binding.navigationOfflineGuidanceSwitch.isChecked) {
                    return@runOnUiThread
                }
                if (result == null) {
                    Toast.makeText(this, R.string.navigation_offline_guidance_missing, Toast.LENGTH_SHORT).show()
                    binding.navigationOfflineGuidanceSwitch.isChecked = false
                    Prefs.setNavigationRouteEnabled(this, false)
                } else {
                    beginOfflineGuidance(destination, result.latitude, result.longitude)
                }
            }
        }
    }

    private fun stopOfflineGuidance() {
        Prefs.setNavigationRouteEnabled(this, false)
        NavigationRouteService.stop(this)
        Toast.makeText(this, R.string.navigation_offline_guidance_stopped, Toast.LENGTH_SHORT).show()
    }

    private fun beginOfflineGuidance(label: String, lat: Double, lon: Double) {
        Prefs.setNavigationRouteLabel(this, label)
        Prefs.setNavigationRouteLat(this, lat)
        Prefs.setNavigationRouteLon(this, lon)
        Prefs.setNavigationRouteEnabled(this, true)
        NavigationRouteService.start(this)
        Toast.makeText(this, R.string.navigation_offline_guidance_started, Toast.LENGTH_SHORT).show()
    }

    private fun startGpxGuidance() {
        val path = Prefs.getNavigationRouteGpxPath(this)
        if (path.isNullOrBlank()) {
            Toast.makeText(this, R.string.navigation_offline_gpx_missing, Toast.LENGTH_SHORT).show()
            binding.navigationOfflineGpxSwitch.isChecked = false
            return
        }
        Prefs.setNavigationRouteEnabled(this, true)
        NavigationRouteService.start(this)
        Toast.makeText(this, R.string.navigation_offline_gpx_loaded, Toast.LENGTH_SHORT).show()
    }

    private fun stopRouteIfNone() {
        val any = binding.navigationOfflineGuidanceSwitch.isChecked || binding.navigationOfflineGpxSwitch.isChecked
        if (!any) {
            Prefs.setNavigationRouteEnabled(this, false)
            NavigationRouteService.stop(this)
        }
    }

    private fun pickGpxFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/gpx+xml", "text/xml", "application/octet-stream"))
        }
        startActivityForResult(intent, REQ_GPX_PICK)
    }

    private fun handleGpxPicked(data: Intent) {
        val uri = data.data ?: return
        val dir = java.io.File(filesDir, "gpx")
        if (!dir.exists()) dir.mkdirs()
        val dest = java.io.File(dir, "route_${System.currentTimeMillis()}.gpx")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: run {
                Toast.makeText(this, R.string.navigation_offline_gpx_failed, Toast.LENGTH_SHORT).show()
                return
            }
        } catch (_: Exception) {
            Toast.makeText(this, R.string.navigation_offline_gpx_failed, Toast.LENGTH_SHORT).show()
            return
        }
        executor.execute {
            val points = try {
                GpxRouteParser.parse(dest)
            } catch (_: Exception) {
                emptyList()
            }
            runOnUiThread {
                if (points.size < 2) {
                    Toast.makeText(this, R.string.navigation_offline_gpx_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                Prefs.setNavigationRouteGpxPath(this, dest.absolutePath)
                Prefs.setNavigationRouteGpxCount(this, points.size)
                Prefs.setNavigationRouteGpxIndex(this, 0)
                binding.navigationOfflineGpxSwitch.isChecked = true
                updateGpxStatus()
            }
        }
    }

    private fun updateGpxStatus() {
        val count = Prefs.getNavigationRouteGpxCount(this)
        if (count <= 0) {
            binding.navigationOfflineGpxStatus.text = getString(R.string.navigation_offline_gpx_missing)
        } else {
            binding.navigationOfflineGpxStatus.text = getString(R.string.navigation_offline_gpx_status, count)
        }
    }

    private fun openMapPointSelect() {
        val coord = parseCoordinates(binding.navigationPlaceInput.text?.toString().orEmpty())
        val intent = Intent(this, MapPointSelectActivity::class.java).apply {
            if (coord != null) {
                putExtra(MapPointSelectActivity.EXTRA_LAT, coord.first.toFloat())
                putExtra(MapPointSelectActivity.EXTRA_LON, coord.second.toFloat())
            }
        }
        startActivityForResult(intent, REQ_MAP_POINT)
    }

    private fun parseCoordinates(input: String): Pair<Double, Double>? {
        if (input.isBlank()) return null
        val cleaned = input.replace(",", " ").trim()
        val parts = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return lat to lon
    }

    private fun startTracking() {
        val apiKey = binding.navigationApiKeyInput.text?.toString()?.trim().orEmpty()
        Prefs.setNavigationApiKey(this, apiKey)
        val selectedTypes = selectedTypes()
        Prefs.setNavigationCategories(this, selectedTypes.joinToString(","))
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        NavigationPoiService.start(this)
        Toast.makeText(this, R.string.navigation_tracking_started, Toast.LENGTH_SHORT).show()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun loadSelectedCategories() {
        val saved = Prefs.getNavigationCategories(this)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        categories.forEachIndexed { index, category ->
            selected[index] = saved.contains(category.type)
        }
    }

    private fun openCategoryDialog() {
        val labels = categories.map { getString(it.labelRes) }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.navigation_categories_button)
            .setMultiChoiceItems(labels, selected) { _, which, isChecked ->
                selected[which] = isChecked
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                Prefs.setNavigationCategories(this, selectedTypes().joinToString(","))
                updateCategorySummary()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun selectedTypes(): List<String> {
        return categories.filterIndexed { index, _ -> selected[index] }.map { it.type }
    }

    private fun updateCategorySummary() {
        val labels = categories.filterIndexed { index, _ -> selected[index] }
            .map { getString(it.labelRes) }
        binding.navigationCategoriesSummary.text = if (labels.isEmpty()) {
            getString(R.string.navigation_categories_none)
        } else {
            labels.joinToString(", ")
        }
    }

    private fun speakConfirmation(destination: String, mode: String) {
        val text = when (mode) {
            "driving" -> getString(R.string.navigation_confirm_driving, destination)
            "walking" -> getString(R.string.navigation_confirm_walking, destination)
            "bicycling" -> getString(R.string.navigation_confirm_bicycling, destination)
            else -> getString(R.string.navigation_confirm_transit, destination)
        }
        val announcer = CallAnnouncer(this)
        announcer.speak(
            text = text,
            repeatCount = 1,
            rate = Prefs.getSpeechRate(this),
            volume = Prefs.getSpeechVolume(this),
            voiceName = Prefs.getVoiceName(this),
            onComplete = { announcer.shutdown() }
        )
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
                if (pendingSharePin) {
                    pendingSharePin = false
                    sharePin()
                } else if (pendingTyflomap) {
                    pendingTyflomap = false
                    fetchTyflomap()
                } else if (pendingRouteStart) {
                    pendingRouteStart = false
                    startOfflineGuidance()
                } else if (pendingOfflineImport) {
                    pendingOfflineImport = false
                    importOfflinePois()
                } else if (pendingOfflineCheck) {
                    pendingOfflineCheck = false
                    checkOfflineBase()
                } else {
                    syncTrackingService()
                }
            } else {
                if (pendingTyflomap) {
                    Toast.makeText(this, R.string.tyflomap_permission_missing, Toast.LENGTH_SHORT).show()
                } else if (pendingRouteStart) {
                    Toast.makeText(this, R.string.navigation_tracking_missing_location, Toast.LENGTH_SHORT).show()
                } else if (pendingOfflineImport) {
                    Toast.makeText(this, R.string.navigation_tracking_missing_location, Toast.LENGTH_SHORT).show()
                } else if (pendingOfflineCheck) {
                    Toast.makeText(this, R.string.navigation_tracking_missing_location, Toast.LENGTH_SHORT).show()
                }
                pendingSharePin = false
                pendingTyflomap = false
                pendingRouteStart = false
                pendingOfflineImport = false
                pendingOfflineCheck = false
                binding.navigationTrackingSwitch.isChecked = false
                binding.navigationTrackLogSwitch.isChecked = false
                binding.navigationOfflineGuidanceSwitch.isChecked = false
            }
        }
    }

    private data class Category(val type: String, val labelRes: Int)

    companion object {
        private const val REQ_LOCATION = 812
        private const val REQ_MAP_SELECT = 813
        private const val REQ_GPX_PICK = 814
        private const val REQ_MAP_POINT = 815
    }

    private fun initPoiControls() {
        val radiusOptions = listOf(50, 100, 200)
        val radiusLabels = radiusOptions.map { getString(R.string.navigation_radius_value, it) }
        val radiusAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            radiusLabels
        )
        radiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationRadiusSpinner.adapter = radiusAdapter
        val radiusIndex = radiusOptions.indexOf(Prefs.getNavigationRadius(this)).let { if (it >= 0) it else 1 }
        binding.navigationRadiusSpinner.setSelection(radiusIndex, false)
        binding.navigationRadiusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = radiusOptions.getOrElse(position) { 100 }
                Prefs.setNavigationRadius(this@NavigationAssistActivity, value)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val intervalOptions = listOf(15, 60, 180)
        val intervalLabels = listOf(
            getString(R.string.navigation_interval_fast),
            getString(R.string.navigation_interval_normal),
            getString(R.string.navigation_interval_slow)
        )
        val intervalAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            intervalLabels
        )
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationIntervalSpinner.adapter = intervalAdapter
        val intervalIndex = intervalOptions.indexOf(Prefs.getNavigationSpeakIntervalSec(this)).let { if (it >= 0) it else 1 }
        binding.navigationIntervalSpinner.setSelection(intervalIndex, false)
        binding.navigationIntervalSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = intervalOptions.getOrElse(position) { 60 }
                Prefs.setNavigationSpeakIntervalSec(this@NavigationAssistActivity, value)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val distanceOptions = listOf(50, 80, 120)
        val distanceLabels = distanceOptions.map { getString(R.string.navigation_min_distance_value, it) }
        val distanceAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            distanceLabels
        )
        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationMinDistanceSpinner.adapter = distanceAdapter
        val distIndex = distanceOptions.indexOf(Prefs.getNavigationMinDistance(this)).let { if (it >= 0) it else 1 }
        binding.navigationMinDistanceSpinner.setSelection(distIndex, false)
        binding.navigationMinDistanceSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = distanceOptions.getOrElse(position) { 80 }
                Prefs.setNavigationMinDistance(this@NavigationAssistActivity, value)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        binding.navigationMovingOnlySwitch.isChecked = Prefs.isNavigationMovingOnly(this)
        binding.navigationMovingOnlySwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationMovingOnly(this, isChecked)
        }
    }

    private fun syncTrackingService() {
        val wantsPoi = binding.navigationTrackingSwitch.isChecked
        val wantsTrack = binding.navigationTrackLogSwitch.isChecked
        if (!wantsPoi && !wantsTrack) {
            NavigationPoiService.stop(this)
            Toast.makeText(this, R.string.navigation_tracking_stopped, Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        if (wantsPoi) {
            val apiKey = binding.navigationApiKeyInput.text?.toString()?.trim().orEmpty()
            Prefs.setNavigationApiKey(this, apiKey)
            val selectedTypes = selectedTypes()
            Prefs.setNavigationCategories(this, selectedTypes.joinToString(","))
            if (selectedTypes.isEmpty()) {
                Toast.makeText(this, R.string.navigation_tracking_no_categories, Toast.LENGTH_SHORT).show()
            } else if (Prefs.getNavigationPoiSource(this) == Prefs.NAV_POI_SOURCE_GOOGLE && apiKey.isBlank()) {
                Toast.makeText(this, R.string.navigation_tracking_missing_key, Toast.LENGTH_SHORT).show()
            } else if (Prefs.getNavigationPoiSource(this) == Prefs.NAV_POI_SOURCE_OFFLINE &&
                NavigationPoiStore.count(this) == 0
            ) {
                Toast.makeText(this, R.string.navigation_offline_missing, Toast.LENGTH_SHORT).show()
            }
        }
        NavigationPoiService.start(this)
        Toast.makeText(this, R.string.navigation_tracking_started, Toast.LENGTH_SHORT).show()
    }

    private fun sharePin() {
        if (!hasLocationPermission()) {
            pendingSharePin = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        val manager = getSystemService(android.location.LocationManager::class.java)
        val providers = listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER)
        val loc = providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (loc == null) {
            Toast.makeText(this, R.string.navigation_pin_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val link = "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.navigation_share_pin)))
    }

    private fun shareLastTrack() {
        val path = Prefs.getNavigationLastTrack(this)
        if (path.isNullOrBlank()) {
            Toast.makeText(this, R.string.navigation_track_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val file = java.io.File(path)
        if (!file.exists()) {
            Toast.makeText(this, R.string.navigation_track_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Blindroid ślad trasy")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.navigation_share_track)))
    }

    private fun shareLastTrackGpx() {
        val path = Prefs.getNavigationLastTrack(this)
        if (path.isNullOrBlank()) {
            Toast.makeText(this, R.string.navigation_track_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val csvFile = java.io.File(path)
        val gpxFile = TrackExporter.csvToGpx(this, csvFile)
        if (gpxFile == null || !gpxFile.exists()) {
            Toast.makeText(this, R.string.navigation_gpx_missing, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            gpxFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gpx+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Blindroid GPX")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.navigation_share_gpx)))
    }

    private fun restoreLastDestination() {
        binding.navigationPlaceInput.setText(Prefs.getNavigationLastPlace(this))
        binding.navigationCityInput.setText(Prefs.getNavigationLastCity(this))
        when (Prefs.getNavigationLastMode(this)) {
            "walking" -> binding.navigationModeWalking.isChecked = true
            "bicycling" -> binding.navigationModeBicycling.isChecked = true
            "transit" -> binding.navigationModeTransit.isChecked = true
            else -> binding.navigationModeDriving.isChecked = true
        }
    }

    private fun openMapSelect() {
        val intent = Intent(this, MapSelectActivity::class.java).apply {
            putExtra(MapSelectActivity.EXTRA_MIN_LAT, Prefs.getNavigationImportMinLat(this@NavigationAssistActivity))
            putExtra(MapSelectActivity.EXTRA_MIN_LON, Prefs.getNavigationImportMinLon(this@NavigationAssistActivity))
            putExtra(MapSelectActivity.EXTRA_MAX_LAT, Prefs.getNavigationImportMaxLat(this@NavigationAssistActivity))
            putExtra(MapSelectActivity.EXTRA_MAX_LON, Prefs.getNavigationImportMaxLon(this@NavigationAssistActivity))
        }
        startActivityForResult(intent, REQ_MAP_SELECT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_MAP_SELECT && resultCode == RESULT_OK && data != null) {
            val minLat = data.getDoubleExtra(MapSelectActivity.EXTRA_MIN_LAT, Double.NaN)
            val minLon = data.getDoubleExtra(MapSelectActivity.EXTRA_MIN_LON, Double.NaN)
            val maxLat = data.getDoubleExtra(MapSelectActivity.EXTRA_MAX_LAT, Double.NaN)
            val maxLon = data.getDoubleExtra(MapSelectActivity.EXTRA_MAX_LON, Double.NaN)
            if (minLat.isNaN() || minLon.isNaN() || maxLat.isNaN() || maxLon.isNaN()) return
            Prefs.setNavigationImportMode(this, Prefs.NAV_IMPORT_MODE_MANUAL)
            Prefs.setNavigationImportMinLat(this, minLat.toFloat())
            Prefs.setNavigationImportMinLon(this, minLon.toFloat())
            Prefs.setNavigationImportMaxLat(this, maxLat.toFloat())
            Prefs.setNavigationImportMaxLon(this, maxLon.toFloat())
            binding.navigationImportMinLatInput.setText(minLat.toString())
            binding.navigationImportMinLonInput.setText(minLon.toString())
            binding.navigationImportMaxLatInput.setText(maxLat.toString())
            binding.navigationImportMaxLonInput.setText(maxLon.toString())
            binding.navigationImportModeManual.isChecked = true
            updateImportModeUi()
        } else if (requestCode == REQ_MAP_POINT && resultCode == RESULT_OK && data != null) {
            val lat = data.getDoubleExtra(MapPointSelectActivity.EXTRA_LAT, Double.NaN)
            val lon = data.getDoubleExtra(MapPointSelectActivity.EXTRA_LON, Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) {
                val coordText = "%.6f, %.6f".format(Locale.US, lat, lon)
                binding.navigationPlaceInput.setText(coordText)
                if (binding.navigationOfflineGuidanceSwitch.isChecked) {
                    beginOfflineGuidance(coordText, lat, lon)
                }
            }
        } else if (requestCode == REQ_GPX_PICK && resultCode == RESULT_OK && data != null) {
            handleGpxPicked(data)
        }
    }

    private fun setupPoiSourceControls() {
        val source = Prefs.getNavigationPoiSource(this)
        binding.navigationPoiSourceGoogle.isChecked = source == Prefs.NAV_POI_SOURCE_GOOGLE
        binding.navigationPoiSourceOffline.isChecked = source == Prefs.NAV_POI_SOURCE_OFFLINE
        binding.navigationPoiSourceOsm.isChecked = source == Prefs.NAV_POI_SOURCE_OSM
        binding.navigationPoiSourceHybrid.isChecked = source == Prefs.NAV_POI_SOURCE_HYBRID
        binding.navigationPoiSourceGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = when (checkedId) {
                R.id.navigationPoiSourceGoogle -> Prefs.NAV_POI_SOURCE_GOOGLE
                R.id.navigationPoiSourceOsm -> Prefs.NAV_POI_SOURCE_OSM
                R.id.navigationPoiSourceHybrid -> Prefs.NAV_POI_SOURCE_HYBRID
                else -> Prefs.NAV_POI_SOURCE_OFFLINE
            }
            Prefs.setNavigationPoiSource(this, value)
            updatePoiSourceUi()
        }
        binding.navigationOfflineImportButton.setOnClickListener { importOfflinePois() }
        binding.navigationOfflineClearButton.setOnClickListener { clearOfflinePois() }
    }

    private fun setupImportControls() {
        binding.navigationOfflineBaseUrlInput.setText(Prefs.getNavigationOfflineBaseUrl(this))
        binding.navigationOfflineBaseUrlInput.setOnFocusChangeListener { _, _ ->
            Prefs.setNavigationOfflineBaseUrl(this, binding.navigationOfflineBaseUrlInput.text?.toString().orEmpty())
        }
        binding.navigationOfflineCheckButton.setOnClickListener { checkOfflineBase() }

        binding.navigationOfflineGzipSwitch.isChecked = Prefs.isNavigationOfflineGzipEnabled(this)
        binding.navigationOfflineGzipSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationOfflineGzipEnabled(this, isChecked)
        }

        binding.navigationOfflineSegmentSwitch.isChecked = Prefs.isNavigationOfflineSegmented(this)
        binding.navigationOfflineSegmentSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationOfflineSegmented(this, isChecked)
            updateOfflineSegmentUi()
        }

        val segmentOptions = listOf(8, 16, 32, 64)
        val segmentLabels = segmentOptions.map { getString(R.string.navigation_offline_segment_value, it) }
        val segmentAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            segmentLabels
        )
        segmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationOfflineSegmentSpinner.adapter = segmentAdapter
        val segmentIndex = segmentOptions.indexOf(Prefs.getNavigationOfflineSegmentSize(this)).let { if (it >= 0) it else 1 }
        binding.navigationOfflineSegmentSpinner.setSelection(segmentIndex, false)
        binding.navigationOfflineSegmentSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val value = segmentOptions.getOrElse(position) { 16 }
                    Prefs.setNavigationOfflineSegmentSize(this@NavigationAssistActivity, value)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
        updateOfflineSegmentUi()

        val zoomOptions = listOf(12, 13, 14, 15, 16, 17)
        val zoomLabels = zoomOptions.map { getString(R.string.navigation_offline_zoom_value, it) }
        val zoomAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            zoomLabels
        )
        zoomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationOfflineZoomSpinner.adapter = zoomAdapter
        val zoomIndex = zoomOptions.indexOf(Prefs.getNavigationOfflineZoom(this)).let { if (it >= 0) it else 3 }
        binding.navigationOfflineZoomSpinner.setSelection(zoomIndex, false)
        binding.navigationOfflineZoomSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = zoomOptions.getOrElse(position) { 15 }
                Prefs.setNavigationOfflineZoom(this@NavigationAssistActivity, value)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val importRadiusOptions = listOf(200, 500, 1000, 2000, 5000, 10000)
        val importRadiusLabels = importRadiusOptions.map { getString(R.string.navigation_import_radius_value, it) }
        val importRadiusAdapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            importRadiusLabels
        )
        importRadiusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationImportRadiusSpinner.adapter = importRadiusAdapter
        val importRadiusIndex = importRadiusOptions.indexOf(Prefs.getNavigationImportRadius(this)).let { if (it >= 0) it else 2 }
        binding.navigationImportRadiusSpinner.setSelection(importRadiusIndex, false)
        binding.navigationImportRadiusSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val value = importRadiusOptions.getOrElse(position) { 1000 }
                Prefs.setNavigationImportRadius(this@NavigationAssistActivity, value)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        val mode = Prefs.getNavigationImportMode(this)
        binding.navigationImportModeRadius.isChecked = mode == Prefs.NAV_IMPORT_MODE_RADIUS
        binding.navigationImportModeManual.isChecked = mode == Prefs.NAV_IMPORT_MODE_MANUAL
        binding.navigationImportModeGroup.setOnCheckedChangeListener { _, checkedId ->
            val value = if (checkedId == R.id.navigationImportModeManual) {
                Prefs.NAV_IMPORT_MODE_MANUAL
            } else {
                Prefs.NAV_IMPORT_MODE_RADIUS
            }
            Prefs.setNavigationImportMode(this, value)
            updateImportModeUi()
        }
        binding.navigationImportMinLatInput.setText(Prefs.getNavigationImportMinLat(this).toString())
        binding.navigationImportMinLonInput.setText(Prefs.getNavigationImportMinLon(this).toString())
        binding.navigationImportMaxLatInput.setText(Prefs.getNavigationImportMaxLat(this).toString())
        binding.navigationImportMaxLonInput.setText(Prefs.getNavigationImportMaxLon(this).toString())
        binding.navigationImportMinLatInput.setOnFocusChangeListener { _, _ ->
            binding.navigationImportMinLatInput.text?.toString()?.toFloatOrNull()?.let {
                Prefs.setNavigationImportMinLat(this, it)
            }
        }
        binding.navigationImportMinLonInput.setOnFocusChangeListener { _, _ ->
            binding.navigationImportMinLonInput.text?.toString()?.toFloatOrNull()?.let {
                Prefs.setNavigationImportMinLon(this, it)
            }
        }
        binding.navigationImportMaxLatInput.setOnFocusChangeListener { _, _ ->
            binding.navigationImportMaxLatInput.text?.toString()?.toFloatOrNull()?.let {
                Prefs.setNavigationImportMaxLat(this, it)
            }
        }
        binding.navigationImportMaxLonInput.setOnFocusChangeListener { _, _ ->
            binding.navigationImportMaxLonInput.text?.toString()?.toFloatOrNull()?.let {
                Prefs.setNavigationImportMaxLon(this, it)
            }
        }
        updateImportModeUi()
        binding.navigationMapSelectButton.setOnClickListener { openMapSelect() }
    }

    private fun updateOfflineSegmentUi() {
        val source = Prefs.getNavigationPoiSource(this)
        val offlineCapable = source == Prefs.NAV_POI_SOURCE_OFFLINE || source == Prefs.NAV_POI_SOURCE_HYBRID
        val enabled = offlineCapable && Prefs.isNavigationOfflineSegmented(this)
        binding.navigationOfflineSegmentLabel.isEnabled = enabled
        binding.navigationOfflineSegmentLabel.alpha = if (enabled) 1f else 0.6f
        binding.navigationOfflineSegmentSpinner.isEnabled = enabled
    }

    private fun setupAutoImportControls() {
        binding.navigationAutoImportSwitch.isChecked = Prefs.isNavigationAutoImportEnabled(this)
        binding.navigationAutoImportWifiSwitch.isChecked = Prefs.isNavigationAutoImportWifiOnly(this)
        binding.navigationAutoImportChargingSwitch.isChecked = Prefs.isNavigationAutoImportChargingOnly(this)

        val intervals = listOf(6, 12, 24)
        val labels = listOf(
            getString(R.string.navigation_auto_import_6h),
            getString(R.string.navigation_auto_import_12h),
            getString(R.string.navigation_auto_import_24h)
        )
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.navigationAutoImportIntervalSpinner.adapter = adapter
        val index = intervals.indexOf(Prefs.getNavigationAutoImportIntervalHours(this)).let { if (it >= 0) it else 2 }
        binding.navigationAutoImportIntervalSpinner.setSelection(index, false)
        binding.navigationAutoImportIntervalSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val value = intervals.getOrElse(position) { 24 }
                    Prefs.setNavigationAutoImportIntervalHours(this@NavigationAssistActivity, value)
                    scheduleAutoImport()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }

        binding.navigationAutoImportSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationAutoImportEnabled(this, isChecked)
            scheduleAutoImport()
        }
        binding.navigationAutoImportWifiSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationAutoImportWifiOnly(this, isChecked)
            scheduleAutoImport()
        }
        binding.navigationAutoImportChargingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationAutoImportChargingOnly(this, isChecked)
            scheduleAutoImport()
        }
        scheduleAutoImport()
    }

    private fun scheduleAutoImport() {
        val enabled = Prefs.isNavigationAutoImportEnabled(this)
        val workManager = androidx.work.WorkManager.getInstance(this)
        if (!enabled) {
            workManager.cancelUniqueWork(OfflineImportWorker.WORK_NAME)
            return
        }
        val interval = Prefs.getNavigationAutoImportIntervalHours(this)
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(
                if (Prefs.isNavigationAutoImportWifiOnly(this)) {
                    androidx.work.NetworkType.UNMETERED
                } else {
                    androidx.work.NetworkType.CONNECTED
                }
            )
            .setRequiresCharging(Prefs.isNavigationAutoImportChargingOnly(this))
            .build()
        val request = androidx.work.PeriodicWorkRequestBuilder<OfflineImportWorker>(
            interval.toLong(),
            java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            OfflineImportWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun updateImportModeUi() {
        val manual = Prefs.getNavigationImportMode(this) == Prefs.NAV_IMPORT_MODE_MANUAL
        val visibility = if (manual) android.view.View.VISIBLE else android.view.View.GONE
        binding.navigationImportMinLatInput.visibility = visibility
        binding.navigationImportMinLonInput.visibility = visibility
        binding.navigationImportMaxLatInput.visibility = visibility
        binding.navigationImportMaxLonInput.visibility = visibility
        binding.navigationImportManualHint.visibility = visibility
        updatePoiSourceUi()
    }

    private fun updatePoiSourceUi() {
        val source = Prefs.getNavigationPoiSource(this)
        val isGoogle = source == Prefs.NAV_POI_SOURCE_GOOGLE
        val isOfflineCapable = source == Prefs.NAV_POI_SOURCE_OFFLINE || source == Prefs.NAV_POI_SOURCE_HYBRID
        binding.navigationApiKeyInput.isEnabled = isGoogle
        binding.navigationApiKeyHint.alpha = if (isGoogle) 1f else 0.6f
        binding.navigationApiKeyInput.alpha = if (isGoogle) 1f else 0.6f
        binding.navigationOfflineImportButton.isEnabled = isOfflineCapable
        binding.navigationOfflineClearButton.isEnabled = isOfflineCapable
        binding.navigationOfflineStatus.alpha = if (isOfflineCapable) 1f else 0.6f
        binding.navigationOfflineBaseUrlInput.isEnabled = isOfflineCapable
        binding.navigationOfflineBaseUrlHint.alpha = if (isOfflineCapable) 1f else 0.6f
        binding.navigationOfflineBaseUrlInput.alpha = if (isOfflineCapable) 1f else 0.6f
        binding.navigationOfflineGzipSwitch.isEnabled = isOfflineCapable
        binding.navigationOfflineSegmentSwitch.isEnabled = isOfflineCapable
        binding.navigationOfflineSegmentLabel.alpha = if (isOfflineCapable) 1f else 0.6f
        binding.navigationOfflineSegmentSpinner.alpha = if (isOfflineCapable) 1f else 0.6f
        binding.navigationOfflineZoomSpinner.isEnabled = isOfflineCapable
        binding.navigationImportRadiusSpinner.isEnabled = isOfflineCapable
        binding.navigationImportModeGroup.isEnabled = isOfflineCapable
        binding.navigationImportModeRadius.isEnabled = isOfflineCapable
        binding.navigationImportModeManual.isEnabled = isOfflineCapable
        binding.navigationOfflineCheckButton.isEnabled = isOfflineCapable
        val manual = Prefs.getNavigationImportMode(this) == Prefs.NAV_IMPORT_MODE_MANUAL
        val enabled = isOfflineCapable && manual
        binding.navigationImportMinLatInput.isEnabled = enabled
        binding.navigationImportMinLonInput.isEnabled = enabled
        binding.navigationImportMaxLatInput.isEnabled = enabled
        binding.navigationImportMaxLonInput.isEnabled = enabled
        updateOfflineSegmentUi()
    }

    private fun updateOfflineStatus() {
        val count = Prefs.getNavigationOfflineCount(this)
        val updated = Prefs.getNavigationOfflineUpdated(this)
        if (count <= 0 || updated <= 0L) {
            binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_empty)
            return
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("pl", "PL"))
        binding.navigationOfflineStatus.text = getString(
            R.string.navigation_offline_status,
            count,
            fmt.format(java.util.Date(updated))
        )
    }

    private fun importOfflinePois() {
        val needsLocation = Prefs.getNavigationImportMode(this) == Prefs.NAV_IMPORT_MODE_RADIUS
        if (needsLocation && !hasLocationPermission()) {
            pendingOfflineCheck = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        val categories = selectedTypes()
        if (categories.isEmpty()) {
            Toast.makeText(this, R.string.navigation_tracking_no_categories, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setNavigationCategories(this, categories.joinToString(","))
        val baseUrl = binding.navigationOfflineBaseUrlInput.text?.toString()?.trim().orEmpty()
        if (baseUrl.isBlank()) {
            Toast.makeText(this, R.string.navigation_offline_base_url_missing, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setNavigationOfflineBaseUrl(this, baseUrl)
        val bbox = resolveImportBbox() ?: return
        binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_importing)
        executor.execute {
            var tooMany = false
            var noTiles = false
            val result = try {
                val config = GithubTileClient.Config(
                    baseUrl = baseUrl,
                    zoom = Prefs.getNavigationOfflineZoom(this),
                    maxTiles = Prefs.getNavigationOfflineSegmentSize(this),
                    useGzip = Prefs.isNavigationOfflineGzipEnabled(this)
                )
                if (Prefs.isNavigationOfflineSegmented(this)) {
                    GithubTileClient.fetchTilesSegmentedWithProgress(
                        bbox = bbox,
                        categories = categories.toSet(),
                        config = config,
                        onProgress = { done, total ->
                            runOnUiThread {
                                binding.navigationOfflineStatus.text =
                                    getString(R.string.navigation_offline_progress, done, total)
                            }
                        }
                    )
                } else {
                    GithubTileClient.fetchTilesWithProgress(
                        bbox = bbox,
                        categories = categories.toSet(),
                        config = config,
                        onProgress = { done, total ->
                            runOnUiThread {
                                binding.navigationOfflineStatus.text =
                                    getString(R.string.navigation_offline_progress, done, total)
                            }
                        }
                    )
                }
            } catch (e: IllegalArgumentException) {
                if (e.message == "too_many_tiles") {
                    tooMany = true
                    emptyList()
                } else if (e.message == "no_tiles") {
                    noTiles = true
                    emptyList()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
            runOnUiThread {
                if (tooMany) {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_too_many_tiles)
                    Toast.makeText(this, R.string.navigation_offline_too_many_tiles, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (noTiles) {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_check_failed)
                    Toast.makeText(this, R.string.navigation_offline_check_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                if (result == null) {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_import_failed)
                    Toast.makeText(this, R.string.navigation_offline_import_failed, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                NavigationPoiStore.replaceAll(this, result)
                Prefs.setNavigationOfflineCount(this, result.size)
                Prefs.setNavigationOfflineUpdated(this, System.currentTimeMillis())
                updateOfflineStatus()
                Toast.makeText(this, R.string.navigation_offline_import_done, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearOfflinePois() {
        try {
            NavigationPoiStore.clear(this)
            Prefs.setNavigationOfflineCount(this, 0)
            Prefs.setNavigationOfflineUpdated(this, 0L)
            updateOfflineStatus()
            Toast.makeText(this, R.string.navigation_offline_clear_done, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, R.string.navigation_offline_clear_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTyflomapControls() {
        binding.navigationTyflomapCheckButton.setOnClickListener { fetchTyflomap() }
        binding.navigationTyflomapOpenButton.setOnClickListener { openTyflomapLink() }
        binding.navigationTyflomapShareButton.setOnClickListener { shareTyflomapLink() }
        tyflomapLink = Prefs.getTyflomapLastLink(this)
        updateTyflomapButtons()
    }

    private fun fetchTyflomap() {
        if (!hasLocationPermission()) {
            pendingTyflomap = true
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
        binding.navigationTyflomapStatus.text = getString(R.string.tyflomap_status_loading)
        executor.execute {
            val outcome = try {
                TyflomapClient.findMapLink(location.latitude, location.longitude)
            } catch (_: Exception) {
                "__error__"
            }
            runOnUiThread {
                if (outcome == "__error__") {
                    binding.navigationTyflomapStatus.text = getString(R.string.tyflomap_status_error)
                } else if (outcome.isNullOrBlank()) {
                    binding.navigationTyflomapStatus.text = getString(R.string.tyflomap_status_missing)
                } else {
                    tyflomapLink = outcome
                    Prefs.setTyflomapLastLink(this, outcome)
                    binding.navigationTyflomapStatus.text = getString(R.string.tyflomap_status_found)
                }
                updateTyflomapButtons()
            }
        }
    }

    private fun openTyflomapLink() {
        val link = tyflomapLink ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    private fun shareTyflomapLink() {
        val link = tyflomapLink ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.tyflomap_share_link)))
    }

    private fun updateTyflomapButtons() {
        val has = !tyflomapLink.isNullOrBlank()
        binding.navigationTyflomapOpenButton.isEnabled = has
        binding.navigationTyflomapShareButton.isEnabled = has
    }

    private fun getLastLocation(): android.location.Location? {
        val manager = getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }

    private fun resolveImportBbox(): GithubTileClient.Bbox? {
        return if (Prefs.getNavigationImportMode(this) == Prefs.NAV_IMPORT_MODE_MANUAL) {
            val minLat = binding.navigationImportMinLatInput.text?.toString()?.trim()?.toDoubleOrNull()
            val minLon = binding.navigationImportMinLonInput.text?.toString()?.trim()?.toDoubleOrNull()
            val maxLat = binding.navigationImportMaxLatInput.text?.toString()?.trim()?.toDoubleOrNull()
            val maxLon = binding.navigationImportMaxLonInput.text?.toString()?.trim()?.toDoubleOrNull()
            if (minLat == null || minLon == null || maxLat == null || maxLon == null) {
                Toast.makeText(this, R.string.navigation_import_bounds_invalid, Toast.LENGTH_SHORT).show()
                return null
            }
            Prefs.setNavigationImportMinLat(this, minLat.toFloat())
            Prefs.setNavigationImportMinLon(this, minLon.toFloat())
            Prefs.setNavigationImportMaxLat(this, maxLat.toFloat())
            Prefs.setNavigationImportMaxLon(this, maxLon.toFloat())
            GithubTileClient.Bbox(
                minLat = minOf(minLat, maxLat),
                minLon = minOf(minLon, maxLon),
                maxLat = maxOf(minLat, maxLat),
                maxLon = maxOf(minLon, maxLon)
            )
        } else {
            val location = getLastLocation() ?: run {
                Toast.makeText(this, R.string.navigation_pin_missing, Toast.LENGTH_SHORT).show()
                return null
            }
            val radius = Prefs.getNavigationImportRadius(this)
            val latDelta = radius / 111_000.0
            val lonDelta = radius / (111_000.0 * kotlin.math.cos(Math.toRadians(location.latitude)).coerceAtLeast(0.1))
            GithubTileClient.Bbox(
                minLat = location.latitude - latDelta,
                minLon = location.longitude - lonDelta,
                maxLat = location.latitude + latDelta,
                maxLon = location.longitude + lonDelta
            )
        }
    }

    private fun checkOfflineBase() {
        val needsLocation = Prefs.getNavigationImportMode(this) == Prefs.NAV_IMPORT_MODE_RADIUS
        if (needsLocation && !hasLocationPermission()) {
            pendingOfflineImport = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION
            )
            return
        }
        val baseUrl = binding.navigationOfflineBaseUrlInput.text?.toString()?.trim().orEmpty()
        if (baseUrl.isBlank()) {
            Toast.makeText(this, R.string.navigation_offline_base_url_missing, Toast.LENGTH_SHORT).show()
            return
        }
        Prefs.setNavigationOfflineBaseUrl(this, baseUrl)
        val bbox = resolveImportBbox() ?: return
        binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_checking)
        executor.execute {
            val zoom = Prefs.getNavigationOfflineZoom(this)
            val tiles = GithubTileClient.listTiles(bbox, zoom)
            val config = GithubTileClient.Config(
                baseUrl = baseUrl,
                zoom = zoom,
                maxTiles = Prefs.getNavigationOfflineSegmentSize(this),
                useGzip = Prefs.isNavigationOfflineGzipEnabled(this)
            )
            if (!Prefs.isNavigationOfflineSegmented(this)) {
                val tooMany = tiles.size > config.maxTiles
                if (tooMany) {
                    runOnUiThread {
                        binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_too_many_tiles)
                        Toast.makeText(this, R.string.navigation_offline_too_many_tiles, Toast.LENGTH_SHORT).show()
                    }
                    return@execute
                }
            }
            val tile = tiles.firstOrNull()
            if (tile == null) {
                runOnUiThread {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_check_failed)
                    Toast.makeText(this, R.string.navigation_offline_check_failed, Toast.LENGTH_SHORT).show()
                }
                return@execute
            }
            val url = "${baseUrl.trimEnd('/')}/$zoom/${tile.x}/${tile.y}.json"
            val pois = try {
                GithubTileClient.fetchTile(url, emptySet(), config.useGzip)
            } catch (_: Exception) {
                null
            }
            runOnUiThread {
                if (pois == null) {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_check_failed)
                    Toast.makeText(this, R.string.navigation_offline_check_failed, Toast.LENGTH_SHORT).show()
                } else {
                    binding.navigationOfflineStatus.text = getString(R.string.navigation_offline_check_ok)
                    Toast.makeText(this, R.string.navigation_offline_check_ok, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
