package org.crosswire.common.util

actual open class ArrayIndexOutOfBoundsException actual constructor(message: String?) : IndexOutOfBoundsException(message) {
    actual constructor(index: Int) : this("Array index out of range: $index")
}
