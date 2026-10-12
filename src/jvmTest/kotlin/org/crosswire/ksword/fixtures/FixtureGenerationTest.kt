package org.crosswire.ksword.fixtures

import okio.Path.Companion.toPath
import java.io.File
import java.nio.file.Files
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixtureGenerationTest {
    private val committed = File(System.getProperty("ksword.fixturesDir") ?: error("ksword.fixturesDir not set"))
    private fun generate(directory: File) {
        val root = File(directory, "space dir/ä")
        val manifest = FixtureSet.MODULES.flatMap { module ->
            FixtureModuleWriter.write(root.path.toPath(), module).map { (key, raw) ->
                "${module.initials}\t$key\t${Base64.getEncoder().encodeToString(raw.toByteArray())}"
            }
        }
        File(directory, "manifest.tsv").writeText(manifest.joinToString("\n", postfix = "\n"))
    }
    private fun files(root: File) = root.walkTopDown().filter { it.isFile }.associateBy { it.relativeTo(root).invariantSeparatorsPath }

    @Test fun generatedBytesMatchCommittedTree() {
        val temporary = Files.createTempDirectory("k4-fixtures-").toFile()
        try {
            generate(temporary)
            if (java.lang.Boolean.getBoolean("k4.fixtures.write")) {
                committed.deleteRecursively()
                temporary.copyRecursively(committed)
            }
            val expected = files(temporary); val actual = files(committed)
            assertEquals(expected.keys, actual.keys, "fixture file inventory")
            expected.forEach { (path, file) -> assertContentEquals(file.readBytes(), actual.getValue(path).readBytes(), path) }
            assertTrue(actual.values.sumOf { it.length() } < 2 * 1024 * 1024, "fixtures exceed 2 MB")
        } finally { temporary.deleteRecursively() }
    }

    @Test fun fixtureDefinitionsPinFormatsAndEdgeContent() {
        assertEquals(15, FixtureSet.MODULES.size)
        val modules = FixtureSet.MODULES.associateBy { it.initials }
        assertEquals(listOf("Gen.1.1", "Gen.1.2", "Gen.1.3", "John.1.1", "John.1.2", "John.1.3"), modules.getValue("K4RawText").entries.map { it.first })
        assertEquals(listOf("CompressType=LZSS", "BlockType=CHAPTER"), modules.getValue("K4ZTextChapterLzss").extras)
        assertEquals(listOf("01.01", "02.29", "12.31"), modules.getValue("K4Devotion").entries.map { it.first })
        assertEquals(listOf("/Part 1/Chapter 1", "/Part 1/Chapter 2", "/Part 2"), modules.getValue("K4GenBook").entries.map { it.first })
        assertEquals("“In the beginning” – the Word — God… é", modules.getValue("K4Latin1").entries.single().second)
    }
}
