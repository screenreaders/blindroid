package com.screenreaders.blindroid.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
        Category("lodging", R.string.navigation_category_hotel)
    )
    private val selected = BooleanArray(categories.size)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navigationModeDriving.isChecked = true
        binding.navigationStartButton.setOnClickListener { startNavigation() }
        binding.navigationTrackingSwitch.isChecked = Prefs.isNavigationTrackingEnabled(this)
        binding.navigationTrackingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationTrackingEnabled(this, isChecked)
            if (isChecked) {
                startTracking()
            } else {
                NavigationPoiService.stop(this)
                Toast.makeText(this, R.string.navigation_tracking_stopped, Toast.LENGTH_SHORT).show()
            }
        }
        binding.navigationCategoriesButton.setOnClickListener { openCategoryDialog() }
        binding.navigationApiKeyInput.setText(Prefs.getNavigationApiKey(this))
        binding.navigationApiKeyInput.setOnFocusChangeListener { _, _ ->
            Prefs.setNavigationApiKey(this, binding.navigationApiKeyInput.text?.toString().orEmpty())
        }
        initPoiControls()
        loadSelectedCategories()
        updateCategorySummary()
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
        if (binding.navigationTrackingSwitch.isChecked) {
            startTracking()
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

    private fun startTracking() {
        val apiKey = binding.navigationApiKeyInput.text?.toString()?.trim().orEmpty()
        Prefs.setNavigationApiKey(this, apiKey)
        if (apiKey.isBlank()) {
            Toast.makeText(this, R.string.navigation_tracking_missing_key, Toast.LENGTH_SHORT).show()
            binding.navigationTrackingSwitch.isChecked = false
            return
        }
        val selectedTypes = selectedTypes()
        if (selectedTypes.isEmpty()) {
            Toast.makeText(this, R.string.navigation_tracking_no_categories, Toast.LENGTH_SHORT).show()
            binding.navigationTrackingSwitch.isChecked = false
            return
        }
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
            if (granted && binding.navigationTrackingSwitch.isChecked) {
                NavigationPoiService.start(this)
            } else {
                binding.navigationTrackingSwitch.isChecked = false
            }
        }
    }

    private data class Category(val type: String, val labelRes: Int)

    companion object {
        private const val REQ_LOCATION = 812
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

        binding.navigationMovingOnlySwitch.isChecked = Prefs.isNavigationMovingOnly(this)
        binding.navigationMovingOnlySwitch.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setNavigationMovingOnly(this, isChecked)
        }
    }
}
