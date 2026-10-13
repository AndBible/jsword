package org.crosswire.common.util

import okio.Buffer
import okio.Sink
import okio.Timeout

/** Preserve lexical output names and FileOutputStream's writes even on an interrupted thread. */
internal fun webResourceOutputSink(uri: KUri): Sink {
    val output = outputCancelled { NetUtil.getOutputStream(uri) }
    // OutputStream.sink() checks Thread.interrupted on write, losing legacy partial downloads on close.
    return object : Sink {
        override fun write(source: Buffer, byteCount: Long) { outputCancelled { output.write(source.readByteArray(byteCount)) } }
        override fun flush() { outputCancelled { output.flush() } }
        override fun timeout(): Timeout = Timeout.NONE
        override fun close() { output.close() }
    }
}

private inline fun <T> outputCancelled(block: () -> T): T = try {
    block()
} catch (error: java.io.InterruptedIOException) {
    throw TransportCancelledException(error)
}
