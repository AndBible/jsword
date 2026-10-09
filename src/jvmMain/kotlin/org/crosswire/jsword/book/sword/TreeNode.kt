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
package org.crosswire.jsword.book.sword

import org.crosswire.common.util.jswordAssert
import java.io.Serializable

/**
 * A node that knows where the data is in the real file and where it is in
 * relationship to other nodes.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
// was package-private; public because the public TreeKeyIndex methods return it (a Kotlin public member cannot expose an internal type)
open class TreeNode
/**
 * Setup with the positions of data in the file
 * 
 * @param theOffset
 */
// was package-private
internal constructor(theOffset: Int) : Cloneable, Serializable {
    /**
     * TreeNode default ctor.
     */
    // was package-private
    internal constructor() : this(-1)

    /**
     * The offset of this TreeNode in the offset.
     */
    /**
     * @return the offset
     */
    /**
     * @param newOffset
     * the offset to set
     */
    open var offset: Int = theOffset

    /**
     * The name of this TreeNode. Note, this is not the path. To get the path,
     * one needs to traverse to the parent to construct the path.
     */
    /**
     * @return the name
     */
    /**
     * @param newName
     * the name to set
     */
    open var name: String = ""

    /**
     * Optional, extra data associated with this TreeNode. For example, this is
     * used to store offset and length for a raw genbook.
     */
    /**
     * @return the userData
     */
    /**
     * @param theUserData
     * the userData to set
     */
    open var userData: ByteArray = ByteArray(0)
        get() = field.clone()
        set(value) {
            field = value.clone()
        }

    /**
     * The offset of the first child record in the offset. Leaf nodes are
     * indicated with a value of -1. That is, this TreeNode does not have any
     * children.
     */
    /**
     * @return the firstChild
     */
    /**
     * @param firstChild
     * the firstChild to set
     */
    open var firstChild: Int = -1

    /**
     * @return whether there are children
     */
    open fun hasChildren(): Boolean {
        return firstChild != -1
    }

    /**
     * The offset of the next sibling record in the offset. Final siblings are
     * indicated with a value of -1. That is, this TreeNode does not have a next
     * sibling.
     */
    /**
     * @return the nextSibling
     */
    /**
     * @param nextSibling
     * the nextSibling to set
     */
    open var nextSibling: Int = -1

    /**
     * @return if there are more siblings
     */
    open fun hasNextSibling(): Boolean {
        return nextSibling != -1
    }

    /**
     * The offset of the parent record in the offset. Root nodes are indicated
     * with a value of -1. That is, this TreeNode does not have a parent.
     */
    /**
     * @return the parent
     */
    /**
     * @param parent
     * the parent to set
     */
    open var parent: Int = -1

    public override fun clone(): TreeNode {
        var clone: TreeNode? = null
        try {
            clone = super.clone() as TreeNode
        } catch (e: CloneNotSupportedException) {
            jswordAssert(false) { e }
        }
        return clone!!
    }

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = -2472601787934480762L
    }
}
