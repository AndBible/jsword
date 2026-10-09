package org.crosswire.common.util

import platform.Foundation.NSLog
import kotlin.reflect.KClass

internal actual fun loggerName(type: KClass<*>): String = type.qualifiedName ?: type.simpleName ?: "ksword"

internal actual fun platformLogger(name: String): Logger = NSLogLogger(name)

private class NSLogLogger(override val name: String) : Logger {
    private fun enabled(level: LogLevel) = level >= LoggerConfig.iosMinimumLevel
    override val isTraceEnabled get() = enabled(LogLevel.TRACE)
    override val isDebugEnabled get() = enabled(LogLevel.DEBUG)
    override val isInfoEnabled get() = enabled(LogLevel.INFO)
    override val isWarnEnabled get() = enabled(LogLevel.WARN)
    override val isErrorEnabled get() = enabled(LogLevel.ERROR)
    override fun trace(msg: String?, vararg args: Any?) = log(LogLevel.TRACE, msg, args)
    override fun debug(msg: String?, vararg args: Any?) = log(LogLevel.DEBUG, msg, args)
    override fun info(msg: String?, vararg args: Any?) = log(LogLevel.INFO, msg, args)
    override fun warn(msg: String?, vararg args: Any?) = log(LogLevel.WARN, msg, args)
    override fun error(msg: String?, vararg args: Any?) = log(LogLevel.ERROR, msg, args)

    private fun log(level: LogLevel, msg: String?, args: Array<out Any?>) {
        if (!enabled(level)) return
        val r = Slf4jFormat.formatForLog(msg, args)
        val text = buildString {
            append(level.name).append(' ').append(name).append(" - ").append(r.message)
            r.throwable?.let { append('\n').append(it.stackTraceToString()) }
        }
        NSLog("%@", text)
    }
}
