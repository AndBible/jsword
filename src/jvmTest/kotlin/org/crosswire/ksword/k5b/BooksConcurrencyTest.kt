package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.Books
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Pins the mutual exclusion of `Books.addBook`/`removeBook` against `getBooks`/`getBook`. The registry keeps a sorted
 * set and two hash maps that are updated together; without one lock, readers see a torn state (a `getBook` hit for a
 * book that `getBooks` does not list, a `ConcurrentModificationException` while copying, a corrupted `HashMap`).
 */
class BooksConcurrencyTest {
    @Test fun addRemoveAndReadersAreMutuallyExclusive() {
        val registry = Books.installed()
        val fixture = K5bFixtureBook.create()
        val initials = fixture.initials
        assertNull("fixture must not be installed already", registry.getBook(initials))

        val failures = ConcurrentLinkedQueue<Throwable>()
        val writersDone = AtomicBoolean(false)
        val start = CountDownLatch(1)
        val writers = (1..8).map {
            Thread {
                start.await()
                try {
                    repeat(200) {
                        // A racing pair may remove an already removed book or add a present one; only those are expected.
                        try { registry.addBook(fixture) } catch (e: Throwable) { failures += e }
                        try { registry.removeBook(fixture) } catch (e: org.crosswire.jsword.book.BookException) { /* another writer won */ } catch (e: Throwable) { failures += e }
                    }
                } catch (e: Throwable) { failures += e }
            }
        }
        val readers = (1..4).map {
            Thread {
                start.await()
                try {
                    while (!writersDone.get()) {
                        val all = registry.getBooks()
                        // getBooks and getBook are two calls, so only the single-call invariants are asserted here.
                        assertTrue(all.size == all.toSet().size)
                        val found = registry.getBook(initials)
                        if (found != null) assertEquals(initials, found.initials)
                        registry.getBooks(null).size
                    }
                } catch (e: Throwable) { failures += e }
            }
        }
        val all = writers + readers
        all.forEach { it.isDaemon = true; it.start() }
        var presentInList = false
        var presentByInitials = false
        try {
            start.countDown()
            writers.forEach { it.join(120_000) }
            writersDone.set(true)
            readers.forEach { it.join(30_000) }
            // Quiescent: the sorted set and the initials/names maps must agree.
            presentInList = registry.getBooks().any { it.initials == initials }
            presentByInitials = registry.getBook(initials) != null
        } finally {
            writersDone.set(true)
            if (registry.getBook(initials) != null) registry.removeBook(fixture)
        }
        assertTrue("threads must finish", all.none { it.isAlive })
        assertTrue("unexpected failures: $failures", failures.isEmpty())
        assertEquals("sorted set and maps disagree", presentInList, presentByInitials)
        assertNull(registry.getBook(initials))
        assertTrue(registry.getBooks().none { it.initials == initials })
    }
}
