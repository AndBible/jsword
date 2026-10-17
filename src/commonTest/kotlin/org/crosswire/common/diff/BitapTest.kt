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
class BitapTest {

    /**
     * Exposes the protected alphabet members of [Bitap] to this test. The Java original
     * reached them as a same-package caller, which Kotlin does not allow.
     */
    private class BitapProbe(text: String, pattern: String, loc: Int) : Bitap(text, pattern, loc) {
        fun initAlphabet() = alphabet()

        fun alphabetMap(): MutableMap<Char, Int> = getAlphabet()
    }

    @Test
    fun testAlphabet() {
        // Initialise the bitmasks for Bitap
        var bitmask: MutableMap<Char, Int> = mutableMapOf('a' to 4, 'b' to 2, 'c' to 1)
        var bitap = BitapProbe("", "abc", 0)
        bitap.initAlphabet()
        assertEquals(bitmask, bitap.alphabetMap(), "match_alphabet: Unique.")
        bitmask = mutableMapOf('a' to 37, 'b' to 18, 'c' to 8)
        bitap = BitapProbe("", "abcaba", 0)
        bitap.initAlphabet()
        assertEquals(bitmask, bitap.alphabetMap(), "match_alphabet: Duplicates.")
    }

    @Test
    fun testBitap() {
        // Bitap algorithm
        Bitap.setBalance(0.5f)
        Bitap.setThreshold(0.5f)
        Bitap.setMinLength(100)
        Bitap.setMaxLength(1000)
        assertEquals(5, Bitap("abcdefghijk", "fgh", 5).locate(), "match_bitap: Exact match #1.")
        assertEquals(5, Bitap("abcdefghijk", "fgh", 0).locate(), "match_bitap: Exact match #2.")
        assertEquals(4, Bitap("abcdefghijk", "efxhi", 0).locate(), "match_bitap: Fuzzy match #1.")
        assertEquals(2, Bitap("abcdefghijk", "cdefxyhijk", 5).locate(), "match_bitap: Fuzzy match #2.")
        assertEquals(-1, Bitap("abcdefghijk", "bxy", 1).locate(), "match_bitap: Fuzzy match #3.")
        assertEquals(2, Bitap("123456789xx0", "3456789x0", 2).locate(), "match_bitap: Overflow.")
        Bitap.setThreshold(0.75f)
        assertEquals(4, Bitap("abcdefghijk", "efxyhi", 1).locate(), "match_bitap: Threshold #1.")
        Bitap.setThreshold(0.1f)
        assertEquals(1, Bitap("abcdefghijk", "bcdef", 1).locate(), "match_bitap: Threshold #2.")
        Bitap.setThreshold(0.5f)
        assertEquals(0, Bitap("abcdexyzabcde", "abccde", 3).locate(), "match_bitap: Multiple select #1.")
        assertEquals(8, Bitap("abcdexyzabcde", "abccde", 5).locate(), "match_bitap: Multiple select #2.")
        Bitap.setBalance(0.6f) // Strict location, loose accuracy.
        assertEquals(-1, Bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24).locate(), "match_bitap: Balance test #1.")
        assertEquals(0, Bitap("abcdefghijklmnopqrstuvwxyz", "abcxdxexfgh", 1).locate(), "match_bitap: Balance test #2.")
        Bitap.setBalance(0.4f) // Strict accuracy loose location.
        assertEquals(0, Bitap("abcdefghijklmnopqrstuvwxyz", "abcdefg", 24).locate(), "match_bitap: Balance test #3.")
        assertEquals(-1, Bitap("abcdefghijklmnopqrstuvwxyz", "abcxdxexfgh", 1).locate(), "match_bitap: Balance test #4.")
        Bitap.setBalance(0.5f)
    }

}
