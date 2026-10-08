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

import java.util.Comparator

/**
 * ChainLink allows for a doubly linked list embedded within objects.
 * This is meant for small lists.
 *
 * @param E the class to be chained
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class ChainLink<E>
/**
 * Create a ChainLink containing an item.
 *
 * @param item this ChainLink's item
 */
(
    item: E
) {
    /**
     * Create an empty ChainLink.
     * It is necessary to call `setItem()` for this to be useful.
     */
    @Suppress("UNCHECKED_CAST")
    constructor() : this(null as E)

    /**
     * The item of this ChainLink.
     * Set the item for this ChainLink / Get the item from this ChainLink.
     */
    var item: E = item

    /**
     * One node is marked as the head of the chain.
     *
     * @return whether this node is the head of the chain.
     */
    var isHead: Boolean = true
        private set

    /**
     * Get the node to the left of this node.
     * The left node of the head is the tail.
     *
     * @return the node to the left
     */
    var left: ChainLink<E> = this
        private set

    /**
     * Get the node to the right of this node.
     * The right node of the tail is the head.
     *
     * @return the node to the right
     */
    var right: ChainLink<E> = this
        private set

    /**
     * Remove this ChainLink from the chain.
     * If the head is removed, then the node that was to the right is now the head.
     */
    fun remove() {
        // First ensure that
        // this node is removed from its current list
        // and that that list is still whole
        this.right.isHead = this.isHead
        this.right.left = this.left
        this.left.right = this.right

        this.isHead = true
        this.right = this
        this.left = this
    }

    /**
     * Add this node before the given node.
     * If the given node was the head, then this node is now the head.
     *
     * @param node A location in the chain for insertion.
     */
    fun addBefore(node: ChainLink<E>) {
        this.isHead = node.isHead // This is head, iff node was
        this.right = node
        this.left = node.left

        node.isHead = false // node cannot now be head
        node.left.right = this
        node.left = this
    }

    /**
     * Add this node after the given node.
     * If the given node was the head, it still is.
     *
     * @param node A location in the chain for insertion.
     */
    fun addAfter(node: ChainLink<E>) {
        this.isHead = false // this can never be head
        this.left = node
        this.right = node.right

        node.right.left = this
        node.right = this
    }

    /**
     * Find the head of the chain.
     * Note, this is most efficient if this node is the head.
     *
     * @return the head of the chain.
     */
    fun findHead(): ChainLink<E> {
        var node: ChainLink<E> = this
        while (!node.isHead) {
            node = node.left
            if (this === node) {
                // We made it all away round and nothing was marked head.
                node.isHead = true
            }
        }
        return node
    }

    /**
     * Find the tail of the chain.
     * Note, this is most efficient if this node is the head.
     *
     * @return the tail of the chain.
     */
    fun findTail(): ChainLink<E> {
        return findHead().left
    }

    /**
     * Add this node as the head of the chain.
     * Note, this is most efficient if anyNode is the head of the chain.
     *
     * @param anyNode any node in the chain
     */
    fun addFirst(anyNode: ChainLink<E>) {
        val node = anyNode.findHead()
        addBefore(node)
    }

    /**
     * Add this node as the tail of the chain.
     * Note, this is most efficient if anyNode is the head of the chain.
     *
     * @param anyNode any node in the chain
     */
    fun addLast(anyNode: ChainLink<E>) {
        val node = anyNode.findHead()
        addAfter(node.left)
    }

    /**
     * Locate the node or the closest node in the chain using the comparator.
     * This assumes that the collection is ordered on some characteristic that
     * comparator assumes.
     *
     * @param element The node to look for
     * @param comparator The comparator that understands the ordering of the list
     * @return The node or the closest node.
     */
    fun locate(element: E, comparator: Comparator<E>): ChainLink<E> {
        // this node might be anywhere in the list.
        // compare it to this one
        var node: ChainLink<E> = this
        var cmp = comparator.compare(element, node.item)
        if (cmp < 0) {
            while (cmp < 0 && !node.isHead) {
                node = node.left
                cmp = comparator.compare(element, node.item)
                if (node === this) {
                    node.isHead = true
                }
            }
            return node
        } else if (cmp > 0) {
            do {
                node = node.right
                cmp = comparator.compare(element, node.item)
            } while (cmp > 0 && !node.isHead && node !== this)
            return node
        }
        return node
    }
}
