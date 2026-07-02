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
package org.crosswire.jsword.book.sword;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.crosswire.common.activate.Activator;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.passage.DefaultKeyList;
import org.crosswire.jsword.passage.DefaultLeafKeyList;
import org.crosswire.jsword.passage.Key;
import org.crosswire.jsword.passage.NoSuchKeyException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Reproduces the NPE in {@link SwordGenBook#getKey(String)} that occurs when a
 * book is repeatedly deactivated/reactivated through the {@link Activator}
 * (as AndBible's AI Documents books are, via sync-triggered refreshes) while
 * concurrently being read from another thread.
 *
 * <p>The root cause is that {@link Activator} performs a non-atomic
 * check-and-act on a non-thread-safe {@code HashSet}, so the Activator's notion
 * of "activated" can desync from the book's own {@code map}/{@code active}
 * state. A reader passing {@code checkActive()} can then observe a null map and
 * crash with an NPE on {@code map.get()}.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class SwordGenBookConcurrencyTest {

    /**
     * A backend whose readIndex() returns a small, non-empty key list so that
     * activate() builds a non-null, populated map.
     */
    private static final class FakeGenBackend extends NullBackend {
        @Override
        public Key readIndex() {
            DefaultKeyList list = new DefaultKeyList();
            list.addAll(new DefaultLeafKeyList("entry1", "entry1"));
            list.addAll(new DefaultLeafKeyList("entry2", "entry2"));
            return list;
        }
    }

    private static SwordGenBook createBook() throws IOException, BookException {
        String conf = "[TestGen]\n"
                + "DataPath=./modules/genbook/rawgenbook/testgen/testgen\n"
                + "ModDrv=RawGenBook\n"
                + "Encoding=UTF-8\n"
                + "SourceType=OSIS\n"
                + "Lang=en\n"
                + "Description=Test Generic Book\n";
        SwordBookMetaData sbmd = new SwordBookMetaData(conf.getBytes("UTF-8"), "TestGen");
        return new SwordGenBook(sbmd, new FakeGenBackend());
    }

    @Test
    public void testGetKeyDoesNotNpeUnderConcurrentActivation() throws Exception {
        final SwordGenBook book = createBook();

        // Prime it: activated set now contains the book, map is populated.
        Activator.activate(book);

        final int readers = 4;
        final int iterations = 20000;
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(readers + 1);

        // Refresher thread: mimics MyDocumentBookManager.refreshDocument(), which
        // deactivates then reactivates the book through the Activator.
        Thread refresher = new Thread(new Runnable() {
            public void run() {
                try {
                    start.await();
                    for (int i = 0; i < iterations && failure.get() == null; i++) {
                        Activator.deactivate(book);
                        Activator.activate(book);
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            }
        }, "refresher");
        refresher.start();

        // Reader threads: mimic getKey() calls from the main thread (workspace
        // restore) and the Speak widget thread (bookmark rendering).
        for (int r = 0; r < readers; r++) {
            Thread reader = new Thread(new Runnable() {
                public void run() {
                    try {
                        start.await();
                        for (int i = 0; i < iterations && failure.get() == null; i++) {
                            try {
                                book.getKey("entry1");
                            } catch (NoSuchKeyException nske) {
                                // acceptable: graceful "no such key" is not the bug
                            }
                        }
                    } catch (Throwable t) {
                        // A NullPointerException here is the bug we are guarding against.
                        failure.compareAndSet(null, t);
                    } finally {
                        done.countDown();
                    }
                }
            }, "reader-" + r);
            reader.start();
        }

        start.countDown();
        Assert.assertTrue("threads did not finish in time", done.await(60, TimeUnit.SECONDS));

        Throwable t = failure.get();
        if (t != null) {
            throw new AssertionError("getKey() must not fail under concurrent (de)activation, but threw: " + t, t);
        }
    }
}
