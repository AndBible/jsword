package org.crosswire.common.progress

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.crosswire.common.util.Cancellable
import org.crosswire.common.util.currentTimeMillis

/**
 * Epoch-wall-clock, start-relative policy used by the Native Job ticker.
 *
 * State transitions and tick admission are synchronized, but user callbacks run outside the lock:
 * cancellation suppresses the successor without interrupting or waiting for the active callback.
 * The next tick is due after max(period, callback duration) measured from callback start.
 * Uncaught callback failures stop the ticker and propagate to the host-facing handler.
 * [now] is injectable for deterministic policy tests; Native uses [currentTimeMillis].
 */
internal class JobTickerScheduler(
    private val periodMs: Long,
    private val now: () -> Long = ::currentTimeMillis,
) {
    private val lock = SynchronizedObject()
    private var stopped = false
    private var active = false
    private var startedAt = 0L
    private var dueAt: Long? = null
    private var task: (() -> Unit)? = null

    /** Next wall-clock due instant, or null while active or stopped. */
    fun nextDueAt(): Long? = synchronized(lock) { if (stopped || active) null else dueAt }

    fun isStopped(): Boolean = synchronized(lock) { stopped }

    /** Binds the callback, validating before execution; deferred start permits cancel-before-first. */
    fun start(immediate: Boolean = true, task: () -> Unit): Cancellable {
        require(periodMs > 0) { "Job ticker period must be positive, was $periodMs" }
        val callback = synchronized(lock) {
            check(this.task == null) { "ticker already started" }
            this.task = task
            val sampledNow = now()
            dueAt = sampledNow
            if (immediate) claim(sampledNow) else null
        }
        val ticker = Cancellable { synchronized(lock) { stopped = true; dueAt = null } }
        callback?.let { invokeCallback(it) }
        return ticker
    }

    /**
     * Handles a consumed one-shot event, returning its relative rearm delay or null after stop.
     * A backwards wall-clock step can make the event early: rearm for the remaining wall-clock
     * gap rather than abandon the consumed timer. Admission and each delay use one clock sample.
     */
    fun onTimerEvent(): Long? {
        val callback = synchronized(lock) {
            if (stopped || active) return null
            val due = dueAt ?: return null
            val sampledNow = now()
            if (sampledNow < due) return due - sampledNow
            claim(sampledNow)
        }
        invokeCallback(callback)
        return onCallbackReturned()
    }

    /** Atomically admits a due tick; concurrent or premature firings do not re-enter it. */
    fun onDue() {
        val callback = synchronized(lock) { claimDue() } ?: return
        invokeCallback(callback)
    }

    /** Records normal return and yields the relative delay computed from one return sample. */
    fun onCallbackReturned(): Long? = synchronized(lock) {
        check(active) { "no active callback" }
        active = false
        if (stopped) {
            dueAt = null
            return null
        }
        val returnedAt = now()
        val elapsed = (returnedAt - startedAt).coerceAtLeast(0L)
        val delay = if (elapsed >= periodMs) 0L else periodMs - elapsed
        dueAt = returnedAt + delay
        delay
    }

    // Both admission helpers are called only while holding lock.
    private fun claimDue(): (() -> Unit)? {
        if (stopped || active) return null
        val due = dueAt ?: return null
        val sampledNow = now()
        return if (sampledNow < due) null else claim(sampledNow)
    }

    private fun claim(sampledNow: Long): () -> Unit {
        active = true
        dueAt = null
        startedAt = sampledNow
        return task!!
    }

    private fun invokeCallback(callback: () -> Unit) {
        try {
            callback()
        } catch (failure: Throwable) {
            synchronized(lock) {
                stopped = true
                active = false
                dueAt = null
            }
            throw failure
        }
    }
}
