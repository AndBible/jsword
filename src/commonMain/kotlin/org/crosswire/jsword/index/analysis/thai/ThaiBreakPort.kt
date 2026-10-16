// Origin and licence: this file is a port of OpenJDK 17's sun.text.RuleBasedBreakIterator,
// sun.text.DictionaryBasedBreakIterator and sun.text.BreakDictionary. OpenJDK is licensed under the GNU General Public
// License, version 2, with the Classpath Exception. See NOTICE-third-party.md.
package org.crosswire.jsword.index.analysis.thai

/**
 * Pure-Kotlin port of the forward word-break path of OpenJDK 17's `RuleBasedBreakIterator`,
 * `DictionaryBasedBreakIterator` and `BreakDictionary` for `BreakIterator.getWordInstance(Locale("th"))`.
 *
 * It is a port of the Swift `Lucene29ThaiTokenizer` of and-bible-ios, which in turn reads the OpenJDK binary data files
 * `WordBreakIteratorData_th` (rules) and `thai_dict` (dictionary). Those files are GPLv2 with the Classpath Exception
 * and must not ship in the JVM or Android artifact, so this class takes them as byte arrays; only the iOS source set
 * embeds them (`IosKswordResources`), and the JVM test feeds it the same files from the test classpath.
 *
 * Instances are immutable; every traversal keeps its state in local variables, so one instance may be shared by threads.
 */
internal class ThaiBreakPort(ruleData: ByteArray, dictionaryData: ByteArray) {
    private val rules = Rules(ruleData)
    private val dictionary = Dictionary(dictionaryData)

    /** UTF-16 word boundaries of [source], including 0 and `source.length`. An empty source yields `[0]`. */
    fun boundaries(source: CharSequence): IntArray {
        if (source.isEmpty()) return intArrayOf(0)
        val out = ArrayList<Int>()
        out.add(0)
        var start = 0
        while (start < source.length) {
            val tentative = rules.nextBoundary(source, start)
            val next: List<Int> =
                if (tentative.dictionaryChars > 1 && tentative.end - start > 1) divideDictionaryRange(source, start, tentative.end)
                else listOf(start, tentative.end)
            for (i in 1 until next.size) if (next[i] > out.last()) out.add(next[i])
            check(tentative.end > start) { "openjdk17 Thai break progress" }
            start = tentative.end
        }
        return out.toIntArray()
    }

    /** `DictionaryBasedBreakIterator.divideUpDictionaryRange`, forward only. Returns boundaries including [start] and [end]. */
    private fun divideDictionaryRange(source: CharSequence, start: Int, end: Int): List<Int> {
        val cursor = Cursor(source, start)
        var character = codePointAt(source, cursor.index) ?: DONE
        var category = rules.category(character)
        while (category == Rules.IGNORE || !rules.isDictionaryCategory(category)) {
            character = cursor.advance()
            if (character == DONE) return listOf(start, end)
            category = rules.category(character)
        }

        var current = ArrayList<Int>()
        val possible = ArrayList<Int>()
        val wrong = HashSet<Int>()
        var state = 0
        var farthest = cursor.index
        var best: List<Int>? = null

        while (true) {
            if (dictionary.nextState(state, 0) == -1) possible.add(cursor.index)
            state = dictionary.nextStateForChar(state, character)
            if (state == -1) {
                current.add(cursor.index)
                break
            } else if (state == 0 || cursor.index >= end) {
                if (cursor.index > farthest) {
                    farthest = cursor.index
                    best = ArrayList(current)
                }
                while (possible.isNotEmpty() && possible.last() in wrong) possible.removeAt(possible.size - 1)
                if (possible.isEmpty()) {
                    val b = best
                    if (b != null) {
                        current = ArrayList(b)
                        if (farthest < end) cursor.index = minOf(farthest + 1, source.length) else break
                    } else {
                        if (current.lastOrNull() != cursor.index && cursor.index != start) current.add(cursor.index)
                        cursor.advance()
                        current.add(cursor.index)
                    }
                } else {
                    val p = possible.removeAt(possible.size - 1)
                    while (current.isNotEmpty() && p < current.last()) wrong.add(current.removeAt(current.size - 1))
                    current.add(p)
                    cursor.index = p
                }
                character = codePointAt(source, cursor.index) ?: DONE
                if (cursor.index >= end) break
                state = 0
            } else {
                character = cursor.advance()
            }
        }

        if (current.isNotEmpty()) current.removeAt(current.size - 1)
        current.add(end)
        return listOf(start) + current
    }

    /** OpenJDK's UTF-16 `CharacterIterator` traversal: stays on the last code point at the end and reports [DONE]. */
    private class Cursor(val source: CharSequence, var index: Int) {
        fun advance(): Int {
            val width = widthAt(source, index) ?: return DONE
            val next = index + width
            if (next >= source.length) return DONE
            index = next
            return codePointAt(source, index) ?: DONE
        }
    }

