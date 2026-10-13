package org.crosswire.jsword.book.sword

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun metadataDefaultsView(values: Map<String, String>): Map<String, String>
internal expect fun storageSeparator(): String
internal expect fun storageChild(parent: StorageLocation?, child: String?): StorageLocation
internal expect fun storageName(location: StorageLocation): String
internal expect fun storageParent(location: StorageLocation): StorageLocation?
internal expect fun storageRead(location: StorageLocation): ByteArray
internal expect fun storageExists(location: StorageLocation): Boolean
internal expect fun storageDirectory(location: StorageLocation): Boolean
internal expect fun storageCanRead(location: StorageLocation): Boolean
internal expect fun storageCanWrite(location: StorageLocation): Boolean
internal expect fun storageMkdirs(location: StorageLocation): Boolean
internal expect fun storageListNames(location: StorageLocation): List<String>?
internal expect fun storageMove(from: StorageLocation, to: StorageLocation): Boolean
internal expect fun storageSave(section: org.crosswire.common.util.IniSection, location: StorageLocation, encoding: String?): Unit
internal expect fun storageUriLocation(uri: org.crosswire.common.util.KUri): StorageLocation
internal expect fun storageFileSpelling(raw: String): String
internal expect fun storageFileEquals(a: StorageLocation, b: StorageLocation): Boolean
internal expect fun storageFileHash(location: StorageLocation): Int
internal expect fun storageOpenRandomAccess(location: StorageLocation, writable: Boolean): org.crosswire.common.util.RandomAccessSource
internal expect fun storageAbsolute(location: StorageLocation): String
