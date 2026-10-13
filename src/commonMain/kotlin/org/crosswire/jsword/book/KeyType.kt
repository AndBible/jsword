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
package org.crosswire.jsword.book

import kotlin.jvm.JvmStatic

import org.crosswire.common.util.jswordAssert

/**
 * Types of Key that a Book uses, either verse, list, or tree.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class KeyType {
    /**
     * Book contains verses and can be understood as book, chapter and verse.
     */
    VERSE,

    /**
     * Book organizes its entries in a list, as in a dictionary.
     */
    LIST,

    /**
     * Book organizes its entries in a tree, as in a general book.
     */
    TREE;

    /**
     * Get an integer representation for this KeyType
     *
     * @return the ordinal of a KeyType
     */
    fun toInteger(): Int {
        return ordinal
    }

    companion object {
        /**
         * Lookup method to convert from a String
         *
         * @param name the name of a KeyType
         * @return the matching KeyType
         */
        @JvmStatic
        fun fromString(name: String?): KeyType? {
            for (v in values()) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }
    }
}
