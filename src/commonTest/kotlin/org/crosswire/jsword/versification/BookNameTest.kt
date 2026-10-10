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
package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException

import org.crosswire.jsword.book.CaseType

/**
 * JUnit test of BookName
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class BookNameTest {
    private var storedCase: CaseType? = null

    @BeforeTest
    fun setUp() {
        storedCase = BookName.getDefaultCase()
    }

    @AfterTest
    fun tearDown() {
        BookName.setCase(storedCase)
    }

    @Test
    fun testCase() {
        BookName.setCase(CaseType.LOWER)
        assertEquals(CaseType.LOWER, BookName.getDefaultCase())
        BookName.setCase(CaseType.UPPER)
        assertEquals(CaseType.UPPER, BookName.getDefaultCase())
        BookName.setCase(CaseType.SENTENCE)
        assertEquals(CaseType.SENTENCE, BookName.getDefaultCase())
    }

    @Test
    fun testGetBookJogger(){
        assertEquals("Gen", BibleBook.GEN.osis)
        assertEquals("Exod", BibleBook.EXOD.osis)
        assertEquals("Rev", BibleBook.REV.osis)
    }

}
