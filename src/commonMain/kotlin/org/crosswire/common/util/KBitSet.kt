package org.crosswire.common.util

/**
 * The subset of `java.util.BitSet` that the passage classes use (K3). `hashCode`, `equals` and `toString` give the
 * same values as `java.util.BitSet`.
 */
class KBitSet(nbits: Int = 64) {
    private var words = LongArray(wordIndex(maxOf(nbits, 1) - 1) + 1)

    operator fun get(i: Int): Boolean {
        require(i >= 0) { "bitIndex < 0: $i" }
        val w = wordIndex(i)
        return w < words.size && (words[w] and (1L shl i)) != 0L
    }

    fun set(i: Int) {
        require(i >= 0) { "bitIndex < 0: $i" }
        val w = wordIndex(i)
        if (w >= words.size) words = words.copyOf(maxOf(2 * words.size, w + 1))
        words[w] = words[w] or (1L shl i)
    }

    fun clear(i: Int) {
        require(i >= 0) { "bitIndex < 0: $i" }
        val w = wordIndex(i)
        if (w < words.size) words[w] = words[w] and (1L shl i).inv()
    }

    fun clear() = words.fill(0L)

    fun nextSetBit(from: Int): Int {
        require(from >= 0) { "fromIndex < 0: $from" }
        var w = wordIndex(from)
        if (w >= words.size) return -1
        var word = words[w] and (-1L shl from)
        while (true) {
            if (word != 0L) return w * 64 + word.countTrailingZeroBits()
            if (++w == words.size) return -1
            word = words[w]
        }
    }

    fun cardinality(): Int = words.sumOf { it.countOneBits() }

    val isEmpty: Boolean get() = words.all { it == 0L }

    fun and(o: KBitSet) {
        for (i in words.indices) words[i] = if (i < o.words.size) words[i] and o.words[i] else 0L
    }

    fun or(o: KBitSet) {
        if (o.words.size > words.size) words = words.copyOf(o.words.size)
        for (i in o.words.indices) words[i] = words[i] or o.words[i]
    }

    fun andNot(o: KBitSet) {
        for (i in 0 until minOf(words.size, o.words.size)) words[i] = words[i] and o.words[i].inv()
    }

    fun copy(): KBitSet = KBitSet(1).also { it.words = words.copyOf() }

    override fun equals(other: Any?): Boolean {
        if (other !is KBitSet) return false
        val n = maxOf(words.size, other.words.size)
        for (i in 0 until n) if (words.getOrElse(i) { 0L } != other.words.getOrElse(i) { 0L }) return false
        return true
    }

    override fun hashCode(): Int {
        var h = 1234L
        for (i in words.indices.reversed()) h = h xor (words[i] * (i + 1))
        return ((h shr 32) xor h).toInt()
    }

    override fun toString(): String = buildString {
        append('{')
        var i = nextSetBit(0)
        var first = true
        while (i >= 0) {
            if (!first) append(", ")
            append(i); first = false
            i = nextSetBit(i + 1)
        }
        append('}')
    }

    private companion object {
        fun wordIndex(bit: Int) = bit shr 6
    }
}
