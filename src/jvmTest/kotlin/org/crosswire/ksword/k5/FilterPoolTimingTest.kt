package org.crosswire.ksword.k5

import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.filter.osis.OSISFilter
import org.junit.Assert.assertTrue
import org.junit.Test

/** Focused JVM timing evidence only, not a stable performance acceptance gate. */
class FilterPoolTimingTest {
    @Test fun tenThousandKjvConversions() {
        val book = requireNotNull(Books.installed().getBook("KJV"))
        val key = book.getKey("Gen.1.1")
        val raw = book.getRawText(key)
        val filter = OSISFilter()
        repeat(2000) { filter.toOSIS(book, key, raw) }
        val start = System.nanoTime()
        var count = 0
        repeat(10000) { count += filter.toOSIS(book, key, raw).size }
        val elapsed = System.nanoTime() - start
        assertTrue(count > 0)
        println("K5 OSIS builder timing: 10000 KJV Gen.1.1 conversions, $elapsed ns, $count content nodes")
    }
}
