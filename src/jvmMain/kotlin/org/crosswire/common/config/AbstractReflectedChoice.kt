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

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.crosswire.common.util.ClassUtil
import org.crosswire.common.util.StringUtil
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSOtherMsg
import org.jdom2.Element
import org.crosswire.common.util.LoggerFactory

/**
 * A helper for when we need to be a choice created dynamically.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
abstract class AbstractReflectedChoice : Choice {
    /**
     * Initialise the choice from its element of config.xml; see [Choice.init].
     */
    @Throws(StartupException::class)
    override fun init(option: Element, configResources: ResourceBundle) {
        keyField = option.getAttributeValue("key")

        // Hidden is an optional field so it is ok for the resource to be
        // missing.
        try {
            val hiddenState = configResources.getString(key + ".hidden")
            hidden = java.lang.Boolean.parseBoolean(hiddenState)
        } catch (e: MissingResourceException) {
            hidden = false
        }

        // Ignore is an optional field so it is ok for the resource to be
        // missing.
        try {
            val ignoreState = configResources.getString(key + ".ignore")
            ignored = java.lang.Boolean.parseBoolean(ignoreState)
            if (ignored) {
                hidden = true
                return
            }
        } catch (e: MissingResourceException) {
            ignored = false
        }

        val help = configResources.getString(key + ".help")
        jswordAssert(help != null) { "" }
        helpText = help

        // OPTIMIZE(dms): This is poorly done (by me!)
        val pathParts = StringUtil.split(key, '.')
        val parentKey = StringBuilder()
        val path = StringBuilder()
        for (i in pathParts.indices) {
            if (i > 0) {
                parentKey.append('.')
                path.append('.')
            }
            parentKey.append(pathParts[i])
            val parent = configResources.getString(parentKey.toString() + ".name")
            jswordAssert(parent != null) { "" }
            path.append(parent)
        }
        fullPath = path.toString()

        external = java.lang.Boolean.parseBoolean(option.getAttributeValue("external"))

        restart = java.lang.Boolean.parseBoolean(option.getAttributeValue("restart"))

        typeField = option.getAttributeValue("type")

        // The important 3 things saying what we update and how we describe
        // ourselves
        val introspector = option.getChild("introspect")
                ?: throw StartupException(JSOtherMsg.lookupText("Missing {0} element in config.xml", "introspect"))

        val clazzname = introspector.getAttributeValue("class")
                ?: throw StartupException(JSOtherMsg.lookupText("Missing {0} element in config.xml", "class"))

        val property = introspector.getAttributeValue("property")
                ?: throw StartupException(JSOtherMsg.lookupText("Missing {0} element in config.xml", "property"))
        propertyname = property

        // log.debug("Looking up {}.set{}({} arg0)", clazzname, propertyname, getConvertionClass().getName());

        val reflected: Class<*>
        try {
            reflected = ClassUtil.forName(clazzname)
        } catch (ex: ClassNotFoundException) {
            throw StartupException(JSOtherMsg.lookupText("Specified class not found: {0}", clazzname), ex)
        }
        clazz = reflected

        try {
            setter = reflected.getMethod("set" + property, conversionClass)
        } catch (ex: NoSuchMethodException) {
            throw StartupException(JSOtherMsg.lookupText("Specified method not found {0}.set{1}({2} arg0)",
                    reflected.name, property, conversionClass.name), ex)
        }

        try {
            try {
                getter = reflected.getMethod("is" + property)
            } catch (e: NoSuchMethodException) {
                getter = reflected.getMethod("get" + property)
            }
        } catch (ex: NoSuchMethodException) {
            throw StartupException(JSOtherMsg.lookupText("Specified method not found {0}.get{1}()", reflected.name, property), ex)
        }

        val returnType = getter!!.returnType
        if (returnType !== conversionClass) {
            log.debug("Not using {} from {} because the return type of the getter is not {}", property, reflected.name, conversionClass.name)
            throw StartupException(JSOtherMsg.lookupText("Mismatch of return types, found: {0} required: {1}", returnType, conversionClass))
        }
    }

    override val key: String?
        get() = keyField

    override val type: String?
        get() = typeField

    /**
     * Convert from a reflection return value to a String for storage
     * 
     * @param orig the object to be converted to a string
     * @return the marshaled representation of the object
     */
    abstract fun convertToString(orig: Any?): String?

    /**
     * Convert from a stored string to an object to use with reflection
     * @param orig the marshaled representation of the object
     * @return the reconstituted object
     */
    abstract fun convertToObject(orig: String?): Any?

    override var fullPath: String? = null

    override var helpText: String? = null

    override val isSaveable: Boolean
        get() = !external

    override val isHidden: Boolean
        get() = hidden

    override val isIgnored: Boolean
        get() = ignored

    override fun requiresRestart(): Boolean {
        return restart
    }

    override var string: String?
        get() {
            try {
                val retval = getter!!.invoke(null)
                return convertToString(retval)
            } catch (ex: IllegalAccessException) {
                log.error("Illegal access getting value from {}.{}", clazz!!.name, getter!!.name, ex)
                return ""
            } catch (ex: InvocationTargetException) {
                log.error("Failed to get value from {}.{}", clazz!!.name, getter!!.name, ex)
                return ""
            }
        }
        @Throws(ConfigException::class)
        set(value) {
            var ex: Exception? = null
            try {
                val obj = convertToObject(value)
                if (obj != null) {
                    setter!!.invoke(null, obj)
                }
            } catch (e: InvocationTargetException) {
                ex = e
            } catch (e: IllegalArgumentException) {
                ex = e
            } catch (e: IllegalAccessException) {
                ex = e
            } catch (e: NullPointerException) {
                ex = e
            }

            if (ex != null) {
                log.info("Exception while attempting to execute: {}", setter!!.toString())

                // So we can't re-throw the original exception because it wasn't an
                // Exception so we will have to re-throw the
                // InvocationTargetException
                throw ConfigException(JSOtherMsg.lookupText("Failed to set option: {0}", setter), ex)
            }
        }

    /**
     * The key of the option.
     */
    private var keyField: String? = null

    /**
     * The type that we reflect to
     */
    private var clazz: Class<*>? = null

    /**
     * The property that we call on the reflecting class
     */
    private var propertyname: String? = null

    /**
     * The type (as specified in config.xml)
     */
    private var typeField: String? = null

    /**
     * The method to call to get the value
     */
    private var getter: Method? = null

    /**
     * The method to call to set the value
     */
    private var setter: Method? = null

    /**
     * Whether this choice should be visible or hidden
     */
    private var hidden = false

    /**
     * Whether this choice should be ignored altogether.
     */
    private var ignored = false

    /**
     * Whether this choice is managed externally, via setXXX and getXXX.
     */
    private var external = false

    /**
     * Whether this choice is requires a restart to be seen.
     */
    private var restart = false

    private companion object {
        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(AbstractReflectedChoice::class)
    }
}
