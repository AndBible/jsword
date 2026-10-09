package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * jvmTest adds `src/embeddedResources` to its own classpath (as the JDK-oracle reference), which would hide a
 * regression that puts those files back on the MAIN classpath. This checks the main resources output directly.
 */
class NoEmbeddedResourceOnMainClasspathTest {
    private val mainDir = File(System.getProperty("ksword.mainResourcesDir") ?: error("ksword.mainResourcesDir not set"))

    private fun files(): Set<String> =
        mainDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(mainDir).invariantSeparatorsPath }.toSet()

    @Test fun mainResourcesDirExists() {
        assertTrue("$mainDir", mainDir.isDirectory)
    }

    @Test fun noEmbeddedResourceIsOnTheMainClasspath() {
        val leaked = files().intersect(KswordResources.paths)
        assertEquals("embedded resources leaked onto the main classpath", emptySet<String>(), leaked)
    }

    @Test fun onlyTheLuceneFilesAreThere() {
        assertEquals(setOf("AnalyzerFactory.properties", "IndexMetadata.properties", "InstalledIndex.properties"), files())
    }
}
