package org.crosswire.jsword.book.sword

internal actual fun restoreInterruptFlag(error: Throwable) {
    if (error is InterruptedException) Thread.currentThread().interrupt()
}
