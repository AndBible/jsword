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
import org.crosswire.common.util.ClassUtil
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.ReflectionUtil
import org.jdom2.Element

/**
 * Factory for the well known Choices.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class ChoiceFactory
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Get a ChoiceFactory by element.
         * 
         * @param option
         *            The element to check
         * @param configResources the resource bundle holding the option
         * @return One of the ChoiceTypes.
         * @throws StartupException if startup is not possible
         * @throws ReflectiveOperationException 
         *               if this {@code data} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no reachable nullary constructor;
         *               or if the instantiation fails for some other reason.
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Throws(StartupException::class, ReflectiveOperationException::class)
        fun getChoice(option: Element, configResources: ResourceBundle): Choice {
            val clazz: Class<Choice>?

            val type = option.getAttributeValue("type")
            if ("custom" == type) {
                val clazzstr = option.getAttributeValue("class")
                clazz = ClassUtil.forName(clazzstr) as Class<Choice>
            } else {
                clazz = if (type == null) null else map.get(type)
            }

            val choice = ReflectionUtil.construct(clazz)
            choice.init(option, configResources)
            return choice
        }

        /**
         * Method getDataMap.
         * 
         * @return the map data
         */
        @JvmStatic
        fun getDataMap(): MutableMap<String, Any?> {
            return datamap
        }

        /**
         * Storage of various registered objects
         */
        private val datamap: MutableMap<String, Any?> = HashMap()

        /**
         * Store of the known ChoiceTypes
         */
        @Suppress("UNCHECKED_CAST")
        private val map: MutableMap<String, Class<Choice>> = PluginRegistry.entries(Choice::class).associateTo(LinkedHashMap()) { it.name to (it.implClass.java as Class<Choice>) }
    }
}
