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

import java.io.IOException
import java.util.HashMap
import java.util.HashSet
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * A utility class that converts bcp-47 codes as supported by {@link Language} to their
 * localized language name.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
// An object, not a companion: initLanguages() resolves its class loader through CWClassLoader.instance(), which
// identifies the calling class from the stack, so the caller must be Languages itself (not Languages$Companion).
// Being an object also makes the constructor private, as the Java utility class had.
object Languages {
    /**
     * Get the language name for the BCP-47 specification of the language.
     *
     * @param code the BCP-47 specification for the language
     * @return the name of the language
     */
    @JvmStatic
    fun getName(code: String?): String? {
        // Returning the code is the fallback for lookup
        var name = code
        try {
            val langs = getLocalisedCommonLanguages()
            if (langs != null) {
                name = langs.getString(code)
            }
        } catch (e: MissingResourceException) {
            // This is allowed
        }
        return name
    }

    /**
     * Gets the localised common languages. Caching here, is done to prevent extra logging
     * happening every time we miss the iso639 ResourceBundle
     * and end up having to lookup the iso639full
     *
     * @return the localised common languages
     */
    private fun getLocalisedCommonLanguages(): ResourceBundle? {
        val locale = LocaleProviderManager.getLocale()
        var langs = localisedCommonLanguages.get(locale)
        if (langs == null) {
            synchronized(Languages::class.java) {
                langs = localisedCommonLanguages.get(locale)
                if (langs == null) {
                    langs = initLanguages(locale)
                    if (langs != null) {
                        localisedCommonLanguages.put(locale, langs!!)
                    }
                }
            }
        }
        return langs
    }

    private fun initLanguages(locale: Locale): ResourceBundle? {
        try {
            return ResourceBundle.getBundle("iso639", locale, CWClassLoader.instance())
        } catch (e: MissingResourceException) {
            log.info("Unable to find language in iso639 bundle", e)
        }
        return null
    }

    /**
     * Provide a fallback lookup against a huge list of all languages.
     * The basic list has a few hundred languages. The full list has
     * over 7000. As a fallback, this file is not internationalized.
     */
    // An object (private constructor, as in Java); its init block calls ResourceUtil.getProperties(String), which
    // identifies the calling class from the stack, so the caller must be Languages$AllLanguages.
    object AllLanguages {
        /**
         * Get the language name for the code. If the language name is not known
         * then return the code.
         *
         * @param languageCode the language code
         * @return the name for the language.
         */
        @JvmStatic
        fun getName(languageCode: String?): String? {
            if (instance != null) {
                // A null code finds nothing (the map has no null key)
                val name = if (languageCode == null) null else instance!!.get(languageCode)
                if (name != null) {
                    return name
                }
            }
            return languageCode
        }

        /**
         * Do lazy loading of the huge file of languages.
         * Note: It is OK for it not to be present.
         */
        private var instance: PropertyMap? = null

        init {
            try {
                instance = ResourceUtil.getProperties("iso639full")
                log.debug("Loading iso639full.properties file")
            } catch (e: IOException) {
                log.info("Unable to load iso639full.properties", e)
            }
        }
    }

    /**
     * Provide a fallback lookup against a huge list of all languages.
     * The basic list has a few hundred languages. The full list has
     * over 7000. As a fallback, this file is not internationalized.
     */
    object RtoL {
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
         * @param script the iso15924 script code, must be in Title case
         * @param lang the iso639 language code, must be lower case
         * @return true if the language is Right-to-Left
         */
        @JvmStatic
        fun isRtoL(script: String?, lang: String?): Boolean {
            if (script != null) {
                return rtol.contains(script)
            }
            if (lang != null) {
                return rtol.contains(lang)
            }
            return false
        }

        /**
         * Do lazy loading of the huge file of languages.
         * Note: It is OK for it not to be present.
         */
        private val rtol: MutableSet<String> = HashSet()

        /**
         * load RtoL data
         */
        init {
            try {
                val index = ResourceUtil.getResource(Translations::class.java, "rtol.txt")
                val list = NetUtil.listByIndexFile(NetUtil.toURI(index)!!)
                log.debug("Loading iso639full.properties file")
                for (i in list.indices) {
                    rtol.add(list[i])
                }
            } catch (ex: IOException) {
                log.info("Unable to load rtol.txt", ex)
            }
        }
    }

    private val localisedCommonLanguages: MutableMap<Locale, ResourceBundle> = HashMap()

    /**
     * The log stream
     */
    // was protected static (package access in Java): Kotlin has no protected members in an object, so public
    @JvmField
    val log: Logger = LoggerFactory.getLogger(Languages::class.java)
}
