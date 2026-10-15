package org.crosswire.jsword.index

import org.crosswire.common.util.KUri
import org.crosswire.jsword.book.Book

/** iOS default until K6: no index exists, index work is refused, deleting a missing index is a no-op. */
internal class UnsupportedIndexManager : IndexManager {
    override fun isIndexed(book: Book?): Boolean = false
    override fun getIndex(book: Book?): Index = throw SearchUnsupportedException()
    override fun needsReindexing(book: Book?): Boolean = false
    override fun scheduleIndexCreation(book: Book?) = throw SearchUnsupportedException()
    override fun installDownloadedIndex(book: Book?, tempDest: KUri?) = throw SearchUnsupportedException()
    override fun deleteIndex(book: Book?) {}
    override fun closeAllIndexes() {}
    override var indexPolicy: IndexPolicy? = IndexPolicyAdapter()
}
