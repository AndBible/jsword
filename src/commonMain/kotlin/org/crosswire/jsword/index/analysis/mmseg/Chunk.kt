package org.crosswire.jsword.index.analysis.mmseg

/**
 * mmseg4j `Chunk`: up to three consecutive words, each given by its length and the degree of its first
 * character (only meaningful for one-character words; -1 otherwise).
 */
internal class Chunk(val lengths: IntArray, val degrees: IntArray) {
    val size: Int get() = lengths.size

    val length: Int get() = lengths.sum()

    val averageLength: Double get() = length.toDouble() / size

    /** Variance of the word lengths (mmseg4j `Chunk.getVariance`, computed as in the Swift port). */
    val variance: Double
        get() {
            val average = averageLength
            var sum = 0.0
            for (l in lengths) {
                val d = l - average
                sum += d * d
            }
            return sum / size
        }

    /** Sum of the single-character degrees that are known (> -1). */
    val sumDegree: Int get() = degrees.filter { it > -1 }.sum()
}
