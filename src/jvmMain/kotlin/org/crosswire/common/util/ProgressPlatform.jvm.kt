package org.crosswire.common.util

/** Task 16 supplies native UUID and source-class actuals, preserving JVM naming here. */
internal actual fun randomJobId(): String = java.util.UUID.randomUUID().toString()
internal actual fun reporterSourceName(source: Any): String {
    val type = if (source is Class<*>) source else source.javaClass
    return type.name.substringAfterLast('.')
}
internal actual fun reporterLoggerType(source: Any?) = source?.javaClass?.kotlin ?: Reporter::class

/** Genuine JVM superclass retains transient source and the frozen serialized descriptor. */
actual typealias ProgressEventBase = java.util.EventObject

/** AWT registration remains host-only, including the original nested handler's binary name. */
internal actual fun grabReporterAwtExceptions(grab: Boolean) {
    val property = "sun.awt.exception.handler"
    val name = Reporter.CustomAWTExceptionHandler::class.java.name
    if (grab) System.setProperty(property, name)
    else if (System.getProperty(property) == name) System.getProperties().remove(property)
}
