package org.crosswire.jsword.index.analysis

/**
 * One scanner token of [ClassicTokenizer].
 *
 * @property type a Lucene token type name such as `<ALPHANUM>`.
 * @property start inclusive UTF-16 start offset in the input.
 * @property end exclusive UTF-16 end offset (after a removed trailing dot, when there was one).
 * @property positionIncrement 1 plus the number of overlong tokens skipped right before this one.
 */
data class ClassicToken(val text: String, val type: String, val start: Int, val end: Int, val positionIncrement: Int = 1)

/**
 * Lucene 3.6.2 `StandardTokenizer(Version.LUCENE_29)`, which is `ClassicTokenizer` over the JFlex-generated
 * `ClassicTokenizerImpl` scanner. The DFA is the byte-exact table in [AnalyzerTables.classicTokenizerTables].
 *
 * Reproduced on purpose:
 * - A token longer than 255 UTF-16 units (`StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH`) is dropped and the next
 *   token's position increment grows by one, as `ClassicTokenizer.incrementToken` does.
 * - Lucene 2.9 `replaceInvalidAcronym`: an `ACRONYM_DEP` token (which ends in a dot) is reported as `<HOST>` and
 *   its trailing dot removed.
 * - Characters the scanner does not accept as part of a token (spaces, punctuation, combining marks) are skipped.
 */
class ClassicTokenizer {
    /** Splits [text] into scanner tokens in source order. */
    fun tokenize(text: String): List<ClassicToken> {
        val t = scanner
        val out = ArrayList<ClassicToken>()
        var cursor = 0
        var increment = 1
        while (cursor < text.length) {
            val start = cursor
            var current = cursor
            var marked = cursor
            var state = 0
            var accepting = if (t.attributes[state].toInt() and 1 == 1) state else -1
            while (current < text.length) {
                val cls = t.characterMap[text[current].code].toInt() and 0xFF
                current++
                val index = t.rowMap[state] + cls
                check(index < t.transitions.size) { "malformed classic tokenizer table" }
                val next = t.transitions[index].toInt()
                if (next < 0) break
                check(next < t.attributes.size) { "malformed classic tokenizer table" }
                state = next
                val attr = t.attributes[state].toInt()
                if (attr and 1 == 1) {
                    accepting = state
                    marked = current
                    if (attr and 8 == 8) break
                }
            }
            check(accepting >= 0 && marked > start && accepting < t.actions.size) { "malformed classic tokenizer table" }
            cursor = marked
            val action = t.actions[accepting].toInt()
            check(action in 1..10) { "malformed classic tokenizer table" }
            if (action == 1) continue
            if (marked - start > MAX_TOKEN_LENGTH) {
                increment++
                continue
            }
            var end = marked
            var type = TYPES[action - 2]
            if (type == ACRONYM_DEP && text[end - 1] == '.') {
                end--
                type = HOST
            }
            out.add(ClassicToken(text.substring(start, end), type, start, end, increment))
            increment = 1
        }
        return out
    }

    private class Scanner(
        val characterMap: ByteArray,
        val actions: ByteArray,
        val rowMap: IntArray,
        val transitions: ByteArray,
        val attributes: ByteArray,
    )

    private companion object {
        const val MAX_TOKEN_LENGTH = 255
        const val ACRONYM_DEP = "<ACRONYM_DEP>"
        const val HOST = "<HOST>"

        /** Scanner action number minus 2 to token type; action 1 is "ignore". */
        val TYPES = ACTION_TYPES

        val scanner: Scanner by lazy { parse(AnalyzerTables.classicTokenizerTables) }

        fun parse(data: ByteArray): Scanner {
            var pos = 0
            fun u32(): Int {
                check(pos + 4 <= data.size) { "truncated classic tokenizer table" }
                var v = 0
                repeat(4) { v = (v shl 8) or (data[pos++].toInt() and 0xFF) }
                return v
            }
            fun bytes(): ByteArray {
                val n = u32()
                check(n >= 0 && pos + n <= data.size) { "truncated classic tokenizer table" }
                return data.copyOfRange(pos, pos + n).also { pos += n }
            }
            check(data.size > 8 && data.decodeToString(0, 4) == "L29C") { "bad classic tokenizer table magic" }
            pos = 4
            check(u32() == 1) { "bad classic tokenizer table version" }
            val map = bytes()
            val actions = bytes()
            val n = u32()
            check(n >= 0 && pos + 2 * n <= data.size) { "truncated classic tokenizer table" }
            val rows = IntArray(n) { ((data[pos + 2 * it].toInt() and 0xFF) shl 8) or (data[pos + 2 * it + 1].toInt() and 0xFF) }
            pos += 2 * n
            val transitions = bytes()
            val attributes = bytes()
            check(map.size == 65536 && actions.size == 51 && rows.size == 51 && transitions.size == 658 &&
                attributes.size == 51 && pos == data.size) { "malformed classic tokenizer table" }
            return Scanner(map, actions, rows, transitions, attributes)
        }
    }
}

// Generated action numbers 2..10 of Lucene 3.6.2 ClassicTokenizerImpl, found by running the bundled DFA on one
// sample of each class (the numbering follows JFlex's rule discovery, not the order of the Lucene constants).
private val ACTION_TYPES = listOf(
    "<ALPHANUM>", "<CJ>", "<HOST>", "<NUM>", "<APOSTROPHE>", "<COMPANY>", "<ACRONYM_DEP>", "<ACRONYM>", "<EMAIL>",
)
