package com.screenreaders.blindroid.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout

class WidgetsActivity : AppCompatActivity() {
    companion object {
        private const val HOST_ID = 1024
        private const val REQUEST_PICK_WIDGET = 1001
        private const val REQUEST_BIND_WIDGET = 1002
        private const val REQUEST_CONFIGURE_WIDGET = 1003
    }

    private lateinit var addButton: Button
    private lateinit var listButton: Button
    private lateinit var gridSwitch: android.widget.Switch
    private lateinit var container: GridLayout
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var appWidgetManager: AppWidgetManager

    private var pendingWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingProvider: AppWidgetProviderInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widgets)

        addButton = findViewById(R.id.addWidgetButton)
        listButton = findViewById(R.id.listWidgetButton)
        gridSwitch = findViewById(R.id.gridWidgetsSwitch)
        container = findViewById(R.id.widgetsContainer)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, HOST_ID)

        addButton.setOnClickListener { pickWidget() }
        listButton.setOnClickListener { showWidgetList() }
        gridSwitch.setOnCheckedChangeListener { _, isChecked ->
            container.columnCount = if (isChecked) 2 else 1
            reloadWidgets()
        }

        applyTheme()
        loadWidgets()
    }

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        appWidgetHost.stopListening()
    }

    private fun applyTheme() {
        val colors = LauncherPrefs.getThemeColors(this)
        findViewById<View>(android.R.id.content).setBackgroundColor(colors.background)
        addButton.setTextColor(colors.text)
        listButton.setTextColor(colors.text)
        gridSwitch.setTextColor(colors.text)
    }

    private fun loadWidgets() {
        reloadWidgets()
    }

    private fun reloadWidgets() {
        container.removeAllViews()
        LauncherStore.getWidgetIds(this).forEach { widgetId ->
            addWidgetView(widgetId)
        }
    }

    private fun pickWidget() {
        pendingWidgetId = appWidgetHost.allocateAppWidgetId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        startActivityForResult(intent, REQUEST_PICK_WIDGET)
    }

    private fun showWidgetList() {
        val providers = appWidgetManager.installedProviders
        if (providers.isNullOrEmpty()) {
            Toast.makeText(this, R.string.launcher_no_widgets, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = providers.map { it.label ?: it.provider.className }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_widgets_list)
            .setItems(labels) { _, which ->
                val info = providers[which]
                pendingProvider = info
                pendingWidgetId = appWidgetHost.allocateAppWidgetId()
                bindWidgetFromProvider(info, pendingWidgetId)
            }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val appWidgetId = data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingWidgetId
        ) ?: pendingWidgetId

        if (resultCode != RESULT_OK) {
            cleanupWidgetId(appWidgetId)
            return
        }

        when (requestCode) {
            REQUEST_PICK_WIDGET -> handleWidgetPicked(appWidgetId)
            REQUEST_BIND_WIDGET -> handleWidgetBound(appWidgetId)
            REQUEST_CONFIGURE_WIDGET -> completeAddWidget(appWidgetId)
        }
    }

    private fun handleWidgetPicked(appWidgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            cleanupWidgetId(appWidgetId)
            return
        }
        bindWidgetFromProvider(info, appWidgetId)
    }

    private fun bindWidgetFromProvider(info: AppWidgetProviderInfo, appWidgetId: Int) {
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
        if (!bound) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            startActivityForResult(intent, REQUEST_BIND_WIDGET)
            return
        }
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = info.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, REQUEST_CONFIGURE_WIDGET)
        } else {
            completeAddWidget(appWidgetId)
        }
    }

    private fun handleWidgetBound(appWidgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (info == null) {
            cleanupWidgetId(appWidgetId)
            return
        }
        if (info.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
            intent.component = info.configure
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(intent, REQUEST_CONFIGURE_WIDGET)
        } else {
            completeAddWidget(appWidgetId)
        }
    }

    private fun completeAddWidget(appWidgetId: Int) {
        LauncherStore.addWidgetId(this, appWidgetId)
        addWidgetView(appWidgetId)
        Toast.makeText(this, R.string.launcher_widget_added, Toast.LENGTH_SHORT).show()
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingProvider = null
    }

    private fun addWidgetView(widgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return
        val hostView = appWidgetHost.createView(this, widgetId, info)
        val wrapper = LinearLayout(this)
        wrapper.orientation = LinearLayout.VERTICAL

        val controls = LinearLayout(this)
        controls.orientation = LinearLayout.HORIZONTAL
        val bigger = Button(this)
        val smaller = Button(this)
        bigger.text = getString(R.string.launcher_widget_bigger)
        smaller.text = getString(R.string.launcher_widget_smaller)
        controls.addView(bigger)
        controls.addView(smaller)
        wrapper.addView(controls)
        wrapper.addView(hostView)

        val params = GridLayout.LayoutParams()
        params.width = if (container.columnCount > 1) 0 else GridLayout.LayoutParams.MATCH_PARENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.launcher_widget_margin))
        wrapper.layoutParams = params

        val size = LauncherStore.getWidgetSize(this, widgetId)
        if (size != null) {
            hostView.layoutParams = LinearLayout.LayoutParams(size.first, size.second)
        } else {
            val minWidth = dpToPx(info.minWidth.toFloat())
            val minHeight = dpToPx(info.minHeight.toFloat())
            hostView.layoutParams = LinearLayout.LayoutParams(minWidth, minHeight)
            LauncherStore.setWidgetSize(this, widgetId, minWidth, minHeight)
        }

        bigger.setOnClickListener {
            resizeWidget(widgetId, hostView, 30)
        }
        smaller.setOnClickListener {
            resizeWidget(widgetId, hostView, -30)
        }

        hostView.setOnLongClickListener {
            confirmRemoveWidget(widgetId, wrapper)
            true
        }

        container.addView(wrapper)
    }

    private fun resizeWidget(widgetId: Int, view: View, deltaDp: Int) {
        val deltaPx = dpToPx(deltaDp.toFloat())
        val size = LauncherStore.getWidgetSize(this, widgetId)
        val width = (size?.first ?: view.width) + deltaPx
        val height = (size?.second ?: view.height) + deltaPx
        val newW = width.coerceAtLeast(dpToPx(60f))
        val newH = height.coerceAtLeast(dpToPx(60f))
        view.layoutParams = LinearLayout.LayoutParams(newW, newH)
        LauncherStore.setWidgetSize(this, widgetId, newW, newH)
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }

    private fun confirmRemoveWidget(widgetId: Int, wrapper: View) {
        AlertDialog.Builder(this)
            .setTitle(R.string.launcher_action_remove_widget)
            .setMessage(R.string.launcher_remove_widget_message)
            .setPositiveButton(R.string.launcher_action_remove_widget) { _, _ ->
                removeWidget(widgetId, wrapper)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun removeWidget(widgetId: Int, wrapper: View) {
        container.removeView(wrapper)
        LauncherStore.removeWidgetId(this, widgetId)
        appWidgetHost.deleteAppWidgetId(widgetId)
        Toast.makeText(this, R.string.launcher_widget_removed, Toast.LENGTH_SHORT).show()
    }

    private fun cleanupWidgetId(widgetId: Int) {
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }
}
