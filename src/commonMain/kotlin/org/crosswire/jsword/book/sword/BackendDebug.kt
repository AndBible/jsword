package org.crosswire.jsword.book.sword

import okio.*
import org.crosswire.common.util.*
import org.crosswire.jsword.book.*
import org.crosswire.jsword.book.sword.*
import org.crosswire.jsword.book.filter.SourceFilter

internal expect fun backendDebugFailure(error: Throwable): Unit
