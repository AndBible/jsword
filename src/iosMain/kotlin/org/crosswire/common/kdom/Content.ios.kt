package org.crosswire.common.kdom

actual abstract class Content internal constructor() {
    actual final override fun equals(other: Any?): Boolean = this === other
    actual final override fun hashCode(): Int = super.hashCode()
}

actual open class Document internal constructor() {
    actual final override fun equals(other: Any?): Boolean = this === other
    actual final override fun hashCode(): Int = super.hashCode()
}
