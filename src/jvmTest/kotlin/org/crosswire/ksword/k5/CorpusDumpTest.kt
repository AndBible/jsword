package org.crosswire.ksword.k5

import java.io.File
import java.io.StringReader
import java.security.MessageDigest
import javax.xml.parsers.SAXParserFactory
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookMetaData
import org.jdom2.input.SAXBuilder
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/**
 * K5 corpus dump: a TOOL (skipped unless `-Dk5.corpus.out=<dir>` is set), not a gate. It writes the exact strings
 * that JSword's XML filters hand to their parsers for a deterministic, evenly spaced key sample of every installed
 * OSIS / ThML / GBF module, for the kdom corpus differential (`-Dkdom.corpus=<dir>`).
 *
 * Output: `<dir>/<module>/<n>.txt` (UTF-8, one parser input per file) and `<dir>/manifest.tsv` with the columns
 * `module  key  stage  file  sha256` (key = OSIS id, file relative to `<dir>`).
 *
 * The raw text is `Book.getRawText(key)`, which is what `AbstractPassageBook`/`SwordDictionary` pass to
 * `Filter.toOSIS`. The fallback chains of `OSISFilter.toOSIS` and `THMLFilter.toOSIS` are replayed (each stage is
 * parsed with the same parser the filter uses, and the next stage is only emitted when the previous one was rejected),
 * so every file is an input the filter really parses:
 *  - OSIS: `osis-raw` (`<xxx>raw</xxx>`, with OSISFilter's MapM prefix hack), then `osis-entities`
 *    (cleanAllEntities), `osis-reclosed` (recloseTags), `osis-tags` (cleanAllTags), each only if the earlier
 *    stages were rejected and the stage text differs (as the filter itself skips unchanged stages).
 *  - ThML: wrapped in `<root>..</root>`: `thml-entities` (cleanAllEntities(raw)), `thml-chars`
 *    (cleanAllCharacters), `thml-close` (closeEmptyTags(chars)), `thml-tags` (cleanAllTags(chars)); the filter
 *    never parses the unmodified raw text, so there is no raw stage. Acceptance is JAXP well-formedness (the
 *    filter's CustomHandler is not replicated).
 *  - GBF: `gbf-raw`, the unparsed raw text (GBFFilter builds OSIS in code); not an XML input.
 *
 * Sample: every ceil(n / N)-th key of the global key list, N = `-Dk5.corpus.max` (default 3000), plus always the
 * keys named in [MUST_KEYS] (the known real rejects).
 */
class CorpusDumpTest {

    @Test
    fun dump() {
        val outDir = System.getProperty("k5.corpus.out").orEmpty()
        assumeTrue("k5.corpus.out not set", outDir.isNotEmpty() && outDir != "false")
        val max = System.getProperty("k5.corpus.max")?.takeIf { it.isNotEmpty() && it != "false" }?.toInt() ?: 3000
        val out = File(outDir).also { it.mkdirs() }
        val manifest = StringBuilder()
        val counts = sortedMapOf<String, Int>()

        val books = Books.installed().getBooks().sortedBy { it.initials }
        for (book in books) {
            val type = (book.bookMetaData.getProperty(SwordBookMetaData.KEY_SOURCE_TYPE) ?: "").uppercase()
            if (type != "OSIS" && type != "THML" && type != "GBF") continue
            val keys = ArrayList<org.crosswire.jsword.passage.Key>()
            for (k in (book.getGlobalKeyList() as Iterable<org.crosswire.jsword.passage.Key>)) keys.add(k)
            val step = maxOf(1, (keys.size + max - 1) / max)
            val picked = LinkedHashSet<Int>()
            for (i in keys.indices step step) picked.add(i)
            for (i in keys.indices) if (keys[i].getOsisID() in MUST_KEYS[book.initials].orEmpty()) picked.add(i)
            val modDir = File(out, book.initials).also { it.mkdirs() }
            var n = 0
            for (i in picked.sorted()) {
                val key = keys[i]
                val raw = try { book.getRawText(key) } catch (e: Exception) { continue }
                for ((stage, text) in stages(book, type, raw)) {
                    n++
                    val name = "$n.txt"
                    val bytes = text.toByteArray(Charsets.UTF_8)
                    File(modDir, name).writeBytes(bytes)
                    manifest.append(book.initials).append('\t').append(key.getOsisID()).append('\t').append(stage).append('\t')
                        .append(book.initials).append('/').append(name).append('\t').append(sha256(bytes)).append('\n')
                    counts.merge("${book.initials}/$stage", 1, Int::plus)
                }
            }
        }
        File(out, "manifest.tsv").writeText(manifest.toString(), Charsets.UTF_8)
        println("K5CORPUS total=${counts.values.sum()} max=$max out=$outDir")
        for ((k, v) in counts) println("K5CORPUS $k $v")
    }

    internal fun stages(book: Book, type: String, raw: String): List<Pair<String, String>> = when (type) {
        "GBF" -> listOf("gbf-raw" to raw)
        "OSIS" -> osisStages(book, raw)
        else -> thmlStages(raw)
    }

    private fun osisStages(book: Book, raw: String): List<Pair<String, String>> {
        var clean = raw
        if (book.initials == "MapM") {
            for (tag in listOf("cell", "row", "table")) {
                val startPos = clean.indexOf("<$tag>")
                val endPos = clean.indexOf("</$tag>")
                if (endPos != -1 && (startPos == -1 || startPos > endPos)) clean = "<$tag>$clean"
            }
        }
        val result = ArrayList<Pair<String, String>>()
        fun attempt(stage: String, text: String): Boolean {
            val wrapped = "<xxx>$text</xxx>"
            result.add(stage to wrapped)
            return try {
                SAXBuilder().apply { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                    .build(StringReader(wrapped))
                true
            } catch (e: Exception) { false }
        }
        if (attempt("osis-raw", clean)) return result
        val entities = XMLUtil.cleanAllEntities(clean)
        if (entities != null && entities != clean) {
            clean = entities
            if (attempt("osis-entities", clean)) return result
        }
        val reclosed = XMLUtil.recloseTags(clean)
        if (reclosed != null && reclosed != clean) {
            clean = reclosed
            if (attempt("osis-reclosed", clean)) return result
        }
        attempt("osis-tags", XMLUtil.cleanAllTags(clean) ?: "")
        return result
    }

    private fun thmlStages(raw: String): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>()
        fun attempt(stage: String, text: String?): Boolean {
            val wrapped = "<root>$text</root>"
            result.add(stage to wrapped)
            return try {
                SAXParserFactory.newInstance().newSAXParser().parse(InputSource(StringReader(wrapped)), DefaultHandler())
                true
            } catch (e: Exception) { false }
        }
        val entities = XMLUtil.cleanAllEntities(raw)
        if (attempt("thml-entities", entities)) return result
        val chars = XMLUtil.cleanAllCharacters(entities!!)
        // THMLFilter.cleanText: cleaning text, then closing empty tags, then cleaning tags (both from `chars`).
        if (attempt("thml-chars", chars)) return result
        if (attempt("thml-close", XMLUtil.closeEmptyTags(chars))) return result
        attempt("thml-tags", XMLUtil.cleanAllTags(chars))
        return result
    }

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    private companion object {
        /** Module initials to OSIS ids always included in the sample: the spike's real parser rejects. */
        val MUST_KEYS = mapOf(
            "NASB" to setOf("Mark.10.34", "Luke.4.43", "Luke.18.42", "John.13.26"),
            "ISV" to setOf("1Cor.14.33"),
        )
    }
}
