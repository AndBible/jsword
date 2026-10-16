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
package org.crosswire.common.crypt

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * JUnit tests for the Sapphire cipher, including the personalize() key derivation function.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class SapphireTest {

    /**
     * Test that license-format keys are correctly transformed to internal cipher keys.
     * Uses a synthetic test key (not a real commercial license).
     */
    @Test
    fun testPersonalizeLicenseKey() {
        // Synthetic test key generated for unit testing
        // License key format: SALT-SEG1-SEG2-SEG3-CHECKSUM
        val licenseKey = "test-VvUj-UeY2-Yg11-nKAK"
        val expectedInternal = "abcd-efgh-ijkl"

        val result = callPersonalize(licenseKey.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(
            expectedInternal, actual, "License key should be transformed to internal key")
    }

    /**
     * Test that simple keys (not in license format) pass through unchanged.
     * This ensures backward compatibility with existing modules.
     */
    @Test
    fun testPersonalizeSimpleKeyPassthrough() {
        val simpleKey = "mysimplekey"

        val result = callPersonalize(simpleKey.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(simpleKey, actual, "Simple key should pass through unchanged")
    }

    /**
     * Test that keys with wrong number of segments pass through unchanged.
     */
    @Test
    fun testPersonalizeWrongSegmentCount() {
        // Only 3 segments instead of 5
        val key = "abc-def-ghi"

        val result = callPersonalize(key.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(
            key, actual, "Key with wrong segment count should pass through unchanged")
    }

    /**
     * Test that keys with invalid checksum pass through unchanged.
     */
    @Test
    fun testPersonalizeInvalidChecksum() {
        // Valid format but wrong checksum (last segment)
        val key = "test-VvUj-UeY2-Yg11-XXXX"

        val result = callPersonalize(key.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(
            key, actual, "Key with invalid checksum should pass through unchanged")
    }

    /**
     * Test that empty key is handled gracefully.
     */
    @Test
    fun testPersonalizeEmptyKey() {
        val emptyKey = ""

        val result = callPersonalize(emptyKey.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(emptyKey, actual, "Empty key should pass through unchanged")
    }

    /**
     * Test that internal keys (already transformed) pass through unchanged.
     * This handles the case where someone directly provides the internal key.
     */
    @Test
    fun testPersonalizeInternalKeyPassthrough() {
        // This is an internal key format (output of personalize)
        val internalKey = "abcd-efgh-ijkl"

        val result = callPersonalize(internalKey.toByteArray(Charsets.ISO_8859_1))
        val actual = String(result, Charsets.ISO_8859_1)

        assertEquals(internalKey, actual, "Internal key should pass through unchanged")
    }

    /**
     * Test that cipher with license key produces same result as cipher with internal key.
     */
    @Test
    fun testCipherWithLicenseKeyEquivalence() {
        val licenseKey = "test-VvUj-UeY2-Yg11-nKAK"
        val internalKey = "abcd-efgh-ijkl"

        val testData = "Test data for encryption".toByteArray(Charsets.UTF_8)

        // Encrypt with license key
        val cipher1 = Sapphire(licenseKey.toByteArray(Charsets.ISO_8859_1))
        val result1 = ByteArray(testData.size)
        for (i in testData.indices) {
            result1[i] = cipher1.cipher(testData[i])
        }

        // Encrypt with internal key (what license key transforms to)
        val cipher2 = Sapphire(internalKey.toByteArray(Charsets.ISO_8859_1))
        val result2 = ByteArray(testData.size)
        for (i in testData.indices) {
            result2[i] = cipher2.cipher(testData[i])
        }

        assertContentEquals(
            result1, result2, "Cipher with license key should produce same result as cipher with internal key")
    }

    /**
     * Helper method to call the private personalize method via reflection.
     */
    private fun callPersonalize(key: ByteArray): ByteArray {
        val method = Sapphire::class.java.getDeclaredMethod("personalize", ByteArray::class.java)
            .apply { isAccessible = true }
        return method.invoke(null, key) as ByteArray
    }
}
