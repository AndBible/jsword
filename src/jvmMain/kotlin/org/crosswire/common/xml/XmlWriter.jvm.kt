package org.crosswire.common.xml

actual typealias XmlWriter = java.io.Writer
internal actual fun stringXmlWriter(): XmlWriter = java.io.StringWriter()
actual typealias XmlTransformerException = javax.xml.transform.TransformerException
