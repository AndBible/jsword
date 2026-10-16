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
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of {@link StringUtil}: pins the behaviour of the hand-written
 * split loops (including their quirks) before the class is converted to Kotlin.
 * Expected values were derived by reading the loops, not by observing them.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class StringUtilCharacterizationTest {
    private static final String EMOJI = "a,\uD83D\uDE00,b";

    private static void check(String msg, String[] expected, String[] actual) {
        Assert.assertArrayEquals(msg, expected, actual);
    }

    private static String[] a(String... items) {
        return items;
    }

    @Test
    public void testSplitWhitespace() {
        check("null", a(), StringUtil.split((String) null));
        check("empty", a(), StringUtil.split(""));
        // No whitespace at all: start stays 0 and the whole string is the only part
        check("comma", a(","), StringUtil.split(","));
        check("commas", a(",a,,b,"), StringUtil.split(",a,,b,"));
        check("single", a("a"), StringUtil.split("a"));
        check("padded", a("a", "b"), StringUtil.split("  a  b "));
        check("emoji", a(EMOJI), StringUtil.split(EMOJI));
        check("leading", a("a"), StringUtil.split(" a"));
        // Character.isWhitespace: NO-BREAK SPACE is not whitespace, EM SPACE is
        check("nbsp", a("a\u00A0b"), StringUtil.split("a\u00A0b"));
        check("emspace", a("a", "b"), StringUtil.split("a\u2003b"));
        check("tab", a("a", "b"), StringUtil.split("a\tb"));
    }

    @Test
    public void testSplitWhitespaceMax() {
        check("null", a(), StringUtil.split((String) null, 2));
        check("empty", a(), StringUtil.split("", 2));
        check("comma", a(","), StringUtil.split(",", 2));
        check("single", a("a"), StringUtil.split("a", 2));
        check("padded max 2", a("a", "b"), StringUtil.split("  a  b ", 2));
        check("padded max 1", a("a"), StringUtil.split("  a  b ", 1));
        check("four max 2", a("a", "b"), StringUtil.split("a b c d", 2));
        check("four max 0 is unlimited", a("a", "b", "c", "d"), StringUtil.split("a b c d", 0));
        check("four max -1 is unlimited", a("a", "b", "c", "d"), StringUtil.split("a b c d", -1));
        // With no separator there is one part whatever max is
        check("emoji", a(EMOJI), StringUtil.split(EMOJI, 1));
    }

    @Test
    public void testSplitChar() {
        check("null", a(), StringUtil.split(null, ','));
        check("empty", a(), StringUtil.split("", ','));
        check("only separator", a(), StringUtil.split(",", ','));
        check("commas", a("a", "b"), StringUtil.split(",a,,b,", ','));
        check("single", a("a"), StringUtil.split("a", ','));
        check("no separator", a("  a  b "), StringUtil.split("  a  b ", ','));
        check("emoji", a("a", "\uD83D\uDE00", "b"), StringUtil.split(EMOJI, ','));
        // Quirk: when exactly one part is counted the whole input is returned, separators included
        check("leading separator", a(",a"), StringUtil.split(",a", ','));
        check("trailing separator", a("a,"), StringUtil.split("a,", ','));
        check("two parts", a("a", "b"), StringUtil.split("a.b", '.'));
    }

    @Test
    public void testSplitCharMax() {
        check("null", a(), StringUtil.split(null, ',', 2));
        check("empty", a(), StringUtil.split("", ',', 2));
        check("only separator", a(), StringUtil.split(",", ',', 2));
        check("commas max 2", a("a", "b"), StringUtil.split(",a,,b,", ',', 2));
        check("commas max 1", a("a"), StringUtil.split(",a,,b,", ',', 1));
        check("commas max 0", a("a", "b"), StringUtil.split(",a,,b,", ',', 0));
        check("single", a("a"), StringUtil.split("a", ',', 2));
        check("no separator", a("  a  b "), StringUtil.split("  a  b ", ',', 2));
        check("emoji max 2", a("a", "\uD83D\uDE00"), StringUtil.split(EMOJI, ',', 2));
        check("a.b.c max 2", a("a", "b"), StringUtil.split("a.b.c", '.', 2));
    }

    @Test
    public void testSplitSeparatorSet() {
        check("null", a(), StringUtil.split(null, " ,"));
        check("empty", a(), StringUtil.split("", " ,"));
        check("only separator", a(), StringUtil.split(",", " ,"));
        check("commas", a("a", "b"), StringUtil.split(",a,,b,", " ,"));
        check("single", a("a"), StringUtil.split("a", " ,"));
        check("padded", a("a", "b"), StringUtil.split("  a  b ", " ,"));
        check("emoji", a("a", "\uD83D\uDE00", "b"), StringUtil.split(EMOJI, " ,"));
        // Quirk: one counted part returns the whole input
        check("leading separator", a(",a"), StringUtil.split(",a", " ,"));
        // Delegations
        check("null set splits on whitespace", a("a", "b"), StringUtil.split("a b", (String) null));
        check("one char set uses the char splitter", a("a", "b"), StringUtil.split("a,b", ","));
        check("one char set keeps the char quirk", a(",a"), StringUtil.split(",a", ","));
        check("empty set splits nothing", a("a b"), StringUtil.split("a b", ""));
    }

    @Test
    public void testSplitSeparatorSetMax() {
        check("null", a(), StringUtil.split(null, "::", 2));
        check("empty", a(), StringUtil.split("", "::", 2));
        check("comma", a(","), StringUtil.split(",", "::", 2));
        check("commas", a(",a,,b,"), StringUtil.split(",a,,b,", "::", 2));
        check("single", a("a"), StringUtil.split("a", "::", 2));
        check("padded", a("  a  b "), StringUtil.split("  a  b ", "::", 2));
        check("emoji", a(EMOJI), StringUtil.split(EMOJI, "::", 2));
        // Each ':' is a separator; the 3rd part is dropped, not merged into the 2nd
        check("a::b::c", a("a", "b"), StringUtil.split("a::b::c", "::", 2));
        check("a:b:c", a("a", "b"), StringUtil.split("a:b:c", "::", 2));
        check("a:b:c unlimited", a("a", "b", "c"), StringUtil.split("a:b:c", "::", 0));
    }

    @Test
    public void testSplitAllChar() {
        check("null", a(), StringUtil.splitAll(null, ','));
        check("empty", a(), StringUtil.splitAll("", ','));
        check("only separator", a("", ""), StringUtil.splitAll(",", ','));
        check("commas", a("", "a", "", "b", ""), StringUtil.splitAll(",a,,b,", ','));
        check("single", a("a"), StringUtil.splitAll("a", ','));
        check("no separator", a("  a  b "), StringUtil.splitAll("  a  b ", ','));
        check("emoji", a("a", "\uD83D\uDE00", "b"), StringUtil.splitAll(EMOJI, ','));
        check("a..b.c", a("a", "", "b", "c"), StringUtil.splitAll("a..b.c", '.'));
    }

    @Test
    public void testSplitAllCharMax() {
        check("null", a(), StringUtil.splitAll(null, ',', 2));
        check("empty", a(), StringUtil.splitAll("", ',', 2));
        check("only separator", a("", ""), StringUtil.splitAll(",", ',', 2));
        check("commas max 2", a("", "a"), StringUtil.splitAll(",a,,b,", ',', 2));
        check("commas max 0", a("", "a", "", "b", ""), StringUtil.splitAll(",a,,b,", ',', 0));
        check("single", a("a"), StringUtil.splitAll("a", ',', 2));
        check("no separator", a("  a  b "), StringUtil.splitAll("  a  b ", ',', 2));
        check("emoji max 2", a("a", "\uD83D\uDE00"), StringUtil.splitAll(EMOJI, ',', 2));
        check("a..b.c max 2", a("a", ""), StringUtil.splitAll("a..b.c", '.', 2));
    }

    @Test
    public void testSplitResultIsNotTheSharedConstant() {
        // The empty results are clones, so callers may not corrupt EMPTY_STRING_ARRAY
        Assert.assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.split((String) null));
        Assert.assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.split(null, ','));
        Assert.assertNotSame(StringUtil.EMPTY_STRING_ARRAY, StringUtil.splitAll("", ','));
        Assert.assertEquals(0, StringUtil.EMPTY_STRING_ARRAY.length);
    }

    @Test
    public void testJoin() {
        Assert.assertEquals("a,,3", StringUtil.join(new Object[] {"a", null, 3}, ","));
        Assert.assertNull(StringUtil.join(null, ","));
        Assert.assertEquals("", StringUtil.join(new Object[0], ","));
        Assert.assertEquals("", StringUtil.join(new Object[] {null}, ","));
        Assert.assertEquals("a--b--c", StringUtil.join(new Object[] {"a", "b", "c"}, "--"));
        Assert.assertEquals("abc", StringUtil.join(new Object[] {"a", "b", "c"}, null));
        Assert.assertEquals("a", StringUtil.join(new Object[] {"a"}, ","));
        Assert.assertEquals(",,a", StringUtil.join(new Object[] {null, "", "a"}, ","));
    }

    @Test
    public void testGetInitials() {
        Assert.assertEquals("KJV", StringUtil.getInitials("King James Version"));
        Assert.assertEquals("CoE", StringUtil.getInitials("Church of England"));
        Assert.assertEquals("", StringUtil.getInitials(""));
        Assert.assertEquals("", StringUtil.getInitials(null));
        // A word contributes its first letter, skipping leading non-letters; a word without letters nothing
        Assert.assertEquals("a", StringUtil.getInitials("123 abc"));
        Assert.assertEquals("a", StringUtil.getInitials("1a"));
    }

    @Test
    public void testCreateTitle() {
        Assert.assertEquals("My Variable Name", StringUtil.createTitle("myVariableName"));
        Assert.assertEquals("ABC", StringUtil.createTitle("ABC"));
        Assert.assertEquals("One Two", StringUtil.createTitle("one_two"));
        Assert.assertEquals("", StringUtil.createTitle(""));
        // Underscores at the start are swallowed because the last char was "a space"
        Assert.assertEquals("A", StringUtil.createTitle("__a"));
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(StringUtil.equals(null, null));
        Assert.assertFalse(StringUtil.equals("a", null));
        Assert.assertFalse(StringUtil.equals(null, "a"));
        Assert.assertTrue(StringUtil.equals("a", new String("a")));
        Assert.assertFalse(StringUtil.equals("a", "b"));
    }

    @Test
    public void testIndexOfAny() {
        char[] str = "a,b;c".toCharArray();
        char[] seps = ",;".toCharArray();
        Assert.assertEquals(1, StringUtil.indexOfAny(str, seps, 0));
        Assert.assertEquals(1, StringUtil.indexOfAny(str, seps, 1));
        Assert.assertEquals(3, StringUtil.indexOfAny(str, seps, 2));
        Assert.assertEquals(-1, StringUtil.indexOfAny(str, seps, 4));
        Assert.assertEquals(-1, StringUtil.indexOfAny(str, seps, 5));
        Assert.assertEquals(-1, StringUtil.indexOfAny(str, seps, 99));
        Assert.assertEquals(-1, StringUtil.indexOfAny(str, new char[0], 0));
    }

    @Test
    public void testIndexOfWhitespace() {
        Assert.assertEquals("\n", StringUtil.NEWLINE);
        char[] str = "ab c\td".toCharArray();
        Assert.assertEquals(2, StringUtil.indexOfWhitespace(str, 0));
        Assert.assertEquals(2, StringUtil.indexOfWhitespace(str, 2));
        Assert.assertEquals(4, StringUtil.indexOfWhitespace(str, 3));
        Assert.assertEquals(-1, StringUtil.indexOfWhitespace(str, 5));
        Assert.assertEquals(-1, StringUtil.indexOfWhitespace(str, 6));
        Assert.assertEquals(-1, StringUtil.indexOfWhitespace(str, 99));
        Assert.assertEquals(-1, StringUtil.indexOfWhitespace("a\u00A0b".toCharArray(), 0));
        Assert.assertEquals(1, StringUtil.indexOfWhitespace("a\u2003b".toCharArray(), 0));
    }

}
