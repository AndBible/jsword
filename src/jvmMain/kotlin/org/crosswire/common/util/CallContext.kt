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

/**
 * This singleton class provides a way for a method to determine which class
 * called it.
 * <p>
 * It has been tested to work in command line and WebStart environments.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
// An object, not a companion: getCallingClass() counts stack frames (CALL_CONTEXT_OFFSET), so the @JvmStatic
// methods below must be the real static methods, with no delegating companion frame in between.
object CallContext {
    /**
     * Singleton accessor
     *
     * @return this singleton
     */
    @JvmStatic
    fun instance(): CallContext {
        return this
    }

    /**
     * When called from a method it will return the class calling that method.
     *
     * @return the immediate calling class
     */
    @JvmStatic
    fun getCallingClass(): Class<*> {
        return getCallingClass(1) // add 1 for this method
    }

    /**
     * When called from a method it will return the i-th class calling that
     * method, up the call chain. If used with a -1 it will return the class
     * making the call -2 and -3 will return this class
     *
     * @param i the i-th coller
     * @return the desired calling class
     * @throws ArrayIndexOutOfBoundsException
     *             if the index is not valid
     */
    @JvmStatic
    fun getCallingClass(i: Int): Class<*> {
        try {
            return Class.forName(Thread.currentThread().stackTrace[CALL_CONTEXT_OFFSET + i].className)
        } catch (e: ClassNotFoundException) {
            return CallContext::class.java
        }
    }

    // may need to change if this class is redesigned
    /**
     * Offset needed to represent the caller of the method that called this
     * method.
     *
     */
    private const val CALL_CONTEXT_OFFSET = 3
}
