package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.slf4j.helpers.MessageFormatter

/** Checks [Slf4jFormat] against slf4j's own `MessageFormatter` on the cases the iOS logger depends on. */
class Slf4jFormatDifferentialTest {
    private val ex = IllegalStateException("boom")
    private val self = arrayOfNulls<Any>(2).also { it[0] = it; it[1] = "x" }
    private val cycleA = arrayOfNulls<Any>(2)
    private val cycleB = arrayOfNulls<Any>(2)
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
        "bool {}" to arrayOf(booleanArrayOf(true, false)),
        "byte {}" to arrayOf(byteArrayOf(1, 2)),
        "char {}" to arrayOf(charArrayOf('a', 'b')),
        "short {}" to arrayOf(shortArrayOf(1, 2)),
        "long {}" to arrayOf(longArrayOf(1L, 2L)),
        "float {}" to arrayOf(floatArrayOf(1.5f, 2f)),
        "double {}" to arrayOf(doubleArrayOf(1.5, 2.0)),
        "trailing {} and {}" to arrayOf("x"),
        "trailing {}" to arrayOf("x", "y"),
        "\\\\{}\\\\{}" to arrayOf("x", "y"),
        "\\\\{}\\\\{} {}" to arrayOf("x"),
        "cycle {}" to arrayOf(cycleA),
        "more {} than {} args" to arrayOf("one"),
        null to arrayOf("x", ex),
        "{}" to arrayOf(object { override fun toString(): String = throw RuntimeException() }),
    )

    init {
        // a -> b -> a: a cycle that is not a direct self-reference
        cycleA[0] = cycleB; cycleA[1] = "a"
        cycleB[0] = cycleA; cycleB[1] = "b"
    }

    @Test
    fun matchesSlf4j() {
        for ((pattern, args) in cases) {
            val expected = MessageFormatter.arrayFormat(pattern, args)
            val actual = Slf4jFormat.format(pattern, args)
            assertEquals("message for $pattern", expected.message, actual.message)
            assertSame("throwable for $pattern", expected.throwable, actual.throwable)
        }
    }

    @Test
    fun formatForLogKeepsMessageAndAttachesLoneThrowable() {
        val r = Slf4jFormat.formatForLog("a {} b", arrayOf(ex))
        assertEquals("a {} b", r.message)
        assertSame(ex, r.throwable)
        assertEquals(null, Slf4jFormat.formatForLog(null, arrayOf(ex)).message)
        // other shapes are plain format()
        val two = Slf4jFormat.formatForLog("a {} b", arrayOf("x", ex))
        assertEquals("a x b", two.message)
        assertSame(ex, two.throwable)
        val one = Slf4jFormat.formatForLog("a {} b", arrayOf("x"))
        assertEquals("a x b", one.message)
        assertEquals(null, one.throwable)
    }
}
