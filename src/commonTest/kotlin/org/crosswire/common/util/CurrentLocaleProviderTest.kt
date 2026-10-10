package org.crosswire.common.util

import org.crosswire.jsword.internationalisation.DefaultLocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentLocaleProviderTest {
    @Test
    fun currentTracksHostProviderChangesWithoutChangingPlatformDefault() {
        val saved = LocaleProviderManager.getLocaleProvider()
        val platformDefault = KswordLocale.default()
        var hostLocale = KLocale("tr", "TR", "POSIX")
        try {
            LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
                override fun getUserLocale(): KLocale = hostLocale
            })
            assertEquals(hostLocale, KswordLocale.current())
            hostLocale = KLocale("sr", "LT")
            assertEquals(hostLocale, KswordLocale.current())
            assertEquals(platformDefault, KswordLocale.default())
            LocaleProviderManager.setLocaleProvider(DefaultLocaleProvider())
            assertEquals(platformDefault, KswordLocale.current())
        } finally {
            LocaleProviderManager.setLocaleProvider(saved)
        }
    }
}
