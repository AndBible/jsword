package org.crosswire.common.util

import kotlin.reflect.KClass
import platform.Foundation.NSUUID

internal actual fun randomJobId(): String = NSUUID().UUIDString.lowercase()
internal actual fun reporterSourceName(source: Any): String =
    (if (source is KClass<*>) source else source::class).simpleName ?: ""
internal actual fun reporterLoggerType(source: Any?): KClass<*> = source?.let { it::class } ?: Reporter::class
internal actual fun grabReporterAwtExceptions(grab: Boolean) {
    // AWT exception registration is JVM-only.
}

actual open class ProgressEventBase actual constructor(source: Any?) {
    private val eventSource: Any = requireNotNull(source) { "null source" }
    actual open fun getSource(): Any? = eventSource
    actual override fun toString(): String = "${this::class.qualifiedName}[source=$eventSource]"
}
