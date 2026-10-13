package org.crosswire.common.progress

import org.junit.Test
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

class JobTickerTest {
    private fun verifyLifecycle(schedule: ((() -> Unit) -> Unit), cleanup: () -> Unit) {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val uncaught = CountDownLatch(1)
        val thread = AtomicReference<Thread>()
        val failure = AtomicReference<Throwable>()
        val error = IllegalStateException("ticker listener")
        try {
            schedule {
                val current = Thread.currentThread()
                thread.set(current)
                current.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, ex -> failure.set(ex); uncaught.countDown() }
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                throw error
            }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            assertFalse(thread.get().isDaemon, "legacy Timer is non-daemon")
            release.countDown()
            assertTrue(uncaught.await(3, TimeUnit.SECONDS), "callback failures must reach the thread handler")
            thread.get().join(3000)
            assertFalse(thread.get().isAlive, "failed ticker thread must terminate")
            assertSame(error, failure.get())
        } finally { release.countDown(); cleanup(); thread.get()?.join(3000) }
    }

    private fun verifySlowCadence(schedule: ((() -> Unit) -> org.crosswire.common.util.Cancellable)) {
        val first = CountDownLatch(1)
        val release = CountDownLatch(1)
        val second = CountDownLatch(1)
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val ticker = schedule {
            if (calls.incrementAndGet() == 1) { first.countDown(); check(release.await(8, TimeUnit.SECONDS)) }
            else second.countDown()
        }
        try {
            assertTrue(first.await(3, TimeUnit.SECONDS))
            // Deliberately overrun the 3-second period; allow 1.5 seconds of scheduling slack,
            // but not an extra full period measured from callback completion.
            assertFalse(second.await(3500, TimeUnit.MILLISECONDS))
            release.countDown()
            assertTrue(second.await(1500, TimeUnit.MILLISECONDS), "slow callback must not add a completion-relative period")
        } finally { release.countDown(); ticker.cancel() }
    }

    @Test fun oldTimerSlowCallbackStartsNextTickWithoutAnotherPeriod() = verifySlowCadence { task ->
        val timer = Timer()
        timer.schedule(object : TimerTask() { override fun run() = task() }, 0, 3000)
        org.crosswire.common.util.Cancellable { timer.cancel() }
    }

    @Test fun jobTickerSlowCallbackStartsNextTickWithoutAnotherPeriod() = verifySlowCadence { task ->
        scheduleJobTicker(3000, task)
    }

    @Test fun oldTimerFailureLifecycleReference() {
        val timer = Timer()
        verifyLifecycle({ task -> timer.schedule(object : TimerTask() { override fun run() = task() }, 0, 100) }, { timer.cancel() })
    }

    @Test fun actualJobTickerKeepsOldThreadFailureLifecycle() {
        val job = JobManager.createJob("ticker-lifecycle") as Job
        val predictions = java.nio.file.Files.createTempFile("ticker", ".properties")
        val callbacks = AtomicReference<(() -> Unit)?>()
        val listener = object : WorkListener {
            override fun workStateChanged(ev: WorkEvent) = Unit
            override fun workProgressed(ev: WorkEvent) {
                if (ev.job === job && Thread.currentThread() !== caller) {
                    callbacks.get()?.invoke()
                }
            }
            val caller = Thread.currentThread()
        }
        try {
            JobManager.addWorkListener(listener)
            verifyLifecycle({ task -> callbacks.set(task); job.beginJob("tick", predictions.toUri()) }, { callbacks.set(null); job.done() })
        } finally { callbacks.set(null); JobManager.removeWorkListener(listener); job.done(); java.nio.file.Files.deleteIfExists(predictions) }
    }
}
