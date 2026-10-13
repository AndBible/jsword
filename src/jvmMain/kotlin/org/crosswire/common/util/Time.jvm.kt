package org.crosswire.common.util

/** Wall-clock epoch milliseconds, preserving state expiry and access timestamp semantics. */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
