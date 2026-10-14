package org.crosswire.common.util

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Retained JVM holders must preserve the frozen K4 File/stream contracts, not just parsed bytes. */
class IniJvmBoundaryDifferentialTest {
    @get:Rule val folder = TemporaryFolder()

    private interface Subject {
        val name: String?
        fun get(key: String): String?
        fun replace(key: String, value: String)
        fun load(file: File, encoding: String = "UTF-8")
        fun load(input: InputStream, encoding: String = "UTF-8", filter: Filter<String>? = null)
        fun save(file: File, encoding: String = "UTF-8")
        fun save()
    }

    private fun subject(frozen: Boolean): Subject = if (frozen) {
        object : Subject {
            val section = FrozenIniSection("Saved")
            override val name get() = section.name
            override fun get(key: String) = section.get(key)
            override fun replace(key: String, value: String) { section.replace(key, value) }
            override fun load(file: File, encoding: String) = section.load(file, encoding)
            override fun load(input: InputStream, encoding: String, filter: Filter<String>?) = section.load(input, encoding, filter)
            override fun save(file: File, encoding: String) = section.save(file, encoding)
            override fun save() = section.save()
        }
    } else {
        object : Subject {
            val section = IniSection("Saved")
            override val name get() = section.name
            override fun get(key: String) = section.get(key)
            override fun replace(key: String, value: String) { section.replace(key, value) }
            override fun load(file: File, encoding: String) = IniSectionJvm.load(section, file, encoding)
            override fun load(input: InputStream, encoding: String, filter: Filter<String>?) = IniSectionJvm.load(section, input, encoding, filter)
            override fun save(file: File, encoding: String) = IniSectionJvm.save(section, file, encoding)
            override fun save() = section.save()
        }
    }

    // Each scenario independently verifies the old implementation before invoking the current holder.
    private fun both(scenario: (Subject, File) -> Unit) {
        for (frozen in listOf(true, false)) {
            scenario(subject(frozen), folder.newFolder(if (frozen) "baseline" else "current"))
        }
    }

    private fun lexicalPair(root: File): Pair<File, File> {
        assumeTrue("Literal backslash filenames require Unix", File.separatorChar == '/')
        val literal = File(root, "dir\\name.conf")
        val decoy = File(File(root, "dir").apply { mkdir() }, "name.conf")
        literal.writeText("[Literal]\nk=literal\n")
        decoy.writeText("[Decoy]\nk=decoy\n")
        return literal to decoy
    }

    @Test fun fileLoadUsesLiteralBackslashNotNormalizedDecoy() = both { section, root ->
        val (literal, decoy) = lexicalPair(root)
        section.load(literal)
        assertEquals("Literal", section.name)
        assertEquals("literal", section.get("k"))
        assertEquals("[Literal]\nk=literal\n", literal.readText())
        assertEquals("[Decoy]\nk=decoy\n", decoy.readText())
    }

    @Test fun explicitFileSaveUsesLiteralBackslashNotNormalizedDecoy() = both { section, root ->
        val (literal, decoy) = lexicalPair(root)
        section.replace("k", "written")
        section.save(literal)
        assertEquals("[Saved]${System.lineSeparator()}k = written${System.lineSeparator()}", literal.readText())
        assertEquals("[Decoy]\nk=decoy\n", decoy.readText())
    }

    @Test fun rememberedFileSaveAfterLoadUsesLiteralTarget() = both { section, root ->
        val (literal, decoy) = lexicalPair(root)
        section.load(literal)
        section.replace("k", "remembered")
        section.save()
        assertEquals("[Literal]${System.lineSeparator()}k = remembered${System.lineSeparator()}", literal.readText())
        assertEquals("[Decoy]\nk=decoy\n", decoy.readText())
    }

    @Test fun rememberedFileSaveAfterExplicitSaveUsesLiteralTarget() = both { section, root ->
        val (literal, decoy) = lexicalPair(root)
        section.save(literal)
        section.replace("k", "remembered")
        section.save()
        assertEquals("[Saved]${System.lineSeparator()}k = remembered${System.lineSeparator()}", literal.readText())
        assertEquals("[Decoy]\nk=decoy\n", decoy.readText())
    }

    @Test fun invalidSaveCharsetStillTruncatesOpenedTarget() = both { section, root ->
        val file = File(root, "existing.conf").apply { writeText("must be truncated") }
        assertFailsWith<UnsupportedEncodingException> { section.save(file, "no-such-k4-charset") }
        assertTrue(file.exists())
        assertEquals(0L, file.length(), "Output must open/truncate before charset construction")
    }

    @Test fun outputOpenFailureWinsOverInvalidSaveCharset() = both { section, root ->
        val file = File(File(root, "missing-parent"), "out.conf")
        assertFailsWith<FileNotFoundException> { section.save(file, "no-such-k4-charset") }
        assertTrue(!file.exists())
        assertTrue(!file.parentFile.exists())
    }

