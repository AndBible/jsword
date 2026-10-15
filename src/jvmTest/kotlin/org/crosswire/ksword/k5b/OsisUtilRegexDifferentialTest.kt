package org.crosswire.ksword.k5b

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.regex.Pattern

/** Compares the common `Regex` forms of the `OSISUtil` and `StrongsNumber` patterns with the old `Pattern` on the JVM. */
class OsisUtilRegexDifferentialTest {
    private val lemmaSource = "strong:([GgHh][0-9]+!?[A-Za-z]*)"
    private val strongsSource = "([GgHh])([0-9]*)!?([A-Za-z]+)?"
    private val inputs = listOf(
        "", "G", "g25", "H0430", "G3588!a", "G3588a", "h8674!b", "G25xy z", " G25", "25", "X1", "Gabc", "Gé", "G٣",
        "strong:H07225 lemma:x strong:H0430!a strong:G25b strong:g3588", "strong:G", "lemma:H1", "xstrong:H5",
        "strong:G25é", "strong:G25!", "strong:G٣", "strong:H1 strong:H2strong:G3", "STRONG:G1",
    )

    @Test fun lemmaPatternFindsTheSameMatches() {
        val old = Pattern.compile(lemmaSource)
        val new = Regex(lemmaSource)
        for (s in inputs) {
            val expected = mutableListOf<List<Any?>>()
            val m = old.matcher(s)
            while (m.find()) expected += listOf(m.group(1), m.start(), m.end())
            val actual = new.findAll(s).map { listOf(it.groupValues[1], it.range.first, it.range.last + 1) }.toList()
            assertEquals(s, expected, actual)
        }
    }

    @Test fun strongsPatternLooksAtTheSameStart() {
        val old = Pattern.compile(strongsSource)
        val new = Regex(strongsSource)
        for (s in inputs) {
            val m = old.matcher(s)
            val expected = if (m.lookingAt()) listOf(m.group(1), m.group(2), m.group(3), m.end()) else null
            val r = new.matchAt(s, 0)
            val actual = r?.let { listOf(it.groupValues[1], it.groupValues[2], it.groups[3]?.value, it.range.last + 1) }
            assertEquals(s, expected, actual)
        }
    }
}
