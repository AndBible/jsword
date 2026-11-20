package org.crosswire.jsword.index.fts

import fi.sykero.kdom.Element
import org.crosswire.jsword.book.OSISUtil

private const val CLOSING = ",.;:!?)]»”’'"
private const val OPENING = "([«“‘"
private const val APOSTROPHES = "'’"

/**
 * The text of an OSIS fragment as a reader sees it, for the stored copy that regex searches run on.
 *
 * It is [OSISUtil.getCanonicalText] with one difference. That function inserts a space where two text nodes meet
 * without whitespace, so markup such as `<w>light</w>: and` comes out as `light : and`, and `<w>day</w>'<w>s</w>` as
 * `day ' s`. Here that inserted space is left out where a reader would see none: before a closing mark (`, . ; : ! ? )
 * ] » ” ’ '`), after an opening one (`( [ « “ ‘`) and after an apostrophe. Whitespace that is part of the text itself is
 * never touched, so a French "mot : mot", a Dutch "huis 's Heeren" and an Afrikaans "en ’n man" stay as written, and
 * two words that meet at an element boundary are still separated.
 *
 * Only the stored text is normalised. The analyzer input stays the canonical text, so the token stream is unchanged.
 */
internal fun writtenText(osis: Element): String = OSISUtil.getCanonicalText(osis, ::separatesAtNodeBoundary)

/** Whether the space [OSISUtil.getCanonicalText] would insert between [previous] and the start of [next] is kept. */
internal fun separatesAtNodeBoundary(previous: Char, next: String): Boolean =
    next[0] !in CLOSING && previous !in OPENING && previous !in APOSTROPHES && !startsWithNt(next)

/** English `n't` starting a node (`do` + `n't`): the node is the tail of the word before it. */
private fun startsWithNt(next: String): Boolean = next.length >= 3 && next[0] == 'n' && next[1] in APOSTROPHES && next[2] == 't'
