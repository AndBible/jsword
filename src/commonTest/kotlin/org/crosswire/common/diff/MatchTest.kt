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

/**
 * JUnit Test.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class MatchTest {

    @Test
    fun testMatchMain() {
        assertEquals(0, Match("abcdef", "abcdef", 1000).locate(), "match_main: Equality.")
        assertEquals(-1, Match("", "abcdef", 1).locate(), "match_main: Null text.")
        assertEquals(3, Match("abcdef", "", 3).locate(), "match_main: Null pattern.")
        assertEquals(3, Match("abcdef", "de", 3).locate(), "match_main: Exact match.")
        Bitap.setThreshold(0.7f)
        assertEquals(4, Match("I am the very model of a modern major general.", " that berry ", 5).locate(), "match_main: Complex match.")
        Bitap.setThreshold(0.5f)
    }

}
