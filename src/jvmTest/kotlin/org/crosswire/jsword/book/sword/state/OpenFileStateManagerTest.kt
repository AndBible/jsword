package org.crosswire.jsword.book.sword.state

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.crosswire.jsword.book.sword.SwordBookMetaDataJvm
import org.junit.Assert.*
import org.junit.Test

class OpenFileStateManagerTest {
    private fun metadata(name: String = "K4RawText"): org.crosswire.jsword.book.sword.SwordBookMetaData {
        val root = File(System.getProperty("ksword.fixturesDir"), "space dir/ä")
        return SwordBookMetaDataJvm.create(File(root, "mods.d/$name.conf"), root.toURI())
    }

    @Test fun returnedStateIsReused() {
        val manager = OpenFileStateManager.createForTest(3600, 3600)
        val md = metadata()
        try {
            val first = manager.getRawBackendState(md)
            manager.release(first)
            val next = manager.getRawBackendState(md)
            try { assertSame(first, next) } finally { manager.release(next) }
        } finally { manager.shutDown() }
    }

    @Test fun idleStateIsClosedAndNotReused() {
        val manager = OpenFileStateManager.createForTest(1, 1)
        val md = metadata()
        try {
            val first = manager.getRawBackendState(md)
            manager.release(first)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!first.isClosedForTest() && System.nanoTime() < deadline) Thread.sleep(10)
            assertTrue("real cleanup must close the idle state", first.isClosedForTest())
            val next = manager.getRawBackendState(md)
            try { assertNotSame(first, next) } finally { manager.release(next) }
        } finally { manager.shutDown() }
    }

    @Test fun borrowersOverlapRealCleanupWithoutSharingOrClosingBorrowedStates() {
        val manager = OpenFileStateManager.createForTest(1, 1)
        val md = metadata()
        val held = ConcurrentHashMap.newKeySet<RawBackendState>()
        val failures = ConcurrentLinkedQueue<Throwable>()
        val ready = CountDownLatch(8)
        val stop = AtomicBoolean(false)
        val workers = (1..8).map {
            Thread {
                try {
                    val state = manager.getRawBackendState(md)
                    try {
                        check(held.add(state)) { "state shared by simultaneous borrowers" }
                        ready.countDown()
                        while (!stop.get()) {
                            check(!state.isClosedForTest()) { "borrowed state was closed" }
                            val transient = manager.getRawBackendState(md)
                            try {
                                check(held.add(transient)) { "transient state shared" }
                                check(!transient.isClosedForTest()) { "transient state closed" }
                                Thread.sleep(5)
                            } finally {
                                held.remove(transient)
                                manager.release(transient)
                            }
                        }
                    } finally {
                        held.remove(state)
                        manager.release(state)
                    }
                } catch (t: Throwable) { failures.add(t); ready.countDown() }
            }
        }
        var idle: RawBackendState? = null
        try {
            workers.forEach { it.start() }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            assertTrue("worker startup failed: $failures", failures.isEmpty())
            val idleMd = metadata("K4RawText4")
            idle = manager.getRawBackendState(idleMd)
            manager.release(idle)
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!idle.isClosedForTest() && System.nanoTime() < deadline) Thread.sleep(10)
            assertTrue("cleanup must run while all eight borrowers remain active", idle.isClosedForTest())
            assertTrue(held.size >= 8)
            held.forEach { assertFalse(it.isClosedForTest()) }
        } finally {
            stop.set(true)
            try {
                workers.forEach { it.join(5000) }
            } finally {
                manager.shutDown()
            }
            workers.forEach { assertFalse("worker did not finish", it.isAlive) }
            if (failures.isNotEmpty()) throw AssertionError("worker failed", failures.first()).apply {
                failures.drop(1).forEach { addSuppressed(it) }
            }
        }
        assertTrue(idle.isClosedForTest())
    }
}
