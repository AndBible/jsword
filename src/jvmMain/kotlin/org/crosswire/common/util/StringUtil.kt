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
/**
 * A collection of various generic utility classes.
 */
package org.crosswire.common.util

import java.io.BufferedReader
import java.io.IOException
import java.io.Reader

/**
 * A generic class of String utilities.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class StringUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * The newline character
         */
        @JvmField
        val NEWLINE: String = System.getProperty("line.separator", "\r\n")

        /**
         * Compare two strings for equality such that both can be null.
         *
         * @param string1 the first string
         * @param string2 the second string
         * @return true when both are null or both have the same string value
         */
        @JvmStatic
        fun equals(string1: String?, string2: String?): Boolean {
            if (string1 == null) {
                return string2 == null
            }
            return string1 == string2
        }

        /**
         * This method reads an InputStream <b>In its entirety</b>, and passes The
         * text back as a string. If you are reading from a source that can block
         * then be prepared for a long wait for this to return.
         *
         * @param in
         *            The Stream to read from.
         * @return A string containing all the text from the Stream.
         * @throws IOException when an I/O error occurred
         */
        @JvmStatic
        @Throws(IOException::class)
        fun read(`in`: Reader): String {
            val retcode = StringBuilder()
            // Quiet Android from complaining about using the default BufferReader buffer size.
            // The actual buffer size is undocumented. So this is a good idea any way.
            val din = BufferedReader(`in`, 8192)

            while (true) {
                val line = din.readLine() ?: break

                retcode.append(line)
                retcode.append(NEWLINE)
            }

            return retcode.toString()
        }

        /**
         * This function creates a readable title from a variable name type input.
         * For example calling: StringUtil.createTitle("one_two") = "One Two"
         * StringUtil.createTitle("oneTwo") = "One Two"
         *
         * @param variable the name of a variable
         * @return the generated title
         */
        @JvmStatic
        fun createTitle(variable: String): String {
            val retcode = StringBuilder()
            var lastlower = false
            var lastspace = true

            for (i in 0 until variable.length) {
                var c = variable[i]

                if (lastlower && Character.isUpperCase(c) && !lastspace) {
                    retcode.append(' ')
                }

                lastlower = !Character.isUpperCase(c)

                if (lastspace) {
                    c = Character.toUpperCase(c)
                }

                if (c == '_') {
                    c = ' '
                }

                if (!lastspace || c != ' ') {
                    retcode.append(c)
                }

                lastspace = c == ' '
            }

            return retcode.toString()
        }

        /**
         * For example getInitials("Java DataBase Connectivity") = "JDC" and
         * getInitials("Church of England") = "CoE".
         *
         * @param sentence
         *            The phrase from which to get the initial letters.
         * @return The initial letters in the given words.
         */
        @JvmStatic
        fun getInitials(sentence: String?): String {
            val words = split(sentence)

            val retcode = StringBuilder()
            for (i in words.indices) {
                val word = words[i]

                var first = 0.toChar()
                var j = 0
                while (first.code == 0 && j < word.length) {
                    val c = word[j]
                    if (Character.isLetter(c)) {
                        first = c
                    }
                    j++
                }

                if (first.code != 0) {
                    retcode.append(first)
                }
            }

            return retcode.toString()
        }

        /**
         * Splits the provided text into an array, using whitespace as the
         * separator. Whitespace is defined by {@link Character#isWhitespace(char)}.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <pre>
         * StringUtil.split(null)       = []
         * StringUtil.split("")         = []
         * StringUtil.split("abc def")  = ["abc", "def"]
         * StringUtil.split("abc  def") = ["abc", "def"]
         * StringUtil.split(" abc ")    = ["abc"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @return an array of parsed Strings, <code>null</code> if null String
         *         input
         */
        @JvmStatic
        fun split(str: String?): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val cstr = str.toCharArray()

            var count = 0
            var start = 0
            var i = 0
            while (indexOfWhitespace(cstr, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    ++count
                }
                start = i + 1
            }

            // If it didn't end with a separator then add in the last part
            if (start < len) {
                ++count
            }

            // Create the array
            val list = newArray(count)

            // If there were no separators
            // then we have one big part
            if (start == 0) {
                list[0] = str
                return list
            }

            start = 0
            i = 0
            var x = 0
            while (indexOfWhitespace(cstr, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    list[x++] = str.substring(start, i)
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len) {
                list[x++] = str.substring(start)
            }

            return list
        }

        /**
         * Splits the provided text into an array, using whitespace as the
         * separator. Whitespace is defined by {@link Character#isWhitespace(char)}.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <pre>
         * StringUtil.split(null)       = []
         * StringUtil.split("")         = []
         * StringUtil.split("abc def")  = ["abc", "def"]
         * StringUtil.split("abc  def") = ["abc", "def"]
         * StringUtil.split(" abc ")    = ["abc"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param max the maximum number of elements to return
         * @return an array of parsed Strings, <code>null</code> if null String
         *         input
         */
        @JvmStatic
        fun split(str: String?, max: Int): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val cstr = str.toCharArray()

            var count = 0
            var start = 0
            var i = 0
            while (indexOfWhitespace(cstr, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    ++count
                }
                start = i + 1
            }

            // If it didn't end with a separator then add in the last part
            if (start < len) {
                ++count
            }

            // If there were no separators
            // then we have one big part
            if (start == 0) {
                val list = newArray(count)
                list[0] = str
                return list
            }

            // Limit the result
            if (max > 0 && count > max) {
                count = max
            }

            // Create the array
            val list = newArray(count)

            start = 0
            i = 0
            var x = 0
            while (indexOfWhitespace(cstr, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start && x < count) {
                    list[x++] = str.substring(start, i)
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len && x < count) {
                list[x++] = str.substring(start)
            }

            return list
        }

        /**
         * Splits the provided text into an array, separator specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>.
         * </p>
         *
         * <pre>
         * StringUtil.split(null, *)         = []
         * StringUtil.split("", *)           = []
         * StringUtil.split("a.b.c", '.')    = ["a", "b", "c"]
         * StringUtil.split("a..b.c", '.')   = ["a", "b", "c"]
         * StringUtil.split("a:b:c", '.')    = ["a:b:c"]
         * StringUtil.split("a b c", ' ')    = ["a", "b", "c"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChar
         *            the character used as the delimiter
         * @return an array of parsed Strings
         */
        @JvmStatic
        fun split(str: String?, separatorChar: Char): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            // Determine the size of the array
            var count = 0
            var start = 0
            var i = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start && i < len) {
                    ++count
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len) {
                ++count
            }

            // Create the array
            val list = newArray(count)

            // If there were no separators
            // then we have one big part
            if (count == 1) {
                list[0] = str
                return list
            }

            start = 0
            i = 0
            var x = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    list[x++] = str.substring(start, i)
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len) {
                list[x++] = str.substring(start, len)
            }

            return list
        }

        /**
         * Splits the provided text into an array, separator specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>.
         * </p>
         *
         * <pre>
         * StringUtil.split(null, *, 2)         = []
         * StringUtil.split("", *, 2)           = []
         * StringUtil.split("a.b.c", '.', 2)    = ["a", "b"]
         * StringUtil.split("a..b.c", '.', 2)   = ["a", "b"]
         * StringUtil.split("a:b:c", '.', 2)    = ["a:b:c"]
         * StringUtil.split("a b c", ' ', 2)    = ["a", "b"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChar
         *            the character used as the delimiter
         * @param max
         *            the maximum number of elements to include in the array.
         *            A zero or negative value implies no limit
         * @return an array of parsed Strings
         */
        @JvmStatic
        fun split(str: String?, separatorChar: Char, max: Int): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            // Determine the size of the array
            var count = 0
            var start = 0
            var i = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    ++count
                }
                start = i + 1
            }

            // If it didn't end with a separator then add in the last part
            if (start < len) {
                ++count
            }

            // If there were no separators
            // then we have one big part
            if (count == 1) {
                val list = newArray(count)
                list[0] = str
                return list
            }

            // Limit the result
            if (max > 0 && count > max) {
                count = max
            }

            // Create the array
            val list = newArray(count)

            start = 0
            i = 0
            var x = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start && x < count) {
                    list[x++] = str.substring(start, i)
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len && x < count) {
                list[x++] = str.substring(start)
            }

            return list
        }

        /**
         * Splits the provided text into an array, separators specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>. A
         * <code>null</code> separatorChars splits on whitespace.
         * </p>
         *
         * <pre>
         * StringUtil.split(null, *)         = []
         * StringUtil.split("", *)           = []
         * StringUtil.split("abc def", null) = ["abc", "def"]
         * StringUtil.split("abc def", " ")  = ["abc", "def"]
         * StringUtil.split("abc  def", " ") = ["abc", "def"]
         * StringUtil.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChars
         *            the characters used as the delimiters, <code>null</code>
         *            splits on whitespace
         * @return an array of parsed Strings, <code>null</code> if null String
         *         input
         */
        @JvmStatic
        fun split(str: String?, separatorChars: String?): Array<String> {
            return split(str, separatorChars, -1)
        }

        /**
         * Splits the provided text into an array, separators specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated as one separator.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>. A
         * <code>null</code> separatorChars splits on whitespace.
         * </p>
         *
         * <pre>
         * StringUtil.split(null, *, *)            = []
         * StringUtil.split("", *, *)              = []
         * StringUtil.split("ab de fg", null, 0)   = ["ab", "cd", "ef"]
         * StringUtil.split("ab   de fg", null, 0) = ["ab", "cd", "ef"]
         * StringUtil.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
         * StringUtil.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorStr
         *            the characters used as the delimiters, <code>null</code>
         *            splits on whitespace
         * @param max
         *            the maximum number of elements to include in the array. A zero
         *            or negative value implies no limit
         * @return an array of parsed Strings
         */
        @JvmStatic
        fun split(str: String?, separatorStr: String?, max: Int): Array<String> {
            // Performance tuned for 2.0 (JDK1.4)
            // Direct code is quicker than StringTokenizer.
            // Also, StringTokenizer uses isSpace() not isWhitespace()

            if (separatorStr == null) {
                return split(str, max)
            }

            if (separatorStr.length == 1) {
                return split(str, separatorStr[0], max)
            }

            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val cstr = str.toCharArray()
            val separatorChars = separatorStr.toCharArray()

            var count = 0
            var start = 0
            var i = 0
            while (indexOfAny(cstr, separatorChars, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start) {
                    ++count
                }
                start = i + 1
            }

            // If it didn't end with a separator then add in the last part
            if (start < len) {
                ++count
            }

            // If there were no separators
            // then we have one big part
            if (count == 1) {
                val list = newArray(count)
                list[0] = str
                return list
            }

            // Limit the result
            if (max > 0 && count > max) {
                count = max
            }

            // Create the array
            val list = newArray(count)

            start = 0
            i = 0
            var x = 0
            while (indexOfAny(cstr, separatorChars, start).also { i = it } != -1) {
                // Don't count separator at beginning,
                // after another or at the end
                if (i > start && x < count) {
                    list[x++] = str.substring(start, i)
                }
                start = i + 1
            }
            // If it didn't end with a separator then add in the last part
            if (start < len && x < count) {
                list[x++] = str.substring(start)
            }

            return list
        }

        /**
         * Splits the provided text into an array, separator specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated individually.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>.
         * </p>
         *
         * <pre>
         * StringUtil.splitAll(null, *)         = []
         * StringUtil.splitAll("", *)           = []
         * StringUtil.splitAll("a.b.c", '.')    = ["a", "b", "c"]
         * StringUtil.splitAll("a..b.c", '.')   = ["a", "", "b", "c"]
         * StringUtil.splitAll("a:b:c", '.')    = ["a:b:c"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChar
         *            the character used as the delimiter
         * @return an array of parsed Strings
         */
        @JvmStatic
        fun splitAll(str: String?, separatorChar: Char): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            // Determine the size of the array
            var count = 1
            var start = 0
            var i = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                ++count
                start = i + 1
            }

            // Create the array
            val list = newArray(count)

            // If there were no separators
            // then we have one big part
            if (count == 1) {
                list[0] = str
                return list
            }

            start = 0
            i = 0
            for (x in 0 until count) {
                i = str.indexOf(separatorChar, start)
                if (i != -1) {
                    list[x] = str.substring(start, i)
                } else {
                    list[x] = str.substring(start)
                }
                start = i + 1
            }

            return list
        }

        /**
         * Splits the provided text into an array, separator specified. This is an
         * alternative to using StringTokenizer.
         *
         * <p>
         * The separator is not included in the returned String array. Adjacent
         * separators are treated individually.
         * </p>
         *
         * <p>
         * A <code>null</code> input String returns <code>null</code>.
         * </p>
         *
         * <pre>
         * StringUtil.splitAll(null, *, 2)         = []
         * StringUtil.splitAll("", *, 2)           = []
         * StringUtil.splitAll("a.b.c", '.', 2)    = ["a", "b"]
         * StringUtil.splitAll("a..b.c", '.', 2)   = ["a", ""]
         * StringUtil.splitAll("a:b:c", '.', 2)    = ["a:b:c"]
         * StringUtil.splitAll("a b c", ' ', 2)    = ["a", "b"]
         * </pre>
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChar
         *            the character used as the delimiter
         * @param max
         *            the maximum number of elements to include in the array.
         *             A zero or negative value implies no limit
         * @return an array of parsed Strings
         */
        @JvmStatic
        fun splitAll(str: String?, separatorChar: Char, max: Int): Array<String> {
            if (str == null) {
                return EMPTY_STRING_ARRAY.clone()
            }

            val len = str.length
            if (len == 0) {
                return EMPTY_STRING_ARRAY.clone()
            }

            // Determine the size of the array
            var count = 1
            var start = 0
            var i = 0
            while (str.indexOf(separatorChar, start).also { i = it } != -1) {
                ++count
                start = i + 1
            }

            // If there were no separators
            // then we have one big part
            if (count == 1) {
                val list = newArray(count)
                list[0] = str
                return list
            }

            // Limit the result
            if (max > 0 && count > max) {
                count = max
            }

            // Create the array
            val list = newArray(count)

            start = 0
            i = 0
            for (x in 0 until count) {
                i = str.indexOf(separatorChar, start)
                if (i != -1) {
                    list[x] = str.substring(start, i)
                } else {
                    list[x] = str.substring(start, len)
                }
                start = i + 1
            }

            return list
        }

        /**
         * Joins the elements of the provided array into a single String containing
         * the provided list of elements.
         *
         * <p>
         * No delimiter is added before or after the list. A <code>null</code>
         * separator is the same as an empty String (""). Null objects or empty
         * strings within the array are represented by empty strings.
         * </p>
         *
         * <pre>
         * StringUtil.join(null, *)                = null
         * StringUtil.join([], *)                  = ""
         * StringUtil.join([null], *)              = ""
         * StringUtil.join(["a", "b", "c"], "--")  = "a--b--c"
         * StringUtil.join(["a", "b", "c"], null)  = "abc"
         * StringUtil.join(["a", "b", "c"], "")    = "abc"
         * StringUtil.join([null, "", "a"], ',')   = ",,a"
         * </pre>
         *
         * @param array
         *            the array of values to join together, may be null
         * @param aSeparator
         *            the separator character to use, null treated as ""
         * @return the joined String, <code>null</code> if null array input
         */
        @JvmStatic
        fun join(array: Array<out Any?>?, aSeparator: String?): String? {
            var separator = aSeparator
            if (array == null) {
                return null
            }
            if (separator == null) {
                separator = ""
            }
            val arraySize = array.size

            // ArraySize == 0: Len = 0
            // ArraySize > 0: Len = NofStrings *(len(firstString) + len(separator))
            // (Assuming that all Strings are roughly equally long)
            val first = if (arraySize == 0) null else array[0]
            val bufSize = if (arraySize == 0) 0 else arraySize * ((if (first == null) 16 else first.toString().length) + separator.length)

            val buf = StringBuilder(bufSize)

            for (i in 0 until arraySize) {
                if (i > 0) {
                    buf.append(separator)
                }
                if (array[i] != null) {
                    buf.append(array[i])
                }
            }
            return buf.toString()
        }

        /**
         * Find the first occurrence of a separator in the character buffer beginning
         * at the given offset.
         *
         * @param str
         *            the String to parse, may be null
         * @param separatorChars
         *            the characters used as the delimiters, <code>null</code>
         *            splits on whitespace
         * @param offset
         *            the index of the first character to consider
         * @return the index of a separator char in the string or -1
         */
        @JvmStatic
        fun indexOfAny(str: CharArray, separatorChars: CharArray, offset: Int): Int {
            val strlen = str.size
            val seplen = separatorChars.size
            for (i in offset until strlen) {
                val ch = str[i]
                for (j in 0 until seplen) {
                    if (separatorChars[j] == ch) {
                        return i
                    }
                }
            }
            return -1
        }

        /**
         * Find the first occurrence of a whitespace in the character buffer beginning
         * at the given offset.
         * Whitespace is defined by {@link Character#isWhitespace(char)}.
         *
         * @param str
         *            the String to parse, may be null
         * @param offset
         *            the index of the first character to consider
         * @return the index of a separator char in the string or -1
         */
        @JvmStatic
        fun indexOfWhitespace(str: CharArray, offset: Int): Int {
            val strlen = str.size
            for (i in offset until strlen) {
                if (Character.isWhitespace(str[i])) {
                    return i
                }
            }
            return -1
        }

        /**
         * An empty immutable <code>String</code> array.
         */
        @JvmField
        val EMPTY_STRING_ARRAY: Array<String> = arrayOf()

        /**
         * A String[] of the given size whose elements are filled in by the caller (as Java's `new String[count]`).
         */
        @Suppress("UNCHECKED_CAST")
        private fun newArray(count: Int): Array<String> = arrayOfNulls<String>(count) as Array<String>
    }
}
