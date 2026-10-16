package org.crosswire.jsword.index.analysis.mmseg

import kotlin.math.ln
import org.crosswire.jsword.index.analysis.AnalyzerTables

/**
 * The immutable mmseg4j 1.8.6 dictionary: a word trie keyed by UTF-16 units, the single-character frequency
 * "degrees" of `chars.dic`, and the unit characters of `units.dic`. The three files are byte copies of the
 * `com.chenlb.mmseg4j:mmseg4j-dic:1.8.6` entries (Apache License 2.0; see `search/PROVENANCE.md`).
 *
 * The trie is stored flat (parallel arrays, nodes in breadth-first order) to keep the 150,000 words compact on
 * mobile. After construction nothing is mutated, so one instance is safely shared by every analyzer and thread.
 */
class Dictionary private constructor(
    /** `childStart[n] until childStart[n + 1]` is the slice of [childChars]/[childNodes] holding node n's children. */
    private val childStart: IntArray,
    private val childChars: CharArray,
    private val childNodes: IntArray,
    private val leaf: BooleanArray,
    private val degrees: Map<Char, Int>,
    private val units: Set<Char>,
) {
    /** mmseg4j `CharNode.freq`-derived degree of the single character [c] (`log(freq) * 100`), or -1 when unknown. */
    fun degree(c: Char): Int = degrees[c] ?: -1

    /** Whether [c] is a `units.dic` character (a measure word that follows digits). */
    fun isUnit(c: Char): Boolean = c in units

    /**
     * mmseg4j `Dictionary.maxMatch`: `0` followed by every tail length (word length minus one) of the dictionary
     * words that start at `sentence[offset]` and match the sentence, ascending. Words of length 1 do not exist.
     */
    fun matchingTailLengths(sentence: CharArray, offset: Int): IntArray {
        if (offset >= sentence.size) return ZERO
        var node = child(0, sentence[offset])
        if (node < 0) return ZERO
        var out: IntArray? = null
        var count = 0
        var i = offset + 1
        while (i < sentence.size) {
            node = child(node, sentence[i])
            if (node < 0) break
            if (leaf[node]) {
                if (out == null) out = IntArray(8)
                if (count == out.size) out = out.copyOf(count * 2)
                out[count++] = i - offset
            }
            i++
        }
        if (out == null) return ZERO
        val result = IntArray(count + 1)
        for (k in 0 until count) result[k + 1] = out[k]
        return result
    }

    private fun child(node: Int, c: Char): Int {
        var lo = childStart[node]
        var hi = childStart[node + 1] - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val m = childChars[mid]
            when {
                c < m -> hi = mid - 1
                c > m -> lo = mid + 1
                else -> return childNodes[mid]
            }
        }
        return -1
    }

    companion object {
        private val ZERO = intArrayOf(0)

        /** The bundled dictionary, loaded once on first use (thread-safe). */
        val default: Dictionary by lazy {
            fun text(name: String) = AnalyzerTables.requireResource("search/mmseg4j/$name.dic").decodeToString()
            parse(text("chars"), text("words"), text("units"))
        }

        /** Builds a dictionary from the contents of `chars.dic`, `words.dic` and `units.dic`. */
        fun parse(chars: String, words: String, units: String): Dictionary {
            val degrees = HashMap<Char, Int>()
            for (line in lines(chars)) {
                if (line.isEmpty() || line[0] == '#') continue
                val fields = line.split(' ').filter { it.isNotEmpty() }
                if (fields.size == 2) {
                    val frequency = fields[1].toIntOrNull() ?: continue
                    degrees[fields[0][0]] = (ln(frequency.toDouble()) * 100).toInt()
                }
            }
            val wordList = ArrayList<String>()
            for (line in lines(words)) {
                if (line.startsWith("#") || line.length < 2) continue
                wordList.add(line)
            }
            wordList.sort()
            val unitChars = HashSet<Char>()
            for (line in lines(units)) if (line.isNotEmpty()) unitChars.add(line[0])
            return build(wordList, degrees, unitChars)
        }

        /** Lines split on `\n` with a trailing `\r` removed (the dictionaries are CRLF in part). */
        private fun lines(text: String): List<String> =
            text.split('\n').map { if (it.endsWith('\r')) it.substring(0, it.length - 1) else it }

        private fun build(sorted: List<String>, degrees: Map<Char, Int>, units: Set<Char>): Dictionary {
            val words = ArrayList<String>(sorted.size)
            for (w in sorted) if (words.isEmpty() || words[words.size - 1] != w) words.add(w)
            // Breadth-first: a node is a (lo, hi, depth) range of the sorted words sharing the first `depth` units.
            val rangeLo = ArrayList<Int>()
            val rangeHi = ArrayList<Int>()
            val rangeDepth = ArrayList<Int>()
            rangeLo.add(0); rangeHi.add(words.size); rangeDepth.add(0)
            val starts = ArrayList<Int>()
            val chars = StringBuilder()
            val nodes = ArrayList<Int>()
            val leaf = ArrayList<Boolean>()
            var n = 0
            while (n < rangeLo.size) {
                val lo = rangeLo[n]
                val hi = rangeHi[n]
                val depth = rangeDepth[n]
                starts.add(chars.length)
                var i = lo
                // A word ending exactly here sorts first in the range.
                val isLeaf = i < hi && words[i].length == depth
                if (isLeaf) i++
                leaf.add(isLeaf)
                while (i < hi) {
                    val c = words[i][depth]
                    var j = i + 1
                    while (j < hi && words[j][depth] == c) j++
                    chars.append(c)
                    nodes.add(rangeLo.size)
                    rangeLo.add(i); rangeHi.add(j); rangeDepth.add(depth + 1)
                    i = j
                }
                n++
            }
            starts.add(chars.length)
            return Dictionary(
                starts.toIntArray(), chars.toString().toCharArray(), nodes.toIntArray(),
                BooleanArray(leaf.size) { leaf[it] }, degrees, units,
            )
        }
    }
}
