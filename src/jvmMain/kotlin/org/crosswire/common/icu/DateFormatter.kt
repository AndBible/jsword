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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.common.icu

import java.lang.reflect.InvocationTargetException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

import org.crosswire.common.util.ClassUtil
import org.crosswire.common.util.ReflectionUtil
import org.crosswire.common.util.jswordAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * DateFormat provides a wrapper of some of DateFormat and SimpleDateFormat
 * using ICU4J if present, otherwise from core Java. Note, only those methods in
 * DateFormat that are actually used are here.
 *
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br></br>
 * The copyright to this program is held by its authors.
 */
class DateFormatter
/**
 * Prevent instantiation.
 */
private constructor() {
    /**
     * The actual formatter.
     */
    private var formatter: Any? = null

    /**
     * The class of the formatter
     */
    private var formatterClass: Class<*>? = null

    /**
     * Set whether this DataFormatter should be lenient in parsing dates.
     *
     * @param lenient whether to be lenient or not
     * @see java.text.DateFormat.setLenient
     */
    fun setLenient(lenient: Boolean) {
        try {
            val lenientTypes = arrayOf<Class<*>?>(
                java.lang.Boolean.TYPE
            )
            val lenientParams = arrayOf<Any?>(
                java.lang.Boolean.valueOf(lenient)
            )
            ReflectionUtil.invoke(formatterClass, formatter, "setLenient", lenientParams, lenientTypes)
        } catch (e: NoSuchMethodException) {
            jswordAssert(false) { e }
        } catch (e: IllegalAccessException) {
            jswordAssert(false) { e }
        } catch (e: InvocationTargetException) {
            jswordAssert(false) { e }
        }
    }

    /**
     * Formats a Date into a date/time string.
     *
     * @param date the time value to be formatted into a time string.
     * @return the formatted time string.
     * @see java.text.DateFormat.format
     */
    fun format(date: Date?): String? {
        try {
            return ReflectionUtil.invoke(formatterClass, formatter, "format", date) as String?
        } catch (e: NoSuchMethodException) {
            jswordAssert(false) { e }
        } catch (e: IllegalAccessException) {
            jswordAssert(false) { e }
        } catch (e: InvocationTargetException) {
            jswordAssert(false) { e }
        }
        return ""
    }

    /**
     * Convert text to a date.
     *
     * @param text the input to parse as a date
     * @return the resultant date
     * @see java.text.DateFormat.parse
     */
    fun parse(text: String?): Date? {
        try {
            return ReflectionUtil.invoke(formatterClass, formatter, "parse", text) as Date?
        } catch (e: NoSuchMethodException) {
            jswordAssert(false) { e }
        } catch (e: IllegalAccessException) {
            jswordAssert(false) { e }
        } catch (e: InvocationTargetException) {
            jswordAssert(false) { e }
        }
        return Date()
    }

    companion object {
        // Note these values are the same for Java and ICU4J
        /**
         * Constant for full style pattern.
         */
        const val FULL: Int = 0

        /**
         * Constant for long style pattern.
         */
        const val LONG: Int = 1

        /**
         * Constant for medium style pattern.
         */
        const val MEDIUM: Int = 2

        /**
         * Constant for short style pattern.
         */
        const val SHORT: Int = 3

        /**
         * Constant for default style pattern. Its value is MEDIUM.
         */
        const val DEFAULT: Int = MEDIUM

        private val LOGGER: Logger = LoggerFactory.getLogger(DateFormatter::class.java)
        private const val DEFAULT_SIMPLE_DATE_FORMAT_CLASS = "com.ibm.icu.text.SimpleDateFormat"
        private const val DEFAULT_DATE_FORMAT_CLASS = "com.ibm.icu.text.DateFormat"

        private var defaultSimpleDateFormat: Class<*>? = null
        private var defaultDateFormat: Class<*>? = null

        init {
            try {
                defaultSimpleDateFormat = ClassUtil.forName(DEFAULT_SIMPLE_DATE_FORMAT_CLASS)
            } catch (ex: ClassNotFoundException) {
                LOGGER.info("Error loading simple date format class [{}]", DEFAULT_SIMPLE_DATE_FORMAT_CLASS)
            }

            try {
                defaultDateFormat = ClassUtil.forName(DEFAULT_DATE_FORMAT_CLASS)
            } catch (ex: ClassNotFoundException) {
                LOGGER.info("Error loading date format class [{}]", DEFAULT_SIMPLE_DATE_FORMAT_CLASS)
            }
        }

        /**
         * Construct a DateFormatter with the given date format.
         *
         * @param format the date format
         * @return a DateFormatter of the given format
         * @see java.text.DateFormat.getDateInstance
         */
        @JvmStatic
        fun getDateInstance(format: Int): DateFormatter {
            val fmt = DateFormatter()
            var oops = false
            try {
                fmt.formatterClass = defaultDateFormat
                // To call a method taking a type of int, the type has to match but
                // the object has to be wrapped
                val instanceTypes = arrayOf<Class<*>?>(
                    Integer.TYPE
                )
                val instanceParams = arrayOf<Any?>(
                    Integer.valueOf(format)
                )
                fmt.formatter = ReflectionUtil.invoke(fmt.formatterClass, fmt.formatterClass, "getDateInstance", instanceParams, instanceTypes)
            } catch (e: NoSuchMethodException) {
                oops = true
            } catch (e: IllegalAccessException) {
                oops = true
            } catch (e: InvocationTargetException) {
                oops = true
            } catch (e: NullPointerException) {
                oops = true
            }

            if (oops) {
                fmt.formatterClass = DateFormat::class.java
                fmt.formatter = DateFormat.getDateInstance(format)
            }

            return fmt
        }

        /**
         * Construct a DateFormatter with the default date format.
         *
         * @return a DateFormatter of the default format
         * @see java.text.DateFormat.getDateInstance
         */
        @JvmStatic
        fun getDateInstance(): DateFormatter {
            return getDateInstance(DEFAULT)
        }

        /**
         * Construct a simple DateFormatter with the given date format.
         *
         * @param format the date format
         * @return a DateFormatter with the given date format
         * @see java.text.DateFormat.getDateInstance
         */
        @JvmStatic
        fun getSimpleDateInstance(format: String?): DateFormatter {
            val fmt = DateFormatter()
            var oops = false
            try {
                fmt.formatterClass = defaultSimpleDateFormat
                fmt.formatter = ReflectionUtil.construct(fmt.formatterClass, format)
            } catch (e: NoSuchMethodException) {
                oops = true
            } catch (e: IllegalAccessException) {
                oops = true
            } catch (e: InvocationTargetException) {
                oops = true
            } catch (e: NullPointerException) {
                oops = true
            } catch (e: InstantiationException) {
                oops = true
            }

            if (oops) {
                fmt.formatterClass = SimpleDateFormat::class.java
                fmt.formatter = SimpleDateFormat(format)
            }

            return fmt
        }
    }
}
