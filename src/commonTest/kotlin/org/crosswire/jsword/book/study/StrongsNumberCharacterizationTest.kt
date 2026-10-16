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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.study

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Characterization of [StrongsNumber]: pins the
 * behaviour of the Java classes before they are converted to Kotlin. Expected
 * values were derived by reading the Java. The odd results (a number built from
 * a char and a short is never valid, a string that does not parse still "is a
 * part") are the behaviour and are kept.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class StrongsNumberCharacterizationTest {
    @Test
    fun testGreekNumber() {
        val n = StrongsNumber("G3056")
        assertTrue(n.isValid())
        assertEquals("G3056", n.getStrongsNumber())
        assertEquals("G3056", n.fullStrongsNumber)
        assertEquals("G3056", n.toString())
        assertTrue(n.isGreek)
        assertFalse(n.isHebrew)
        assertFalse(n.isPart)
    }

    @Test
    fun testHebrewNumberIsZeroPadded() {
        val n = StrongsNumber("H7225")
        assertTrue(n.isValid())
        assertEquals("H7225", n.getStrongsNumber())
        assertTrue(n.isHebrew)
        assertFalse(n.isGreek)

        val small = StrongsNumber("h1")
        assertTrue(small.isValid())
        assertEquals("H0001", small.getStrongsNumber())
    }

    @Test
    fun testLowerCaseAndLeadingZeros() {
        assertEquals("G3056", StrongsNumber("g3056").getStrongsNumber())
        assertTrue(StrongsNumber("g3056").isGreek)
        assertEquals("G3056", StrongsNumber("G03056").getStrongsNumber())
        assertEquals("G3056", StrongsNumber("G0000003056").getStrongsNumber())
    }

    @Test
    fun testPartIsKeptSeparately() {
        val n = StrongsNumber("G3056!a")
        assertTrue(n.isValid())
        assertTrue(n.isPart)
        assertEquals("G3056", n.getStrongsNumber())
        assertEquals("G3056a", n.fullStrongsNumber)

        // A part without the bang is accepted too, a bang with no letters is not a part.
        assertEquals("G0012abc", StrongsNumber("G12abc").fullStrongsNumber)
        val bang = StrongsNumber("G12!")
        assertTrue(bang.isValid())
        assertFalse(bang.isPart)
        assertEquals("G0012", bang.fullStrongsNumber)
        // Only a prefix has to match
        assertEquals("G0012", StrongsNumber("G12 and more").getStrongsNumber())
    }

    @Test
    fun testInputsThatDoNotParse() {
        for (input in listOf("3056", "strong:G3056", "x", "", "Z12", " G12")) {
            val n = StrongsNumber(input)
            assertFalse(n.isValid(), input)
            // The defaults of a failed parse
            assertEquals("U9999", n.getStrongsNumber(), input)
            assertFalse(n.isGreek, input)
            assertFalse(n.isHebrew, input)
            // part is "" rather than null, so it counts as a part
            assertTrue(n.isPart, input)
            assertEquals("U9999", n.fullStrongsNumber, input)
        }
    }

    @Test
    fun testLanguageWithoutDigitsOrWithOverflow() {
        for (input in listOf("G", "Gabc", "H", "G99999999999")) {
            val n = StrongsNumber(input)
            assertFalse(n.isValid(), input)
            // NumberFormatException on the digits: the number is 0, the language is kept
            assertEquals(input[0] + "0000", n.getStrongsNumber(), input)
            assertEquals(input[0] == 'G', n.isGreek, input)
        }
    }

    @Test
    fun testRanges() {
        assertTrue(StrongsNumber("H8674").isValid())
        assertFalse(StrongsNumber("H8675").isValid())
        assertFalse(StrongsNumber("H0").isValid())
        assertTrue(StrongsNumber("H1").isValid())

        assertTrue(StrongsNumber("G1417").isValid())
        assertFalse(StrongsNumber("G1418").isValid())
        assertTrue(StrongsNumber("G1419").isValid())
        assertFalse(StrongsNumber("G2717").isValid())
        assertTrue(StrongsNumber("G3202").isValid())
        assertFalse(StrongsNumber("G3203").isValid())
        assertFalse(StrongsNumber("G3302").isValid())
        assertTrue(StrongsNumber("G3303").isValid())
        assertTrue(StrongsNumber("G5623").isValid())
        assertFalse(StrongsNumber("G5624").isValid())
        assertFalse(StrongsNumber("G5999").isValid())
        assertTrue(StrongsNumber("G6000").isValid())
        assertTrue(StrongsNumber("G9999").isValid())
        assertFalse(StrongsNumber("G10000").isValid())
        // An out of range number still formats
        assertEquals("G1418", StrongsNumber("G1418").getStrongsNumber())
        assertEquals("G10000", StrongsNumber("G10000").getStrongsNumber())
    }

    @Test
    fun testCharShortConstructorIsNeverValid() {
        // valid is still false when the constructor asks isValid(), so the result is always false
        val n = StrongsNumber('G', 3056.toShort())
        assertFalse(n.isValid())
        assertEquals("G3056", n.getStrongsNumber())
        assertTrue(n.isGreek)
        assertFalse(n.isPart)

        val withPart = StrongsNumber('H', 7.toShort(), "b")
        assertFalse(withPart.isValid())
        assertTrue(withPart.isPart)
        assertEquals("H0007", withPart.getStrongsNumber())
        assertEquals("H0007b", withPart.fullStrongsNumber)
        assertTrue(withPart.isHebrew)
    }

    @Test
    fun testEqualsAndHashCodeIgnorePartAndValidity() {
        val a = StrongsNumber("G3056")
        val b = StrongsNumber("g03056!x")
        val c = StrongsNumber('G', 3056.toShort())
        assertEquals(a, a)
        assertEquals(a, b)
        assertEquals(a, c)
        assertEquals(a.hashCode(), b.hashCode())
        // 31 * (31 + 'G') + 3056
        assertEquals(31 * (31 + 'G'.code) + 3056, a.hashCode())

        assertNotEquals(a, StrongsNumber("H3056"))
        assertNotEquals(a, StrongsNumber("G3057"))
        assertNotEquals<Any?>(a, null)
        assertNotEquals<Any?>(a, "G3056")
        // Two inputs that do not parse are equal (U9999)
        assertEquals(StrongsNumber("x"), StrongsNumber(""))
    }
}
