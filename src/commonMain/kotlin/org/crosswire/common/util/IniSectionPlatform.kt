package org.crosswire.common.util

internal expect fun platformIniLineSeparator(): String
internal expect fun unmodifiableIniKeys(keys: Set<String>): Collection<String>
internal expect fun unmodifiableIniValues(values: Collection<String>): Collection<String>
