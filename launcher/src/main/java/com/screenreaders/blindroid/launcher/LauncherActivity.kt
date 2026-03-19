package com.screenreaders.blindroid.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LauncherActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var pinnedGrid: RecyclerView
    private lateinit var allAppsButton: Button
    private lateinit var adapter: AppAdapter
    private var allApps: List<AppEntry> = emptyList()
    private var pinned: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        searchInput = findViewById(R.id.searchInput)
        pinnedGrid = findViewById(R.id.pinnedGrid)
        allAppsButton = findViewById(R.id.allAppsButton)

        pinnedGrid.layoutManager = GridLayoutManager(this, 4)
        adapter = AppAdapter(emptyList(), ::launchApp) { entry ->
            val removed = LauncherStore.removePinned(this, entry.component)
            if (removed) {
                Toast.makeText(this, "Usunięto z ekranu głównego", Toast.LENGTH_SHORT).show()
                refreshPinned()
            }
        }
        pinnedGrid.adapter = adapter

        allAppsButton.setOnClickListener {
            startActivity(Intent(this, AllAppsActivity::class.java))
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                launchSearch(searchInput.text?.toString())
                true
            } else {
                false
            }
        }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        refreshPinned()
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                refreshPinned()
            }
        }.start()
    }

    private fun refreshPinned() {
        if (allApps.isEmpty()) return
        pinned = LauncherStore.loadPinned(this, allApps)
        adapter.submit(pinned)
    }

    private fun launchApp(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun launchSearch(query: String?) {
        val text = query?.trim().orEmpty()
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        if (text.isNotBlank()) {
            intent.putExtra(SearchManager.QUERY, text)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Brak aplikacji wyszukiwania", Toast.LENGTH_SHORT).show()
        }
    }
}
