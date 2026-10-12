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
package org.crosswire.common.util


/**
 * An immutable Language by specification. The specifier consists of up to three parts:
 * <ul>
 * <li>LL - An iso639-2 or iso639-3 language code</li>
 * <li>SSSS - A 4-letter iso15924 script code</li>
 * <li>CC - A 2-letter iso3166 country code</li>
 * </ul>
 * Note: This is a subset of the BCP-47 standard.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Language(specification: String?) : Comparable<Language> {
    /**
     * The original specification provided by the user.
     */
    private var given: String? = null

    /**
     * The effective specification.
     */
    private var found: String? = null

    /**
     * The lower case iso639 language code.
     * Assigned by parse() for every specification, so it is never null once constructed.
     */
    private lateinit var languageCode: String

    /**
     * The Title case iso15924 script code.
     */
    private var scriptCode: String? = null

    /**
     * The UPPER case iso3166 country code.
     */
    private var countryCode: String? = null

    /**
     * The name as defined by Languages.
     */
    private var resolvedName: String? = null

    /**
     * Flag to store whether the code is valid.
     */
    private var valid = false
    private var knowsDirection = false
    private var ltor = false

    /**
     * A single language defined by an ISO-639 code. If the code is null or
     * empty then it is considered to be DEFAULT_LANG (that is, English).
     *
     * @param specification
     *            the specifier for the particular language
     */
    init {
        given = specification
        parse(given)
    }

    /**
     * The specification that was given might not be be the one that
     * ultimately gets the name.
     *
     * @return the specification that was originally given.
     */
    open val givenSpecification: String?
        get() = given

    /**
     * The specification that was given might not be be the one that
     * ultimately gets the name.
     *
     * @return the specification that was used to find the name.
     */
    open val foundSpecification: String?
        get() {
            name
            return found
        }

    /**
     * Determine whether this language is valid.
     * <ul>
     * <li>LL - An iso639-2 or iso639-3 language code</li>
     * <li>SSSS - A 4-letter iso15924 script code</li>
     * <li>CC - A 2-letter iso3166 country code</li>
     * </ul>
     *
     * @return true if the language is valid.
     */
    open val isValidLanguage: Boolean
        get() {
            name
            return valid
        }

    /**
     * Get the iso639 language code.
     *
     * @return the code for the language in lower case.
     */
    open val code: String
        get() = languageCode

    /**
     * Get the iso15924 script for the language. May be null.
     *
     * @return the code for the script in Title case.
     */
    open val script: String?
        get() = scriptCode

    /**
     * Get the iso3166 script for the language. May be null.
     *
     * @return the code for the country in UPPER case.
     */
    open val country: String?
        get() = countryCode

    /**
     * Get the localized language name.
     *
     * @return the name of the language
     */
    open val name: String
        get() {
            // Note: This is not quite thread safe. Unless name is volatile.
            // But it will just do the work multiple times.
            if (resolvedName == null) {
                var more = true
                // Code is the ultimate fallback
                var result: String? = languageCode
                var lookup = languageCode

                val sb = StringBuilder()
                // The lookup is as follows.
                // There is always a code
                // If all parts are specified then use that
                if (scriptCode != null && countryCode != null) {
                    sb.append(languageCode)
                    sb.append('-')
                    sb.append(scriptCode)
                    sb.append('-')
                    sb.append(countryCode)
                    lookup = sb.toString()
                    result = Languages.getName(lookup)
                    more = lookup == result
                }

                // If script is specified it has precedence over country
                if (more && scriptCode != null) {
                    sb.setLength(0)
                    sb.append(languageCode)
                    sb.append('-')
                    sb.append(scriptCode)
                    lookup = sb.toString()
                    result = Languages.getName(lookup)
                    more = lookup == result
                }

                // If country was specified, check for that now.
                if (more && countryCode != null) {
                    sb.setLength(0)
                    sb.append(languageCode)
                    sb.append('-')
                    sb.append(countryCode)
                    lookup = sb.toString()
                    result = Languages.getName(lookup)
                    more = lookup == result
                }

                // Now check just the code.
                if (more) {
                    lookup = languageCode
                    result = Languages.getName(lookup)
                    more = lookup == result
                }

                // Oops, the localized lookup failed.
                // See if Java has one.
                if (more) {
                    lookup = languageCode
                    result = platformDisplayLanguage(lookup)
                    more = lookup == result
                }

                // Oops, Java doesn't have a clue
                // Look into our heavy handed listing
                if (more) {
                    lookup = languageCode
                    result = Languages.AllLanguages.getName(lookup)
                    more = lookup == result
                }

                // Oops, didn't find it anywhere. Mark it as invalid.
                if (more) {
                    valid = false
                }
                // now that we are here go with what we last used and got
                found = lookup
                // Assign name last to help with synchronization issues
                resolvedName = result
            }
            return resolvedName!!
        }

    /**
     * Determine whether this language is a Left-to-Right or a Right-to-Left
     * language. If the language has a script, it is used for the determination.
     * Otherwise, check the language.
     * <p>
     * Note: This is problematic. Languages do not have direction.
     * Scripts do. Further, there are over 7000 living languages, many of which
     * are written in Right-to-Left scripts and are not listed here.
     * </p>
     *
     * @return true if the language is Left-to-Right.
     */
    open val isLeftToRight: Boolean
        get() {
            if (!knowsDirection) {
                ltor = !Languages.RtoL.isRtoL(scriptCode, languageCode)
                knowsDirection = true
            }
            return ltor
        }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        if (found == null) {
            name
        }
        return found!!.hashCode()
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || this::class != other::class) {
            return false
        }

        val that = other as Language

        return languageCode == that.languageCode && compareStrings(scriptCode, that.scriptCode) && compareStrings(countryCode, that.countryCode)
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return name
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Language): Int {
        return name.compareTo(other.name)
    }

    /**
     * Split the specification on '-' into 1 to 3 parts.
     *
     * @param spec the specification to parse
     */
    private fun parse(spec: String?) {
        var specification = spec
        if (specification == null) {
            specification = DEFAULT_LANG_CODE
        }

        val len = specification.length

        // It used to be that SWORD modules used x- and X- as a language prefix
        // for minority languages. Now that we have a complete iso639 spec,
        // SWORD does not use it.
        if (len < 2 || specification[0] == '-' || specification[1] == '-') {
            valid = false
            languageCode = UNKNOWN_LANG_CODE
            return
        }

        // Obvious optimization of the most common case: only the language code is given
        if (len <= 3) {
            languageCode = CanonicalUtils.getLanguage(specification, 0, len)!!
        }

        var partLen = 0
        var start = 0
        var split: Int
        split = 2
        while (split < len) {
            val c = specification[split]
            if (c == '-') {
                break
            }
            ++split
        }
        languageCode = CanonicalUtils.getLanguage(specification, start, split)!!
        partLen = split - start
        valid = partLen == 2 || partLen == 3
        start = split + 1

        // Get the second part. It is either a script or a country code
        if (split < len) {
            split = start
            while (split < len) {
                val c = specification[split]
                if (c == '-') {
                    break
                }
                ++split
            }
            partLen = split - start
            if (partLen == 4) {
                scriptCode = CanonicalUtils.getScript(specification, start, split)
            } else if (partLen == 2) {
                countryCode = CanonicalUtils.getCountry(specification, start, split)
            } else {
                valid = false
            }
            start = split + 1
        }

        // Get the third part, if any. It can only be a country code.
        if (countryCode == null && split < len) {
            split = start
            while (split < len) {
                val c = specification[split]
                if (c == '-') {
                    break
                }
                ++split
            }
            partLen = split - start
            if (partLen == 2) {
                countryCode = CanonicalUtils.getCountry(specification, start, split)
            } else {
                valid = false
            }
            start = split + 1
        }

        if (start <= len) {
            valid = false
        }
    }

    /**
     * Equal if both a and b are the same.
     *
     * @param a a string to compare
     * @param b a string to compare
     * @return true if both are the same.
     */
    private fun compareStrings(a: String?, b: String?): Boolean {
        return (a == null && b == null) || (a != null && a == b)
    }

    /**
     * Converts substrings to the canonical representation for language code, script and country.
     */
    private object CanonicalUtils {
        /**
         * The iso639 language code's canonical form is lower case.
         *
         * @param specification
         *            the bcp47 specification of the language
         * @param start
         *            the start of the code
         * @param end
         *            the position of the character following the code
         * @return the canonical representation for the code
         */
        fun getLanguage(specification: String, start: Int, end: Int): String? {

            // An empty string means no work
            if (start == end) {
                return null
            }

            // Avoid construction by analyzing the string
            // to see if it is already LanguageCase.
            // Find the first character that is not LanguageCase
            var first: Int
            first = start
            while (first < end && isLowerASCII(specification[first])) {
                ++first
            }

            // If we get to the end of the string then it is CountryCase
            if (first == end) {
                return specification.substring(start, end)
            }

            // Bummer, we need to do work
            val len = end - start
            val buf = CharArray(len)
            var i = 0
            for (j in start until end) {
                buf[i++] = if (j < first) specification[j] else toLowerASCII(specification[j])
            }
            return String(buf)
        }

        /**
         * The iso3166 country code's canonical form is upper case.
         *
         * @param specification
         *            the bcp47 specification of the language
         * @param start
         *            the start of the code
         * @param end
         *            the position of the character following the code
         * @return the canonical representation for the code
         */
        fun getCountry(specification: String, start: Int, end: Int): String? {

            // An empty string means no work
            if (start == end) {
                return null
            }

            // Avoid construction by analyzing the string
            // to see if it is already CountryCase.
            // Find the first character that is not CountryCase
            var first: Int
            first = start
            while (first < end && isUpperASCII(specification[first])) {
                ++first
            }

            // If we get to the end of the string then it is CountryCase
            if (first == end) {
                return specification.substring(start, end)
            }

            // Bummer, we need to do work
            val len = end - start
            val buf = CharArray(len)
            var i = 0
            for (j in start until end) {
                buf[i++] = if (j < first) specification[j] else toUpperASCII(specification[j])
            }
            return String(buf)
        }

        /**
         * The iso15924 script code's canonical form is title case.
         *
         * @param specification
         *            the bcp47 specification of the language
         * @param start
         *            the start of the code
         * @param end
         *            the position of the character following the code
         * @return the canonical representation for the code
         */
        fun getScript(specification: String, start: Int, end: Int): String? {

            // An empty string means no work
            if (start == end) {
                return null
            }

            // Avoid construction by analyzing the string
            // to see if it is already ScriptCase.
            // Find the first character that is not ScriptCase
            var first = start
            if (isUpperASCII(specification[start])) {
                first = start + 1
                while (first < end && isLowerASCII(specification[first])) {
                    ++first
                }

                // If we get to the end of the string then it is ScriptCase
                if (first == end) {
                    return specification.substring(start, end)
                }
            }

            // Bummer, we need to do work.
            val len = end - start
            val buf = CharArray(len)
            buf[0] = if (first == start) toUpperASCII(specification[first]) else specification[first]
            var i = 1
            for (j in start + 1 until end) {
                buf[i++] = if (j < first) specification[j] else toLowerASCII(specification[j])
            }
            return String(buf)
        }

        /**
         * Determine whether the character is one of A-Z.
         *
         * @param c the character to examine
         * @return true if it is in A-Z
         */
        private fun isUpperASCII(c: Char): Boolean {
            return c >= 'A' && c <= 'Z'
        }

        /**
         * Determine whether the character is one of a-z.
         *
         * @param c the character to examine
         * @return true if it is in a-z
         */
        private fun isLowerASCII(c: Char): Boolean {
            return c >= 'a' && c <= 'z'
        }

        /**
         * Convert a character, in in a-z to its upper case value, otherwise leave it alone.
         *
         * @param c the character to convert, if in a-z
         * @return the upper case ASCII representation of the character or the character itself.
         */
        private fun toUpperASCII(c: Char): Char {
            return if (isLowerASCII(c)) (c.code - 32).toChar() else c
        }

        /**
         * Convert a character, in in A-Z to its lower case value, otherwise leave it alone.
         *
         * @param c the character to convert, if in A-Z
         * @return the lower case ASCII representation of the character or the character itself.
         */
        private fun toLowerASCII(c: Char): Char {
            return if (isUpperASCII(c)) (c.code + 32).toChar() else c
        }
    }

    companion object {
        /**
         * The default language code is en for English.
         */
        const val DEFAULT_LANG_CODE: String = "en"

        /**
         * The language code for invalid language specifications is und for Undetermined.
         */
        const val UNKNOWN_LANG_CODE: String = "und"

        /**
         * The default language is English.
         */
        @JvmField
        val DEFAULT_LANG: Language = Language(DEFAULT_LANG_CODE)
    }
}
