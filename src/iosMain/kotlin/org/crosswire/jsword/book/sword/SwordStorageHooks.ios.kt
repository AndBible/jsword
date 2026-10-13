package org.crosswire.jsword.book.sword

import okio.buffer
import okio.use
import org.crosswire.common.util.*

internal actual fun metadataDefaultsView(values: Map<String, String>): Map<String, String> = object : AbstractMap<String, String>() {
    private val snapshot = values.entries.map { entry -> object : Map.Entry<String, String> {
        override val key = entry.key
        override val value = entry.value
        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int = key.hashCode() xor value.hashCode()
    } }
    override val entries: Set<Map.Entry<String, String>> = object : AbstractSet<Map.Entry<String, String>>() {
        override val size: Int get() = snapshot.size
        override fun iterator(): Iterator<Map.Entry<String, String>> = object : Iterator<Map.Entry<String, String>> {
            private var index = 0
            override fun hasNext(): Boolean = index < snapshot.size
            override fun next(): Map.Entry<String, String> = if (hasNext()) snapshot[index++] else throw NoSuchElementException()
        }
    }
}
internal actual fun storageSeparator(): String = "/"
internal actual fun storageChild(parent: StorageLocation?, child: String?): StorageLocation =
    StorageLocation(NativeLexicalFiles.child(parent?.spelling, child ?: throw NullPointerException()))
internal actual fun storageName(location: StorageLocation): String = NativeLexicalFiles.name(location.spelling)
internal actual fun storageParent(location: StorageLocation): StorageLocation? = NativeLexicalFiles.parent(location.spelling)?.let(::StorageLocation)
internal actual fun storageRead(location: StorageLocation): ByteArray = NativeLexicalFiles.read(location.spelling)
internal actual fun storageExists(location: StorageLocation): Boolean = NativeLexicalFiles.exists(location.spelling)
internal actual fun storageDirectory(location: StorageLocation): Boolean = NativeLexicalFiles.directory(location.spelling)
internal actual fun storageCanRead(location: StorageLocation): Boolean = NativeLexicalFiles.canRead(location.spelling)
internal actual fun storageCanWrite(location: StorageLocation): Boolean = NativeLexicalFiles.canWrite(location.spelling)
internal actual fun storageMkdirs(location: StorageLocation): Boolean = NativeLexicalFiles.mkdirs(location.spelling)
internal actual fun storageListNames(location: StorageLocation): List<String>? = NativeLexicalFiles.list(location.spelling)
internal actual fun storageMove(from: StorageLocation, to: StorageLocation): Boolean = NativeLexicalFiles.move(from.spelling, to.spelling)
internal actual fun storageSave(section: IniSection, location: StorageLocation, encoding: String?) {
    val sink = NativeLexicalFiles.sink(location.spelling).buffer()
    // Match open/truncate-before-charset. Unlike the JVM constructor leak, the owned fd closes on failure.
    sink.use {
        KCharset.encode("", encoding)
        val output = StringBuilder()
        section.save(output)
        it.write(KCharset.encode(output.toString(), encoding))
    }
}
internal actual fun storageUriLocation(uri: KUri): StorageLocation = StorageLocation(nativeFileUriSpelling(uri))
internal actual fun storageFileSpelling(raw: String): String = NativeLexicalFiles.spelling(raw)
internal actual fun storageFileEquals(a: StorageLocation, b: StorageLocation): Boolean = a.spelling == b.spelling
internal actual fun storageFileHash(location: StorageLocation): Int = location.spelling.hashCode() xor 1234321
internal actual fun storageOpenRandomAccess(location: StorageLocation, writable: Boolean): RandomAccessSource =
    RandomAccessSource(NativeLexicalFiles.handle(location.spelling, writable))
internal actual fun storageAbsolute(location: StorageLocation): String = NativeLexicalFiles.absolute(location.spelling)
