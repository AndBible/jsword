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
/**
 * Utilities that require icu4j via reflection.
 * That is, if the icu4j jar is not on the classpath,
 * the utilities in this package provide standard java
 * behavior.
 */
package org.crosswire.common.icu

import org.crosswire.common.util.KswordLocale

/**
 * NumberShaper changes numbers from one number system to another. That is, the
 * numbers 0-9 have different representations in some locales. This means that
 * they have different code points. For example, Eastern Arabic numbers are from
 * ۰ - ۹.
 *
 * Internally, numbers will be represented with 0-9, but externally they should
 * show as a user wishes. Further user input may, optionally, use the external
 * form.
 *
 * This shaper has special behavior for Arabic numbers that are in the form
 * "12:34" as this is taken as chapter:verse. Normally, a ':' is treated as a
 * numeric separator, this results in "12:34", but for verses it should be
 * "34:12". That is, Arabic, numbers are left-to-right (even though the rest of
 * the script is right-to-left) and the ':' as a numeric separator does not
 * change that. So to get around this we mark the ':' as a right-to-left
 * character.
 *
 * See also: com.ibm.icu.text.ArabicShaping
 *
 * @see java.awt.font.NumericShaper
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class NumberShaper
/**
 * Create a shaper that is appropriate for the user's locale.
 */
