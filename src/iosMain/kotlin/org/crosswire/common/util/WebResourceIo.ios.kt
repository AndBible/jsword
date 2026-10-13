package org.crosswire.common.util

import okio.Sink

/** Owned literal file output, with no lossy Path projection or thread-interruption check. */
internal actual fun webResourceOutputSink(uri: KUri): Sink {
    if (uri.getScheme() != "file") throw okio.IOException("Non-file output requires a platform protocol provider: $uri")
    // NetUtilJvm uses FileOutputStream(uri.path), not File(uri): authority/query do not validate here.
    return NativeLexicalFiles.sink(uri.getPath() ?: throw NullPointerException())
}
