package org.crosswire.common.util

/** Task 16 supplies native UUID and source-class actuals, preserving JVM naming here. */
internal fun randomJobId(): String = java.util.UUID.randomUUID().toString()
internal fun eventClassName(event: Any): String = event.javaClass.name
internal fun reporterSourceName(source: Any): String {
    val type = if (source is Class<*>) source else source.javaClass
    return type.name.substringAfterLast('.')
}
internal fun reporterLoggerType(source: Any?) = source?.javaClass?.kotlin ?: Reporter::class

/** JVM Serializable marker retained for events; native actual is an empty interface. */
internal typealias ProgressEventSerializable = java.io.Serializable

/** AWT registration remains host-only, including the original nested handler's binary name. */
internal fun grabReporterAwtExceptions(grab: Boolean) {
    val property = "sun.awt.exception.handler"
    val name = Reporter.CustomAWTExceptionHandler::class.java.name
    if (grab) System.setProperty(property, name)
    else if (System.getProperty(property) == name) System.getProperties().remove(property)
}
