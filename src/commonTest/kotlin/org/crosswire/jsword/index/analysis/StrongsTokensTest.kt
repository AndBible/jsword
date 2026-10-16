package org.crosswire.jsword.index.analysis

import fi.sykero.kdom.input.KdomBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [StrongsTokens] with literal expectations on every platform. Each expectation was captured from the baseline
 * `StrongsNumberFilter` (oracle `strongs` op); the same cases are compared live in the `searchParityTest` source set.
 */
class StrongsTokensTest {
    @Test fun normalizePadsAndUppercases() {
        assertEquals("H7225", StrongsTokens.normalize("H07225"))
        assertEquals("H7225", StrongsTokens.normalize("h7225"))
        assertEquals("G0011", StrongsTokens.normalize("G11"))
        assertEquals("G0011", StrongsTokens.normalize("g0011a"))
        assertEquals("H1234", StrongsTokens.normalize("H1234!"))
    }

    @Test fun normalizeReturnsNullForInvalidTokens() {
        assertNull(StrongsTokens.normalize("garbage"))
        assertNull(StrongsTokens.normalize("H12345"))
        assertNull(StrongsTokens.normalize("G1418"))
        assertNull(StrongsTokens.normalize(""))
    }

    @Test fun fromLexicalEmitsTheNumberAndItsPartForm() {
        assertEquals(listOf("H7225"), StrongsTokens.fromLexical("H07225"))
        assertEquals(listOf("H7225"), StrongsTokens.fromLexical("h7225"))
        assertEquals(listOf("G0011"), StrongsTokens.fromLexical("G11"))
        assertEquals(listOf("G0011", "G0011a"), StrongsTokens.fromLexical("g0011a"))
        assertEquals(listOf("H1234"), StrongsTokens.fromLexical("H1234!"))
        assertEquals(emptyList(), StrongsTokens.fromLexical("garbage"))
        assertEquals(listOf("G2532", "G3588", "G3588a", "H7225"), StrongsTokens.fromLexical("G2532 g3588!a garbage H7225"))
    }

    @Test fun fromOsisReadsTheLemmasOfAVerse() {
        val osis = KdomBuilder().build(
            "<div><verse osisID=\"Matt.4.19\"><w lemma=\"strong:G2532\">And</w> <w lemma=\"strong:g3588!a\">the</w> " +
                "<w lemma=\"strong:H7225\">x</w></verse></div>").rootElement
        assertEquals(listOf("G2532", "G3588", "G3588a", "H7225"), StrongsTokens.fromOsis(osis))
    }
}
