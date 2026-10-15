package org.crosswire.common.xml

actual typealias XmlWriter = java.io.Writer
internal actual fun stringXmlWriter(): XmlWriter = java.io.StringWriter()
actual typealias XmlTransformerException = javax.xml.transform.TransformerException

internal actual fun reportXmlWriteFailure(error: okio.IOException) { error.printStackTrace(System.err) }

internal actual fun xmlIndentation(chars: CharArray, length: Int): String = String(chars, 0, length).intern()
