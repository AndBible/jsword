package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JavaWhitespaceDifferentialTest {
    @Test
    fun everyCharMatchesCharacterIsWhitespace() {
        for (code in 0..0xFFFF) {
            val c = code.toChar()
            assertEquals(Character.isWhitespace(c), c.isJavaWhitespace(), "U+%04X".format(code))
        }
    }

    @Test
    fun noBreakSpacesAreNotWhitespace() {
        // Kotlin accepts these as whitespace; Java does not. Passage parsing needs Java's answer.
        for (c in listOf('\u00A0', '\u2007', '\u202F')) assertFalse(c.isJavaWhitespace())
    }
}
