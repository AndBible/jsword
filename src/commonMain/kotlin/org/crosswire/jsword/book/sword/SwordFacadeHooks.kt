package org.crosswire.jsword.book.sword

import okio.*
import org.crosswire.common.kdom.Document
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun swordMetaDataToOsis(md: SwordBookMetaData): Document
internal expect fun sourceFilterFor(sourceType: String?): SourceFilter
internal expect fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book
internal expect fun registerSwordDriver(): Unit
