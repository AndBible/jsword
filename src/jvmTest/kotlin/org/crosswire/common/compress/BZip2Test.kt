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
package org.crosswire.common.compress

import java.io.IOException
import java.io.InputStream
import java.util.MissingResourceException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail
import org.crosswire.common.util.PlatformTestUtils
import org.crosswire.common.util.ResourceUtil

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BZip2Test {

    @Test
    fun testCompression() {
        val kjvGenesis: InputStream = try {
            ResourceUtil.getResourceAsStream("kjv_genesis.txt")
        } catch (e: MissingResourceException) {
            fail()
        } catch (e: IOException) {
            fail()
        }
        // new ByteArrayInputStream("ATATAAAFFFF".getBytes()));
        val compressor: Compressor = BZip2(org.apache.commons.compress.utils.IOUtils.toByteArray(kjvGenesis))
        val bosCompressed: ByteArray = try {
            compressor.compress()
        } catch (e: IOException) {
            fail()
        }
        val uncompressor: Compressor = BZip2(bosCompressed)
        val bosUncompressed: ByteArray = try {
            uncompressor.uncompress()
        } catch (e: IOException) {
            fail()
        }
        // The UnsupportedEncodingException catch of the Java original cannot fire for UTF-8,
        // and String(bytes, UTF_8) does not throw, so it is not translated.
        val result = String(bosUncompressed, Charsets.UTF_8)
        assertTrue(
            PlatformTestUtils.startsWith(result, "^          \r?\nThe First Book of Moses, called Genesis"), "round trip ZIP uncompression")
    }
}
