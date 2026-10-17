/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.jsword.book.sword

import org.crosswire.common.activate.Activator
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Reproduces the NPE in [SwordGenBook.getKey] that occurs when a
 * book is repeatedly deactivated/reactivated through the [Activator]
 * (as AndBible's AI Documents books are, via sync-triggered refreshes) while
 * concurrently being read from another thread.
 *
 * <p>The root cause is that [Activator] performs a non-atomic
 * check-and-act on a non-thread-safe {@code HashSet}, so the Activator's notion
 * of "activated" can desync from the book's own {@code map}/{@code active}
 * state. A reader passing {@code checkActive()} can then observe a null map and
 * crash with an NPE on {@code map.get()}.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class SwordGenBookConcurrencyTest {

    /**
     * A backend whose readIndex() returns a small, non-empty key list so that
     * activate() builds a non-null, populated map.
     */
    private class FakeGenBackend : NullBackend() {
        override fun readIndex(): Key {
            val list = DefaultKeyList()
            list.addAll(DefaultLeafKeyList("entry1", "entry1"))
            list.addAll(DefaultLeafKeyList("entry2", "entry2"))
            return list
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetKeyDoesNotNpeUnderConcurrentActivation() {
        val book = createBook()

        // Prime it: activated set now contains the book, map is populated.
        Activator.activate(book)

        val readers = 4
        val iterations = 20000
        val failure = AtomicReference<Throwable>()
        val start = CountDownLatch(1)
        val done = CountDownLatch(readers + 1)

        // Refresher thread: mimics MyDocumentBookManager.refreshDocument(), which
        // deactivates then reactivates the book through the Activator.
        val refresher = Thread({
            try {
                start.await()
                var i = 0
                while (i < iterations && failure.get() == null) {
                    Activator.deactivate(book)
                    Activator.activate(book)
                    i++
                }
            } catch (t: Throwable) {
                failure.compareAndSet(null, t)
            } finally {
                done.countDown()
            }
        }, "refresher")
        refresher.start()

        // Reader threads: mimic getKey() calls from the main thread (workspace
        // restore) and the Speak widget thread (bookmark rendering).
        for (r in 0 until readers) {
            val reader = Thread({
                try {
                    start.await()
                    var i = 0
                    while (i < iterations && failure.get() == null) {
                        try {
                            book.getKey("entry1")
                        } catch (nske: NoSuchKeyException) {
                            // acceptable: graceful "no such key" is not the bug
                        }
                        i++
                    }
                } catch (t: Throwable) {
                    // A NullPointerException here is the bug we are guarding against.
                    failure.compareAndSet(null, t)
                } finally {
                    done.countDown()
                }
            }, "reader-$r")
            reader.start()
        }

        start.countDown()
        assertTrue(done.await(60, TimeUnit.SECONDS), "threads did not finish in time")

        val t = failure.get()
        if (t != null) {
            throw AssertionError("getKey() must not fail under concurrent (de)activation, but threw: $t", t)
        }
    }

    /**
     * Reproduces OSTicket 3369: [SwordGenBook.getGlobalKeyList] returned
     * the `global` field directly after `checkActive()`. A concurrent
     * `deactivate()` nulls `global`, so the reader (CachedKeyPage on
     * the WebView handler thread, iterating `doc.getGlobalKeyList()`) could
     * receive null and crash. getGlobalKeyList() must never return null.
     */
    @Test
    @Throws(Exception::class)
    fun testGetGlobalKeyListDoesNotReturnNullUnderConcurrentActivation() {
        val book = createBook()

        Activator.activate(book)

        val readers = 4
        val iterations = 20000
        val failure = AtomicReference<Throwable>()
        val start = CountDownLatch(1)
        val done = CountDownLatch(readers + 1)

        val refresher = Thread({
            try {
                start.await()
                var i = 0
                while (i < iterations && failure.get() == null) {
                    Activator.deactivate(book)
                    Activator.activate(book)
                    i++
                }
            } catch (t: Throwable) {
                failure.compareAndSet(null, t)
            } finally {
                done.countDown()
            }
        }, "refresher")
        refresher.start()

        // Reader threads mimic CachedKeyPage.cachedGlobalKeyList's
        // `for (key in doc.globalKeyList)` iteration.
        for (r in 0 until readers) {
            val reader = Thread(Runnable {
                try {
                    start.await()
                    var i = 0
                    while (i < iterations && failure.get() == null) {
                        val global: Key? = book.getGlobalKeyList()
                        if (global == null) {
                            failure.compareAndSet(null,
                                    AssertionError("getGlobalKeyList() returned null"))
                            return@Runnable
                        }
                        // Iterating (as CachedKeyPage does) must not NPE either.
                        for (ignored in global) {
                            // no-op
                        }
                        i++
                    }
                } catch (t: Throwable) {
                    failure.compareAndSet(null, t)
                } finally {
                    done.countDown()
                }
            }, "reader-$r")
            reader.start()
        }

        start.countDown()
        assertTrue(done.await(60, TimeUnit.SECONDS), "threads did not finish in time")

        val t = failure.get()
        if (t != null) {
            throw AssertionError("getGlobalKeyList() must not return null or NPE under concurrent (de)activation, but: $t", t)
        }
    }

    companion object {
        @Throws(java.io.IOException::class, org.crosswire.jsword.book.BookException::class)
        private fun createBook(): SwordGenBook {
            val conf = "[TestGen]\n" +
                    "DataPath=./modules/genbook/rawgenbook/testgen/testgen\n" +
                    "ModDrv=RawGenBook\n" +
                    "Encoding=UTF-8\n" +
                    "SourceType=OSIS\n" +
                    "Lang=en\n" +
                    "Description=Test Generic Book\n"
            val sbmd = SwordBookMetaData(conf.toByteArray(Charsets.UTF_8), "TestGen")
            return SwordGenBook(sbmd, FakeGenBackend())
        }
    }
}
