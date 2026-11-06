package org.crosswire.jsword.book.sword

internal actual fun restoreInterruptFlag(error: Throwable) {
    // No thread interrupt flag on Kotlin/Native.
}
