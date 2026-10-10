package org.crosswire.jsword.versification

import org.crosswire.common.util.KLocale
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonLocaleBoundaryTest {
    @Test
    fun commonLocaleRendersReferencesAndNormalizesBookNames() {
        val fullName = BookName.isFullBookName()
        val case = BookName.getDefaultCase()
        val truncate = BookName.getTruncateShortName()
        try {
            BookName.setFullBookName(true)
            BookName.setTruncateShortName(0)
            BookName.setCase(org.crosswire.jsword.book.CaseType.SENTENCE)
            assertEquals("ıi", BookName.normalize("I. İ", KLocale("tr", "TR", "POSIX")))
            assertEquals("ος", BookName.normalize("ΟΣ", KLocale("el")))
            assertEquals("Postanje", BibleNames.instance().getPreferredNameInLocale(BibleBook.GEN, KLocale("sr", "LT")))
            val v11n = Versifications.instance().getVersification("KJV")!!
            assertEquals("Postanje", v11n.getPreferredNameInLocale(BibleBook.GEN, KLocale("sr", "LT")))
            val start = Verse(v11n, BibleBook.GEN, 1, 1)
            assertEquals("Postanje 1:1", start.getNameInLocale(null, KLocale("sr", "LT")))
            assertEquals("Postanje 1:1-2", VerseRange(v11n, start, Verse(v11n, BibleBook.GEN, 1, 2)).getNameInLocale(null, KLocale("sr", "LT")))
        } finally {
            BookName.setFullBookName(fullName)
            BookName.setCase(case)
            BookName.setTruncateShortName(truncate)
        }
    }
}
