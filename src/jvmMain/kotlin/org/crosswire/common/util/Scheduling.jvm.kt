package org.crosswire.common.util

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


/** One daemon thread per schedule, fixed delay, first run immediately. */
actual fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable {
    val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable).apply { isDaemon = true }
    }
    val future = executor.scheduleWithFixedDelay({ task() }, 0, periodMs, TimeUnit.MILLISECONDS)
    return Cancellable { future.cancel(false); executor.shutdown() }
}
