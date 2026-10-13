package org.crosswire.jsword.book.sword

/** Preserve the experimental dump methods' stderr diagnostics across the common boundary. */
internal fun backendDebugFailure(error: Throwable) {
    error.printStackTrace()
}
