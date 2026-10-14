package org.crosswire.common.util

/** Live INI view shared by Native and the platform-independent iterator contract tests. */
internal fun iniLiveView(values: Collection<String>): Collection<String> = object : AbstractCollection<String>() {
    override val size: Int get() = values.size
    override fun iterator(): Iterator<String> {
        val iterator = values.iterator()
        // Do not expose the backing MutableIterator through the read-only view.
        return object : Iterator<String> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): String = iterator.next()
        }
    }
    override fun contains(element: String): Boolean = values.contains(element)
}
