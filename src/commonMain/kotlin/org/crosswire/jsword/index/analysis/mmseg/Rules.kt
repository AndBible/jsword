package org.crosswire.jsword.index.analysis.mmseg

/**
 * mmseg4j `ComplexSeg`'s four disambiguation rules, applied in order while more than one chunk remains:
 * maximum matching length, largest average word length, smallest variance, largest sum of single-character degrees.
 * The first remaining chunk (insertion order) wins a final tie.
 */
internal object Rules {
    fun select(candidates: List<Chunk>): Chunk? {
        if (candidates.isEmpty()) return null
        var c = candidates
        val maximum = c.maxOf { it.length }
        c = c.filter { it.length == maximum }
        if (c.size > 1) {
            val best = c.maxOf { it.averageLength }
            c = c.filter { it.averageLength == best }
        }
        if (c.size > 1) {
            val best = c.minOf { it.variance }
            c = c.filter { it.variance == best }
        }
        if (c.size > 1) {
            val best = c.maxOf { it.sumDegree }
            c = c.filter { it.sumDegree == best }
        }
        return c.first()
    }
}
