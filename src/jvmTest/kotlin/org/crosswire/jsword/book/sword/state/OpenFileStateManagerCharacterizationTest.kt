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
package org.crosswire.jsword.book.sword.state

import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.BlockType
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.junit.Assume
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Characterization of [OpenFileStateManager]: states are pooled per
 * metadata, released states are reused in FIFO order and a released state is
 * stamped with the access time.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class OpenFileStateManagerCharacterizationTest {

    /** Checks a state out of the manager. */
    private fun interface Checkout<T : OpenFileState> {
        @Throws(BookException::class)
        fun get(): T
    }

    @Test
    fun testInstanceIsSingleton() {
        assertSame(OpenFileStateManager.instance(), OpenFileStateManager.instance())
    }

    @Test
    @Throws(BookException::class)
    fun testZVerseStateIsReusedAfterRelease() {
        val kjv = meta("KJV")
        val manager = OpenFileStateManager.instance()
        val source = zVerse(manager, kjv)

        val first = source.get()
        assertSame(kjv, first.getBookMetaData())

        // while the first is checked out it cannot be served again: a distinct state is returned
        val second = source.get()
        assertNotSame(first, second)

        manager.release(first)
        manager.release(second)

        // the pool is a FIFO queue: first was released first, so it is served first
        val seen = checkoutUntil(source, second)
        assertTrue(seen.contains(first))
        assertTrue(seen.indexOf(first) < seen.indexOf(second))
        manager.release(first)
        manager.release(second)
    }

    @Test
    @Throws(BookException::class)
    fun testReleaseAndReuseStampLastAccess() {
        val kjv = meta("KJV")
        val manager = OpenFileStateManager.instance()
        val state = zVerse(manager, kjv).get()

        state.setLastAccess(0L)
        assertEquals(0L, state.getLastAccess())

        val before = System.currentTimeMillis()
        manager.release(state)
        val afterRelease = System.currentTimeMillis()
        assertTrue(state.getLastAccess() >= before)
        assertTrue(state.getLastAccess() <= afterRelease)

        // checking the state out again refreshes the stamp as well
        state.setLastAccess(0L)
        val beforeReuse = System.currentTimeMillis()
        checkoutUntil(zVerse(manager, kjv), state)
        assertTrue(state.getLastAccess() >= beforeReuse)
        manager.release(state)
    }

    @Test
    fun testReleaseNullIsIgnored() {
        OpenFileStateManager.instance().release(null)
    }

    @Test
    @Throws(BookException::class)
    fun testCloseReleasesBackToThePool() {
        val kjv = meta("KJV")
        val manager = OpenFileStateManager.instance()
        val state = zVerse(manager, kjv).get()
        state.close()
        checkoutUntil(zVerse(manager, kjv), state)
        manager.release(state)
    }

    @Test
    @Throws(BookException::class)
    fun testPoolsAreSeparatePerMetadata() {
        val kjv = meta("KJV")
        val esv = meta("ESV2011")
        val manager = OpenFileStateManager.instance()
        val kjvState = zVerse(manager, kjv).get()
        manager.release(kjvState)

        // the esv pool never serves a kjv state, however many are asked for
        for (i in 0 until 20) {
            val esvState = zVerse(manager, esv).get()
            assertNotSame(kjvState, esvState)
            assertSame(esv, esvState.getBookMetaData())
            esvState.releaseResources()
        }
        checkoutUntil(zVerse(manager, kjv), kjvState)
        manager.release(kjvState)
    }

    @Test
    @Throws(BookException::class)
    fun testStrongsGreekRawLDStateIsReused() {
        val strongs = meta("StrongsGreek")
        assertEquals("RawLD", strongs.getProperty(SwordBookMetaData.KEY_MOD_DRV))
        val manager = OpenFileStateManager.instance()

        val state = manager.getRawLDBackendState(strongs)
        assertSame(strongs, state.getBookMetaData())
        manager.release(state)
        checkoutUntil(Checkout { manager.getRawLDBackendState(strongs) }, state)
        manager.release(state)
    }

    @Test
    @Throws(BookException::class)
    fun testGenBookStateIsReused() {
        val josephus = meta("Josephus")
        Assume.assumeTrue("RawGenBook" == josephus.getProperty(SwordBookMetaData.KEY_MOD_DRV))
        val manager = OpenFileStateManager.instance()

        val meta = josephus
        val state = manager.getGenBookBackendState(josephus)
        assertSame(josephus, state.getBookMetaData())
        manager.release(state)
        checkoutUntil(Checkout { manager.getGenBookBackendState(meta) }, state)
        manager.release(state)
    }

    /**
     * A conf whose DataPath points at a directory holding only the .idx file of a
     * RawLD module: the index is readable but the data file is not, which makes
     * RawLDBackendState give up with a BookException.
     */
    @Test
    @Throws(IOException::class, BookException::class)
    fun testMissingDataFileThrowsBookException() {
        val root = Files.createTempDirectory("ofsm").toFile()
        val modules = File(root, "modules")
        assertTrue(modules.mkdirs())
        val idx = File(modules, "ghost.idx")
        assertTrue(idx.createNewFile())
        try {
            val conf = "[Ghost]\nDataPath=./modules/ghost\nModDrv=RawLD\nLang=en\n"
            val bmd = SwordBookMetaData(conf.toByteArray(Charsets.ISO_8859_1), "ghost.conf")
            bmd.library = root.toURI()
            try {
                OpenFileStateManager.instance().getRawLDBackendState(bmd)
                fail("expected a BookException for the missing .dat file")
            } catch (expected: BookException) {
                assertTrue(expected.message!!.contains("ghost.dat"), expected.message)
            }
        } finally {
            idx.delete()
            modules.delete()
            root.delete()
        }
    }

    companion object {
        private fun meta(initials: String): BookMetaData {
            val book = Books.installed().getBook(initials)
            Assume.assumeNotNull(book)
            return book!!.bookMetaData
        }

        /**
         * Checks states out until `target` comes back (the pool may already hold states released
         * by earlier tests, which are served first because the pool is a FIFO queue). The other states
         * seen on the way are not put back. Returns the states in the order they were served.
         */
        @Throws(BookException::class)
        private fun <T : OpenFileState> checkoutUntil(source: Checkout<T>, target: T): List<T> {
            val seen = ArrayList<T>()
            for (i in 0 until 500) {
                val state = source.get()
                seen.add(state)
                if (state === target) {
                    return seen
                }
            }
            fail("the released state was never served again")
        }

        private fun zVerse(manager: OpenFileStateManager, meta: BookMetaData): Checkout<ZVerseBackendState> {
            return Checkout { manager.getZVerseBackendState(meta, BlockType.BLOCK_BOOK) }
        }
    }
}
