package org.crosswire.jsword.index.analysis.mmseg

/** mmseg4j 1.8.6 `ComplexSeg`: complex maximum matching over a run of "other letters" (CJK, kana, ...). */
internal class ComplexSeg(private val dictionary: Dictionary) {
    /** Splits [sentence] into words. Only local state is used; the instance is safe to share. */
    fun segment(sentence: CharArray): List<String> {
        val result = ArrayList<String>()
        var offset = 0
        while (offset < sentence.size) {
            val firstTails = dictionary.matchingTailLengths(sentence, offset)
            val candidates = ArrayList<Chunk>()
            var maximumLength = 0
            for (a in firstTails.indices.reversed()) {
                val first = firstTails[a]
                val secondOffset = offset + first + 1
                val secondTails = dictionary.matchingTailLengths(sentence, secondOffset)
                for (b in secondTails.indices.reversed()) {
                    val second = secondTails[b]
                    val thirdOffset = secondOffset + second + 1
                    val third = dictionary.matchingTailLengths(sentence, thirdOffset).last()
                    val sumLength = first + second + third + 3
                    if (sumLength < maximumLength) continue
                    maximumLength = sumLength
                    val tails = intArrayOf(first, second, third)
                    val offsets = intArrayOf(offset, secondOffset, thirdOffset)
                    val lengths = ArrayList<Int>(3)
                    val degrees = ArrayList<Int>(3)
                    for (k in 0 until 3) {
                        if (offsets[k] >= sentence.size) continue
                        val length = tails[k] + 1
                        lengths.add(length)
                        degrees.add(if (length == 1) dictionary.degree(sentence[offsets[k]]) else -1)
                    }
                    if (lengths.isNotEmpty()) candidates.add(Chunk(lengths.toIntArray(), degrees.toIntArray()))
                }
            }
            val selected = Rules.select(candidates)
            if (selected == null) {
                result.add(sentence[offset].toString())
                offset++
                continue
            }
            var start = offset
            for (l in selected.lengths) {
                result.add(sentence.concatToString(start, start + l))
                start += l
            }
            offset += selected.length
        }
        return result
    }
}
