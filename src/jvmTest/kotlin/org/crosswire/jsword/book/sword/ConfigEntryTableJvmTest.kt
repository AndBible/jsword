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
 * © CrossWire Bible Society, 2009 - 2016
 *
 */
package org.crosswire.jsword.book.sword

import fi.sykero.kdom.Element
import org.crosswire.common.util.IniSection
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.passage.NoSuchKeyException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * A Raw File format that allows for each verse to have it's own storage.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author mbergmann
 * @author DM Smith
 */
class ConfigEntryTableJvmTest {

    // TODO(DMS): make this test use mocks or setup its own environment
    @Test
    fun failingAddConfigEntry() {
        val modName = "TestBook"
        val table = IniSection(modName)
        assertNotNull(table)

        table.add(BookMetaData.KEY_LANG, "de")
        assertEquals("de", table.get(BookMetaData.KEY_LANG))
        val feature = FeatureType.STRONGS_NUMBERS
        table.add(SwordBookMetaData.KEY_FEATURE, FeatureType.STRONGS_NUMBERS.toString())
        if (table.containsValue(SwordBookMetaData.KEY_FEATURE, feature.toString())) {
            assertTrue(true, "Should have Strongs")
        } else {
            // Many "features" are GlobalOptionFilters, which in the Sword C++ API
            // indicate a class to use for filtering.
            // These mostly have the source type prepended to the feature
            val buffer = StringBuilder(table.get(SwordBookMetaData.KEY_SOURCE_TYPE)!!)
            buffer.append(feature)
            if (table.containsValue(SwordBookMetaData.KEY_GLOBAL_OPTION_FILTER, buffer.toString())) {
                assertTrue(true, "Should have Strongs")
            } else {
                // But some do not
                assertTrue(table.containsValue(SwordBookMetaData.KEY_GLOBAL_OPTION_FILTER, feature.toString()), "Should have Strongs")
            }
        }
        val book = Books.installed().getBook("KJV")!!
        assertTrue(book.bookMetaData.hasFeature(FeatureType.STRONGS_NUMBERS), "Should have Strongs")

        try {
            val key = book.getKey("Gen 1:1")
            val data = BookData(book, key)
            try {
                val osis: Element = data.osisFragment
                val strongsNumbers = OSISUtil.getStrongsNumbers(osis)
                assertTrue(strongsNumbers.length > 0, "No Strongs in KJV")
            } catch (ex: BookException) {
                fail("Should have Gen 1:1 data")
            }
        } catch (ex: NoSuchKeyException) {
            fail("Should have Gen 1:1 key")
        }
    }

}
