package org.crosswire.jsword.index.analysis

/**
 * Word segmentation of Thai text as `java.text.BreakIterator.getWordInstance(Locale("th"))` does it in the baseline
 * `ThaiWordFilter`. On the JVM it is that very `BreakIterator`; on iOS it is a pure-Kotlin port that reads the OpenJDK 17
 * data files (the data is GPLv2+CPE and ships only in iOS binaries).
 */
internal expect object ThaiWordBreaker {
    /** The word ranges of [text] in order, as inclusive UTF-16 index ranges that tile the whole text. Empty text yields none. */
    fun words(text: String): List<IntRange>
}
