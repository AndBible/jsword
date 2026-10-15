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
                val (rows, rawRows) = capture(book, sample(book))
                sections["${book.initials}.osis.tsv"] = rows
                sections["${book.initials}.inputs.tsv"] = rawRows
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

    @Test fun syntheticJosephusRootIsRecordedButRealReadErrorsPropagate() = withExtraModules {
        val book = Books.installed().getBook("Josephus")
        if (System.getProperty("k5.oracle.write") != null || System.getProperty("k5.oracle.check") != null) requireNotNull(book) { "Full oracle requires Josephus regression coverage" }
        assumeTrue("Optional Josephus module not installed for normal CI", book != null)
        requireNotNull(book)
        val keys = sample(book)
        val root = keys.first()
        assertEquals("", root.getOsisID())
        val captured = capture(book, listOf(root, keys.first { it.getOsisID().isNotEmpty() }))
        assertTrue(captured.second.contains("synthetic-genbook-root"))
        assertTrue(captured.first.contains("SKIP"))
        assertTrue(captured.second.contains("source"))
        assertThrows(org.crosswire.jsword.book.BookException::class.java) {
            capture(book, listOf(org.crosswire.jsword.passage.TreeKey("DefinitelyMissingEntry")))
        }
    }

    @Test fun thmlStagesUseProductionAcceptanceAndDistinguishTotalRejection() {
        val book = requireNotNull(Books.installed().getBook("KJV"))
        val key = book.getKey("Gen.1.1")
        val parser = org.crosswire.jsword.book.filter.thml.THMLFilter::class.java.getDeclaredMethod("parse", Book::class.java, Key::class.java, String::class.java, String::class.java).apply { isAccessible = true }
        for (raw in listOf("<p>Good</p>", "<p>A<br>B</p>", "<p>unclosed", "bad\u0001text", "bad\uD800text")) {
            val replay = replay(book, key, "THML", raw)
            val expected = replay.first.indexOfFirst { (_, wrapped) ->
                parser.invoke(org.crosswire.jsword.book.filter.thml.THMLFilter(), book, key, wrapped.removePrefix("<root>").removeSuffix("</root>"), "test") != null
            }
            assertEquals(expected, replay.second)
        }
        assertEquals(-1, acceptingStage(listOf("<bad>", "<also-bad>")) { input ->
            parser.invoke(org.crosswire.jsword.book.filter.thml.THMLFilter(), book, key, input, "test") != null
        })
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
            assertTrue(differences(dir, mapOf("KJV.osis.tsv" to File(dir, "KJV.osis.tsv").readText() + "\n")).any { "section=KJV.osis.tsv" in it })
        } finally { dir.deleteRecursively() }
    }

    private fun withExtraModules(action: () -> Unit) {
        val previous = SwordBookPathJvm.getAugmentPath()
        try {
            val root = File(requireNotNull(System.getProperty("k5.project.root")))
            val extras = File(root, ".local/k5-sword").walkTopDown().filter { it.isDirectory && File(it, "mods.d").isDirectory }.toList()
            SwordBookPathJvm.setAugmentPath((previous.toList() + extras).distinct().toTypedArray())
            action()
        } finally { SwordBookPathJvm.setAugmentPath(previous) }
    }

    private fun capture(book: Book, keys: List<Key>): Pair<String, String> {
        val rows = StringBuilder(); val inputs = StringBuilder()
        val type = book.bookMetaData.getProperty(SwordBookMetaData.KEY_SOURCE_TYPE).orEmpty().uppercase()
        for ((position, key) in keys.withIndex()) {
            if (syntheticRoot(book, key)) {
                rows.append(row(key.getOsisID(), "SKIP", "synthetic-genbook-root", "Non-content index container"))
                inputs.append(row(key.getOsisID(), "synthetic-genbook-root", "sample-position=$position", "Non-content index container; name empty, parent absent, children present"))
                continue
            }
            val raw = book.getRawText(key)
            val (stages, accepted) = replay(book, key, type, raw)
            val xml = XMLOutputter(Format.getRawFormat()).outputString(BookData(book, key).getOsisFragment(false))
            rows.append(row(key.getOsisID(), accepted.toString(), digest(xml), xml))
            inputs.append(row(key.getOsisID(), "source", digest(raw), raw))
            for ((stage, text) in stages) inputs.append(row(key.getOsisID(), stage, digest(text), text))
        }
        return rows.toString() to inputs.toString()
    }

    private fun replay(book: Book, key: Key, type: String, raw: String): Pair<List<Pair<String, String>>, Int> {
        if (type !in listOf("OSIS", "THML")) return emptyList<Pair<String, String>>() to 0
        val attempts = mutableListOf<Pair<String, String>>()
        if (type == "THML") {
            val filter = org.crosswire.jsword.book.filter.thml.THMLFilter()
            val parse = filter.javaClass.getDeclaredMethod("parse", Book::class.java, Key::class.java, String::class.java, String::class.java).apply { isAccessible = true }
            val entities = org.crosswire.common.xml.XMLUtil.cleanAllEntities(raw)!!
            val chars by lazy { org.crosswire.common.xml.XMLUtil.cleanAllCharacters(entities) }
            val stages = listOf<Pair<String, () -> String?>>("thml-entities" to { entities }, "thml-chars" to { chars }, "thml-close" to { org.crosswire.common.xml.XMLUtil.closeEmptyTags(chars) }, "thml-tags" to { org.crosswire.common.xml.XMLUtil.cleanAllTags(chars) })
            for ((index, stage) in stages.withIndex()) {
                val text = stage.second()
                attempts.add(stage.first to "<root>$text</root>")
                if (parse.invoke(filter, book, key, text, stage.first) != null) return attempts to index
            }
            return attempts to -1
        }
        val stages = CorpusDumpTest().stages(book, type, raw)
        attempts.addAll(stages)
        val parse = org.crosswire.jsword.book.filter.osis.OSISFilter::class.java.getDeclaredMethod("parse", String::class.java).apply { isAccessible = true }
        val index = acceptingStage(stages.map { it.second }) { wrapped ->
            try { parse.invoke(org.crosswire.jsword.book.filter.osis.OSISFilter(), wrapped.removePrefix("<xxx>").removeSuffix("</xxx>")) != null }
            catch (e: java.lang.reflect.InvocationTargetException) {
                if (e.cause is org.jdom2.JDOMException || e.cause is java.io.IOException) false else throw e
            }
        }
        val stageIndex = if (index < 0) -1 else when (stages[index].first) { "osis-entities" -> 1; "osis-reclosed" -> 2; "osis-tags" -> 3; else -> 0 }
        return attempts to stageIndex
    }

    private fun acceptingStage(inputs: List<String>, accepts: (String) -> Boolean): Int = inputs.indexOfFirst(accepts)

    private fun syntheticRoot(book: Book, key: Key): Boolean = book is org.crosswire.jsword.book.sword.SwordGenBook &&
        book.backend is org.crosswire.jsword.book.sword.GenBookBackend && key.getParent() == null &&
        key.getName().isEmpty() && key.getOsisID().isEmpty() && key.getChildCount() > 0

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
                val baseline = File(dir, name)
                if (!baseline.exists() || name !in actual) return@flatMap listOf("module=${name.substringBefore('.')} key=? section=$name offset=0 missing=${if (!baseline.exists()) "baseline" else "actual"}")
                val old = baseline.readText().split('\n')
                val new = actual.getValue(name).split('\n')
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
