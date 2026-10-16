package org.crosswire.ksword.parity.search

import java.io.File
import kotlin.random.Random
import kotlin.test.Test

/**
 * Regenerates the per-module word picks hard-coded in [SearchQuerySets] (`MODULE_WORDS`). Not a check: it does
 * nothing unless the environment variable `K6_PRINT_WORDS` names an output file. Deterministic (fixed seed per
 * module) given the corpus whose hashes are in `parity/k6-corpus.sha256`.
 *
 * Picking rule: split the canonical text of the whole module into words (maximal runs of letters, marks and digits,
 * at least 3 characters; CJK modules use the character bigrams of Han/kana runs, Thai the space-separated chunks),
 * rank them by frequency and take a seeded shuffle of the words in the 2%..25% rank window, so every pick occurs
 * in many verses without being a function word. Phrase picks are adjacent word pairs/triples of seeded verses.
 */
class SearchQueryWordPicker {
    @Test fun printWords() {
        val out = System.getenv("K6_PRINT_WORDS") ?: return
        val sb = StringBuilder()
        for (initials in K6Corpus.modules) {
            val text = OracleSearch.call("canonicalText", K6Corpus.root.path, initials, "1")
            val verses = text.lines().filter { it.contains('\t') }.map { it.substringAfter('\t') }
            val rnd = Random(initials.hashCode() xor 1406)
            val cjk = initials in setOf("ChiUns", "ChiUn", "JapKougo")
            val thai = initials == "ThaiKJV"
            fun words(v: String): List<String> = when {
                cjk -> Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー]+").findAll(v).flatMap { m ->
                    val s = m.value; (0 until s.length - 1).map { s.substring(it, it + 2) }.asSequence()
                }.toList()
                thai -> v.split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.length in 4..12 }
                else -> v.split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.length >= 3 }
            }
            val freq = HashMap<String, Int>()
            verses.forEach { v -> words(v).forEach { freq.merge(it, 1, Int::plus) } }
            val ranked = freq.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key }
            val window = ranked.subList((ranked.size * 0.02).toInt(), (ranked.size * 0.25).toInt().coerceAtLeast(30))
            val picks = window.shuffled(rnd).take(12)
            fun phrases(n: Int, count: Int): List<String> {
                val result = ArrayList<String>()
                var guard = 0
                while (result.size < count && guard++ < 10000) {
                    val v = verses[rnd.nextInt(verses.size)]
                    val ws = if (cjk) listOf(Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}ー]{4,}").find(v)?.value ?: continue)
                    else words(v)
                    if (cjk) { val s = ws[0]; val st = rnd.nextInt(s.length - 3); result += s.substring(st, st + 4); continue }
                    if (ws.size < n + 2) continue
                    val st = rnd.nextInt(ws.size - n)
                    result += ws.subList(st, st + n).joinToString(" ")
                }
                return result
            }
            val p2 = phrases(2, 3)
            val p3 = phrases(3, 2)
            fun lit(l: List<String>) = l.joinToString(", ") { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" }
            sb.append("\"$initials\" to ModuleWords(\n    words = listOf(${lit(picks)}),\n    pairs = listOf(${lit(p2)}),\n    triples = listOf(${lit(p3)}),\n),\n")
            println("K6 words $initials: ${verses.size} verses, ${freq.size} distinct, picks ${picks.take(4)}")
        }
        File(out).writeText(sb.toString())
    }
}
