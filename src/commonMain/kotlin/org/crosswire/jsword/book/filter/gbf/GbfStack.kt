package org.crosswire.jsword.book.filter.gbf

/** Required GBF stack operations; JVM retains the public LinkedList parameter contract. */
expect class GbfStack<E>() {
    fun isEmpty(): Boolean
    fun addFirst(element: E)
    fun removeFirst(): E
    operator fun get(index: Int): E
    fun getLast(): E
}
