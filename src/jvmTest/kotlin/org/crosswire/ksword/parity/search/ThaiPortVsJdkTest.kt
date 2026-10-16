package org.crosswire.ksword.parity.search

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.analysis.ClassicTokenizer
import org.crosswire.jsword.index.analysis.thai.ThaiBreakPort
import java.text.BreakIterator
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Proves the pure-Kotlin Thai break port (the iOS code path) against the JDK it was ported from, over every ThaiKJV
 * verse: the port, fed the two OpenJDK data files from the jvmTest classpath, must give the same word boundaries as
 * `java.text.BreakIterator.getWordInstance(Locale("th"))` for every Thai-led classic token. The data files are
 * on the test classpath only (GPLv2+CPE; they ship in iOS binaries only).
 */
class ThaiPortVsJdkTest {
    private fun resource(name: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream("search/openjdk17/$name")) { "missing test resource $name" }.use { it.readBytes() }

    private val port = ThaiBreakPort(resource("WordBreakIteratorData_th"), resource("thai_dict"))

    private fun jdkBoundaries(text: String): List<Int> {
        val b = BreakIterator.getWordInstance(Locale("th"))
        b.setText(text)
        val out = arrayListOf(b.first())
        while (true) {
            val n = b.next()
            if (n == BreakIterator.DONE) break
            out.add(n)
        }
        return out
    }

    private fun book(): Book {
        SwordBookPathJvm.setAugmentPath(arrayOf(K6Corpus.root))
        return requireNotNull(Books.installed().getBook("ThaiKJV")) { "no ThaiKJV in the corpus" }
    }

    @Test fun portBoundariesEqualJdkOnEveryThaiKjvVerse() {
        val book = book()
        val tokenizer = ClassicTokenizer()
        var verses = 0
        var tokens = 0
        var mismatches = 0
        val report = StringBuilder()
        for (key in book.getGlobalKeyList()) {
            val text = OSISUtil.getCanonicalText(BookData(book, key).getOsisFragment(false))
            verses++
            for (t in tokenizer.tokenize(text)) {
                if (t.text.isEmpty() || t.text[0].code !in 0x0E00..0x0E7F) continue
                tokens++
                val expected = jdkBoundaries(t.text)
                val actual = port.boundaries(t.text).toList()
                if (expected != actual) {
                    mismatches++
                    if (mismatches <= 10) report.append("  ${key.getOsisRef()} '${t.text}'\n    jdk : $expected\n    port: $actual\n")
                }
            }
        }
        assertTrue(verses > 1000 && tokens > 10000, "suspiciously little data: $verses verses, $tokens Thai tokens")
        if (mismatches > 0) fail("$mismatches of $tokens Thai tokens differ; first:\n$report")
    }

    @Test fun portHandlesEdgeInputs() {
        assertEquals(listOf(0), port.boundaries("").toList())
        for (s in listOf("ก", "กก", "a", "😀", "ก😀ก", "กักา", "ๆ", "ฯะ")) {
            assertEquals(jdkBoundaries(s), port.boundaries(s).toList(), "input ${s.map { it.code.toString(16) }}")
        }
    }
}
