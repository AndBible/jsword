package org.crosswire.common.util

internal actual fun platformIniLineSeparator(): String = "\n"

internal actual fun unmodifiableIniKeys(keys: Set<String>): Collection<String> = iniLiveView(keys)

internal actual fun unmodifiableIniValues(values: Collection<String>): Collection<String> = iniLiveView(values)
