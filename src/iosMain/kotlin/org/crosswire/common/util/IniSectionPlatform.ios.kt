package org.crosswire.common.util

internal actual fun platformIniLineSeparator(): String = "\n"

internal actual fun unmodifiableIniKeys(keys: Set<String>): Collection<String> = object : AbstractCollection<String>() {
    override val size: Int get() = keys.size
    override fun iterator(): Iterator<String> = keys.iterator()
    override fun contains(element: String): Boolean = keys.contains(element)
}

internal actual fun unmodifiableIniValues(values: Collection<String>): Collection<String> = object : AbstractCollection<String>() {
    override val size: Int get() = values.size
    override fun iterator(): Iterator<String> = values.iterator()
    override fun contains(element: String): Boolean = values.contains(element)
}
