package org.crosswire.common.util

import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Common replacement for `java.util.concurrent.CopyOnWriteArrayList` (K2 F5). Every mutation copies the
 * backing list under a lock and publishes it through a volatile field; reads never lock.
 *
 * Semantics follow the JDK class:
 *  - [iterator], [listIterator] and [subList] work on the snapshot taken when they are called. They never throw
 *    `ConcurrentModificationException` and do not see later changes.
 *  - Iterator `remove`/`set`/`add` throw [UnsupportedOperationException].
 *  - [subList] is a read-only copy, not a write-through view; its mutators throw [UnsupportedOperationException].
 *  - Single operations are atomic; compound sequences (check then add) are not, as with the JDK class.
 *  - Marked [RandomAccess]. Prefer the member [removeAll] with a predicate over the stdlib `MutableList.removeAll { }`
 *    extension: the member does one copy and one publish, whereas the stdlib helper would go through element-wise
 *    mutators (many copies, and not atomic).
 */
class CopyOnWriteList<E>() : MutableList<E>, RandomAccess {
    private val lock = SynchronizedObject()

    @Volatile
    private var array: List<E> = emptyList()

    constructor(elements: Collection<E>) : this() {
        array = elements.toList()
    }

    private inline fun <R> mutate(block: (MutableList<E>) -> R): R = synchronized(lock) {
        val copy = array.toMutableList()
        val result = block(copy)
        array = copy
        result
    }

    /** Applies [block] to a copy and publishes it only when [block] returns true. */
    private inline fun mutateIf(block: (MutableList<E>) -> Boolean): Boolean = synchronized(lock) {
        val copy = array.toMutableList()
        val changed = block(copy)
        if (changed) array = copy
        changed
    }

    override val size: Int get() = array.size
    override fun isEmpty(): Boolean = array.isEmpty()
    override fun contains(element: E): Boolean = array.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = array.containsAll(elements)
    override fun get(index: Int): E = array[index]
    override fun indexOf(element: E): Int = array.indexOf(element)
    override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun add(element: E): Boolean = mutate { it.add(element) }
    override fun add(index: Int, element: E) = mutate { it.add(index, element) }
    override fun addAll(elements: Collection<E>): Boolean = mutateIf { it.addAll(elements) }
    override fun addAll(index: Int, elements: Collection<E>): Boolean = mutateIf { it.addAll(index, elements) }
    override fun set(index: Int, element: E): E = mutate { it.set(index, element) }
    override fun removeAt(index: Int): E = mutate { it.removeAt(index) }
    override fun remove(element: E): Boolean = mutateIf { it.remove(element) }

    // `elements.contains` decides, as in the JDK (so a TreeSet with a comparator works). It runs on a snapshot of this
    // list's own contents and publishes once, so `elements === this` or a `contains` that reads this list is safe.
    override fun removeAll(elements: Collection<E>): Boolean = mutateIf { copy -> copy.removeAll { elements.contains(it) } }
    override fun retainAll(elements: Collection<E>): Boolean = mutateIf { copy -> copy.retainAll { elements.contains(it) } }
    override fun clear() = mutate { it.clear() }

    /** Atomic equivalent of `removeIf`: one copy, one publish. */
    fun removeAll(predicate: (E) -> Boolean): Boolean = mutateIf { it.removeAll(predicate) }

    override fun iterator(): MutableIterator<E> = SnapshotIterator(array, 0)
    override fun listIterator(): MutableListIterator<E> = SnapshotIterator(array, 0)
    override fun listIterator(index: Int): MutableListIterator<E> {
        val snapshot = array
        if (index < 0 || index > snapshot.size) throw IndexOutOfBoundsException("Index: $index, Size: ${snapshot.size}")
        return SnapshotIterator(snapshot, index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        val snapshot = array
        if (fromIndex < 0 || toIndex > snapshot.size || fromIndex > toIndex) {
            throw IndexOutOfBoundsException("fromIndex: $fromIndex, toIndex: $toIndex, size: ${snapshot.size}")
        }
        return ReadOnlySnapshot(snapshot.subList(fromIndex, toIndex).toList())
    }

    override fun equals(other: Any?): Boolean = other === this || (other is List<*> && array == other)
    override fun hashCode(): Int = array.hashCode()
    override fun toString(): String = array.toString()

    private class SnapshotIterator<E>(private val snapshot: List<E>, start: Int) : MutableListIterator<E> {
        private val delegate = snapshot.listIterator(start)
        override fun hasNext(): Boolean = delegate.hasNext()
        override fun next(): E = delegate.next()
        override fun hasPrevious(): Boolean = delegate.hasPrevious()
        override fun previous(): E = delegate.previous()
        override fun nextIndex(): Int = delegate.nextIndex()
        override fun previousIndex(): Int = delegate.previousIndex()
        override fun remove() = throw UnsupportedOperationException()
        override fun set(element: E) = throw UnsupportedOperationException()
        override fun add(element: E) = throw UnsupportedOperationException()
    }

    private class ReadOnlySnapshot<E>(private val items: List<E>) : AbstractMutableList<E>() {
        override val size: Int get() = items.size
        override fun get(index: Int): E = items[index]
        override fun add(index: Int, element: E) = throw UnsupportedOperationException()
        override fun removeAt(index: Int): E = throw UnsupportedOperationException()
        override fun set(index: Int, element: E): E = throw UnsupportedOperationException()
    }
}
