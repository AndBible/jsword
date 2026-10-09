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
 * © CrossWire Bible Society, 2005 - 2016
 */
package org.crosswire.jsword.book.sword.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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

    @Test
    public void testInstanceIsSingleton() {
        Assert.assertSame(OpenFileStateManager.instance(), OpenFileStateManager.instance());
    }

    @Test
    public void testZVerseStateIsReusedAfterRelease() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();

        ZVerseBackendState first = manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
        Assert.assertSame(kjv, first.getBookMetaData());

        // while the first is checked out, the pool is empty: a distinct state is created
        ZVerseBackendState second = manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
        Assert.assertNotSame(first, second);

        manager.release(first);
        manager.release(second);

        // the pool is a FIFO queue
        Assert.assertSame(first, manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK));
        Assert.assertSame(second, manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK));
        manager.release(first);
        manager.release(second);
        // drain so that other tests start from a known pool
        manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
        manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
    }

    @Test
    public void testReleaseAndReuseStampLastAccess() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState state = manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);

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
        Assert.assertSame(state, manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK));
        Assert.assertTrue(state.getLastAccess() >= beforeReuse);
    }

    @Test
    public void testReleaseNullIsIgnored() {
        OpenFileStateManager.instance().release(null);
    }

    @Test
    public void testCloseReleasesBackToThePool() throws BookException {
        BookMetaData kjv = meta("KJV");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState state = manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
        state.close();
        Assert.assertSame(state, manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK));
    }

    @Test
    public void testPoolsAreSeparatePerMetadata() throws BookException {
        BookMetaData kjv = meta("KJV");
        BookMetaData esv = meta("ESV2011");
        OpenFileStateManager manager = OpenFileStateManager.instance();
        ZVerseBackendState kjvState = manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK);
        manager.release(kjvState);
        ZVerseBackendState esvState = manager.getZVerseBackendState(esv, BlockType.BLOCK_BOOK);
        Assert.assertNotSame(kjvState, esvState);
        Assert.assertSame(esv, esvState.getBookMetaData());
        Assert.assertSame(kjvState, manager.getZVerseBackendState(kjv, BlockType.BLOCK_BOOK));
        manager.release(kjvState);
        manager.release(esvState);
    }

    @Test
    public void testStrongsGreekRawLDStateIsReused() throws BookException {
        BookMetaData strongs = meta("StrongsGreek");
        Assert.assertEquals("RawLD", strongs.getProperty(SwordBookMetaData.KEY_MOD_DRV));
        OpenFileStateManager manager = OpenFileStateManager.instance();

        RawLDBackendState state = manager.getRawLDBackendState(strongs);
        Assert.assertSame(strongs, state.getBookMetaData());
        manager.release(state);
        Assert.assertSame(state, manager.getRawLDBackendState(strongs));
        manager.release(state);
    }

    @Test
    public void testGenBookStateIsReused() throws BookException {
        BookMetaData josephus = meta("Josephus");
        Assume.assumeTrue("RawGenBook".equals(josephus.getProperty(SwordBookMetaData.KEY_MOD_DRV)));
        OpenFileStateManager manager = OpenFileStateManager.instance();

        GenBookBackendState state = manager.getGenBookBackendState(josephus);
        Assert.assertSame(josephus, state.getBookMetaData());
        manager.release(state);
        Assert.assertSame(state, manager.getGenBookBackendState(josephus));
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
