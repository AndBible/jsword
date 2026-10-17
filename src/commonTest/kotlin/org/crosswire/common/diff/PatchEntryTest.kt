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

/* Test harness for diff_match_patch.java
 *
 * Version 2.2, May 2007
 * If debugging errors, start with the first reported error,
 * subsequent tests often rely on earlier ones.
 */

/**
 * JUnit Test.
 *
 * Based on the LGPL Diff_Match_Patch v1.5 javascript of Neil Fraser, Copyright © 2006<br>
 * <a href="http://neil.fraser.name/software/diff_match_patch/">http://neil.fraser.name/software/diff_match_patch/</a>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class PatchEntryTest {

    /**
     * Exposes the protected setDifferences of [PatchEntry] to this test. The Java original
     * reached it as a same-package caller, which Kotlin does not allow.
     */
    private class PatchEntryProbe : PatchEntry() {
        fun setDifferencesProbe(newDiffs: MutableList<Difference>) = setDifferences(newDiffs)
    }

    @Test
    fun testPatchObj() {
        // Patch Object
        val p = PatchEntryProbe()
        p.sourceStart = 20
        p.targetStart = 21
        p.sourceLength = 18
        p.targetLength = 17
        p.setDifferencesProbe(diffList(
            Difference(EditType.EQUAL, "jump"), Difference(EditType.DELETE, "s"), Difference(EditType.INSERT, "ed"), Difference(EditType.EQUAL, " over "), Difference(EditType.DELETE, "the"), Difference(EditType.INSERT, "a"), Difference(EditType.EQUAL, " laz")))
        assertEquals("jumps over the laz", p.getSourceText(), "Patch: text1.")
        assertEquals("jumped over a laz", p.getTargetText(), "Patch: text2.")
        val strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n"
        assertEquals(strp, p.toString(), "Patch: toString.")
    }

    fun testMatchFromText() {
        val strp = "@@ -21,18 +22,17 @@\n jump\n-s\n+ed\n  over \n-the\n+a\n  laz\n"
        assertEquals(strp, PatchEntry(strp).toString(), "PatchEntry.fromText: #1.")
        assertEquals("@@ -1 +1 @@\n-a\n+b\n", PatchEntry("@@ -1 +1 @@\n-a\n+b\n").toString(), "PatchEntry.fromText: #2.")
        assertEquals("@@ -1,3 +0,0 @@\n-abc\n", PatchEntry("@@ -1,3 +0,0 @@\n-abc\n").toString(), "PatchEntry.fromText: #3.")
        assertEquals("@@ -0,0 +1,3 @@\n+abc\n", PatchEntry("@@ -0,0 +1,3 @@\n+abc\n").toString(), "PatchEntry.fromText: #4.")
        assertEquals("@@ -1,7 +1,6 @@\n foo\n-%0A\n bar\n", PatchEntry("@@ -1,7 +1,6 @@\n foo\n-%0A\n bar\n").toString(), "PatchEntry.fromText: #5.")
        assertEquals("@@ -1,4 +1,3 @@\n foo\n-%0A\n", PatchEntry("@@ -1,4 +1,3 @@\n foo\n-%0A\n").toString(), "PatchEntry.fromText: #6.")
        assertEquals("@@ -1,4 +1,3 @@\n-%0A\n foo\n", PatchEntry("@@ -1,4 +1,3 @@\n-%0A\n foo\n").toString(), "PatchEntry.fromText: #7.")
        assertEquals("@@ -1,3 +1,4 @@\n foo\n+%0A\n", PatchEntry("@@ -1,3 +1,4 @@\n foo\n+%0A\n").toString(), "PatchEntry.fromText: #8.")
        assertEquals("@@ -1,3 +1,4 @@\n+%0A\n foo\n", PatchEntry("@@ -1,3 +1,4 @@\n+%0A\n foo\n").toString(), "PatchEntry.fromText: #9.")
        assertEquals("@@ -1,4 +1,4 @@\n-foo%0A\n+%0Afoo\n", PatchEntry("@@ -1,4 +1,4 @@\n-foo%0A\n+%0Afoo\n").toString(), "PatchEntry.fromText: #10.")
        assertEquals("@@ -1,4 +1,4 @@\n-%0Afoo\n+foo%0A\n", PatchEntry("@@ -1,4 +1,4 @@\n-%0Afoo\n+foo%0A\n").toString(), "PatchEntry.fromText: #11.")
    }

    fun testMatchAddContext() {
        PatchEntry.setMargin(4)
        var p = PatchEntry("@@ -21,4 +21,10 @@\n-jump\n+somersault\n")
        p.addContext("The quick brown fox jumps over the lazy dog.")
        assertEquals("@@ -17,12 +17,18 @@\n fox \n-jump\n+somersault\n s ov\n", p.toString(), "PatchEntry.addContext: Simple case.")

        p = PatchEntry("@@ -21,4 +21,10 @@\n-jump\n+somersault\n")
        p.addContext("The quick brown fox jumps.")
        assertEquals("@@ -17,10 +17,16 @@\n fox \n-jump\n+somersault\n s.\n", p.toString(), "PatchEntry.addContext: Not enough trailing context.")

        p = PatchEntry("@@ -3 +3,2 @@\n-e\n+at\n")
        p.addContext("The quick brown fox jumps.")
        assertEquals("@@ -1,7 +1,8 @@\n Th\n-e\n+at\n  qui\n", p.toString(), "PatchEntry.addContext: Not enough leading context.")

        p = PatchEntry("@@ -3 +3,2 @@\n-e\n+at\n")
        p.addContext("The quick brown fox jumps.  The quick brown fox crashes.")
        assertEquals("@@ -1,27 +1,28 @@\n Th\n-e\n+at\n  quick brown fox jumps. \n", p.toString(), "PatchEntry.addContext: Ambiguity.")
    }

    // Private function for quickly building lists of diffs.
    private fun <T> diffList(vararg items: T): MutableList<T> = items.toMutableList()
}
