package org.crosswire.jsword.book.sword


internal fun metadataDefaultsView(values: Map<String, String>): Map<String, String> =
    java.util.Collections.unmodifiableMap(java.util.HashMap(values))

internal fun storageSeparator(): String = java.io.File.separator

// Lexical File operations: Unix backslashes and dot spelling cannot survive okio Path conversion.
internal fun storageChild(parent: StorageLocation?, child: String?): StorageLocation =
    StorageLocation(java.io.File(parent?.let { java.io.File(it.spelling) }, child).path)
internal fun storageName(location: StorageLocation): String = java.io.File(location.spelling).name
internal fun storageParent(location: StorageLocation): StorageLocation? =
    java.io.File(location.spelling).parentFile?.let { StorageLocation(it.path) }
internal fun storageRead(location: StorageLocation): ByteArray = java.io.File(location.spelling).readBytes()
internal fun storageExists(location: StorageLocation): Boolean = java.io.File(location.spelling).exists()
internal fun storageDirectory(location: StorageLocation): Boolean = java.io.File(location.spelling).isDirectory
internal fun storageCanRead(location: StorageLocation): Boolean = java.io.File(location.spelling).canRead()
internal fun storageCanWrite(location: StorageLocation): Boolean = java.io.File(location.spelling).canWrite()
internal fun storageMkdirs(location: StorageLocation): Boolean = java.io.File(location.spelling).mkdirs()
internal fun storageListNames(location: StorageLocation): List<String>? = java.io.File(location.spelling).list()?.toList()
internal fun storageMove(from: StorageLocation, to: StorageLocation): Boolean =
    java.io.File(from.spelling).renameTo(java.io.File(to.spelling))
internal fun storageSave(section: org.crosswire.common.util.IniSection, location: StorageLocation, encoding: String?) {
    var writer: java.io.Writer? = null
    try {
        writer = java.io.OutputStreamWriter(java.io.FileOutputStream(java.io.File(location.spelling)), encoding)
        org.crosswire.common.util.IniSectionJvm.save(section, writer)
    } finally {
        // Match the legacy owned Writer close, including propagating close failures.
        writer?.close()
    }
}

internal fun storageUriLocation(uri: org.crosswire.common.util.KUri): StorageLocation =
    StorageLocation(java.io.File(uri).path)

internal fun storageFileSpelling(raw: String): String = java.io.File(raw).path
internal fun storageFileEquals(a: StorageLocation, b: StorageLocation): Boolean =
    java.io.File(a.spelling) == java.io.File(b.spelling)
internal fun storageFileHash(location: StorageLocation): Int = java.io.File(location.spelling).hashCode()
