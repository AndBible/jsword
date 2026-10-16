package org.crosswire.jsword.index.analysis

/**
 * Word segmentation of Thai text as `java.text.BreakIterator.getWordInstance(Locale("th"))` does it in the baseline
 * `ThaiWordFilter`. On the JVM it is that very `BreakIterator`; on iOS it is a pure-Kotlin port that reads the OpenJDK 17
 * data files (the port's code and the data are GPLv2+CPE and ship only in iOS binaries: the port is compiled only by
 * `iosMain` and `jvmTest`, see `build.gradle.kts` `src/openjdkThai`).
 */
internal expect object ThaiWordBreaker {
    /**
     * A splitter for the tokens of one text. It may keep a non-thread-safe `BreakIterator` between calls, so a session
     * is used by one thread only (one per `analyze()` call); [words] on the object itself is a one-shot session.
     */
    fun session(): ThaiWordSplitter

    /** The word ranges of [text] in order, as inclusive UTF-16 index ranges that tile the whole text. Empty text yields none. */
    fun words(text: String): List<IntRange>
}

/** The per-text handle of [ThaiWordBreaker.session]. */
internal fun interface ThaiWordSplitter {
    fun words(text: String): List<IntRange>
}
