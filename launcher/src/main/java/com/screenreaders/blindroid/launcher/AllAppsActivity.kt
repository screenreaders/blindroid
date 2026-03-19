package com.screenreaders.blindroid.launcher

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
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
    private lateinit var suggestedGrid: RecyclerView
    private lateinit var suggestedLabel: TextView
    private lateinit var categoryRow: LinearLayout
    private lateinit var adapter: AppAdapter
    private lateinit var suggestedAdapter: AppAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var suggestedLayoutManager: GridLayoutManager
    private var allApps: List<AppEntry> = emptyList()
    private var filteredApps: List<AppEntry> = emptyList()
    private var targetPageIndex: Int = 0
    private var targetFolderId: String? = null
    private var currentCategory: Category = Category.ALL

    private enum class Category(val labelRes: Int, val appCategory: Int?) {
        ALL(R.string.launcher_category_all, null),
        MEDIA(R.string.launcher_category_media, ApplicationInfo.CATEGORY_AUDIO),
        PRODUCTIVITY(R.string.launcher_category_productivity, ApplicationInfo.CATEGORY_PRODUCTIVITY),
        SOCIAL(R.string.launcher_category_social, ApplicationInfo.CATEGORY_SOCIAL),
        GAME(R.string.launcher_category_game, ApplicationInfo.CATEGORY_GAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_apps)
        searchInput = findViewById(R.id.searchInput)
        appsGrid = findViewById(R.id.appsGrid)
        suggestedGrid = findViewById(R.id.suggestedGrid)
        suggestedLabel = findViewById(R.id.suggestedLabel)
        categoryRow = findViewById(R.id.categoryRow)

        targetPageIndex = intent.getIntExtra(EXTRA_PAGE_INDEX, 0)
        targetFolderId = intent.getStringExtra(EXTRA_FOLDER_ID)

        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager = GridLayoutManager(this, baseConfig.columns)
        suggestedLayoutManager = GridLayoutManager(this, baseConfig.columns)
        appsGrid.layoutManager = gridLayoutManager
        suggestedGrid.layoutManager = suggestedLayoutManager
        adapter = AppAdapter(emptyList(), baseConfig.copy(showLabels = true), ::launchApp, ::handleLongPress)
        suggestedAdapter = AppAdapter(emptyList(), baseConfig.copy(showLabels = true), ::launchApp, ::handleLongPress)
        appsGrid.adapter = adapter
        suggestedGrid.adapter = suggestedAdapter

        setupCategories()
        loadApps()
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        applyUiConfig()
        applyTheme()
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                applyFilters()
                updateSuggested()
            }
        }.start()
    }

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        gridLayoutManager.spanCount = baseConfig.columns
        suggestedLayoutManager.spanCount = baseConfig.columns
        adapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
        suggestedAdapter.updateConfig(baseConfig.copy(itemHeightPx = 0, showLabels = true))
    }

    private fun applyTheme() {
        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<android.view.View>(android.R.id.content).setBackgroundColor(colors.background)
        searchInput.setTextColor(colors.text)
        searchInput.setHintTextColor(colors.muted)
        suggestedLabel.setTextColor(colors.text)
        for (i in 0 until categoryRow.childCount) {
            val button = categoryRow.getChildAt(i) as android.widget.Button
            button.setTextColor(colors.text)
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })
    }

    private fun setupCategories() {
        categoryRow.removeAllViews()
        Category.values().forEach { category ->
            val button = android.widget.Button(this)
            button.text = getString(category.labelRes)
            button.setOnClickListener {
                currentCategory = category
                applyFilters()
                updateCategoryButtons()
            }
            categoryRow.addView(button)
        }
        updateCategoryButtons()
    }

    private fun updateCategoryButtons() {
        for (i in 0 until categoryRow.childCount) {
            val button = categoryRow.getChildAt(i) as android.widget.Button
            val category = Category.values()[i]
            val selected = category == currentCategory
            button.alpha = if (selected) 1.0f else 0.5f
            button.isEnabled = !selected
        }
    }

    private fun applyFilters() {
        val query = searchInput.text?.toString()?.trim().orEmpty().lowercase()
        filteredApps = allApps.filter { entry ->
            val matchesQuery = query.isBlank() || entry.label.lowercase().contains(query)
            val matchesCategory = when (currentCategory) {
                Category.ALL -> true
                Category.MEDIA -> {
                    val cat = getAppCategory(entry)
                    cat == ApplicationInfo.CATEGORY_AUDIO ||
                        cat == ApplicationInfo.CATEGORY_VIDEO ||
                        cat == ApplicationInfo.CATEGORY_IMAGE
                }
                else -> getAppCategory(entry) == currentCategory.appCategory
            }
            matchesQuery && matchesCategory
        }
        adapter.submit(filteredApps)
    }

    private fun getAppCategory(entry: AppEntry): Int? {
        return try {
            val info = packageManager.getApplicationInfo(entry.component.packageName, 0)
            info.category
        } catch (_: Exception) {
            null
        }
    }

    private fun updateSuggested() {
        val suggested = LauncherStore.getSuggestedApps(this, allApps, 4)
        if (suggested.isEmpty()) {
            suggestedLabel.visibility = android.view.View.GONE
            suggestedGrid.visibility = android.view.View.GONE
        } else {
            suggestedLabel.visibility = android.view.View.VISIBLE
            suggestedGrid.visibility = android.view.View.VISIBLE
            suggestedAdapter.submit(suggested)
        }
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
        LauncherStore.recordLaunch(this, entry.component)
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(entry.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
