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

import java.util.Comparator
import java.util.Locale

/**
 * Provides different ways to sort Books.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BookComparators
/**
 * Ensure we can't be created
 */
private constructor() {
    companion object {
        /**
         * Order by default Book ordering
         *
         * @return the desired Comparator
         */
        @JvmStatic
        fun getDefault(): Comparator<Book> {
            return object : Comparator<Book> {
                override fun compare(o1: Book, o2: Book): Int {
                    return o1.compareTo(o2)
                }
            }
        }

        /**
         * Order by Initials.
         *
         * @return the desired Comparator
         */
        @JvmStatic
        fun getInitialComparator(): Comparator<Book> {
            return object : Comparator<Book> {
                override fun compare(o1: Book, o2: Book): Int {
                    return o1.initials.uppercase(Locale.ENGLISH).compareTo(o2.initials.uppercase(Locale.ENGLISH))
                }
            }
        }
    }
}
