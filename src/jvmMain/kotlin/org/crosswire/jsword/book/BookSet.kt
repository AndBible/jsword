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
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book

import java.util.Collections
import java.util.TreeSet

import org.crosswire.common.util.Filter

open class BookSet : ArrayList<Book>, MutableSet<Book> {
    constructor() : super()

    constructor(books: Collection<Book>) : this() {
        addAll(books)
    }

    /**
     * Gets the sorted set of all keys which can be used for groupings. These
     * are all the property keys across the BookMetaDatas in this list.
     *
     * @return the set of all keys which can be used for grouping.
     */
    open val groups: Set<String>
        get() {
            val results: MutableSet<String> = TreeSet()
            for (book in this) {
                results.addAll(book.propertyKeys)
            }
            return results
        }

    /**
     * Get the sorted set of all values for a particular key. If there is a
     * BookMetaData that does not have a value for that key, then null will be
     * in the set. This can be use to categorize books that don't have that key.
     * For example, "Language" will return all the languages for this
     * BookMetaDataList and null for which the language is unknown.
     *
     * @param key the property key
     * @return the values for a particular key.
     */
    open fun getGroup(key: String?): Set<Any> {
        val results: MutableSet<Any> = TreeSet()
        for (book in this) {
            val property: Any? = book.getProperty(key)
            if (property != null) {
                results.add(property)
            }
        }
        return results
    }

    open fun filter(key: String?, value: Any?): BookSet {
        return filter(GroupFilter(key, value))
    }

    /**
     * Get a set of books that satisfy the condition imposed by the filter.
     *
     * @param filter the condition on which to select books
     * @return the set of matching books
     */
    open fun filter(filter: Filter<Book>): BookSet {
        // create a copy of the list and
        // remove everything that fails the test.
        val listSet = clone() as BookSet
        val iter = listSet.iterator()
        while (iter.hasNext()) {
            val obj = iter.next()
            if (!filter.test(obj)) {
                iter.remove()
            }
        }
        return listSet
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#add(int, java.lang.Object)
     */
    override fun add(index: Int, element: Book) {
        // ignore the requested index
        add(element)
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#add(java.lang.Object)
     */
    final override fun add(element: Book): Boolean {
        // Add the item only if it is not in the list.
        // Add it into the list so that it is in sorted order.
        val pos = Collections.binarySearch(this, element)
        if (pos < 0) {
            super.add(-pos - 1, element)
            return true
        }
        return false
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#addAll(java.util.Collection)
     */
    final override fun addAll(elements: Collection<Book>): Boolean {
        // Might be better to add the list to the end
        // and then sort the list.
        // This can be revisited if the list performs badly.
        var added = false
        for (book in elements) {
            try {
                book.toString()
            } catch (e: NullPointerException) {
                continue
            }
            if (add(book)) {
                added = true
            }
        }
        return added
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#addAll(int, java.util.Collection)
     */
    final override fun addAll(index: Int, elements: Collection<Book>): Boolean {
        // Ignore the index
        return addAll(elements)
    }

    /* (non-Javadoc)
     * @see java.util.ArrayList#set(int, java.lang.Object)
     */
    override fun set(index: Int, element: Book): Book {
        // remove the item at the index (keep it to return it),
        // then insert the item into the sorted list.
        val item = removeAt(index)
        add(element)
        return item
    }

    /**
     * Kotlin must be told which inherited implementation wins: Java picks the one of the superclass
     * (ArrayList) over the default method of Set.
     */
    override fun spliterator(): java.util.Spliterator<Book> {
        return super<ArrayList>.spliterator()
    }

    /**
     * GroupFilter does the SQL traditional group by.
     */
    private class GroupFilter(private var key: String?, private var value: Any?) : Filter<Book> {
        override fun test(obj: Book): Boolean {
            val property = obj.getProperty(key)
            return property != null && property == value
        }
    }

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = 3258688806185154867L
    }
}
