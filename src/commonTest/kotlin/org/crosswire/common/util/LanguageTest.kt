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
class LanguageTest {
    @Test
    fun testCanonical() {
        var lang = Language("en")
        assertEquals("en", lang.code, "Test for LL.getCode")
        assertEquals(null, lang.script, "Test for LL.getScript")
        assertEquals(null, lang.country, "Test for LL.getCountry")
        lang = Language("en-Latn")
        assertEquals("en", lang.code, "Test for LL-SSSS.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS.getScript")
        assertEquals(null, lang.country, "Test for LL-SSSS.getCountry")
        lang = Language("en-US")
        assertEquals("en", lang.code, "Test for LL-CC.getCode")
        assertEquals(null, lang.script, "Test for LL-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-CC.getCountry")
        lang = Language("en-Latn-US")
        assertEquals("en", lang.code, "Test for LL-SSSS-CC.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-SSSS-CC.getCountry")
        lang = Language("en-US-Latn")
        assertEquals("en", lang.code, "Test for LL-CC-SSSS.getCode")
        assertEquals(null, lang.script, "Test for LL-CC-SSSS.getScript")
        assertEquals("US", lang.country, "Test for LL-CC-SSSS.getCountry")
    }

    @Test
    fun testNonCanonical() {
        var lang = Language("EN")
        assertEquals("en", lang.code, "Test for LL.getCode")
        assertEquals(null, lang.script, "Test for LL.getScript")
        assertEquals(null, lang.country, "Test for LL.getCountry")
        lang = Language("EN-latn")
        assertEquals("en", lang.code, "Test for LL-SSSS.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS.getScript")
        assertEquals(null, lang.country, "Test for LL-SSSS.getCountry")
        lang = Language("EN-us")
        assertEquals("en", lang.code, "Test for LL-CC.getCode")
        assertEquals(null, lang.script, "Test for LL-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-CC.getCountry")
        lang = Language("EN-latn-us")
        assertEquals("en", lang.code, "Test for LL-SSSS-CC.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-SSSS-CC.getCountry")
        lang = Language("EN-us-latn")
        assertEquals("en", lang.code, "Test for LL-CC-SSSS.getCode")
        assertEquals(null, lang.script, "Test for LL-CC-SSSS.getScript")
        assertEquals("US", lang.country, "Test for LL-CC-SSSS.getCountry")
    }

    @Test
    fun testMixed() {
        var lang = Language("En")
        assertEquals("en", lang.code, "Test for LL.getCode")
        assertEquals(null, lang.script, "Test for LL.getScript")
        assertEquals(null, lang.country, "Test for LL.getCountry")
        assertEquals("En", lang.givenSpecification, "Test for LL.getGiven")
        assertEquals("en", lang.foundSpecification, "Test for LL.getFound")
        assertTrue(lang.isValidLanguage, "Test for LL.isValid")
        lang = Language("En-lATn")
        assertEquals("en", lang.code, "Test for LL-SSSS.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS.getScript")
        assertEquals(null, lang.country, "Test for LL-SSSS.getCountry")
        assertEquals("En-lATn", lang.givenSpecification, "Test for LL-SSSS.getGiven")
        assertEquals("en", lang.foundSpecification, "Test for LL-SSSS.getFound")
        assertTrue(lang.isValidLanguage, "Test for LL-SSSS-CC.isValid")
        lang = Language("En-Us")
        assertEquals("en", lang.code, "Test for LL-CC.getCode")
        assertEquals(null, lang.script, "Test for LL-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-CC.getCountry")
        assertEquals("En-Us", lang.givenSpecification, "Test for LL-CC.getGiven")
        assertEquals("en-US", lang.foundSpecification, "Test for LL-CC.getFound")
        assertTrue(lang.isValidLanguage, "Test for LL-CC.isValid")
        lang = Language("En-lATn-Us")
        assertEquals("en", lang.code, "Test for LL-SSSS-CC.getCode")
        assertEquals("Latn", lang.script, "Test for LL-SSSS-CC.getScript")
        assertEquals("US", lang.country, "Test for LL-SSSS-CC.getCountry")
        assertEquals("En-lATn-Us", lang.givenSpecification, "Test for LL-SSSS-CC.getGiven")
        assertEquals("en-US", lang.foundSpecification, "Test for LL-SSSS=CC.getFound")
        assertTrue(lang.isValidLanguage, "Test for LL-SSSS-CC.isValid")
        lang = Language("En-Us-lATn")
        assertEquals("en", lang.code, "Test for LL-CC-SSSS.getCode")
        assertEquals(null, lang.script, "Test for LL-CC-SSSS.getScript")
        assertEquals("US", lang.country, "Test for LL-CC-SSSS.getCountry")
        assertEquals("En-Us-lATn", lang.givenSpecification, "Test for LL-CC-SSSS.getGiven")
        assertEquals("en-US", lang.foundSpecification, "Test for LL-CC-SSSS.getFound")
        assertFalse(lang.isValidLanguage, "Test for LL-CC-SSSS.isValid")
    }
}
