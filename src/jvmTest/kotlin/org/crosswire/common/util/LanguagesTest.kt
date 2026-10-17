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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class LanguagesTest {
    @Test
    fun testBCP47() {
        // Some day there may be a zzz language code and then this test will need to change.
        assertEquals("zzz", Languages.getName("zzz"), "Test for unknown code")
        // When the code is not valid the language name will the code
        assertEquals("zzzz", Languages.getName("zzzz"), "Test for bad code")
        // ur-Deva is defined so it should not return the code. The actual answer
        // is Locale dependent, so we don't test for an exact value
        assertFalse("ur-Deva".equals(Languages.getName("ur-Deva")), "Test for known code")
    }

    @Test
    fun testISO639() {
        // Some day there may be a zzz language code and then this test will need to change.
        assertEquals("zzz", Languages.AllLanguages.getName("zzz"), "Test for unknown code")
        // When the code is not valid the language name will the code
        assertEquals("zzzz", Languages.AllLanguages.getName("zzzz"), "Test for bad code")
        // ur-Deva is defined above but not here, so it should return the code.
        assertEquals("ur-Deva", Languages.AllLanguages.getName("ur-Deva"), "Test for known code")
        // en-US is defined above but not here, so it should return the code.
        assertEquals("en-US", Languages.AllLanguages.getName("en-US"), "Test for known code")
    }

    @Test
    fun testRtoL() {
        // Known means in rtol.txt
        assertTrue(Languages.RtoL.isRtoL("Arab", null), "Test for known RtoL script: Arab  # Arabic")
        assertTrue(Languages.RtoL.isRtoL("Armi", null), "Test for known RtoL script: Armi  # Imperial Aramaic")
        assertTrue(Languages.RtoL.isRtoL("Avst", null), "Test for known RtoL script: Avst  # Avestan")
        assertTrue(Languages.RtoL.isRtoL("Hebr", null), "Test for known RtoL script: Hebr  # Hebrew")
        assertTrue(Languages.RtoL.isRtoL("Hung", null), "Test for known RtoL script: Hung  # Old Hungarian (Hungarian Runic)")
        assertTrue(Languages.RtoL.isRtoL("Lydi", null), "Test for known RtoL script: Lydi  # Lydian")
        assertTrue(Languages.RtoL.isRtoL("Mand", null), "Test for known RtoL script: Mand  # Mandaic, Mandaean")
        assertTrue(Languages.RtoL.isRtoL("Mani", null), "Test for known RtoL script: Mani  # Manichaean")
        assertTrue(Languages.RtoL.isRtoL("Merc", null), "Test for known RtoL script: Merc  # Meroitic Cursive")
        assertTrue(Languages.RtoL.isRtoL("Mero", null), "Test for known RtoL script: Mero  # Meroitic Hieroglyphs")
        assertTrue(Languages.RtoL.isRtoL("Mong", null), "Test for known RtoL script: Mong  # Mongolian")
        assertTrue(Languages.RtoL.isRtoL("Mroo", null), "Test for known RtoL script: Mroo  # Mro, Mru")
        assertTrue(Languages.RtoL.isRtoL("Narb", null), "Test for known RtoL script: Narb  # Old North Arabian (Ancient North Arabian)")
        assertTrue(Languages.RtoL.isRtoL("Nbat", null), "Test for known RtoL script: Nbat  # Nabataean")
        assertTrue(Languages.RtoL.isRtoL("Nkoo", null), "Test for known RtoL script: Nkoo  # N’Ko")
        assertTrue(Languages.RtoL.isRtoL("Orkh", null), "Test for known RtoL script: Orkh  # Old Turkic, Orkhon Runic")
        assertTrue(Languages.RtoL.isRtoL("Palm", null), "Test for known RtoL script: Palm  # Palmyrene")
        assertTrue(Languages.RtoL.isRtoL("Phli", null), "Test for known RtoL script: Phli  # Inscriptional Pahlavi")
        assertTrue(Languages.RtoL.isRtoL("Phlp", null), "Test for known RtoL script: Phlp  # Psalter Pahlavi")
        assertTrue(Languages.RtoL.isRtoL("Phlv", null), "Test for known RtoL script: Phlv  # Book Pahlavi")
        assertTrue(Languages.RtoL.isRtoL("Phnx", null), "Test for known RtoL script: Phnx  # Phoenician")
        assertTrue(Languages.RtoL.isRtoL("Prti", null), "Test for known RtoL script: Prti  # Inscriptional Parthian")
        assertTrue(Languages.RtoL.isRtoL("Samr", null), "Test for known RtoL script: Samr  # Samaritan")
        assertTrue(Languages.RtoL.isRtoL("Sarb", null), "Test for known RtoL script: Sarb  # Old South Arabian")
        assertTrue(Languages.RtoL.isRtoL("Syrc", null), "Test for known RtoL script: Syrc  # Syriac")
        assertTrue(Languages.RtoL.isRtoL("Syre", null), "Test for known RtoL script: Syre  # Syriac (Estrangelo variant)")
        assertTrue(Languages.RtoL.isRtoL("Syrj", null), "Test for known RtoL script: Syrj  # Syriac (Western variant)")
        assertTrue(Languages.RtoL.isRtoL("Syrn", null), "Test for known RtoL script: Syrn  # Syriac (Eastern variant)")
        assertTrue(Languages.RtoL.isRtoL("Tfng", null), "Test for known RtoL script: Tfng  # Tifinagh (Berber)")
        assertTrue(Languages.RtoL.isRtoL("Thaa", null), "Test for known RtoL script: Thaa  # Thaana")

        assertTrue(Languages.RtoL.isRtoL(null, "ar"), "Test for known RtoL lang: ar    # Arabic")
        assertTrue(Languages.RtoL.isRtoL(null, "fa"), "Test for known RtoL lang: fa    # Farsi/Persian")
        assertTrue(Languages.RtoL.isRtoL(null, "he"), "Test for known RtoL lang: he    # Hebrew")
        assertTrue(Languages.RtoL.isRtoL(null, "syr"), "Test for known RtoL lang: syr   # Syriac")
        assertTrue(Languages.RtoL.isRtoL(null, "ur"), "Test for known RtoL lang: ur    # Uighur")
        assertTrue(Languages.RtoL.isRtoL(null, "uig"), "Test for known RtoL lang: uig   # Uighur")

        assertFalse(Languages.RtoL.isRtoL("diddly squat", null), "Test for unknown RtoL script value: diddly-squat")
        assertFalse(Languages.RtoL.isRtoL(null, "diddly squat"), "Test for unknown RtoL lang value: diddly-squat")
        assertFalse(Languages.RtoL.isRtoL("diddly squat", "diddly squat"), "Test for unknown RtoL script and lang value: diddly-squat")

        assertTrue(Languages.RtoL.isRtoL("Arab", "fa"), "Test for known RtoL script and language")
        assertTrue(Languages.RtoL.isRtoL("Arab", "he"), "Test for known RtoL script and language, but bad combo")
        assertTrue(Languages.RtoL.isRtoL("Arab", "zzzzzz"), "Test for known RtoL script and unknown language")
        assertFalse(Languages.RtoL.isRtoL("Xxxx", "fa"), "Test for known RtoL unknown script and known language")

        assertFalse(Languages.RtoL.isRtoL("arab", null), "Test case insensitivity of RtoL script")
        assertFalse(Languages.RtoL.isRtoL(null, "FA"), "Test case insensitivity of RtoL lang")
    }
}
