package org.crosswire.common.util

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun randomJobId(): String
internal expect fun reporterSourceName(source: Any): String
internal expect fun reporterLoggerType(source: Any?): kotlin.reflect.KClass<*>
internal expect fun grabReporterAwtExceptions(grab: Boolean): Unit
expect open class ProgressEventBase(source: Any?) {
    open fun getSource(): Any?
    override fun toString(): String
}
