package org.crosswire.common.util

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfEven

/**
 * Foundation-backed formats. Values go through `NSNumber(double)`, so a `Long` above 2^53 loses precision;
 * JSword formats counts and ordinals far below that.
 */
actual object NumberFormats {
    private fun formatter(locale: KLocale) = NSNumberFormatter().apply { this.locale = NSLocale(localeIdentifier = locale.toString()) }
    private fun ns(value: Number) = NSNumber(double = value.toDouble())

    actual fun general(value: Number, locale: KLocale): String = formatter(locale).apply {
        numberStyle = NSNumberFormatterDecimalStyle; maximumFractionDigits = 3u; roundingMode = NSNumberFormatterRoundHalfEven
    }.stringFromNumber(ns(value)) ?: value.toString()

    actual fun integer(value: Number, locale: KLocale): String = formatter(locale).apply {
        numberStyle = NSNumberFormatterDecimalStyle; maximumFractionDigits = 0u; roundingMode = NSNumberFormatterRoundHalfEven
    }.stringFromNumber(ns(value)) ?: value.toString()

    actual fun pattern(value: Number, pattern: String, locale: KLocale): String = formatter(locale).apply {
        positiveFormat = pattern; negativeFormat = "-$pattern"
    }.stringFromNumber(ns(value)) ?: value.toString()
}
