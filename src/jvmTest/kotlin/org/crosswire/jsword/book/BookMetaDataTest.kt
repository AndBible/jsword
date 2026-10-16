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

import org.crosswire.jsword.book.basic.DefaultBookMetaData
import org.crosswire.jsword.book.sword.SwordBookMetaData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class BookMetaDataTest {
    @Test
    fun testDifferentInitialsNotEqual() {
        val kjvMetaData = "[KJV]\nDataPath=./modules/texts/ztext/kjv/\nModDrv=zText\nEncoding=UTF-8\nBlockType=BOOK\nCompressType=ZIP\nSourceType=OSIS\nLang=en\nVersion=2.3\nDescription=King James Version (1769) with Strongs Numbers and Morphology\nLCSH=Bible. English.\n"
        // the only difference is the initials
        val kjvaMetaData = "[KJVA]\nDataPath=./modules/texts/ztext/kjva/\nModDrv=zText\nEncoding=UTF-8\nBlockType=BOOK\nCompressType=ZIP\nSourceType=OSIS\nLang=en\nVersion=2.3\nDescription=King James Version (1769) with Strongs Numbers and Morphology\nLCSH=Bible. English.\n"
        val bmKJV: BookMetaData = SwordBookMetaData(kjvMetaData.toByteArray(), "KJV")
        val bmKJV2: BookMetaData = SwordBookMetaData(kjvMetaData.toByteArray(), "KJV")
        val bmKJVA: BookMetaData = SwordBookMetaData(kjvaMetaData.toByteArray(), "KJVA")
        assertTrue(bmKJV == bmKJV2, "Same metadata should equal")
        assertFalse(bmKJV == bmKJVA, "Different initials should not equal")
    }

    @Test
    fun testSortOrderIsByInitials() {
        val kjvMetaData = "[KJV]\nDataPath=./modules/texts/ztext/kjv/\nModDrv=zText\nEncoding=UTF-8\nBlockType=BOOK\nCompressType=ZIP\nSourceType=OSIS\nLang=en\nVersion=2.3\nDescription=King James Version (1769) with Strongs Numbers and Morphology\nLCSH=Bible. English.\n"
        // Desc starts with 'The...' but initials are 'Common' so comes first if sorted by initials
        val commonMetaData = "[Common]\nDataPath=./modules/texts/ztext/common/\nModDrv=zText\nEncoding=UTF-8\nBlockType=BOOK\nCompressType=ZIP\nSourceType=OSIS\nLang=en\nVersion=2.3\nDescription=The Common Edition: New Testament\nLCSH=Bible. English.\n"
        // Ensure sort is case insensitive
        val aaaMetaData = "[AAA]\nDataPath=./modules/texts/ztext/common/\nModDrv=zText\nEncoding=UTF-8\nBlockType=BOOK\nCompressType=ZIP\nSourceType=OSIS\nLang=en\nVersion=2.3\nDescription=aaa aaa aaa\nLCSH=Bible. English.\n"
        // create some book meta data
        val bmKJV: BookMetaData = SwordBookMetaData(kjvMetaData.toByteArray(), "KJV")
        val bmCommon: BookMetaData = SwordBookMetaData(commonMetaData.toByteArray(), "Common")
        val bmaaa: BookMetaData = SwordBookMetaData(aaaMetaData.toByteArray(), "AAA")

        // sort them
        val mdList = mutableListOf(bmaaa, bmKJV, bmCommon)
        mdList.sort()

        // ensure the book order is as expected
        assertEquals(bmaaa, mdList[0], "AAA should be first in sorted book list")
        assertEquals(bmCommon, mdList[1], "Common should be second in sorted book list")
        assertEquals(bmKJV, mdList[2], "KJV should be last in sorted book list")
    }

    @Test
    fun testEqualityRequiresSameConcreteClass() {
        val base = DefaultBookMetaData(null, "Demo", BookCategory.BIBLE)
        base.setInitials("Demo")
        val derived: BookMetaData = DerivedMetadata()
        val derived2: BookMetaData = DerivedMetadata()
        assertEquals(derived, derived2)
        assertFalse(base == derived)
        assertFalse(derived == base)
    }

    private class DerivedMetadata : DefaultBookMetaData(null, "Demo", BookCategory.BIBLE) {
        init {
            setInitials("Demo")
        }
    }
}
