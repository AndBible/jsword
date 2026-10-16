package org.crosswire.ksword.parity.search

import fi.sykero.kdom.input.KdomBuilder
import org.crosswire.jsword.index.analysis.StrongsTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [StrongsTokens] against the baseline `StrongsNumberFilter` (the oracle's `strongs` op runs it over whitespace tokens).
 */
class StrongsTokensTest {
    private fun baseline(text: String): List<String> = OracleSearch.call("strongs", text).lines().filter { it.isNotEmpty() }

    @Test fun normalizeMatchesTheBaselineFilter() {
        // Baseline output for a single token: the padded number, and for a '!a' part also the full form (see fromLexical).
        assertEquals(listOf("H7225"), baseline("H07225"))
        assertEquals("H7225", StrongsTokens.normalize("H07225"))
        assertEquals(listOf("H7225"), baseline("h7225"))
        assertEquals("H7225", StrongsTokens.normalize("h7225"))
        assertEquals(listOf("G0011"), baseline("G11"))
        assertEquals("G0011", StrongsTokens.normalize("G11"))
        assertEquals(listOf("G0011", "G0011a"), baseline("g0011a"))
        assertEquals("G0011", StrongsTokens.normalize("g0011a"))
        assertEquals(listOf("H1234"), baseline("H1234!"))
        assertEquals("H1234", StrongsTokens.normalize("H1234!"))
        assertEquals(emptyList(), baseline("garbage"))
        assertNull(StrongsTokens.normalize("garbage"))
    }

    @Test fun invalidNumbersAreDroppedNotNormalized() {
        assertEquals(emptyList(), baseline("H12345"))
        assertNull(StrongsTokens.normalize("H12345"))
        assertEquals(emptyList(), baseline("G1418"))
        assertNull(StrongsTokens.normalize("G1418"))
        assertNull(StrongsTokens.normalize(""))
    }

    @Test fun fromLexicalMatchesTheBaselineStreamForAWholeField() {
        val field = "G2532 H7225 garbage G3588!a H1234! G11"
        assertEquals(baseline(field), StrongsTokens.fromLexical(field))
    }

    @Test fun fromOsisReadsTheWLemmasOfAVerse() {
        val osis = KdomBuilder().build(
            "<div><verse osisID=\"Matt.4.19\"><w lemma=\"strong:G2532\">And</w> <w lemma=\"strong:g3588!a\">the</w> " +
                "<w lemma=\"strong:H7225\">x</w></verse></div>").rootElement
        assertEquals(baseline("G2532 g3588!a H7225"), StrongsTokens.fromOsis(osis))
    }
}
