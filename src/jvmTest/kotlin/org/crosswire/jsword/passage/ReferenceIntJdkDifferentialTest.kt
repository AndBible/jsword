package org.crosswire.jsword.passage

import org.crosswire.common.util.parseReferenceIntPortable
import org.crosswire.common.util.referenceDecimalDigit
import kotlin.test.Test
import kotlin.test.assertEquals

class ReferenceIntJdkDifferentialTest {
    @Test fun everyBmpCharMatchesJdk17DecimalDigit() {
        for (code in 0..65535) {
            assertEquals(Character.digit(code.toChar(), 10), referenceDecimalDigit(code.toChar()), "U+${code.toString(16)}")
        }
    }

    @Test fun portableAlgorithmMatchesJdkSyntaxBoundsAndMessages() {
        fun outcome(parse: () -> Int): Pair<Int?, String?> = try {
            parse() to null
        } catch (e: NumberFormatException) {
            null to e.message
        }
        val inputs = listOf("", "+", "-", "0", "-0", "+0", "0001", "2147483647", "2147483648",
            "-2147483648", "-2147483649", "+2147483647", "+2147483648", "99999999999999999999",
            " 1", "1 ", "1\n", " 1", "1_0", "1.0", "0x10", "++1", "--1", "+-1", "−1",
            "١۲", "１", "𝟙", "\ud835", "\udfd9", "\u0000", "\"", "\n")
        for (input in inputs) {
            assertEquals(outcome { Integer.parseInt(input) }, outcome { parseReferenceIntPortable(input) }, input)
        }
        for (value in listOf(Int.MIN_VALUE, Int.MIN_VALUE + 1, -100, -1, 0, 1, 100, Int.MAX_VALUE - 1, Int.MAX_VALUE)) {
            for (zeros in 0..12) {
                val magnitude = value.toString().removePrefix("-")
                val input = (if (value < 0) "-" else "+") + "0".repeat(zeros) + magnitude
                assertEquals(value, parseReferenceIntPortable(input), input)
            }
        }
    }
}
