package org.crosswire.common.compress

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun platformCompress(type: CompressorType, input: ByteArray): ByteArray
internal expect fun platformUncompress(type: CompressorType, input: ByteArray, expectedLength: Int): ByteArray
internal expect fun platformGzipUncompress(input: ByteArray, expectedLength: Int): ByteArray
