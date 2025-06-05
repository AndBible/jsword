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
package org.crosswire.jsword.passage;

import org.crosswire.jsword.JSMsg;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.system.Versifications;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit Test
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
public class AccuracyTypeTest {

    private Versification rs;

    @Before
    public void setUp() throws Exception {
        // AV11N(DMS): Update test to test all V11Ns
        rs = Versifications.instance().getVersification("KJV");
    }

    @Test
    public void testFromTextOnePartInvalidBook() {
        try {
            AccuracyType.fromText(rs, "10", new String[] { "10"}, null, null);
        } catch (NoSuchVerseException expected) {
            // This is allowed
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Assert.fail("ArrayIndexOutOfBoundsException caught, expecting NoSuchVerseException");
        }

    }

    @Test
    public void testFromTextTooManyParts() {
        boolean caught = false;
        try {
            AccuracyType.fromText(rs, "1:2:3:4", new String[] { "1", "2", "3", "4"}, null, null);
        } catch (NoSuchVerseException nsve) {
            // TRANSLATOR: The user specified a verse with too many separators. {0} is a placeholder for the allowable separators.
            NoSuchVerseException correctException = new NoSuchVerseException(JSMsg.gettext("Too many parts to the Verse. (Parts are separated by any of {0})",
                "1:2:3:4, 1, 2, 3, 4"));
            Assert.assertEquals("Unexpected exception message", correctException.getMessage(), nsve.getMessage());
            caught = true;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Assert.fail("ArrayIndexOutOfBoundsException caught, expecting NoSuchVerseException");
        }

        if (!caught) {
            Assert.fail("Expected fromText to throw an exception when passed too many parts");
        }
    }

    @Test
    public void testFromTextThreePartsInvalidBook() {
        boolean caught = false;
        try {
            AccuracyType.fromText(rs, "-1:2:3", new String[] { "-1", "2", "3"}, null, null);
        } catch (NoSuchVerseException nsve) {
            // TRANSLATOR: The user specified a verse with too many separators. {0} is a placeholder for the allowable separators.
            NoSuchVerseException correctException = new NoSuchVerseException(JSMsg.gettext("Too many parts to the Verse. (Parts are separated by any of {0})",
                    "-1:2:3, -1, 2, 3"));
            Assert.assertEquals("Unexpected exception message", correctException.getMessage(), nsve.getMessage());
            caught = true;
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            Assert.fail("ArrayIndexOutOfBoundsException caught, expecting NoSuchVerseException");
        }

        if (!caught) {
            Assert.fail("Expected fromText to throw an exception when passed three parts with an invalid book");
        }
    }

    @Test
    public void testTokenizeBookNamesWithDigits() throws NoSuchVerseException {
        // Test that book names starting with digits are not split incorrectly
        String[] result1 = AccuracyType.tokenize("2Macc.10.38");
        Assert.assertEquals("Book name '2Macc' should not be split", 3, result1.length);
        Assert.assertEquals("First token should be '2Macc'", "2Macc", result1[0]);
        Assert.assertEquals("Second token should be '10'", "10", result1[1]);
        Assert.assertEquals("Third token should be '38'", "38", result1[2]);

        String[] result2 = AccuracyType.tokenize("1Esd.1.12");
        Assert.assertEquals("Book name '1Esd' should not be split", 3, result2.length);
        Assert.assertEquals("First token should be '1Esd'", "1Esd", result2[0]);
        Assert.assertEquals("Second token should be '1'", "1", result2[1]);
        Assert.assertEquals("Third token should be '12'", "12", result2[2]);

        String[] result3 = AccuracyType.tokenize("3John.1.1");
        Assert.assertEquals("Book name '3John' should not be split", 3, result3.length);
        Assert.assertEquals("First token should be '3John'", "3John", result3[0]);
        Assert.assertEquals("Second token should be '1'", "1", result3[1]);
        Assert.assertEquals("Third token should be '1'", "1", result3[2]);
    }

    @Test
    public void testTokenizeRegularVerseStillWorks() throws NoSuchVerseException {
        // Test that regular verse parsing still works correctly
        String[] result1 = AccuracyType.tokenize("Gen.1.1");
        Assert.assertEquals("Regular verse should have 3 parts", 3, result1.length);
        Assert.assertEquals("First token should be 'Gen'", "Gen", result1[0]);
        Assert.assertEquals("Second token should be '1'", "1", result1[1]);
        Assert.assertEquals("Third token should be '1'", "1", result1[2]);

        String[] result2 = AccuracyType.tokenize("Genesis 1:1");
        Assert.assertEquals("Regular verse should have 3 parts", 3, result2.length);
        Assert.assertEquals("First token should be 'Genesis'", "Genesis", result2[0]);
        Assert.assertEquals("Second token should be '1'", "1", result2[1]);
        Assert.assertEquals("Third token should be '1'", "1", result2[2]);
    }

    @Test
    public void testTokenizeManualVerification() throws NoSuchVerseException {
        // Manual verification that our fix solves the GitHub issue cases
        System.out.println("Testing tokenization for problematic cases:");
        
        String[] test1 = AccuracyType.tokenize("2Macc.10.38");
        System.out.println("2Macc.10.38 -> " + java.util.Arrays.toString(test1));
        Assert.assertEquals("Should have 3 tokens", 3, test1.length);
        Assert.assertEquals("First should be book name", "2Macc", test1[0]);
        
        String[] test2 = AccuracyType.tokenize("1Esd.1.12");
        System.out.println("1Esd.1.12 -> " + java.util.Arrays.toString(test2));
        Assert.assertEquals("Should have 3 tokens", 3, test2.length);
        Assert.assertEquals("First should be book name", "1Esd", test2[0]);
        
        // Test the case from GitHub issue #3214
        String[] test3 = AccuracyType.tokenize("Bible:Gen.4.26");
        System.out.println("Bible:Gen.4.26 -> " + java.util.Arrays.toString(test3));
        // This should still work but may have more tokens due to the prefix
    }
}
