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
 * A Key that knows where the data is in the real file.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class TreeKey : AbstractKeyList {
    /** Copies the fields and preserves the deep-copy steps of the original clone. */
    protected constructor(copyFrom: TreeKey) : super(copyFrom) {
        parent = copyFrom.parent
        children = copyFrom.children
    }

    /**
     * Setup with the key name and positions of data in the file
     *
     * @param name the key for this TreeKey
     * @param parent the parent node for this TreeKey
     */
    constructor(name: String?, parent: Key?) : super(name) {
        this.parent = parent
        this.children = ArrayList()
    }

    /**
     * Setup with the key name. Use solely for searching.
     *
     * @param text the key for this TreeKey
     */
    constructor(text: String?) : this(text, null)

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#canHaveChildren()
     */
    override fun canHaveChildren(): Boolean {
        return true
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getChildCount()
     */
    override fun getChildCount(): Int {
        return children.size
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getCardinality()
     */
    override fun getCardinality(): Int {
        var cardinality = 1 // count this node
        for (child in children) {
            cardinality += child!!.getCardinality()
        }

        return cardinality
    }

    override fun isEmpty(): Boolean {
        return children.isEmpty()
    }

    override fun contains(key: Key?): Boolean {
        if (children.contains(key)) {
            return true
        }

        for (child in children) {
            if (child!!.contains(key)) {
                return true
            }
        }

        return false
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    override fun iterator(): MutableIterator<Key> {
        return KeyIterator(this)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#addAll(org.crosswire.jsword.passage.Key)
     */
    override fun addAll(key: Key?) {
        children.add(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#removeAll(org.crosswire.jsword.passage.Key)
     */
    override fun removeAll(key: Key?) {
        children.remove(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#clear()
     */
    override fun clear() {
        children.clear()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#get(int)
     */
    override fun get(index: Int): Key? {
        return children.get(index)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#indexOf(org.crosswire.jsword.passage.Key)
     */
    override fun indexOf(that: Key?): Int {
        return children.indexOf(that)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#getParent()
     */
    override fun getParent(): Key? {
        return parent
    }

    /** equality is tricky if comparing TreeKeys (as used by GenBooks) because some child keys can have the same name but different parents
     */
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

        val otherTreeKey = other as TreeKey
        if (getName() != otherTreeKey.getName()) {
            return false
        }

        // names match so now work up the tree comparing parents

        if (getParent() == null) {
            return otherTreeKey.getParent() == null
        }

        // KeyTrees nodes can have the same name but different parents
        return getParent()!! == otherTreeKey.getParent()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?) {
        blur(by, restrict, true, true)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.Key#blur(int, org.crosswire.jsword.passage.RestrictionType)
     */
    override fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean) {
        log.warn("attempt to blur a non-blur-able list")
    }

    override fun clone(): TreeKey {
        check(this::class == TreeKey::class) { "${this::class} must override clone()" }
        return TreeKey(this)
    }

    override fun getRootName(): String {
        var rootName = getName()
        var parentKey: Key? = this
        while (parentKey != null && parentKey.getName().length > 0) {
            rootName = parentKey.getName()
            parentKey = parentKey.getParent()
        }
        return rootName
    }

    override fun getOsisRef(): String {
        return getOsisID()
    }

    override fun getOsisID(): String {
        val b = StringBuilder(100)
        b.append(getName())
        var parentKey: Key? = this.getParent()
        while (parentKey != null && parentKey.getName().length > 0) {
            b.insert(0, "/")
            b.insert(0, parentKey.getName())
            parentKey = parentKey.getParent()
        }
        // Remove the leading .
        return b.toString()
    }

    /**
     * The parent of this key.
     */
    private var parent: Key? = null

    /**
     * The immediate children of this tree node.
     */
    private var children: MutableList<Key?>

    private companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(TreeKey::class)
    }
}
