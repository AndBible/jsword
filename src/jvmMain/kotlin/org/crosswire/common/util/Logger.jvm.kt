package org.crosswire.common.util

import kotlin.reflect.KClass

internal actual fun loggerName(type: KClass<*>): String = type.java.name

internal actual fun platformLogger(name: String): Logger = Slf4jLogger(org.slf4j.LoggerFactory.getLogger(name))

/** Forwards to slf4j choosing the overload a direct Kotlin call to slf4j would have chosen. */
internal class Slf4jLogger(private val d: org.slf4j.Logger) : Logger {
    override val name: String get() = d.name
    override val isTraceEnabled get() = d.isTraceEnabled
    override val isDebugEnabled get() = d.isDebugEnabled
    override val isInfoEnabled get() = d.isInfoEnabled
    override val isWarnEnabled get() = d.isWarnEnabled
    override val isErrorEnabled get() = d.isErrorEnabled

    override fun trace(msg: String?, vararg args: Any?) = dispatch(args, { d.trace(msg) }, { d.trace(msg, it) }, { d.trace(msg, it) }, { a, b -> d.trace(msg, a, b) }, { d.trace(msg, *it) })
    override fun debug(msg: String?, vararg args: Any?) = dispatch(args, { d.debug(msg) }, { d.debug(msg, it) }, { d.debug(msg, it) }, { a, b -> d.debug(msg, a, b) }, { d.debug(msg, *it) })
    override fun info(msg: String?, vararg args: Any?) = dispatch(args, { d.info(msg) }, { d.info(msg, it) }, { d.info(msg, it) }, { a, b -> d.info(msg, a, b) }, { d.info(msg, *it) })
    override fun warn(msg: String?, vararg args: Any?) = dispatch(args, { d.warn(msg) }, { d.warn(msg, it) }, { d.warn(msg, it) }, { a, b -> d.warn(msg, a, b) }, { d.warn(msg, *it) })
    override fun error(msg: String?, vararg args: Any?) = dispatch(args, { d.error(msg) }, { d.error(msg, it) }, { d.error(msg, it) }, { a, b -> d.error(msg, a, b) }, { d.error(msg, *it) })

    private inline fun dispatch(
        args: Array<out Any?>,
        none: () -> Unit,
        throwable: (Throwable) -> Unit,
        one: (Any?) -> Unit,
        two: (Any?, Any?) -> Unit,
        many: (Array<out Any?>) -> Unit,
    ) {
        when (args.size) {
            0 -> none()
            1 -> { val a = args[0]; if (a is Throwable) throwable(a) else one(a) }
            2 -> two(args[0], args[1])
            else -> many(args)
        }
    }
}
