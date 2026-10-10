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
 * © CrossWire Bible Society, 2014 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException


import org.crosswire.common.config.ConfigException
import org.crosswire.jsword.versification.system.Versifications

/**
 * JUnit Test
 *
 * @author Chris Burrell
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br>
 * The copyright to this program is held by its authors.
 */
class FileVersificationMappingTest {
    @Test
    fun testVersifications(){
        for (v11nName in Versifications.instance().iterator().asSequence().asIterable()) {
            var versification: Versification = Versifications.instance().getVersification(v11nName)!!
            try {
                var m: FileVersificationMapping = FileVersificationMapping(versification)
                var mapper: VersificationToKJVMapper = VersificationToKJVMapper(versification, m)
                assertFalse(mapper.hasErrors(), "Failed to parse " + v11nName)
            } catch (mre: MissingResourceException) {
                // This is allowed
                // ignore, as this basically means we don't have mappings...
            }
        }
    }
}
