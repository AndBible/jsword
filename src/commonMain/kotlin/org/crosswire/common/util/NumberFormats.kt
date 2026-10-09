package org.crosswire.common.util

/**
 * Locale-aware number formatting; the JVM actual is `java.text`, so output is byte-identical to the old code.
 * Callers pass `KswordLocale.default()`, i.e. `Locale.getDefault()` (not `getDefault(Locale.Category.FORMAT)`), and
 * the KLocale conversion drops script and `-u-` extensions (e.g. a `-u-nu-` numbering-system extension is ignored).
 */
expect object NumberFormats {
    /** `NumberFormat.getInstance(locale)`. */
    fun general(value: Number, locale: KLocale): String

    /** `NumberFormat.getIntegerInstance(locale)`. */
    fun integer(value: Number, locale: KLocale): String

    /**
     * `DecimalFormat(pattern, DecimalFormatSymbols(locale))`. The JVM throws [IllegalArgumentException] for a bad
     * pattern; the iOS actual only approximates DecimalFormat patterns (via `NSNumberFormatter` positive/negative
     * formats) and never throws.
     */
    fun pattern(value: Number, pattern: String, locale: KLocale): String
}
