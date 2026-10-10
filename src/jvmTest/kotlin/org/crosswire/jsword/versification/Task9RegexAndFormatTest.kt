package org.crosswire.jsword.versification

import org.crosswire.common.util.KLocale
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseKey
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.system.Versifications
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Task9RegexAndFormatTest {
    @Test
    fun cardinalityErrorKeepsTheCapturedFormat() {
        val case = BookName.getDefaultCase()
        val full = BookName.isFullBookName()
        val truncate = BookName.getTruncateShortName()
        val provider = org.crosswire.jsword.internationalisation.LocaleProviderManager.getLocaleProvider()
        try {
            BookName.setCase(org.crosswire.jsword.book.CaseType.SENTENCE)
            BookName.setFullBookName(true)
            BookName.setTruncateShortName(0)
            org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(
                object : org.crosswire.jsword.internationalisation.LocaleProvider {
                    override fun getUserLocale() = KLocale.ENGLISH
                })
            val v11n = Versifications.instance().getVersification("KJV")!!
            val mapper = VersificationToKJVMapper(v11n, FileVersificationMapping())
            val report = VersificationToKJVMapper::class.java.getDeclaredMethod(
                "reportCardinalityError", VerseKey::class.java, VerseKey::class.java)
            report.isAccessible = true
            val start = Verse(v11n, BibleBook.GEN, 1, 1)
            val error = assertFailsWith<InvocationTargetException> {
                report.invoke(mapper, VerseRange(v11n, start, Verse(v11n, BibleBook.GEN, 1, 2)),
                    VerseRange(v11n, start, Verse(v11n, BibleBook.GEN, 1, 3)))
            }
            assertEquals("Genesis 1:1-2 has a cardinality of 2 whilst Genesis 1:1-3 has a cardinality of 3.", error.cause?.message)
        } finally {
            BookName.setCase(case)
            BookName.setFullBookName(full)
            BookName.setTruncateShortName(truncate)
            org.crosswire.jsword.internationalisation.LocaleProviderManager.setLocaleProvider(provider)
        }
    }
}
