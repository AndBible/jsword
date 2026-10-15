package org.crosswire.common.xml

/** Only the operations used by the SAX serializer; JVM retains its Writer API. */
expect abstract class XmlWriter {
    open fun write(text: String?)
    open fun write(character: Int)
}

internal expect fun stringXmlWriter(): XmlWriter

expect open class XmlTransformerException : Exception

/** Preserve the legacy write-failure diagnostic without changing callback propagation. */
internal expect fun reportXmlWriteFailure(error: okio.IOException)

internal expect fun xmlIndentation(chars: CharArray, length: Int): String
