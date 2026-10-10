package org.crosswire.common.util

import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.lowercaseStringWithLocale
import platform.Foundation.uppercaseStringWithLocale

@Suppress("CAST_NEVER_SUCCEEDS")
actual fun String.lowercase(locale: KLocale): String =
    (this as NSString).lowercaseStringWithLocale(NSLocale(localeIdentifier = locale.toString()))

/**
 * Foundation's Greek locale removes tonos on upper-casing, unlike JDK 17. Use the root locale for
 * Greek to retain accents and match the JDK; other languages keep their locale-sensitive mappings.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
actual fun String.uppercase(locale: KLocale): String {
    val mappingLocale = if (locale.language == "el") KLocale.ROOT else locale
    return (this as NSString).uppercaseStringWithLocale(NSLocale(localeIdentifier = mappingLocale.toString()))
}
