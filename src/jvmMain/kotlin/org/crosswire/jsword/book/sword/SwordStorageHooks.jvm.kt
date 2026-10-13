package org.crosswire.jsword.book.sword


internal actual fun metadataDefaultsView(values: Map<String, String>): Map<String, String> =
    java.util.Collections.unmodifiableMap(java.util.HashMap(values))

internal actual fun storageSeparator(): String = java.io.File.separator

// Lexical File operations: Unix backslashes and dot spelling cannot survive okio Path conversion.
internal actual fun storageChild(parent: StorageLocation?, child: String?): StorageLocation =
    StorageLocation(java.io.File(parent?.let { java.io.File(it.spelling) }, child).path)
internal actual fun storageName(location: StorageLocation): String = java.io.File(location.spelling).name
internal actual fun storageParent(location: StorageLocation): StorageLocation? =
    java.io.File(location.spelling).parentFile?.let { StorageLocation(it.path) }
internal actual fun storageRead(location: StorageLocation): ByteArray = java.io.File(location.spelling).readBytes()
internal actual fun storageExists(location: StorageLocation): Boolean = java.io.File(location.spelling).exists()
internal actual fun storageDirectory(location: StorageLocation): Boolean = java.io.File(location.spelling).isDirectory
internal actual fun storageCanRead(location: StorageLocation): Boolean = java.io.File(location.spelling).canRead()
internal actual fun storageCanWrite(location: StorageLocation): Boolean = java.io.File(location.spelling).canWrite()
internal actual fun storageMkdirs(location: StorageLocation): Boolean = java.io.File(location.spelling).mkdirs()
internal actual fun storageListNames(location: StorageLocation): List<String>? = java.io.File(location.spelling).list()?.toList()
internal actual fun storageMove(from: StorageLocation, to: StorageLocation): Boolean =
    java.io.File(from.spelling).renameTo(java.io.File(to.spelling))
internal actual fun storageSave(section: org.crosswire.common.util.IniSection, location: StorageLocation, encoding: String?) {
    var writer: java.io.Writer? = null
    try {
        writer = java.io.OutputStreamWriter(java.io.FileOutputStream(java.io.File(location.spelling)), encoding)
        org.crosswire.common.util.IniSectionJvm.save(section, writer)
    } finally {
        // Match the legacy owned Writer close, including propagating close failures.
        writer?.close()
    }
}

internal actual fun storageUriLocation(uri: org.crosswire.common.util.KUri): StorageLocation =
    StorageLocation(java.io.File(uri).path)

internal actual fun storageFileSpelling(raw: String): String = java.io.File(raw).path
internal actual fun storageFileEquals(a: StorageLocation, b: StorageLocation): Boolean =
    java.io.File(a.spelling) == java.io.File(b.spelling)
internal actual fun storageFileHash(location: StorageLocation): Int = java.io.File(location.spelling).hashCode()

/** Open the authoritative File spelling without an okio Path projection. */
internal actual fun storageOpenRandomAccess(location: StorageLocation, writable: Boolean): org.crosswire.common.util.RandomAccessSource {
    val raf = java.io.RandomAccessFile(java.io.File(location.spelling), if (writable) "rw" else "r")
    return org.crosswire.common.util.RandomAccessSource(object : okio.FileHandle(writable) {
        override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int = synchronized(raf) {
            raf.seek(fileOffset)
            raf.read(array, arrayOffset, byteCount)
        }
        override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) = synchronized(raf) {
            raf.seek(fileOffset)
            raf.write(array, arrayOffset, byteCount)
        }
        override fun protectedSize(): Long = synchronized(raf) { raf.length() }
        override fun protectedResize(size: Long) = synchronized(raf) { raf.setLength(size) }
        override fun protectedFlush() = raf.fd.sync()
        override fun protectedClose() = raf.close()
    })
}

internal actual fun storageAbsolute(location: StorageLocation): String = java.io.File(location.spelling).absolutePath
