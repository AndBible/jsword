package org.crosswire.common.util

import kotlin.reflect.KClass

/**
 * JSword's logging facade (K2 F2). The JVM actual forwards to slf4j under the same logger names, so
 * Android output through slf4j-android is unchanged; the iOS actual writes through NSLog.
 * Patterns use slf4j's `{}` placeholders; a trailing Throwable not consumed by a placeholder is
 * logged as the exception.
 */
interface Logger {
    val name: String
    val isTraceEnabled: Boolean
    val isDebugEnabled: Boolean
    val isInfoEnabled: Boolean
    val isWarnEnabled: Boolean
    val isErrorEnabled: Boolean
    fun trace(msg: String?, vararg args: Any?)
    fun debug(msg: String?, vararg args: Any?)
    fun info(msg: String?, vararg args: Any?)
    fun warn(msg: String?, vararg args: Any?)
    fun error(msg: String?, vararg args: Any?)
}

enum class LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

/** Settings for platforms without an slf4j binding (iOS). */
object LoggerConfig {
    var iosMinimumLevel: LogLevel = LogLevel.WARN
}

object LoggerFactory {
    fun getLogger(name: String): Logger = platformLogger(name)
    fun getLogger(type: KClass<*>): Logger = platformLogger(loggerName(type))
}

/** The logger name for [type]: on the JVM the binary name (`Outer$Inner`), as slf4j had it. */
internal expect fun loggerName(type: KClass<*>): String

internal expect fun platformLogger(name: String): Logger
