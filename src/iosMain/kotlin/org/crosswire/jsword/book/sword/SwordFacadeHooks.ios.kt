package org.crosswire.jsword.book.sword

import org.crosswire.common.kdom.Document
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.filter.SourceFilter

internal actual fun swordMetaDataToOsis(md: SwordBookMetaData): Document =
    throw UnsupportedOperationException("K5b: SwordBookMetaData.toOSIS needs the OSIS facade")
internal actual fun sourceFilterFor(sourceType: String?): SourceFilter =
    throw UnsupportedOperationException("K5b: source filters")
internal actual fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book =
    throw UnsupportedOperationException("K5b: book facade")
internal actual fun registerSwordDriver() {
    // No platform SwordBookDriver registry until K5b.
}
