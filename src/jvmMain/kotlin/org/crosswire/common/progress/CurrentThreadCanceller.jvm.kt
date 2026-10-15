package org.crosswire.common.progress

internal actual fun currentThreadCanceller(): (() -> Unit)? {
    val thread = Thread.currentThread()
    return { thread.interrupt() }
}
