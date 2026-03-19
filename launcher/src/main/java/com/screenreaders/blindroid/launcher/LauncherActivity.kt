package com.screenreaders.blindroid.launcher

import android.app.SearchManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class LauncherActivity : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var homePager: ViewPager2
    private lateinit var hotseatRow: RecyclerView
    private lateinit var allAppsButton: Button
    private lateinit var widgetsButton: Button
    private lateinit var settingsButton: Button
    private lateinit var gestureDetector: GestureDetector
    private var twoFingerActive = false
    private var twoFingerStartX = 0f
    private var twoFingerStartY = 0f
    private var twoFingerLastX = 0f
    private var twoFingerLastY = 0f
    private var threeFingerActive = false
    private var threeFingerStartX = 0f
    private var threeFingerStartY = 0f
    private var threeFingerLastX = 0f
    private var threeFingerLastY = 0f

    private lateinit var homeAdapter: HomePagerAdapter
    private lateinit var hotseatAdapter: HomeItemAdapter

    private var allApps: List<AppEntry> = emptyList()
    private var pages: List<List<HomeItem>> = emptyList()
    private var hotseat: List<HomeItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        searchInput = findViewById(R.id.searchInput)
        homePager = findViewById(R.id.homePager)
        hotseatRow = findViewById(R.id.hotseatRow)
        allAppsButton = findViewById(R.id.allAppsButton)
        widgetsButton = findViewById(R.id.widgetsButton)
        settingsButton = findViewById(R.id.settingsButton)

        val baseConfig = LauncherPrefs.getUiConfig(this)

        homeAdapter = HomePagerAdapter(
            pages.map { it.toMutableList() }.toMutableList(),
            baseConfig,
            ::onHomeItemClick,
            ::onHomeItemLongClick,
            ::onHomeItemMoved
        )
        homePager.adapter = homeAdapter
        homePager.offscreenPageLimit = 1

        hotseatAdapter = HomeItemAdapter(
            hotseat.toMutableList(),
            LauncherPrefs.getDockConfig(this, 0),
            ::onHotseatItemClick,
            ::onHotseatItemLongClick
        )
        hotseatRow.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        hotseatRow.adapter = hotseatAdapter
        setupHotseatDrag()

        allAppsButton.setOnClickListener { openAllApps() }

        widgetsButton.setOnClickListener {
            startActivity(Intent(this, WidgetsActivity::class.java))
        }

        settingsButton.setOnClickListener { openSettings() }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                launchSearch(searchInput.text?.toString())
                true
            } else {
                false
            }
        }

        setupGestureDetector()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        refreshHome()
        applyUiConfig()
    }

    private fun loadApps() {
        Thread {
            val apps = LauncherStore.loadAllApps(this)
            runOnUiThread {
                allApps = apps
                refreshHome()
                applyUiConfig()
            }
        }.start()
    }

    private fun refreshHome() {
        if (allApps.isEmpty()) return
        val allPages = LauncherStore.loadPages(this, allApps)
        pages = if (LauncherPrefs.isSuperSimpleEnabled(this)) {
            listOf(allPages.firstOrNull() ?: emptyList())
        } else {
            allPages
        }
        hotseat = LauncherStore.loadHotseat(this, allApps)
        homeAdapter.submitPages(pages)
        hotseatAdapter.submit(hotseat)
    }

    private fun applyUiConfig() {
        val baseConfig = LauncherPrefs.getUiConfig(this)
        homePager.post {
            val rows = baseConfig.rows
            val itemHeight = if (rows > 0) homePager.height / rows else 0
            val pageConfig = baseConfig.copy(itemHeightPx = itemHeight, showLabels = true)
            homeAdapter.updateConfig(pageConfig)
            hotseatAdapter.updateConfig(LauncherPrefs.getDockConfig(this, 0))
        }
        val simple = LauncherPrefs.isSuperSimpleEnabled(this)
        searchInput.visibility = if (simple) View.GONE else View.VISIBLE
        widgetsButton.visibility = if (simple) View.GONE else View.VISIBLE
        settingsButton.visibility = if (simple) View.GONE else View.VISIBLE
        allAppsButton.visibility = View.VISIBLE
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (LauncherPrefs.isDoubleTapLockEnabled(this@LauncherActivity)) {
                    attemptLock()
                }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                openSettings()
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                val absX = kotlin.math.abs(diffX)
                val absY = kotlin.math.abs(diffY)
                if (absY > absX && absY > 120 && kotlin.math.abs(velocityY) > 120) {
                    if (diffY < 0) {
                        openAllApps()
                    } else {
                        focusSearch()
                    }
                    return true
                }
                return false
            }
        })
        val listener = View.OnTouchListener { _, event ->
            handleTwoFingerGesture(event)
            handleThreeFingerGesture(event)
            gestureDetector.onTouchEvent(event)
            false
        }
        findViewById<View>(R.id.launcherRoot).setOnTouchListener(listener)
        homePager.setOnTouchListener(listener)
    }

    private fun openAllApps() {
        val intent = Intent(this, AllAppsActivity::class.java)
        intent.putExtra(AllAppsActivity.EXTRA_PAGE_INDEX, homePager.currentItem)
        startActivity(intent)
    }

    private fun openWidgets() {
        startActivity(Intent(this, WidgetsActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, LauncherSettingsActivity::class.java))
    }

    private fun openQuickSettings() {
        val intent = Intent("android.settings.QUICK_SETTINGS")
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    private fun focusSearch() {
        searchInput.requestFocus()
        searchInput.setSelection(searchInput.text?.length ?: 0)
    }

    private fun goToNextPage() {
        val next = (homePager.currentItem + 1).coerceAtMost(homeAdapter.itemCount - 1)
        homePager.currentItem = next
    }

    private fun goToPrevPage() {
        val prev = (homePager.currentItem - 1).coerceAtLeast(0)
        homePager.currentItem = prev
    }

    private fun handleTwoFingerGesture(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    twoFingerActive = true
                    twoFingerStartX = averageX(event, 2)
                    twoFingerStartY = averageY(event, 2)
                    twoFingerLastX = twoFingerStartX
                    twoFingerLastY = twoFingerStartY
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (twoFingerActive && event.pointerCount >= 2) {
                    twoFingerLastX = averageX(event, 2)
                    twoFingerLastY = averageY(event, 2)
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (twoFingerActive) {
                    val deltaX = twoFingerLastX - twoFingerStartX
                    val deltaY = twoFingerLastY - twoFingerStartY
                    val absX = kotlin.math.abs(deltaX)
                    val absY = kotlin.math.abs(deltaY)
                    if (absX > absY && absX > 120) {
                        if (deltaX > 0) {
                            goToNextPage()
                        } else {
                            goToPrevPage()
                        }
                    } else if (absY > 120) {
                        if (deltaY < 0) {
                            openWidgets()
                        } else {
                            openSettings()
                        }
                    }
                }
                twoFingerActive = false
            }
        }
    }

    private fun handleThreeFingerGesture(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 3) {
                    threeFingerActive = true
                    threeFingerStartX = averageX(event, 3)
                    threeFingerStartY = averageY(event, 3)
                    threeFingerLastX = threeFingerStartX
                    threeFingerLastY = threeFingerStartY
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (threeFingerActive && event.pointerCount >= 3) {
                    threeFingerLastX = averageX(event, 3)
                    threeFingerLastY = averageY(event, 3)
                }
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (threeFingerActive) {
                    val deltaX = threeFingerLastX - threeFingerStartX
                    val deltaY = threeFingerLastY - threeFingerStartY
                    val absX = kotlin.math.abs(deltaX)
                    val absY = kotlin.math.abs(deltaY)
                    if (absX > absY && absX > 120) {
                        if (deltaX > 0) {
                            openQuickSettings()
                        } else {
                            focusSearch()
                        }
                    } else if (absY > 120) {
                        if (deltaY < 0) {
                            openAllApps()
                        } else {
                            openQuickSettings()
                        }
                    }
                }
                threeFingerActive = false
            }
        }
    }

    private fun averageX(event: MotionEvent, count: Int): Float {
        val safe = minOf(count, event.pointerCount)
        var sum = 0f
        for (i in 0 until safe) {
            sum += event.getX(i)
        }
        return sum / safe
    }

    private fun averageY(event: MotionEvent, count: Int): Float {
        val safe = minOf(count, event.pointerCount)
        var sum = 0f
        for (i in 0 until safe) {
            sum += event.getY(i)
        }
        return sum / safe
    }

    private fun setupHotseatDrag() {
        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                0
            ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                hotseatAdapter.moveItem(from, to)
                LauncherStore.moveItemInHotseat(this@LauncherActivity, from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
        touchHelper.attachToRecyclerView(hotseatRow)
    }

    private fun attemptLock() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, LauncherAdminReceiver::class.java)
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow()
            Toast.makeText(this, R.string.launcher_lock_enabled, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.launcher_lock_requires_admin, Toast.LENGTH_SHORT).show()
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.launcher_admin_explanation)
            )
            startActivity(intent)
        }
    }

    private fun onHomeItemClick(pageIndex: Int, item: HomeItem) {
        when (item) {
            is HomeItem.App -> launchApp(item.component)
            is HomeItem.Folder -> openFolder(item.id)
        }
    }

    private fun onHomeItemLongClick(pageIndex: Int, item: HomeItem) {
        when (item) {
            is HomeItem.App -> showHomeAppOptions(pageIndex, item)
            is HomeItem.Folder -> showFolderOptions(pageIndex, item)
        }
    }

    private fun onHotseatItemClick(item: HomeItem) {
        when (item) {
            is HomeItem.App -> launchApp(item.component)
            is HomeItem.Folder -> openFolder(item.id)
        }
    }

    private fun onHotseatItemLongClick(item: HomeItem) {
        when (item) {
            is HomeItem.App -> showHotseatAppOptions(item)
            is HomeItem.Folder -> showFolderOptions(homePager.currentItem, item)
        }
    }

    private fun onHomeItemMoved(pageIndex: Int, from: Int, to: Int) {
        LauncherStore.moveItemInPage(this, pageIndex, from, to)
    }

    private fun showHomeAppOptions(pageIndex: Int, app: HomeItem.App) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_remove_from_home) to {
            LauncherStore.removeFromPage(this, pageIndex, appKey(app))
            refreshHome()
        }
        options += getString(R.string.launcher_action_move_to_hotseat) to {
            val added = LauncherStore.addToHotseat(this, app.component)
            if (added) {
                LauncherStore.removeFromPage(this, pageIndex, appKey(app))
                refreshHome()
                Toast.makeText(this, R.string.launcher_added_to_hotseat, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.launcher_already_in_hotseat, Toast.LENGTH_SHORT).show()
            }
        }
        options += getString(R.string.launcher_action_create_folder) to {
            promptCreateFolder(pageIndex, app)
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker { folderId ->
                val added = LauncherStore.addToFolder(this, folderId, app.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        showOptionsDialog(getString(R.string.launcher_home_options), options)
    }

    private fun showHotseatAppOptions(app: HomeItem.App) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_remove_from_hotseat) to {
            LauncherStore.removeFromHotseat(this, appKey(app))
            refreshHome()
        }
        options += getString(R.string.launcher_action_move_to_home) to {
            val pageIndex = homePager.currentItem
            val added = LauncherStore.addToPage(this, pageIndex, app.component)
            if (added) {
                LauncherStore.removeFromHotseat(this, appKey(app))
                refreshHome()
                Toast.makeText(this, R.string.launcher_added_to_home, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.launcher_already_on_home, Toast.LENGTH_SHORT).show()
            }
        }
        options += getString(R.string.launcher_action_add_to_folder) to {
            showFolderPicker { folderId ->
                val added = LauncherStore.addToFolder(this, folderId, app.component)
                Toast.makeText(
                    this,
                    if (added) R.string.launcher_added_to_folder else R.string.launcher_already_in_folder,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        showOptionsDialog(getString(R.string.launcher_hotseat_options), options)
    }

    private fun showFolderOptions(pageIndex: Int, folder: HomeItem.Folder) {
        val options = mutableListOf<Pair<String, () -> Unit>>()
        options += getString(R.string.launcher_action_open_folder) to { openFolder(folder.id) }
        options += getString(R.string.launcher_action_rename_folder) to { promptRenameFolder(folder) }
        options += getString(R.string.launcher_action_delete_folder) to {
            AlertDialog.Builder(this)
                .setTitle(R.string.launcher_action_delete_folder)
                .setMessage(R.string.launcher_delete_folder_message)
                .setPositiveButton(R.string.launcher_action_delete_folder) { _, _ ->
                    LauncherStore.removeFolderFromPage(this, pageIndex, folder.id, true)
                    refreshHome()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        showOptionsDialog(folder.label, options)
    }

    private fun promptCreateFolder(pageIndex: Int, first: HomeItem.App) {
        val candidates = pages.getOrNull(pageIndex)
            ?.filterIsInstance<HomeItem.App>()
            ?.filter { it.component != first.component }
            .orEmpty()
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folder_candidates, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder_app)
            .setItems(labels) { _, which ->
                val second = candidates[which]
                LauncherStore.createFolderOnPage(this, pageIndex, first.component, second.component)
                refreshHome()
            }
            .show()
    }

    private fun promptRenameFolder(folder: HomeItem.Folder) {
        val input = EditText(this)
        input.setText(folder.label)
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_rename_folder)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                LauncherStore.setFolderLabel(this, folder.id, input.text.toString())
                refreshHome()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showFolderPicker(onPick: (String) -> Unit) {
        val folders = LauncherStore.listFolders(this)
        if (folders.isEmpty()) {
            Toast.makeText(this, R.string.launcher_no_folders, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = folders.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_choose_folder)
            .setItems(labels) { _, which ->
                onPick(folders[which].id)
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

    private fun openFolder(folderId: String) {
        val intent = Intent(this, FolderActivity::class.java)
        intent.putExtra(FolderActivity.EXTRA_FOLDER_ID, folderId)
        startActivity(intent)
    }

    private fun launchApp(component: ComponentName) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
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
            Toast.makeText(this, R.string.launcher_search_missing, Toast.LENGTH_SHORT).show()
        }
    }

    private fun appKey(app: HomeItem.App): String = "a:${app.component.flattenToString()}"
}
