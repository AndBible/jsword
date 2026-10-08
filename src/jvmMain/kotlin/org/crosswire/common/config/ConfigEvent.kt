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

import java.io.IOException
import java.io.ObjectInputStream
import java.util.EventObject

/**
 * An event indicating that an exception has happened.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ConfigEvent
/**
 * Constructs an ConfigEvent object.
 * 
 * @param source
 *            The event originator, or log stream
 * @param key the key for the config item
 * @param model the config item
 */
(source: Any?, key: String?, model: Choice?) : EventObject(source) {
    // The getters stay functions: the private fields have to stay non-final as in the serialized form.
    private var key: String? = key

    /**
     * The config item, not serialized.
     */
    @Transient
    private var model: Choice? = model

    /**
     * Returns the key.
     * 
     * @return the key
     */
    open fun getKey(): String? {
        return key
    }

    /**
     * Returns the choice.
     * 
     * @return the choice
     */
    open fun getChoice(): Choice? {
        return model
    }

    /**
     * Returns the choice.
     * 
     * @return the choice
     */
    open fun getPath(): Choice? {
        return model
    }

    /**
     * Serialization support.
     * 
     * @param is the input stream
     * @throws  IOException if an I/O error occurs.
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(`is`: ObjectInputStream) {
        // Broken but we don't serialize events
        model = null
        `is`.defaultReadObject()
    }

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = 3257006561900376375L
    }
}
