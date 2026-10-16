package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * jvmTest adds `src/embeddedResources` to its own classpath (as the JDK-oracle reference), which would hide a
 * regression that puts those files back on the MAIN classpath. This checks the main resources output directly.
 */
class NoEmbeddedResourceOnMainClasspathTest {
    private val mainDir = File(System.getProperty("ksword.mainResourcesDir") ?: error("ksword.mainResourcesDir not set"))

    /** The directory does not exist at all once no main resource is left (K6): no files then. */
    private fun files(): Set<String> =
        mainDir.walkTopDown().filter { it.isFile }.map { it.relativeTo(mainDir).invariantSeparatorsPath }.toSet()

    @Test fun noEmbeddedResourceIsOnTheMainClasspath() {
        val leaked = files().intersect(KswordResources.paths)
        assertEquals("embedded resources leaked onto the main classpath", emptySet<String>(), leaked)
    }

    @Test fun theMainResourcesAreEmptyAfterTheLuceneRemoval() {
        assertEquals("the Lucene .properties files are gone (K6)", emptySet<String>(), files())
    }
}
