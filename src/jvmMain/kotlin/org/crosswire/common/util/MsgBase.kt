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

import java.text.MessageFormat
import java.util.HashMap
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle

import org.crosswire.common.icu.NumberShaper
import org.crosswire.jsword.internationalisation.LocaleProviderManager

/**
 * A base class for implementing type safe internationalization (i18n) that is
 * easy for most cases.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
open class MsgBase
/**
 * Create a MsgBase object
 */
protected constructor() {
    /**
     * Get the internationalized text, but return key if key is unknown.
     *
     * @param key the format key to internationalize
     * @param params the parameters for the format
     * @return the internationalized text
     */
    open fun lookup(key: String, vararg params: Any?): String {
        var rawMessage = obtainString(key)
        if (params.isEmpty()) {
            return shaper.shape(rawMessage)!!
        }
        //MessageFormat strips off all single apostrophes from the message so replace single quotes with two quotes
        rawMessage = rawMessage.replace("'", "''")
        return shaper.shape(MessageFormat.format(rawMessage, *params))!!
    }

    private fun obtainString(key: String): String {
        try {
            if (getLocalisedResources() != null) {
                return getLocalisedResources().getString(key)
            }
        } catch (ex: MissingResourceException) {
            log.error("Missing resource: Locale={} name={} package={}", LocaleProviderManager.getLocale(), key, javaClass.name)
        }
        return key
    }

    private fun getLocalisedResources(): ResourceBundle {
        val implementingClass: Class<out MsgBase> = javaClass
        val className = implementingClass.name
        val shortClassName = ClassUtil.getShortClassName(className)
        val currentUserLocale = LocaleProviderManager.getLocale()
        val localisedResourceMap = getLazyLocalisedResourceMap(currentUserLocale)
        var resourceBundle: ResourceBundle? = localisedResourceMap[className]
        if (resourceBundle == null) {
            resourceBundle = getResourceBundleForClass(implementingClass, className, shortClassName, currentUserLocale, localisedResourceMap)
        }

        // if for some reason, we are still looking at a null, then we can only do our best, which is to return the English Locale.
        if (resourceBundle == null) {
            resourceBundle = getResourceBundleForClass(implementingClass, className, shortClassName, Locale.ENGLISH, localisedResourceMap)
        }

        //if we're still looking at a null, there is definitely nothing else we can do, so throw an exception
        if (resourceBundle == null) {
            log.error("Missing resources: Locale={} class={}", currentUserLocale, className)
            throw MissingResourceException("Unable to find the language resources.", className, shortClassName)
        }
        return resourceBundle
    }

    /**
     * Gets the resource bundle for a particular class
     *
     * @param implementingClass the implementing class
     * @param className the class name
     * @param shortClassName the short class name
     * @param currentUserLocale the current user locale
     * @param localisedResourceMap the localised resource map
     * @return the resource bundle for class
     */
    private fun getResourceBundleForClass(implementingClass: Class<out MsgBase>, className: String, shortClassName: String, currentUserLocale: Locale, localisedResourceMap: MutableMap<String, ResourceBundle>): ResourceBundle? {
        var resourceBundle: ResourceBundle?
        synchronized(MsgBase::class.java) {
            resourceBundle = localisedResourceMap[className]
            if (resourceBundle == null) {
                try {
                    resourceBundle = ResourceBundle.getBundle(shortClassName, currentUserLocale, CWClassLoader.instance(implementingClass))
                    localisedResourceMap[className] = resourceBundle!!
                } catch (ex: MissingResourceException) {
                    log.warn("Assuming key is the default message {}", className)
                }
            }
        }
        return resourceBundle
    }

    /**
     * Gets the localised resource map, initialising it if it doesn't already exist
     *
     * @param currentUserLocale the current user locale
     * @return the lazy localised resource map
     */
    private fun getLazyLocalisedResourceMap(currentUserLocale: Locale): MutableMap<String, ResourceBundle> {
        var localisedResourceMap = localeToResourceMap[currentUserLocale]
        if (localisedResourceMap == null) {
            synchronized(MsgBase::class.java) {
                localisedResourceMap = localeToResourceMap[currentUserLocale]
                if (localisedResourceMap == null) {
                    localisedResourceMap = HashMap<String, ResourceBundle>(512)
                    localeToResourceMap[currentUserLocale] = localisedResourceMap!!
                }
            }
        }
        return localisedResourceMap!!
    }

    /** Internationalize numbers  */
    private val shaper: NumberShaper = NumberShaper()

    private companion object {
        private val localeToResourceMap: MutableMap<Locale, MutableMap<String, ResourceBundle>> = HashMap()

        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(MsgBase::class)
    }
}
