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

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg

/**
 * A read-only wrapper around any writable implementation of Key.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ReadOnlyKeyList
/**
 * Simple ctor
 *
 * @param keys the keys
 * @param ignore
 */
(keys: Key?, ignore: Boolean) : Key, Cloneable {
    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        return keys!!.getCardinality()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return keys!!.canHaveChildren()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return keys!!.getChildCount()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#isEmpty()
     */
    override fun isEmpty(): Boolean {
        return keys!!.isEmpty()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return keys!!.contains(key)
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return keys!!.iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only key list"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only key list"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#retainAll(org.crosswire.jsword.passage.Key)
     */
    override fun retainAll(key: Key?) {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only key list"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only key list"))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName()
     */
    override fun getName(): String? {
        return keys!!.getName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getName(org.crosswire.jsword.passage.Key)
     */
    override fun getName(base: Key?): String? {
        return keys!!.getName(base)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getRootName()
     */
    override fun getRootName(): String? {
        return keys!!.getRootName()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisRef()
     */
    override fun getOsisRef(): String? {
        return keys!!.getOsisRef()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getOsisID()
     */
    override fun getOsisID(): String? {
        return keys!!.getOsisID()
    }

    override fun hashCode(): Int {
        return keys!!.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return keys!!.equals(other)
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Key?): Int {
        return keys!!.compareTo(other)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return keys!!.get(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        return keys!!.indexOf(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return keys!!.getParent()
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
        if (ignore) {
            return
        }

        throw IllegalStateException(JSOtherMsg.lookupText("Cannot alter a read-only key list"))
    }

    override fun clone(): ReadOnlyKeyList {
        var clone: ReadOnlyKeyList? = null
        try {
            clone = super<Cloneable>.clone() as ReadOnlyKeyList
            clone.keys = keys!!.clone()
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    /**
     * Do we ignore write requests or throw?
     */
    private var ignore: Boolean = ignore

    /**
     * The Key to which we proxy
     */
    private var keys: Key? = keys

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = -7947159638198641657L
    }
}
