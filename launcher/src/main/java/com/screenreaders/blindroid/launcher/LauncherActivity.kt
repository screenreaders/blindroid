package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import java.text.Collator

class LauncherActivity : AppCompatActivity() {
    private lateinit var appsList: ListView
    private var entries: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        appsList = findViewById(R.id.appsList)
        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val currentPackage = packageName
        val list = resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            if (packageName == currentPackage) return@mapNotNull null
            val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return@mapNotNull null
            val label = info.loadLabel(pm)?.toString() ?: packageName
            AppEntry(label, launchIntent)
        }
        val collator = Collator.getInstance()
        entries = list.sortedWith(compareBy(collator) { it.label })
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            entries.map { it.label }
        )
        appsList.adapter = adapter
        appsList.setOnItemClickListener { _, _, position, _ ->
            val entry = entries.getOrNull(position) ?: return@setOnItemClickListener
            val launch = entry.intent
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
    }

    private data class AppEntry(
        val label: String,
        val intent: Intent
    )
}
