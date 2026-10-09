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

import java.util.NoSuchElementException

/**
 * This KeyIterator performs a depth first iteration over the subkeys in the
 * key.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class KeyIterator(key: Key?) : MutableIterator<Key> {
    private val stack: ArrayDeque<Locator> = ArrayDeque()

    init {
        stack.addLast(Locator(key))
    }

    protected open fun prepare() {
        // If there is nothing on the stack we have nothing to do.
        if (stack.size == 0) {
            return
        }

        // Check to see if there are more children to process
        val peek = stack.last()

        if (peek.parent!!.getChildCount() > peek.position) {
            return
        }

        // There are no more so we are done with this Locator.
        stack.removeLast()

        // Try the next
        prepare()
    }

    override fun hasNext(): Boolean {
        prepare()
        return stack.size != 0
    }

    override fun next(): Key {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val peek = stack.last()

        // Determine which child in the list of children to consider
        val childNum = peek.position

        // Advance to the next potential child
        peek.position = childNum + 1

        // If we have exhausted all the children,
        // then return the parent key
        if (childNum == -1) {
            return peek.parent!!
        }

        stack.addLast(Locator(peek.parent!!.get(childNum)))

        return next()
    }

    override fun remove() {
        throw UnsupportedOperationException()
    }

    /**
     * A helper class that remembers where we've been and where we are.
     */
    open class Locator(
        /**
         * the parent
         */
        open var parent: Key?
    ) {
        /**
         * the position
         */
        open var position: Int = -1
    }
}