    private class Boundary(val end: Int, val dictionaryChars: Int)

    /** Parsed `WordBreakIteratorData_th` forward state machine. */
    private class Rules(data: ByteArray) {
        private val stateTable: IntArray
        private val endStates: BooleanArray
        private val lookaheadStates: BooleanArray
        private val categories: CompactByteTable
        private val supplementary: SupplementaryTable
        private val dictionaryFlags: BooleanArray
        private val categoryCount: Int

        init {
            val r = Reader(data, "WordBreakIteratorData_th")
            check(r.bytes(7).contentEquals("BIdata\u0000".encodeToByteArray()) && r.u8() == 1) { "bad WordBreakIteratorData_th" }
            val payload = r.u32()
            check(r.offset + payload == data.size) { "bad WordBreakIteratorData_th length" }
            val stateLength = r.u32()
            val backwardsLength = r.u32()
            val endLength = r.u32()
            val lookaheadLength = r.u32()
            val bmpLength = r.u32()
            val supplementaryLength = r.u32()
            val additionalLength = r.u32()
            r.bytes(8) // OpenJDK checksum; the resource hashes are pinned in the provenance notes.
            stateTable = r.i16s(stateLength)
            r.i16s(backwardsLength)
            endStates = r.bytes(endLength).let { b -> BooleanArray(b.size) { b[it].toInt() == 1 } }
            lookaheadStates = r.bytes(lookaheadLength).let { b -> BooleanArray(b.size) { b[it].toInt() == 1 } }
            val indices = r.u16s(512)
            val values = r.bytes(bmpLength)
            categories = CompactByteTable(indices, values)
            supplementary = SupplementaryTable(r.i32s(supplementaryLength))
            dictionaryFlags = r.bytes(additionalLength).let { b -> BooleanArray(b.size) { b[it].toInt() == 1 } }
            check(r.atEnd && endStates.isNotEmpty() && stateTable.size % endStates.size == 0) { "bad WordBreakIteratorData_th tables" }
            categoryCount = stateTable.size / endStates.size
        }

        /** One run of the forward rule state machine from [start]; counts the dictionary-category characters seen. */
        fun nextBoundary(source: CharSequence, start: Int): Boundary {
            if (start >= source.length) return Boundary(source.length, 0)
            var cursor = start
            var result = minOf(start + (widthAt(source, start) ?: 1), source.length)
            var lookaheadResult = 0
            var state = 1
            var dictionaryChars = 0
            while (cursor < source.length && state != 0) {
                val character = codePointAt(source, cursor) ?: break
                val width = widthAt(source, cursor) ?: break
                val category = category(character)
                val nextIndex = minOf(cursor + width, source.length)
                if (category != IGNORE) {
                    if (isDictionaryCategory(category)) dictionaryChars++
                    val tableIndex = state * categoryCount + category
                    check(tableIndex in stateTable.indices) { "openjdk17 Thai rule state" }
                    state = stateTable[tableIndex]
                }
                check(state in endStates.indices && state in lookaheadStates.indices) { "openjdk17 Thai rule transition" }
                if (lookaheadStates[state]) {
                    if (endStates[state]) result = lookaheadResult else lookaheadResult = nextIndex
                } else if (endStates[state]) {
                    result = nextIndex
                }
                cursor = nextIndex
            }
            if (cursor == source.length && lookaheadResult == source.length) result = lookaheadResult
            check(result > start && result <= source.length) { "openjdk17 Thai rule boundary" }
            return Boundary(result, dictionaryChars)
        }

        fun category(character: Int): Int = if (character < 0x10000) categories.value(character) else supplementary.value(character)

        fun isDictionaryCategory(category: Int): Boolean = category in dictionaryFlags.indices && dictionaryFlags[category]

        companion object {
            const val IGNORE = -1
        }
    }

    /** Parsed `thai_dict` compressed trie/state table. */
    private class Dictionary(data: ByteArray) {
        private val columnMap: CompactByteTable
        private val supplementaryColumnMap: SupplementaryTable
        private val columnCount: Int
        private val rowIndex: IntArray
        private val rowIndexFlagsIndex: IntArray
        private val rowIndexFlags: IntArray
        private val rowIndexShifts: IntArray
        private val table: IntArray

