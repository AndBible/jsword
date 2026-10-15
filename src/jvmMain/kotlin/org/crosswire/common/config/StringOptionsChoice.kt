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

import java.util.ResourceBundle
import org.crosswire.jsword.JSOtherMsg
import fi.sykero.kdom.Element

/**
 * A class to convert between strings and objects of a type.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class StringOptionsChoice : AbstractReflectedChoice(), MultipleChoice {
    @Suppress("UNCHECKED_CAST")
    @Throws(StartupException::class)
    override fun init(option: Element, configResources: ResourceBundle) {
        super.init(option, configResources)
        val map = option.getChild("map")
                ?: throw StartupException(JSOtherMsg.lookupText("Missing {0} element in config.xml", "map"))

        val name = map.getAttributeValue("name")
        array = ChoiceFactory.getDataMap().get(name) as Array<String>?
    }

    @Suppress("UNCHECKED_CAST")
    override fun getOptions(): Array<String> {
        val copy = arrayOfNulls<String>(array!!.size)
        System.arraycopy(array!!, 0, copy, 0, array!!.size)
        return copy as Array<String>
    }

    override val conversionClass: Class<String>
        get() = String::class.java

    override fun convertToString(orig: Any?): String? {
        return orig as String?
    }

    override fun convertToObject(orig: String?): Any? {
        return orig
    }

    private var array: Array<String>? = null
}
