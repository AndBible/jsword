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

import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization of [Languages] and [Language], run with the English locale
 * (iso639_en.properties over iso639.properties) so the names are deterministic.
 * Expected values were derived by reading the properties files and the lookup code.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class LanguagesCharacterizationTest {
    private val savedProvider: LocaleProvider = LocaleProviderManager.getLocaleProvider()

    @BeforeTest
    fun useEnglish() {
        LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
            override fun getUserLocale(): KLocale = KLocale("en", "", "")
        })
    }

    @AfterTest
    fun restoreLocale() {
        LocaleProviderManager.setLocaleProvider(savedProvider)
    }

    @Test
    fun testLanguagesGetName() {
        assertEquals("English", Languages.getName("en"))
        assertEquals("Finnish (Suomi)", Languages.getName("fi"))
        assertEquals(GRC_NAME, Languages.getName("grc"))
        assertEquals(HE_NAME, Languages.getName("he"))
        // A missing key falls back to the code itself
        assertEquals("x-unknown", Languages.getName("x-unknown"))
        assertEquals("", Languages.getName(""))
    }

    @Test
    fun testLanguagesGetNameNull() {
        // ResourceBundle.getString(null) throws NPE; only MissingResourceException is caught
        assertFailsWith<NullPointerException> {
            Languages.getName(null)
        }
    }

    @Test
    fun testIsRtoL() {
        assertTrue(Languages.RtoL.isRtoL(null, "he"))
        // A script wins over the language, even when it says the opposite
        assertFalse(Languages.RtoL.isRtoL("Latn", "he"))
        assertTrue(Languages.RtoL.isRtoL("Arab", "en"))
        assertFalse(Languages.RtoL.isRtoL(null, null))
    }

    @Test
    fun testLanguageEnUs() {
        val l = Language("en-US")
        assertEquals("en", l.code)
        assertNull(l.script)
        assertEquals("US", l.country)
        assertEquals("English (US)", l.name)
        assertEquals("English (US)", l.toString())
        assertTrue(l.isLeftToRight)
        assertTrue(l.isValidLanguage)
        assertEquals("en-US", l.givenSpecification)
        assertEquals("en-US", l.foundSpecification)
    }

    @Test
    fun testLanguageZhHantTw() {
        val l = Language("zh-Hant-TW")
        assertEquals("zh", l.code)
        assertEquals("Hant", l.script)
        assertEquals("TW", l.country)
        // zh-Hant-TW has no entry; the lookup falls back to zh-Hant
        assertEquals(ZH_HANT_NAME, l.name)
        assertEquals("zh-Hant", l.foundSpecification)
        assertTrue(l.isLeftToRight)
        assertTrue(l.isValidLanguage)
    }

    @Test
    fun testLanguageFi() {
        val l = Language("fi")
        assertEquals("fi", l.code)
        assertNull(l.script)
        assertNull(l.country)
        assertEquals("Finnish (Suomi)", l.name)
        assertTrue(l.isLeftToRight)
    }

    @Test
    fun testLanguageHebrewIsRightToLeft() {
        val l = Language("he")
        assertEquals(HE_NAME, l.name)
        assertFalse(l.isLeftToRight)
        assertFalse(l.isLeftToRight, "a second call uses the cached answer")
    }

    @Test
    fun testLanguageUnd() {
        val l = Language("und")
        assertEquals("und", l.code)
        assertEquals("Undetermined", l.name)
        assertTrue(l.isValidLanguage)
    }

    @Test
    fun testLanguageEmptyIsUndeterminedAndInvalid() {
        val l = Language("")
        // Fewer than 2 characters: code is "und" and the language is invalid
        assertEquals("und", l.code)
        assertNull(l.script)
        assertNull(l.country)
        assertEquals("Undetermined", l.name)
        assertFalse(l.isValidLanguage)
        assertEquals("", l.givenSpecification)
        assertEquals("und", l.foundSpecification)
        assertTrue(l.isLeftToRight)
    }

    @Test
    fun testLanguageNullIsEnglish() {
        val l = Language(null)
        assertEquals("en", l.code)
        assertEquals("English", l.name)
        assertNull(l.givenSpecification)
        assertTrue(l.isValidLanguage)
        assertEquals(Language.DEFAULT_LANG, l)
        assertEquals("en", Language.DEFAULT_LANG.code)
    }

    @Test
    fun testLanguageUnknownCode() {
        val l = Language("zzz")
        assertEquals("zzz", l.code)
        assertEquals("zzz", l.name)
        assertFalse(l.isValidLanguage)
        assertEquals("zzz", l.foundSpecification)
        // A leading "x-" makes the second character a '-'
        val x = Language("x-foo")
        assertEquals("und", x.code)
        assertFalse(x.isValidLanguage)
    }

    @Test
    fun testLanguageCanonicalisationAndInvalidShapes() {
        val l = Language("EN-latn-us")
        assertEquals("en", l.code)
        assertEquals("Latn", l.script)
        assertEquals("US", l.country)
        // A fourth part makes the specification invalid, though code/script/country are kept
        val extra = Language("en-US-x-foo")
        assertEquals("en", extra.code)
        assertEquals("US", extra.country)
        assertFalse(extra.isValidLanguage)
        // A part of the wrong length
        assertFalse(Language("en-USA").isValidLanguage)
    }

    @Test
    fun testEqualsHashCodeCompareTo() {
        val a = Language("en-US")
        val b = Language("EN-us")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertEquals(0, a.compareTo(b))
        assertTrue(a.equals(a))
        assertFalse(a.equals(null))
        assertFalse(a.equals("en-US"))

        val en = Language("en")
        val fi = Language("fi")
        assertFalse(en.equals(a))
        assertFalse(en.equals(fi))
        // compareTo compares the names: "English" < "Finnish (Suomi)"
        assertTrue(en.compareTo(fi) < 0)
        assertTrue(fi.compareTo(en) > 0)

        // "" and "und" are the same language
        assertEquals(Language(""), Language("und"))
        assertEquals(Language("").hashCode(), Language("und").hashCode())

        // Not equal (country differs) but same name, same found spec: hashes and ordering agree anyway
        val hant = Language("zh-Hant")
        val hantTw = Language("zh-Hant-TW")
        assertFalse(hant.equals(hantTw))
        assertEquals(hant.hashCode(), hantTw.hashCode())
        assertEquals(0, hant.compareTo(hantTw))
    }

    private companion object {
        const val GRC_NAME = "Greek, Ancient [to 1453] (Ἑλληνική)"
        const val HE_NAME = "Hebrew (עברית)"
        const val ZH_HANT_NAME = "Chinese, Traditional (繁体中文)"
    }
}
