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
 * © CrossWire Bible Society, 2008 - 2016
 *
 */
package org.crosswire.jsword.book.sword

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.sword.state.OpenFileState
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.RestrictionType
import java.io.IOException
import java.util.NoSuchElementException

/**
 * A Backend that can be used as a global key list.
 * 
 * @param <T> The type of the OpenFileState that this class extends.
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
abstract class AbstractKeyBackend<T : OpenFileState>
/**
 * Simple ctor
 * 
 * @param sbmd
 * the book's metadata
 */
(sbmd: SwordBookMetaData?) : AbstractBackend<T>(sbmd), Key, Cloneable {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return getCardinality() == 0
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.sword.AbstractBackend#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return indexOf(key) >= 0
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return object : MutableIterator<Key> {

            /* (non-Javadoc)
             * @see java.util.Iterator#hasNext()
             */
            override fun hasNext(): Boolean {
                return here < count
            }

            /* (non-Javadoc)
             * @see java.util.Iterator#next()
             */
            @Throws(NoSuchElementException::class)
            override fun next(): Key {
                if (here >= count) {
                    throw NoSuchElementException()
                }
                return get(here++)!!
            }

            /* (non-Javadoc)
             * @see java.util.Iterator#remove()
             */
            override fun remove() {
                throw UnsupportedOperationException()
            }

            private var here = 0
            private val count = getCardinality()
        }
    }


    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun setAliasKey(state: T, alias: Key?, source: Key?) {
        throw UnsupportedOperationException()
    }

    @Throws(BookException::class, IOException::class)
    override fun setRawText(state: T, key: Key?, text: String?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return null
    }

    override fun clone(): AbstractKeyBackend<T> {
        var clone: AbstractKeyBackend<T>? = null
        try {
            @Suppress("UNCHECKED_CAST")
            clone = super<Cloneable>.clone() as AbstractKeyBackend<T>
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String {
        return bookMetaData.initials
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    override fun equals(other: Any?): Boolean {
        // Since this can not be null
        if (other == null) {
            return false
        }

        // Check that that is the same as this
        // Don't use instanceOf since that breaks inheritance
        if (other.javaClass != this.javaClass) {
            return false
        }

        return compareTo(other as Key) == 0
    }

    override fun hashCode(): Int {
        return getName().hashCode()
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {

        if (this === other) {
            return 0
        }

        if (other == null) {
            // he is empty, we are not so he is greater
            return -1
        }

        val ret = this.getName().compareTo(other.getName())

        if (ret != 0) {
            return ret
        }

        // Compare the contents.
        val thisIter = this.iterator()
        val thatIter = other.iterator()

        var thisfirst: Key? = null
        var thatfirst: Key? = null

        if (thisIter.hasNext()) {
            thisfirst = thisIter.next()
        }

        if (thatIter.hasNext()) {
            thatfirst = thatIter.next()
        }

        if (thisfirst == null) {
            if (thatfirst == null) {
                // we are both empty, and rank the same
                return 0
            }
            // i am empty, he is not so we are greater
            return 1
        }

        if (thatfirst == null) {
            // he is empty, we are not so he is greater
            return -1
        }

        return thisfirst.getName().compareTo(thatfirst.getName())
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
    }


    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = -2782112117361556089L
    }
}
