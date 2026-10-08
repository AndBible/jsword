package org.crosswire.common.util

actual fun jswordAssert(value: Boolean, lazyMessage: () -> Any) {
    // Java assertions are disabled in production builds; native has no -ea switch.
}
