package org.crosswire.jsword.book.install

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun archiveRead(path: String): ByteArray
internal expect fun archiveSink(path: String): BufferedSink
internal expect fun archiveGzipSource(path: String): okio.Source
internal expect fun archiveLowercase(name: String): String
internal expect fun archiveZipName(bytes: ByteArray, utf8: Boolean): String
internal expect fun archiveDescriptorSource(bytes: ByteArray, offset: Int, consumed: (Int) -> Unit): okio.Source
internal expect fun archiveUnsupportedZipFeature(archiveSpelling: String, centralIndex: Int): Nothing
internal expect fun archiveCacheSource(bytes: ByteArray, offset: Int, compressedSize: Int): Pair<okio.Source, () -> Unit>
internal expect fun archiveInflateSource(bytes: ByteArray): okio.Source
internal expect fun archiveExtract(dest: String, name: String, directory: Boolean, source: () -> okio.Source): Unit
