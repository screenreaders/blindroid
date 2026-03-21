package com.screenreaders.blindroid.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.FileProvider
import com.screenreaders.blindroid.BuildConfig
import com.screenreaders.blindroid.data.Prefs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object CrashReporter {
    private const val DIR_NAME = "crash_reports"
    private const val MAX_REPORTS = 5
    private const val REPORT_ENDPOINT = "https://report.asteja.eu/"
    private const val REPORT_HEADER = "X-Report-Token"
    private const val REPORT_CLIENT_HEADER = "X-Client-Token"
    private const val REPORT_TOKEN = "47dc28661ef7d1ac07400827508b6aa9"
    private const val CONNECT_TIMEOUT_MS = 5000
    private const val READ_TIMEOUT_MS = 7000
    private var initialized = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    @Volatile private var uploadInProgress = false
    @Volatile private var appInForeground = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (Prefs.isCrashReportingEnabled(appContext)) {
                try {
                    writeReport(appContext, thread, throwable)
                    uploadPendingReports(appContext)
                } catch (_: Exception) {
                    // Ignore reporting failures
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun setAppInForeground(value: Boolean) {
        appInForeground = value
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
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.forLanguageTag("pl-PL"))
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

    fun reportAnr(context: Context, reason: String, threadDump: String) {
        if (!Prefs.isCrashReportingEnabled(context)) return
        try {
            val content = buildAnrReport(context, reason, threadDump)
            val file = writeCustomReport(context, "anr", content)
            if (file != null && canUploadNow(context)) {
                uploadReport(context, file)
            }
        } catch (_: Exception) {
            // Ignore reporting failures
        }
    }

    fun uploadPendingReports(context: Context) {
        if (!Prefs.isCrashReportingEnabled(context)) return
        if (uploadInProgress) return
        if (Prefs.isCrashForegroundOnly(context) && !appInForeground) return
        val dir = File(context.filesDir, DIR_NAME)
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        if (files.isEmpty()) return
        val appContext = context.applicationContext
        uploadInProgress = true
        Thread {
            try {
                if (!hasAllowedNetwork(appContext)) return@Thread
                if (!hasAllowedPower(appContext)) return@Thread
                for (file in files) {
                    val ok = uploadReport(appContext, file)
                    if (ok) {
                        file.delete()
                    } else {
                        break
                    }
                }
            } finally {
                uploadInProgress = false
            }
        }.start()
    }

    fun canUploadNow(context: Context): Boolean {
        if (!Prefs.isCrashReportingEnabled(context)) return false
        if (Prefs.isCrashForegroundOnly(context) && !appInForeground) return false
        if (!hasAllowedNetwork(context)) return false
        if (!hasAllowedPower(context)) return false
        return true
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

    private fun writeCustomReport(context: Context, prefix: String, content: String): File? {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "${prefix}_$timestamp.txt")
        file.writeText(content)
        trimOldReports(dir)
        return file
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val sb = StringBuilder()
        val reportId = UUID.randomUUID().toString()
        val clientId = ensureClientId(context)
        sb.appendLine("Blindroid crash report")
        sb.appendLine("Time: ${Date()}")
        sb.appendLine("ReportId: $reportId")
        sb.appendLine("ClientId: $clientId")
        sb.appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine("Thread: ${thread.name}")
        sb.appendLine()
        sb.appendLine(maskPhoneNumbers(throwable.stackTraceToString()))
        sb.appendLine()
        sb.appendLine("Prefs:")
        sb.appendLine("announce=${Prefs.isAnnounceEnabled(context)}")
        sb.appendLine("speaker=${Prefs.isAutoSpeakerEnabled(context)}")
        sb.appendLine("moduleShortcuts=${Prefs.isModuleShortcutsEnabled(context)}")
        if (Prefs.isCrashDeviceInfoEnabled(context)) {
            sb.appendLine()
            sb.appendLine("DeviceInfo:")
            sb.appendLine("brand=${Build.BRAND}")
            sb.appendLine("device=${Build.DEVICE}")
            sb.appendLine("product=${Build.PRODUCT}")
            sb.appendLine("hardware=${Build.HARDWARE}")
            sb.appendLine("cpu_abis=${Build.SUPPORTED_ABIS.joinToString()}")
            sb.appendLine("locale=${context.resources.configuration.locales.toLanguageTags()}")
            sb.appendLine("timezone=${java.util.TimeZone.getDefault().id}")
            sb.appendLine("security_patch=${Build.VERSION.SECURITY_PATCH ?: "unknown"}")
            val am = context.getSystemService(ActivityManager::class.java)
            if (am != null) {
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                sb.appendLine("ram_total=${bytesToHuman(info.totalMem)}")
                sb.appendLine("ram_avail=${bytesToHuman(info.availMem)}")
            }
            val storage = getStorageInfo()
            if (storage != null) {
                sb.appendLine("storage_avail=${storage.first}")
                sb.appendLine("storage_total=${storage.second}")
            }
            sb.appendLine("battery=${getBatteryPercent(context)}")
            sb.appendLine("charging=${isCharging(context)}")
        }
        val diagnostics = DiagnosticLog.dump(context)
        if (diagnostics.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Diagnostics:")
            diagnostics.forEach { sb.appendLine(it) }
        }
        return sb.toString()
    }

    private fun buildAnrReport(context: Context, reason: String, threadDump: String): String {
        val sb = StringBuilder()
        val reportId = UUID.randomUUID().toString()
        val clientId = ensureClientId(context)
        sb.appendLine("Blindroid ANR report")
        sb.appendLine("Time: ${Date()}")
        sb.appendLine("ReportId: $reportId")
        sb.appendLine("ClientId: $clientId")
        sb.appendLine("Reason: $reason")
        sb.appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        sb.appendLine("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        sb.appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        sb.appendLine()
        sb.appendLine(maskPhoneNumbers(threadDump))
        if (Prefs.isCrashDeviceInfoEnabled(context)) {
            sb.appendLine()
            sb.appendLine("DeviceInfo:")
            sb.appendLine("brand=${Build.BRAND}")
            sb.appendLine("device=${Build.DEVICE}")
            sb.appendLine("product=${Build.PRODUCT}")
            sb.appendLine("hardware=${Build.HARDWARE}")
            sb.appendLine("cpu_abis=${Build.SUPPORTED_ABIS.joinToString()}")
            sb.appendLine("locale=${context.resources.configuration.locales.toLanguageTags()}")
            sb.appendLine("timezone=${java.util.TimeZone.getDefault().id}")
            sb.appendLine("security_patch=${Build.VERSION.SECURITY_PATCH ?: "unknown"}")
        }
        return sb.toString()
    }

    private fun hasAllowedNetwork(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (!validated) return false
        return if (Prefs.isCrashWifiOnly(context)) {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            true
        }
    }

    private fun hasAllowedPower(context: Context): Boolean {
        if (!Prefs.isCrashChargingOnly(context)) return true
        return isCharging(context)
    }

    private fun uploadReport(context: Context, file: File): Boolean {
        return try {
            val conn = (URL(REPORT_ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
                setRequestProperty(REPORT_HEADER, REPORT_TOKEN)
                setRequestProperty(REPORT_CLIENT_HEADER, ensureClientId(context))
            }
            conn.outputStream.use { it.write(file.readBytes()) }
            val code = conn.responseCode
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun ensureClientId(context: Context): String {
        val existing = Prefs.getCrashClientId(context)
        if (!existing.isNullOrBlank()) return existing
        val id = UUID.randomUUID().toString()
        Prefs.setCrashClientId(context, id)
        return id
    }

    private fun getStorageInfo(): Pair<String, String>? {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val available = bytesToHuman(stat.availableBytes)
            val total = bytesToHuman(stat.totalBytes)
            available to total
        } catch (_: Exception) {
            null
        }
    }

    private fun bytesToHuman(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        var value = bytes.toDouble()
        var exp = 0
        while (value >= 1024.0 && exp < 6) {
            value /= 1024.0
            exp += 1
        }
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.US, "%.1f %sB", value, pre)
    }

    private fun getBatteryPercent(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    private fun isCharging(context: Context): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun maskPhoneNumbers(text: String): String {
        val regex = Regex("(?<!\\d)(\\+?\\d[\\d\\s\\-]{5,}\\d)")
        return regex.replace(text) { match ->
            val raw = match.value
            val digits = raw.filter { it.isDigit() }
            if (digits.length < 7) return@replace raw
            val maskedDigits = buildString {
                val keep = 2
                val maskLen = (digits.length - keep).coerceAtLeast(0)
                repeat(maskLen) { append('*') }
                append(digits.takeLast(keep))
            }
            var idx = 0
            val result = StringBuilder()
            for (ch in raw) {
                if (ch.isDigit()) {
                    result.append(maskedDigits[idx])
                    idx += 1
                } else {
                    result.append(ch)
                }
            }
            result.toString()
        }
    }

    private fun trimOldReports(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= MAX_REPORTS) return
        files.drop(MAX_REPORTS).forEach { it.delete() }
    }
}
