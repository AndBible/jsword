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
package org.crosswire.common.icu

import org.crosswire.common.util.KLocale
import org.crosswire.jsword.internationalisation.LocaleProvider
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JUnit test of NumberShaper.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class NumberShaperTest {
    private val savedProvider: LocaleProvider = LocaleProviderManager.getLocaleProvider()

    @AfterTest
    fun tearDown() {
        LocaleProviderManager.setLocaleProvider(savedProvider)
    }

    @Test
    fun testShape() {
        //This presumably prevents tests from running in parallel, but can't do much about this, given all the statics.
        LocaleProviderManager.setLocaleProvider(object : LocaleProvider {
            override fun getUserLocale(): KLocale = KLocale("fa", "", "")
        })

        //test the number shaper
        val shaper = NumberShaper()
        assertEquals(EASTERN_ARABIC_DIGITS, shaper.shape(EUROPEAN_DIGITS))
        // Note: the following depends upon whether icu is on the classpath
        if (shaper.canUnshape()) {
            assertEquals(EUROPEAN_DIGITS, shaper.unshape(EASTERN_ARABIC_DIGITS))
        } else {
            assertEquals(EASTERN_ARABIC_DIGITS, shaper.unshape(EASTERN_ARABIC_DIGITS))
        }
    }

    companion object {
        private const val EUROPEAN_DIGITS = "0123456789"
        private const val EASTERN_ARABIC_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    }

}
