package org.crosswire.jsword.book

import org.crosswire.jsword.passage.Key
import org.crosswire.common.util.LoggerFactory
import kotlinx.atomicfu.atomic

actual class DataPolice {
    actual companion object {
        private val reporting = atomic(false)
        private val logger = LoggerFactory.getLogger(DataPolice::class)
        actual var isReporting: Boolean
            get() = reporting.value
            set(value) { reporting.value = value }

        actual fun report(book: Book?, key: Key?, message: String?) {
            if (!reporting.value) return
            val metadata: BookMetaData? = book!!.bookMetaData
            val text = StringBuilder()
            if (metadata != null) text.append(metadata.initials)
            if (metadata != null && key != null) text.append(':')
            if (key != null) text.append(key.getOsisID())
            text.append(": ").append(message)
            logger.info(text.toString())
        }
    }
}
