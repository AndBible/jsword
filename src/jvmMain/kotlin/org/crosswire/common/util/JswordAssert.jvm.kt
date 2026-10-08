package org.crosswire.common.util

actual fun jswordAssert(value: Boolean, lazyMessage: () -> Any) {
    assert(value, lazyMessage)
}
