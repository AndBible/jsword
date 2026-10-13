package org.crosswire.common.util

import java.io.File
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the exact file-scheme behaviour of [NetUtil] and [FileUtil.delete] (return values and exception types),
 * so that delegating them to the common `KFiles` stays behaviour-neutral.
 */
class NetUtilFileCharacterizationTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun uri(f: File): URI = NetUtilJvm.getURI(f)

    @Test
    fun isFileAndIsDirectory() {
        val file = tmp.newFile("a.txt")
        val dir = tmp.newFolder("d")
        val missing = File(tmp.root, "nope")
        assertTrue(NetUtil.isFile(uri(file)))
        assertFalse(NetUtil.isFile(uri(dir)))
        assertFalse(NetUtil.isFile(uri(missing)))
        assertFalse(NetUtil.isDirectory(uri(file)))
        assertTrue(NetUtil.isDirectory(uri(dir)))
        assertFalse(NetUtil.isDirectory(uri(missing)))
        assertFalse(NetUtil.isDirectory(URI("http://example.org/x")))
    }

    @Test
    fun makeDirectoryNestedAndIdempotent() {
        val nested = File(tmp.root, "a/b/c")
        NetUtil.makeDirectory(uri(nested))
        assertTrue(nested.isDirectory)
        NetUtil.makeDirectory(uri(nested)) // already a directory: no-op
        assertTrue(nested.isDirectory)
    }

    @Test
    fun makeDirectoryOnFileThrowsMalformedUrl() {
        val file = tmp.newFile("f")
        val ex = assertFailsWith<MalformedURLException> { NetUtil.makeDirectory(uri(file)) }
        assertTrue(ex.message!!.contains("is a file"), ex.message)
    }

    @Test
    fun makeDirectoryNonFileSchemeThrowsMalformedUrl() {
        val ex = assertFailsWith<MalformedURLException> { NetUtil.makeDirectory(URI("http://example.org/x")) }
        assertTrue(ex.message!!.contains("is not a file"), ex.message)
    }

    @Test
    fun makeDirectoryUnderAFileThrowsCouldNotBeCreated() {
        val file = tmp.newFile("f")
        val ex = assertFailsWith<MalformedURLException> { NetUtil.makeDirectory(uri(File(file, "sub"))) }
        assertTrue(ex.message!!.contains("could not be created as a directory"), ex.message)
    }

    @Test
    fun makeFileNewAndExisting() {
        val f = File(tmp.root, "new.txt")
        NetUtil.makeFile(uri(f))
        assertTrue(f.isFile)
        assertEquals(0L, f.length())
        f.writeText("keep")
        NetUtil.makeFile(uri(f)) // existing file is left untouched
        assertEquals("keep", f.readText())
    }

    @Test
    fun makeFileOnDirectoryThrowsMalformedUrl() {
        val dir = tmp.newFolder("d")
        val ex = assertFailsWith<MalformedURLException> { NetUtil.makeFile(uri(dir)) }
        assertTrue(ex.message!!.contains("is a directory"), ex.message)
    }

    @Test
    fun makeFileInMissingParentThrowsIoException() {
        val f = File(tmp.root, "missing/new.txt")
        assertFailsWith<IOException> { NetUtil.makeFile(uri(f)) }
        assertFalse(f.exists())
    }

    @Test
    fun moveFileToNewName() {
        val src = tmp.newFile("src.txt").apply { writeText("x") }
        val dst = File(tmp.root, "dst.txt")
        assertTrue(NetUtil.move(uri(src), uri(dst)))
        assertFalse(src.exists())
        assertEquals("x", dst.readText())
    }

    @Test
    fun moveOntoExistingFileReplaces() {
        val src = tmp.newFile("src.txt").apply { writeText("new") }
        val dst = tmp.newFile("dst.txt").apply { writeText("old") }
        assertTrue(NetUtil.move(uri(src), uri(dst)))
        assertFalse(src.exists())
        assertEquals("new", dst.readText())
    }

    @Test
    fun moveMissingSourceReturnsFalse() {
        val dst = File(tmp.root, "dst.txt")
        assertFalse(NetUtil.move(uri(File(tmp.root, "nope")), uri(dst)))
        assertFalse(dst.exists())
    }

    @Test
    fun moveNonFileSchemeThrowsMalformedUrl() {
        val f = tmp.newFile("f")
        assertFailsWith<MalformedURLException> { NetUtil.move(URI("http://example.org/x"), uri(f)) }
    }

    @Test
    fun deleteFileEmptyDirNonEmptyDirMissing() {
        val file = tmp.newFile("f")
        assertTrue(NetUtil.delete(uri(file)))
        assertFalse(file.exists())

        val empty = tmp.newFolder("empty")
        assertTrue(NetUtil.delete(uri(empty)))
        assertFalse(empty.exists())

        val full = tmp.newFolder("full")
        File(full, "child").writeText("c")
        assertFalse(NetUtil.delete(uri(full)))
        assertTrue(full.isDirectory)
        assertTrue(File(full, "child").exists())

        assertFalse(NetUtil.delete(uri(File(tmp.root, "nope"))))
    }

    @Test
    fun deleteNonFileSchemeThrowsMalformedUrl() {
        assertFailsWith<MalformedURLException> { NetUtil.delete(URI("http://example.org/x")) }
    }

    @Test
    fun listByFileWithoutAndWithFilter() {
        val dir = tmp.newFolder("d")
        File(dir, "a.txt").writeText("a")
        File(dir, "b.dat").writeText("b")
        File(dir, "sub").mkdir()
        assertEquals(setOf("a.txt", "b.dat", "sub"), NetUtil.listByFile(uri(dir), DefaultURIFilter())!!.toSet())
        val onlyTxt = object : URIFilter {
            override fun accept(name: String?) = name!!.endsWith(".txt")
        }
        assertEquals(setOf("a.txt"), NetUtil.listByFile(uri(dir), onlyTxt)!!.toSet())
        val none = object : URIFilter {
            override fun accept(name: String?) = false
        }
        assertEquals(0, NetUtil.listByFile(uri(dir), none)!!.size)
    }

    @Test
    fun listByFileOnNonDirectoryThrowsMalformedUrl() {
        val file = tmp.newFile("f")
        val ex = assertFailsWith<MalformedURLException> { NetUtil.listByFile(uri(file), DefaultURIFilter()) }
        assertTrue(ex.message!!.contains("is not a directory"), ex.message)
        assertFailsWith<MalformedURLException> { NetUtil.listByFile(uri(File(tmp.root, "nope")), DefaultURIFilter()) }
    }

    @Test
    fun sizeAndLastModifiedForFileAndMissing() {
        val file = tmp.newFile("f").apply { writeBytes(ByteArray(1234)); setLastModified(1_500_000_000_000L) }
        assertEquals(1234, NetUtil.getSize(uri(file)))
        assertEquals(1_500_000_000_000L, NetUtil.getLastModified(uri(file)))
        val missing = uri(File(tmp.root, "nope"))
        assertEquals(PINNED_MISSING_SIZE, NetUtil.getSize(missing))
        assertEquals(PINNED_MISSING_MODIFIED, NetUtil.getLastModified(missing))
    }

    @Test
    fun fileUtilDeleteTree() {
        val root = tmp.newFolder("tree")
        File(root, "a/b").mkdirs()
        File(root, "a/f1").writeText("1")
        File(root, "a/b/f2").writeText("2")
        File(root, "f3").writeText("3")
        assertEquals(emptyList(), FileUtilJvm.delete(root))
        assertFalse(root.exists())
    }

    @Test
    fun fileUtilDeleteSingleFileAndMissing() {
        val f = tmp.newFile("f")
        assertEquals(emptyList(), FileUtilJvm.delete(f))
        assertFalse(f.exists())
        val missing = File(tmp.root, "nope")
        assertEquals(listOf(missing), FileUtilJvm.delete(missing))
    }

    @Test
    fun fileUtilDeleteReadOnlySubdirectoryReportsFailuresInOrder() {
        val root = tmp.newFolder("tree")
        val ro = File(root, "ro").also { it.mkdir() }
        val inner = File(ro, "inner").also { it.writeText("x") }
        val other = File(root, "other").also { it.writeText("y") }
        assertTrue(ro.setWritable(false))
        try {
            val permissionsEnforced = !File(ro, "probe").let {
                runCatching { it.createNewFile() }.getOrDefault(false).also { _ -> it.delete() }
            }
            Assume.assumeTrue("running as root: permissions are not enforced, nothing to pin", permissionsEnforced)
            val failures = FileUtilJvm.delete(root)
            // children first (inner, then the read-only dir), then the root which is non-empty now
            assertEquals(listOf(inner, ro, root), failures)
            assertFalse(other.exists())
        } finally {
            ro.setWritable(true)
        }
    }

    @Test
    fun listByFileWithNullFilter() {
        val empty = tmp.newFolder("empty")
        assertEquals(0, NetUtil.listByFile(uri(empty), null)!!.size)
        val full = tmp.newFolder("full")
        File(full, "a").writeText("a")
        // the old FilenameFilter wrapper dereferenced the null filter for every entry
        assertFailsWith<NullPointerException> { NetUtil.listByFile(uri(full), null) }
    }

    @Test
    fun listByFileOnUnreadableDirectoryReturnsNull() {
        val dir = tmp.newFolder("locked")
        File(dir, "a").writeText("a")
        assertTrue(dir.setReadable(false))
        try {
            Assume.assumeTrue("running as root: permissions are not enforced", dir.list() == null)
            // File.list() returned null for a directory that cannot be listed, and listByFile passed that on
            assertEquals(null, NetUtil.listByFile(uri(dir), DefaultURIFilter()))
        } finally {
            dir.setReadable(true)
        }
    }

    @Test
    fun uriWithPercentEncodedSpaceAndNonAsciiNameThroughDelegatingOps() {
        val dir = tmp.newFolder("my dir")
        val file = File(dir, "tiedosto \u00e4\u00f6 \u4e2d.txt").apply { writeBytes(ByteArray(7)) }
        val u = URI("file:" + file.toURI().rawPath) // percent-encoded form, as it arrives from the outside
        assertTrue(u.rawPath.contains("%20"), u.rawPath)
        assertTrue(NetUtil.isFile(u))
        assertTrue(NetUtil.isDirectory(NetUtilJvm.getURI(dir)))
        assertEquals(7, NetUtil.getSize(u))
        assertEquals(file.lastModified(), NetUtil.getLastModified(u))
        assertEquals(setOf(file.name), NetUtil.listByFile(NetUtilJvm.getURI(dir), DefaultURIFilter())!!.toSet())
        val moved = File(dir, "uusi \u00e5.txt")
        assertTrue(NetUtil.move(u, uri(moved)))
        assertTrue(moved.isFile)
        assertFalse(file.exists())
        assertTrue(NetUtil.delete(uri(moved)))
        assertFalse(moved.exists())
    }

    @Test
    fun moveFileOntoExistingDirectoryReturnsFalseAndChangesNothing() {
        val src = tmp.newFile("src.txt").apply { writeText("x") }
        val dstDir = tmp.newFolder("dstDir")
        assertFalse(File(src.path).renameTo(dstDir), "File.renameTo baseline")
        assertFalse(NetUtil.move(uri(src), uri(dstDir)))
        assertEquals("x", src.readText())
        assertTrue(dstDir.isDirectory)
    }

    @Test
    fun sizeAboveIntRangeIsMinusOne() {
        assertEquals(-1, NetUtil.fileSizeToInt(Int.MAX_VALUE.toLong() + 1))
        assertEquals(-1, NetUtil.fileSizeToInt(5L shl 30))
        assertEquals(Int.MAX_VALUE, NetUtil.fileSizeToInt(Int.MAX_VALUE.toLong()))
        assertEquals(0, NetUtil.fileSizeToInt(0))
    }

    companion object {
        const val PINNED_MISSING_SIZE = 0
        const val PINNED_MISSING_MODIFIED = 0L
    }
}
