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
 *       http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.common.config

import org.crosswire.common.util.ClassUtil
import org.slf4j.LoggerFactory

/**
 * A class to convert between strings and objects of a type.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ClassChoice : AbstractReflectedChoice() {
    override val conversionClass: Class<*>
        get() = Class::class.java

    override fun convertToString(orig: Any?): String? {
        if (orig == null) {
            return null
        }

        return (orig as Class<*>).name
    }

    override fun convertToObject(orig: String?): Any? {
        try {
            return ClassUtil.forName(orig)
        } catch (ex: ClassNotFoundException) {
            log.warn("Class not found: {}", orig, ex)
            return null
        }
    }

    private companion object {
        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(ClassChoice::class.java)
    }
}
