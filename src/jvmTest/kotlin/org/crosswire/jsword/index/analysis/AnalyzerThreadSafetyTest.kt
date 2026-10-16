package org.crosswire.jsword.index.analysis

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A cached analyzer is shared by concurrent callers, so one instance must give the single-threaded result on every
 * call from every thread. The inputs mix stemmable and unstemmable words, and umlauts and sharp s for German.
 */
class AnalyzerThreadSafetyTest {
    private val germanText = "Häuser geliebten laufend Kinder Straße Schönheit Matrizen Gottheiten Kindergärten"
    private val finnishText = "talossa rakastaminen ihmiset koirat kissoilla juoksemassa"

    private fun assertConcurrentlySame(analyzer: TextAnalyzer, text: String) {
        val expected = analyzer.analyze(text)
        val failures = ConcurrentLinkedQueue<String>()
        val threads = List(THREADS) {
            Thread {
                repeat(ITERATIONS) {
                    if (analyzer.analyze(text) != expected) failures.add("thread ${Thread.currentThread().name} diverged")
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertTrue(failures.isEmpty(), "${failures.size} diverging analyses, first: ${failures.firstOrNull()}")
        assertEquals(expected, analyzer.analyze(text))
    }

    @Test fun germanAnalyzerIsThreadSafe() = assertConcurrentlySame(GermanAnalyzer(), germanText)

    @Test fun snowballAnalyzerIsThreadSafe() = assertConcurrentlySame(SnowballAnalyzer("Finnish"), finnishText)

    private companion object {
        const val THREADS = 8
        const val ITERATIONS = 200
    }
}
