package org.crosswire.jsword.index.analysis

import fi.sykero.kdom.Element
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.study.StrongsNumber

/**
 * The Strong's number tokens of the `strong` index field, as the baseline `StrongsNumberAnalyzer` produces them
 * (`WhitespaceTokenizer` followed by `StrongsNumberFilter`).
 *
 * The baseline filter drops an invalid token and keeps a valid one as its padded number (`H430` becomes `H0430`).
 * A token with a `!` part (`G3588!a`) yields two tokens: the number (`G3588`) and the full form (`G3588a`).
 */
internal object StrongsTokens {
    /** The tokens of the lemmas in [osis], in document order: [OSISUtil.getStrongsNumbers], then [fromLexical]. */
    fun fromOsis(osis: Element): List<String> = fromLexical(OSISUtil.getStrongsNumbers(osis))

    /** The tokens of a whitespace-separated list of Strong's numbers, as the baseline filter emits them. */
    fun fromLexical(text: String): List<String> {
        val out = ArrayList<String>()
        for (raw in text.split(WHITESPACE).filter { it.isNotEmpty() }) {
            val number = StrongsNumber(raw)
            if (!number.isValid()) continue
            out += number.getStrongsNumber()
            if (number.isPart) out += number.fullStrongsNumber
        }
        return out
    }

    /**
     * The padded number for one raw token, without any `!` part, or null when the token is not a valid Strong's number.
     * Case-insensitive: `h7225` is `H7225`.
     */
    fun normalize(raw: String): String? {
        val number = StrongsNumber(raw)
        return if (number.isValid()) number.getStrongsNumber() else null
    }

    private val WHITESPACE = Regex("\\s+")
}
