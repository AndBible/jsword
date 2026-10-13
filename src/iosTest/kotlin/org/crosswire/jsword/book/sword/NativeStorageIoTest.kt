@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.sword

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.test.*
import okio.*
import okio.Path.Companion.toPath
import org.crosswire.common.util.*

class NativeStorageIoTest {
    private fun temporary(block: (StorageLocation) -> Unit) = memScoped {
        val template = (platform.Foundation.NSTemporaryDirectory() + "ksword-XXXXXX").cstr.getPointer(this)
        val root = StorageLocation(mkdtemp(template)!!.toKString())
        try { block(root) } finally {
            fun remove(location: StorageLocation) {
                storageListNames(location)?.forEach { remove(storageChild(location, it)) }
                if (storageDirectory(location)) rmdir(location.spelling) else unlink(location.spelling)
            }
            remove(root)
        }
    }

    @Test fun literalIoAndRandomWritesReachRealFiles() = temporary { root ->
        val file = storageChild(root, "literal\\file")
        assertTrue(rawFileCreate(file)); assertFalse(rawFileCreate(file))
        assertTrue(storageCanRead(file)); assertTrue(storageCanWrite(file))
        assertTrue(storageMkdirs(storageChild(root, "nested/dir")))
        assertFalse(storageMkdirs(storageChild(root, "nested/dir")))
        assertTrue(storageDirectory(storageChild(root, "nested")))
        assertTrue(pathUri(root.spelling.toPath()).getPath()!!.endsWith('/'))
        rawFileWrite(file, byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(1, 2, 3), storageRead(file))
        assertEquals(setOf("literal\\file", "nested"), storageListNames(root)!!.toSet())
        assertNull(storageListNames(file))
        storageOpenRandomAccess(file, true).use { source ->
            source.writeAtCursor(5, byteArrayOf(7))
            assertEquals(6L, source.size())
            assertContentEquals(byteArrayOf(1, 2, 3, 0, 0, 7), source.read(0, 6))
        }
        storageOpenRandomAccess(file, false).use { source ->
            assertFailsWith<IOException> { source.writeAtCursor(0, byteArrayOf(9)) }
        }
        NativeLexicalFiles.handle(file.spelling, true).use { handle ->
            handle.resize(8)
            handle.flush()
            assertContentEquals(byteArrayOf(1, 2, 3, 0, 0, 7, 0, 0), storageRead(file))
            handle.resize(6)
        }
        val closed = storageOpenRandomAccess(file, false)
        closed.close(); closed.close()
        assertFailsWith<IOException> { closed.size() }
        val moved = storageChild(root, "moved\\file")
        assertTrue(storageMove(file, moved)); assertFalse(storageExists(file))
        assertContentEquals(byteArrayOf(1, 2, 3, 0, 0, 7), storageRead(moved))
        val created = storageChild(root, "new")
        storageOpenRandomAccess(created, true).close()
        assertTrue(storageExists(created))
        assertFailsWith<FileNotFoundException> { storageOpenRandomAccess(storageChild(root, "missing"), false) }
    }

    @Test fun saveAndAppendOpenBeforeResolvingEncoding() = temporary { root ->
        val file = storageChild(root, "save\\conf")
        val section = IniSection("Demo").apply { add("Description", "café\nnext") }
        rawFileWrite(file, byteArrayOf(1, 2))
        assertFailsWith<UnsupportedCharsetException> { storageSave(section, file, "invalid") }
        assertContentEquals(byteArrayOf(), storageRead(file))
        storageSave(section, file, "UTF-8")
        val expected = StringBuilder().also { section.save(it) }.toString()
        assertContentEquals(KCharset.encode(expected, "UTF-8"), storageRead(file))
        val append = storageChild(root, "append")
        assertFailsWith<UnsupportedCharsetException> { rawFileAppend(append, 1, "invalid") }
        assertTrue(storageExists(append))
        rawFileAppend(append, 1, "UTF-8")
        assertEquals(rawFileNumber(1, true), storageRead(append).decodeToString())
        rawFilePopulate(file, 3)
        assertContentEquals(ByteArray(18), storageRead(file))
        val log = LoggerFactory.getLogger(NativeStorageIoTest::class)
        assertContentEquals(ByteArray(18), rawFileRead(file, log))
        rawFileWrite(file, byteArrayOf())
        assertContentEquals(byteArrayOf(), rawFileRead(file, log))
        rawFileWriteInc(file, byteArrayOf(8), log)
        assertContentEquals(byteArrayOf(8), storageRead(file))
        rawFileWriteInc(storageChild(root, "absent/child"), byteArrayOf(8), log)
        assertFailsWith<NullPointerException> { rawFileWriteInc(null, byteArrayOf(), log) }
        assertEquals("caféxx", rawFileDecodeName("caféxxxrest".encodeToByteArray()))
    }

    @Test fun fileUrisRejectRegistryAuthoritiesAndKeepLiteralPaths() = temporary { root ->
        val file = storageChild(root, "uri\\file")
        val uri = KUri("file", null, file.spelling, null)
        assertEquals(file, storageUriLocation(uri))
        webResourceOutputSink(uri).buffer().use { it.writeUtf8("literal") }
        assertEquals("literal", storageRead(file).decodeToString())
        webResourceOutputSink(KUri("file://registry:bad${file.spelling.replace("\\", "%5C")}?ignored#ignored")).buffer().use { it.writeUtf8("decoded path") }
        assertEquals("decoded path", storageRead(file).decodeToString())
        assertTrue(pathUri("relative".toPath()).getPath()!!.startsWith('/'))
        for (text in listOf("file://registry:bad/path", "file://host/path", "file:/path?q", "file:/path#f", "file:opaque", "/relative", "http:/path")) {
            assertFailsWith<IllegalArgumentException>(text) { storageUriLocation(KUri(text)) }
            assertFailsWith<IllegalArgumentException>(text) { validatedFileUriPath(KUri(text)) }
        }
    }
}
