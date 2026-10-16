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
import kotlin.test.assertNull

/**
 * JUnit Test.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class CommonalityTest {

    @Test
    fun testPrefix() {
        // Detect and remove any common prefix.
        assertEquals(0, Commonality.prefix("abc", "xyz"), "Commonality.prefix: Null case.")
        assertEquals(4, Commonality.prefix("1234abc", "1234xyz"), "Commonality.prefix: Non-null case.")
    }

    @Test
    fun testSuffix() {
        // Detect and remove any common suffix.
        assertEquals(0, Commonality.suffix("abc", "xyz"), "Commonality.suffix: Null case.")
        assertEquals(4, Commonality.suffix("abc1234", "xyz1234"), "Commonality.suffix: Non-null case.")
    }

    @Test
    fun testHalfmatch() {
        // Detect a halfmatch.
        assertNull(Commonality.halfMatch("1234567890", "abcdef"), "Commonality.halfMatch: No match.")
        assertEquals(
            CommonMiddle("12", "90", "a", "z", "345678"), Commonality.halfMatch("1234567890", "a345678z"), "Commonality.halfMatch: Single Match #1.")
        assertEquals(
            CommonMiddle("a", "z", "12", "90", "345678"), Commonality.halfMatch("a345678z", "1234567890"), "Commonality.halfMatch: Single Match #2.")
        assertEquals(
            CommonMiddle("12123", "123121", "a", "z", "1234123451234"), Commonality.halfMatch("121231234123451234123121", "a1234123451234z"), "Commonality.halfMatch: Multiple Matches #1.")
        assertEquals(
            CommonMiddle("", "-=-=-=-=-=", "x", "", "x-=-=-=-=-=-=-="), Commonality.halfMatch("x-=-=-=-=-=-=-=-=-=-=-=-=", "xx-=-=-=-=-=-=-="), "Commonality.halfMatch: Multiple Matches #2.")
        assertEquals(
            CommonMiddle("-=-=-=-=-=", "", "", "y", "-=-=-=-=-=-=-=y"), Commonality.halfMatch("-=-=-=-=-=-=-=-=-=-=-=-=y", "-=-=-=-=-=-=-=yy"), "Commonality.halfMatch: Multiple Matches #3.")
    }
}
