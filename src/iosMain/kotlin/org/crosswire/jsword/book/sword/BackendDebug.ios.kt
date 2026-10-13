@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.crosswire.jsword.book.sword

import platform.posix.fputs
import platform.posix.stderr

internal actual fun backendDebugFailure(error: Throwable) {
    fputs(error.stackTraceToString() + "\n", stderr)
}
