package org.crosswire.common.util

// A java.util.Locale argument resolves to the stdlib JVM overloads, not to these functions.
actual fun String.lowercase(locale: KLocale): String = lowercase(locale.toJavaLocale())

actual fun String.uppercase(locale: KLocale): String = uppercase(locale.toJavaLocale())
