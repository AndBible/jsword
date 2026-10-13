package org.crosswire.common.util

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

fun interface Cancellable { fun cancel() }
expect fun scheduleRepeating(periodMs: Long, task: () -> Unit): Cancellable
