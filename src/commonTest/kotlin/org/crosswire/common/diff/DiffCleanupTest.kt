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
class DiffCleanupTest {

    @Test
    fun testDiffCleanupMerge() {
        // Cleanup a messy diff
        var diffs: MutableList<Difference> = diffList()
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(diffList(), diffs, "DiffCleanup.cleanupMerge: Null case.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "b"), Difference(EditType.INSERT, "c"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(
            diffList(Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "b"), Difference(EditType.INSERT, "c")), diffs, "DiffCleanup.cleanupMerge: No change case.")
        diffs = diffList(
            Difference(EditType.EQUAL, "a"), Difference(EditType.EQUAL, "b"), Difference(EditType.EQUAL, "c"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(diffList(Difference(EditType.EQUAL, "abc")), diffs, "DiffCleanup.cleanupMerge: Merge equalities.")
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.DELETE, "b"), Difference(EditType.DELETE, "c"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(diffList(Difference(EditType.DELETE, "abc")), diffs, "DiffCleanup.cleanupMerge: Merge deletions.")
        diffs = diffList(
            Difference(EditType.INSERT, "a"), Difference(EditType.INSERT, "b"), Difference(EditType.INSERT, "c"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(diffList(Difference(EditType.INSERT, "abc")), diffs, "DiffCleanup.cleanupMerge: Merge insertions.")
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "b"), Difference(EditType.DELETE, "c"), Difference(EditType.INSERT, "d"), Difference(EditType.EQUAL, "e"), Difference(EditType.EQUAL, "f"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "ac"), Difference(EditType.INSERT, "bd"), Difference(EditType.EQUAL, "ef")), diffs, "DiffCleanup.cleanupMerge: Merge interweave.")
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "abc"), Difference(EditType.DELETE, "dc"))
        DiffCleanup.cleanupMerge(diffs)
        assertEquals(
            diffList(Difference(EditType.EQUAL, "a"), Difference(EditType.DELETE, "d"), Difference(EditType.INSERT, "b"), Difference(EditType.EQUAL, "c")), diffs, "DiffCleanup.cleanupMerge: Prefix and suffix detection.")
    }

    @Test
    fun testDiffCleanupSemantic() {
        // Cleanup semantically trivial equalities
        var diffs: MutableList<Difference> = diffList()
        DiffCleanup.cleanupSemantic(diffs)
        assertEquals(diffList(), diffs, "DiffCleanup.cleanupSemantic: Null case.")
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "b"), Difference(EditType.EQUAL, "cd"), Difference(EditType.DELETE, "e"))
        DiffCleanup.cleanupSemantic(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "a"), Difference(EditType.INSERT, "b"), Difference(EditType.EQUAL, "cd"), Difference(EditType.DELETE, "e")), diffs, "DiffCleanup.cleanupSemantic: No elimination.")
        diffs = diffList(
            Difference(EditType.DELETE, "a"), Difference(EditType.EQUAL, "b"), Difference(EditType.DELETE, "c"))
        DiffCleanup.cleanupSemantic(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "abc"), Difference(EditType.INSERT, "b")), diffs, "DiffCleanup.cleanupSemantic: Simple elimination.")
        diffs = diffList(
            Difference(EditType.DELETE, "ab"), Difference(EditType.EQUAL, "cd"), Difference(EditType.DELETE, "e"), Difference(EditType.EQUAL, "f"), Difference(EditType.INSERT, "g"))
        DiffCleanup.cleanupSemantic(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "abcdef"), Difference(EditType.INSERT, "cdfg")), diffs, "DiffCleanup.cleanupSemantic: Backpass elimination.")
    }

    @Test
    fun testDiffCleanupEfficiency() {
        // Cleanup operationally trivial equalities
        DiffCleanup.setEditCost(4)
        var diffs: MutableList<Difference> = diffList()
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(diffList(), diffs, "DiffCleanup.cleanupEfficiency: Null case.")
        diffs = diffList(
            Difference(EditType.DELETE, "ab"), Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "wxyz"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "34"))
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "ab"), Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "wxyz"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "34")), diffs, "DiffCleanup.cleanupEfficiency: No elimination.")
        diffs = diffList(
            Difference(EditType.DELETE, "ab"), Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "xyz"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "34"))
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "abxyzcd"), Difference(EditType.INSERT, "12xyz34")), diffs, "DiffCleanup.cleanupEfficiency: Four-edit elimination.")
        diffs = diffList(
            Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "x"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "34"))
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "xcd"), Difference(EditType.INSERT, "12x34")), diffs, "DiffCleanup.cleanupEfficiency: Three-edit elimination.")
        diffs = diffList(
            Difference(EditType.DELETE, "ab"), Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "xy"), Difference(EditType.INSERT, "34"), Difference(EditType.EQUAL, "z"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "56"))
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "abxyzcd"), Difference(EditType.INSERT, "12xy34z56")), diffs, "DiffCleanup.cleanupEfficiency: Backpass elimination.")
        DiffCleanup.setEditCost(5)
        diffs = diffList(
            Difference(EditType.DELETE, "ab"), Difference(EditType.INSERT, "12"), Difference(EditType.EQUAL, "wxyz"), Difference(EditType.DELETE, "cd"), Difference(EditType.INSERT, "34"))
        DiffCleanup.cleanupEfficiency(diffs)
        assertEquals(
            diffList(Difference(EditType.DELETE, "abwxyzcd"), Difference(EditType.INSERT, "12wxyz34")), diffs, "DiffCleanup.cleanupEfficiency: High cost elimination.")
        DiffCleanup.setEditCost(4)
    }

    // Private function for quickly building lists of diffs.
    private fun diffList(): MutableList<Difference> = mutableListOf()

    // Private function for quickly building lists of diffs.
    private fun <T> diffList(vararg items: T): MutableList<T> = items.toMutableList()
}