        init {
            val r = Reader(data, "thai_dict")
            check(r.u32() == 1) { "bad thai_dict version" }
            val payload = r.u32()
            check(r.offset + payload == data.size) { "bad thai_dict length" }
            val indexCount = r.u32()
            val indices = r.u16s(indexCount)
            val valueCount = r.u32()
            columnMap = CompactByteTable(indices, r.bytes(valueCount))
            columnCount = r.u32()
            r.u32() // number of 32-column flag groups; the indexes encode its use
            rowIndex = r.i16s(r.u32())
            rowIndexFlagsIndex = r.i16s(r.u32())
            val flagCount = r.u32()
            rowIndexFlags = IntArray(flagCount) { r.u32Raw() }
            val shiftCount = r.u32()
            rowIndexShifts = r.bytes(shiftCount).let { b -> IntArray(b.size) { b[it].toInt() } }
            table = r.i16s(r.u32())
            supplementaryColumnMap = SupplementaryTable(r.i32s(r.u32()))
            check(r.atEnd) { "bad thai_dict tables" }
        }

        fun nextStateForChar(row: Int, character: Int): Int {
            val column = if (character < 0x10000) columnMap.value(character) else supplementaryColumnMap.value(character)
            return nextState(row, column)
        }

        fun nextState(row: Int, column: Int): Int {
            check(row in rowIndexFlagsIndex.indices && row in rowIndex.indices && row in rowIndexShifts.indices && column >= 0) {
                "openjdk17 Thai dictionary state"
            }
            val flagIndex = rowIndexFlagsIndex[row]
            val populated: Boolean
            if (flagIndex < 0) {
                populated = column == -flagIndex
            } else {
                val index = flagIndex + (column shr 5)
                check(index in rowIndexFlags.indices) { "openjdk17 Thai dictionary flags" }
                populated = rowIndexFlags[index] and (1 shl (column and 0x1F)) != 0
            }
            if (!populated) return 0
            val tableIndex = rowIndex[row] * columnCount + column + rowIndexShifts[row]
            check(tableIndex in table.indices) { "openjdk17 Thai dictionary table" }
            return table[tableIndex]
        }
    }

    /** OpenJDK's 512-block compact byte lookup. */
    private class CompactByteTable(private val indices: IntArray, private val values: ByteArray) {
        init { check(indices.size == 512) { "openjdk17 Thai compact table" } }

        fun value(character: Int): Int {
            check(character in 0..0xFFFF) { "openjdk17 Thai compact table range" }
            val offset = indices[character shr 7] + (character and 0x7F)
            check(offset in values.indices) { "openjdk17 Thai compact table offset" }
            return values[offset].toInt()
        }
    }

    /** OpenJDK's range-compressed supplementary-code-point lookup. */
    private class SupplementaryTable(private val values: IntArray) {
        fun value(character: Int): Int {
            check(values.size >= 2) { "openjdk17 Thai supplementary table" }
            var lower = 0
            var upper = values.size - 1
            while (lower < upper - 1) {
                val middle = (lower + upper) / 2
                if (character < (values[middle] shr 8)) upper = middle else lower = middle
            }
            val start = values[lower] shr 8
            val end = values[lower + 1] shr 8
            check(character >= start && character < end) { "openjdk17 Thai supplementary value" }
            val v = values[lower] and 0xFF
            return if (v == 0xFF) Rules.IGNORE else v
        }
    }

    /** Bounds-checked big-endian reader. */
    private class Reader(private val bytes: ByteArray, private val name: String) {
        var offset = 0
        val atEnd: Boolean get() = offset == bytes.size

        fun bytes(count: Int): ByteArray {
            check(count >= 0 && offset + count <= bytes.size) { "truncated $name" }
            return bytes.copyOfRange(offset, offset + count).also { offset += count }
        }

        fun u8(): Int = bytes(1)[0].toInt() and 0xFF
        fun u16(): Int = bytes(2).let { ((it[0].toInt() and 0xFF) shl 8) or (it[1].toInt() and 0xFF) }
        fun u32Raw(): Int = bytes(4).fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }

        /** A length or count field; a value above Int.MAX_VALUE is malformed data. */
        fun u32(): Int = u32Raw().also { check(it >= 0) { "bad length in $name" } }
        fun u16s(count: Int): IntArray = IntArray(count) { u16() }
        fun i16s(count: Int): IntArray = IntArray(count) { u16().toShort().toInt() }
        fun i32s(count: Int): IntArray = IntArray(count) { u32Raw() }
    }

    private companion object {
        const val DONE = 0xFFFF

        fun widthAt(source: CharSequence, index: Int): Int? {
            if (index !in 0 until source.length) return null
            val first = source[index]
            if (first.code in 0xD800..0xDBFF && index + 1 < source.length && source[index + 1].code in 0xDC00..0xDFFF) return 2
            return 1
        }

        fun codePointAt(source: CharSequence, index: Int): Int? {
            val width = widthAt(source, index) ?: return null
            val first = source[index].code
            return if (width == 2) 0x10000 + ((first - 0xD800) shl 10) + (source[index + 1].code - 0xDC00) else first
        }
    }
}
