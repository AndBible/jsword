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
package org.crosswire.jsword.book

import kotlinx.atomicfu.atomic
import org.crosswire.jsword.passage.Key
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * When we can't convert some source data then the user doesn't really care and
 * just wants it to work, but it would be good to have some way to get the
 * problems fixed, so as a start point we report them through this class.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
actual class DataPolice
/**
 * Prevent instantiation
 */
private constructor() {
    actual companion object {
        /**
         * Report a message against the current book and key.
         * 
         * @param book
         *            the book against which to report
         * @param key
         *            the key against which to report
         * @param message
         *            the police report.
         */
        @JvmStatic
        actual fun report(book: Book?, key: Key?, message: String?) {
            if (!reporting.value) {
                return
            }
            val buf = StringBuilder()
            val bmd: BookMetaData? = book!!.bookMetaData
            if (bmd != null) {
                buf.append(bmd.initials)
            }
            if (bmd != null && key != null) {
                buf.append(':')
            }
            if (key != null) {
                buf.append(key.getOsisID())
            }
            buf.append(": ")
            buf.append(message)
            LOGGER.info(buf.toString())
        }

        /**
         * Whether to report found problems. Turn on reporting of found problems.
         */
        @JvmStatic
        actual var isReporting: Boolean
            get() = reporting.value
            set(value) {
                reporting.value = value
            }

        /**
         * Whether to report problems or not. By default, reporting is off.
         */
        private val reporting = atomic(false)

        /**
         * The log stream
         */
        private val LOGGER: Logger = LoggerFactory.getLogger(DataPolice::class)
    }
}
