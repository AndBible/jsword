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

import org.crosswire.common.util.StringUtil

import java.util.Locale

/**
 * The SentenceUtil class provide utility functions for the various Books.
 * 
 * It is not designed to be used outside of the book package, so using it
 * outside of these bounds is at your own risk.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class SentenceUtil
/**
 * Ensure we can not be instantiated
 */
private constructor() {
    companion object {
        /**
         * Take a string and tokenize it using " " and "--" as delimiters into an
         * Array of Strings. There is a question mark over what to do with initial
         * spaces. This algorithm discards them, I'm not sure if this is the right
         * thing to do.
         * 
         * @param sentence
         *            The string to parse.
         * @return The string array
         */
        @JvmStatic
        fun tokenize(sentence: String): Array<String> {
            val tokens: MutableList<String> = ArrayList()

            var pos = 0
            var temp: String
            var alive = true

            while (alive) {
                // Find the next space and double dash
                var nextSpace = sentence.indexOf(' ', pos)
                var nextDDash = sentence.indexOf("--", pos)

                // If there is a space just after the ddash then ignore the ddash
                if (nextSpace == nextDDash + 2) {
                    nextDDash = -1
                }

                // If there is a ddash just after the space then ignore the space
                if (nextDDash == nextSpace + 1) {
                    nextSpace = -1
                }

                // if there are no more tokens then just add in what we've got.
                if (nextSpace == -1 && nextDDash == -1) {
                    temp = sentence.substring(pos)
                    alive = false
                } else if ((nextSpace != -1 && nextSpace < nextDDash) || (nextDDash == -1)) {
                    // Space is next if it is not -1 and it is less than ddash
                    // The next separator is a space
                    temp = sentence.substring(pos, nextSpace) + ' '
                    pos = nextSpace + 1
                } else {
                    // The next separator is a ddash
                    temp = sentence.substring(pos, nextDDash) + "--"
                    pos = nextDDash + 2
                }

                // Java's String.trim() removes every char <= ' '
                if ("" != temp.trim { it <= ' ' }) {
                    tokens.add(temp)
                }
            }

            // Create a String[]
            val retcode = arrayOfNulls<String>(tokens.size)
            var i = 0
            for (token in tokens) {
                retcode[i++] = token
            }

            @Suppress("UNCHECKED_CAST")
            return retcode as Array<String>
        }

        /**
         * From a sentence get a list of words (in original order) without any
         * punctuation, and all in lower case.
         * 
         * @param words
         *            Words with punctuation
         * @return Words without punctuation
         */
        @JvmStatic
        fun stripPunctuation(vararg words: String): Array<String> {
            val retcode = arrayOfNulls<String>(words.size)

            // Remove the punctuation from the ends of the words.
            for (i in words.indices) {
                retcode[i] = stripPunctuationWord(words[i])
            }

            @Suppress("UNCHECKED_CAST")
            return retcode as Array<String>
        }

        /**
         * From a sentence get a list of words (in original order) without any
         * punctuation, and all in lower case.
         * 
         * @param words
         *            Words with punctuation
         * @return Punctuation without words
         */
        @JvmStatic
        fun stripWords(vararg words: String): Array<String> {
            if (words.isEmpty()) {
                return arrayOf()
            }

            val retcode = arrayOfNulls<String>(words.size + 1)

            // The first bit of punctuation is what comes in front of the first word
            val first = firstLetter(words[0])
            if (first == 0) {
                retcode[0] = ""
            } else {
                retcode[0] = words[0].substring(0, first)
            }

            // The rest of the words
            for (i in 1 until words.size) {
                retcode[i] = stripWords(words[i - 1], words[i])
            }

            // The last bit of punctuation is what comes at the end of the last word
            val last = lastLetter(words[words.size - 1])
            if (last == words[words.size - 1].length) {
                retcode[words.size] = ""
            } else {
                retcode[words.size] = words[words.size - 1].substring(last + 1)
            }

            @Suppress("UNCHECKED_CAST")
            return retcode as Array<String>
        }

        /**
         * Remove the punctuation from the ends of the word. The special case is
         * that if the first word ends "--" and the last word has no punctuation at
         * the beginning, then the answer is "--" and not "-- ". We miss out the
         * space because "--" is a special separator.
         * 
         * @param first
         *            The word to grab the punctuation from the end of
         * @param last
         *            The word to grab the punctuation from the start of
         * @return The end of the first, a space, and the end of the first
         */
        @JvmStatic
        fun stripWords(first: String, last: String): String {
            val init1 = first.substring(lastLetter(first) + 1)
            val init2 = last.substring(0, firstLetter(last))

            return init1 + init2
        }

        /**
         * From a sentence get a list of words (in original order) without any
         * punctuation, and all in lower case.
         * 
         * @param aSentence
         *            The string to parse.
         * @return The words split up as an array
         */
        @JvmStatic
        fun getWords(aSentence: String): Array<String> {
            var sentence = aSentence
            // First there are some things we regard as word delimiters even if
            // they are not near space. Note that "-" should not be in this list
            // because words like abel-beth-maiacha contain them.
            sentence = sentence.replace("--", " ")
            sentence = sentence.replace('.', ' ')
            sentence = sentence.replace('!', ' ')
            sentence = sentence.replace('?', ' ')
            sentence = sentence.replace(':', ' ')
            sentence = sentence.replace(';', ' ')
            sentence = sentence.replace('"', ' ')
            sentence = sentence.replace('\'', ' ')
            sentence = sentence.replace('(', ' ')
            sentence = sentence.replace(')', ' ')

            val words = StringUtil.split(sentence, " ")
            val retcode = arrayOfNulls<String>(words.size)

            // Remove the punctuation from the ends of the words.
            for (i in words.indices) {
                retcode[i] = stripPunctuationWord(words[i]).lowercase(Locale.ENGLISH)
            }

            @Suppress("UNCHECKED_CAST")
            return retcode as Array<String>
        }

        /**
         * Remove the punctuation from the ends of the word
         * 
         * @param word
         *            Word with punctuation
         * @return Word without punctuation
         */
        @JvmStatic
        fun stripPunctuationWord(word: String): String {
            val first = firstLetter(word)
            val last = lastLetter(word) + 1

            if (first > last) {
                return word
            }

            return word.substring(first, last)
        }

        /**
         * Where is the first letter in this word
         * 
         * @param word
         *            The word to search for letters
         * @return The offset of the first letter
         */
        @JvmStatic
        fun firstLetter(word: String): Int {
            var first = 0

            while (first < word.length) {
                val c = word[first]
                if (Character.isLetterOrDigit(c)) {
                    break
                }
                first++
            }

            return first
        }

        /**
         * Where is the last letter in this word
         * 
         * @param word
         *            The word to search for letters
         * @return The offset of the last letter
         */
        @JvmStatic
        fun lastLetter(word: String): Int {
            var last = word.length - 1

            while (last >= 0) {
                val c = word[last]
                if (Character.isLetterOrDigit(c)) {
                    break
                }
                last--
            }

            return last
        }
    }
}
