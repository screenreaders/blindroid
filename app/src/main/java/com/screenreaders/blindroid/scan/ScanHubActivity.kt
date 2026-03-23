package com.screenreaders.blindroid.scan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.currency.CurrencyActivity
import com.screenreaders.blindroid.data.Prefs
import com.screenreaders.blindroid.databinding.ActivityScanHubBinding
import com.screenreaders.blindroid.document.DocumentAssistActivity
import com.screenreaders.blindroid.face.FaceAssistActivity
import com.screenreaders.blindroid.light.LightActivity
import com.screenreaders.blindroid.obstacle.ObstacleAssistActivity

class ScanHubActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.documentsButton.setOnClickListener {
            startActivity(Intent(this, DocumentAssistActivity::class.java))
        }
        binding.currencyButton.setOnClickListener {
            startActivity(Intent(this, CurrencyActivity::class.java))
        }
        binding.faceButton.setOnClickListener {
            if (!Prefs.isFaceAssistEnabled(this)) {
                Toast.makeText(this, R.string.face_enable, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, FaceAssistActivity::class.java))
        }
        binding.obstacleButton.setOnClickListener {
            startActivity(Intent(this, ObstacleAssistActivity::class.java))
        }
        binding.lightButton.setOnClickListener {
            startActivity(Intent(this, LightActivity::class.java))
        }
    }
}
