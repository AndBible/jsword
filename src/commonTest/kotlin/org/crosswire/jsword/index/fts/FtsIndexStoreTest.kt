package org.crosswire.jsword.index.fts

import okio.FileSystem
import okio.Path
import org.crosswire.jsword.index.analysis.AnalyzedToken
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FtsIndexStoreTest {
    private val files = mutableListOf<Path>()
    private fun tmp(): Path =
        (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "fts-test-${Random.nextLong().toString(16)}.db").also { files += it }

    @AfterTest fun cleanup() { files.forEach { FileSystem.SYSTEM.delete(it) } }

    private fun e(t: String) = SearchTokenCodec.encode(t)
    private fun toks(vararg w: String) = w.mapIndexed { i, s -> AnalyzedToken(s, i) }
    private val meta = FtsIndexMetadata(FTS_SCHEMA_VERSION, "simple", SearchTokenCodec.ID, "KJV", "1.0", "fp")

    private fun built(block: FtsIndexStore.() -> Unit): FtsIndexStore {
        val s = FtsIndexStore.create(tmp())
        s.beginBuild(); s.block(); s.writeMetadata(meta); s.commitBuild()
        return s
    }

    private fun sample() = built {
        addEntry(1, "Gen.1.1", toks("in", "the", "beginning", "god"), listOf("H430"))
        addEntry(2, "Gen.1.2", toks("the", "earth", "was", "void"), listOf("H776", "H430"))
        addEntry(3, "John.1.1", toks("αγάπη", "愛", "𝄞"), emptyList())
    }

    @Test fun phrasePrefixAndNot() {
        sample().use { s ->
            assertEquals(setOf(1L), s.match("\"${e("in")} ${e("the")}\""))
            assertEquals(setOf(1L, 2L), s.match(e("the")))
            assertEquals(setOf(1L), s.match("${e("the")} NOT ${e("earth")}"))
            assertEquals(setOf(1L, 2L), s.match("${e("th")}*"))
            assertEquals(setOf(3L), s.match(e("愛")))
            assertEquals(setOf(3L), s.match(e("𝄞")))
        }
    }

    @Test fun vocabularyStrongsOsisAndRows() {
        sample().use { s ->
            assertEquals(setOf("in", "the", "beginning", "god", "earth", "was", "void", "αγάπη", "愛", "𝄞"), s.vocabulary().toSet())
            assertEquals(setOf(1L, 2L), s.strongs("H430"))
            assertEquals(setOf(2L), s.strongs("H776"))
            assertEquals(listOf("H430", "H776"), s.strongsVocabulary().toList())
            assertEquals("John.1.1", s.osisRef(3))
            assertEquals(setOf(1L, 2L, 3L), s.allRowIds())
            assertEquals(toks("the", "earth", "was", "void"), s.tokensOf(2))
            assertEquals(emptyList(), s.tokensOf(99))
        }
    }

    @Test fun metadataRoundTripAndReopen() {
        val path = tmp()
        FtsIndexStore.create(path).use { s ->
            s.beginBuild(); s.addEntry(1, "a", toks("x"), emptyList()); s.writeMetadata(meta.copy(bookVersion = null)); s.commitBuild()
            assertEquals(meta.copy(bookVersion = null), s.metadata())
        }
        FtsIndexStore.open(path).also { assertNotNull(it) }!!.use { assertEquals(setOf(1L), it.match(e("x"))) }
    }

    @Test fun openRejectsMissingWrongVersionAndGarbage() {
        assertNull(FtsIndexStore.open(tmp()))
        val p = tmp()
        FtsIndexStore.create(p).use { it.beginBuild(); it.writeMetadata(meta.copy(schemaVersion = 99)); it.commitBuild() }
        assertNull(FtsIndexStore.open(p))
        val noMeta = tmp()
        FtsIndexStore.create(noMeta).close()
        assertNull(FtsIndexStore.open(noMeta))
        val garbage = tmp()
        FileSystem.SYSTEM.write(garbage) { writeUtf8("this is not a sqlite database at all, just text".repeat(50)) }
        assertNull(FtsIndexStore.open(garbage))
    }

    @Test fun gapsKeepPositionsAndAreNotInVocabulary() {
        // positions 2 and 5: leading gap of 2, then 2 missing positions between
        val s = built {
            addEntry(1, "a", listOf(AnalyzedToken("a", 2), AnalyzedToken("b", 3), AnalyzedToken("c", 6)), emptyList())
            addEntry(2, "b", toks("a", "b"), emptyList())
        }
        s.use {
            assertEquals(setOf(1L, 2L), it.match("\"${e("a")} ${e("b")}\""))
            assertEquals(emptySet(), it.match("\"${e("b")} ${e("c")}\""), "gap between b and c must break a phrase")
            assertEquals(setOf(1L), it.match("NEAR(${e("b")} ${e("c")}, 2)"))
            assertEquals(emptySet(), it.match("NEAR(${e("b")} ${e("c")}, 1)"))
            assertEquals(listOf(AnalyzedToken("a", 2), AnalyzedToken("b", 3), AnalyzedToken("c", 6)), it.tokensOf(1))
            assertEquals(setOf("a", "b", "c"), it.vocabulary().toSet())
            assertEquals(setOf(1L), it.match(FtsSchema.GAP), "gap token is stored only in row 1")
        }
    }

    @Test fun emptyTermIsStoredAndMatched() {
        val s = built {
            addEntry(1, "a", listOf(AnalyzedToken("", 0), AnalyzedToken("w", 1)), emptyList())
            addEntry(2, "b", toks("w"), emptyList())
        }
        s.use {
            assertEquals(setOf(1L), it.match(e("")))
            assertEquals(setOf(1L), it.match("\"${e("")} ${e("w")}\""))
            assertTrue("" in it.vocabulary().toSet())
            assertEquals(listOf(AnalyzedToken("", 0), AnalyzedToken("w", 1)), it.tokensOf(1))
        }
    }

    @Test fun rejectsNonIncreasingPositions() {
        val s = FtsIndexStore.create(tmp())
        s.use {
            it.beginBuild()
            assertFailsWith<IllegalArgumentException> {
                it.addEntry(1, "a", listOf(AnalyzedToken("a", 1), AnalyzedToken("b", 1)), emptyList())
            }
        }
    }

    @Test fun entryWithNoTokens() {
        val s = built {
            addEntry(1, "Gen.1.1", emptyList(), emptyList())
            addEntry(2, "Gen.1.2", toks("w"), emptyList())
        }
        s.use {
            assertEquals(emptyList(), it.tokensOf(1))
            assertEquals(setOf(2L), it.match(e("w")))
            assertEquals(setOf(1L, 2L), it.allRowIds())
            assertEquals("Gen.1.1", it.osisRef(1))
        }
    }

    @Test fun failedBuildRollsBackOnClose() {
        val p = tmp()
        FtsIndexStore.create(p).use {
            it.beginBuild()
            it.addEntry(1, "a", toks("a"), emptyList())
            assertFailsWith<IllegalArgumentException> {
                it.addEntry(2, "b", listOf(AnalyzedToken("a", 1), AnalyzedToken("b", 1)), emptyList())
            }
        }
        assertNull(FtsIndexStore.open(p)) // no metadata was committed
        // the uncommitted row 1 is gone too: write metadata into the reopened raw file and look
        val raw = androidx.sqlite.driver.bundled.BundledSQLiteDriver().open(p.toString())
        raw.prepare("SELECT count(*) FROM entry").use { st -> st.step(); assertEquals(0L, st.getLong(0)) }
        raw.close()
    }
}
