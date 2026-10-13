package org.crosswire.common.progress

import kotlin.test.*

/**
 * Deterministic policy of the production ticker scheduler. Drives [JobTickerScheduler] with an
 * injected clock, so cadence, overlap and termination are proven without a fake timer that only
 * echoes the requested period.
 */
class JobTickerPolicyTest {
    private class Clock(start: Long = 1_000L) {
        var now: Long = start
    }

    private fun scheduler(clock: Clock) = JobTickerScheduler(periodMs = 100L) { clock.now }

    @Test fun rejectsNonPositivePeriodBeforeAnyCallback() {
        val clock = Clock()
        var calls = 0
        assertFailsWith<IllegalArgumentException> { JobTickerScheduler(0L) { clock.now }.start { calls++ } }
        assertFailsWith<IllegalArgumentException> { JobTickerScheduler(-1L) { clock.now }.start { calls++ } }
        assertEquals(0, calls)
    }

    @Test fun firstTickIsImmediateAndCadenceIsStartRelative() {
        val clock = Clock()
        val starts = mutableListOf<Long>()
        val scheduler = scheduler(clock)
        scheduler.start { starts.add(clock.now) }
        assertEquals(listOf(1_000L), starts)
        // A 40 ms callback overruns nothing: next start is exactly one period after this start.
        clock.now = 1_040L
        scheduler.onCallbackReturned()
        assertEquals(listOf(1_000L), starts)
        assertEquals(1_100L, scheduler.nextDueAt())
        clock.now = 1_100L
        scheduler.onDue()
        assertEquals(listOf(1_000L, 1_100L), starts)
    }

    @Test fun slowCallbackStartsNextTickAfterDurationNotAfterAnotherPeriod() {
        val clock = Clock()
        val starts = mutableListOf<Long>()
        val scheduler = scheduler(clock)
        scheduler.start { starts.add(clock.now) }
        // 3.5 periods of work: the next start is the return instant, not return + period.
        clock.now = 1_350L
        scheduler.onCallbackReturned()
        assertEquals(1_350L, scheduler.nextDueAt())
        clock.now = 1_350L
        scheduler.onDue()
        assertEquals(listOf(1_000L, 1_350L), starts)
    }

    @Test fun noOverlapWhileCallbackIsActive() {
        val clock = Clock()
        var calls = 0
        val scheduler = scheduler(clock)
        scheduler.start { calls++ }
        clock.now = 5_000L
        scheduler.onDue()
        assertEquals(1, calls, "a due firing must not re-enter an active callback")
        assertNull(scheduler.nextDueAt())
    }

    @Test fun cancelBeforeFirstCallbackRunsNothing() {
        val clock = Clock()
        var calls = 0
        val scheduler = scheduler(clock)
        val ticker = scheduler.start(immediate = false) { calls++ }
        ticker.cancel()
        scheduler.onDue()
        assertEquals(0, calls)
        assertTrue(scheduler.isStopped())
    }

    @Test fun cancelDuringCallbackDoesNotInterruptItAndSuppressesTheNextTick() {
        val clock = Clock()
        var calls = 0
        var finished = false
        val scheduler = scheduler(clock)
        lateinit var ticker: org.crosswire.common.util.Cancellable
        ticker = scheduler.start {
            calls++
            ticker.cancel()
            finished = true
        }
        assertEquals(1, calls)
        assertTrue(finished, "cancellation must not interrupt the active callback")
        scheduler.onCallbackReturned()
        clock.now = 9_000L
        scheduler.onDue()
        assertEquals(1, calls)
        assertTrue(scheduler.isStopped())
    }

    @Test fun uncaughtFailureTerminatesTickerAndStaysVisible() {
        val clock = Clock()
        val error = IllegalStateException("ticker listener")
        val scheduler = scheduler(clock)
        val thrown = assertFailsWith<IllegalStateException> { scheduler.start { throw error } }
        assertSame(error, thrown)
        assertTrue(scheduler.isStopped())
        clock.now = 9_000L
        scheduler.onDue()
        assertTrue(scheduler.isStopped())
        assertNull(scheduler.nextDueAt())
    }

    @Test fun failureOnALaterTickAlsoTerminates() {
        val clock = Clock()
        var calls = 0
        val scheduler = scheduler(clock)
        scheduler.start {
            calls++
            if (calls == 2) throw IllegalStateException("later")
        }
        clock.now = 1_100L
        scheduler.onCallbackReturned()
        val thrown = assertFailsWith<IllegalStateException> { scheduler.onDue() }
        assertEquals("later", thrown.message)
        assertEquals(2, calls)
        assertTrue(scheduler.isStopped())
        assertNull(scheduler.nextDueAt())
    }
}
