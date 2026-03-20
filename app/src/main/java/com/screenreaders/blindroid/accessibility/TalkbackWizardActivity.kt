package com.screenreaders.blindroid.accessibility

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.databinding.ActivityTalkbackWizardBinding

class TalkbackWizardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTalkbackWizardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTalkbackWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.talkbackSettingsButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.talkbackRefreshButton.setOnClickListener {
            updateStatus()
        }
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = TalkbackUtils.isTalkBackEnabled(this)
        binding.talkbackStatus.text = getString(
            if (enabled) R.string.talkback_status_on else R.string.talkback_status_off
        )
    }
}
