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
package org.crosswire.jsword.passage

import kotlin.jvm.JvmSuppressWildcards

import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * A Key that uses a Set of Keys as it's store of data.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SetKeyList : AbstractKeyList {
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: SetKeyList) : super(copyFrom) {
        parent = copyFrom.parent
        list = copyFrom.list
    }

    override fun clone(): SetKeyList {
        check(this::class == SetKeyList::class) { "${this::class} must override clone()" }
        return SetKeyList(this)
    }

    /**
     * Simple ctor
     * @param set
     */
    constructor(set: Set<@JvmSuppressWildcards Key>?) : this(set, null as Key?, null as String?)

    /**
     * Simple ctor
     * @param set
     * @param name
     */
    constructor(set: Set<@JvmSuppressWildcards Key>?, name: String?) : this(set, null as Key?, name)

    /**
     * Simple ctor
     * @param set
     * @param parent
     */
    constructor(set: Set<@JvmSuppressWildcards Key>?, parent: Key?) : this(set, parent, null as String?)

    /**
     * Simple ctor
     * @param set
     * @param parent
     * @param name
     */
    constructor(set: Set<@JvmSuppressWildcards Key>?, parent: Key?, name: String?) : super(name) {
        this.parent = parent
        list.addAll(set!!)
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#add(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        list.add(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        list.clear()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage
     * .Key)
     */
    override fun contains(key: Key?): Boolean {
        return list.contains(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(other: Any?): Boolean {
        if (other is SetKeyList) {
            return list == other.list
        }
        return false
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        return list.hashCode()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#iterator()
     */
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<Key> {
        return list.iterator() as MutableIterator<Key>
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#remove(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        list.remove(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return list.size
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return false
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return 0
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return list.get(index)
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage
     * .Key)
     */
    override fun indexOf(that: Key?): Int {
        return list.indexOf(that)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return parent
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#blur(int)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#blur(int)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        log.warn("attempt to blur a non-blur-able list")
    }

    /**
     * The parent of this key
     */
    private var parent: Key? = null

    /**
     * The Set that we are proxying to
     */
    private var list: MutableList<Key?> = ArrayList()

    private companion object {

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SetKeyList::class)
    }
}
