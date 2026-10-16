package org.crosswire.ksword.parity.search

import java.io.File
import java.nio.file.Files
import java.util.function.BiFunction
import org.crosswire.ksword.parity.Oracle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The baseline search probe in the oracle class loader (Lucene 3.6.2), like [Oracle.call] but for `SearchProbe`. */
object OracleSearch {
    @Suppress("UNCHECKED_CAST")
    private val probe: BiFunction<String, List<String>, String> by lazy {
        Oracle.loader.loadClass("org.crosswire.ksword.parity.SearchProbe").getDeclaredConstructor().newInstance()
            as BiFunction<String, List<String>, String>
    }

    fun call(op: String, vararg args: String): String {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = Oracle.loader
        try {
            return probe.apply(op, args.toList())
        } finally {
            thread.contextClassLoader = previous
        }
    }
}

class SearchProbeSmokeTest {
    @Test
    fun analyzeFoldsAccentsAndLowercases() {
        assertEquals("surely\t1\ncafe\t1", OracleSearch.call("analyze", "en", "Sûrely Café"))
    }

    @Test
    fun buildIndexAndFindStrongsInKjv() {
        val root = K6Corpus.root
        val dir = Files.createTempDirectory("k6-smoke-index").toFile()
        try {
            val built = OracleSearch.call("buildIndex", root.path, "KJV", File(dir, "idx").path)
            assertTrue(built, built.startsWith("ok "))
            val refs = OracleSearch.call("find", root.path, "KJV", File(dir, "idx").path, "strong:h7225").lines()
            assertTrue(refs.toString(), "Gen.1.1" in refs)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun missingCorpusFailsInsteadOfSkipping() {
        val key = "ksword.k6.corpus"
        val previous = System.getProperty(key)
        try {
            System.clearProperty(key)
            try {
                K6Corpus.root
                fail("expected K6Corpus.root to throw without the property")
            } catch (expected: IllegalStateException) {
            }
            val empty = Files.createTempDirectory("k6-empty").toFile()
            System.setProperty(key, empty.path)
            try {
                K6Corpus.root
                fail("expected K6Corpus.root to throw for an empty corpus")
            } catch (expected: IllegalStateException) {
            } finally {
                empty.deleteRecursively()
            }
        } finally {
            if (previous != null) System.setProperty(key, previous)
        }
    }
}
