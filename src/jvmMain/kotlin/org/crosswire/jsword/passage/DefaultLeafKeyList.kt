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

import org.crosswire.common.util.ItemIterator
import org.crosswire.common.util.jswordAssert

/**
 * A simple default implementation of the Key interface.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DefaultLeafKeyList : Key, Cloneable {
    /**
     * Default ctor
     * @param name
     */
    constructor(name: String?) : this(name, name, null)

    /**
     * Default ctor
     * @param name
     * @param osisName
     */
    constructor(name: String?, osisName: String?) : this(name, osisName, null)

    /**
     * Default ctor
     * @param name
     * @param osisName
     * @param parent
     */
    constructor(name: String?, osisName: String?, parent: Key?) {
        this.name = name
        this.parent = parent
        this.osisName = osisName
    }

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
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String? {
        return name
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String? {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String? {
        return getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String? {
        return osisName
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String? {
        return getOsisRef()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return parent
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return 1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return this == key
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return ItemIterator<Key>(this)
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
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        throw UnsupportedOperationException()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        if (index == 0) {
            return this
        }
        return null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        if (this == that) {
            return 0
        }
        return -1
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType, boolean, boolean)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        throw UnsupportedOperationException()
    }

    override fun toString(): String {
        return getName()!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        // Since this can not be null
        if (other == null) {
            return false
        }

        // We might consider checking for equality against all Keys?
        // However currently we don't.

        // Check that that is the same as this
        // Don't use instanceof since that breaks inheritance
        if (other.javaClass != this.javaClass) {
            return false
        }

        // The real bit ...
        val that = other as DefaultLeafKeyList
        return osisName!! == that.osisName
    }

    override fun hashCode(): Int {
        return osisName!!.hashCode()
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        val that = other as DefaultLeafKeyList
        return osisName!!.compareTo(that.osisName!!)
    }

    override fun clone(): DefaultLeafKeyList {
        var clone: DefaultLeafKeyList? = null
        try {
            clone = super<Cloneable>.clone() as DefaultLeafKeyList
            if (parent != null) {
                clone.parent = parent!!.clone()
            }
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    /**
     * The parent of this key
     */
    private var parent: Key? = null

    /**
     * The human readable string that this key represents
     */
    private var name: String? = null

    /**
     * The OSIS version of this Key
     */
    private var osisName: String? = null

    private companion object {
        /**
         *
         */
        private const val serialVersionUID = -7462556005744186622L
    }
}
