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
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.jsword.passage

import kotlin.test.*

/**
 * Characterization of {@link DefaultLeafKeyList} with a null name. Java returned the null
 * from the getters the Kotlin conversion declares them non-null and returns "" instead
 * (documented deviation) so that iterating such a key never throws.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class DefaultLeafKeyListCharacterizationTest {
    @Test
    fun iteratingListWithNullNamedLeafDoesNotThrow() {
        var list: DefaultKeyList = DefaultKeyList(null, "list")
        var leaf: DefaultLeafKeyList = DefaultLeafKeyList((null as String?))
        list.addAll(leaf)
        var seen: Int = 0
        for (k in list) {
            assertSame(leaf, k)
            assertEquals("", k.getName())
            assertEquals("", k.getOsisRef())
            assertEquals("", k.getOsisID())
            assertEquals("", k.getRootName())
            seen++
        }
        assertEquals(1, seen)
    }
}
