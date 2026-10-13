@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.darwin.*

actual fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable {
    require(periodMs > 0)
    val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
    val timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0u, 0u, queue)!!
    val lock = SynchronizedObject()
    var cancelled = false
    fun arm(delayNs: Long) {
        dispatch_source_set_timer(timer, dispatch_time(DISPATCH_TIME_NOW, delayNs), DISPATCH_TIME_FOREVER, 0u)
    }
    dispatch_source_set_event_handler(timer) {
        if (synchronized(lock) { !cancelled }) {
            try {
                task()
                synchronized(lock) {
                    if (!cancelled) arm(periodMs * 1_000_000L)
                }
            } catch (_: Throwable) {
                // ScheduledExecutor suppresses subsequent runs after an uncaught task failure.
                synchronized(lock) {
                    cancelled = true
                    dispatch_source_cancel(timer)
                }
            }
        }
    }
    arm(0)
    dispatch_resume(timer)
    return Cancellable {
        synchronized(lock) {
            if (!cancelled) {
                cancelled = true
                dispatch_source_cancel(timer)
            }
        }
    }
}
