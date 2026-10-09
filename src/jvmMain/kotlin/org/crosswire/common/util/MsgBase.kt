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

import java.util.MissingResourceException

import org.crosswire.common.icu.NumberShaper

/**
 * A base class for implementing type safe internationalization (i18n) that is
 * easy for most cases. Bundles come from [ResourceBundles] (embedded resources),
 * messages are formatted by [MessageFormatLite].
 *
 * Locale notes: the bundle follows the provider locale ([KswordLocale.current]) while number formatting follows
 * [KswordLocale.default], i.e. `Locale.getDefault()` rather than `Locale.getDefault(FORMAT)`. The conversion to
 * [KLocale] drops script and `-u-` extensions, so e.g. a `zh-Hant` or `-u-nu-arab` locale formats as its
 * language/country/variant only.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 * @param bundleName the resource base name, e.g. `JSMsg`; empty derives it from the runtime class's short name
 */
open class MsgBase protected constructor(private val bundleName: String) {
    /** Create a MsgBase object whose bundle name is the short name of the runtime class. */
    protected constructor() : this("")

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
        return shaper.shape(MessageFormatLite.format(rawMessage, KswordLocale.default(), params))!!
    }

    private fun obtainString(key: String): String {
        // A missing bundle (e.g. FTPMsg, whose properties sit in a package directory) degrades to the key, as before.
        val value = try { localisedResources().getString(key) } catch (ex: MissingResourceException) { null }
        if (value == null) {
            log.error("Missing resource: Locale={} name={} package={}", KswordLocale.current(), key, javaClass.name)
            return key
        }
        return value
    }

    private fun localisedResources(): KBundle {
        val name = bundleName.ifEmpty { ClassUtil.getShortClassName(javaClass.name) }
        val locale = KswordLocale.current()
        // if for some reason there is no bundle, then we can only do our best, which is to return the English Locale.
        return ResourceBundles.get(name, locale) ?: ResourceBundles.get(name, KLocale.ENGLISH) ?: run {
            log.error("Missing resources: Locale={} class={}", locale, javaClass.name)
            throw MissingResourceException("Unable to find the language resources.", javaClass.name, name)
        }
    }

    /** Internationalize numbers  */
    private val shaper: NumberShaper = NumberShaper()

    private companion object {
        /**
         * The log stream
         */
        private val log = LoggerFactory.getLogger(MsgBase::class)
    }
}
