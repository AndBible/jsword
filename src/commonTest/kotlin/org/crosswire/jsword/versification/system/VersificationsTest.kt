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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.versification.system

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException


import org.crosswire.jsword.versification.Versification


/**
 * JUnit test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class VersificationsTest {
    @Test
    fun testFluff() {
        var v11n: Versification? = null
        var iter: Iterator<String> = Versifications.instance().iterator()
        while (iter.hasNext()) {
            var name: String = iter.next()
            v11n = Versifications.instance().getVersification(name)!!
            assertTrue(Versifications.instance().isDefined(name), "Predefined v11n is known")
            assertEquals(name, v11n.name, "Name matches")
            assertTrue(v11n.maximumOrdinal() > 0, "v11n is not empty")
        }
    }
}
