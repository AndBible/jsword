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
package org.crosswire.common.crypt;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

/**
 * JUnit tests for the Sapphire cipher, including the personalize() key derivation function.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class SapphireTest {

    /**
     * Test that license-format keys are correctly transformed to internal cipher keys.
     * Uses a synthetic test key (not a real commercial license).
     */
    @Test
    public void testPersonalizeLicenseKey() throws Exception {
        // Synthetic test key generated for unit testing
        // License key format: SALT-SEG1-SEG2-SEG3-CHECKSUM
        String licenseKey = "test-VvUj-UeY2-Yg11-nKAK";
        String expectedInternal = "abcd-efgh-ijkl";

        byte[] result = callPersonalize(licenseKey.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("License key should be transformed to internal key",
                expectedInternal, actual);
    }

    /**
     * Test that simple keys (not in license format) pass through unchanged.
     * This ensures backward compatibility with existing modules.
     */
    @Test
    public void testPersonalizeSimpleKeyPassthrough() throws Exception {
        String simpleKey = "mysimplekey";

        byte[] result = callPersonalize(simpleKey.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("Simple key should pass through unchanged", simpleKey, actual);
    }

    /**
     * Test that keys with wrong number of segments pass through unchanged.
     */
    @Test
    public void testPersonalizeWrongSegmentCount() throws Exception {
        // Only 3 segments instead of 5
        String key = "abc-def-ghi";

        byte[] result = callPersonalize(key.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("Key with wrong segment count should pass through unchanged",
                key, actual);
    }

    /**
     * Test that keys with invalid checksum pass through unchanged.
     */
    @Test
    public void testPersonalizeInvalidChecksum() throws Exception {
        // Valid format but wrong checksum (last segment)
        String key = "test-VvUj-UeY2-Yg11-XXXX";

        byte[] result = callPersonalize(key.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("Key with invalid checksum should pass through unchanged",
                key, actual);
    }

    /**
     * Test that empty key is handled gracefully.
     */
    @Test
    public void testPersonalizeEmptyKey() throws Exception {
        String emptyKey = "";

        byte[] result = callPersonalize(emptyKey.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("Empty key should pass through unchanged", emptyKey, actual);
    }

    /**
     * Test that internal keys (already transformed) pass through unchanged.
     * This handles the case where someone directly provides the internal key.
     */
    @Test
    public void testPersonalizeInternalKeyPassthrough() throws Exception {
        // This is an internal key format (output of personalize)
        String internalKey = "abcd-efgh-ijkl";

        byte[] result = callPersonalize(internalKey.getBytes(StandardCharsets.ISO_8859_1));
        String actual = new String(result, StandardCharsets.ISO_8859_1);

        Assert.assertEquals("Internal key should pass through unchanged", internalKey, actual);
    }

    /**
     * Test that cipher with license key produces same result as cipher with internal key.
     */
    @Test
    public void testCipherWithLicenseKeyEquivalence() {
        String licenseKey = "test-VvUj-UeY2-Yg11-nKAK";
        String internalKey = "abcd-efgh-ijkl";

        byte[] testData = "Test data for encryption".getBytes(StandardCharsets.UTF_8);

        // Encrypt with license key
        Sapphire cipher1 = new Sapphire(licenseKey.getBytes(StandardCharsets.ISO_8859_1));
        byte[] result1 = new byte[testData.length];
        for (int i = 0; i < testData.length; i++) {
            result1[i] = cipher1.cipher(testData[i]);
        }

        // Encrypt with internal key (what license key transforms to)
        Sapphire cipher2 = new Sapphire(internalKey.getBytes(StandardCharsets.ISO_8859_1));
        byte[] result2 = new byte[testData.length];
        for (int i = 0; i < testData.length; i++) {
            result2[i] = cipher2.cipher(testData[i]);
        }

        Assert.assertArrayEquals(
                "Cipher with license key should produce same result as cipher with internal key",
                result1, result2);
    }

    /**
     * Helper method to call the private personalize method via reflection.
     */
    private byte[] callPersonalize(byte[] key) throws Exception {
        Method method = Sapphire.class.getDeclaredMethod("personalize", byte[].class);
        method.setAccessible(true);
        return (byte[]) method.invoke(null, key);
    }
}
