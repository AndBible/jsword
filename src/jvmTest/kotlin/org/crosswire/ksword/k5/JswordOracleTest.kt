package org.crosswire.ksword.k5

import java.io.File
import java.security.MessageDigest
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.passage.Key
import org.jdom2.output.Format
import org.jdom2.output.XMLOutputter
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class JswordOracleTest {
    @Test fun oracle() {
        val write = System.getProperty("k5.oracle.write")
        val check = System.getProperty("k5.oracle.check")
        assumeTrue("Set k5.oracle.write or k5.oracle.check", write != null || check != null)
        require((write == null) != (check == null)) { "Choose exactly one oracle mode" }
        val previous = SwordBookPathJvm.getAugmentPath()
        try {
            val root = File(System.getProperty("k5.project.root"))
            val extras = File(root, ".local/k5-sword").walkTopDown()
                .filter { it.isDirectory && File(it, "mods.d").isDirectory }.toList()
            SwordBookPathJvm.setAugmentPath((previous.toList() + extras).distinct().toTypedArray())
            val sections = sortedMapOf<String, String>()
            val coverage = StringBuilder()
            for (book in Books.installed().getBooks().sortedBy { it.initials }) {
                val type = book.bookMetaData.getProperty(SwordBookMetaData.KEY_SOURCE_TYPE).orEmpty().uppercase()
                val rows = StringBuilder()
                val rawRows = StringBuilder()
                for (key in sample(book)) {
                    val raw = book.getRawText(key)
                    val stages = if (type in listOf("OSIS", "THML", "GBF")) CorpusDumpTest().stages(book, type, raw) else emptyList()
                    val xml = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, key).getOsisFragment(false))
                    val stageIndex = when (stages.lastOrNull()?.first) {
                        "osis-entities", "thml-chars" -> 1
                        "osis-reclosed", "thml-close" -> 2
                        "osis-tags", "thml-tags" -> 3
                        else -> 0
                    }
                    rows.append(row(key.getOsisID(), stageIndex.toString(), digest(xml), xml))
                    // Keep the original source AND each attempted parser input, not just the final OSIS.
                    rawRows.append(row(key.getOsisID(), "source", digest(raw), raw))
                    for ((stage, text) in stages) rawRows.append(row(key.getOsisID(), stage, digest(text), text))
                }
                sections["${book.initials}.osis.tsv"] = rows.toString()
                sections["${book.initials}.inputs.tsv"] = rawRows.toString()
                coverage.append(row(book.initials, type, rows.lines().count { it.isNotEmpty() }.toString()))
            }
            check(sections.isNotEmpty()) { "No installed modules; install the test modules first" }
            sections["coverage.tsv"] = coverage.toString()
            val dir = File(write ?: check!!)
            if (write != null) { dir.mkdirs(); sections.forEach { (name, text) -> File(dir, name).writeText(text) } }
            else assertTrue(differences(dir, sections).joinToString("\n"), differences(dir, sections).isEmpty())
        } finally {
            SwordBookPathJvm.setAugmentPath(previous)
        }
    }

    @Test fun realFragmentAndFallbackInputsAreCaptured() {
        val book = requireNotNull(Books.installed().getBook("KJV"))
        val key = book.getKey("Gen.1.1")
        val xml = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, key).getOsisFragment(false))
        assertTrue(xml.contains("beginning"))
        val osis = CorpusDumpTest().stages(book, "OSIS", "<p>unclosed")
        assertEquals("osis-raw", osis.first().first)
        assertTrue(osis.size > 1)
        val thml = CorpusDumpTest().stages(book, "THML", "<p>A<br>B</p>")
        assertEquals(listOf("thml-entities", "thml-chars", "thml-close"), thml.map { it.first })
        assertTrue(thml.last().second.contains("<br"))
    }

    @Test fun comparisonReportsEveryChangedMissingAndExtraEntry() {
        val dir = kotlin.io.path.createTempDirectory("oracle-check").toFile()
        try {
            File(dir, "KJV.osis.tsv").writeText("Gen.1.1\t0\tx\told\nJohn.3.16\t0\tx\told\n")
            File(dir, "extra.tsv").writeText("extra\n")
            val failures = differences(dir, mapOf("KJV.osis.tsv" to "Gen.1.1\t0\tx\tnew\nJohn.3.16\t0\tx\tnew\n", "missing.tsv" to "new\n"))
            assertEquals(4, failures.size)
            assertTrue(failures.any { "Gen.1.1" in it && "offset=12" in it })
            assertTrue(failures.any { "John.3.16" in it })
            assertTrue(failures.any { "extra.tsv" in it })
            assertTrue(failures.any { "missing.tsv" in it })
        } finally { dir.deleteRecursively() }
    }

    companion object {
        fun sample(book: Book, max: Int = 3000): List<Key> {
            val keys = (book.getGlobalKeyList() as Iterable<Key>).toList()
            val step = maxOf(1, (keys.size + max - 1) / max)
            val must = when (book.initials) {
                "NASB" -> setOf("Mark.10.34", "Luke.4.43", "Luke.18.42", "John.13.26")
                "ISV" -> setOf("1Cor.14.33")
                else -> emptySet()
            }
            return keys.filterIndexed { i, key -> i % step == 0 || key.getOsisID() in must }
        }
        fun digest(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        fun row(vararg cells: String) = cells.joinToString("\t") { it.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n") } + "\n"
        fun differences(dir: File, actual: Map<String, String>): List<String> {
            val names = (actual.keys + dir.listFiles().orEmpty().filter { it.isFile }.map { it.name }).sorted()
            return names.distinct().flatMap { name ->
                val old = File(dir, name).takeIf { it.exists() }?.readText()?.trimEnd('\n')?.lines().orEmpty()
                val new = actual[name]?.trimEnd('\n')?.lines().orEmpty()
                (0 until maxOf(old.size, new.size)).mapNotNull { i ->
                    val a = old.getOrNull(i); val b = new.getOrNull(i)
                    if (a == b) null else {
                        val offset = (0 until minOf(a.orEmpty().length, b.orEmpty().length)).firstOrNull { a!![it] != b!![it] }
                            ?: minOf(a.orEmpty().length, b.orEmpty().length)
                        "module=${name.substringBefore('.')} key=${(b ?: a).orEmpty().substringBefore('\t')} section=$name offset=$offset row=${i + 1}"
                    }
                }
            }
        }
    }
}
