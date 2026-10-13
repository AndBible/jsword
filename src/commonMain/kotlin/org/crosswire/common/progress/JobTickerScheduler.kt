package org.crosswire.common.progress

import org.crosswire.common.util.Cancellable
import org.crosswire.common.util.currentTimeMillis

/**
 * Epoch-wall-clock, start-relative ticker policy used by the Native Job ticker.
 *
 * The callback is bound once, in [start], and every later firing goes through [onDue] and
 * [onCallbackReturned] with no arguments: the Native handler and the policy tests call the same
 * functions. The next tick is due at `max(thisStart + period, callbackReturn)`, so a callback
 * that outruns the period starts its successor at the return instant rather than one more period
 * after completion. A tick never overlaps another. Cancellation never interrupts the active
 * callback; it only suppresses the successor. An uncaught callback failure stops the ticker and
 * propagates to the caller, which makes it visible to the host.
 *
 * [now] is injectable so the policy is testable without a timer. The Native actual passes
 * [currentTimeMillis].
 */
internal class JobTickerScheduler(
    private val periodMs: Long,
    private val now: () -> Long = ::currentTimeMillis,
) {
    private var stopped = false
    private var active = false
    private var startedAt = 0L
    private var dueAt: Long? = null
    private var task: (() -> Unit)? = null

    /** When the next tick may start, or null while a callback is active or the ticker has stopped. */
    fun nextDueAt(): Long? = if (stopped || active) null else dueAt

    fun isStopped(): Boolean = stopped

    /**
     * Validates the period, binds [task], then starts. The first tick runs immediately unless
     * [immediate] is false, in which case it is armed at [now] so a cancel-before-first boundary
     * is observable. A non-positive period is rejected before any callback and before any arming.
     */
    fun start(immediate: Boolean = true, task: () -> Unit): Cancellable {
        require(periodMs > 0) { "Job ticker period must be positive, was $periodMs" }
        check(this.task == null) { "ticker already started" }
        this.task = task
        val ticker = Cancellable { stopped = true }
        if (immediate) fire() else dueAt = now()
        return ticker
    }

    /** Fires the bound callback if a tick is due. Does nothing while one is active, before the due instant, or after stop. */
    fun onDue() {
        if (stopped || active) return
        val due = dueAt ?: return
        if (now() < due) return
        fire()
    }

    /**
     * Records that the active callback returned normally. A cancellation or failure that landed
     * during the callback wins: no successor is armed. Must not be called after a failure, which
     * has already cleared the active callback.
     */
    fun onCallbackReturned() {
        check(active) { "no active callback" }
        active = false
        if (stopped) {
            dueAt = null
            return
        }
        val elapsed = (now() - startedAt).coerceAtLeast(0L)
        // Start-relative: a fast callback waits out the remainder of the period; a slow one is due now.
        dueAt = now() + if (elapsed >= periodMs) 0L else periodMs - elapsed
    }

    private fun fire() {
        active = true
        dueAt = null
        startedAt = now()
        try {
            task!!.invoke()
        } catch (failure: Throwable) {
            stopped = true
            active = false
            dueAt = null
            throw failure
        }
    }
}
