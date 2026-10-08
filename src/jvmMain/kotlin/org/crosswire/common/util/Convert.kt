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

import org.slf4j.LoggerFactory

/**
 * Conversions between various types and Strings.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class Convert
/**
 * We don't want anyone doing this ...
 */
private constructor() {
    companion object {
        /**
         * Convert a String to a boolean
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun string2Boolean(data: String?): Boolean {
            return java.lang.Boolean.parseBoolean(data)
                    || "yes".equals(data, ignoreCase = true)
                    || "ok".equals(data, ignoreCase = true)
                    || "okay".equals(data, ignoreCase = true)
                    || "on".equals(data, ignoreCase = true)
                    || "1" == data
        }

        /**
         * Convert a boolean to a String
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun boolean2String(data: Boolean): String {
            return data.toString()
        }

        /**
         * Convert a String to an int
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun string2Int(data: String?): Int {
            try {
                return java.lang.Integer.parseInt(data)
            } catch (ex: NumberFormatException) {
                return 0
            }
        }

        /**
         * Convert an int to a String
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun int2String(data: Int): String {
            return data.toString()
        }

        /**
         * Convert a String to an Object
         *
         * @param data the thing to convert
         * @return the converted data
         * @throws ReflectiveOperationException
         *               if this {@code data} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no reachable nullary constructor;
         *               or if the instantiation fails for some other reason.
         */
        @JvmStatic
        @Throws(ReflectiveOperationException::class)
        fun string2Object(data: String?): Any {
            return ReflectionUtil.construct<Any>(data)
        }

        /**
         * Convert an Object to a String
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun object2String(data: Any): String {
            return data.javaClass.name
        }

        /**
         * Convert a String to a Map, without type checking
         *
         * @param data
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun string2Map(data: String?): PropertyMap {
            val commands = PropertyMap()

            val parts = StringUtil.split(data, " ")
            var entry = ""
            for (i in parts.indices) {
                try {
                    entry = parts[i]
                    val pos = entry.indexOf('=')
                    val key = entry.substring(0, pos)
                    val value = entry.substring(pos + 1)
                    val clazz = ClassUtil.forName(value)

                    if (clazz.isAssignableFrom(Any::class.java)) {
                        jswordAssert(false) { "" }
                    } else {
                        commands.put(key, value)
                    }
                } catch (ex: ClassNotFoundException) {
                    log.warn("Invalid config file entry: {} System message: {}", entry, ex.message)
                    Reporter.informUser(Convert::class.java, ex)
                }
            }

            return commands
        }

        /**
         * Convert a Map to a Sting
         *
         * @param commands
         *            the thing to convert
         * @return the converted data
         */
        @JvmStatic
        fun map2String(commands: Map<out Any?, out Any?>): String {
            val retcode = StringBuilder()
            for (entry in commands.entries) {
                retcode.append(entry.key)
                retcode.append('=')
                retcode.append(entry.value)
                retcode.append(' ')
            }

            // Java String.trim() strips everything up to U+0020, unlike Kotlin trim()
            return retcode.toString().trim { it <= ' ' }
        }

        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(Convert::class.java)
    }
}
