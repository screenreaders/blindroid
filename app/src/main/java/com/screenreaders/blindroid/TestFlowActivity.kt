package com.screenreaders.blindroid

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.accessibility.TalkbackWizardActivity

class TestFlowActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val expectedToken = "${BuildConfig.APPLICATION_ID}:${BuildConfig.VERSION_CODE}"
        val token = intent.getStringExtra(EXTRA_TOKEN)
        if (token != expectedToken) {
            finish()
            return
        }
        when (intent.getStringExtra(EXTRA_TARGET)) {
            TARGET_ONBOARDING -> startActivity(Intent(this, OnboardingActivity::class.java))
            TARGET_DIAGNOSTICS -> startActivity(Intent(this, DiagnosticsActivity::class.java))
            TARGET_TALKBACK_WIZARD -> startActivity(Intent(this, TalkbackWizardActivity::class.java))
        }
        finish()
    }

    companion object {
        const val EXTRA_TARGET = "blindroid_test_target"
        const val EXTRA_TOKEN = "blindroid_test_token"
        const val TARGET_ONBOARDING = "onboarding"
        const val TARGET_DIAGNOSTICS = "diagnostics"
        const val TARGET_TALKBACK_WIZARD = "talkback_wizard"
    }
}
