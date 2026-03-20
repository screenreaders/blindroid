package com.screenreaders.blindroid.diagnostics

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.screenreaders.blindroid.BuildConfig
import com.screenreaders.blindroid.data.Prefs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val DIR_NAME = "crash_reports"
    private const val MAX_REPORTS = 5
    private var initialized = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (Prefs.isCrashReportingEnabled(appContext)) {
                try {
                    writeReport(appContext, thread, throwable)
                } catch (_: Exception) {
                    // Ignore reporting failures
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun getLatestReport(context: Context): File? {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) return null
        return dir.listFiles()?.maxByOrNull { it.lastModified() }
    }

    fun clearReports(context: Context) {
        val dir = File(context.filesDir, DIR_NAME)
        dir.listFiles()?.forEach { it.delete() }
    }

    fun buildReportSummary(context: Context): String {
        val file = getLatestReport(context) ?: return "Brak raportów"
        val date = Date(file.lastModified())
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("pl", "PL"))
        return "Ostatni raport: ${fmt.format(date)}"
    }

    fun createShareIntent(context: Context): Intent? {
        val file = getLatestReport(context) ?: return null
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Blindroid crash report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createIssueIntent(context: Context): Intent {
        val title = Uri.encode("Crash report v${BuildConfig.VERSION_NAME}")
        val summary = Uri.encode(buildReportSummary(context))
        val body = Uri.encode(
            "Opis problemu:\n\nKroki do odtworzenia:\n\n" +
                "Dane:\n" +
                "- Wersja: ${BuildConfig.VERSION_NAME}\n" +
                "- Android: ${android.os.Build.VERSION.RELEASE}\n" +
                "- Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n" +
                "Załącz plik raportu z aplikacji."
        )
        val url = "https://github.com/screenreaders/blindroid/issues/new?title=$title&body=$body"
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    private fun writeReport(context: Context, thread: Thread, throwable: Throwable) {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "crash_$timestamp.txt")
        val report = buildReport(context, thread, throwable)
        file.writeText(report)
        trimOldReports(dir)
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        sb.appendLine("Blindroid crash report")
        sb.appendLine("Time: ${Date()}")
        sb.appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine()
        sb.appendLine(throwable.stackTraceToString())
        sb.appendLine()
        sb.appendLine("Prefs:")
        sb.appendLine("announce=${Prefs.isAnnounceEnabled(context)}")
        sb.appendLine("speaker=${Prefs.isAutoSpeakerEnabled(context)}")
        sb.appendLine("moduleShortcuts=${Prefs.isModuleShortcutsEnabled(context)}")
        return sb.toString()
    }

    private fun trimOldReports(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= MAX_REPORTS) return
        files.drop(MAX_REPORTS).forEach { it.delete() }
    }
}
