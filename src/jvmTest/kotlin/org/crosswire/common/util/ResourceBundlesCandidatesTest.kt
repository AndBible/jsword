package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.ResourceBundle

/** [ResourceBundles.candidates] against the JDK's `Control.getCandidateLocales` for script-less locales. */
class ResourceBundlesCandidatesTest {
    private val control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT)

    // KLocale normalises the legacy codes iw/in/ji to he/id/yi while java.util.Locale (JDK 17) keeps them
    // in candidate lists, so the JDK's side is normalised through toKLocale() before comparing.
    private fun jdk(l: Locale): List<KLocale> = control.getCandidateLocales("B", l).map { it.toKLocale() }

    @Test fun matchesJdk() {
        val locales = listOf(
            Locale("nn", "NO"), Locale("nn", "NO", "X"), Locale("no", "NO", "NY"), Locale("no"), Locale("no", "NO"),
            Locale("nb"), Locale("nb", "NO"), Locale("fi", "FI"), Locale("en", "", "POSIX"), Locale("pt", "PT"),
            Locale("xx"), Locale.ROOT, Locale("he"), Locale("iw"), Locale("id"), Locale("in"),
        )
        for (l in locales) assertEquals("candidates for '$l'", jdk(l), ResourceBundles.candidates(l.toKLocale()))
    }
}
