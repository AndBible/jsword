package org.crosswire.common.util

/** The former Integer.parseInt contract, including its observable raw error messages. */
internal expect fun parseReferenceInt(input: String): Int

// Decimal zero code units captured from JDK 17 Character.digit(char, 10).
// An exhaustive JVM differential test checks every BMP char, including surrogate rejection.
private val decimalZeros = intArrayOf(
    0x0030, 0x0660, 0x06f0, 0x07c0, 0x0966, 0x09e6, 0x0a66, 0x0ae6,
    0x0b66, 0x0be6, 0x0c66, 0x0ce6, 0x0d66, 0x0de6, 0x0e50, 0x0ed0,
    0x0f20, 0x1040, 0x1090, 0x17e0, 0x1810, 0x1946, 0x19d0, 0x1a80,
    0x1a90, 0x1b50, 0x1bb0, 0x1c40, 0x1c50, 0xa620, 0xa8d0, 0xa900,
    0xa9d0, 0xa9f0, 0xaa50, 0xabf0, 0xff10
)

internal fun referenceDecimalDigit(char: Char): Int {
    for (zero in decimalZeros) {
        val digit = char.code - zero
        if (digit in 0..9) return digit
    }
    return -1
}

/** Negative accumulation admits MIN_VALUE without overflowing its positive magnitude. */
internal fun parseReferenceIntPortable(input: String): Int {
    fun invalid(): Nothing = throw NumberFormatException("For input string: \"$input\"")
    if (input.isEmpty()) invalid()
    val negative = input[0] == '-'
    val start = if (negative || input[0] == '+') 1 else 0
    if (start == input.length) invalid()
    val limit = if (negative) Int.MIN_VALUE else -Int.MAX_VALUE
    val multiplyLimit = limit / 10
    var result = 0
    for (index in start until input.length) {
        val digit = referenceDecimalDigit(input[index])
        if (digit < 0 || result < multiplyLimit) invalid()
        result *= 10
        if (result < limit + digit) invalid()
        result -= digit
    }
    return if (negative) result else -result
}
