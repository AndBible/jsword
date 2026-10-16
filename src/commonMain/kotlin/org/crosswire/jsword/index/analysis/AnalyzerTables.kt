package org.crosswire.jsword.index.analysis

import org.crosswire.common.util.KswordResources

/**
 * The Lucene 3.6.2 character tables the analyzers need, read from the byte-exact resources in `search/`
 * (see `search/PROVENANCE.md` for their generation and SHA-256 values).
 *
 * Lucene's classes are not used at runtime; these tables reproduce the Java `Character` behaviour the
 * oracle (Lucene 3.6.2 on Temurin 17) observed, so results do not depend on the platform's Unicode version.
 * A missing or malformed resource throws [IllegalStateException]: there is deliberately no fallback table.
 */
object AnalyzerTables {
    private class Tables(
        val letterLow: CharArray,
        val letterHigh: CharArray,
        val lowerCase: Map<Char, Char>,
        val asciiFold: Map<Char, String>,
        val categoryLow: CharArray,
        val categoryHigh: CharArray,
        val categoryMask: IntArray,
    )

    private val tables: Tables by lazy {
        val letters = ranges("lucene-letter-ranges")
        val categories = ranges("lucene-character-categories", withMask = true)
        Tables(
            letterLow = letters.low,
            letterHigh = letters.high,
            lowerCase = lowerCaseRows(),
            asciiFold = foldingRows(),
            categoryLow = categories.low,
            categoryHigh = categories.high,
            categoryMask = categories.mask,
        )
    }

    /** The Lucene 3.6.2 `ClassicTokenizerImpl` DFA tables (`ZZ_CMAP`, `ZZ_ACTION`, ...), consumed by the classic tokenizer port. */
    val classicTokenizerTables: ByteArray by lazy { requireResource("search/lucene-classic-tokenizer-2.9.bin") }

    /**
     * Java `Character.isLetter(char)` as pinned in `lucene-letter-ranges.tsv`.
     * Surrogate code units are never letters, matching Lucene's `CharTokenizer`, which works on UTF-16 units.
     */
    fun isLetter(c: Char): Boolean = indexIn(tables.letterLow, tables.letterHigh, c) >= 0

    /** Java `Character.toLowerCase(char)` as pinned in `lucene-lowercase.tsv`; unmapped units are returned unchanged. */
    fun lowerCase(c: Char): Char = tables.lowerCase[c] ?: c

    /**
     * Lucene 3.6.2 `ASCIIFoldingFilter` applied to every UTF-16 unit of [input]. One unit may fold to several
     * characters (for example `Æ` to `AE`). Units without a mapping, including lone surrogates, are copied as-is.
     */
    fun foldAscii(input: String): String {
        val out = StringBuilder(input.length)
        for (c in input) {
            val folded = tables.asciiFold[c]
            if (folded != null) out.append(folded) else out.append(c)
        }
        return out.toString()
    }

    /**
     * The Java `Character.getType(char)` category bitmask recorded in `lucene-character-categories.tsv`, or 0 when
     * the unit is not listed. The bits are: 0x01 nonspacing mark, 0x02 decimal digit, 0x04 letter number,
     * 0x08 other number, 0x10 other letter, 0x20 cased or modifier letter.
     */
    fun characterType(c: Char): Int {
        val i = indexIn(tables.categoryLow, tables.categoryHigh, c)
        return if (i >= 0) tables.categoryMask[i] else 0
    }

    /** Loads the embedded resource at [path]; a missing resource is an [IllegalStateException]. */
    internal fun requireResource(path: String): ByteArray =
        checkNotNull(KswordResources.bytes(path)) { "analyzer resource $path is not embedded" }

    /** The rows of `search/<name>.tsv`, each split on tabs; the file's trailing newline yields no row. */
    private fun rows(name: String): List<List<String>> =
        requireResource("search/$name.tsv").decodeToString().split('\n').filter { it.isNotEmpty() }
            .map { it.split('\t') }

    private fun hex(field: String): Int = field.toIntOrNull(16)
        ?: throw IllegalStateException("malformed hex field '$field' in analyzer resources")

    /** `lo<TAB>hi` rows (plus a category mask when [withMask]), sorted by `lo` as generated. */
    private class Ranges(val low: CharArray, val high: CharArray, val mask: IntArray)

    private fun ranges(name: String, withMask: Boolean = false): Ranges {
        val rows = rows(name)
        val width = if (withMask) 3 else 2
        val low = CharArray(rows.size)
        val high = CharArray(rows.size)
        val mask = IntArray(rows.size)
        rows.forEachIndexed { i, fields ->
            check(fields.size == width) { "malformed row in search/$name.tsv" }
            low[i] = hex(fields[0]).toChar()
            high[i] = hex(fields[1]).toChar()
            if (withMask) mask[i] = hex(fields[2])
        }
        return Ranges(low, high, mask)
    }

    /** `source<TAB>target` rows of `lucene-lowercase.tsv`: Java `Character.toLowerCase` for each mapped unit. */
    private fun lowerCaseRows(): Map<Char, Char> = rows("lucene-lowercase").associate { fields ->
        check(fields.size == 2) { "malformed row in search/lucene-lowercase.tsv" }
        hex(fields[0]).toChar() to hex(fields[1]).toChar()
    }

    /**
     * `unit<TAB>replacement` rows of `lucene-ascii-folding.tsv`. A replacement may itself contain tabs, so only
     * the first tab separates the fields; the split is done on the raw line, not on [rows].
     */
    private fun foldingRows(): Map<Char, String> =
        requireResource("search/lucene-ascii-folding.tsv").decodeToString().split('\n').filter { it.isNotEmpty() }
            .associate { line ->
                val tab = line.indexOf('\t')
                check(tab > 0) { "malformed row in search/lucene-ascii-folding.tsv" }
                hex(line.substring(0, tab)).toChar() to line.substring(tab + 1)
            }

    /** Binary search for the row containing [c]; returns its index or -1. */
    private fun indexIn(low: CharArray, high: CharArray, c: Char): Int {
        var lo = 0
        var hi = low.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            when {
                c < low[mid] -> hi = mid - 1
                c > high[mid] -> lo = mid + 1
                else -> return mid
            }
        }
        return -1
    }
}
