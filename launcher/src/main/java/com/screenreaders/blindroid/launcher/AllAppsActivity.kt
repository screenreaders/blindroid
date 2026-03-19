package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllAppsActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var appsGrid: RecyclerView
    private lateinit var adapter: AppAdapter
    private var allApps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        searchInput = findViewById(R.id.searchInput)
        appsGrid = findViewById(R.id.appsGrid)
        appsGrid.layoutManager = GridLayoutManager(this, 4)
        adapter = AppAdapter(emptyList(), ::launchApp) { entry ->
            val added = LauncherStore.addPinned(this, entry.component)
            Toast.makeText(
                this,
                if (added) "Dodano do ekranu głównego" else "Już na ekranie głównym",
                Toast.LENGTH_SHORT
            ).show()
        }
        appsGrid.adapter = adapter

        loadApps()
        setupSearch()
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                adapter.submit(allApps)
            }
        }.start()
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty().lowercase()
                if (query.isBlank()) {
                    adapter.submit(allApps)
                } else {
                    adapter.submit(allApps.filter { it.label.lowercase().contains(query) })
                }
            }
        })
    }

    private fun launchApp(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
