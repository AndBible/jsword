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
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException




/**
 * JUnit test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BibleNamesTest {
    private var fullName = false

    /**
     * {@link BookName#setFullBookName(boolean)} is global state that several other test classes
     * change without restoring it. Pin it here so that these tests do not depend on test order.
     */
    @BeforeTest
    fun setUp() {
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(true)
    }

    @AfterTest
    fun tearDown() {
        BookName.setFullBookName(fullName)
    }

    @Test
    fun testLoadEnglish() {
        BibleNames.instance().load(KLocale.ENGLISH)
    }

    @Test
    fun testLoadAF() {
        BibleNames.instance().load(KLocale("af"))
    }

    @Test
    fun testLoadEgyptianArabic() {
        BibleNames.instance().load(KLocale("ar", "EG"))
    }

    @Test
    fun testLoadBG() {
        BibleNames.instance().load(KLocale("bg"))
    }

    @Test
    fun testLoadCS() {
        BibleNames.instance().load(KLocale("cs"))
    }

    @Test
    fun testLoadCY() {
        BibleNames.instance().load(KLocale("cy"))
    }

    @Test
    fun testLoadDanish() {
        BibleNames.instance().load(KLocale("da"))
    }

    @Test
    fun testLoadGerman() {
        BibleNames.instance().load(KLocale("de"))
    }

    @Test
    fun testLoadSpanish() {
        BibleNames.instance().load(KLocale("es"))
    }

    @Test
    fun testLoadET() {
        BibleNames.instance().load(KLocale("et"))
    }

    @Test
    fun testLoadFarsi() {
        BibleNames.instance().load(KLocale("fa"))
    }

    @Test
    fun testLoadFinnish() {
        BibleNames.instance().load(KLocale("fi"))
    }

    @Test
    fun testLoadFO() {
        BibleNames.instance().load(KLocale("fo"))
    }

    @Test
    fun testLoadFrench() {
        BibleNames.instance().load(KLocale("fr"))
    }

    @Test
    fun testLoadHebrew() {
        BibleNames.instance().load(KLocale("he"))
    }

    @Test
    fun testLoadHU() {
        BibleNames.instance().load(KLocale("hu"))
    }

    @Test
    fun testLoadID() {
        BibleNames.instance().load(KLocale("id"))
    }

    @Test
    fun testLoadIN() {
        // "in" is the legacy ISO code for Indonesian (modern "id"); both must yield
        // Indonesian book names, never Hindi. BibleNames_in.properties previously held
        // a copy of the Hindi names, which surfaced as Hindi for Indonesian users on
        // runtimes that resolve Indonesian to the "in" bundle (e.g. Android's ART).
        BibleNames.instance().load(KLocale("in"))
        var viaIn: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale("in"))
        var viaId: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale("id"))
        var hindi: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale("hi"))
        assertEquals(viaId, viaIn)
        assertNotEquals(hindi, viaIn)
    }

    @Test
    fun testLoadItalian() {
        BibleNames.instance().load(KLocale("it"))
    }

    @Test
    fun testLoadIW() {
        BibleNames.instance().load(KLocale("iw"))
    }

    @Test
    fun testLoadKO() {
        BibleNames.instance().load(KLocale("ko"))
    }

    @Test
    fun testLoadLA() {
        BibleNames.instance().load(KLocale("la"))
    }

    @Test
    fun testLoadLT() {
        BibleNames.instance().load(KLocale("lt"))
    }

    @Test
    fun testLoadNB() {
        BibleNames.instance().load(KLocale("nb"))
    }

    @Test
    fun testLoadDutch() {
        BibleNames.instance().load(KLocale("nl"))
    }

    @Test
    fun testLoadNN() {
        BibleNames.instance().load(KLocale("nn"))
    }

    @Test
    fun testLoadPL() {
        BibleNames.instance().load(KLocale("pl"))
    }

    @Test
    fun testLoadBrazillianPortuguese() {
        BibleNames.instance().load(KLocale("pt", "BR"))
    }

    @Test
    fun testLoadPortuguese() {
        BibleNames.instance().load(KLocale("pt"))
    }

    @Test
    fun testLoadRo() {
        BibleNames.instance().load(KLocale("ro"))
    }

    @Test
    fun testLoadRU() {
        BibleNames.instance().load(KLocale("ru"))
    }

    @Test
    fun testLoadSK() {
        BibleNames.instance().load(KLocale("sk"))
    }

    @Test
    fun testLoadSL() {
        BibleNames.instance().load(KLocale("sl"))
    }

    @Test
    fun testLoadSwedish() {
        BibleNames.instance().load(KLocale("sv"))
    }

    @Test
    fun testLoadThai() {
        BibleNames.instance().load(KLocale("th"))
    }

    @Test
    fun testLoadTR() {
        BibleNames.instance().load(KLocale("tr"))
    }

    @Test
    fun testLoadUkranian() {
        BibleNames.instance().load(KLocale("uk"))
    }

    @Test
    fun testLoadVietnamese() {
        BibleNames.instance().load(KLocale("vi"))
    }

    @Test
    fun testLoadChineseTraditional() {
        BibleNames.instance().load(KLocale("zh", "CN"))
    }

    @Test
    fun testLoadChineseSimplified() {
        BibleNames.instance().load(KLocale("zh"))
    }

    @Test
    fun testLoadSwahili() {
        BibleNames.instance().load(KLocale("sw"))
    }

    @Test
    fun testLoadMS() {
        var locale: KLocale = KLocale("ms")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }

    @Test
    fun testLoadCA() {
        var locale: KLocale = KLocale("ca")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }

    @Test
    fun testLoadFIL() {
        // Filipino Genesis is also "Genesis"; assert on Exodus ("Exodo") instead.
        var locale: KLocale = KLocale("fil")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.EXOD, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.EXOD, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }

    @Test
    fun testLoadNE() {
        var locale: KLocale = KLocale("ne")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }

    @Test
    fun testLoadUR() {
        var locale: KLocale = KLocale("ur")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }

    @Test
    fun testLoadUZ() {
        var locale: KLocale = KLocale("uz")
        BibleNames.instance().load(locale)
        var localized: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, locale)
        var english: String = BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale.ENGLISH)
        assertNotEquals(english, localized)
    }


}
