package org.crosswire.common.util

/** Task 16 supplies native UUID and source-class actuals, preserving JVM naming here. */
internal fun randomJobId(): String = java.util.UUID.randomUUID().toString()
internal fun reporterSourceName(source: Any): String {
    val type = if (source is Class<*>) source else source.javaClass
    return type.name.substringAfterLast('.')
}
internal fun reporterLoggerType(source: Any?) = source?.javaClass?.kotlin ?: Reporter::class

/** Genuine JVM superclass retains transient source and the frozen serialized descriptor. */
internal typealias ProgressEventBase = java.util.EventObject

/** AWT registration remains host-only, including the original nested handler's binary name. */
internal fun grabReporterAwtExceptions(grab: Boolean) {
    val property = "sun.awt.exception.handler"
    val name = Reporter.CustomAWTExceptionHandler::class.java.name
    if (grab) System.setProperty(property, name)
    else if (System.getProperty(property) == name) System.getProperties().remove(property)
}
