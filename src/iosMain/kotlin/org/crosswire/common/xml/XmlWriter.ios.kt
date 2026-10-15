package org.crosswire.common.xml

actual abstract class XmlWriter {
    actual abstract fun write(text: String?)
    actual abstract fun write(character: Int)
}

internal actual fun stringXmlWriter(): XmlWriter = object : XmlWriter() {
    private val text = StringBuilder()
    override fun write(text: String?) { this.text.append(text ?: throw NullPointerException()) }
    override fun write(character: Int) { text.append(character.toChar()) }
    override fun toString(): String = text.toString()
}

actual open class XmlTransformerException : Exception()

internal actual fun reportXmlWriteFailure(error: okio.IOException) { error.printStackTrace() }

internal actual fun xmlIndentation(chars: CharArray, length: Int): String = chars.concatToString(0, length)
