package org.crosswire.common.util

/** Keeps the JVM exception identity of passage bounds checks when they move to common. */
expect open class ArrayIndexOutOfBoundsException(message: String?) : IndexOutOfBoundsException {
    constructor(index: Int)
}
