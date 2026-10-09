package org.crosswire.common.util

import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Mirrors the cases pinned by the JVM `NetUtilFileCharacterizationTest`, on a [FakeFileSystem]. */
class KFilesTest {
    private val fs = FakeFileSystem()
    private val root: Path = "/tmp/kf".toPath()

    init {
        fs.createDirectories(root)
    }

    private fun file(name: String, text: String = ""): Path = (root / name).also { fs.write(it) { writeUtf8(text) } }

    @Test
    fun existsIsFileIsDirectory() {
        val f = file("a.txt")
        val d = (root / "d").also { fs.createDirectory(it) }
        val missing = root / "nope"
        assertTrue(KFiles.isFile(f, fs)); assertFalse(KFiles.isFile(d, fs)); assertFalse(KFiles.isFile(missing, fs))
        assertFalse(KFiles.isDirectory(f, fs)); assertTrue(KFiles.isDirectory(d, fs)); assertFalse(KFiles.isDirectory(missing, fs))
        assertTrue(KFiles.exists(f, fs)); assertTrue(KFiles.exists(d, fs)); assertFalse(KFiles.exists(missing, fs))
    }

    @Test
    fun sizeAndLastModified() {
        val f = (root / "f").also { fs.write(it) { write(ByteArray(1234)) } }
        assertEquals(1234L, KFiles.size(f, fs))
        assertEquals(-1L, KFiles.size(root / "nope", fs))
        assertEquals(0L, KFiles.lastModifiedMillis(root / "nope", fs))
    }

    @Test
    fun readAndWriteRoundTrip() {
        val p = root / "rw"
        KFiles.write(p, byteArrayOf(1, 2, 3), fs)
        assertContentEquals(byteArrayOf(1, 2, 3), KFiles.readBytes(p, fs))
        KFiles.write(p, "héllo".encodeToByteArray(), fs)
        assertEquals("héllo", KFiles.readUtf8(p, fs))
        assertFailsWith<IOException> { KFiles.readBytes(root / "nope", fs) }
    }

    @Test
    fun createDirectoriesNestedAndIdempotent() {
        val nested = root / "a" / "b" / "c"
        KFiles.createDirectories(nested, fs)
        assertTrue(KFiles.isDirectory(nested, fs))
        KFiles.createDirectories(nested, fs)
        assertTrue(KFiles.isDirectory(nested, fs))
    }

    @Test
    fun createEmptyFileNewAndExisting() {
        val p = root / "new.txt"
        KFiles.createEmptyFile(p, fs)
        assertTrue(KFiles.isFile(p, fs))
        assertEquals(0L, KFiles.size(p, fs))
        KFiles.write(p, "keep".encodeToByteArray(), fs)
        KFiles.createEmptyFile(p, fs)
        assertEquals("keep", KFiles.readUtf8(p, fs))
    }

    @Test
    fun createEmptyFileFailsOnDirectoryAndMissingParent() {
        val d = (root / "d").also { fs.createDirectory(it) }
        assertFailsWith<IOException> { KFiles.createEmptyFile(d, fs) }
        assertFailsWith<IOException> { KFiles.createEmptyFile(root / "missing" / "new.txt", fs) }
        assertFalse(KFiles.exists(root / "missing", fs))
    }

    @Test
    fun moveCases() {
        val src = file("src.txt", "x")
        val dst = root / "dst.txt"
        assertTrue(KFiles.move(src, dst, fs))
        assertFalse(KFiles.exists(src, fs))
        assertEquals("x", KFiles.readUtf8(dst, fs))

        val src2 = file("src2.txt", "new")
        assertTrue(KFiles.move(src2, dst, fs))
        assertEquals("new", KFiles.readUtf8(dst, fs))

        assertFalse(KFiles.move(root / "nope", root / "other", fs))
        assertFalse(KFiles.exists(root / "other", fs))
    }

    @Test
    fun deleteCases() {
        val f = file("f")
        assertTrue(KFiles.delete(f, fs)); assertFalse(KFiles.exists(f, fs))
        val empty = (root / "empty").also { fs.createDirectory(it) }
        assertTrue(KFiles.delete(empty, fs)); assertFalse(KFiles.exists(empty, fs))
        val full = (root / "full").also { fs.createDirectory(it) }
        file("full/child", "c")
        assertFalse(KFiles.delete(full, fs))
        assertTrue(KFiles.isDirectory(full, fs)); assertTrue(KFiles.exists(full / "child", fs))
        assertFalse(KFiles.delete(root / "nope", fs))
    }

    @Test
    fun listCases() {
        val d = (root / "d").also { fs.createDirectory(it) }
        file("d/a.txt"); file("d/b.dat"); fs.createDirectory(d / "sub")
        assertEquals(setOf("a.txt", "b.dat", "sub"), KFiles.list(d, fs).toSet())
        assertEquals(emptyList(), KFiles.list(file("plain"), fs))
        assertEquals(emptyList(), KFiles.list(root / "nope", fs))
    }

    @Test
    fun deleteRecursivelyTree() {
        val tree = (root / "tree").also { fs.createDirectory(it) }
        fs.createDirectories(tree / "a" / "b")
        file("tree/a/f1"); file("tree/a/b/f2"); file("tree/f3")
        assertEquals(emptyList(), KFiles.deleteRecursively(tree, fs))
        assertFalse(KFiles.exists(tree, fs))
    }

    @Test
    fun deleteRecursivelySingleFileAndMissing() {
        val f = file("f")
        assertEquals(emptyList(), KFiles.deleteRecursively(f, fs))
        assertFalse(KFiles.exists(f, fs))
        val missing = root / "nope"
        assertEquals(listOf(missing), KFiles.deleteRecursively(missing, fs))
    }

    @Test
    fun deleteRecursivelyReportsFailuresChildrenFirst() {
        val tree = (root / "tree").also { fs.createDirectory(it) }
        val stuck = file("tree/stuck")
        val ok = file("tree/ok")
        // An open handle makes FakeFileSystem refuse to delete (like a locked file), a proxy for permission failures.
        fs.allowDeletingOpenFiles = false
        val handle = fs.source(stuck)
        try {
            val failures = KFiles.deleteRecursively(tree, fs)
            assertEquals(listOf(stuck, tree), failures)
            assertFalse(KFiles.exists(ok, fs))
        } finally {
            handle.close()
        }
    }

    @Test
    fun systemDefaultFileSystemIsUsable() {
        assertTrue(KFiles.exists(FileSystem.SYSTEM_TEMPORARY_DIRECTORY, FileSystem.SYSTEM))
    }
}
