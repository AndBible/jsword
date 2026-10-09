package org.crosswire.common.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.helpers.MarkerIgnoringBase

/**
 * Checks that [Slf4jLogger] picks the same slf4j overload a direct Kotlin call would have picked:
 * 0 args -> (String); one Throwable -> (String, Throwable); one arg -> (String, Object);
 * two args -> (String, Object, Object); more -> varargs.
 */
class Slf4jLoggerDispatchTest {
    private val ex = IllegalStateException("boom")
    private val recording = Recording()
    private val logger = Slf4jLogger(recording)

    @Test
    fun throwableOnlyUsesThrowableOverload() {
        logger.error("failed", ex)
        assertEquals(listOf("error:(String,Throwable):failed:[boom]"), recording.events)
    }

    @Test
    fun messageOnly() {
        logger.warn("w")
        assertEquals(listOf("warn:(String):w:[]"), recording.events)
    }

    @Test
    fun oneArg() {
        logger.debug("a {}", 1)
        assertEquals(listOf("debug:(String,Object):a {}:[1]"), recording.events)
    }

    @Test
    fun twoArgs() {
        logger.info("a {} {}", 1, 2)
        assertEquals(listOf("info:(String,Object,Object):a {} {}:[1, 2]"), recording.events)
    }

    @Test
    fun threeArgs() {
        logger.trace("{} {} {}", 1, 2, 3)
        assertEquals(listOf("trace:(String,Object...):{} {} {}:[1, 2, 3]"), recording.events)
    }

    @Test
    fun placeholderThenThrowable() {
        logger.warn("x {}", "a", ex)
        assertEquals(listOf("warn:(String,Object,Object):x {}:[a, boom]"), recording.events)
    }

    @Test
    fun nullMessageWithThrowable() {
        logger.warn(null, ex)
        assertEquals(listOf("warn:(String,Throwable):null:[boom]"), recording.events)
    }

    @Test
    fun singleNullArgIsObjectOverload() {
        logger.info("a {}", null)
        assertEquals(listOf("info:(String,Object):a {}:[null]"), recording.events)
    }

    @Test
    fun singleArrayArgStaysObjectOverload() {
        val arr = arrayOf("p", "q")
        logger.debug("a {}", arr)
        // not spread as varargs: one (String,Object) call, the array shown by its own toString
        assertEquals(listOf("debug:(String,Object):a {}:[$arr]"), recording.events)
    }

    @Test
    fun throwableOverloadForEveryLevel() {
        logger.trace("t", ex)
        logger.debug("d", ex)
        logger.info("i", ex)
        logger.warn("w", ex)
        assertEquals(
            listOf(
                "trace:(String,Throwable):t:[boom]",
                "debug:(String,Throwable):d:[boom]",
                "info:(String,Throwable):i:[boom]",
                "warn:(String,Throwable):w:[boom]",
            ),
            recording.events,
        )
    }

    /** Records which slf4j overload was called; Throwables are shown by message so the expectations stay short. */
    private class Recording : MarkerIgnoringBase() {
        val events = mutableListOf<String>()

        private fun show(x: Any?): String = if (x is Throwable) x.message ?: "" else x.toString()

        private fun rec(level: String, sig: String, msg: String?, args: List<Any?>) {
            events += "$level:$sig:$msg:[${args.joinToString(", ") { show(it) }}]"
        }

        override fun getName(): String = "recording"
        override fun isTraceEnabled(): Boolean = true
        override fun isDebugEnabled(): Boolean = true
        override fun isInfoEnabled(): Boolean = true
        override fun isWarnEnabled(): Boolean = true
        override fun isErrorEnabled(): Boolean = true

        override fun trace(msg: String?) = rec("trace", "(String)", msg, emptyList())
        override fun trace(msg: String?, arg: Any?) = rec("trace", "(String,Object)", msg, listOf(arg))
        override fun trace(msg: String?, t: Throwable?) = rec("trace", "(String,Throwable)", msg, listOf(t))
        override fun trace(msg: String?, a1: Any?, a2: Any?) = rec("trace", "(String,Object,Object)", msg, listOf(a1, a2))
        override fun trace(msg: String?, vararg args: Any?) = rec("trace", "(String,Object...)", msg, args.toList())

        override fun debug(msg: String?) = rec("debug", "(String)", msg, emptyList())
        override fun debug(msg: String?, arg: Any?) = rec("debug", "(String,Object)", msg, listOf(arg))
        override fun debug(msg: String?, t: Throwable?) = rec("debug", "(String,Throwable)", msg, listOf(t))
        override fun debug(msg: String?, a1: Any?, a2: Any?) = rec("debug", "(String,Object,Object)", msg, listOf(a1, a2))
        override fun debug(msg: String?, vararg args: Any?) = rec("debug", "(String,Object...)", msg, args.toList())

        override fun info(msg: String?) = rec("info", "(String)", msg, emptyList())
        override fun info(msg: String?, arg: Any?) = rec("info", "(String,Object)", msg, listOf(arg))
        override fun info(msg: String?, t: Throwable?) = rec("info", "(String,Throwable)", msg, listOf(t))
        override fun info(msg: String?, a1: Any?, a2: Any?) = rec("info", "(String,Object,Object)", msg, listOf(a1, a2))
        override fun info(msg: String?, vararg args: Any?) = rec("info", "(String,Object...)", msg, args.toList())

        override fun warn(msg: String?) = rec("warn", "(String)", msg, emptyList())
        override fun warn(msg: String?, arg: Any?) = rec("warn", "(String,Object)", msg, listOf(arg))
        override fun warn(msg: String?, t: Throwable?) = rec("warn", "(String,Throwable)", msg, listOf(t))
        override fun warn(msg: String?, a1: Any?, a2: Any?) = rec("warn", "(String,Object,Object)", msg, listOf(a1, a2))
        override fun warn(msg: String?, vararg args: Any?) = rec("warn", "(String,Object...)", msg, args.toList())

        override fun error(msg: String?) = rec("error", "(String)", msg, emptyList())
        override fun error(msg: String?, arg: Any?) = rec("error", "(String,Object)", msg, listOf(arg))
        override fun error(msg: String?, t: Throwable?) = rec("error", "(String,Throwable)", msg, listOf(t))
        override fun error(msg: String?, a1: Any?, a2: Any?) = rec("error", "(String,Object,Object)", msg, listOf(a1, a2))
        override fun error(msg: String?, vararg args: Any?) = rec("error", "(String,Object...)", msg, args.toList())
    }
}
