package org.crosswire.ksword.parity.search

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import okio.Path.Companion.toOkioPath
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.SwordBookPathJvm
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.analysis.BookAnalyzers
import org.crosswire.jsword.index.fts.FTS_SCHEMA_VERSION
import org.crosswire.jsword.index.fts.FtsIndexBuilder
import org.crosswire.jsword.index.fts.FtsIndexManager
import org.crosswire.jsword.index.fts.UnsupportedSearchSyntaxException
import org.crosswire.jsword.index.lucene.LuceneSearcher
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * K6 task 14: the complete per-module search parity matrix. For every corpus module both indexes are built once
 * (baseline Lucene 3.6.2 through the oracle, FTS5 through [FtsIndexManager]) under `build/k6-indexes/`, then every
 * query of [QUERY_SETS] runs through the JSword search layer on both sides (`LuceneSearcher`: `QueryBuilder` for
 * `+[range]` and `~N` blur, then `Index.find` of the baseline `LuceneIndex` / of the `FtsIndex`), which is what
 * `Book.find` does for an indexed book. Required per query: the same sorted osisRef list, or the same failure kind.
 *
 * A difference is reported but passes when it is approved in `jsword/parity/approved-differences.md`, either by
 * rule (`RULE <id>`, recognized by [approvedRule]) or for one case (`DIFF <Module> <query as a JSON string>`). Anything
 * else fails the module's test. One test method per module so one module can be run alone
 * (`--tests '*everyQueryMatchesBaseline_KJV*'`).
 *
 * Index cache: keyed by the module hash (the zip hash of `parity/k6-corpus.sha256`, or a fingerprint of the module
 * files for KJV/FinPR which come from testmods.zip) plus the analyzer id, [FTS_SCHEMA_VERSION] and a digest of the
 * compiled classes that shape an index (everything under `org/crosswire` except the query side), so a change to the builder,
 * the analyzers or the canonical text (book, filter, passage) cannot be masked by a stale cache. A change in a dependency
 * outside that (a resource, a library) is not seen: run `rm -rf jsword/build/k6-indexes` before gate runs.
 */
class SearchCorpusParityTest {
    private class Outcome(val hits: List<String>?, val failure: String?, val syntax: String? = null) {
        override fun toString() = hits?.let { "${it.size} verses ${it.take(3)}" } ?: "FAIL($failure)"
        fun sameAs(o: Outcome) = if (hits != null) hits == o.hits else o.hits == null && failure == o.failure
    }

    private fun baselineOutcome(root: String, initials: String, dir: File, query: String): Outcome {
        val out = OracleSearch.call("search", root, initials, dir.path, query)
        if (!out.startsWith("!")) return Outcome(out.lines().filter { it.isNotEmpty() }, null)
        // "!<class>: <message> [ <- <cause class>]"
        val cls = out.substring(1).substringBefore(':').substringAfterLast('.')
        val cause = if (" <- " in out) out.substringAfterLast(" <- ").substringAfterLast('.') else null
        return Outcome(null, kind(cls, cause))
    }

    private fun currentOutcome(searcher: LuceneSearcher, query: String): Outcome = try {
        val key = searcher.search(query)
        Outcome(key.map { it.getOsisRef() }.sorted(), null)
    } catch (e: Throwable) {
        Outcome(null, kind(e.javaClass.simpleName, e.cause?.javaClass?.simpleName), (e as? UnsupportedSearchSyntaxException)?.syntax)
    }

    /** Failure kind: the exception class and its cause's, with the baseline's Lucene `ParseException` and ours as one. */
    private fun kind(cls: String, cause: String?): String =
        listOfNotNull(cls, cause).joinToString("<-") { if (it == "ParseException") "QueryParseException" else it }

    // ---- approvals ----------------------------------------------------------------------------------------------

    /**
     * The approved-difference rules (ids as in `approved-differences.md`). Returns the rule that covers this
     * difference, or null. Every rule is narrow: it states the exact shape of both outcomes.
     */
    private fun approvedRule(query: String, base: Outcome, mine: Outcome): String? {
        // Matched on the syntax the parser reports (UnsupportedSearchSyntaxException.syntax), not on the query text.
        mine.syntax?.let { syn ->
            if (syn == "^" || syn == "[a TO b]" || syn == "{a TO b}" || (syn.endsWith(":") && !syn.startsWith("strong:"))) return "K6-3-unsupported-syntax"
            if (syn == "strong:term~" || syn == "strong:\"phrase\"") return "strong-phrase-or-fuzzy"
        }
        // term~1 (and larger): Lucene's FuzzyQuery throws a raw IllegalArgumentException, here a typed BookException.
        if (base.failure == "IllegalArgumentException" && mine.failure == "BookException<-QueryParseException" && FUZZY_INT.containsMatchIn(query)) {
            return "fuzzy-similarity-at-least-1"
        }
        if (query.count { it == '(' } > 200 && mine.failure?.startsWith("BookException") == true) return "nesting-over-200"
        return null
    }

