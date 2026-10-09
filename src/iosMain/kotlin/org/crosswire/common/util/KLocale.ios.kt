package org.crosswire.common.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

internal actual fun platformCurrentLocale(): KLocale = platformDefaultLocale()
internal actual fun platformDefaultLocale(): KLocale {
    val l = NSLocale.currentLocale
    return KLocale(l.languageCode, l.countryCode ?: "")
}
