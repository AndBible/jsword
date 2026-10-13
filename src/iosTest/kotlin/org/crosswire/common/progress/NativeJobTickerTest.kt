@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package org.crosswire.common.progress

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.incrementAndFetch
import platform.posix.usleep
import kotlin.test.*

/**
 * Exercises the real Native ticker: dispatch scheduling, cancellation boundaries and failure
 * propagation. Linux hosts compile this source set but cannot run it; results come from the
 * host-owned Native runtime.
 */
class NativeJobTickerTest {
    private fun waitFor(timeoutMs: Int, condition: () -> Boolean): Boolean {
        val steps = timeoutMs / 10
        for (i in 0 until steps) {
            if (condition()) return true
            usleep(10_000u)
        }
        return condition()
    }

    @Test fun rejectsNonPositivePeriodBeforeScheduling() {
        assertFailsWith<IllegalArgumentException> { scheduleJobTicker(0L) { } }
        assertFailsWith<IllegalArgumentException> { scheduleJobTicker(-5L) { } }
    }

    @Test fun firstTickIsImmediateAndCancelStopsFurtherTicks() {
        val calls = AtomicInt(0)
        val ticker = scheduleJobTicker(200L) { calls.incrementAndFetch() }
        assertTrue(waitFor(2_000) { calls.load() >= 1 }, "first tick must be scheduled immediately")
        val seen = calls.load()
        ticker.cancel()
        usleep(500_000u)
        assertEquals(seen, calls.load(), "cancellation must stop later ticks")
    }

    @Test fun cancelDuringCallbackDoesNotInterruptIt() {
        val entered = AtomicInt(0)
        val finished = AtomicInt(0)
        val release = AtomicInt(0)
        val holder = arrayOfNulls<org.crosswire.common.util.Cancellable>(1)
        holder[0] = scheduleJobTicker(50L) {
            if (entered.incrementAndFetch() == 1) {
                holder[0]!!.cancel()
                for (i in 0 until 300) {
                    if (release.load() == 1) break
                    usleep(10_000u)
                }
                finished.incrementAndFetch()
            }
        }
        assertTrue(waitFor(3_000) { entered.load() >= 1 })
        usleep(200_000u)
        assertEquals(0, finished.load(), "the active callback must still be running after cancel")
        release.incrementAndFetch()
        assertTrue(waitFor(3_000) { finished.load() == 1 }, "cancel must not interrupt the callback")
        val seen = entered.load()
        usleep(300_000u)
        assertEquals(seen, entered.load(), "no tick may start after cancellation")
    }

    @Test fun slowCallbackDoesNotQueueAnOverlappingTick() {
        val active = AtomicInt(0)
        val overlaps = AtomicInt(0)
        val calls = AtomicInt(0)
        val ticker = scheduleJobTicker(30L) {
            if (active.incrementAndFetch() > 1) overlaps.incrementAndFetch()
            calls.incrementAndFetch()
            usleep(120_000u)
            active.fetchAndAdd(-1)
        }
        assertTrue(waitFor(3_000) { calls.load() >= 3 })
        ticker.cancel()
        assertEquals(0, overlaps.load(), "ticks must not overlap")
    }

    @Test fun uncaughtFailureReachesTheHostAndStopsTheTicker() {
        val calls = AtomicInt(0)
        val failure = AtomicReference<Throwable?>(null)
        val previous = setUnhandledExceptionHook { failure.store(it) }
        try {
            val ticker = scheduleJobTicker(40L) {
                calls.incrementAndFetch()
                throw IllegalStateException("ticker listener")
            }
            assertTrue(waitFor(3_000) { failure.load() != null }, "failure must be visible to the host hook")
            assertTrue(failure.load()!!.message!!.contains("ticker listener"))
            val seen = calls.load()
            usleep(300_000u)
            assertEquals(seen, calls.load(), "failure must terminate the ticker")
            ticker.cancel()
        } finally {
            setUnhandledExceptionHook(previous)
        }
    }

    @Test fun throwingHookDoesNotReplaceTheOriginalFailure() {
        val calls = AtomicInt(0)
        val reported = AtomicReference<Throwable?>(null)
        // The hook throws on the first delivery. The ticker must report the original callback
        // failure again, with the hook's exception suppressed, rather than lose it.
        val previous = setUnhandledExceptionHook { thrown ->
            val first = reported.load() == null
            reported.store(thrown)
            if (first) throw IllegalStateException("hook blew up")
        }
        try {
            val ticker = scheduleJobTicker(40L) {
                calls.incrementAndFetch()
                throw IllegalStateException("ticker listener")
            }
            assertTrue(waitFor(3_000) { reported.load()?.suppressedExceptions?.isNotEmpty() == true }, "the original failure must be re-reported")
            val surfaced = reported.load()!!
            assertTrue(surfaced.message!!.contains("ticker listener"), "the original failure must be what is reported")
            assertTrue(surfaced.suppressedExceptions.any { it.message?.contains("hook blew up") == true }, "the hook failure must be suppressed, not a replacement")
            val seen = calls.load()
            usleep(300_000u)
            assertEquals(seen, calls.load(), "the ticker must still terminate when the hook throws")
            ticker.cancel()
        } finally {
            setUnhandledExceptionHook(previous)
        }
    }

    @Test fun repeatedCancelReleasesTheTicker() {
        val ticker = scheduleJobTicker(1_000L) { }
        ticker.cancel()
        ticker.cancel()
    }
}