    @Test fun failedSaveStillRemembersFileAndCharset() = both { section, root ->
        val file = File(root, "remembered.conf").apply { writeText("old") }
        assertFailsWith<UnsupportedEncodingException> { section.save(file, "no-such-k4-charset") }
        file.writeText("truncate again")
        assertFailsWith<UnsupportedEncodingException> { section.save() }
        assertEquals(0L, file.length())
    }

    /** A long valid prefix exceeds both the legacy 2KiB character buffer and decoder readahead. */
    private fun prefix(): ByteArray = buildString {
        append("[Prefix]\n")
        repeat(6000) { append("key$it=value$it\n") }
    }.toByteArray(Charsets.UTF_8)

    private class ControlledStream(
        val bytes: ByteArray,
        val failAtEnd: Boolean,
        val closeFailure: IOException? = null
    ) : InputStream() {
        val readFailure = IOException("read sentinel")
        var position = 0
        var closeCount = 0
        override fun read(): Int {
            if (position == bytes.size) {
                if (failAtEnd) throw readFailure
                return -1
            }
            return bytes[position++].toInt() and 255
        }
        override fun read(target: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            if (position == bytes.size) return read()
            val count = minOf(length, 2048, bytes.size - position)
            bytes.copyInto(target, offset, position, position + count)
            position += count
            return count
        }
        override fun close() {
            closeCount++
            closeFailure?.let { throw it }
        }
    }

    @Test fun readFailureRetainsAlreadyParsedPrefixAndFilterCallbacks() = both { section, _ ->
        val stream = ControlledStream(prefix(), true)
        val callbacks = mutableListOf<String>()
        val failure = assertFailsWith<IOException> {
            section.load(stream, filter = Filter { key -> callbacks.add(key); true })
        }
        assertSame(stream.readFailure, failure)
        assertEquals("Prefix", section.name)
        assertEquals("value0", section.get("key0"))
        assertEquals("value100", section.get("key100"))
        assertTrue(callbacks.size > 100, "Valid prefix must be parsed before later read failure")
        assertEquals("key0", callbacks.first())
        assertEquals(1, stream.closeCount)
    }

    @Test fun throwingFilterRunsBeforeLaterInputReadFailure() = both { section, _ ->
        val stream = ControlledStream(prefix(), true)
        val filterFailure = IllegalStateException("filter sentinel")
        val callbacks = mutableListOf<String>()
        val failure = assertFailsWith<IllegalStateException> {
            section.load(stream, filter = Filter { key -> callbacks.add(key); throw filterFailure })
        }
        assertSame(filterFailure, failure)
        assertEquals(listOf("key0"), callbacks)
        assertEquals("Prefix", section.name)
        assertNull(section.get("key0"))
        assertTrue(stream.position < stream.bytes.size, "Filter must run before draining the stream")
        assertEquals(1, stream.closeCount)
    }

    @Test fun readAndCloseFailureUsesLegacyClosePriority() = both { section, _ ->
        val closeFailure = IOException("close sentinel")
        val stream = ControlledStream(prefix(), true, closeFailure)
        val failure = assertFailsWith<IOException> { section.load(stream) }
        assertSame(closeFailure, failure, "Legacy finally-close replaces the read failure")
        assertEquals(0, failure.suppressed.size)
        assertEquals("value0", section.get("key0"))
        assertEquals(1, stream.closeCount)
    }

    @Test fun filterAndCloseFailureUsesLegacyClosePriority() = both { section, _ ->
        val closeFailure = IOException("close sentinel")
        val stream = ControlledStream(prefix(), false, closeFailure)
        val failure = assertFailsWith<IOException> {
            section.load(stream, filter = Filter { throw IllegalStateException("filter sentinel") })
        }
        assertSame(closeFailure, failure)
        assertEquals(0, failure.suppressed.size)
        assertEquals("Prefix", section.name)
        assertTrue(stream.position < stream.bytes.size)
        assertEquals(1, stream.closeCount)
    }

    @Test fun invalidLoadCharsetDoesNotConsumeOrCloseCallerStream() = both { section, _ ->
        val stream = ControlledStream(prefix(), false)
        assertFailsWith<UnsupportedEncodingException> { section.load(stream, "no-such-k4-charset") }
        assertEquals(0, stream.position)
        assertEquals(0, stream.closeCount, "Legacy reader construction failed before acquiring ownership")
    }

    @Test fun successfulStreamLoadClosesExactlyOnce() = both { section, _ ->
        val stream = ControlledStream("[Success]\nk=value\n".toByteArray(), false)
        section.load(stream)
        assertEquals("Success", section.name)
        assertEquals("value", section.get("k"))
        assertEquals(1, stream.closeCount)
    }
}
