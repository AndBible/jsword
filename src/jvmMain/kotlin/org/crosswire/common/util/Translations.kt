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
import java.util.Locale
import org.crosswire.common.config.ChoiceFactory
import org.slf4j.LoggerFactory

/**
 * Translations provides a list of locales that BibleDesktop has been translated
 * into.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class Translations
/**
 * Singleton classes have private constructors.
 */
private constructor() {
    /**
     * The translation that BibleDesktop should use.
     */
    private var translation: String? = null

    /**
     * List of available translations.
     */
    private var translations: Array<String>? = null

    init {
        initTranslation()
    }

    // The body of the Java constructor, a function so that its early returns stay returns
    private fun initTranslation() {
        try {
            loadSupportedTranslations()
            val props = ResourceUtil.getProperties(javaClass)
            translation = props.get(TRANSLATION_KEY)
            val current = translation
            if (current == null || current.length == 0) {
                // check for a match against language and country
                // This pertains to zh_TW and zh_CN
                for (i in translations!!.indices) {
                    val supportedLocale = Locale(translations!![i])
                    if (supportedLocale.language == originalLocale.language && supportedLocale.country == originalLocale.country) {
                        translation = translations!![i]
                        return
                    }
                }

                // check for a match against just language
                for (i in translations!!.indices) {
                    val supportedLocale = Locale(translations!![i])
                    if (supportedLocale.language == originalLocale.language) {
                        translation = translations!![i]
                        return
                    }
                }

                // if we don't have a matching locale then just use the default.
                translation = DEFAULT_TRANSLATION
            }
        } catch (e: IOException) {
            translation = DEFAULT_TRANSLATION
        }
    }

    /**
     * Gets a listing of all the translations that Bible Desktop supports.
     *
     * @return an string array of translations in locale friendly names.
     */
    val supported: PropertyMap
        get() {
            loadSupportedTranslations()

            // I18N(DMS) Collate these according to the current locale, putting the
            // current locale's locale first.
            val names = PropertyMap()

            for (i in translations!!.indices) {
                names.put(translations!![i], toString(translations!![i]))
            }

            return names
        }

    /**
     * Get the locale for the current translation.
     *
     * @return the translation's locale
     */
    val currentLocale: Locale
        get() {
            // If there is no particular translation, then return the default
            // locale.
            val current = translation
            if (current == null || DEFAULT_TRANSLATION == current) {
                return DEFAULT_LOCALE
            }

            // If the local consists of a language and a country then use both
            if (current.indexOf('_') != -1) {
                val locale = StringUtil.split(current, '_')
                return Locale(locale[0], locale[1])
            }

            // otherwise just use the country.
            return Locale(current)
        }

    // getCurrent() and setCurrent(String) are kept as functions: the setter accepts null (the Java code
    // compared it with equals) while the getter's type is a non-null String.

    /**
     * Get the current translation as a human readable string.
     *
     * @return the current translation
     */
    fun getCurrent(): String {
        return toString(translation!!)
    }

    /**
     * Set the current translation, using human readable string.
     *
     * @param newTranslation
     *            the translation to use
     */
    fun setCurrent(newTranslation: String?) {
        var found = DEFAULT_TRANSLATION
        var currentTranslation = ""
        for (i in translations!!.indices) {
            val trans = translations!![i]
            currentTranslation = toString(translation!!)

            if (trans == newTranslation || currentTranslation == newTranslation) {
                found = trans
                break
            }
        }

        try {
            translation = found
            val props = PropertyMap()
            if (DEFAULT_TRANSLATION != translation) {
                props.put(TRANSLATION_KEY, translation!!)
            }

            val outputURI = CWProject.instance().getWritableURI(javaClass.name, FileUtil.EXTENSION_PROPERTIES)
            NetUtil.storeProperties(props, outputURI!!, "BibleDesktop UI Translation")
        } catch (ex: IOException) {
            log.error("Failed to save BibleDesktop UI Translation", ex)
        }
    }

    /**
     * Set the locale for the program to the one the user has selected. But
     * don't set it to the default translation, so that the user's actual
     * locale, is used for Bible book names.
     *
     * This only makes sense after config has called setCurrentTranslation.
     */
    fun setLocale() {
        Locale.setDefault(currentLocale)
    }

    /**
     * Register this class with the common config engine.
     */
    fun register() {
        ChoiceFactory.getDataMap().put(TRANSLATION_KEY, getSupportedTranslations())
    }

    /**
     * Get a list of the supported translations
     */
    private fun loadSupportedTranslations() {
        if (translations == null) {
            try {
                val index = ResourceUtil.getResource(Translations::class.java, "translations.txt")
                translations = NetUtil.listByIndexFile(NetUtil.toURI(index)!!)
            } catch (ex: IOException) {
                translations = arrayOf()
            }
        }
    }

    fun toString(translationCode: String): String {
        val currentTranslation = StringBuilder(Languages.getName(translationCode))

        if (translationCode.indexOf('_') != -1) {
            val locale = StringUtil.split(translationCode, '_')
            currentTranslation.append(", ")
            currentTranslation.append(Countries.getCountry(locale[1]))
        }

        return currentTranslation.toString()
    }

    companion object {
        /**
         * All access to Translations is through this single instance.
         *
         * @return the singleton instance
         */
        @JvmStatic
        fun instance(): Translations {
            return instance
        }

        /**
         * Get the current translation as a human readable string.
         *
         * @return the current translation
         */
        @JvmStatic
        fun getCurrentTranslation(): String {
            return Translations.instance().getCurrent()
        }

        /**
         * Set the current translation, using human readable string.
         *
         * @param newTranslation
         *            the translation to use
         */
        @JvmStatic
        fun setCurrentTranslation(newTranslation: String?) {
            Translations.instance().setCurrent(newTranslation)
        }

        /**
         * Gets a listing of all the translations that Bible Desktop supports.
         *
         * @return an string array of translations in locale friendly names.
         */
        @JvmStatic
        fun getSupportedTranslations(): Map<String, String> {
            return Translations.instance().supported
        }

        /**
         * The key used in config.xml
         */
        private const val TRANSLATION_KEY: String = "translation-codes"

        /**
         * The default translation, if the user has not chosen anything else.
         */
        const val DEFAULT_TRANSLATION: String = "en"

        /**
         * The default Locale, it the user has not chosen anything else.
         */
        @JvmField
        val DEFAULT_LOCALE: Locale = Locale.ENGLISH

        /**
         * The locale that the program starts with. This needs to precede
         * "instance."
         */
        private val originalLocale: Locale = Locale.getDefault()

        private val instance: Translations = Translations()

        /**
         * The log stream
         */
        private val log: org.slf4j.Logger = LoggerFactory.getLogger(Translations::class.java)
    }
}
