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
import kotlin.test.assertContentEquals
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
class DiffTest {
    @Test
    fun testDiffMain() {
        // Perform a trivial diff
        var diffs: List<Difference> = diffList(
            Difference(EditType.EQUAL, "abc"))
        assertEquals(diffs, Diff("abc", "abc", false).compare(), "diff_main: Null case.")
        diffs = diffList(
            Difference(EditType.EQUAL, "ab"), Difference(EditType.INSERT, "123"), Difference(EditType.EQUAL, "c"))
        assertEquals(diffs, Diff("abc", "ab123c", false).compare(), "diff_main: Simple insertion.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "123"), Difference(EditType.EQUAL, "bc"))
        assertEquals(diffs, Diff("a123bc", "abc", false).compare(), "diff_main: Simple deletion.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.INSERT, "123"), Difference(EditType.EQUAL, "b"), Difference(EditType.INSERT, "456"), Difference(EditType.EQUAL, "c"))
        assertEquals(diffs, Diff("abc", "a123b456c", false).compare(), "diff_main: Two insertions.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "123"), Difference(EditType.EQUAL, "b"), Difference(EditType.DELETE, "456"), Difference(EditType.EQUAL, "c"))
        assertEquals(diffs, Diff("a123b456c", "abc", false).compare(), "diff_main: Two deletions.")

        // Perform a real diff
        // Switch off the timeout.
        // dmp.Diff_Timeout = 0;
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "b"))
        assertEquals(diffs, Diff("a", "b", false).compare(), "diff_main: Simple case #1.")
        diffs = diffList(
            Difference(EditType.DELETE, "Apple"), Difference(EditType.INSERT, "Banana"), Difference(EditType.EQUAL, "s are a"), Difference(EditType.INSERT, "lso"), Difference(EditType.EQUAL, " fruit."))
        assertEquals(diffs, Diff("Apples are a fruit.", "Bananas are also fruit.", false).compare(), "diff_main: Simple case #2.")
        diffs = diffList(
            Difference(EditType.DELETE, "1"), Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "y"), Difference(EditType.EQUAL, "b"), Difference(EditType.DELETE, "2"), Difference(EditType.INSERT, "xab"))
        assertEquals(diffs, Diff("1ayb2", "abxab", false).compare(), "diff_main: Overlap #1.")
        diffs = diffList(
            Difference(EditType.INSERT, "x"), Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "b"), Difference(EditType.INSERT, "x"), Difference(EditType.EQUAL, "c"), Difference(EditType.DELETE, "y"), Difference(EditType.INSERT, "xabc"))
        assertEquals(diffs, Diff("abcy", "xaxcxabc", false).compare(), "diff_main: Overlap #2.")

        // Test the linemode speedup
        // Must be long to pass the 250 char cutoff.
        var a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n"
        var b = "abcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\nabcdefghij\n"
        assertEquals(Diff(a, b, false).compare(), Diff(a, b, true).compare(), "diff_main: Simple.")
        a = "1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n1234567890\n"
        b = "abcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n1234567890\n1234567890\n1234567890\nabcdefghij\n"
        val textsLineMode = diffRebuildTests(Diff(a, b, true).compare())
        val textsTextMode = diffRebuildTests(Diff(a, b, false).compare())
        assertContentEquals(textsTextMode, textsLineMode, "diff_main: Overlap.")
    }

    @Test
    fun testDiffXIndex() {
        // Translate a location in text1 to text2
        var diffs: List<Difference> = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "1234"), Difference(EditType.EQUAL, "xyz"))
        assertEquals(5, Diff("", "").xIndex(diffs, 2), "diff_xIndex: Translation on equality.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "1234"), Difference(EditType.EQUAL, "xyz"))
        assertEquals(1, Diff("", "").xIndex(diffs, 3), "diff_xIndex: Translation on deletion.")
    }

    // Construct the two texts which made up the diff originally.
    private fun diffRebuildTests(diffs: List<Difference>): Array<String> {
        val text = arrayOf("", "")
        for (myDiff in diffs) {
            val editType = myDiff.editType
            if (EditType.INSERT != editType) {
                text[0] += myDiff.text
            }

            if (EditType.DELETE != editType) {
                text[1] += myDiff.text
            }
        }
        return text
    }

    // Private function for quickly building lists of diffs.
    private fun <T> diffList(vararg items: T): MutableList<T> = items.toMutableList()
}
