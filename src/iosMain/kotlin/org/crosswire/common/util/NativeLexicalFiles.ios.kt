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
        if (part.isEmpty()) return if (parent.isEmpty()) "/" else parent
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
        val canonical = try { canonicalFallback(path) } catch (_: IOException) { return false }
        val up = parent(canonical) ?: return false
        return (mkdirs(up) || exists(up)) && mkdir(canonical, 511.convert()) == 0
    }

    /** File.mkdirs canonicalizes only after its direct mkdir fails, including nonexistent tails. */
    private fun canonicalFallback(path: String): String {
        var prefix = absolute(path)
        val tail = ArrayDeque<String>()
        var resolved: String
        while (true) {
            val real = realpath(prefix, null)
            if (real != null) {
                resolved = try { real.toKString() } finally { free(real) }
                break
            }
            if (errno != ENOENT && errno != ENOTDIR) throw failure("canonicalize $path")
            val up = parent(prefix) ?: throw failure("canonicalize $path")
            tail.addFirst(name(prefix))
            prefix = up
        }
        for (part in tail) {
            resolved = when (part) {
                "", "." -> resolved
                ".." -> parent(resolved) ?: "/"
                else -> if (resolved == "/") "/$part" else "$resolved/$part"
            }
            // JDK canonicalization collapses the unresolved suffix lexically, without resolving newly exposed links.
        }
        return resolved
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
    fun source(path: String): Source {
        val fd = openFile(path, O_RDONLY)
        val streamLock = SynchronizedObject()
        var closed = false
        return object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long = synchronized(streamLock) {
                check(!closed) { "closed" }
                require(byteCount >= 0)
                if (byteCount == 0L) return@synchronized 0L
                val bytes = ByteArray(minOf(byteCount, 8192L).toInt())
                val count = bytes.usePinned { platform.posix.read(fd, it.addressOf(0), bytes.size.toULong()) }
                if (count < 0) throw failure("read")
                if (count == 0L) -1L else {
                    sink.write(bytes, 0, count.toInt())
                    count
                }
            }
            override fun timeout(): Timeout = Timeout.NONE
            override fun close() = synchronized(streamLock) {
                if (!closed) { closed = true; closeChecked(fd) }
            }
        }
    }
    fun sink(path: String, append: Boolean = false): Sink {
        val fd = openFile(path, O_WRONLY or O_CREAT or (if (append) O_APPEND else O_TRUNC))
        val streamLock = SynchronizedObject()
        return object : Sink {
            private var closed = false
            override fun write(source: Buffer, byteCount: Long) = synchronized(streamLock) {
                check(!closed) { "closed" }
                require(byteCount >= 0 && byteCount <= source.size)
                var remaining = byteCount
                while (remaining > 0) {
                    val bytes = source.readByteArray(minOf(remaining, 8192L))
                    var done = 0
                    bytes.usePinned { pin ->
                        while (done < bytes.size) {
                            val count = platform.posix.write(fd, pin.addressOf(done), (bytes.size - done).toULong())
                            if (count <= 0) throw failure("write")
                            done += count.toInt()
                        }
                    }
                    remaining -= bytes.size
                }
            }
            // FileOutputStream.flush is a no-op; only random-access handle flush calls fsync.
            override fun flush() = Unit
            override fun timeout(): Timeout = Timeout.NONE
            override fun close() = synchronized(streamLock) {
                if (!closed) { closed = true; closeChecked(fd) }
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
