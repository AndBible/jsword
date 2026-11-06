package org.crosswire.jsword.book.sword

/** Re-sets the current thread's interrupt flag when [error] is an interruption that a catch-all swallowed (JVM only). */
internal expect fun restoreInterruptFlag(error: Throwable)
