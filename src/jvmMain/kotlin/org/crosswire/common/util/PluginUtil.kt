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
 * © CrossWire Bible Society, 2008 - 2016
 *
 */
package org.crosswire.common.util

import java.io.IOException
import java.net.MalformedURLException
import java.util.MissingResourceException
import kotlin.reflect.KClass

/**
 * Compatibility shim over [PluginRegistry] (class mappings) plus the `.plugin` data
 * file reader ([getPlugin]). A plugin maps one or more implementations to an interface or abstract class. When there is more than one
 * implementation, one is marked as a default.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class PluginUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Get the known implementors of some interface or abstract class. This is
         * currently done by looking up a plugin file by the name of the given
         * class, and assuming that values are implementors of said class. Those
         * that are not are warned, but ignored.
         *
         * @param <T> the implementor's type
         * @param clazz
         *            The class or interface to find implementors of.
         * @return The list of implementing classes.
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> getImplementors(clazz: Class<T>): Array<Class<T>> {
            val matches = PluginRegistry.entries(kclass(clazz)).map { it.implClass.java as Class<T> }
            log.debug("Found {} implementors of {}", Integer.toString(matches.size), clazz.name)
            return matches.toTypedArray()
        }

        /**
         * Get a map of known implementors of some interface or abstract class. This
         * is currently done by looking up a plugins file by the name of the given
         * class, and assuming that values are implementors of said class. Those
         * that are not are warned, but ignored. The reply is in the form of a map
         * of keys=strings, and values=classes in case you need to get at the names
         * given to the classes in the plugin file.
         *
         * @param <T> the implementor's type
         * @param clazz
         *            The class or interface to find implementors of.
         * @return The map of implementing classes.
         * @see PluginUtil#getImplementors(Class)
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> getImplementorsMap(clazz: Class<T>): MutableMap<String, Class<T>> {
            val matches: MutableMap<String, Class<T>> = LinkedHashMap()
            for (entry in PluginRegistry.entries(kclass(clazz))) {
                matches[entry.name] = entry.implClass.java as Class<T>
            }
            log.debug("Found {} implementors of {}", Integer.toString(matches.size), clazz.name)
            return matches
        }

        /**
         * Get the preferred implementor of some interface or abstract class. This
         * is currently done by looking up a plugins file by the name of the given
         * class, and assuming that the "default" key is an implementation of said
         * class. Warnings are given otherwise.
         *
         * @param <T> the implementor's type
         * @param clazz
         *            The class or interface to find an implementation of.
         * @return The configured implementing class.
         * @throws MalformedURLException
         *             if the plugin file can not be found
         * @throws IOException
         *             if there is a problem reading the found file
         * @throws ClassNotFoundException
         *             if the read contents are not found
         * @see PluginUtil#getImplementors(Class)
         */
        @JvmStatic
        @Throws(IOException::class, ClassNotFoundException::class)
        @Suppress("UNCHECKED_CAST")
        fun <T> getImplementor(clazz: Class<T>): Class<T> {
            val entry = PluginRegistry.default(kclass(clazz))
                ?: throw ClassNotFoundException(clazz.name)
            return entry.implClass.java as Class<T>
        }

        /**
         * Get and instantiate the preferred implementor of some interface or
         * abstract class.
         *
         * @param <T> the implementor's type
         * @param clazz
         *            The class or interface to find an implementation of.
         * @return The configured implementing class.
         * @throws MalformedURLException
         *             if the plugin file can not be found
         * @throws IOException
         *             if there is a problem reading the found file
         * @throws ReflectiveOperationException
         *               if this {@code data} represents an abstract class,
         *               an interface, an array class, a primitive type, or void;
         *               or if the class has no reachable nullary constructor;
         *               or if the instantiation fails for some other reason.

         * @see PluginUtil#getImplementors(Class)
         */
        @JvmStatic
        @Throws(MalformedURLException::class, IOException::class, ReflectiveOperationException::class)
        fun <T> getImplementation(clazz: Class<T>): T {
            val entry = PluginRegistry.default(kclass(clazz))
                ?: throw ClassNotFoundException(clazz.name)
            @Suppress("UNCHECKED_CAST")
            return entry.create() as T
        }

        /**
         * Get and load a plugin file by looking it up as a resource.
         *
         * @param <T> the implementor's type
         * @param clazz
         *            The name of the desired resource
         * @return The found and loaded plugin file
         * @throws IOException
         *             if the resource can not be loaded
         * @throws MissingResourceException
         *             if the resource can not be found
         *
         * Note: this reads the `.plugin` file from the classpath. The embedded resources (including
         * `InstallManager.plugin`) are no longer on the main classpath, so for such classes this silently returns an
         * empty map; read them through [KswordResources] instead.
         */
        @Deprecated("Reads the classpath, where the embedded .plugin files no longer live; use KswordResources.")
        @JvmStatic
        @Throws(IOException::class)
        fun <T> getPlugin(clazz: Class<T>): PropertyMap {
            val subject = ClassUtil.getShortClassName(clazz)

            try {
                val lookup = subject + EXTENSION_PLUGIN
                val `in` = ResourceUtil.getResourceAsStream(clazz, lookup)

                val prop = PropertyMap()
                PropertyMapJvm.load(prop, `in`!!)
                return prop
            } catch (e: MissingResourceException) {
                return PropertyMap()
            }
        }

        /**
         * Extension for properties files
         */
        const val EXTENSION_PLUGIN: String = ".plugin"

        @Suppress("UNCHECKED_CAST")
        private fun <T> kclass(clazz: Class<T>): KClass<Any> = (clazz as Class<Any>).kotlin

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(PluginUtil::class)
    }
}
