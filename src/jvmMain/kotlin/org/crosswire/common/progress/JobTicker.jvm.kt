package org.crosswire.common.progress

import java.util.Timer
import java.util.TimerTask
import org.crosswire.common.util.Cancellable

/** Legacy Job timer: wall-clock start-relative cadence, non-daemon, uncaught failure terminates it. */
internal actual fun scheduleJobTicker(periodMs: Long, task: () -> Unit): Cancellable {
    val timer = Timer()
    timer.schedule(object : TimerTask() {
        override fun run() = task()
    }, 0L, periodMs)
    return Cancellable { timer.cancel() }
}
