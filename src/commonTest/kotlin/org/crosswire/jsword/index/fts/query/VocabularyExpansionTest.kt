package org.crosswire.jsword.index.fts.query

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VocabularyExpansionTest {
    private val vocab = listOf("bless", "blessed", "blessing", "bliss", "bless2", "boss", "b", "ab", "witness", "business", "ness", "x", "cat", "ca", "believ", "xx")

    private fun w(p: String) = VocabularyExpansion.wildcard(p, vocab.asSequence())

    @Test fun trailingStar() = assertEquals(listOf("bless", "blessed", "blessing", "bless2"), w("bless*"))

    @Test fun leadingStar() = assertEquals(listOf("witness", "business", "ness"), w("*ness"))

    @Test fun questionMarkIsExactlyOneUnit() {
        assertEquals(listOf("boss"), w("b?ss"))
        assertEquals(listOf("bless", "bliss"), w("bl?ss"))
        assertEquals(listOf("bless"), w("b?ess"))
    }

    @Test fun questionAloneMatchesSingleUnitTerms() = assertEquals(listOf("b", "x"), w("?"))

    @Test fun starAloneMatchesEveryTerm() = assertEquals(vocab, w("*"))

    @Test fun doubleStarEqualsStar() = assertEquals(vocab, w("**"))

    @Test fun middleStarCanMatchEmptyAndNonEmpty() {
        assertEquals(listOf("bless", "bliss", "boss", "business"), w("b*s"))
        assertEquals(listOf("cat", "ca"), w("ca*"))
    }

    @Test fun trailingQuestionsNeedExactlyThatManyUnits() {
        // Lucene quirk reproduced: "cat" does not match "ca??" (only "ca" plus two more units would).
        assertEquals(emptyList(), w("ca??"))
        assertEquals(listOf("cat"), w("ca?"))
        assertEquals(listOf("ab", "ca", "xx"), w("??"))
    }

    @Test fun surrogatePairIsTwoUnits() {
        val v = sequenceOf("a😀", "ab")
        assertEquals(listOf("ab"), VocabularyExpansion.wildcard("a?", v))
        assertEquals(listOf("a😀"), VocabularyExpansion.wildcard("a??", v))
    }

    @Test fun noWildcardIsExactMatch() = assertEquals(listOf("bless"), w("bless"))

    @Test fun prefixIsLiteral() {
        val v = sequenceOf("a*b", "a?", "ab")
        assertEquals(listOf("a*b"), VocabularyExpansion.prefix("a*", v))
        assertEquals(listOf("a?"), VocabularyExpansion.prefix("a?", v))
    }

    private val believers = listOf("believe", "believed", "believer", "believes", "relieve", "belive", "bleieve", "xyz", "believing", "unbelief")

    @Test fun fuzzyExplicitSimilarity() {
        // believe~0.6: terms within the distance limit; xyz and the far-off lengths are out.
        assertEquals(listOf("believe", "believed", "believer", "believes", "belive", "bleieve", "relieve"),
            VocabularyExpansion.fuzzy("believe", 0.6f, believers.asSequence()))
    }

    @Test fun fuzzyDefaultHalf() {
        val r = VocabularyExpansion.fuzzy("believe", 0.5f, believers.asSequence())
        assertTrue("believing" in r) // distance 3 of 7 letters: similarity 0.571 > 0.5
        assertFalse("xyz" in r)
        assertFalse("unbelief" in r) // maxDistance = (int)(0.5 * 7) = 3 < |8-7|? no: edit distance 4 -> 0.43
    }

    @Test fun shortTermOnlyMatchesItself() {
        assertEquals(listOf("x"), VocabularyExpansion.fuzzy("x", 0.99f, vocab.asSequence()))
        assertEquals(emptyList(), VocabularyExpansion.fuzzy("y", 0.99f, vocab.asSequence()))
        // x~0.5: length 1 is not above 1 / (1 - 0.5) = 2, so it is exact too.
        assertEquals(listOf("x"), VocabularyExpansion.fuzzy("x", 0.5f, vocab.asSequence()))
    }

    @Test fun cutKeepsBestThenTermOrder() {
        val v = sequenceOf("believc", "believa", "believb", "believe")
        // exact match first by similarity; of the three equal ones, the first in term order survives.
        assertEquals(listOf("believa", "believe"), VocabularyExpansion.fuzzy("believe", 0.6f, v, maxTerms = 2))
    }

    @Test fun threeThousandNearMatchesKeepExactly1024() {
        val v = (0 until 3000).map { "believe" + it.toString().padStart(4, '0') } + "believe"
        val r = VocabularyExpansion.fuzzy("believe", 0.3f, v.asSequence())
        assertEquals(1024, r.size)
        assertTrue("believe" in r)
        assertEquals(r.sorted(), r)
    }
}
