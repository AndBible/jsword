package org.crosswire.jsword.book.sword.state

import okio.IOException
import org.crosswire.common.util.LoggerFactory
import org.crosswire.common.util.RandomAccessSource

/** Match IOUtil.close's suppressed IOException and logger identity for owned state handles. */
internal fun closeStateSource(source: RandomAccessSource?) {
    try {
        source?.close()
    } catch (ex: IOException) {
        LoggerFactory.getLogger("org.crosswire.common.util.IOUtil").error("close", ex)
    }
}
