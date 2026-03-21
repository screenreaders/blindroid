package com.screenreaders.blindroid.launcher

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object LauncherExecutors {
    private val threadId = AtomicInteger(1)
    val io: ExecutorService = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "launcher-io-${threadId.getAndIncrement()}")
    }
}
