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
package org.crosswire.jsword.book.readings

import java.text.MessageFormat
import java.util.Calendar
import java.util.Date

import org.crosswire.common.icu.DateFormatter
import org.crosswire.jsword.passage.DefaultLeafKeyList
import org.crosswire.jsword.passage.Key

/**
 * For a readings dictionary the keys are dates.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class ReadingsKey : DefaultLeafKeyList, Cloneable {
    /**
     * Simple Constructor.
     * 
     * @param text
     *            The textual version of the date for these readings in the
     *            format "d mmmm"
     * @param osisName
     *            The OSIS id of this Key
     * @param parent
     *            This Key's parent (or null of this Key has no parent)
     */
    // Java had this constructor protected, used from ReadingsBook in the same package; a Kotlin protected
    // constructor is not visible to a package neighbour, so internal (which compiles to public).
    internal constructor(text: String?, osisName: String?, parent: Key?) : super(text, osisName, parent) {
        val formatter = DateFormatter.getDateInstance()
        formatter.setLenient(true)
        date = formatter.parse(text)
    }

    /**
     * Simple Constructor.
     * 
     * @param date
     *            The date for this key
     */
    // Java had this constructor protected, see above.
    internal constructor(date: Date?) : super(DateFormatter.getDateInstance().format(date), DateFormatter.getSimpleDateInstance("d.MMMM").format(date)) {
        this.date = date
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        // Since this can not be null
        if (other == null) {
            return false
        }

        // Check that that is the same as this
        // Don't use instanceof since that breaks inheritance
        if (other.javaClass != this.javaClass) {
            return false
        }

        // The real bit ...
        val that = other as ReadingsKey

        return getName() == that.getName()
    }

    override fun hashCode(): Int {
        return date!!.hashCode()
    }

    override fun compareTo(other: Key?): Int {
        val that = other as ReadingsKey
        return this.date!!.compareTo(that.date!!)
    }

    override fun clone(): ReadingsKey {
        return (super<Cloneable>.clone() as ReadingsKey).apply { cloneParent() }
    }

    /**
     * The day of the year for the readings
     */
    private var date: Date? = null

    companion object {
        /**
         * Convert the Gregorian Calendar to a string.
         * 
         * @param externalKey
         * @return the internal representation of the key
         */
        @JvmStatic
        fun external2internal(externalKey: Calendar): String {
            val objs = arrayOf<Any?>(
                Integer.valueOf(1 + externalKey.get(Calendar.MONTH)), Integer.valueOf(externalKey.get(Calendar.DATE))
            )
            return KEY_FORMAT.format(objs)

        }

        /**
         * Date formatter
         */
        private val KEY_FORMAT: MessageFormat = MessageFormat("{0,number,00}.{1,number,00}")
    }
}
