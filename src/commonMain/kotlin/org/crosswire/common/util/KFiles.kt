package org.crosswire.common.util

import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * Common file operations on okio, with the exact return-value conventions of the `java.io.File` calls that
 * `NetUtil` and `FileUtil` used before (booleans instead of exceptions where `File` returned booleans).
 * Every function takes the [FileSystem] so tests can use a fake one.
 */
object KFiles {
    fun exists(path: Path, fs: FileSystem = FileSystem.SYSTEM): Boolean = fs.metadataOrNull(path) != null

    fun isDirectory(path: Path, fs: FileSystem = FileSystem.SYSTEM): Boolean =
        fs.metadataOrNull(path)?.isDirectory == true

    fun isFile(path: Path, fs: FileSystem = FileSystem.SYSTEM): Boolean =
        fs.metadataOrNull(path)?.isRegularFile == true

    /** Size in bytes, or -1 when absent or unknown. */
    fun size(path: Path, fs: FileSystem = FileSystem.SYSTEM): Long = fs.metadataOrNull(path)?.size ?: -1L

    /** Modification time in epoch milliseconds, 0 when absent or unknown, like `File.lastModified`. */
    fun lastModifiedMillis(path: Path, fs: FileSystem = FileSystem.SYSTEM): Long =
        fs.metadataOrNull(path)?.lastModifiedAtMillis ?: 0L

    fun readBytes(path: Path, fs: FileSystem = FileSystem.SYSTEM): ByteArray = fs.read(path) { readByteArray() }

    fun readUtf8(path: Path, fs: FileSystem = FileSystem.SYSTEM): String = fs.read(path) { readUtf8() }

    fun write(path: Path, bytes: ByteArray, fs: FileSystem = FileSystem.SYSTEM) {
        fs.write(path) { write(bytes) }
    }

    /** Child names (not paths) of [dir]; empty when [dir] is not a readable directory. */
    fun list(dir: Path, fs: FileSystem = FileSystem.SYSTEM): List<String> =
        fs.listOrNull(dir)?.map { it.name } ?: emptyList()

    fun createDirectories(path: Path, fs: FileSystem = FileSystem.SYSTEM) {
        fs.createDirectories(path, mustCreate = false)
    }

    /**
     * Creates an empty file unless a regular file is already there (an existing file is left untouched).
     * Throws [IOException] when [path] is a directory or its parent is missing.
     */
    fun createEmptyFile(path: Path, fs: FileSystem = FileSystem.SYSTEM) {
        if (isFile(path, fs)) return
        fs.write(path) { }
    }

    /** Non-recursive delete of a file or empty directory. False when absent, non-empty or on any failure. */
    fun delete(path: Path, fs: FileSystem = FileSystem.SYSTEM): Boolean {
        if (fs.metadataOrNull(path) == null) return false
        return try {
            fs.delete(path, mustExist = false)
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Deletes a file or a directory with all its contents.
     * @return the paths that could not be deleted, children before their parent (the order `FileUtil.delete` gave)
     */
    fun deleteRecursively(path: Path, fs: FileSystem = FileSystem.SYSTEM): List<Path> {
        val failures = ArrayList<Path>()
        if (isDirectory(path, fs)) deleteContents(path, fs, failures)
        if (!delete(path, fs)) failures.add(path)
        return failures
    }

    private fun deleteContents(dir: Path, fs: FileSystem, failures: MutableList<Path>) {
        for (child in fs.listOrNull(dir).orEmpty()) {
            if (isDirectory(child, fs)) deleteContents(child, fs, failures)
            if (!delete(child, fs)) failures.add(child)
        }
    }

    /** Renames [from] to [to], replacing an existing file. False on any failure, like `File.renameTo`. */
    fun move(from: Path, to: Path, fs: FileSystem = FileSystem.SYSTEM): Boolean {
        if (fs.metadataOrNull(from) == null) return false
        return try {
            fs.atomicMove(from, to)
            true
        } catch (e: IOException) {
            false
        }
    }
}
