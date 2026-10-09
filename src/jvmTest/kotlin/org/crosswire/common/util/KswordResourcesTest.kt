package org.crosswire.common.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KswordResourcesTest {
    private val root = File("src/embeddedResources")

    // Mirrors the generator's rule: everything under the root except the top-level README.md.
    private fun sourceFiles(): Map<String, File> =
        root.walkTopDown().filter { it.isFile && it.relativeTo(root).invariantSeparatorsPath != "README.md" }
            .associateBy { it.relativeTo(root).invariantSeparatorsPath }

    @Test fun everyFileRoundTripsByteExact() {
        val files = sourceFiles()
        assertEquals(files.keys, KswordResources.paths)
        for ((path, f) in files) assertArrayEquals(path, f.readBytes(), KswordResources.bytes(path))
    }

    @Test fun unknownPathIsNull() { assertNull(KswordResources.bytes("nope.properties")) }

    @Test fun textDecodesUtf8() {
        val expected = File(root, "BibleNames_fi.properties").readText()
        assertTrue("fixture must contain non-ASCII characters", expected.any { it.code > 127 })
        assertEquals(expected, KswordResources.text("BibleNames_fi.properties"))
    }
}
