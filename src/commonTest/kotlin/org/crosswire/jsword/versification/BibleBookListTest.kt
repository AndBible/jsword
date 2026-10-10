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
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException



/**
 * JUnit test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BibleBookListTest {

    private val booksNT = arrayOf(
    BibleBook.MATT,
    BibleBook.MARK,
    BibleBook.LUKE,
    BibleBook.JOHN,
    BibleBook.ACTS,
    BibleBook.JAS,
    BibleBook.PET1,
    BibleBook.PET2,
    BibleBook.JOHN1,
    BibleBook.JOHN2,
    BibleBook.JOHN3,
    BibleBook.JUDE,
    BibleBook.ROM,
    BibleBook.COR1,
    BibleBook.COR2,
    BibleBook.GAL,
    BibleBook.EPH,
    BibleBook.PHIL,
    BibleBook.COL,
    BibleBook.THESS1,
    BibleBook.THESS2,
    BibleBook.TIM1,
    BibleBook.TIM2,
    BibleBook.TITUS,
    BibleBook.PHLM,
    BibleBook.HEB,
    BibleBook.REV,
    )

    private lateinit var list: BibleBookList

    @BeforeTest
    fun setUp(){
        list = BibleBookList(booksNT)
    }

    @Test
    fun testCount() {
        assertEquals(booksNT.size, list.bookCount)
    }

    @Test
    fun testContains() {
        // Are all the books from booksNT present
        for (b in booksNT) {
            assertTrue(list.contains(b), b.osis)
        }
    }

    @Test
    fun testOrdinal() {
        for (i in 0 until booksNT.size) {
            var b: BibleBook = booksNT[i]
            assertEquals(i, list.getOrdinal(b), b.osis)
        }
    }

    @Test
    fun testGet() {
        for (i in 0 until booksNT.size) {
            var b: BibleBook = booksNT[i]
            assertEquals(b, list.getBook(i), b.osis)
        }
        assertEquals(booksNT[0], list.getBook(-1), booksNT[0].osis)
        assertEquals(booksNT[booksNT.size - 1], list.getBook(booksNT.size), booksNT[booksNT.size - 1].osis)
    }

    @Test
    fun testNextBook() {
        for (i in 0 until booksNT.size - 1) {
            var b: BibleBook = booksNT[i]
            var n: BibleBook = booksNT[i + 1]
            assertEquals(n, list.getNextBook(b), b.osis)
        }
        // Test the last book in the list
        assertEquals(null, list.getNextBook(booksNT[booksNT.size - 1]), "Next after last book")
        // Test a book not in the list
        assertEquals(null, list.getNextBook(BibleBook.GEN), BibleBook.GEN.osis)
    }

    @Test
    fun testPreviousBook() {
        for (i in 1 until booksNT.size) {
            var b: BibleBook = booksNT[i]
            var p: BibleBook = booksNT[i - 1]
            assertEquals(p, list.getPreviousBook(b), b.osis)
        }
        // Test the first book in the list
        assertEquals(null, list.getPreviousBook(booksNT[0]), "Prior before first book")
        // Test a book not in the list
        assertEquals(null, list.getPreviousBook(BibleBook.GEN), BibleBook.GEN.osis)
    }

    @Test
    fun testIterator() {
        var i: Int = 0
        for (book in list) {
            var b: BibleBook = booksNT[i++]
            assertEquals(b, book, b.osis)
        }
        // Same loop written differently.
        i = 0
        var iter: Iterator<BibleBook> = list.iterator()
        while (iter.hasNext()) {
            var book: BibleBook = iter.next()
            var b: BibleBook = booksNT[i++]
            assertEquals(b, book, b.osis)
            try {
                (iter as MutableIterator<BibleBook>).remove()
                fail("Remove is not a supported operation on a BibleBookList")
            } catch (e: UnsupportedOperationException) {
                // This is allowed
            }
        }
        try {
            iter.next()
            fail("Cannot call next after exhausting a BibleBookList iterator")
        } catch (e: NoSuchElementException) {
            // This is allowed
        }
    }
}
