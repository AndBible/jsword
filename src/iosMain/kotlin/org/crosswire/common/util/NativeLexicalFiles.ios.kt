@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.common.util

import kotlinx.cinterop.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.*
import platform.posix.*

/** Literal Unix names. No Path roundtrip: backslashes and dot segments are significant. */
internal object NativeLexicalFiles {
    fun spelling(raw: String): String {
        val normalized = raw.replace(Regex("/+"), "/")
        return if (normalized.length > 1) normalized.trimEnd('/') else normalized
    }

    fun absolute(path: String): String {
        if (path.startsWith('/')) return path
        val cwd = getcwd(null, 0u) ?: throw failure("getcwd")
        val root = try { cwd.toKString() } finally { free(cwd) }
        return if (path.isEmpty()) root else spelling("$root/$path")
    }

    fun name(path: String): String = path.substringAfterLast('/')
    fun parent(path: String): String? {
        val at = path.lastIndexOf('/')
        return when { at < 0 -> null; at == 0 && path.length == 1 -> null; at == 0 -> "/"; else -> path.substring(0, at) }
    }
    fun child(parent: String?, child: String): String {
        val part = spelling(child)
        if (parent == null) return part
        if (part.isEmpty()) return parent
        return spelling(parent + (if (part.startsWith('/')) "" else "/") + part)
    }

    private fun valid(path: String): Boolean = '\u0000' !in path
    fun exists(path: String): Boolean = valid(path) && access(path, F_OK) == 0
    fun canRead(path: String): Boolean = valid(path) && access(path, R_OK) == 0
    fun canWrite(path: String): Boolean = valid(path) && access(path, W_OK) == 0
    fun directory(path: String): Boolean = memScoped {
        val info = alloc<stat>()
        valid(path) && stat(path, info.ptr) == 0 && (info.st_mode.toInt() and S_IFMT) == S_IFDIR
    }
    fun length(path: String): Long = memScoped {
        val info = alloc<stat>()
        if (valid(path) && stat(path, info.ptr) == 0) info.st_size else 0L
    }
    fun mkdirs(path: String): Boolean {
        if (exists(path) || !valid(path)) return false
        if (mkdir(path, 511.convert()) == 0) return true
        val up = parent(absolute(path)) ?: return false
        return (mkdirs(up) || directory(up)) && mkdir(path, 511.convert()) == 0
    }
    fun list(path: String): List<String>? {
        if (!valid(path)) return null
        val dir = opendir(path) ?: return null
        try {
            return buildList {
                while (true) {
                    val entry = readdir(dir) ?: break
                    val name = entry.pointed.d_name.toKString()
                    if (name != "." && name != "..") add(name)
                }
            }
        } finally { closedir(dir) }
    }
    fun move(from: String, to: String): Boolean = valid(from) && valid(to) && rename(from, to) == 0
    fun create(path: String): Boolean {
        if (!valid(path)) throw IOException("Invalid file path")
        val fd = open(path, O_WRONLY or O_CREAT or O_EXCL, 438)
        if (fd < 0) {
            if (errno == EEXIST) return false
            throw failure("create $path")
        }
        closeChecked(fd)
        return true
    }

    fun handle(path: String, writable: Boolean): FileHandle = Descriptor(openFile(path, if (writable) O_RDWR or O_CREAT else O_RDONLY), writable)
    fun source(path: String): Source = handle(path, false).let { handle ->
        // A FileHandle source retains its descriptor after the owner's close until the source closes.
        val source = handle.source()
        handle.close()
        source
    }
    fun sink(path: String, append: Boolean = false): Sink {
        val descriptor = Descriptor(openFile(path, O_WRONLY or O_CREAT or (if (append) O_APPEND else O_TRUNC)), true)
        // O_APPEND is kernel-owned; this offset is only meaningful for non-append output.
        return object : Sink {
            private var position = 0L
            private var closed = false
            override fun write(source: Buffer, byteCount: Long) {
                check(!closed) { "closed" }
                require(byteCount >= 0 && byteCount <= source.size)
                var remaining = byteCount
                while (remaining > 0) {
                    val bytes = source.readByteArray(minOf(remaining, 8192L))
                    descriptor.write(position, bytes, 0, bytes.size)
                    position += bytes.size
                    remaining -= bytes.size
                }
            }
            // FileOutputStream.flush is a no-op; only random-access handle flush calls fsync.
            override fun flush() = Unit
            override fun timeout(): Timeout = Timeout.NONE
            override fun close() {
                if (!closed) { closed = true; descriptor.close() }
            }
        }
    }
    fun read(path: String): ByteArray = source(path).buffer().use { it.readByteArray() }

    private fun openFile(path: String, flags: Int): Int {
        if (!valid(path)) throw FileNotFoundException("Invalid file path: $path")
        val fd = open(path, flags, 438)
        if (fd < 0) throw FileNotFoundException("$path: ${strerror(errno)?.toKString()}")
        val isDirectory = memScoped {
            val info = alloc<stat>()
            if (fstat(fd, info.ptr) != 0) {
                val error = failure("fstat $path")
                close(fd)
                throw error
            }
            (info.st_mode.toInt() and S_IFMT) == S_IFDIR
        }
        if (isDirectory) {
            close(fd)
            throw FileNotFoundException("$path: Is a directory")
        }
        return fd
    }
    private fun failure(operation: String): IOException = IOException("$operation: ${strerror(errno)?.toKString()}")
    private fun closeChecked(fd: Int) { if (close(fd) != 0) throw failure("close") }

    private class Descriptor(private val fd: Int, writable: Boolean) : FileHandle(writable) {
        private val descriptorLock = SynchronizedObject()
        override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int = synchronized(descriptorLock) {
            if (byteCount == 0) return@synchronized 0
            if (lseek(fd, fileOffset, SEEK_SET) < 0) throw failure("seek")
            val count = array.usePinned { read(fd, it.addressOf(arrayOffset), byteCount.toULong()) }
            if (count < 0) throw failure("read")
            if (count == 0L) -1 else count.toInt()
        }
        override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) = synchronized(descriptorLock) {
            if (lseek(fd, fileOffset, SEEK_SET) < 0) throw failure("seek")
            var done = 0
            array.usePinned { pin ->
                while (done < byteCount) {
                    val count = write(fd, pin.addressOf(arrayOffset + done), (byteCount - done).toULong())
                    if (count <= 0) throw failure("write")
                    done += count.toInt()
                }
            }
        }
        override fun protectedSize(): Long = synchronized(descriptorLock) {
            memScoped {
                val info = alloc<stat>()
                if (fstat(fd, info.ptr) != 0) throw failure("fstat")
                info.st_size
            }
        }
        override fun protectedResize(size: Long) = synchronized(descriptorLock) {
            if (ftruncate(fd, size) != 0) throw failure("resize")
        }
        override fun protectedFlush() = synchronized(descriptorLock) { if (fsync(fd) != 0) throw failure("flush") }
        override fun protectedClose() = synchronized(descriptorLock) { closeChecked(fd) }
    }
}
