package org.crosswire.common.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class SchedulingTest {
    @Test fun startsImmediatelyOnDaemonAndUsesDelayAfterCompletion() {
        val entered = CountDownLatch(1)
        val unblock = CountDownLatch(1)
        val second = CountDownLatch(1)
        val worker = AtomicReference<Thread>()
        val starts = java.util.concurrent.CopyOnWriteArrayList<Long>()
        val end = java.util.concurrent.atomic.AtomicLong()
        val job = scheduleRepeating(100) {
            starts.add(System.nanoTime())
            worker.set(Thread.currentThread())
            if (starts.size == 1) {
                entered.countDown()
                check(unblock.await(5, TimeUnit.SECONDS))
                end.set(System.nanoTime())
            } else second.countDown()
        }
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            assertTrue(worker.get().isDaemon)
            Thread.sleep(150)
            assertEquals(1, starts.size)
            unblock.countDown()
            assertTrue(second.await(2, TimeUnit.SECONDS))
            assertTrue(starts[1] - end.get() >= TimeUnit.MILLISECONDS.toNanos(80))
        } finally {
            unblock.countDown()
            job.cancel()
            worker.get()?.join(2000)
        }
        assertFalse(worker.get().isAlive)
    }
}
