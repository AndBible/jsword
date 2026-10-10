package org.crosswire.common.util

/**
 * A locale value for common code (language, country, variant) (K2 F1). The string form matches
 * `java.util.Locale` except for a variant-only locale (ours "__POSIX", Java ""). There is no script
 * support, so the JDK's script-based candidates (e.g. zh_#Hant) are not produced.
 */
class KLocale(language: String, country: String = "", variant: String = "") {
    val language: String = normalizeLanguage(language.lowercase())
    val country: String = country.uppercase()
    val variant: String = variant

    override fun equals(other: Any?): Boolean =
        other is KLocale && language == other.language && country == other.country && variant == other.variant

    override fun hashCode(): Int = (language.hashCode() * 31 + country.hashCode()) * 31 + variant.hashCode()

    override fun toString(): String {
        if (language.isEmpty() && country.isEmpty() && variant.isEmpty()) return ""
        val sb = StringBuilder(language)
        if (country.isNotEmpty() || variant.isNotEmpty()) sb.append('_').append(country)
        if (variant.isNotEmpty()) sb.append('_').append(variant)
        return sb.toString()
    }

    companion object {
        val ROOT = KLocale("")
        val ENGLISH = KLocale("en")

        /** JDK 17+ reports the current ISO 639 codes for Hebrew, Indonesian and Yiddish. */
        fun normalizeLanguage(code: String): String = when (code) { "iw" -> "he"; "in" -> "id"; "ji" -> "yi"; else -> code }

        /** The legacy code JSword's resource files may still use for [code], or null. */
        fun legacyLanguage(code: String): String? = when (code) { "he" -> "iw"; "id" -> "in"; "yi" -> "ji"; else -> null }
    }
}

/** The locale JSword formats and translates for; routed through the platform until Task 11. */
object KswordLocale {
    fun current(): KLocale = platformCurrentLocale()
    /** The platform default locale (`Locale.getDefault()` on the JVM), used by bundle fallback and number formats. */
    fun default(): KLocale = platformDefaultLocale()
}

internal expect fun platformCurrentLocale(): KLocale
internal expect fun platformDefaultLocale(): KLocale
