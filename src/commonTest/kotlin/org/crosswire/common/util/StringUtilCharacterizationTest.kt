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
package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization of [StringUtil]: pins the behaviour of the hand-written
 * split loops (including their quirks) before the class is converted to Kotlin.
 * Expected values were derived by reading the loops, not by observing them.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class StringUtilCharacterizationTest {

    private fun check(msg: String, expected: Array<String>, actual: Array<String>) {
        assertContentEquals(expected, actual, msg)
    }

    private fun a(vararg items: String): Array<String> = arrayOf(*items)

    @Test
    fun testSplitWhitespace() {
        check("null", a(), StringUtil.split(null))
        check("empty", a(), StringUtil.split(""))
        // No whitespace at all: start stays 0 and the whole string is the only part
        check("comma", a(","), StringUtil.split(","))
        check("commas", a(",a,,b,"), StringUtil.split(",a,,b,"))
        check("single", a("a"), StringUtil.split("a"))
        check("padded", a("a", "b"), StringUtil.split("  a  b "))
        check("emoji", a(EMOJI), StringUtil.split(EMOJI))
        check("leading", a("a"), StringUtil.split(" a"))
        // Character.isWhitespace: NO-BREAK SPACE is not whitespace, EM SPACE is
        check("nbsp", a("a b"), StringUtil.split("a b"))
        check("emspace", a("a", "b"), StringUtil.split("a b"))
        check("tab", a("a", "b"), StringUtil.split("a\tb"))
    }

    @Test
    fun testSplitWhitespaceMax() {
        check("null", a(), StringUtil.split(null, 2))
        check("empty", a(), StringUtil.split("", 2))
        check("comma", a(","), StringUtil.split(",", 2))
        check("single", a("a"), StringUtil.split("a", 2))
        check("padded max 2", a("a", "b"), StringUtil.split("  a  b ", 2))
        check("padded max 1", a("a"), StringUtil.split("  a  b ", 1))
        check("four max 2", a("a", "b"), StringUtil.split("a b c d", 2))
        check("four max 0 is unlimited", a("a", "b", "c", "d"), StringUtil.split("a b c d", 0))
        check("four max -1 is unlimited", a("a", "b", "c", "d"), StringUtil.split("a b c d", -1))
        // With no separator there is one part whatever max is
        check("emoji", a(EMOJI), StringUtil.split(EMOJI, 1))
    }

    @Test
    fun testSplitChar() {
        check("null", a(), StringUtil.split(null, ','))
        check("empty", a(), StringUtil.split("", ','))
        check("only separator", a(), StringUtil.split(",", ','))
        check("commas", a("a", "b"), StringUtil.split(",a,,b,", ','))
        check("single", a("a"), StringUtil.split("a", ','))
        check("no separator", a("  a  b "), StringUtil.split("  a  b ", ','))
        check("emoji", a("a", "😀", "b"), StringUtil.split(EMOJI, ','))
        // Quirk: when exactly one part is counted the whole input is returned, separators included
        check("leading separator", a(",a"), StringUtil.split(",a", ','))
        check("trailing separator", a("a,"), StringUtil.split("a,", ','))
        check("two parts", a("a", "b"), StringUtil.split("a.b", '.'))
    }

    @Test
    fun testSplitCharMax() {
        check("null", a(), StringUtil.split(null, ',', 2))
        check("empty", a(), StringUtil.split("", ',', 2))
        check("only separator", a(), StringUtil.split(",", ',', 2))
        check("commas max 2", a("a", "b"), StringUtil.split(",a,,b,", ',', 2))
        check("commas max 1", a("a"), StringUtil.split(",a,,b,", ',', 1))
        check("commas max 0", a("a", "b"), StringUtil.split(",a,,b,", ',', 0))
        check("single", a("a"), StringUtil.split("a", ',', 2))
        check("no separator", a("  a  b "), StringUtil.split("  a  b ", ',', 2))
        check("emoji max 2", a("a", "😀"), StringUtil.split(EMOJI, ',', 2))
        check("a.b.c max 2", a("a", "b"), StringUtil.split("a.b.c", '.', 2))
    }

    @Test
    fun testSplitSeparatorSet() {
        check("null", a(), StringUtil.split(null, " ,"))
        check("empty", a(), StringUtil.split("", " ,"))
        check("only separator", a(), StringUtil.split(",", " ,"))
        check("commas", a("a", "b"), StringUtil.split(",a,,b,", " ,"))
        check("single", a("a"), StringUtil.split("a", " ,"))
        check("padded", a("a", "b"), StringUtil.split("  a  b ", " ,"))
        check("emoji", a("a", "😀", "b"), StringUtil.split(EMOJI, " ,"))
        // Quirk: one counted part returns the whole input
        check("leading separator", a(",a"), StringUtil.split(",a", " ,"))
        // Delegations
        check("null set splits on whitespace", a("a", "b"), StringUtil.split("a b", null))
        check("one char set uses the char splitter", a("a", "b"), StringUtil.split("a,b", ","))
        check("one char set keeps the char quirk", a(",a"), StringUtil.split(",a", ","))
        check("empty set splits nothing", a("a b"), StringUtil.split("a b", ""))
    }

    @Test
    fun testSplitSeparatorSetMax() {
        check("null", a(), StringUtil.split(null, "::", 2))
        check("empty", a(), StringUtil.split("", "::", 2))
        check("comma", a(","), StringUtil.split(",", "::", 2))
        check("commas", a(",a,,b,"), StringUtil.split(",a,,b,", "::", 2))
        check("single", a("a"), StringUtil.split("a", "::", 2))
        check("padded", a("  a  b "), StringUtil.split("  a  b ", "::", 2))
        check("emoji", a(EMOJI), StringUtil.split(EMOJI, "::", 2))
        // Each ':' is a separator; the 3rd part is dropped, not merged into the 2nd
        check("a::b::c", a("a", "b"), StringUtil.split("a::b::c", "::", 2))
        check("a:b:c", a("a", "b"), StringUtil.split("a:b:c", "::", 2))
        check("a:b:c unlimited", a("a", "b", "c"), StringUtil.split("a:b:c", "::", 0))
    }

    @Test
    fun testSplitAllChar() {
        check("null", a(), StringUtil.splitAll(null, ','))
        check("empty", a(), StringUtil.splitAll("", ','))
        check("only separator", a("", ""), StringUtil.splitAll(",", ','))
        check("commas", a("", "a", "", "b", ""), StringUtil.splitAll(",a,,b,", ','))
        check("single", a("a"), StringUtil.splitAll("a", ','))
        check("no separator", a("  a  b "), StringUtil.splitAll("  a  b ", ','))
        check("emoji", a("a", "😀", "b"), StringUtil.splitAll(EMOJI, ','))
        check("a..b.c", a("a", "", "b", "c"), StringUtil.splitAll("a..b.c", '.'))
    }

    @Test
    fun testSplitAllCharMax() {
        check("null", a(), StringUtil.splitAll(null, ',', 2))
        check("empty", a(), StringUtil.splitAll("", ',', 2))
        check("only separator", a("", ""), StringUtil.splitAll(",", ',', 2))
        check("commas max 2", a("", "a"), StringUtil.splitAll(",a,,b,", ',', 2))
        check("commas max 0", a("", "a", "", "b", ""), StringUtil.splitAll(",a,,b,", ',', 0))
        check("single", a("a"), StringUtil.splitAll("a", ',', 2))
        check("no separator", a("  a  b "), StringUtil.splitAll("  a  b ", ',', 2))
        check("emoji max 2", a("a", "😀"), StringUtil.splitAll(EMOJI, ',', 2))
        check("a..b.c max 2", a("a", ""), StringUtil.splitAll("a..b.c", '.', 2))
    }

    @Test
    fun testSplitResultIsNotTheSharedConstant() {
        // The empty results are clones, so callers may not corrupt EMPTY_STRING_ARRAY
        assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.split(null))
        assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.split(null, ','))
        assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.splitAll("", ','))
        assertEquals(0, StringUtil.EMPTY_STRING_ARRAY.size)
    }

    @Test
    fun testJoin() {
        assertEquals("a,,3", StringUtil.join(arrayOf<Any?>("a", null, 3), ","))
        assertNull(StringUtil.join(null, ","))
        assertEquals("", StringUtil.join(arrayOf<Any?>(), ","))
        assertEquals("", StringUtil.join(arrayOf<Any?>(null), ","))
        assertEquals("a--b--c", StringUtil.join(arrayOf<Any?>("a", "b", "c"), "--"))
        assertEquals("abc", StringUtil.join(arrayOf<Any?>("a", "b", "c"), null))
        assertEquals("a", StringUtil.join(arrayOf<Any?>("a"), ","))
        assertEquals(",,a", StringUtil.join(arrayOf<Any?>(null, "", "a"), ","))
    }

    @Test
    fun testGetInitials() {
        assertEquals("KJV", StringUtil.getInitials("King James Version"))
        assertEquals("CoE", StringUtil.getInitials("Church of England"))
        assertEquals("", StringUtil.getInitials(""))
        assertEquals("", StringUtil.getInitials(null))
        // A word contributes its first letter, skipping leading non-letters; a word without letters nothing
        assertEquals("a", StringUtil.getInitials("123 abc"))
        assertEquals("a", StringUtil.getInitials("1a"))
    }

    @Test
    fun testCreateTitle() {
        assertEquals("My Variable Name", StringUtil.createTitle("myVariableName"))
        assertEquals("ABC", StringUtil.createTitle("ABC"))
        assertEquals("One Two", StringUtil.createTitle("one_two"))
        assertEquals("", StringUtil.createTitle(""))
        // Underscores at the start are swallowed because the last char was "a space"
        assertEquals("A", StringUtil.createTitle("__a"))
    }

    @Test
    fun testEquals() {
        assertTrue(StringUtil.equals(null, null))
        assertFalse(StringUtil.equals("a", null))
        assertFalse(StringUtil.equals(null, "a"))
        // A separate instance with the same content
        assertTrue(StringUtil.equals("a", buildString { append("a") }))
        assertFalse(StringUtil.equals("a", "b"))
    }

    @Test
    fun testIndexOfAny() {
        val str = "a,b;c".toCharArray()
        val seps = ",;".toCharArray()
        assertEquals(1, StringUtil.indexOfAny(str, seps, 0))
        assertEquals(1, StringUtil.indexOfAny(str, seps, 1))
        assertEquals(3, StringUtil.indexOfAny(str, seps, 2))
        assertEquals(-1, StringUtil.indexOfAny(str, seps, 4))
        assertEquals(-1, StringUtil.indexOfAny(str, seps, 5))
        assertEquals(-1, StringUtil.indexOfAny(str, seps, 99))
        assertEquals(-1, StringUtil.indexOfAny(str, CharArray(0), 0))
    }

    @Test
    fun testIndexOfWhitespace() {
        assertEquals("\n", StringUtil.NEWLINE)
        val str = "ab c\td".toCharArray()
        assertEquals(2, StringUtil.indexOfWhitespace(str, 0))
        assertEquals(2, StringUtil.indexOfWhitespace(str, 2))
        assertEquals(4, StringUtil.indexOfWhitespace(str, 3))
        assertEquals(-1, StringUtil.indexOfWhitespace(str, 5))
        assertEquals(-1, StringUtil.indexOfWhitespace(str, 6))
        assertEquals(-1, StringUtil.indexOfWhitespace(str, 99))
        assertEquals(-1, StringUtil.indexOfWhitespace("a b".toCharArray(), 0))
        assertEquals(1, StringUtil.indexOfWhitespace("a b".toCharArray(), 0))
    }

    private companion object {
        const val EMOJI = "a,😀,b"
    }
}
