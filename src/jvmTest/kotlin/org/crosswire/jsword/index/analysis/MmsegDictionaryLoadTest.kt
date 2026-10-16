package org.crosswire.jsword.index.analysis

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Dictionary cost on the JVM (one log line) and concurrent use of one shared [MmsegAnalyzer]. */
class MmsegDictionaryLoadTest {
    @Test fun loadTimeAndMemoryAreLogged() {
        val runtime = Runtime.getRuntime()
        System.gc()
        val before = runtime.totalMemory() - runtime.freeMemory()
        val start = System.nanoTime()
        fun text(name: String) = AnalyzerTables.requireResource("search/mmseg4j/$name.dic").decodeToString()
        // A fresh instance, so the number does not depend on whether another test loaded the shared one first.
        val dictionary = org.crosswire.jsword.index.analysis.mmseg.Dictionary.parse(text("chars"), text("words"), text("units"))
        val millis = (System.nanoTime() - start) / 1_000_000
        System.gc()
        val after = runtime.totalMemory() - runtime.freeMemory()
        println("MMSEG-DICTIONARY load ${millis} ms, retained heap about ${(after - before) / 1024 / 1024} MB")
        assertTrue(dictionary.matchingTailLengths("耶稣".toCharArray(), 0).size > 1)
    }

    @Test fun oneAnalyzerGivesTheSameResultOnEveryThread() {
        val analyzer = MmsegAnalyzer()
        val text = "耶稣说ABC 123个 神爱世人，甚至将他的独生子赐给他们，叫一切信他的，不至灭亡，反得永生。"
        val expected = analyzer.analyze(text)
        val failures = ConcurrentLinkedQueue<String>()
        val threads = List(8) {
            Thread { repeat(200) { if (analyzer.analyze(text) != expected) failures.add("diverged") } }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertTrue(failures.isEmpty(), "${failures.size} diverging analyses")
        assertEquals(expected, analyzer.analyze(text))
    }
}
