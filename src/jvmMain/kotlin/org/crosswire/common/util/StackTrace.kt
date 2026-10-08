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

import java.io.PrintWriter
import java.io.StringWriter
import java.util.NoSuchElementException

/**
 * Unscramble the current stack, and present the data from it to the user in
 * various forms. This code is slightly dodgy in that it makes use of the way
 * exceptions print their stack traces, however it is probably a safe enough
 * assumption for the moment.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class StackTrace {
    /**
     * Generate a stack trace an model it
     */
    constructor() {
        init(Throwable(), 2)
    }

    /**
     * We already have an Exception that we'd like to model
     *
     * @param ex
     *            The Exception to model
     */
    constructor(ex: Throwable) {
        init(ex, 1)
    }

    /**
     * Create a stack trace of the code at this point
     *
     * @param exception
     *            The Throwable containing the Stack Trace
     * @param discard
     *            The number of uppermost stack frames to ignore
     */
    private fun init(exception: Throwable, discard: Int) {
        val sout = StringWriter()
        exception.printStackTrace(PrintWriter(sout))
        val msg = String(sout.buffer)
        val calls = StringUtil.split(msg, "\n\r")

        var total = 0
        for (i in 0 until calls.size - discard) {
            val call = calls[i + discard]

            if (!(call.startsWith("Caused") || call.indexOf("...") >= 0)) {
                total++
            }
        }

        classNames = arrayOfNulls(total)
        methodNames = arrayOfNulls(total)
        fileNames = arrayOfNulls(total)
        lineNumbers = IntArray(total)

        var j = 0
        for (i in 0 until calls.size - discard) {
            val call = calls[i + discard]
            var oops = false
            try {
                if (!(call.startsWith("Caused") || call.indexOf("...") >= 0)) {
                    val spcIndex = call.indexOf(' ')
                    val lhsIndex = call.indexOf('(')
                    val clnIndex = call.indexOf(':')
                    val rhsIndex = call.indexOf(')')

                    // trim like java.lang.String.trim(): everything up to U+0020
                    val fullFn = call.substring(spcIndex + 1, lhsIndex).trim { it <= ' ' }
                    val lastDot = fullFn.lastIndexOf('.')

                    classNames[j] = fullFn.substring(0, lastDot).replace('/', '.')
                    methodNames[j] = fullFn.substring(lastDot + 1)

                    if (clnIndex != -1 && lhsIndex < clnIndex) {
                        fileNames[j] = call.substring(lhsIndex + 1, clnIndex)
                        lineNumbers[j] = Integer.parseInt(call.substring(clnIndex + 1, rhsIndex))
                    } else {
                        fileNames[j] = call.substring(lhsIndex + 1, rhsIndex)
                        lineNumbers[j] = 0
                    }
                    j++
                }
            } catch (ex: NumberFormatException) {
                oops = true
            } catch (ex: StringIndexOutOfBoundsException) {
                // For whatever reason, Java 7 under Web Start is throwing this on
                // call.substring(spcIndex + 1, lhsIndex) with a -56 being passed.
                oops = true
            }
            if (oops) {
                classNames[j] = "ParseError: "
                methodNames[j] = call
                fileNames[j] = "Error"
                lineNumbers[j] = 0
                j++
            }
        }
    }

    /**
     * How many stack elements are there?
     *
     * @return the number of stack elements
     */
    fun countStackElements(): Int {
        return methodNames.size
    }

    /**
     * Get the name of a function
     *
     * @param level
     *            Number of calling function
     * @return the function name
     */
    fun getFunctionName(level: Int): String? {
        return methodNames[level]
    }

    /**
     * Get the name of a function including class name
     *
     * @param level
     *            Number of calling function
     * @return the full function name
     */
    fun getFullFunctionName(level: Int): String {
        return classNames[level] + "." + methodNames[level] + "()"
    }

    /**
     * Get the name of a class
     *
     * @param level
     *            Number of calling function
     * @return the class name
     */
    fun getClassName(level: Int): String? {
        return classNames[level]
    }

    /**
     * Get the name of a file
     *
     * @param level
     *            Number of calling function
     * @return the file name
     */
    fun getFileName(level: Int): String? {
        return fileNames[level]
    }

    /**
     * Get the line number within a file
     *
     * @param level
     *            Number of calling function
     * @return the line number
     */
    fun getLineNumber(level: Int): Int {
        return lineNumbers[level]
    }

    /**
     * Get the count of classes
     *
     * @return the number of classes
     */
    val classCount: Int
        get() = classNames.size

    /**
     * Get the Class that owns the function
     *
     * @param level
     *            Number of calling function
     * @return the function owner
     */
    fun getClass(level: Int): Class<*>? {
        try {
            return ClassUtil.forName(classNames[level])
        } catch (ex: ClassNotFoundException) {
            jswordAssert(false) { ex }
            return null
        }
    }

    /**
     * Base class for the real enumeration implementations below
     * @param <T> the type of the object in the stack
     */
    abstract inner class AbstractStackIterator<T> : MutableIterator<T> {
        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        override fun hasNext(): Boolean {
            return level < classCount
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        override fun remove() {
            throw UnsupportedOperationException()
        }

        /**
         * @return the level.
         * @throws NoSuchElementException 
         */
        @Throws(NoSuchElementException::class)
        open fun getAndIncrementLevel(): Int {
            return level++
        }

        /**
         * Are there more stack levels
         */
        private var level = 0
    }

    /**
     * To iterate over the class names
     *
     * @return an iterator of class names
     */
    fun getClassNameElements(): MutableIterator<String?> {
        return object : AbstractStackIterator<String?>() {
            @Throws(NoSuchElementException::class)
            override fun next(): String? {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return getClassName(getAndIncrementLevel())
            }
        }
    }

    /**
     * To iterate over the function names
     *
     * @return an iterator of function names
     */
    fun getFunctionNameElements(): MutableIterator<String?> {
        return object : AbstractStackIterator<String?>() {
            @Throws(NoSuchElementException::class)
            override fun next(): String? {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return getFunctionName(getAndIncrementLevel())
            }
        }
    }

    /**
     * To iterate over the full function names
     *
     * @return an iterator of full function names
     */
    fun getFullFunctionNameElements(): MutableIterator<String> {
        return object : AbstractStackIterator<String>() {
            @Throws(NoSuchElementException::class)
            override fun next(): String {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                return getFullFunctionName(getAndIncrementLevel())
            }
        }
    }

    /**
     * Array containing the class names
     */
    private lateinit var classNames: Array<String?>

    /**
     * Array containing the method names
     */
    private lateinit var methodNames: Array<String?>

    /**
     * Array containing the file names
     */
    private lateinit var fileNames: Array<String?>

    /**
     * Array containing the line numbers
     */
    private lateinit var lineNumbers: IntArray
}
