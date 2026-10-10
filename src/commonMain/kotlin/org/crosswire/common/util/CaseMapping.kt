package org.crosswire.common.util

/** Locale-sensitive lower-casing, as `String.lowercase(java.util.Locale)` (Turkish i, Lithuanian dots, ...). */
expect fun String.lowercase(locale: KLocale): String

/** Locale-sensitive upper-casing, as `String.uppercase(java.util.Locale)`. */
expect fun String.uppercase(locale: KLocale): String
