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

    @Test fun earlyOneShotAfterBackwardClockStepIsRearmed() {
        val clock = Clock()
        var calls = 0
        val scheduler = scheduler(clock)
        scheduler.start(immediate = false) { calls++ }
        clock.now = 900L
        assertEquals(100L, scheduler.onTimerEvent(), "a consumed early one-shot must be rearmed")
        assertEquals(0, calls, "wall-clock due time must still be respected")
        clock.now = 1_000L
        assertEquals(100L, scheduler.onTimerEvent())
        assertEquals(1, calls)
    }

    @Test fun timerEventReturnsDelayWithoutResamplingAfterCallbackReturn() {
        var samples = 0
        val scheduler = JobTickerScheduler(periodMs = 100L) {
            when (++samples) {
                1, 2 -> 1_000L // Deferred start and callback admission.
                3 -> 1_040L // Callback return.
                else -> 9_000L // Must not be sampled to translate due-at back to delay.
            }
        }
        scheduler.start(immediate = false) { }
        assertEquals(60L, scheduler.onTimerEvent())
        assertEquals(1_100L, scheduler.nextDueAt())
        assertEquals(3, samples)
    }

    @Test fun callbackReturnUsesOneClockSampleAcrossAClockStep() {
        var samples = 0
        val scheduler = JobTickerScheduler(periodMs = 100L) {
            when (++samples) {
                1 -> 1_000L // Callback start.
                2 -> 1_040L // Callback return.
                else -> 9_000L // A wall-clock step must not change the already computed gap.
            }
        }
        scheduler.start { }
        scheduler.onCallbackReturned()
        assertEquals(1_100L, scheduler.nextDueAt())
        assertEquals(2, samples, "callback return must use a single clock sample")
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
        ticker = scheduler.start(immediate = false) {
            calls++
            ticker.cancel()
            finished = true
        }
        scheduler.onDue()
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
