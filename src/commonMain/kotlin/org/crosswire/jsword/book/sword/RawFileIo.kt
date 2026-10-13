package org.crosswire.jsword.book.sword

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun rawFileCreate(location: StorageLocation): Boolean
internal expect fun rawFileNumber(value: Long, newline: Boolean): String
internal expect fun rawFileDecodeName(data: ByteArray): String
internal expect fun rawFileAppend(location: StorageLocation, ordinal: Long, charset: String?): Unit
internal expect fun rawFileWrite(location: StorageLocation, data: ByteArray): Unit
internal expect fun rawFileRead(location: StorageLocation, log: Logger): ByteArray
internal expect fun rawFileWriteInc(location: StorageLocation?, data: ByteArray, log: Logger): Unit
internal expect fun rawFilePopulate(location: StorageLocation, count: Int): Unit
