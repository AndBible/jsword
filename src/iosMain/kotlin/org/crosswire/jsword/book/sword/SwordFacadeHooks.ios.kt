package org.crosswire.jsword.book.sword

import fi.sykero.kdom.Document
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.filter.SourceFilter

internal actual fun swordMetaDataToOsis(md: SwordBookMetaData): Document =
    throw UnsupportedOperationException("K5b: SwordBookMetaData.toOSIS needs the OSIS facade")
internal actual fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book =
    throw UnsupportedOperationException("K5b: book facade")
internal actual fun registerSwordDriver() {
    // No platform SwordBookDriver registry until K5b.
}
