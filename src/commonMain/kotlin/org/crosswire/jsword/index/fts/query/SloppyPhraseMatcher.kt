/*
 * Provenance: hand port of `org.apache.lucene.search.SloppyPhraseScorer`, `PhrasePositions`, `PhraseQueue` and the
 * array heap of `org.apache.lucene.util.PriorityQueue` from Apache Lucene 3.6.2 (`lucene-core`).
 * Lucene 3.6.2 (Apache License 2.0, see src/embeddedResources/search/LICENSE.apache-2.0.txt); listed in
 * jsword/NOTICE-third-party.md.
 */

package org.crosswire.jsword.index.fts.query

import org.crosswire.jsword.index.analysis.AnalyzedToken

/**
 * Decides whether one document (a verse) contains a sloppy phrase (`"a b"~N`) exactly as Lucene 3.6.2 does.
 *
 * This is a faithful port of Lucene's `SloppyPhraseScorer` (`phraseFreq` for one document, with its repeating-terms
 * handling `advanceRpts` / `advanceRepeatGroups` / `collide`), `PhrasePositions` and `PhraseQueue` (a port of Lucene's
 * array heap `PriorityQueue`, because the repeat handling advances positions of elements that sit inside the heap and
 * the resulting heap order is part of the behaviour). A document matches when the scorer's phrase frequency is above
 * zero, that is when it finds a match length (`end - min`) of at most the slop.
 *
 * The rule that falls out, with `o_i` the query position of phrase term `i` and `p_i` the document position chosen for
 * it: `max(p_i - o_i) - min(p_i - o_i) <= slop`, repeated terms of the phrase on distinct document positions. Two terms
 * in reverse order need a slop of the distance plus two (`"b a"~N` on `a b` needs N >= 2, on `a x b` N >= 3). The
 * scorer is not defined by that formula (its running `end` and the greedy repeat resolution might in principle miss a
 * match the formula allows), but no difference was observed on 4,000 seeded random cases (`SloppyPhraseMatcherTest`)
 * nor against the baseline oracle; the port reproduces the scorer, and the formula is used only as an upper bound.
 *
 * Positions are the analyzer's stream positions ([AnalyzedToken.position]), never list indexes: streams can have position
 * gaps (the ClassicTokenizer drops over-long tokens, Greek/Hebrew/Thai among them, and Thai pieces can skip positions;
 * stop words are not removed) and Lucene's phrase offsets include them. Query tokens that share a
 * position (Lucene would build a `MultiPhraseQuery`) are not supported ([hasSamePosition]).
 */
object SloppyPhraseMatcher {
    /** True when two tokens of [phrase] share a position (a `MultiPhraseQuery` in Lucene; not ported). */
    fun hasSamePosition(phrase: List<AnalyzedToken>): Boolean = phrase.map { it.position }.toSet().size != phrase.size

    /** Convenience for gap-free text: positions are the list indexes. */
    fun matchesWords(phrase: List<String>, slop: Int, tokens: List<String>): Boolean =
        matches(phrase.mapIndexed { i, t -> AnalyzedToken(t, i) }, slop, tokens.mapIndexed { i, t -> AnalyzedToken(t, i) })

    /**
     * Does [tokens] (one document, with stream positions) match the sloppy [phrase] (query tokens with their query
     * positions) at [slop]? A phrase of fewer than two tokens is a plain term: it matches when the token occurs.
     */
    fun matches(phrase: List<AnalyzedToken>, slop: Int, tokens: List<AnalyzedToken>): Boolean {
        if (phrase.isEmpty()) return false
        val byText = HashMap<String, MutableList<Int>>()
        for (t in tokens) byText.getOrPut(t.text) { ArrayList() } += t.position
        val base = phrase[0].position
        val pps = ArrayList<PP>(phrase.size)
        for ((i, q) in phrase.withIndex()) {
            val ps = byText[q.text] ?: return false
            ps.sort()
            pps += PP(q.text, q.position - base, i, ps.toIntArray())
        }
        if (pps.size < 2) return true
        return Scorer(pps, slop).phraseFreqPositive()
    }

    private class PP(val term: String, val offset: Int, val ord: Int, val positions: IntArray) {
        var position = 0
        var index = 0
        var rptGroup = -1
        var rptInd = 0

        fun firstPosition() { index = 0; nextPosition() }

        fun nextPosition(): Boolean {
            if (index < positions.size) { position = positions[index++] - offset; return true }
            return false
        }
    }

    /** Lucene's `PhraseQueue` over the array heap of Lucene's `PriorityQueue` (1-based, same sift loops). */
    private class Queue(capacity: Int) {
        private val heap = arrayOfNulls<PP>(capacity + 1)
        var size = 0
            private set

        private fun lessThan(a: PP, b: PP): Boolean =
            if (a.position == b.position) { if (a.offset == b.offset) a.ord < b.ord else a.offset < b.offset } else a.position < b.position

        fun clear() { for (i in 0..size) heap[i] = null; size = 0 }

        fun add(pp: PP) { size++; heap[size] = pp; upHeap() }

        fun top(): PP = heap[1]!!

