package org.crosswire.common.util

/**
 * `java.lang.Character.isWhitespace` semantics. Kotlin's [Char.isWhitespace] also accepts the
 * no-break spaces U+00A0, U+2007 and U+202F; JSword's parsers must not.
 */
fun Char.isJavaWhitespace(): Boolean = when (this) {
    '\u00A0', '\u2007', '\u202F' -> false
    '\t', '\n', '\u000B', '\u000C', '\r', '\u001C', '\u001D', '\u001E', '\u001F' -> true
    else -> when (category) {
        CharCategory.SPACE_SEPARATOR, CharCategory.LINE_SEPARATOR, CharCategory.PARAGRAPH_SEPARATOR -> true
        else -> false
    }
}
