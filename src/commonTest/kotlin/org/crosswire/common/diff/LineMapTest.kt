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
class LineMapTest {

    @Test
    fun testCompile() {
        // Convert lines down to characters
        val list = mutableListOf("")
        list.add("alpha\n")
        list.add("beta\n")
        var map = LineMap("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n")
        assertEquals("\u0001\u0002\u0001", map.getSourceMap(), "new LineMap:")
        assertEquals("\u0002\u0001\u0002", map.getTargetMap(), "new LineMap:")
        assertEquals(list, map.getLines(), "new LineMap:")

        list.clear()
        list.add("")
        list.add("alpha\r\n")
        list.add("beta\r\n")
        list.add("\r\n")
        map = LineMap("", "alpha\r\nbeta\r\n\r\n\r\n")
        assertEquals("", map.getSourceMap(), "new LineMap:")
        assertEquals("\u0001\u0002\u0003\u0003", map.getTargetMap(), "new LineMap:")
        assertEquals(list, map.getLines(), "new LineMap:")
    }

    @Test
    fun testRestore() {
        // Convert chars up to lines
        val diffs = diffList(
            Difference(EditType.EQUAL, "\u0001\u0002\u0001"), Difference(EditType.INSERT, "\u0002\u0001\u0002"))
        val map = LineMap("alpha\nbeta\nalpha\n", "beta\nalpha\nbeta\n")
        map.restore(diffs)
        assertEquals(
            diffList(Difference(EditType.EQUAL, "alpha\nbeta\nalpha\n"), Difference(EditType.INSERT, "beta\nalpha\nbeta\n"))[0], diffs[0], "LineMap.restore:")
        assertEquals(
            diffList(Difference(EditType.EQUAL, "alpha\nbeta\nalpha\n"), Difference(EditType.INSERT, "beta\nalpha\nbeta\n"))[diffs.size - 1], diffs[diffs.size - 1], "LineMap.restore:")
        assertEquals(
            diffList(Difference(EditType.EQUAL, "alpha\nbeta\nalpha\n"), Difference(EditType.INSERT, "beta\nalpha\nbeta\n")), diffs, "LineMap.restore:")
    }

    // Private function for quickly building lists of diffs.
    private fun <T> diffList(vararg items: T): MutableList<T> = items.toMutableList()
}
