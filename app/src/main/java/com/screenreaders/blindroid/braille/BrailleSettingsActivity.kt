package com.screenreaders.blindroid.braille

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.R
import com.screenreaders.blindroid.braillekeyboard.PreferenceIME

class BrailleSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_braille_settings)

        findViewById<Button>(R.id.brailleOpenSettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.brailleOpenKeyboardSettings).setOnClickListener {
            startActivity(Intent(this, PreferenceIME::class.java))
        }
    }
}
