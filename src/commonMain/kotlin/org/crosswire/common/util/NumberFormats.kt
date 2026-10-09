package org.crosswire.common.util

/** Locale-aware number formatting; the JVM actual is `java.text`, so output is byte-identical to the old code. */
expect object NumberFormats {
    /** `NumberFormat.getInstance(locale)`. */
    fun general(value: Number, locale: KLocale): String

    /** `NumberFormat.getIntegerInstance(locale)`. */
    fun integer(value: Number, locale: KLocale): String

    /** `DecimalFormat(pattern, DecimalFormatSymbols(locale))`; throws [IllegalArgumentException] for a bad pattern. */
    fun pattern(value: Number, pattern: String, locale: KLocale): String
}
