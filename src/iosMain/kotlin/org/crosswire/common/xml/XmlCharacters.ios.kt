package org.crosswire.common.xml

internal actual fun xmlCharacters(chars: CharArray, offset: Int, length: Int): String {
    requireSlice(chars.size, offset, length)
    return chars.concatToString(offset, offset + length)
}

private fun requireSlice(size: Int, offset: Int, length: Int) {
    if (offset < 0 || length < 0 || offset > size - length) {
        throw IndexOutOfBoundsException("offset $offset, count $length, length $size")
    }
}
