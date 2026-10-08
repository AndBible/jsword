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
package org.crosswire.common.config

import java.io.File
import org.crosswire.common.util.StringUtil

/**
 * A class to convert between strings and objects of a type.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class PathChoice : AbstractReflectedChoice() {
    override val conversionClass: Class<Array<File>>
        get() = Array<File>::class.java

    @Suppress("UNCHECKED_CAST")
    override fun convertToString(orig: Any?): String? {
        val paths = orig as Array<File>
        val names = arrayOfNulls<String>(paths.size)
        for (i in paths.indices) {
            names[i] = paths[i].absolutePath
        }

        return StringUtil.join(names, File.pathSeparator)
    }

    override fun convertToObject(orig: String?): Any? {
        val names = StringUtil.split(orig, File.pathSeparator)
        val paths = arrayOfNulls<File>(names.size)
        for (i in names.indices) {
            paths[i] = File(names[i])
        }

        return paths
    }
}
