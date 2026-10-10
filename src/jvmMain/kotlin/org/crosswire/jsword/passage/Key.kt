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

/**
 * A Key is a Key that can contain other Keys.
 *
 * The interface is modeled on the java.util.Set interface customized because
 * KeyLists can only store other Keys and simplified by making add() and
 * remove() return void and not a boolean.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface Key : Comparable<Key?>, MutableIterable<Key> {
    /**
     * A Human readable version of the Key. For Biblical passages this uses
     * short books names, and the shortest sensible rendering, for example
     * "Mat 3:1-4" and "Mar 1:1, 3, 5" and "3Jo, Jude"
     *
     * @return a String containing a description of the Key
     */
    fun getName(): String

    /**
     * A Human readable version of the Key's top level name. For Biblical
     * passages this uses short books names. For a dictionary it might return
     * A-Z.
     *
     * @return a String containing a description of the Key
     */
    fun getRootName(): String

    /**
     * Translate the Key into a human readable string, with the assumption that
     * the specified Key has just been output, so if we are in the same region,
     * we do not need to display the region name, and so on.
     *
     * @param base
     *            The key to use to cut down unnecessary output.
     * @return The string representation
     */
    fun getName(base: Key?): String

    /**
     * The OSIS defined reference specification for this Key. When the key is a
     * single element, it is an OSIS book name with '.' separating the parts.
     * When the key is multiple elements, it uses a range notation. Note, this
     * will create a comma separated list of ranges, which is improper OSIS.
     *
     * @return a String containing the OSIS description of the verses
     */
    fun getOsisRef(): String

    /**
     * The OSIS defined id specification for this Key. When the key is a single
     * element, it is an OSIS book name with '.' separating the parts. When the
     * key is multiple elements, it uses a space to separate each.
     *
     * @return a String containing the OSIS description of the verses
     */
    fun getOsisID(): String

    /**
     * All keys have parents unless they are the root of a Key.
     *
     * @return The parent of this tree, or null if this Key is the root.
     */
    fun getParent(): Key?

    /**
     * Returns false if the receiver is a leaf node and can not have children.
     * Any attempt to add()/remove() will throw
     *
     * @return true if the key can have children
     */
    fun canHaveChildren(): Boolean

    /**
     * Returns the number of children that this node has. Leaf nodes return 0.
     *
     * @return the number of children for the node
     */
    fun getChildCount(): Int

    /**
     * Returns the number of elements in this set (its cardinality). If this set
     * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     * <p>
     * This method is potentially expensive, as it often requires cycling through all the keys in the set.</p>
     * @return the number of elements in this set (its cardinality).
     */
    fun getCardinality(): Int

    /**
     * Does this Key have 0 members
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    fun isEmpty(): Boolean

    /**
     * Returns <tt>true</tt> if this set contains the specified element.
     *
     * @param key
     *            element whose presence in this set is to be tested.
     * @return <tt>true</tt> if this set contains the specified element.
     */
    fun contains(key: Key?): Boolean

    /**
     * Adds the specified element to this set if it is not already present.
     *
     * @param key
     *            element to be added to this set.
     * @throws NullPointerException
     *             if the specified element is null
     */
    fun addAll(key: Key?)

    /**
     * Removes the specified elements from this set if it is present.
     *
     * @param key
     *            object to be removed from this set, if present.
     * @throws NullPointerException
     *             if the specified element is null
     */
    fun removeAll(key: Key?)

    /**
     * Removes all but the specified element from this set.
     *
     * @param key
     *            object to be left in this set.
     * @throws NullPointerException
     *             if the specified element is null
     */
    fun retainAll(key: Key?)

    /**
     * Removes all of the elements from this set (optional operation). This set
     * will be empty after this call returns (unless it throws an exception).
     */
    fun clear()

    /**
     * Gets a key from a specific point in this list of children.
     *
     * @param index
     *            The index of the Key to retrieve
     * @return The specified key
     * @throws IndexOutOfBoundsException
     */
    fun get(index: Int): Key?

    /**
     * Reverse a Key into the position the key holds in the list of children
     *
     * @param that
     *            The Key to find
     * @return The index of the key or &lt; 0 if the key is not in the list
     */
    fun indexOf(that: Key?): Int

    /**
     * Widen the range of the verses/keys in this list. This is primarily for
     * "find x within n verses of y" type applications.
     *
     * @param by
     *            The number of verses/keys to widen by
     * @param restrict
     *            How should we restrict the blurring?
     * @see Passage
     */
    fun blur(by: Int, restrict: RestrictionType?)

    /**
     * Widen the range of the verses/keys in this list. This is primarily for
     * "find x within n verses of y" type applications.
     *
     * @param by
     *            The number of verses/keys to widen by
     * @param restrict
     *            How should we restrict the blurring?
     * @param blurDown true to indicate the passage should be extended downwards (i.e. back towards the start)
     * @param blurUp true to indicate the passage should be extended upwards (i.e. towards the end)
     * @see Passage
     */
    fun blur(by: Int, restrict: RestrictionType?, blurDown: Boolean, blurUp: Boolean)

    /**
     * This needs to be declared here so that it is visible as a method on a
     * derived Key.
     *
     * @return A complete copy of ourselves
     */
    fun clone(): Key

    // equals(Object) and hashCode() were redeclared here in the Java interface so that they
    // were visible as methods on a derived Key; Kotlin interfaces cannot redeclare them
    // without making them abstract for implementers. Every implementer inherits them from Object.

}
