package org.crosswire.jsword.book.sword

import okio.Path
import okio.Path.Companion.toPath

/** File(parent, child) permits a null parent and preserves JVM child path joining. */
internal fun metadataChildPath(parent: Path?, child: String?): Path =
    java.io.File(parent?.toFile(), child).path.toPath()

internal fun metadataDefaultsView(values: Map<String, String>): Map<String, String> =
    java.util.Collections.unmodifiableMap(java.util.HashMap(values))

internal fun storageCanRead(path: Path): Boolean = path.toFile().canRead()
internal fun storageCanWrite(path: Path): Boolean = path.toFile().canWrite()
internal fun storageSeparator(): String = java.io.File.separator
internal fun storageMkdirs(path: Path): Boolean = path.toFile().mkdirs()

/** Preserve File.list native iteration order (okio sorts its directory lists). */
internal fun storageListNames(path: Path): List<String>? = path.toFile().list()?.toList()
