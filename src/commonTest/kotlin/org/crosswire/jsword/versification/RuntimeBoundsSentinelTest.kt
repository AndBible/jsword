package org.crosswire.jsword.versification

import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals

/** Runtime array bounds failures on Native are IndexOutOfBoundsException, not our JVM alias. */
class RuntimeBoundsSentinelTest {
    @Test
    fun invalidChapterReturnsZero() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        assertEquals(0, kjv.getLastVerse(BibleBook.GEN, 999))
        assertEquals(0, kjv.getLastVerse(BibleBook.GEN, -1))
    }

    @Test
    fun absentBookReturnsZero() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        assertEquals(0, kjv.getLastChapter(BibleBook.TOB))
        assertEquals(0, kjv.getLastVerse(BibleBook.TOB, 1))
    }

    @Test
    fun foreignVerseOrdinalReturnsZero() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        val nrsv = Versifications.instance().getVersification("NRSV")!!
        assertEquals(0, kjv.getOrdinal(Verse(nrsv, BibleBook.TOB, 1, 1)))
    }

}
