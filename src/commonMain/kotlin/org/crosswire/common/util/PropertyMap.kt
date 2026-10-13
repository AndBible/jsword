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

/**
 * A PropertyMap is a Map&lt;String,String&gt; sitting over top a Property file.
 * As such it must be defined in the same way as a java.util.Properties expects.
 *
 * @see java.util.Properties
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class PropertyMap(
    /**
     * Default values for any keys not found in this property map.
     */
    private var defaults: PropertyMap?
) : PropertyMapSuperclass<String, String>() {
    /**
     * Creates an empty property list with no default values.
     */
    constructor() : this(null)

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns
     * <code>null</code> if the property is not found.
     *
     * @param   key   the lookup key.
     * @return  the value in this property list with the specified key value.
     * @see     java.util.Properties#setProperty
     */
    override fun get(key: String): String? {
        val value = super.get(key)
        val d = defaults
        return if ((value == null) && (d != null)) d.get(key) else value
    }

    /**
     * Searches for the property with the specified key in this property list.
     * If the key is not found in this property list, the default property list,
     * and its defaults, recursively, are then checked. The method returns the
     * default value argument if the property is not found.
     *
     * @param   key            the lookup key.
     * @param   defaultValue   a default value.
     *
     * @return  the value in this property list with the specified key value.
     * @see     java.util.Properties#setProperty
     */
    open fun get(key: String, defaultValue: String?): String? {
        val value = get(key)
        return value ?: defaultValue
    }

    /** Loads Latin-1 properties, retaining the legacy JVM enumeration order. */
    open fun load(bytes: ByteArray) {
        for ((key, value) in legacyPropertyEntries(bytes)) put(key, value)
    }

    /** Serializes explicit entries only, including the legacy timestamp and escaping. */
    open fun store(comments: String?): ByteArray = legacyPropertyBytes(this, comments)

}
