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

import kotlin.jvm.JvmStatic

import org.crosswire.common.util.lowercase
import org.crosswire.common.util.uppercase

import org.crosswire.common.util.jswordAssert
import org.crosswire.common.util.KswordLocale as CurrentLanguage

/**
 * Types of Sentence Case.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
enum class CaseType {
    LOWER {
        override fun setCase(word: String): String {
            return word.lowercase(CurrentLanguage.current())
        }
    },

    SENTENCE {
        override fun setCase(word: String): String {
            val index = word.indexOf('-')
            if (index == -1) {
                return toSentenceCase(word)
            }

            // So there is a "-", however first some exceptions
            if ("maher-shalal-hash-baz".equals(word, ignoreCase = true)) {
                return "Maher-Shalal-Hash-Baz"
            }

            if ("no-one".equals(word, ignoreCase = true)) {
                return "No-one"
            }

            if ("god-".equals(word.substring(0, 4), ignoreCase = true)) {
                return toSentenceCase(word)
            }

            // So cut by the -
            return toSentenceCase(word.substring(0, index)) + "-" + toSentenceCase(word.substring(index + 1))
        }
    },

    UPPER {
        override fun setCase(word: String): String {
            return word.uppercase(CurrentLanguage.current())
        }
    };

    /**
     * Adjust the case of the string as defined by the type.
     *
     * @param word the string to transform
     * @return the transformed string
     */
    abstract fun setCase(word: String): String

    /**
     * Get an integer representation for this CaseType
     *
     * @return the ordinal value of this CaseType
     */
    fun toInteger(): Int {
        return ordinal
    }

    companion object {
        /**
         * Change to sentence case - that is first character in caps, the rest in lower.
         *
         * @param word
         *            The word to be manipulated
         * @return The altered word
         */
        @JvmStatic
        fun toSentenceCase(word: String): String {
            if (word.length == 0) {
                return ""
            }

            return word[0].uppercaseChar().toString() + word.substring(1).lowercase(CurrentLanguage.current())
        }

        /**
         * What case is the specified word?. A blank word is LOWER, a word with a
         * single upper case letter is SENTENCE and not UPPER - Simply because this
         * is more likely, however TO BE SURE I WOULD NEED TO THE CONTEXT. I could
         * not tell otherwise.
         *
         *
         * The issue here is that getCase("FreD") is undefined. Telling if this is
         * SENTENCE (Tubal-Cain) or MIXED (really the case) is complex and would
         * slow things down for a case that I don't believe happens with Bible text.
         *
         *
         * @param word
         *            The word to be tested
         * @return LOWER, SENTENCE, UPPER or MIXED
         * @exception IllegalArgumentException
         *                is the word is null
         */
        @JvmStatic
        fun getCase(word: String): CaseType {
            // Blank word
            if (word.length == 0) {
                return LOWER
            }

            // Lower case?
            if (word == word.lowercase(CurrentLanguage.current())) {
                return LOWER
            }

            // Upper case?
            // A string length of 1 is no good ('I' or 'A' is sentence case)
            if (word == word.uppercase(CurrentLanguage.current()) && word.length != 1) {
                return UPPER
            }

            // So ...
            return SENTENCE
        }

        /**
         * Lookup method to convert from a String
         *
         * @param name the name of the CaseType
         * @return the CaseType that matches
         */
        @JvmStatic
        fun fromString(name: String?): CaseType? {
            for (v in values()) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }

        /**
         * Lookup method to convert from an integer
         *
         * @param i the ordinal value of the CaseType
         * @return the matching CaseType
         */
        @JvmStatic
        fun fromInteger(i: Int): CaseType? {
            for (v in values()) {
                if (v.ordinal == i) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }
    }
}
