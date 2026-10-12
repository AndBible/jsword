package org.crosswire.common.util

/** Live read-only views; even no-op mutations reject the caller as the legacy views did. */
internal open class ReadOnlyIniCollection<T>(private val backing: Collection<T>) : AbstractMutableCollection<T>() {
    override val size: Int get() = backing.size
    override fun contains(element: T): Boolean = backing.contains(element)
    override fun add(element: T): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: T): Boolean = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()
    override fun iterator(): MutableIterator<T> {
        val iterator = backing.iterator()
        return object : MutableIterator<T> {
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): T = iterator.next()
            override fun remove(): Unit = throw UnsupportedOperationException()
        }
    }
}

internal class ReadOnlyIniSet<T>(private val backing: Set<T>) : AbstractMutableSet<T>() {
    override val size: Int get() = backing.size
    override fun contains(element: T): Boolean = backing.contains(element)
    override fun add(element: T): Boolean = throw UnsupportedOperationException()
    override fun addAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun remove(element: T): Boolean = throw UnsupportedOperationException()
    override fun removeAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun retainAll(elements: Collection<T>): Boolean = throw UnsupportedOperationException()
    override fun clear(): Unit = throw UnsupportedOperationException()
    override fun iterator(): MutableIterator<T> = ReadOnlyIniCollection(backing).iterator()
}
