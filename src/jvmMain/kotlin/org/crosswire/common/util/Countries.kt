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
import org.crosswire.jsword.internationalisation.LocaleProviderManager

/**
 * A utility class that converts ISO-3166 codes or locales to their "friendly"
 * country name.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
// An object (private constructor, as the Java utility class had).
object Countries {
    /**
     * Determine whether the country code is valid. The code is valid if it is
     * null or empty. The code is valid if it is in iso3166.properties. If a
     * locale is used for the iso3166Code, it will use the part after the '_'.
     * Thus, this code does not support dialects.
     *
     * @param iso3166Code the country code
     * @return true if the country is valid.
     */
    @JvmStatic
    fun isValidCountry(iso3166Code: String?): Boolean {
        val lookup = iso3166Code
        if (lookup == null || lookup.length == 0) {
            return true
        }

        if (lookup.indexOf('_') != -1) {
            val locale = StringUtil.split(lookup, '_')
            return isValidCountry(locale[1])
        }

        if (lookup.length > 2) {
            return false
        }

        return getLocalisedCountries().getString(lookup) != null
    }

    /**
     * Get the country name from the country code. If the code is null or empty
     * then it is considered to be DEFAULT_COUNTRY_CODE (that is, US).
     * Otherwise, it will generate a log message and return unknown. If a locale
     * is used for the iso3166Code, it will use the part before the '_'. Thus,
     * this code does not support dialects, except as found in the iso3166.
     *
     * @param iso3166Code the country code
     * @return the name of the country
     */
    @JvmStatic
    fun getCountry(iso3166Code: String?): String {
        val lookup = iso3166Code
        if (lookup == null || lookup.length == 0) {
            return getCountry(DEFAULT_COUNTRY_CODE)
        }

        if (lookup.indexOf('_') != -1) {
            val locale = StringUtil.split(lookup, '_')
            return getCountry(locale[1])
        }

        return getLocalisedCountries().getString(lookup) ?: getCountry(UNKNOWN_COUNTRY_CODE)
    }

    /**
     * Gets the localised countries.
     *
     * @return the localised countries
     */
    private fun getLocalisedCountries(): KBundle {
        return ResourceBundles.get("iso3166", KswordLocale.current())
            ?: throw MissingResourceException("Can't find bundle for base name iso3166", "iso3166", "")
    }

    const val DEFAULT_COUNTRY_CODE: String = "US"
    private const val UNKNOWN_COUNTRY_CODE: String = "XX"
}
