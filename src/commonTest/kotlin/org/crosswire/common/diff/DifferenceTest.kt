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
package org.crosswire.common.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class DifferenceTest {
    @Test
    fun testHashCode() {
        assertTrue(Difference(EditType.EQUAL, "a").hashCode() == Difference(EditType.EQUAL, "a").hashCode(), "Difference.hashCode:")
        assertFalse(Difference(EditType.EQUAL, "a").hashCode() == Difference(EditType.EQUAL, "ab").hashCode(), "Difference.hashCode:")
        assertFalse(Difference(EditType.EQUAL, "a").hashCode() == Difference(EditType.INSERT, "a").hashCode(), "Difference.hashCode:")
    }

    @Test
    fun testEquals() {
        // First check that Diff equality works
        assertTrue(Difference(EditType.EQUAL, "a").equals(Difference(EditType.EQUAL, "a")), "Difference.equals:")
        assertEquals(Difference(EditType.EQUAL, "a"), Difference(EditType.EQUAL, "a"), "Difference.equals:")
    }
}
