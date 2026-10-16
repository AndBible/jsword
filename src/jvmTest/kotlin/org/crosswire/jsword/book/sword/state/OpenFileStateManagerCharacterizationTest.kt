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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 */
package org.crosswire.jsword.book.sword.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.book.BookMetaData;
import org.crosswire.jsword.book.Books;
import org.crosswire.jsword.book.sword.BlockType;
import org.crosswire.jsword.book.sword.SwordBookMetaData;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

/**
 * Characterization of {@link OpenFileStateManager}: states are pooled per
 * metadata, released states are reused in FIFO order and a released state is
 * stamped with the access time.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class OpenFileStateManagerCharacterizationTest {
    private static BookMetaData meta(String initials) {
        Book book = Books.installed().getBook(initials);
        Assume.assumeNotNull(book);
        return book.getBookMetaData();
    }

    /** Checks a state out of the manager. */
    private interface Checkout<T extends OpenFileState> {
        T get() throws BookException;
    }

    /**
     * Checks states out until {@code target} comes back (the pool may already hold states released
     * by earlier tests, which are served first because the pool is a FIFO queue). The other states
     * seen on the way are not put back. Returns the states in the order they were served.
     */
    private static <T extends OpenFileState> List<T> checkoutUntil(Checkout<T> source, T target) throws BookException {
        List<T> seen = new ArrayList<T>();
        for (int i = 0; i < 500; i++) {
            T state = source.get();
            seen.add(state);
            if (state == target) {
                return seen;
            }
        }
        Assert.fail("the released state was never served again");
        return seen;
    }

    private static Checkout<ZVerseBackendState> zVerse(final OpenFileStateManager manager, final BookMetaData meta) {
        return new Checkout<ZVerseBackendState>() {
            public ZVerseBackendState get() throws BookException {
                return manager.getZVerseBackendState(meta, BlockType.BLOCK_BOOK);
            }
        };
    }

    @Test
    public void testInstanceIsSingleton() {
        Assert.assertSame(OpenFileStateManager.instance(), OpenFileStateManager.instance());
    }

    @Test
    public void testZVerseStateIsReusedAfterRelease() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        Checkout<ZVerseBackendState> source = zVerse(manager, kjv);

        ZVerseBackendState first = source.get();
        Assert.assertSame(kjv, first.getBookMetaData());

        // while the first is checked out it cannot be served again: a distinct state is returned
        ZVerseBackendState second = source.get();
        Assert.assertNotSame(first, second);

        manager.release(first);
        manager.release(second);

        // the pool is a FIFO queue: first was released first, so it is served first
        List<ZVerseBackendState> seen = checkoutUntil(source, second);
        Assert.assertTrue(seen.contains(first));
        Assert.assertTrue(seen.indexOf(first) < seen.indexOf(second));
        manager.release(first);
        manager.release(second);
    }

    @Test
    public void testReleaseAndReuseStampLastAccess() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState state = zVerse(manager, kjv).get();

        state.setLastAccess(0L);
        Assert.assertEquals(0L, state.getLastAccess());

        long before = System.currentTimeMillis();
        manager.release(state);
        long afterRelease = System.currentTimeMillis();
        Assert.assertTrue(state.getLastAccess() >= before);
        Assert.assertTrue(state.getLastAccess() <= afterRelease);

        // checking the state out again refreshes the stamp as well
        state.setLastAccess(0L);
        long beforeReuse = System.currentTimeMillis();
        checkoutUntil(zVerse(manager, kjv), state);
        Assert.assertTrue(state.getLastAccess() >= beforeReuse);
        manager.release(state);
    }

    @Test
    public void testReleaseNullIsIgnored() {
        OpenFileStateManager.instance().release(null);
    }

    @Test
    public void testCloseReleasesBackToThePool() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState state = zVerse(manager, kjv).get();
        state.close();
        checkoutUntil(zVerse(manager, kjv), state);
        manager.release(state);
    }

    @Test
    public void testPoolsAreSeparatePerMetadata() throws BookException {
        BookMetaData kjv = meta("KJV");
        BookMetaData esv = meta("ESV2011");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState kjvState = zVerse(manager, kjv).get();
        manager.release(kjvState);

        // the esv pool never serves a kjv state, however many are asked for
        for (int i = 0; i < 20; i++) {
            ZVerseBackendState esvState = zVerse(manager, esv).get();
            Assert.assertNotSame(kjvState, esvState);
            Assert.assertSame(esv, esvState.getBookMetaData());
            esvState.releaseResources();
        }
        checkoutUntil(zVerse(manager, kjv), kjvState);
        manager.release(kjvState);
    }

    @Test
    public void testStrongsGreekRawLDStateIsReused() throws BookException {
        final BookMetaData strongs = meta("StrongsGreek");
        Assert.assertEquals("RawLD", strongs.getProperty(SwordBookMetaData.KEY_MOD_DRV));
        final OpenFileStateManager manager = OpenFileStateManager.instance();

        RawLDBackendState state = manager.getRawLDBackendState(strongs);
        Assert.assertSame(strongs, state.getBookMetaData());
        manager.release(state);
        checkoutUntil(new Checkout<RawLDBackendState>() {
            public RawLDBackendState get() throws BookException {
                return manager.getRawLDBackendState(strongs);
            }
        }, state);
        manager.release(state);
    }

    @Test
    public void testGenBookStateIsReused() throws BookException {
        BookMetaData josephus = meta("Josephus");
        Assume.assumeTrue("RawGenBook".equals(josephus.getProperty(SwordBookMetaData.KEY_MOD_DRV)));
        final OpenFileStateManager manager = OpenFileStateManager.instance();

        final BookMetaData meta = josephus;
        GenBookBackendState state = manager.getGenBookBackendState(josephus);
        Assert.assertSame(josephus, state.getBookMetaData());
        manager.release(state);
        checkoutUntil(new Checkout<GenBookBackendState>() {
            public GenBookBackendState get() throws BookException {
                return manager.getGenBookBackendState(meta);
            }
        }, state);
        manager.release(state);
    }

    /**
     * A conf whose DataPath points at a directory holding only the .idx file of a
     * RawLD module: the index is readable but the data file is not, which makes
     * RawLDBackendState give up with a BookException.
     */
    @Test
    public void testMissingDataFileThrowsBookException() throws IOException, BookException {
        File root = Files.createTempDirectory("ofsm").toFile();
        File modules = new File(root, "modules");
        Assert.assertTrue(modules.mkdirs());
        File idx = new File(modules, "ghost.idx");
        Assert.assertTrue(idx.createNewFile());
        try {
            String conf = "[Ghost]\nDataPath=./modules/ghost\nModDrv=RawLD\nLang=en\n";
            SwordBookMetaData bmd = new SwordBookMetaData(conf.getBytes("ISO-8859-1"), "ghost.conf");
            bmd.setLibrary(root.toURI());
            try {
                OpenFileStateManager.instance().getRawLDBackendState(bmd);
                Assert.fail("expected a BookException for the missing .dat file");
            } catch (BookException expected) {
                Assert.assertTrue(expected.getMessage(), expected.getMessage().contains("ghost.dat"));
            }
        } finally {
            idx.delete();
            modules.delete();
            root.delete();
        }
    }
}
