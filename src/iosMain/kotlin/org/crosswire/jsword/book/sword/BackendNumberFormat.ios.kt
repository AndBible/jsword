package org.crosswire.jsword.book.sword

import platform.Foundation.currentLocale

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

internal actual fun backendZeroPad(number: Int, width: Int): String = NSNumberFormatter().apply {
    locale = NSLocale.currentLocale
    numberStyle = NSNumberFormatterDecimalStyle
    usesGroupingSeparator = false
    minimumIntegerDigits = width.toULong()
    maximumFractionDigits = 0u
}.stringFromNumber(NSNumber(int = number)) ?: error("Unable to format backend number")
