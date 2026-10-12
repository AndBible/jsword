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

import org.crosswire.jsword.JSOtherMsg
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.MissingResourceException

/**
 * Better implementations of the getResource methods with less ambiguity and
 * that are less dependent on the specific class loader situation.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
// An object, not a companion: CallContext.getCallingClass() counts stack frames, so the @JvmStatic methods
// below must be the real static methods (a companion adds a delegating frame for Java callers).
object ResourceUtil {
    /**
     * Generic resource URL fetcher. One way or the other we'll find it! Either
     * as a relative or an absolute reference.
     *
     * @param search
     *            The name of the resource (without a leading /) to find
     * @return The requested resource
     * @throws MissingResourceException
     *             if the resource can not be found
     */
    @JvmStatic
    @Throws(MissingResourceException::class)
    fun getResource(search: String?): URL {
        return getResource(CallContext.getCallingClass(), search)
    }

    /**
     * Generic resource URL fetcher. One way or the other we'll find it! Either
     * as a relative or an absolute reference.
     *
     * @param <T> the type of the resource
     * @param clazz the basis to search for the resource first.
     * @param resourceName
     *            The resource to find
     * @return The requested resource
     * @throws MissingResourceException
     *             if the resource can not be found
     */
    @JvmStatic
    @Throws(MissingResourceException::class)
    fun <T> getResource(clazz: Class<T>, resourceName: String?): URL {
        val resource = CWClassLoader.instance(clazz).findResource(resourceName)

        if (resource == null) {
            throw MissingResourceException(JSOtherMsg.lookupText("Cannot find resource: {0}", resourceName), clazz.name, resourceName)
        }

        return resource
    }

    /**
     * Generic resource URL fetcher
     *
     * @param search
     *            The name of the resource (without a leading /) to find
     * @return The requested resource
     * @throws IOException
     *             if there is a problem reading the file
     * @throws MissingResourceException
     *             if the resource can not be found
     */
    @JvmStatic
    @Throws(IOException::class, MissingResourceException::class)
    fun getResourceAsStream(search: String?): InputStream {
        return getResourceAsStream(CallContext.getCallingClass(), search)
    }

    /**
     * Generic resource URL fetcher
     *
     * @param <T> the type of the resource
     * @param clazz the basis to search for the resource first.
     * @param search
     *            The name of the resource (without a leading /) to find
     * @return The requested resource
     * @throws IOException
     *             if there is a problem reading the file
     * @throws MissingResourceException
     *             if the resource can not be found
     */
    @JvmStatic
    @Throws(IOException::class, MissingResourceException::class)
    fun <T> getResourceAsStream(clazz: Class<T>, search: String?): InputStream {
        return ResourceUtil.getResource(clazz, search).openStream()
    }

    /**
     * Get and load a properties file from the writable area or if that fails
     * from the classpath (where a default ought to be stored)
     *
     * @param subject
     *            The name of the desired resource (without any extension)
     * @return The found and loaded properties file
     * @throws IOException
     *             if the resource can not be loaded
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getProperties(subject: String?): PropertyMap {
        return getProperties(CallContext.getCallingClass(), subject)
    }

    /**
     * Get and load a properties file from the writable area or if that fails
     * from the classpath (where a default ought to be stored)
     *
     * @param <T> the type of the resource
     * @param clazz
     *            The name of the desired resource
     * @return The found and loaded properties file
     * @throws IOException
     *             if the resource can not be loaded
     */
    @JvmStatic
    @Throws(IOException::class)
    fun <T> getProperties(clazz: Class<T>): PropertyMap {
        return getProperties(clazz, ClassUtil.getShortClassName(clazz))
    }

    /**
     * Get and load a properties file from the writable area or if that fails
     * from the classpath (where a default ought to be stored)
     *
     * @param <T> the type of the resource
     * @param clazz
     *            The name of the desired resource
     * @param subject
     *            The name of the desired resource (without any extension)
     * @return The found and loaded properties file
     * @throws IOException
     *             if the resource can not be loaded
     */
    @Throws(IOException::class)
    private fun <T> getProperties(clazz: Class<T>, subject: String?): PropertyMap {
        try {
            val lookup = subject + FileUtil.EXTENSION_PROPERTIES
            val `in` = getResourceAsStream(clazz, lookup)

            val prop = PropertyMap()
            PropertyMapJvm.load(prop, `in`!!)
            return prop
        } catch (e: MissingResourceException) {
            return PropertyMap()
        }
    }
}
