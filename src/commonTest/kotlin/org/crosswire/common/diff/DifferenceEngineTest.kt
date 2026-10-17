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
class DifferenceEngineTest {

    /**
     * Exposes the protected path tracing of [DifferenceEngine] to this test. The Java original
     * reached it as a same-package caller, which Kotlin does not allow.
     */
    private class PathProbe : DifferenceEngine() {
        fun tracePath1(vMap: MutableList<MutableSet<String>>, newSource: String, newTarget: String): MutableList<Difference> =
            path1(vMap, newSource, newTarget)

        fun tracePath2(vMap: MutableList<MutableSet<String>>, newSource: String, newTarget: String): MutableList<Difference> =
            path2(vMap, newSource, newTarget)
    }

    @Test
    fun testDiffPath() {
        // Single letters
        // Trace a path from back to front.
        val generator = PathProbe()
        val vMap: MutableList<MutableSet<String>> = mutableListOf()
        vMap.add(mutableSetOf("0,0"))
        vMap.add(mutableSetOf("0,1", "1,0"))
        vMap.add(mutableSetOf("0,2", "2,0", "2,2"))
        vMap.add(mutableSetOf("0,3", "2,3", "3,0", "4,3"))
        vMap.add(mutableSetOf("0,4", "2,4", "4,0", "4,4", "5,3"))
        vMap.add(mutableSetOf("0,5", "2,5", "4,5", "5,0", "6,3", "6,5"))
        vMap.add(mutableSetOf("0,6", "2,6", "4,6", "6,6", "7,5"))

        var diffs: MutableList<Difference> = diffList(
            Difference(EditType.INSERT, "W"), Difference(EditType.DELETE, "A"), Difference(EditType.EQUAL, "1"), Difference(EditType.DELETE, "B"), Difference(EditType.EQUAL, "2"), Difference(EditType.INSERT, "X"), Difference(EditType.DELETE, "C"), Difference(EditType.EQUAL, "3"), Difference(EditType.DELETE, "D"))
        assertEquals(diffs, generator.tracePath1(vMap, "A1B2C3D", "W12X3"), "diff_path1: Single letters.")

        // Trace a path from front to back.
        vMap.removeAt(vMap.size - 1)
        diffs = diffList(
            Difference(EditType.EQUAL, "4"), Difference(EditType.DELETE, "E"), Difference(EditType.INSERT, "Y"), Difference(EditType.EQUAL, "5"), Difference(EditType.DELETE, "F"), Difference(EditType.EQUAL, "6"), Difference(EditType.DELETE, "G"), Difference(EditType.INSERT, "Z"))
        assertEquals(diffs, generator.tracePath2(vMap, "4E5F6G", "4Y56Z"), "diff_path2: Single letters.")

        // Double letters
        // Trace a path from back to front.
        val vMap2: MutableList<MutableSet<String>> = mutableListOf()
        vMap2.add(mutableSetOf("0,0"))
        vMap2.add(mutableSetOf("0,1", "1,0"))
        vMap2.add(mutableSetOf("0,2", "1,1", "2,0"))
        vMap2.add(mutableSetOf("0,3", "1,2", "2,1", "3,0"))
        vMap2.add(mutableSetOf("0,4", "1,3", "3,1", "4,0", "4,4"))

        diffs = diffList(
            Difference(EditType.INSERT, "WX"), Difference(EditType.DELETE, "AB"), Difference(EditType.EQUAL, "12"))
        assertEquals(diffs, generator.tracePath1(vMap2, "AB12", "WX12"), "diff_path1: Double letters.")

        // Trace a path from front to back.
        val vMap3: MutableList<MutableSet<String>> = mutableListOf()
        vMap3.add(mutableSetOf("0,0"))
        vMap3.add(mutableSetOf("0,1", "1,0"))
        vMap3.add(mutableSetOf("1,1", "2,0", "2,4"))
        vMap3.add(mutableSetOf("2,1", "2,5", "3,0", "3,4"))
        vMap3.add(mutableSetOf("2,6", "3,5", "4,4"))

        diffs = diffList(
            Difference(EditType.DELETE, "CD"), Difference(EditType.EQUAL, "34"), Difference(EditType.INSERT, "YZ"))
        assertEquals(diffs, generator.tracePath2(vMap3, "CD34", "34YZ"), "diff_path2: Double letters.")
    }

    @Test
    fun testTimeout() {
        DifferenceEngine.setTimeout(0.001f) // 1ms
        // This test may fail on extremely fast computers. If so, just increase
        // the text lengths.
        assertNull(
            DifferenceEngine("`Twas brillig, and the slithy toves\nDid gyre and gimble in the wabe:\nAll mimsy were the borogoves,\nAnd the mome raths outgrabe.", "I am the very model of a modern major general,\nI've information vegetable, animal, and mineral,\nI know the kings of England, and I quote the fights historical,\nFrom Marathon to Waterloo, in order categorical.").generate(), "diff_main: Timeout.")
        DifferenceEngine.setTimeout(0f)
    }

    // Private function for quickly building lists of diffs.
    private fun <T> diffList(vararg items: T): MutableList<T> = items.toMutableList()
}
