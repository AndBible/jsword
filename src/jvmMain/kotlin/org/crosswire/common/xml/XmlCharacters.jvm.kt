package org.crosswire.common.xml

internal actual fun xmlCharacters(chars: CharArray, offset: Int, length: Int): String = String(chars, offset, length)
internal actual typealias JvmCallbackThrows = kotlin.jvm.Throws
