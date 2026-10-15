package org.crosswire.jsword.book

import org.crosswire.common.util.splitLikeJava
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `BookFilters.getCustom` splits its match on `;` and each part on `=` (K5b, rule 21: `Regex` instead of
 * `java.util.regex.Pattern`). Expected values are the results of Java's `String.split`, trailing empty strings
 * removed and a leading empty string kept.
 */
class BookFiltersRegexTest {
    @Test fun semicolonSplitMatchesJavaSplit() {
        val semicolon = Regex(";")
        assertEquals(listOf("a=b", "c!=d"), "a=b;c!=d".splitLikeJava(semicolon))
        assertEquals(listOf(""), "".splitLikeJava(semicolon))
        assertEquals(listOf("", "a"), ";a;;".splitLikeJava(semicolon))
        assertEquals(listOf("a"), "a".splitLikeJava(semicolon))
        assertEquals(emptyList(), ";".splitLikeJava(semicolon))
    }

    @Test fun equalsSplitMatchesJavaSplit() {
        val equals = Regex("=")
        assertEquals(listOf("a", "b"), "a=b".splitLikeJava(equals))
        assertEquals(listOf("a"), "a=".splitLikeJava(equals))
        assertEquals(listOf("", "b"), "=b".splitLikeJava(equals))
        assertEquals(listOf("a", "b", "c"), "a=b=c".splitLikeJava(equals))
        assertEquals(emptyList(), "=".splitLikeJava(equals))
    }
}
