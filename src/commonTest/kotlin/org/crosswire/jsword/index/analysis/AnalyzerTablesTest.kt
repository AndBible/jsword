package org.crosswire.jsword.index.analysis

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import okio.ByteString.Companion.toByteString
import org.crosswire.common.util.KswordResources

class AnalyzerTablesTest {
    /** SHA-256 values recorded in `src/embeddedResources/search/PROVENANCE.md`. */
    private val provenance = mapOf(
        "search/lucene-letter-ranges.tsv" to "ec4628b3d774854032d487349d1f111fc985c2492e852cf67af2eae9d28f23fd",
        "search/lucene-lowercase.tsv" to "a7ae93204edcaaa82d79aec61225a49e1f881754c6d2d604cfe109b2e4b1849e",
        "search/lucene-ascii-folding.tsv" to "5a7b168a1ecb2240d459c1047aa1bdfa318f79d4989f0a14967f23efaaac48cc",
        "search/lucene-character-categories.tsv" to "136920d4d3aed398a573338b1afd92925ab471d2d4383ecf5c0ba1f29810d247",
        "search/lucene-classic-tokenizer-2.9.bin" to "6c1a9c85cae3d24644247aef57bf79353b43dbb131fb9ea30366d479940d9bd7",
    )

    @Test
    fun embeddedTablesMatchProvenanceHashes() {
        for ((path, expected) in provenance) {
            val bytes = KswordResources.bytes(path)
            assertTrue(bytes != null, "$path is not embedded")
            assertEquals(expected, bytes.toByteString().sha256().hex(), "SHA-256 of $path")
        }
    }

    @Test
    fun classicTokenizerTablesAreTheProvenanceBytes() {
        assertEquals(
            provenance.getValue("search/lucene-classic-tokenizer-2.9.bin"),
            AnalyzerTables.classicTokenizerTables.toByteString().sha256().hex(),
        )
    }

    @Test
    fun missingResourceFailsInsteadOfFallingBack() {
        // A missing table must surface as IllegalStateException from the loader, never as a silent default.
        assertFailsWith<IllegalStateException> { AnalyzerTables.requireResource("search/does-not-exist.tsv") }
    }

    @Test
    fun letterTableFollowsJavaCharacterIsLetter() {
        assertTrue(AnalyzerTables.isLetter('a'))
        assertTrue(AnalyzerTables.isLetter('é'))
        assertTrue(AnalyzerTables.isLetter('ж'))
        assertTrue(!AnalyzerTables.isLetter('1'))
        assertTrue(!AnalyzerTables.isLetter(' '))
        assertTrue(!AnalyzerTables.isLetter('\uD835'))
    }

    @Test
    fun lowerCaseMapsJavaToLowerCaseChar() {
        assertEquals('a', AnalyzerTables.lowerCase('A'))
        assertEquals('ж', AnalyzerTables.lowerCase('Ж'))
        assertEquals('1', AnalyzerTables.lowerCase('1'))
        assertEquals('é', AnalyzerTables.lowerCase('É'))
    }

    @Test
    fun asciiFoldingReplacesAndKeepsUnmappedUnits() {
        assertEquals("cafe", AnalyzerTables.foldAscii("café"))
        assertEquals("ж", AnalyzerTables.foldAscii("ж"))
        // Lone surrogates are copied unchanged, never replaced by U+FFFD.
        assertEquals("\uD835bc", AnalyzerTables.foldAscii("\uD835bc"))
    }

    @Test
    fun characterTypeReturnsCategoryBitmaskOrZeroOutsideTable() {
        // 0x20 is the uppercase/modifier-letter bit for the Basic Latin capital range (0041..005A).
        assertEquals(0x20, AnalyzerTables.characterType('A'))
        assertEquals(0, AnalyzerTables.characterType('\u0000'))
    }
}