        fun pop(): PP {
            val result = heap[1]!!
            heap[1] = heap[size]; heap[size] = null; size--
            if (size > 0) downHeap()
            return result
        }

        private fun upHeap() {
            var i = size
            val node = heap[i]!!
            var j = i ushr 1
            while (j > 0 && lessThan(node, heap[j]!!)) { heap[i] = heap[j]; i = j; j = j ushr 1 }
            heap[i] = node
        }

        private fun downHeap() {
            var i = 1
            val node = heap[i]!!
            var j = i shl 1
            var k = j + 1
            if (k <= size && lessThan(heap[k]!!, heap[j]!!)) j = k
            while (j <= size && lessThan(heap[j]!!, node)) {
                heap[i] = heap[j]; i = j
                j = i shl 1; k = j + 1
                if (k <= size && lessThan(heap[k]!!, heap[j]!!)) j = k
            }
            heap[i] = node
        }
    }

    /** `SloppyPhraseScorer` for one document; every phrase term is its own [PP] (no multi-term positions). */
    private class Scorer(private val pps: List<PP>, private val slop: Int) {
        private val pq = Queue(pps.size)
        private var end = Int.MIN_VALUE
        private var hasRpts = false
        private lateinit var rptGroups: List<Array<PP>>
        private val rptStack = arrayOfNulls<PP>(pps.size)

        fun phraseFreqPositive(): Boolean {
            if (!initPhrasePositions()) return false
            var matched = false
            var pp = pq.pop()
            var matchLength = end - pp.position
            var next = pq.top().position
            while (advancePP(pp)) {
                if (hasRpts && !advanceRpts(pp)) break
                if (pp.position > next) {
                    if (matchLength <= slop) matched = true
                    pq.add(pp)
                    pp = pq.pop()
                    next = pq.top().position
                    matchLength = end - pp.position
                } else {
                    val matchLength2 = end - pp.position
                    if (matchLength2 < matchLength) matchLength = matchLength2
                }
            }
            if (matchLength <= slop) matched = true
            return matched
        }

        private fun advancePP(pp: PP): Boolean {
            if (!pp.nextPosition()) return false
            if (pp.position > end) end = pp.position
            return true
        }

        private fun advanceRpts(start: PP): Boolean {
            var pp = start
            if (pp.rptGroup < 0) return true
            val rg = rptGroups[pp.rptGroup]
            val bits = BooleanArray(rg.size)
            val k0 = pp.rptInd
            var k = collide(pp)
            while (k >= 0) {
                pp = lesser(pp, rg[k])
                if (!advancePP(pp)) return false
                if (k != k0) bits[k] = true
                k = collide(pp)
            }
            var n = 0
            while (bits.any { it }) {
                val pp2 = pq.pop()
                rptStack[n++] = pp2
                if (pp2.rptGroup >= 0 && bits[pp2.rptInd]) bits[pp2.rptInd] = false
            }
            for (i in n - 1 downTo 0) pq.add(rptStack[i]!!)
            return true
        }

        private fun lesser(a: PP, b: PP): PP =
            if (a.position < b.position || (a.position == b.position && a.offset < b.offset)) a else b

        private fun tpPos(pp: PP) = pp.position + pp.offset

        private fun collide(pp: PP): Int {
            val tp = tpPos(pp)
            for (other in rptGroups[pp.rptGroup]) if (other !== pp && tpPos(other) == tp) return other.rptInd
            return -1
        }

        /** `initFirstTime` / `initSimple` / `initComplex`: the repeat groups depend only on the phrase terms. */
        private fun initPhrasePositions(): Boolean {
            end = Int.MIN_VALUE
            for (pp in pps) pp.firstPosition()
            val repeated = pps.groupingBy { it.term }.eachCount().filterValues { it > 1 }.keys
            hasRpts = repeated.isNotEmpty()
            if (hasRpts) {
                // gatherRptGroups without multi-terms: pps on the same first position (= the same term) form a group
                val groups = ArrayList<ArrayList<PP>>()
                for (i in pps.indices) {
                    val pp = pps[i]
                    if (pp.term !in repeated || pp.rptGroup >= 0) continue
                    for (j in i + 1 until pps.size) {
                        val pp2 = pps[j]
                        if (pp2.rptGroup >= 0 || pp2.term != pp.term) continue
                        if (pp.rptGroup < 0) { pp.rptGroup = groups.size; groups += arrayListOf(pp) }
                        pp2.rptGroup = pp.rptGroup
                        groups[pp.rptGroup] += pp2
                    }
                }
                rptGroups = groups.map { g -> g.sortedBy { it.offset }.toTypedArray().also { a -> a.forEachIndexed { i, p -> p.rptInd = i } } }
                if (!advanceRepeatGroups()) return false
            }
            pq.clear()
            for (pp in pps) { if (pp.position > end) end = pp.position; pq.add(pp) }
            return true
        }

        private fun advanceRepeatGroups(): Boolean {
            for (rg in rptGroups) {
                for (j in 1 until rg.size) for (k in 0 until j) if (!rg[j].nextPosition()) return false
            }
            return true
        }
    }
}
