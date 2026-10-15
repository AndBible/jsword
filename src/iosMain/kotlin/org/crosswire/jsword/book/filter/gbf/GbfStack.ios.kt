package org.crosswire.jsword.book.filter.gbf

actual class GbfStack<E> actual constructor() {
    private val elements = ArrayDeque<E>()
    actual fun isEmpty(): Boolean = elements.isEmpty()
    actual fun addFirst(element: E) { elements.addFirst(element) }
    actual fun removeFirst(): E {
        if (elements.isEmpty()) throw NoSuchElementException()
        return elements.removeFirst()
    }
    actual operator fun get(index: Int): E {
        if (index !in elements.indices) throw IndexOutOfBoundsException("Index: $index, Size: ${elements.size}")
        return elements[index]
    }
    actual fun getLast(): E {
        if (elements.isEmpty()) throw NoSuchElementException()
        return elements.last()
    }
}
