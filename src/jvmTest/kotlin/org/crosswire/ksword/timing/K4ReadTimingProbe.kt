package org.crosswire.ksword.timing

import org.crosswire.jsword.book.Books
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/** Re-run unchanged after commonization; materialize no raw corpus, only consume one key at a time. */
class K4ReadTimingProbe {
    @Test fun readEveryKeyThreeTimes() {
        assumeTrue(java.lang.Boolean.getBoolean("k4.timing"))
        for (initials in listOf("KJV", "StrongsGreek")) {
            val book = Books.installed().getBook(initials) ?: error("missing timing module $initials")
            repeat(3) { run ->
                var characters = 0L
                var keys = 0
                val start = System.nanoTime()
                for (key in book.getGlobalKeyList()) {
                    characters += book.getRawText(key).length
                    keys++
                }
                val milliseconds = (System.nanoTime() - start) / 1_000_000.0
                assertTrue(keys > 0 && characters > 0, "timing must consume real text")
                println("K4TIMING $initials ${run + 1} $milliseconds keys=$keys characters=$characters")
            }
        }
    }
}
