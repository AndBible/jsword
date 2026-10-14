package org.crosswire.common.progress

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.*

/** Exercises the production Native ticker policy across JVM threads, not the JVM Timer actual. */
class JobTickerSchedulerRaceTest {
    @Test fun concurrentDueChecksClaimOnlyOneCallback() {
        val firstCheck = CountDownLatch(1)
        val releaseCheck = CountDownLatch(1)
        val secondDone = CountDownLatch(1)
        val armed = AtomicBoolean(false)
        val checks = AtomicInteger()
        val calls = AtomicInteger()
        val failure = AtomicReference<Throwable?>()
        val scheduler = JobTickerScheduler(100L) {
            if (armed.get() && checks.incrementAndGet() == 1) {
                firstCheck.countDown()
                check(releaseCheck.await(5, TimeUnit.SECONDS))
            }
            1_000L
        }
        val ticker = scheduler.start(immediate = false) { calls.incrementAndGet() }
        armed.set(true)
        fun fire() {
            try { scheduler.onDue() } catch (error: Throwable) { failure.compareAndSet(null, error) }
        }
        val first = thread { fire() }
        var second: Thread? = null
        try {
            assertTrue(firstCheck.await(5, TimeUnit.SECONDS))
            second = thread {
                try { fire() } finally { secondDone.countDown() }
            }
            // Without atomic admission the second thread completes while the first check is
            // paused, then the first enters the same active callback. A synchronized admission
            // waits here; release it without requiring the second thread to acquire the lock.
            secondDone.await(250, TimeUnit.MILLISECONDS)
        } finally {
            releaseCheck.countDown()
            first.join(5_000)
            second?.join(5_000)
            ticker.cancel()
        }
        assertFalse(first.isAlive)
        assertFalse(second.isAlive)
        assertNull(failure.get())
        assertEquals(1, calls.get(), "concurrent due checks must not overlap an active tick")
    }

    @Test fun crossThreadCancelReturnsBeforeCallbackAndSuppressesSuccessor() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val calls = AtomicInteger()
        val finished = AtomicBoolean(false)
        val failure = AtomicReference<Throwable?>()
        val scheduler = JobTickerScheduler(100L) { 1_000L }
        val ticker = scheduler.start(immediate = false) {
            calls.incrementAndGet()
            entered.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            finished.set(true)
        }
        val firing = thread {
            try {
                scheduler.onDue()
                scheduler.onCallbackReturned()
            } catch (error: Throwable) { failure.compareAndSet(null, error) }
        }
        var cancelling: Thread? = null
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            cancelling = thread {
                try { ticker.cancel() } finally { cancelled.countDown() }
            }
            assertTrue(cancelled.await(2, TimeUnit.SECONDS), "cancel must not wait for callback return")
            assertFalse(finished.get(), "cancel must not interrupt the active callback")
            assertTrue(scheduler.isStopped())
        } finally {
            release.countDown()
            firing.join(5_000)
            cancelling?.join(5_000)
            ticker.cancel()
        }
        assertFalse(firing.isAlive)
        assertFalse(cancelling.isAlive)
        assertNull(failure.get())
        assertTrue(finished.get())
        scheduler.onDue()
        assertEquals(1, calls.get())
        assertNull(scheduler.nextDueAt())
    }
}
