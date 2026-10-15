package org.crosswire.ksword.k5b

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookDriver
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Books startup timing (spec section 5.4). Skipped unless `-Dk5b.timing=true`. Each round clears the registry's
 * book collections and re-registers a fresh Sword driver, then lists the installed books; two warm-up and three
 * measured rounds. Lines `books-startup<TAB>round<TAB>milliseconds` go to the file named by `-Dk5b.timing.out`.
 */
class BooksStartupTimingTest {
    @Test fun booksStartup() {
        assumeTrue("Set -Dk5b.timing=true", System.getProperty("k5b.timing") == "true")
        val out = System.getProperty("k5b.timing.out")?.let(::File)
        val registry = Books.installed()
        val fields = listOf("books", "initials", "names", "driverSet").map {
            Books::class.java.getDeclaredField(it).apply { isAccessible = true }
        }
        val lines = StringBuilder()
        repeat(5) { round ->
            synchronized(registry) {
                fields[0].set(registry, java.util.TreeSet<Book>())
                fields[1].set(registry, HashMap<String, Book>())
                fields[2].set(registry, HashMap<String, Book>())
                fields[3].set(registry, HashSet<Any>())
            }
            val start = System.nanoTime()
            registry.registerDriver(SwordBookDriver())
            val count = registry.getBooks().size
            val ms = (System.nanoTime() - start) / 1_000_000.0
            assertTrue("no books found; install the test modules first", count > 0)
            if (round >= 2) lines.append("books-startup\t${round - 1}\t$ms\n")
        }
        out?.apply { parentFile?.mkdirs(); writeText(lines.toString()) } ?: print(lines)
    }
}
