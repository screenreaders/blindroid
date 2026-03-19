package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllAppsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PAGE_INDEX = "extra_page_index"
        const val EXTRA_FOLDER_ID = "extra_folder_id"
    }

    private lateinit var searchInput: EditText
    private lateinit var appsGrid: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private var allApps: List<AppEntry> = emptyList()
    private var targetPageIndex: Int = 0
    private var targetFolderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        searchInput = findViewById(R.id.searchInput)
        appsGrid = findViewById(R.id.appsGrid)

        targetPageIndex = intent.getIntExtra(EXTRA_PAGE_INDEX, 0)
        targetFolderId = intent.getStringExtra(EXTRA_FOLDER_ID)

        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager = GridLayoutManager(this, baseConfig.columns)
        appsGrid.layoutManager = gridLayoutManager
        adapter = AppAdapter(emptyList(), baseConfig.copy(showLabels = true), ::launchApp, ::handleLongPress)
        appsGrid.adapter = adapter

        loadApps()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        applyUiConfig()
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

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager.spanCount = baseConfig.columns
        adapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
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

    private fun handleLongPress(entry: AppEntry) {
        val folderId = targetFolderId
        if (folderId != null) {
            val added = LauncherStore.addToFolder(this, folderId, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                Toast.LENGTH_SHORT
            ).show()
            if (added) {
                finish()
            }
            return
        }

        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_add_to_home) to {
            val added = LauncherStore.addToPage(this, targetPageIndex, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_home else R.string.launcher_already_on_home,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_add_to_hotseat) to {
            val added = LauncherStore.addToHotseat(this, entry.component)
            Toast.makeText(
                this,
                if (added) R.string.launcher_added_to_hotseat else R.string.launcher_already_in_hotseat,
                Toast.LENGTH_SHORT
            ).show()
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker(entry)
        }
        showOptionsDialog(getString(R.string.launcher_all_apps), options)
    }

    private fun showFolderPicker(entry: AppEntry) {
        val folders = LauncherStore.listFolders(this)
        if (folders.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folders, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = folders.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder)
            .setItems(labels) { _, which ->
                val folderId = folders[which].id
                val added = LauncherStore.addToFolder(this, folderId, entry.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    private fun showOptionsDialog(title: String, options: List<Pair<String, () -> Unit>>) {
        val labels = options.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels) { _, which ->
                options[which].second.invoke()
            }
            .show()
    }

    private fun launchApp(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
