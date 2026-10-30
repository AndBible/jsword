package org.crosswire.jsword.index.fts

import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.system.Versifications
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexReadErrorsTest {
    private val v11n = Versifications.instance().getVersification("KJV")!!
    private fun verses(n: Int): List<Key> = (1..n).map { Verse(v11n, BibleBook.GEN, 1, it) }

    @Test fun noErrorsIsAlwaysTolerable() {
        assertTrue(IndexReadErrors.isTolerable(0, 0))
        assertTrue(IndexReadErrors.isTolerable(0, 31102))
    }

    @Test fun smallModulesTolerateTheFixedMinimum() {
        assertTrue(IndexReadErrors.isTolerable(5, 100))
        assertFalse(IndexReadErrors.isTolerable(6, 100))
        assertTrue(IndexReadErrors.isTolerable(5, 600))
    }

    @Test fun largeModulesTolerateOnePercent() {
        assertTrue(IndexReadErrors.isTolerable(311, 31102))
        assertFalse(IndexReadErrors.isTolerable(312, 31102)) // 311.02 is the limit
        assertTrue(IndexReadErrors.isTolerable(1000, 100000))
        assertFalse(IndexReadErrors.isTolerable(1001, 100000))
    }

    @Test fun aModuleWhereEverythingFailedIsNeverTolerable() {
        assertFalse(IndexReadErrors.isTolerable(3, 3))
        assertFalse(IndexReadErrors.isTolerable(1, 1))
    }

    @Test fun theWarningListsAtMostTenReferences() {
        val text = IndexReadErrors.message(verses(10), IndexReadErrors.MAX_WARNED_REFERENCES)
        assertTrue(text.contains("Genesis 1:10"), text)
        assertFalse(text.contains("more"), text)
        val cut = IndexReadErrors.message(verses(14), IndexReadErrors.MAX_WARNED_REFERENCES)
        assertTrue(cut.contains("Genesis 1:10"), cut)
        assertFalse(cut.contains("Genesis 1:11"), cut)
        assertTrue(cut.contains("and 4 more"), cut)
    }

    @Test fun theRefusalListsAtMostTwentyReferencesAndCountsTheRest() {
        val text = IndexReadErrors.message(verses(300), IndexReadErrors.MAX_REFUSAL_REFERENCES)
        assertEquals(20, Regex("Genesis 1:\\d+").findAll(text).count(), text)
        assertTrue(text.contains("and 280 more"), text)
    }
}
