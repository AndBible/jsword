@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package org.crosswire.common.progress

import kotlin.native.getUnhandledExceptionHook
import kotlin.native.terminateWithUnhandledException
import org.crosswire.common.util.Cancellable
import org.crosswire.common.util.currentTimeMillis
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.DISPATCH_SOURCE_TYPE_TIMER
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_resume
import platform.darwin.dispatch_source_cancel
import platform.darwin.dispatch_source_create
import platform.darwin.dispatch_source_set_event_handler
import platform.darwin.dispatch_source_set_timer
import platform.darwin.dispatch_time

/**
 * Native Job ticker.
 *
 * Lifetime: one dispatch source per ticker, retained by GCD until [dispatch_source_cancel]. The
 * returned [Cancellable] is the host's only handle; Job releases it in `done()`. Kotlin/Native
 * has no JVM daemon flag to mirror, and none is claimed: the source does not keep the process
 * alive, and both cancellation and callback failure cancel it, so neither path leaks the worker.
 * `dispatch_source_cancel` is idempotent, so a repeated cancel is safe.
 *
 * Cadence comes from [JobTickerScheduler]: the first tick is armed at delay 0, and each
 * successor is armed at max(thisStart + period, callbackReturn) so a slow callback adds no extra
 * period. Ticks never overlap because the source is one-shot and is re-armed only after the
 * callback returns. Cancellation does not interrupt the active callback; it marks the scheduler
 * stopped, and the handler releases the source when that callback returns. An uncaught callback
 * failure terminates the ticker and is reported through the host's unhandled-exception hook,
 * falling back to [terminateWithUnhandledException]. It is never swallowed.
 *
 * This is deliberately not [org.crosswire.common.util.scheduleRepeating]: that pool timer rearms
 * a fixed delay after completion and swallows callback failures.
 */
internal actual fun scheduleJobTicker(periodMs: Long, task: () -> Unit): Cancellable {
    // Rejects a non-positive period before any source exists, matching JVM Timer's ordering.
    val scheduler = JobTickerScheduler(periodMs) { currentTimeMillis() }
    // Deferred start validates and binds without invoking user code or acquiring a source.
    val ticker = scheduler.start(immediate = false, task)
    val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
    val timer = dispatch_source_create(DISPATCH_SOURCE_TYPE_TIMER, 0u, 0u, queue)!!

    fun arm(delayMs: Long) {
        dispatch_source_set_timer(
            timer,
            dispatch_time(DISPATCH_TIME_NOW, delayMs * 1_000_000L),
            DISPATCH_TIME_FOREVER,
            0u,
        )
    }
    dispatch_source_set_event_handler(timer) {
        if (scheduler.isStopped()) {
            dispatch_source_cancel(timer)
            return@dispatch_source_set_event_handler
        }
        val delay = try {
            scheduler.onTimerEvent()
        } catch (failure: Throwable) {
            // Report before releasing the source, and contain a hook that itself throws, so the
            // original callback failure is what the host sees rather than the hook's exception.
            reportTickerFailure(failure)
            dispatch_source_cancel(timer)
            return@dispatch_source_set_event_handler
        }
        if (delay != null) {
            arm(delay)
        } else if (scheduler.isStopped()) {
            dispatch_source_cancel(timer)
        }
    }
    // Arms the first tick at now without running it, so a cancel issued before the handler runs
    // wins and the callback never starts.
    arm(0L)
    dispatch_resume(timer)
    return Cancellable {
        ticker.cancel()
        dispatch_source_cancel(timer)
    }
}

/**
 * Makes a callback failure visible to the host. The hook is read and invoked before the caller
 * releases the source. A hook that itself throws must not replace the original failure, so its
 * exception is reported alongside it and the original is still delivered to the fallback.
 *
 * [terminateWithUnhandledException] ends the process, so it stays the last resort and is not
 * directly tested: the testable paths are a hook that records the failure and a hook that throws.
 */
private fun reportTickerFailure(failure: Throwable) {
    val hook = getUnhandledExceptionHook()
    if (hook != null) {
        try {
            hook(failure)
        } catch (hookFailure: Throwable) {
            // The hook threw, so the host did not keep the failure. Re-report the original with
            // the hook's exception suppressed, through the current hook if one is still installed
            // and through the fallback otherwise.
            failure.addSuppressed(hookFailure)
            val current = getUnhandledExceptionHook()
            if (current != null) {
                try {
                    current(failure)
                } catch (_: Throwable) {
                    terminateWithUnhandledException(failure)
                }
            } else {
                terminateWithUnhandledException(failure)
            }
        }
    } else {
        terminateWithUnhandledException(failure)
    }
}
