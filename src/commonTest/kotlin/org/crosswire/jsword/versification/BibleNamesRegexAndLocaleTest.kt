package org.crosswire.jsword.versification

import org.crosswire.common.util.KLocale
import org.crosswire.jsword.book.CaseType
import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BibleNamesRegexAndLocaleTest {
    @Test
    fun undefinedNamesRequireAWholeStringComment() {
        assertTrue(BibleNames.undefinedPattern.matches("#undefined"))
        assertTrue(BibleNames.undefinedPattern.matches("#"))
        assertFalse(BibleNames.undefinedPattern.matches("name#undefined"))
        assertFalse(BibleNames.undefinedPattern.matches("#undefined\nname"))
        assertFalse(BibleNames.undefinedPattern.matches(""))
    }

    @Test
    fun normalizationStripsOnlyDotsAndAsciiSpaces() {
        assertEquals("1john", BookName.normalize("1. John", KLocale.ENGLISH))
        assertEquals("john\t, ", BookName.normalize("John\t, ", KLocale.ENGLISH))
    }

    @Test
    fun literalBundlesAndFinnishParserUseTheSelectedLocale() {
        val provider = LocaleProviderManager.getLocaleProvider()
        val case = BookName.getDefaultCase()
        val full = BookName.isFullBookName()
        val truncate = BookName.getTruncateShortName()
        try {
            BookName.setCase(CaseType.SENTENCE)
            BookName.setFullBookName(true)
            BookName.setTruncateShortName(0)
            val kjv = Versifications.instance().getVersification("KJV")!!
            // Gen.Full from the checked-in embedded bundles (tr, el, fi, sr_LT).
            for ((locale, expected) in listOf(
            KLocale("tr") to "Yaratılış", KLocale("el") to "Γένεση",
            KLocale("fi") to "1. Mooseksen kirja", KLocale("sr", "LT") to "Postanje")) {
                assertEquals(expected, kjv.getPreferredNameInLocale(BibleBook.GEN, locale))
            }
            LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
                override fun getUserLocale() = KLocale("fi")
            })
            assertEquals("Gen.1.1", PassageKeyFactory.instance().getKey(kjv, "1. Mooseksen kirja 1:1").getOsisRef())
        } finally {
            LocaleProviderManager.setLocaleProvider(provider)
            BookName.setCase(case)
            BookName.setFullBookName(full)
            BookName.setTruncateShortName(truncate)
        }
    }
}
