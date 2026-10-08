/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2015 - 2016
 */
package org.crosswire.common.util

import java.util.AbstractCollection
import java.util.ArrayList
import java.util.Comparator
import java.util.TreeSet

/**
 * Defines a List that maintains the uniqueness of elements
 *
 * @param E The type of the element in this ListSet.
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
@Suppress("UNCHECKED_CAST")
open class ListSet<E> : AbstractCollection<E>, MutableSet<E> {
    /**
     * Constructs a new, empty list set.
     */
    constructor() : this(null)

    /**
     * Constructs a new, empty list set, sorted according to the specified
     * comparator.  All elements inserted into the set must be *mutually
     * comparable* by the specified comparator: `comparator.compare(e1,
     * e2)` must not throw a `ClassCastException` for any elements
     * `e1` and `e2` in the set.  If the user attempts to add
     * an element to the set that violates this constraint, the
     * `add` call will throw a `ClassCastException`.
     *
     * @param comparator the comparator that will be used to order this set.
     *        If `null`, the natural
     *        ordering of the elements will be used.
     */
    constructor(comparator: Comparator<in E>?) : super() {
        list = ArrayList()
        set = TreeSet(comparator)
    }

    override fun contains(element: E): Boolean {
        return set.contains(element)
    }

    override fun add(element: E): Boolean {
        val added = set.add(element)
        if (added) {
            list.add(element)
        }
        return added
    }

    override fun remove(element: E): Boolean {
        val removed = set.remove(element)
        if (removed) {
            list.remove(element)
        }
        return removed
    }

    open fun remove(index: Int): E {
        val t = list[index]
        remove(t)
        return t
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return set.containsAll(elements)
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        var removed = false
        val it = this.iterator()
        while (it.hasNext()) {
            val next = it.next()
            if (!elements.contains(next)) {
                it.remove()
                removed = true
            }
        }
        return removed
    }

    override val size: Int
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("size")
        get() = list.size

    override fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    override fun clear() {
        set.clear()
        list.clear()
    }

    open fun get(index: Int): E {
        return list[index]
    }

    open fun get(index: Int, defaultValue: E): E {
        val value = list[index]
        return if (value != null) value else defaultValue
    }

    override fun iterator(): MutableIterator<E> {
        // Need to properly implement remove
        return object : MutableIterator<E> {
            override fun hasNext(): Boolean {
                return itr.hasNext()
            }

            override fun next(): E {
                val next = itr.next()
                current = next
                return next
            }

            override fun remove() {
                itr.remove()
                set.remove(current as E)
                current = null
            }

            private val itr: MutableIterator<E> = list.iterator()
            private var current: E? = null
        }
    }

    override fun toArray(): Array<Any?> {
        return (list as java.util.Collection<E>).toArray()
    }

    override fun <T> toArray(a: Array<T>): Array<T> {
        return (list as java.util.Collection<E>).toArray(a)
    }

    @JvmField
    protected var list: MutableList<E>

    @JvmField
    protected var set: MutableSet<E>
}
