package org.crosswire.ksword.parity

import java.util.jar.JarFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.crosswire.jsword.book.Books

/** Proves the oracle is really the baseline and not the current code (K0 spec §6.1). */
class OracleIsolationTest {
    @Test
    fun oracleJswordClassesComeFromTheBaselineJar() {
        val oracleSource = Oracle.call("self.codeSource")
        assertTrue(oracleSource.endsWith("oracle/jsword-baseline.jar"), oracleSource)
        assertFalse(Current.call("self.codeSource").endsWith("jsword-baseline.jar"))
    }

    @Test
    fun rootResourcesResolveInsideTheBaselineJar() {
        val url = Oracle.call("self.rootResource")
        assertTrue(url.startsWith("jar:") && url.endsWith("jsword-baseline.jar!/BibleNames.properties"), url)
    }

    @Test
    fun oracleClassesAreDistinctFromCurrentOnes() {
        val oracleBooks = Oracle.loader.loadClass("org.crosswire.jsword.book.Books")
        assertNotSame(Books::class.java, oracleBooks)
        assertSame(Oracle.loader, oracleBooks.classLoader)
    }

    @Test
    fun oracleCannotSeeTestClasses() {
        assertFailsWith<ClassNotFoundException> {
            Oracle.loader.loadClass("org.crosswire.ksword.parity.OracleIsolationTest")
        }
    }

    @Test
    fun manifestCarriesTheRecordedBaselineCommit() {
        val recorded = recordedBaselineCommit()
        assertTrue(Regex("[0-9a-f]{40}").matches(recorded), "not a full hash: $recorded")
        val jar = Oracle.classpath.single { it.name == "jsword-baseline.jar" }
        val inManifest = JarFile(jar).use { it.manifest.mainAttributes.getValue("Baseline-Commit") }
        assertEquals(recorded, inManifest)
    }
}
