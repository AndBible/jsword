package org.crosswire.common.xml

/** Only the operations used by the SAX serializer; JVM retains its Writer API. */
expect abstract class XmlWriter {
    fun write(text: String?)
    fun write(character: Int)
}

internal expect fun stringXmlWriter(): XmlWriter

expect open class XmlTransformerException : Exception