() {
    /**
     * Determine whether shaping is possible.
     *
     * @return whether shaping back to 0-9 is possible.
     */
    open fun canShape(): Boolean {
        // return arabicShaper != null || numericShaper != null || getNine() !=
        // '9';
        return getNine() != '9'
    }

    /**
     * Replace 0-9 in the input with representations appropriate for the script.
     *
     * @param input
     *            the text to be transformed
     * @return the transformed text
     */
    open fun shape(input: String?): String? {
        if (input == null) {
            return input
        }

        val src = input.toCharArray()
        val transformed = BooleanArray(1)
        transformed[0] = false
        val dest = shaped(src, transformed)
        if (transformed[0]) {
            return dest.concatToString()
        }

        return input
    }

    /**
     * Determine whether shaping back to 0-9 is possible.
     *
     * @return whether shaping back to 0-9 is possible.
     */
    open fun canUnshape(): Boolean {
        return getNine() != '9'
    }

    /**
     * Replace script representations of numbers with 0-9.
     *
     * @param input
     *            the text to be transformed
     * @return the transformed text
     */
    open fun unshape(input: String): String {
        val src = input.toCharArray()
        val transformed = BooleanArray(1)
        transformed[0] = false
        val dest = unshaped(src, transformed)
        if (transformed[0]) {
            return dest.concatToString()
        }

        return input
    }

    /**
     * Perform shaping back to 0-9.
     * @param src
     *            the text to transform
     * @param transformed
     *            an input parameter of one boolean that can hold whether there
     *            was a transformation
     * @return the unshaped text
     */
    private fun unshaped(src: CharArray, transformed: BooleanArray): CharArray {
        val nine = getNine().code
        if (nine == '9'.code) {
            return src
        }

        val zero = nine - 9
        return transform(src, zero, nine, '9'.code - nine, transformed)
    }

    /**
     * @param src
     *            the text to transform
     * @param transformed
     *            an input parameter of one boolean that can hold whether there
     *            was a transformation
     * @return the shaped string
     */
    private fun shaped(src: CharArray, transformed: BooleanArray): CharArray {
        val nine = getNine()
        if (nine == '9') {
            return src
        }

        return transform(src, '0'.code, '9'.code, nine.code - '9'.code, transformed)
    }

    /**
     * Transform either to or from 0-9 and the script representation, returning
     * the result and true when at least one character is transformed.
     *
     * @param src
     *            the text to transform
     * @param zero
     *            zero in the source representation
     * @param nine
     *            nine in the source representation
     * @param offset
     *            the distance between zeros in the source and target
     *            representation
     * @param transformed
     *            an input parameter of one boolean that can hold whether there
     *            was a transformation
     * @return the shaped string
     */
    private fun transform(src: CharArray, zero: Int, nine: Int, offset: Int, transformed: BooleanArray): CharArray {
        val text = src

        // offset > 0 when we are going from 0-9
        // FIXME(DMS): C:V should be shown as V:C in Farsi.
/*
        int srcLen = text.length;
        int destLen = srcLen;
        if (offset > 0 && srcLen > 3) {
            // count the number of ':' flanked by '0' to '9'
            // each one of these is going
            // to be bracketed with RLO and PDF.
            for (int i = 1; i < srcLen - 1; i++) {
                char prevChar = text[i - 1];
                char curChar = text[i];
                char nextChar = text[i + 1];
                if (curChar == ':' && prevChar >= '0' && prevChar <= '9' && nextChar >= '0' && nextChar <= '9') {
                    destLen += 2;
                }
            }

            // Did we actually see a ':'
            if (destLen != srcLen) {
                transformed[0] = true;
                int sPos = 0;
                int dPos = 0;
                int stop = srcLen - 1; // ensure look-ahead
                char[] dest = new char[destLen];
                dest[dPos++] = text[sPos++];
                while (sPos < stop) {
                    char prevChar = text[sPos - 1];
                    char nextChar = text[sPos + 1];
                    char curChar = text[sPos++];
                    if (curChar == ':' && prevChar >= '0' && prevChar <= '9' && nextChar >= '0' && nextChar <= '9') {
                        dest[dPos++] = '\u202E'; // RLO
                        dest[dPos++] = curChar;
                        dest[dPos++] = '\u202C'; // PDF
                    } else if (curChar >= zero && curChar <= nine) {
                        dest[dPos++] = (char) (curChar + offset);
                    } else {
                        dest[dPos++] = curChar;
                    }
                }
                // copy the rest
                while (sPos < srcLen) {
                    dest[dPos++] = text[sPos++];
                }
                return dest;
            }
        }
        // Are we going to '0' - '9' with embedded, specially marked ':'
        else if (offset < 0 && srcLen > 3) {
            for (int sPos = 0; sPos < srcLen - 2; sPos++) {
                if (text[sPos] == '\u202E' && text[sPos + 1] == ':' && text[sPos + 2] == '\u202C') {
                    destLen -= 2;
                    sPos += 2;
                }
            }

            // Did we actually see a '\u202E:\u202C'
            if (destLen != srcLen) {
                transformed[0] = true;
                char[] dest = new char[destLen];
                int sPos = 0;
                int dPos = 0;
                int stop = srcLen - 2; // ensure look-ahead
                while (sPos < stop) {
                    char curChar = text[sPos++];
                    if (curChar == '\u202E' && text[sPos] == ':' && text[sPos + 1] == '\u202C') {
                        dest[dPos++] = ':';
                        sPos += 2; // skip the whole pattern
                    } else if (curChar >= zero && curChar <= nine) {
                        dest[dPos++] = (char) (curChar + offset);
                    } else {
                        dest[dPos++] = curChar;
                    }
                }

                // copy the rest
                while (sPos < srcLen) {
                    dest[dPos++] = text[sPos++];
                }

                return dest;
            }
        }
*/
        val len = src.size
        for (i in 0 until len) {
            val c = text[i]
            if (c.code >= zero && c.code <= nine) {
                text[i] = (c.code + offset).toChar()
                transformed[0] = true
            }
        }

        return text
    }

    /**
     * Establish nine for the script. There are scripts that don't have zeroes.
     *
     * @return the representation for 9 in the script
     */
    private fun getNine(): Char {
        if (nineShape == '\u0000') {
            nineShape = '9'
            val language = KswordLocale.current().language
            if ("fa" == language) {
                nineShape = '۹'
            } else if ("ar" == language) {
                nineShape = '٩'
            }
        }
        return nineShape
    }

    /**
     * Nine for this shaper.
     */
    private var nineShape: Char = '\u0000'

}
