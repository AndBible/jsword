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

import java.util.EventObject

import org.crosswire.common.icu.NumberShaper

/**
 * An event indicating that some bit of data needs capturing.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ReporterEvent private constructor(source: Any?, private var ex: Throwable?, message: String?) : EventObject(source) {
    /**
     * Constructs an CaptureEvent object.
     *
     * @param source
     *            The event originator (typically `this`)
     * @param ex
     *            An exception
     */
    constructor(source: Any?, ex: Throwable?) : this(source, ex, null)

    /**
     * Constructs an CaptureEvent object.
     *
     * @param source
     *            The event originator (typically `this`)
     * @param message
     *            An message to log
     */
    constructor(source: Any?, message: String?) : this(source, null, NumberShaper().shape(message))

    /**
     * Returns a string specifying the source of the message.
     *
     * @return The Source as a String
     */
    open val sourceName: String
        get() {
            val src = getSource()
            val clazz: Class<*> = if (src is Class<*>) {
                src
            } else {
                src.javaClass
            }
            val full = clazz.name
            val lastDot = full.lastIndexOf('.')
            if (lastDot == -1) {
                return full
            }
            return full.substring(lastDot + 1)
        }

    /**
     * Returns the exception.
     *
     * @return the Exception
     */
    open val exception: Throwable?
        get() = ex

    /**
     * Returns the message.
     *
     * @return the message
     */
    open val message: String? = message

    private companion object {
        /**
         * Serialization ID
         */
        private const val serialVersionUID = 4121978048640988213L
    }
}
