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

    private fun runtimeBoundsVersification(): Versification {
        val v11n = Versification()
        // The real open ordinal lookup executes inside all three production try blocks.
        // Inject only in this private instance; do not alter the global versification registry.
        val field = Versification::class.java.getDeclaredField("bookList")
        field.isAccessible = true
        field.set(v11n, object : BibleBookList(emptyArray()) {
            override fun getOrdinal(book: BibleBook?): Int = throw IndexOutOfBoundsException("runtime array bounds")
        })
        return v11n
    }

    @Test
    fun runtimeBoundsInLastChapterReturnsZero() {
        assertEquals(0, runtimeBoundsVersification().getLastChapter(BibleBook.GEN))
    }

    @Test
    fun runtimeBoundsInLastVerseReturnsZero() {
        assertEquals(0, runtimeBoundsVersification().getLastVerse(BibleBook.GEN, 999))
    }

    @Test
    fun runtimeBoundsInOrdinalReturnsZero() {
        val kjv = Versifications.instance().getVersification("KJV")!!
        assertEquals(0, runtimeBoundsVersification().getOrdinal(Verse(kjv, BibleBook.GEN, 1, 1)))
    }
}
