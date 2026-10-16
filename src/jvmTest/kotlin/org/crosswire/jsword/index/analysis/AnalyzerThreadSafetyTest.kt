package org.crosswire.jsword.index.analysis

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs [analyzer] on [text] from [threads] threads x [iterations] calls, all released together by a start barrier so
 * they really overlap, and asserts every result equals the single-threaded one. A throwing worker is recorded as a
 * failure (an uncaught exception would otherwise only end that thread and let the test pass).
 */
internal fun assertConcurrentlySame(analyzer: TextAnalyzer, text: String, threads: Int = 8, iterations: Int = 200) {
    val expected = analyzer.analyze(text)
    val failures = ConcurrentLinkedQueue<String>()
    val ready = CountDownLatch(threads)
    val go = CountDownLatch(1)
    val workers = List(threads) {
        Thread {
            try {
                ready.countDown()
                go.await()
                repeat(iterations) {
                    if (analyzer.analyze(text) != expected) failures.add("thread ${Thread.currentThread().name} diverged")
                }
            } catch (t: Throwable) {
                failures.add("thread ${Thread.currentThread().name} threw $t")
            }
        }
    }
    workers.forEach { it.start() }
    ready.await()
    go.countDown()
    workers.forEach { it.join() }
    assertTrue(failures.isEmpty(), "${failures.size} failures, first: ${failures.firstOrNull()}")
    assertEquals(expected, analyzer.analyze(text))
}

/**
 * A cached analyzer is shared by concurrent callers, so one instance must give the single-threaded result on every
 * call from every thread. The inputs mix stemmable and unstemmable words, and umlauts and sharp s for German.
 */
class AnalyzerThreadSafetyTest {
    private val germanText = "Häuser geliebten laufend Kinder Straße Schönheit Matrizen Gottheiten Kindergärten"
    private val finnishText = "talossa rakastaminen ihmiset koirat kissoilla juoksemassa"
    private val greekText = "Ἐν ἀρχῇ ἦν ὁ λόγος, καὶ ὁ λόγος ἦν πρὸς τὸν θεόν ΛΌΓΟΣ ς Σ"
    private val thaiText = "พระเจ้าทรงสร้างโลก กขABC และ Hello พระเยซูตรัสว่า"
    private val arabicText = "في البدء كان الكلمة والكلمة كان عند الله وكان الكلمة الله ABC"

    @Test fun germanAnalyzerIsThreadSafe() = assertConcurrentlySame(GermanAnalyzer(), germanText)

    @Test fun snowballAnalyzerIsThreadSafe() = assertConcurrentlySame(SnowballAnalyzer("Finnish"), finnishText)

    @Test fun greekClassicTokenizerAnalyzerIsThreadSafe() = assertConcurrentlySame(GreekAnalyzer(), greekText)

    @Test fun thaiAnalyzerIsThreadSafe() = assertConcurrentlySame(ThaiAnalyzer(), thaiText)

    @Test fun arabicAnalyzerIsThreadSafe() = assertConcurrentlySame(ArabicAnalyzer(), arabicText)

    @Test fun harnessReportsAThrowingWorker() {
        val caller = Thread.currentThread()
        val boom = TextAnalyzer { text -> if (Thread.currentThread() !== caller) error("boom") else listOf(AnalyzedToken(text, 0)) }
        val e = runCatching { assertConcurrentlySame(boom, "x", threads = 2, iterations = 1) }.exceptionOrNull()
        assertTrue(e is AssertionError && "threw" in e.message.orEmpty(), "got $e")
    }
}
