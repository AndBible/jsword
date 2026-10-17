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
 * © CrossWire Bible Society, 2014 - 2016
 *
 */
package org.crosswire.jsword.book.filter.thml

import fi.sykero.kdom.Element
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.passage.NoSuchKeyException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * JUnit Test
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class ATagTest {

    @Test
    fun testReference() {
        try {
            val thmlFilter = THMLFilter()
            val dummyBook = Books.installed().getBook("KJV")
            val out = thmlFilter.toOSIS(dummyBook, dummyBook!!.getKey("Gen.1.1"), "<a href=\"sword://StrongsRealGreek/01909\">1909</a>")
            assertEquals(
                "<reference osisRef=\"sword://StrongsRealGreek/01909\">1909</reference>",
                XMLOutputter().outputString(out[0] as Element),
                "THML reference not handled correctly",
            )
        } catch (e: NoSuchKeyException) {
            fail()
        }
    }

}
