package org.crosswire.common.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class KswordResourcesTest {
    private val roots = listOf(File("src/embeddedResources"))
    private val excluded = setOf("AnalyzerFactory.properties", "IndexMetadata.properties", "InstalledIndex.properties")

    private fun sourceFiles(): Map<String, File> = roots.filter { it.isDirectory }.flatMap { root ->
        root.walkTopDown().filter { it.isFile && it.name != "README.md" }.map { it.relativeTo(root).invariantSeparatorsPath to it }
    }.filter { (p, _) -> p !in excluded && (!p.endsWith(".plugin") || p == "InstallManager.plugin") }.toMap()

    @Test fun everyFileRoundTripsByteExact() {
        val files = sourceFiles()
        assertEquals(files.keys, KswordResources.paths)
        for ((path, f) in files) assertArrayEquals(path, f.readBytes(), KswordResources.bytes(path))
    }

    @Test fun unknownPathIsNull() { assertNull(KswordResources.bytes("nope.properties")) }

    @Test fun textDecodesUtf8() {
        assertEquals(File("src/embeddedResources/BibleNames_fi.properties").readText(), KswordResources.text("BibleNames_fi.properties"))
    }
}
