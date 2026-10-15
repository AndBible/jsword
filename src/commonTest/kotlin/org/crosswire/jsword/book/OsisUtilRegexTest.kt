package org.crosswire.jsword.book

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The two patterns of `OSISUtil` and `StrongsNumber` (K5b, rule 21: `Regex` instead of `java.util.regex.Pattern`).
 * Neither uses a lookbehind, a `\p{..}` class, a back-reference or an inline flag: both are plain ASCII classes with
 * greedy quantifiers, so Java and common regex engines agree. The patterns are copied here because the classes
 * move to commonMain only with the facade; `OsisUtilRegexDifferentialTest` (jvmTest) compares them with `Pattern`.
 */
class OsisUtilRegexTest {
    private val lemmaPattern = Regex("strong:([GgHh][0-9]+!?[A-Za-z]*)")
    private val strongsPattern = Regex("([GgHh])([0-9]*)!?([A-Za-z]+)?")

    private fun lemmas(s: String) = lemmaPattern.findAll(s).map { it.groupValues[1] }.toList()

    @Test fun lemmaFindsEveryStrongsNumber() {
        assertEquals(listOf("H07225", "H0430!a", "G25b", "g3588"), lemmas("strong:H07225 lemma:x strong:H0430!a strong:G25b strong:g3588"))
        assertEquals(listOf("G1161"), lemmas("strong:G1161"))
    }

    @Test fun lemmaRejectsNonStrongsAndMissingDigits() {
        assertEquals(emptyList(), lemmas("lemma:H1"))
        assertEquals(emptyList(), lemmas("strong:G"))
        assertEquals(emptyList(), lemmas("strong:X12"))
        assertEquals(emptyList(), lemmas(""))
        assertEquals(listOf("H5"), lemmas("xstrong:H5"))
    }

    @Test fun lemmaLettersAfterBangAreGreedyAsciiOnly() {
        assertEquals(listOf("G25abc"), lemmas("strong:G25abc"))
        assertEquals(listOf("G25"), lemmas("strong:G25é"))
        assertEquals(listOf("G25!"), lemmas("strong:G25!"))
    }

    @Test fun strongsMatchesAtStartOnly() {
        val m = assertNotNull(strongsPattern.matchAt("G3588!a", 0))
        assertEquals("G", m.groupValues[1])
        assertEquals("3588", m.groupValues[2])
        assertEquals("a", m.groups[3]?.value)
        assertNull(strongsPattern.matchAt(" G25", 0))
        assertNull(strongsPattern.matchAt("25", 0))
        assertNull(strongsPattern.matchAt("", 0))
    }

    @Test fun strongsEmptyDigitsAndAbsentPart() {
        val bare = assertNotNull(strongsPattern.matchAt("g", 0))
        assertEquals("", bare.groupValues[2])
        assertNull(bare.groups[3])
        val trailing = assertNotNull(strongsPattern.matchAt("H25xy z", 0))
        assertEquals("xy", trailing.groups[3]?.value)
        val gabc = assertNotNull(strongsPattern.matchAt("Gabc", 0))
        assertEquals("", gabc.groupValues[2])
        assertEquals("abc", gabc.groups[3]?.value)
    }
}
