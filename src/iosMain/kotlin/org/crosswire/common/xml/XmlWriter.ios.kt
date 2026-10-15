package org.crosswire.common.xml

actual abstract class XmlWriter {
    private val text = StringBuilder()
    actual open fun write(text: String?) { this.text.append(text ?: throw NullPointerException()) }
    actual open fun write(character: Int) { text.append(character.toChar()) }
    override fun toString(): String = text.toString()
}

internal actual fun stringXmlWriter(): XmlWriter = object : XmlWriter() {}

actual open class XmlTransformerException : Exception()

internal actual fun reportXmlWriteFailure(error: okio.IOException) { error.printStackTrace() }

internal actual fun xmlIndentation(chars: CharArray, length: Int): String = chars.concatToString(0, length)
