package org.crosswire.ksword.parity.search

import java.io.File
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

/** Reads a module's conf and refuses any module whose `DistributionLicense` is not exactly `Public Domain`. */
internal object K6FixtureLicense {
    /** The conf as a key/value map (first value per key; `Key=Value` lines, continuation lines ignored). */
    fun conf(root: File, initials: String): Map<String, String> {
        val file = File(root, "mods.d/${initials.lowercase()}.conf")
        check(file.isFile) { "no conf for $initials at $file" }
        val out = LinkedHashMap<String, String>()
        file.readLines().forEach { line ->
            val i = line.indexOf('=')
            if (i > 0 && !line.startsWith("[") && !line.startsWith("#")) out.putIfAbsent(line.substring(0, i).trim(), line.substring(i + 1).trim())
        }
        return out
    }

    /** Fails loudly, naming the module, unless the license is exactly `Public Domain`. Returns the conf. */
    fun requirePublicDomain(root: File, initials: String): Map<String, String> {
        val conf = conf(root, initials)
        val license = conf["DistributionLicense"]
        check(license == "Public Domain") {
            "REFUSED: module $initials has DistributionLicense='$license', not 'Public Domain'; its text must never be committed as a fixture"
        }
        return conf
    }
}

/** The KJV conf of the corpus says "General public license for distribution for any purpose": it must be refused. */
class K6FixtureLicenseTest {
    @Test fun refusesAModuleThatIsNotExactlyPublicDomain() {
        val root = K6Corpus.root
        try {
            K6FixtureLicense.requirePublicDomain(root, "KJV")
            fail("KJV must be refused")
        } catch (e: IllegalStateException) {
            assert("KJV" in e.message!!) { e.message!! }
        }
        assertEquals("Public Domain", K6FixtureLicense.requirePublicDomain(root, "ChiUns")["DistributionLicense"])
    }
}

/**
 * K6 task 14B generator of the iOS search fixtures: `jsword/src/commonTest/k6-fixtures/<Module>.tsv`. Run by the Gradle
 * task `generateK6IosFixtures` (needs `.local/k6-corpus` and the baseline Lucene 3.6.2 oracle); never part of a normal test run.
 *
 * Modules: only those whose conf says `DistributionLicense=Public Domain` ([K6FixtureLicense]; a module that does not is
 * refused with an error naming it). KJV is not on the list: its conf in the corpus carries a general-public-license wording.
 *
 * File format (UTF-8, `\\`, `\t`, `\n`, `\r` escaped inside fields):
 * ```
 * #module<TAB><initials><TAB><language code>
 * <osisRef><TAB><canonical text><TAB><oracle tokens>        (200 lines)
 * #queries
 * <query><TAB><space separated osisRefs of the oracle result, sorted>   (up to 10 lines)
 * ```
 * Oracle tokens are the baseline `analyzeBook` rendering: terms separated by a space, `@n` when the position increment is not 1.
 *
 * Verse choice: the module's verses with non-empty canonical text and tokens are cut into 200 equal buckets of the
 * canonical order; one verse per bucket is drawn with a `java.util.Random` seeded by the module name (deterministic, varied).
 * Strong's numbers are not part of the fixtures (the format has no column for them; they are covered by StrongsTokensTest).
 *
 * Query oracle: the baseline cannot build an index over a verse subset (the probe only knows whole books), so the query
 * is run on the baseline index of the FULL module (the cache of [SearchCorpusParityTest] under `build/k6-indexes/baseline`,
 * built if absent) and its result is intersected with the 200 verse keys. That is exact for terms, phrases, booleans and
 * proximity (a verse matches a query on a subset index iff it matches on the full index, the clauses being per verse).
 * Wildcard and fuzzy expansion depend on the index vocabulary (a subset has fewer terms), but a verse matches iff it contains
 * a term satisfying the term-level predicate, which holds on both indexes; the only exception is the fuzzy top-1024 cap, so
 * fuzzy queries use long words with a similarity that keeps the expansion small. The assumption is checked, not trusted:
 * `PdFixtureSearchTest` runs every query on an index of exactly the 200 verses and must reproduce these sets.
 * Candidate queries are drawn from the words of the chosen verses; a candidate is kept when the baseline accepts it and
 * the intersection is non-empty. Ten query shapes (word, OR, AND, phrase, NOT, trailing/leading wildcard, `?`, fuzzy,
 * proximity) are tried in turn with up to 80 candidates each.
 */
class GenerateIosSearchFixtures {
    private val modules = listOf("ChiUns", "ChiUn", "JapKougo", "GerBoLut", "GreVamvas", "CzeBKR", "RusSynodal", "FreCrampon")

    private fun escape(text: String) =
        text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun baselineIndex(root: File, initials: String): File {
        val cache = File(System.getProperty("ksword.projectDir"), "build/k6-indexes/baseline")
        cache.listFiles { f -> f.isDirectory && f.name.startsWith("$initials-") }?.sortedBy { it.name }?.forEach {
            val idx = File(it, "index")
            if (File(cache, it.name + ".done").exists() && idx.exists()) return idx
        }
        val dir = File(cache, "$initials-fixtures")
        val idx = File(dir, "index")
        dir.deleteRecursively(); dir.mkdirs()
        val built = OracleSearch.call("buildIndex", root.path, initials, idx.path)
        check(built.startsWith("ok ")) { "baseline index of $initials: $built" }
        return idx
    }

    private class Verse(val ref: String, val text: String, val tokens: String)

    private fun isCjk(c: Char) = c.code in 0x3040..0x30FF || c.code in 0x3400..0x9FFF

    @Test fun generate() {
        val out = File(System.getProperty("ksword.k6.fixturesOut") ?: error("ksword.k6.fixturesOut not set"))
        out.mkdirs()
        val root = K6Corpus.root
        for (initials in modules) generateModule(root, initials, out)
    }

