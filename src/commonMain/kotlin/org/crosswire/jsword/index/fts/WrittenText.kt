package org.crosswire.jsword.index.fts

private const val CLOSING = ",.;:!?)]»”’"
private const val OPENING = "([«“‘"

/**
 * Turns canonical text into the text as a reader sees it, for the stored copy that regex searches run on.
 *
 * `OSISUtil.getCanonicalText` separates adjacent text nodes and elements with a space, so markup such as
 * `<w>light</w>:` comes out as `light : and`. This removes every whitespace run that sits directly before a closing
 * punctuation mark (`, . ; : ! ? ) ] » ” ’`) or directly after an opening one (`( [ « “ ‘`); all other text is
 * unchanged. A text that genuinely has a space before such a mark (French "mot : mot") loses it in the stored copy;
 * that is the accepted price of matching what the other (far more common) modules show.
 *
 * The space `getCanonicalText` puts at an element boundary inside a word is also removed where the word is an
 * apostrophe-s or n't contraction: `day ' s` (KJV Acts 1:12) becomes `day's`, `do n't` becomes
 * `don't`. Ordinary adjacent words are never joined.
 *
 * Only the stored text is normalised. The analyzer input stays the canonical text, so the token stream is unchanged.
 */
internal fun writtenText(canonical: String): String = joinContractions(spacedPunctuationRemoved(canonical))

private val apostropheS = Regex("""(\w)(\s*)(['’])(\s*)s\b""")
private val nApostropheT = Regex("""(\w)\s+(n['’]t)\b""")

private fun joinContractions(text: String): String {
    if ('\'' !in text && '’' !in text) return text
    val s = apostropheS.replace(text) { m ->
        if (m.groupValues[2].isEmpty() && m.groupValues[4].isEmpty()) m.value else m.groupValues[1] + m.groupValues[3] + "s"
    }
    return nApostropheT.replace(s) { m -> m.groupValues[1] + m.groupValues[2] }
}

private fun spacedPunctuationRemoved(canonical: String): String {
    val sb = StringBuilder(canonical.length)
    var i = 0
    while (i < canonical.length) {
        if (!canonical[i].isWhitespace()) { sb.append(canonical[i++]); continue }
        var j = i
        while (j < canonical.length && canonical[j].isWhitespace()) j++
        val afterOpening = sb.isNotEmpty() && sb[sb.length - 1] in OPENING
        val beforeClosing = j < canonical.length && canonical[j] in CLOSING
        if (!afterOpening && !beforeClosing) sb.append(canonical, i, j)
        i = j
    }
    return sb.toString()
}
