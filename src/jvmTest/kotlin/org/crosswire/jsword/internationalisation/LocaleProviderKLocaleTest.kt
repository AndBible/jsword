package org.crosswire.jsword.internationalisation

import org.crosswire.common.util.KLocale
import org.crosswire.common.util.KswordLocale
import org.crosswire.common.util.toJavaLocale
import org.crosswire.jsword.book.CaseType
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.BibleNames
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocaleProviderKLocaleTest {
    private val saved = LocaleProviderManager.getLocaleProvider()

    @AfterTest
    fun restore() = LocaleProviderManager.setLocaleProvider(saved)

    private fun useLocale(locale: KLocale) {
        LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
            override fun getUserLocale() = locale
        })
    }

    @Test
    fun currentFollowsTheProvider() {
        useLocale(KLocale("sr", "LT"))
        assertEquals(KLocale("sr", "LT"), KswordLocale.current())
        assertEquals(KLocale("sr", "LT"), LocaleProviderManager.getLocale())
    }

    @Test
    fun serbianLatinPicksTheSameBundleAsBefore() {
        // :app maps sr-Latn to sr_LT, which selects BibleNames_sr_LT.properties.
        useLocale(KLocale("sr", "LT"))
        assertEquals("Postanje", BibleNames.instance().getPreferredName(BibleBook.GEN))
        assertEquals(BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale("sr", "LT")),
            BibleNames.instance().getPreferredName(BibleBook.GEN))
    }

    @Test
    fun variantSurvivesTheProviderAndJvmBoundary() {
        val locale = KLocale("tr", "TR", "POSIX")
        useLocale(locale)
        assertEquals(locale, KswordLocale.current())
        assertEquals(Locale("tr", "TR", "POSIX"), LocaleProviderManager.getLocale().toJavaLocale())
        // Exercises the locale-sensitive consumer, not only the value's getters.
        assertEquals("ı", CaseType.LOWER.setCase("I"))
    }

    @Test
    fun defaultProviderIsThePlatformDefault() {
        LocaleProviderManager.setLocaleProvider(DefaultLocaleProvider())
        assertEquals(KswordLocale.default(), KswordLocale.current())
    }
}
