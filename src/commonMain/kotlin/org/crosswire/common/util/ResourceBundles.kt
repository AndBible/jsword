package org.crosswire.common.util

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** A loaded bundle with its parent chain, like `PropertyResourceBundle` (K2 F1). */
class KBundle internal constructor(val locale: KLocale, private val entries: Map<String, String>, val parent: KBundle?) {
    fun getString(key: String): String? = entries[key] ?: parent?.getString(key)

    val keys: Set<String>
        get() = buildSet {
            var b: KBundle? = this@KBundle
            while (b != null) { addAll(b.entries.keys); b = b.parent }
        }
}

/**
 * `ResourceBundle.getBundle` over [KswordResources]: the JDK's default candidate list (incl. the
 * Norwegian rules and the legacy ISO 639 codes), parent chaining, and the default-locale fallback.
 */
object ResourceBundles {
    private val lock = SynchronizedObject()
    private val cache = HashMap<String, KBundle?>()

    fun get(baseName: String, locale: KLocale, charset: PropertiesCharset = PropertiesCharset.UTF8_THEN_LATIN1): KBundle? {
        val primary = chain(baseName, locale, charset)
        if (primary != null && (primary.locale != KLocale.ROOT || locale == KLocale.ROOT)) return primary
        val fallback = KswordLocale.default()
        if (fallback != locale) {
            val second = chain(baseName, fallback, charset)
            if (second != null && second.locale != KLocale.ROOT) return second
        }
        return primary
    }

    private fun chain(baseName: String, locale: KLocale, charset: PropertiesCharset): KBundle? {
        val key = "$baseName|$locale|$charset"
        synchronized(lock) { if (cache.containsKey(key)) return cache[key] }
        var parent: KBundle? = null
        for (candidate in candidates(locale).asReversed()) {
            val bytes = load(baseName, candidate) ?: continue
            parent = KBundle(candidate, PropertiesParser.parse(bytes, charset), parent)
        }
        synchronized(lock) { cache[key] = parent }
        return parent
    }

    private fun load(baseName: String, l: KLocale): ByteArray? {
        KswordResources.bytes(fileName(baseName, l))?.let { return it }
        val legacy = KLocale.legacyLanguage(l.language) ?: return null
        return KswordResources.bytes(fileName(baseName, l, legacy))
    }

    internal fun fileName(baseName: String, l: KLocale, language: String = l.language): String {
        val suffix = KLocale(language, l.country, l.variant).toString()
            .let { if (language != l.language) it.replaceFirst(KLocale.normalizeLanguage(language), language) else it }
        return (if (suffix.isEmpty()) baseName else "${baseName}_$suffix") + ".properties"
    }

    /** JDK `ResourceBundle.Control.getCandidateLocales` for locales without a script, ROOT last. */
    internal fun candidates(l: KLocale): List<KLocale> {
        if (l.language == "nb" || (l.language == "no" && !(l.country == "NO" && l.variant == "NY"))) {
            val base = if (l.language == "nb") "nb" else "no"
            val other = if (base == "nb") "no" else "nb"
            return buildList {
                for (c in defaultList(base, l.country, l.variant)) { add(c); add(KLocale(other, c.country, c.variant)) }
                add(KLocale.ROOT)
            }
        }
        if (l.language == "nn" || (l.language == "no" && l.country == "NO" && l.variant == "NY")) {
            // JDK: nn candidates, then the no_NO_NY / no_NO / no chain (no_NO_NY itself starts with the "nn" list).
            val nnVariant = if (l.language == "nn") l.variant else ""
            val nnCountry = if (l.language == "nn") l.country else "NO"
            return defaultList("nn", nnCountry, nnVariant) +
                listOf(KLocale("no", "NO", "NY"), KLocale("no", "NO"), KLocale("no")) + KLocale.ROOT
        }
        return defaultList(l.language, l.country, l.variant) + KLocale.ROOT
    }

    private fun defaultList(language: String, country: String, variant: String): List<KLocale> = buildList {
        if (variant.isNotEmpty()) {
            var v = variant
            while (true) {
                add(KLocale(language, country, v))
                val cut = v.lastIndexOf('_')
                if (cut < 0) break
                v = v.substring(0, cut)
            }
        }
        if (country.isNotEmpty()) add(KLocale(language, country))
        if (language.isNotEmpty()) add(KLocale(language))
    }
}
