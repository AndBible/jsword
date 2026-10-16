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
package org.crosswire.jsword.book.study;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Characterization of {@link StrongsNumber}: pins the
 * behaviour of the Java classes before they are converted to Kotlin. Expected
 * values were derived by reading the Java. The odd results (a number built from
 * a char and a short is never valid, a string that does not parse still "is a
 * part") are the behaviour and are kept.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class StrongsNumberCharacterizationTest {
    @Test
    public void testGreekNumber() {
        StrongsNumber n = new StrongsNumber("G3056");
        Assert.assertTrue(n.isValid());
        Assert.assertEquals("G3056", n.getStrongsNumber());
        Assert.assertEquals("G3056", n.getFullStrongsNumber());
        Assert.assertEquals("G3056", n.toString());
        Assert.assertTrue(n.isGreek());
        Assert.assertFalse(n.isHebrew());
        Assert.assertFalse(n.isPart());
    }

    @Test
    public void testHebrewNumberIsZeroPadded() {
        StrongsNumber n = new StrongsNumber("H7225");
        Assert.assertTrue(n.isValid());
        Assert.assertEquals("H7225", n.getStrongsNumber());
        Assert.assertTrue(n.isHebrew());
        Assert.assertFalse(n.isGreek());

        StrongsNumber small = new StrongsNumber("h1");
        Assert.assertTrue(small.isValid());
        Assert.assertEquals("H0001", small.getStrongsNumber());
    }

    @Test
    public void testLowerCaseAndLeadingZeros() {
        Assert.assertEquals("G3056", new StrongsNumber("g3056").getStrongsNumber());
        Assert.assertTrue(new StrongsNumber("g3056").isGreek());
        Assert.assertEquals("G3056", new StrongsNumber("G03056").getStrongsNumber());
        Assert.assertEquals("G3056", new StrongsNumber("G0000003056").getStrongsNumber());
    }

    @Test
    public void testPartIsKeptSeparately() {
        StrongsNumber n = new StrongsNumber("G3056!a");
        Assert.assertTrue(n.isValid());
        Assert.assertTrue(n.isPart());
        Assert.assertEquals("G3056", n.getStrongsNumber());
        Assert.assertEquals("G3056a", n.getFullStrongsNumber());

        // A part without the bang is accepted too, a bang with no letters is not a part.
        Assert.assertEquals("G0012abc", new StrongsNumber("G12abc").getFullStrongsNumber());
        StrongsNumber bang = new StrongsNumber("G12!");
        Assert.assertTrue(bang.isValid());
        Assert.assertFalse(bang.isPart());
        Assert.assertEquals("G0012", bang.getFullStrongsNumber());
        // Only a prefix has to match
        Assert.assertEquals("G0012", new StrongsNumber("G12 and more").getStrongsNumber());
    }

    @Test
    public void testInputsThatDoNotParse() {
        for (String input : Arrays.asList("3056", "strong:G3056", "x", "", "Z12", " G12")) {
            StrongsNumber n = new StrongsNumber(input);
            Assert.assertFalse(input, n.isValid());
            // The defaults of a failed parse
            Assert.assertEquals(input, "U9999", n.getStrongsNumber());
            Assert.assertFalse(input, n.isGreek());
            Assert.assertFalse(input, n.isHebrew());
            // part is "" rather than null, so it counts as a part
            Assert.assertTrue(input, n.isPart());
            Assert.assertEquals(input, "U9999", n.getFullStrongsNumber());
        }
    }

    @Test
    public void testLanguageWithoutDigitsOrWithOverflow() {
        for (String input : Arrays.asList("G", "Gabc", "H", "G99999999999")) {
            StrongsNumber n = new StrongsNumber(input);
            Assert.assertFalse(input, n.isValid());
            // NumberFormatException on the digits: the number is 0, the language is kept
            Assert.assertEquals(input, input.charAt(0) + "0000", n.getStrongsNumber());
            Assert.assertEquals(input, input.charAt(0) == 'G', n.isGreek());
        }
    }

    @Test
    public void testRanges() {
        Assert.assertTrue(new StrongsNumber("H8674").isValid());
        Assert.assertFalse(new StrongsNumber("H8675").isValid());
        Assert.assertFalse(new StrongsNumber("H0").isValid());
        Assert.assertTrue(new StrongsNumber("H1").isValid());

        Assert.assertTrue(new StrongsNumber("G1417").isValid());
        Assert.assertFalse(new StrongsNumber("G1418").isValid());
        Assert.assertTrue(new StrongsNumber("G1419").isValid());
        Assert.assertFalse(new StrongsNumber("G2717").isValid());
        Assert.assertTrue(new StrongsNumber("G3202").isValid());
        Assert.assertFalse(new StrongsNumber("G3203").isValid());
        Assert.assertFalse(new StrongsNumber("G3302").isValid());
        Assert.assertTrue(new StrongsNumber("G3303").isValid());
        Assert.assertTrue(new StrongsNumber("G5623").isValid());
        Assert.assertFalse(new StrongsNumber("G5624").isValid());
        Assert.assertFalse(new StrongsNumber("G5999").isValid());
        Assert.assertTrue(new StrongsNumber("G6000").isValid());
        Assert.assertTrue(new StrongsNumber("G9999").isValid());
        Assert.assertFalse(new StrongsNumber("G10000").isValid());
        // An out of range number still formats
        Assert.assertEquals("G1418", new StrongsNumber("G1418").getStrongsNumber());
        Assert.assertEquals("G10000", new StrongsNumber("G10000").getStrongsNumber());
    }

    @Test
    public void testCharShortConstructorIsNeverValid() {
        // valid is still false when the constructor asks isValid(), so the result is always false
        StrongsNumber n = new StrongsNumber('G', (short) 3056);
        Assert.assertFalse(n.isValid());
        Assert.assertEquals("G3056", n.getStrongsNumber());
        Assert.assertTrue(n.isGreek());
        Assert.assertFalse(n.isPart());

        StrongsNumber withPart = new StrongsNumber('H', (short) 7, "b");
        Assert.assertFalse(withPart.isValid());
        Assert.assertTrue(withPart.isPart());
        Assert.assertEquals("H0007", withPart.getStrongsNumber());
        Assert.assertEquals("H0007b", withPart.getFullStrongsNumber());
        Assert.assertTrue(withPart.isHebrew());
    }

    @Test
    public void testEqualsAndHashCodeIgnorePartAndValidity() {
        StrongsNumber a = new StrongsNumber("G3056");
        StrongsNumber b = new StrongsNumber("g03056!x");
        StrongsNumber c = new StrongsNumber('G', (short) 3056);
        Assert.assertEquals(a, a);
        Assert.assertEquals(a, b);
        Assert.assertEquals(a, c);
        Assert.assertEquals(a.hashCode(), b.hashCode());
        // 31 * (31 + 'G') + 3056
        Assert.assertEquals(31 * (31 + 'G') + 3056, a.hashCode());

        Assert.assertNotEquals(a, new StrongsNumber("H3056"));
        Assert.assertNotEquals(a, new StrongsNumber("G3057"));
        Assert.assertNotEquals(a, null);
        Assert.assertNotEquals(a, "G3056");
        // Two inputs that do not parse are equal (U9999)
        Assert.assertEquals(new StrongsNumber("x"), new StrongsNumber(""));
    }
}
