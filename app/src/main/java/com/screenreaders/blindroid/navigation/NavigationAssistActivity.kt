package com.screenreaders.blindroid.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.databinding.ActivityNavigationAssistBinding

class NavigationAssistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNavigationAssistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationAssistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navigationModeDriving.isChecked = true
        binding.navigationStartButton.setOnClickListener { startNavigation() }
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
        val mode = if (binding.navigationModeDriving.isChecked) "driving" else "walking"
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
}
