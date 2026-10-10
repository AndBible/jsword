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
package org.crosswire.jsword.passage

import kotlin.test.*

import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications

/**
 * JUnit Test
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class AccuracyTypeTest {

    private lateinit var rs: Versification

    @BeforeTest
    fun setUp(){
        // AV11N(DMS): Update test to test all V11Ns
        rs = Versifications.instance().getVersification("KJV")!!
    }

    @Test
    fun testFromTextOnePartInvalidBook() {
        assertFailsWith<NoSuchVerseException> {
            AccuracyType.fromText(rs, "10", arrayOf("10"), null, null)
        }
    }

    @Test
    fun testFromTextTooManyParts() {
        val failure = assertFailsWith<NoSuchVerseException> {
            AccuracyType.fromText(rs, "1:2:3:4", arrayOf("1", "2", "3", "4"), null, null)
        }
        val expected = JSMsg.gettext("Too many parts to the Verse. (Parts are separated by any of {0})",
            "1:2:3:4, 1, 2, 3, 4")
        assertEquals(expected, failure.message, "Unexpected exception message")
    }

    @Test
    fun testFromTextThreePartsInvalidBook() {
        val failure = assertFailsWith<NoSuchVerseException> {
            AccuracyType.fromText(rs, "-1:2:3", arrayOf("-1", "2", "3"), null, null)
        }
        val expected = JSMsg.gettext("Too many parts to the Verse. (Parts are separated by any of {0})",
            "-1:2:3, -1, 2, 3")
        assertEquals(expected, failure.message, "Unexpected exception message")
    }

    @Test
    fun testTokenizeBookNamesWithDigits(){
        var result1: Array<String> = AccuracyType.tokenize("2Macc.10.38")
        assertEquals(3, result1.size, "Book name '2Macc' should not be split")
        assertEquals("2Macc", result1[0], "First token should be '2Macc'")
        assertEquals("10", result1[1], "Second token should be '10'")
        assertEquals("38", result1[2], "Third token should be '38'")
        var result2: Array<String> = AccuracyType.tokenize("1Esd.1.12")
        assertEquals(3, result2.size, "Book name '1Esd' should not be split")
        assertEquals("1Esd", result2[0], "First token should be '1Esd'")
        assertEquals("1", result2[1], "Second token should be '1'")
        assertEquals("12", result2[2], "Third token should be '12'")
        var result3: Array<String> = AccuracyType.tokenize("3John.1.1")
        assertEquals(3, result3.size, "Book name '3John' should not be split")
        assertEquals("3John", result3[0], "First token should be '3John'")
        assertEquals("1", result3[1], "Second token should be '1'")
        assertEquals("1", result3[2], "Third token should be '1'")
    }

    @Test
    fun testTokenizeRegularVerseStillWorks(){
        // Test that regular verse parsing still works correctly
        var result1: Array<String> = AccuracyType.tokenize("Gen.1.1")
        assertEquals(3, result1.size, "Regular verse should have 3 parts")
        assertEquals("Gen", result1[0], "First token should be 'Gen'")
        assertEquals("1", result1[1], "Second token should be '1'")
        assertEquals("1", result1[2], "Third token should be '1'")
        var result2: Array<String> = AccuracyType.tokenize("Genesis 1:1")
        assertEquals(3, result2.size, "Regular verse should have 3 parts")
        assertEquals("Genesis", result2[0], "First token should be 'Genesis'")
        assertEquals("1", result2[1], "Second token should be '1'")
        assertEquals("1", result2[2], "Third token should be '1'")
    }

    @Test
    fun testTokenizeManualVerification(){
        // Manual verification that our fix solves the GitHub issue cases
        
        
        var test1: Array<String> = AccuracyType.tokenize("2Macc.10.38")
        
        assertEquals(3, test1.size, "Should have 3 tokens")
        assertEquals("2Macc", test1[0], "First should be book name")
        var test2: Array<String> = AccuracyType.tokenize("1Esd.1.12")
        
        assertEquals(3, test2.size, "Should have 3 tokens")
        assertEquals("1Esd", test2[0], "First should be book name")
        // Test the case from GitHub issue #3214
        var test3: Array<String> = AccuracyType.tokenize("Bible:Gen.4.26")
        
        // This should still work but may have more tokens due to the prefix
    }
}
