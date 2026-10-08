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
package org.crosswire.common.util

import java.io.IOException
import java.net.URI
import java.util.ArrayList
import java.util.HashSet
import java.util.Properties

/**
 * Some utils to help work with Collections.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class CollectionUtil
/**
 * Dont do this
 */
private constructor() {
    companion object {
        /**
         * Create a List from an Iterable.
         *
         * @param T The type of data for the list
         * @param it  The source of data for the list
         * @return List
         */
        @JvmStatic
        fun <T> createList(it: Iterable<T>): MutableList<T> {
            val reply: MutableList<T> = ArrayList()
            for (obj in it) {
                reply.add(obj)
            }
            return reply
        }

        /**
         * Create a Set from an Iterable.
         *
         * @param T The type of data for the list
         * @param it  The source of data for the list
         * @return the created set
         */
        @JvmStatic
        fun <T> createSet(it: Iterable<T>): MutableSet<T> {
            val reply: MutableSet<T> = HashSet()
            for (obj in it) {
                reply.add(obj)
            }
            return reply
        }

        /**
         * Convert a `Properties` into a `Map`.
         *
         * @param prop The Properties to convert
         * @return The map
         */
        @JvmStatic
        fun properties2Map(prop: Properties): PropertyMap {
            val propMap = PropertyMap()
            val e = prop.keys()
            while (e.hasMoreElements()) {
                val k = e.nextElement()
                val v = prop.get(k)
                if (k is String && v is String) {
                    propMap.put(k, v)
                }
            }
            return propMap
        }

        /**
         * Convert a `Properties` located at `propURI` into a
         * `Map`.
         *
         * @param propUri
         *            The URI of the Properties to convert
         * @return The map
         * @throws IOException If there is a problem with getting the properties from the URI
         */
        @JvmStatic
        @Throws(IOException::class)
        fun properties2Map(propUri: URI): PropertyMap {
            return NetUtil.loadProperties(propUri)
        }
    }
}
