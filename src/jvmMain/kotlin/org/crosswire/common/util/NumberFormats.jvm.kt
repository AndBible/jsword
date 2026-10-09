package org.crosswire.common.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

actual object NumberFormats {
    actual fun general(value: Number, locale: KLocale): String = NumberFormat.getInstance(locale.toJavaLocale()).format(value)

    actual fun integer(value: Number, locale: KLocale): String = NumberFormat.getIntegerInstance(locale.toJavaLocale()).format(value)

    actual fun pattern(value: Number, pattern: String, locale: KLocale): String =
        DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale.toJavaLocale())).format(value)
}