    private fun generateModule(root: File, initials: String, out: File) {
        val conf = K6FixtureLicense.requirePublicDomain(root, initials)
        val lang = conf["Lang"] ?: error("no Lang for $initials")
        val texts = OracleSearch.call("canonicalText", root.path, initials, "1").lines().filter { it.isNotEmpty() }
            .associate { it.substringBefore('\t') to it.substringAfter('\t', "") }
        val tokens = OracleSearch.call("analyzeBook", root.path, initials, "1").lines().filter { it.isNotEmpty() }
            .associate { it.substringBefore('\t') to it.substringAfter('\t', "") }
        check(texts.isNotEmpty() && texts.size == tokens.size) { "$initials: ${texts.size} texts, ${tokens.size} token lines" }
        val eligible = texts.keys.filter { texts.getValue(it).trim().length >= 12 && tokens[it].orEmpty().isNotBlank() }
        // The seed is String.hashCode() of the module name: specified by the Java language, so stable across platforms and JVMs.
        val rnd = Random(initials.hashCode().toLong())
        val chosen = (0 until 200).map { b ->
            val from = b * eligible.size / 200
            val to = (b + 1) * eligible.size / 200
            eligible[from + rnd.nextInt(maxOf(1, to - from))]
        }.distinct()
        check(chosen.size >= 190) { "$initials: only ${chosen.size} verses chosen" }
        val verses = chosen.map { Verse(it, texts.getValue(it), tokens.getValue(it)) }
        val keys = chosen.toSet()
        val index = baselineIndex(root, initials)

        fun run(query: String): List<String>? {
            val raw = OracleSearch.call("find", root.path, initials, index.path, query)
            if (raw.startsWith("!")) return null
            return raw.lines().filter { it in keys }
        }

        val cjk = verses.any { v -> v.text.any { isCjk(it) } }
        val queries = LinkedHashMap<String, List<String>>()
        val r = Random(("q" + initials).hashCode().toLong())
        // (verse text as unescaped words) -> candidates per shape
        val words = verses.map { v ->
            if (cjk) v.text.filter { isCjk(it) }.map { it.toString() }
            else Regex("\\p{L}+").findAll(v.text).map { it.value.lowercase() }.toList()
        }
        fun tryShape(name: String, gen: () -> String?) {
            repeat(80) {
                val q = gen() ?: return@repeat
                if (q in queries) return@repeat
                val res = run(q) ?: return@repeat
                if (res.isNotEmpty()) { queries[q] = res; return }
            }
            println("K6 FIXTURES $initials: no candidate for shape $name")
        }
        fun pickVerse(minWords: Int): List<String>? {
            val c = words.filter { it.size >= minWords }
            return if (c.isEmpty()) null else c[r.nextInt(c.size)]
        }
        // Latin words: at least 4 letters; CJK: single characters or character runs.
        fun pickWord(w: List<String>): String? {
            val c = if (cjk) w else w.filter { it.length >= 4 }
            return if (c.isEmpty()) null else c[r.nextInt(c.size)]
        }
        fun unit(w: String) = w // a CJK single char or a Latin word
        tryShape("word") { pickVerse(4)?.let { pickWord(it) }?.let(::unit) }
        tryShape("or") { pickVerse(4)?.let { v -> val a = pickWord(v); val b = pickWord(v); if (a != null && b != null && a != b) "$a $b" else null } }
        tryShape("and") { pickVerse(4)?.let { v -> val a = pickWord(v); val b = pickWord(v); if (a != null && b != null && a != b) "+$a +$b" else null } }
        tryShape("phrase") {
            pickVerse(5)?.let { v ->
                val i = r.nextInt(v.size - 1)
                if (cjk) "\"${v.subList(i, minOf(v.size, i + 3)).joinToString("")}\"" else "\"${v[i]} ${v[i + 1]}\""
            }
        }
        tryShape("not") { pickVerse(4)?.let { v -> val a = pickWord(v); val b = pickWord(pickVerse(4) ?: v); if (a != null && b != null && a != b) "$a -$b" else null } }
        tryShape("prefix") { pickVerse(4)?.let { pickWord(it) }?.let { if (cjk) "$it*" else it.take(4) + "*" } }
        tryShape("suffix") { pickVerse(4)?.let { pickWord(it) }?.let { if (cjk) "*$it" else "*" + it.takeLast(4) } }
        tryShape("single-char") { pickVerse(4)?.let { pickWord(it) }?.let { if (cjk) "$it?" else it.substring(0, 2) + "?" + it.substring(3) } }
        tryShape("fuzzy") {
            pickVerse(4)?.let { v ->
                val w = (if (cjk) v else v.filter { it.length >= 6 }).let { if (it.isEmpty()) null else it[r.nextInt(it.size)] }
                w?.let { if (cjk) "$it~0.5" else it.dropLast(1) + "x~0.8" }
            }
        }
        tryShape("proximity") {
            pickVerse(6)?.let { v ->
                val i = r.nextInt(v.size - 3)
                if (cjk) "\"${v[i]} ${v[i + 3]}\"~4" else "\"${v[i]} ${v[i + 3]}\"~4"
            }
        }

        File(out, "$initials.tsv").bufferedWriter().use { w ->
            w.write("#module\t$initials\t$lang\n")
            verses.forEach { w.write("${it.ref}\t${escape(it.text)}\t${it.tokens}\n") }
            w.write("#queries\n")
            queries.forEach { (q, res) -> w.write("${escape(q)}\t${res.sorted().joinToString(" ")}\n") }
        }
        println("K6 FIXTURES $initials: ${verses.size} verses, ${queries.size} queries ${queries.map { it.value.size }}")
    }
}
