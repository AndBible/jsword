package org.crosswire.jsword.index

import org.crosswire.jsword.index.query.UnsupportedQueryDecorator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SearchUnsupportedTest {
    @Test fun indexManagerReportsNothingIndexedAndRefusesWork() {
        val m = UnsupportedIndexManager()
        assertFalse(m.isIndexed(null))
        assertFalse(m.needsReindexing(null))
        assertFailsWith<SearchUnsupportedException> { m.getIndex(null) }
        assertFailsWith<SearchUnsupportedException> { m.scheduleIndexCreation(null) }
        assertFailsWith<SearchUnsupportedException> { m.installDownloadedIndex(null, null) }
        m.deleteIndex(null)      // nothing to delete: no-op, so deleting a book works
        m.closeAllIndexes()      // no-op
    }

    @Test fun queryDecoratorRefusesOnUseNotOnCreation() {
        val d = UnsupportedQueryDecorator()
        assertFailsWith<SearchUnsupportedException> { d.decoratePhrase("a b") }
        assertFailsWith<SearchUnsupportedException> { d.decorateRange("Gen") }
    }

    @Test fun exceptionIsABookException() {
        assertEquals("Search is not supported on this platform yet", SearchUnsupportedException().message)
        assertFailsWith<org.crosswire.jsword.book.BookException> { throw SearchUnsupportedException() }
    }
}
