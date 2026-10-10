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

import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * A default implementation of Key.
 *
 *
 * This implementation uses `java.util.List` to store keys.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DefaultKeyList : AbstractKeyList {
    /**
     * Simple ctor
     */
    constructor() : super(null)

    /**
     * Simple ctor
     * @param parent
     * @param name
     */
    constructor(parent: Key?, name: String?) : super(name) {
        this.parent = parent
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
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return keys.size
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return keys.isEmpty()
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage
     * .Key)
     */
    override fun contains(key: Key?): Boolean {
        return keys.contains(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#iterator()
     */
    @Suppress("UNCHECKED_CAST")
    override fun iterator(): MutableIterator<Key> {
        return keys.iterator() as MutableIterator<Key>
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#add(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        keys.add(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#remove(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        keys.remove(key)
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        keys.clear()
    }

    /*
     * (non-Javadoc)
     *
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return keys.get(index)
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage
     * .Key)
     */
    override fun indexOf(that: Key?): Int {
        return keys.indexOf(that)
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
        this.blur(by, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        log.warn("attempt to blur a non-blur-able list")
    }

    /**
     * The parent of this key
     */
    private var parent: Key? = null

    /**
     * The store of Keys
     */
    private var keys: MutableList<Key?> = ArrayList()

    private companion object {

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(DefaultKeyList::class)
    }
}
