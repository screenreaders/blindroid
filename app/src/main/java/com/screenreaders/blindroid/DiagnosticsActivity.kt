package com.screenreaders.blindroid

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.screenreaders.blindroid.diagnostics.DiagnosticLog

class DiagnosticsActivity : AppCompatActivity() {
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostics)

        logView = findViewById(R.id.diagLog)
        findViewById<Button>(R.id.diagRefreshButton).setOnClickListener { refresh() }
        findViewById<Button>(R.id.diagClearButton).setOnClickListener {
            DiagnosticLog.clear(this)
            refresh()
        }
        findViewById<Button>(R.id.diagShareButton).setOnClickListener { share() }

        refresh()
    }

    private fun refresh() {
        val lines = DiagnosticLog.dump(this)
        logView.text = if (lines.isEmpty()) {
            getString(R.string.diagnostics_empty)
        } else {
            lines.joinToString(separator = "\n")
        }
    }

    private fun share() {
        val lines = DiagnosticLog.dump(this)
        val text = if (lines.isEmpty()) getString(R.string.diagnostics_empty) else lines.joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Blindroid diagnostics")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.diagnostics_share)))
    }
}
