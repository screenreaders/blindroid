package com.screenreaders.blindroid.diagnostics

import android.content.Context
import android.os.Handler
import android.os.Looper

class AnrWatchdog(
    private val context: Context,
    private val timeoutMs: Long = 8000L
) : Thread("Blindroid-AnrWatchdog") {
    @Volatile private var tick = 0
    @Volatile private var lastTick = 0
    @Volatile private var reported = false
    private val handler = Handler(Looper.getMainLooper())

    override fun run() {
        while (!isInterrupted) {
            tick += 1
            handler.post { lastTick = tick }
            try {
                sleep(timeoutMs)
            } catch (_: InterruptedException) {
                return
            }
            if (lastTick != tick && !reported) {
                reported = true
                val dump = buildThreadDump()
                CrashReporter.reportAnr(context, "main_thread_blocked", dump)
            }
        }
    }

    private fun buildThreadDump(): String {
        val sb = StringBuilder()
        val traces = Thread.getAllStackTraces()
        for ((thread, stack) in traces) {
            sb.appendLine("Thread: ${thread.name} (${thread.state})")
            for (element in stack) {
                sb.appendLine("  at $element")
            }
            sb.appendLine()
        }
        return sb.toString()
    }
}
