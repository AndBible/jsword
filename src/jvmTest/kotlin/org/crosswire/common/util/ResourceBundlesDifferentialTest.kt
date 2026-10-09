package org.crosswire.common.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

class ResourceBundlesDifferentialTest {
    private lateinit var savedDefault: Locale
    @Before fun save() { savedDefault = Locale.getDefault() }
    @After fun restore() { Locale.setDefault(savedDefault) }

    /** JSword's classpath resources through the JDK, as the old code read them. */
    private val loader = CWClassLoader.instance(ResourceBundlesDifferentialTest::class.java)

    private fun jdk(base: String, l: Locale): Map<String, String>? = try {
        val rb = ResourceBundle.getBundle(base, l, loader)
        rb.keySet().associateWith { rb.getString(it) }
    } catch (e: MissingResourceException) { null }

    private fun ours(base: String, l: Locale): Map<String, String>? =
        ResourceBundles.get(base, l.toKLocale())?.let { b -> b.keys.associateWith { b.getString(it)!! } }

    private val locales: List<Locale> = buildList {
        for (path in KswordResources.paths) {
            val m = Regex("^(BibleNames|JSMsg|JSOtherMsg|iso639|iso3166|OldMsg|GPLMsg|ot1nt2)_(.+)\\.properties$").find(path) ?: continue
            val parts = m.groupValues[2].split('_')
            add(Locale(parts[0], parts.getOrElse(1) { "" }))
        }
        addAll(listOf(Locale("fi", "FI"), Locale("pt", "PT"), Locale("zh", "HK"), Locale("sr", "LT"), Locale("nb", "NO"),
            Locale("no", "NO"), Locale("no", "NO", "NY"), Locale("nn", "NO"), Locale("iw"), Locale("he"), Locale("in"),
            Locale("id"), Locale("xx"), Locale.ROOT, Locale.ENGLISH))
    }.distinct()

    @Test fun everyBundleAndLocaleMatchesJdk() {
        for (default in listOf(Locale.ENGLISH, Locale("fi", "FI"), Locale("de"))) {
            Locale.setDefault(default)
            for (base in listOf("BibleNames", "JSMsg", "JSOtherMsg", "OldMsg", "iso639", "iso3166", "GPLMsg", "LGPLMsg", "ot1nt2")) {
                for (l in locales) assertEquals("$base $l (default $default)", jdk(base, l), ours(base, l))
            }
        }
    }
}
