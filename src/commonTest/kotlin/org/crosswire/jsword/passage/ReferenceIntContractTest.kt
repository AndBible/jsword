package org.crosswire.jsword.passage

import org.crosswire.common.util.parseReferenceInt
import org.crosswire.common.util.parseReferenceIntPortable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReferenceIntContractTest {
    @Test fun literalJdkBoundsAndSyntaxOnBothImplementations() {
        for (parse in listOf(::parseReferenceInt, ::parseReferenceIntPortable)) {
            for ((input, expected) in listOf("2147483647" to Int.MAX_VALUE, "-2147483648" to Int.MIN_VALUE,
                "+2147483647" to Int.MAX_VALUE, "-0001" to -1, "١۲" to 12, "１" to 1)) {
                assertEquals(expected, parse(input))
            }
            for (input in listOf("", "+", "-", "2147483648", "-2147483649", " 1", "1 ", "1_0", "𝟙")) {
                assertEquals("For input string: \"$input\"", assertFailsWith<NumberFormatException> { parse(input) }.message)
            }
        }
    }
}