    private fun approvedFile(): List<String> =
        File(System.getProperty("ksword.projectDir") ?: error("ksword.projectDir not set"), "parity/approved-differences.md").readLines()

    private fun approvedCases(initials: String): Set<String> =
        approvedFile().filter { it.startsWith("DIFF $initials ") }.map { it.removePrefix("DIFF $initials ").let { j -> unjson(j) } }.toSet()

    private fun unjson(s: String): String {
        require(s.startsWith("\"") && s.endsWith("\"")) { "DIFF entry is not a JSON string: $s" }
        val sb = StringBuilder()
        var i = 1
        while (i < s.length - 1) {
            val ch = s[i]
            if (ch != '\\') { sb.append(ch); i++; continue }
            when (val n = s[i + 1]) {
                'u' -> { sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 6 }
                'n' -> { sb.append('\n'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                else -> { sb.append(n); i += 2 }
            }
        }
        return sb.toString()
    }

    // ---- index cache --------------------------------------------------------------------------------------------

    private val buildDir get() = File(System.getProperty("ksword.projectDir") ?: error("ksword.projectDir not set"), "build/k6-indexes")

    private fun sha(vararg parts: ByteArray): String =
        MessageDigest.getInstance("SHA-256").also { md -> parts.forEach { md.update(it); md.update(0) } }.digest().joinToString("") { "%02x".format(it) }

    /** The zip hash of the module from `k6-corpus.sha256`, else a fingerprint of its conf and data files. */
    private fun moduleHash(root: File, initials: String): String {
        val projectDir = File(System.getProperty("ksword.projectDir")!!)
        val zipHash = File(projectDir, "parity/k6-corpus.sha256").readLines()
            .firstOrNull { it.trim().endsWith("  $initials.zip") }?.substringBefore(' ')
        val conf = File(root, "mods.d/${initials.lowercase()}.conf")
        val fingerprint = StringBuilder(conf.readText())
        File(root, "modules").walkTopDown().filter { it.isFile }.sortedBy { it.path }.forEach {
            // only this module's data: the conf's DataPath names its directory
            if (it.path.contains("/${initials.lowercase()}/")) fingerprint.append(it.relativeTo(root).path).append(':').append(it.length()).append('\n')
        }
        return sha((zipHash ?: "").toByteArray(), fingerprint.toString().toByteArray()).take(20)
    }

    /** Digest of the compiled classes whose change alters an index (builder, analyzers, codec, stemmers, dictionaries). */
    private val indexCodeDigest: String by lazy {
        val loc = File(FtsIndexBuilder::class.java.protectionDomain.codeSource.location.toURI())
        val md = MessageDigest.getInstance("SHA-256")
        if (loc.isFile) md.update(loc.readBytes()) else {
            Files.walk(loc.toPath()).use { s ->
                s.filter { Files.isRegularFile(it) }.map { loc.toPath().relativize(it).toString() to it }
                    .filter { (rel, _) ->
                        // every class that shapes an index: book access and canonical text (book, filter, passage, versification, ...),
                        // builder, analyzers, codecs; not the query side (`index/fts/query`, `index/query`, `index/search`)
                        (rel.contains("org/crosswire/") && !rel.contains("index/fts/query") && !rel.contains("index/search") && !rel.contains("index/query")) ||
                            rel.contains("tartarus") || rel.contains("snowball", true) || rel.contains("thai", true) || rel.contains("mmseg", true)
                    }
                    .sorted(compareBy { it.first }).forEach { (rel, p) -> md.update(rel.toByteArray()); md.update(Files.readAllBytes(p)) }
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }.take(16)
    }

    private fun book(root: File, initials: String): Book {
        SwordBookPathJvm.setAugmentPath(arrayOf(root))
        return requireNotNull(Books.installed().getBook(initials)) { "no such book $initials in $root" }
    }

    /** Builds (or reuses) the baseline index; returns its directory and the build time in ms (0 when cached). */
    private fun baselineIndex(root: File, initials: String, hash: String): Pair<File, Long> {
        val dir = File(buildDir, "baseline/$initials-$hash")
        val marker = File(dir.parentFile, "$initials-$hash.done")
        val index = File(dir, "index")
        if (marker.exists() && index.exists()) return index to 0L
        dir.deleteRecursively(); marker.delete()
        dir.mkdirs()
        val t0 = System.nanoTime()
        val built = OracleSearch.call("buildIndex", root.path, initials, index.path)
        assertTrue(built.startsWith("ok "), "baseline index of $initials: $built")
        marker.writeText(built)
        return index to (System.nanoTime() - t0) / 1_000_000
    }

    private fun currentManager(root: File, initials: String, hash: String, b: Book): Triple<FtsIndexManager, Long, Boolean> {
        val key = sha(hash.toByteArray(), "s$FTS_SCHEMA_VERSION".toByteArray(), BookAnalyzers.analyzerId(b.language.code).toByteArray(), indexCodeDigest.toByteArray()).take(20)
        val dir = File(buildDir, "current/$initials-$key")
        val m = FtsIndexManager(File(dir, "search").toOkioPath(), File(dir, "lucene").toOkioPath()) { bk, k -> BookData(bk, k).getOsisFragment(false) }
        if (m.isIndexed(b) && !m.needsReindexing(b)) return Triple(m, 0L, true)
        val t0 = System.nanoTime()
        m.scheduleIndexCreation(b)
        assertEquals(IndexStatus.DONE, b.indexStatus, "FTS index of $initials")
        return Triple(m, (System.nanoTime() - t0) / 1_000_000, false)
    }

    // ---- the matrix ---------------------------------------------------------------------------------------------

    private fun compareModule(initials: String) {
        val root = requireCorpus(K6Corpus.root, initials)
        val queries = QUERY_SPECS[initials] ?: fail("no query set for $initials")
        val t0 = System.nanoTime()
        val hash = moduleHash(root, initials)
        val b = book(root, initials)
        val (baseDir, baseMs) = baselineIndex(root, initials, hash)
        val (manager, curMs, curCached) = currentManager(root, initials, hash, b)
        val newDiffs = ArrayList<String>()
        val approved = ArrayList<String>()
        var nonEmpty = 0
        var failures = 0
        val approvedCases = approvedCases(initials)
        val floorSet = EXPECTED_HITS.getValue(initials)
        var floorHits = 0
        try {
            val searcher = LuceneSearcher().also { it.init(manager.getIndex(b)) }
            for (spec in queries) {
                val q = spec.query
                val base = baselineOutcome(root.path, initials, baseDir, q)
                val mine = currentOutcome(searcher, q)
                if (base.hits?.isNotEmpty() == true) nonEmpty++
                if (base.hits == null) failures++
                if (q in floorSet && base.hits?.isNotEmpty() == true && mine.hits?.isNotEmpty() == true) floorHits++
                if (base.sameAs(mine)) continue
                val rule = approvedRule(q, base, mine) ?: if (q in approvedCases) "DIFF-case" else null
                if (rule != null) approved += "[$rule] '$q': baseline $base, mine $mine"
                else newDiffs += "$initials '$q': baseline $base, mine $mine"
            }
        } finally {
            manager.closeAllIndexes()
        }
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("K6 CORPUS $initials: ${queries.size} queries, $nonEmpty non-empty (baseline), $failures failing on both-or-baseline side, " +
            "${approved.size} approved differences, ${newDiffs.size} new; floor $floorHits/${floorSet.size}; " +
            "baseline index ${if (baseMs == 0L) "cached" else "${baseMs} ms"}, FTS index ${if (curCached) "cached" else "${curMs} ms"}, total ${ms} ms")
        approved.forEach { println("K6 APPROVED $initials $it") }
        // the floor: an all-empty regression cannot pass
        assertTrue(floorHits * 10 >= floorSet.size * 6, "$initials: only $floorHits of ${floorSet.size} expected-hit queries are non-empty on both sides (floor 60%)")
        if (newDiffs.isNotEmpty()) fail("NEW parity differences (${newDiffs.size}):\n" + newDiffs.joinToString("\n"))
    }

    @Test fun everyQueryMatchesBaseline_ChiUns() = compareModule("ChiUns")
    @Test fun everyQueryMatchesBaseline_ChiUn() = compareModule("ChiUn")
    @Test fun everyQueryMatchesBaseline_JapKougo() = compareModule("JapKougo")
    @Test fun everyQueryMatchesBaseline_ThaiKJV() = compareModule("ThaiKJV")
    @Test fun everyQueryMatchesBaseline_GerBoLut() = compareModule("GerBoLut")
    @Test fun everyQueryMatchesBaseline_GreVamvas() = compareModule("GreVamvas")
    @Test fun everyQueryMatchesBaseline_Byz() = compareModule("Byz")
    @Test fun everyQueryMatchesBaseline_HebModern() = compareModule("HebModern")
    @Test fun everyQueryMatchesBaseline_WLC() = compareModule("WLC")
    @Test fun everyQueryMatchesBaseline_AraSVD() = compareModule("AraSVD")
    @Test fun everyQueryMatchesBaseline_FarOPV() = compareModule("FarOPV")
    @Test fun everyQueryMatchesBaseline_CzeBKR() = compareModule("CzeBKR")
    @Test fun everyQueryMatchesBaseline_RusSynodal() = compareModule("RusSynodal")
    @Test fun everyQueryMatchesBaseline_FreCrampon() = compareModule("FreCrampon")
    @Test fun everyQueryMatchesBaseline_MHC() = compareModule("MHC")
    @Test fun everyQueryMatchesBaseline_KJV() = compareModule("KJV")
    @Test fun everyQueryMatchesBaseline_FinPR() = compareModule("FinPR")

    // ---- guards of the matrix itself (no corpus needed unless said) -----------------------------------------------

    @Test fun everyCorpusModuleHasAtLeast25Queries() {
        assertEquals(K6Corpus.modules.toSet(), QUERY_SETS.keys, "QUERY_SETS covers exactly the corpus modules")
        for ((m, qs) in QUERY_SETS) {
            assertTrue(qs.size >= 25, "$m has only ${qs.size} queries")
            assertEquals(qs.size, qs.toSet().size, "$m has duplicate queries")
            assertTrue(EXPECTED_HITS.getValue(m).size >= 15, "$m has few expected-hit queries")
        }
    }

    @Test fun theUiQueryShapesAreTheBaselineDecoratorOutput() {
        val s = org.crosswire.jsword.index.search.SearchType::class.java
        assertEquals("+a +b", org.crosswire.jsword.index.search.SearchType.ALL_WORDS.decorate("a b"))
        assertEquals("a b", org.crosswire.jsword.index.search.SearchType.ANY_WORDS.decorate("a b"))
        assertEquals("\"a b\"", org.crosswire.jsword.index.search.SearchType.PHRASE.decorate("a b"))
        assertTrue(s.isEnum)
        val kjv = QUERY_SETS.getValue("KJV")
        for (shape in listOf("+[Gen-Mal] ", "+[Mat-Rev] ", "+[Rom] ")) assertTrue(kjv.any { it.startsWith(shape) }, shape)
        assertTrue(kjv.any { it.startsWith("strong:") } && kjv.any { Regex("\\w+~0\\.[579]").containsMatchIn(it) } && kjv.any { Regex("\"[^\"]+\"~\\d+").containsMatchIn(it) })
    }

    /** A missing or empty corpus fails loudly (never skips), as does a module the corpus does not hold. */
    @Test fun aMissingCorpusFails() {
        val empty = Files.createTempDirectory("k6-no-corpus").toFile()
        try {
            assertFailsWith<IllegalStateException> { requireCorpus(empty, "KJV") }
            File(empty, "mods.d").mkdirs()
            assertFailsWith<IllegalStateException> { requireCorpus(empty, "KJV") }
            File(empty, "mods.d/other.conf").writeText("[Other]\n")
            assertFailsWith<IllegalStateException> { requireCorpus(empty, "KJV") }
        } finally {
            empty.deleteRecursively()
        }
    }

    @Test fun approvedDifferencesFileNamesEveryRuleTheTestKnows() {
        val text = approvedFile().joinToString("\n")
        for (rule in RULES) assertTrue("RULE $rule" in text, "approved-differences.md has no 'RULE $rule' entry")
    }

    private fun requireCorpus(root: File, initials: String): File {
        check(File(root, "mods.d").list().orEmpty().isNotEmpty()) { "K6 corpus missing or empty at $root; run jsword/scripts/fetch-k6-corpus.sh" }
        check(File(root, "mods.d/${initials.lowercase()}.conf").isFile) { "module $initials is not in the K6 corpus at $root" }
        return root
    }

    private companion object {
        val RULES = listOf("K6-3-unsupported-syntax", "strong-phrase-or-fuzzy", "fuzzy-similarity-at-least-1", "nesting-over-200")
        val FUZZY_INT = Regex("~[1-9][0-9]*(\\.[0-9]+)?(\\s|\\)|$)")
    }
}
