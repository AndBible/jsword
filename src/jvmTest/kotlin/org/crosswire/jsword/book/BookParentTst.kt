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
 *
 */
package org.crosswire.jsword.book

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * JUnit Test. For when we don't actually want to do testing of responses
 *
 * Open (not abstract) so that Gradle still runs this class itself, as it did in Java, and the
 * subclasses inherit [testNothing].
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class BookParentTst {
    protected lateinit var bmds: Array<BookMetaData?>
    protected lateinit var bibles: Array<Book?>

    @BeforeTest
    @Throws(Exception::class)
    open fun setUp() {
        val lbmds = Books.installed().getBooks(BookFilters.getOnlyBibles())
        bibles = arrayOfNulls(lbmds.size)
        bmds = arrayOfNulls(lbmds.size)

        var i = 0
        for (book in lbmds) {
            bmds[i] = book.bookMetaData
            i++
        }
    }

    @Test
    open fun testNothing() {
        assertTrue(true)
    }
}
