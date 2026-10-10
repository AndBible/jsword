package org.crosswire.common.util

/**
 * A sorted `MutableSet` over a sorted `ArrayList` with binary search (K3), replacing `java.util.TreeSet` where only
 * `MutableSet` operations are used. Iteration is ascending; the iterator supports `remove` and fails fast on
 * concurrent modification, as `TreeSet`'s does. Insert/remove are O(n); the passage stores it backs are small.
 */
class SortedArraySet<T>(private val comparator: Comparator<in T>) : AbstractMutableSet<T>() {
    private val items = ArrayList<T>()
    private var modCount = 0

    override val size: Int get() = items.size

    private fun search(element: T): Int = items.binarySearch(element, comparator)

    override fun contains(element: T): Boolean = search(element) >= 0

    override fun add(element: T): Boolean {
        val i = search(element)
        if (i >= 0) return false
        items.add(-i - 1, element)
        modCount++
        return true
    }

    override fun remove(element: T): Boolean {
        val i = search(element)
        if (i < 0) return false
        items.removeAt(i)
        modCount++
        return true
    }

    override fun clear() {
        items.clear()
        modCount++
    }

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        private var next = 0
        private var last = -1
        private var expected = modCount

        override fun hasNext(): Boolean = next < items.size

        override fun next(): T {
            if (modCount != expected) throw ConcurrentModificationException()
            if (next >= items.size) throw NoSuchElementException()
            last = next
            return items[next++]
        }

        override fun remove() {
            check(last >= 0) { "next() not called or element already removed" }
            if (modCount != expected) throw ConcurrentModificationException()
            items.removeAt(last)
            next = last
            last = -1
            modCount++
            expected = modCount
        }
    }

    companion object {
        /** A set ordered by the elements' natural order. */
        operator fun <T : Comparable<T>> invoke(): SortedArraySet<T> = SortedArraySet(naturalOrder())
    }
}
