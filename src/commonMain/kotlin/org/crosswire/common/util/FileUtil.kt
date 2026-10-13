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

import kotlin.jvm.JvmStatic


/**
 * .
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class FileUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Extension for java files
         */
        const val EXTENSION_JAVA: String = ".java"

        /**
         * Extension for properties files
         */
        const val EXTENSION_PROPERTIES: String = ".properties"

        /**
         * Extension for plug-in files
         */
        const val EXTENSION_PLUGIN: String = ".plugin"

        /**
         * Extension for XSLT files
         */
        const val EXTENSION_XSLT: String = ".xsl"

        /**
         * Extension for XML files
         */
        const val EXTENSION_XML: String = ".xml"

        /**
         * Modes for opening random access files
         */
        const val MODE_READ: String = "r"

        /**
         * Modes for opening random access files
         */
        const val MODE_WRITE: String = "rw"
    }
}
