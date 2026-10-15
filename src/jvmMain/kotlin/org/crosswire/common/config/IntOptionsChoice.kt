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

import java.util.ResourceBundle
import java.util.TreeMap
import fi.sykero.kdom.Element

/**
 * A class to convert between strings and objects of a type.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class IntOptionsChoice : AbstractReflectedChoice(), MappedChoice<Int, String> {
    @Throws(StartupException::class)
    override fun init(option: Element, configResources: ResourceBundle) {
        super.init(option, configResources)

        val prefix = key + ".alternative."

        val alternatives = TreeMap<Int, String>()
        options = alternatives
        val iter = option.getChildren("alternative").iterator()
        while (iter.hasNext()) {
            val alternative = iter.next()
            val number = Integer.parseInt(alternative.getAttributeValue("number"))
            val name = configResources.getString(prefix + number)
            alternatives.put(Integer.valueOf(number), name)
        }
    }

    override fun getOptions(): Map<Int, String> {
        return TreeMap<Int, String>(options!!)
    }

    override val conversionClass: Class<Int>
        get() = Integer.TYPE

    override fun convertToString(orig: Any?): String? {
        return orig!!.toString()
    }

    override fun convertToObject(orig: String?): Any? {
        // First check to see if this is a number
        try {
            return Integer.valueOf(orig)
        } catch (ex: NumberFormatException) {
            for (mapEntry in options!!.entries) {
                if (mapEntry.value == orig) {
                    return mapEntry.key
                }
            }
            return Integer.valueOf(0)
        }
    }

    private var options: Map<Int, String>? = null
}
