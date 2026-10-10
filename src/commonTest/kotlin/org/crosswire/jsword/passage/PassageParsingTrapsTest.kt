package org.crosswire.jsword.passage

import org.crosswire.common.util.KLocale
import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Literals from task-9-prechange.xml on 452ea363, not the converted parser. */
class PassageParsingTrapsTest {
    @Test
    fun preservesPrechangeWhitespaceReferences() {
        val provider = LocaleProviderManager.getLocaleProvider()
        try {
            LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
                override fun getUserLocale() = KLocale.ENGLISH
            })
            val v11n = Versifications.instance().getVersification("KJV")
            for (space in listOf(' ', '\t', '\n', ' ', ' ', ' ', ' ')) {
                assertEquals(if (space == '\t' || space == '\n') "John" else "John.3.16",
                    PassageKeyFactory.instance().getKey(v11n, "John${space}3:16").getOsisRef())
                assertEquals("John.3.16-John.3.17",
                    PassageKeyFactory.instance().getKey(v11n, "John 3:16,$space 17").getOsisRef())
                assertEquals("1John.1.1",
                    PassageKeyFactory.instance().getKey(v11n, "1${space}John 1:1").getOsisRef())
                val input = "John 3:16${space}17"
                if (space in listOf(' ', ' ', ' ')) {
                    val error = assertFailsWith<NoSuchVerseException> { PassageKeyFactory.instance().getKey(v11n, input) }
                    assertEquals("Too many parts to the Verse. (Parts are separated by any of 3:16${space}17, 3, 16, 17)", error.message)
                } else {
                    assertEquals("John.3.16-John.3.17", PassageKeyFactory.instance().getKey(v11n, input).getOsisRef())
                }
                assertEquals(listOf("John", "3", "16", "17"), AccuracyType.tokenize(input).toList())
            }
        } finally {
            LocaleProviderManager.setLocaleProvider(provider)
        }
    }
}
