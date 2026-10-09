package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.slf4j.helpers.MessageFormatter

/** Checks [Slf4jFormat] against slf4j's own `MessageFormatter` on the cases the iOS logger depends on. */
class Slf4jFormatDifferentialTest {
    private val ex = IllegalStateException("boom")
    private val self = arrayOfNulls<Any>(2).also { it[0] = it; it[1] = "x" }
    private val cases: List<Pair<String?, Array<Any?>>> = listOf(
        "plain" to arrayOf(),
        "a {} b" to arrayOf("x"),
        "a {} b {}" to arrayOf("x", 2),
        "a {} b" to arrayOf("x", ex),
        "a {}" to arrayOf(ex),
        "no placeholder" to arrayOf(ex),
        "no placeholder" to arrayOf("x", ex),
        "esc \\{} {}" to arrayOf("x"),
        "double \\\\{}" to arrayOf("x"),
        "{}{}{}" to arrayOf(1, null, 3),
        "arr {}" to arrayOf(arrayOf("a", arrayOf(1, 2))),
        "prim {}" to arrayOf(intArrayOf(1, 2)),
        "self {}" to arrayOf(self),
        "more {} than {} args" to arrayOf("one"),
        null to arrayOf("x", ex),
        "{}" to arrayOf(object { override fun toString(): String = throw RuntimeException() }),
    )

    @Test
    fun matchesSlf4j() {
        for ((pattern, args) in cases) {
            val expected = MessageFormatter.arrayFormat(pattern, args)
            val actual = Slf4jFormat.format(pattern, args)
            assertEquals("message for $pattern", expected.message, actual.message)
            assertSame("throwable for $pattern", expected.throwable, actual.throwable)
        }
    }
}
