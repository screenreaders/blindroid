package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FolderActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FOLDER_ID = "extra_folder_id"
    }

    private lateinit var folderTitle: TextView
    private lateinit var renameButton: Button
    private lateinit var addButton: Button
    private lateinit var folderGrid: RecyclerView
    private lateinit var adapter: AppAdapter
    private lateinit var gridLayoutManager: GridLayoutManager

    private var allApps: List<AppEntry> = emptyList()
    private var folderId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folder)

        folderId = intent.getStringExtra(EXTRA_FOLDER_ID) ?: ""
        if (folderId.isBlank()) {
            finish()
            return
        }

        folderTitle = findViewById(R.id.folderTitle)
        renameButton = findViewById(R.id.renameFolderButton)
        addButton = findViewById(R.id.addFolderAppButton)
        folderGrid = findViewById(R.id.folderGrid)

        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager = GridLayoutManager(this, baseConfig.columns)
        folderGrid.layoutManager = gridLayoutManager
        adapter = AppAdapter(emptyList(), baseConfig.copy(showLabels = true), ::launchApp, ::removeFromFolder)
        folderGrid.adapter = adapter

        renameButton.setOnClickListener { promptRename() }
        addButton.setOnClickListener { openAllAppsForFolder() }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        applyUiConfig()
        refreshFolder()
        applyTheme()
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                refreshFolder()
            }
        }.start()
    }

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager.spanCount = baseConfig.columns
        adapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
    }

    private fun applyTheme() {
        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(colors.background)
        folderTitle.setTextColor(colors.text)
        ThemeUtils.tintButton(renameButton, colors, false)
        ThemeUtils.tintButton(addButton, colors, true)
    }

    private fun refreshFolder() {
        if (allApps.isEmpty()) return
        folderTitle.text = LauncherStore.getFolderLabel(this, folderId)
        val entries = LauncherStore.getFolderAppEntries(this, folderId, allApps)
        adapter.submit(entries)
    }

    private fun promptRename() {
        val input = EditText(this)
        input.setText(LauncherStore.getFolderLabel(this, folderId))
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_rename_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                LauncherStore.setFolderLabel(this, folderId, input.text.toString())
                refreshFolder()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openAllAppsForFolder() {
        val intent = Intent(this, AllAppsActivity::class.java)
        intent.putExtra(AllAppsActivity.EXTRA_FOLDER_ID, folderId)
        startActivity(intent)
    }

    private fun removeFromFolder(entry: AppEntry) {
        val removed = LauncherStore.removeFromFolder(this, folderId, entry.component)
        Toast.makeText(
            this,
            if (removed) R.string.launcher_removed_from_folder else R.string.launcher_not_in_folder,
            Toast.LENGTH_SHORT
        ).show()
        refreshFolder()
    }

    private fun launchApp(entry: AppEntry) {
        LauncherStore.recordLaunch(this, entry.component)
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
