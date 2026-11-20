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
 * Only the stored text is normalised. The analyzer input stays the canonical text, so the token stream is unchanged.
 */
internal fun writtenText(canonical: String): String {
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
