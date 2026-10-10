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

import org.crosswire.jsword.JSMsg

/**
 * A LucidException adds 2 concepts to a base Exception, that of a wrapped
 * Exception, that of internationalized (i18n) messages.
 *
 * The first addition is the concept of an optional wrapped Exception (actually
 * a Throwable), which describes what caused this to happen. Any well defined
 * interface will define the exact exceptions that the methods of that interface
 * will throw, and not leave it to the ambiguous "throws Exception". However the
 * interface should have no idea how it will be implemented and so the details
 * of exactly what broke under the covers gets lost. With LucidException this
 * detail is kept in the wrapped Exception. This functionality has been added to
 * the base Exception class in J2SE 1.4
 *
 * The second addition is the concept of i18n messages. Normal Exceptions are
 * created with an almost random string in the message field, LucidExceptions
 * define this string to be a key into a resource bundle, and to help formatting
 * this string there is an optional Object array of format options. There is a
 * constructor that allows us to specify no i18n lookup, which is useful if this
 * lookup may have been done already.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @see LucidRuntimeException
 */
open class LucidException : Exception {
    /**
     * All LucidExceptions are constructed with references to resources in an
     * i18n properties file.
     *
     * @param msg
     *            The resource id to read
     */
    constructor(msg: String?) : super(msg)

    /**
     * All LucidExceptions are constructed with references to resources in an
     * i18n properties file.
     *
     * @param msg   The resource id to read
     * @param cause The cause of the exception
     */
    constructor(msg: String?, cause: Throwable?) : super(msg, cause)

    /**
     * Accessor of the full detailed version of the string
     *
     * @return The full unraveled i18n string
     */
    open val detailedMessage: String?
        get() {
            val cause = cause ?: return message

            // TRANSLATOR: When an error occurs this label precedes the details of the problem.
            val reason = JSMsg.gettext("Reason:")
            if (cause is LucidException) {
                return message + reason + cause.detailedMessage
            }
            return message + reason + cause.message
        }

}
