package org.crosswire.common.util

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Hammers [CopyOnWriteList] from several threads; the results must be deterministic and nothing may throw. */
class CopyOnWriteListConcurrencyTest {
    private fun runAll(threads: Int, body: (Int) -> Unit) {
        val failure = AtomicReference<Throwable?>(null)
        val start = CountDownLatch(1)
        val workers = (0 until threads).map { t ->
            thread {
                start.await()
                try { body(t) } catch (e: Throwable) { failure.compareAndSet(null, e) }
            }
        }
        start.countDown()
        workers.forEach { it.join(60_000) }
        assertNull(failure.get(), "worker failed: ${failure.get()}")
    }

    @Test
    fun concurrentAddsLoseNothing() {
        val list = CopyOnWriteList<Int>()
        val threads = 8
        val perThread = 500
        runAll(threads) { t -> repeat(perThread) { list.add(t * perThread + it) } }
        assertEquals(threads * perThread, list.size)
        assertEquals((0 until threads * perThread).toSet(), list.toSet())
    }

    @Test
    fun concurrentAddsAndRemoveAllLeaveExactlyTheKeptElements() {
        val list = CopyOnWriteList<Int>()
        val threads = 8
        val perThread = 400
        runAll(threads) { t ->
            repeat(perThread) {
                val v = t * perThread + it
                list.add(v)
                // odd values are removed again, in batches through removeAll and singly through remove
                if (v % 2 == 1) {
                    if (it % 2 == 0) list.removeAll(listOf(v)) else list.remove(v)
                }
            }
        }
        val expected = (0 until threads * perThread).filter { it % 2 == 0 }.toSet()
        assertEquals(expected.size, list.size)
        assertEquals(expected, list.toSet())
    }

    @Test
    fun iterationWhileAnotherThreadMutatesNeverThrows() {
        val list = CopyOnWriteList((0 until 100).toList())
        val stop = AtomicBoolean(false)
        val mutator = thread {
            var i = 100
            while (!stop.get()) {
                list.add(i++)
                if (list.size > 200) list.removeAll { it % 3 == 0 }
                if (list.size > 300) list.clear()
            }
        }
        try {
            runAll(4) {
                repeat(2000) {
                    var previous = -1
                    var count = 0
                    for (v in list) { count++; previous = v }
                    check(count >= 0 && previous >= -1)
                    list.toList()
                    list.subList(0, 0)
                    list.listIterator().hasNext()
                }
            }
        } finally {
            stop.set(true)
            mutator.join(TimeUnit.SECONDS.toMillis(30))
        }
    }
}
