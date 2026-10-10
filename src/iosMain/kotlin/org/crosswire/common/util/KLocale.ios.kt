package org.crosswire.common.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * `NSLocale.currentLocale` reflects the app bundle's localizations and ignores the script.
 * An iOS host calls `LocaleProviderManager.setLocaleProvider` to select JSword's current locale;
 * this platform default remains the fallback for bundles and number formats.
 */
internal actual fun platformDefaultLocale(): KLocale {
    val l = NSLocale.currentLocale
    return KLocale(l.languageCode, l.countryCode ?: "")
}
