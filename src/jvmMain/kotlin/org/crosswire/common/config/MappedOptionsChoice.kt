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
import org.crosswire.jsword.JSOtherMsg
import fi.sykero.kdom.Element
import org.crosswire.common.util.LoggerFactory

/**
 * A class to convert between strings and objects of a type.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class MappedOptionsChoice : AbstractReflectedChoice(), MappedChoice<Any?, Any?> {
    @Throws(StartupException::class)
    override fun init(option: Element, configResources: ResourceBundle) {
        super.init(option, configResources)
        val mapElement = option.getChild("map")
                ?: throw StartupException(JSOtherMsg.lookupText("Missing {0} element in config.xml", "map"))

        val name = mapElement.getAttributeValue("name")
        val map = ChoiceFactory.getDataMap().get(name)
        if (map is Map<*, *>) {
            options = map
        } else {
            options = TreeMap<Any?, Any?>()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getOptions(): Map<Any?, Any?> {
        return TreeMap<Any?, Any?>(options as Map<Any?, Any?>)
    }

    override val conversionClass: Class<String>
        get() = String::class.java

    override fun convertToString(orig: Any?): String? {
        return if (orig != null) orig.toString() else ""
    }

    override fun convertToObject(orig: String?): Any? {
        val iter = options!!.entries.iterator()
        var mapEntry: Map.Entry<*, *>? = null
        while (iter.hasNext()) {
            mapEntry = iter.next()
            if (mapEntry.value!!.toString() == orig || mapEntry.key!!.toString() == orig) {
                return mapEntry.key!!.toString()
            }
        }
        log.warn(JSOtherMsg.lookupText("Ignoring invalid option: {0}", orig))
        return ""
    }

    private var options: Map<*, *>? = null

    private companion object {
        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(MappedOptionsChoice::class)
    }
}
