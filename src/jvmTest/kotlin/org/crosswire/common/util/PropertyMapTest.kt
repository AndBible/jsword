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
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.MissingResourceException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * JUnit Test
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class PropertyMapTest {
    @BeforeTest
    fun setUp() {
        m = PropertyMap()
    }

    @Test
    fun testBasic() {
        assertEquals(null, m.get("diddly"), "Test for an element not present")
        m.put("diddly", "squat")
        assertEquals("squat", m.get("diddly"), "Test for a present element")
    }

    @Test
    fun testLoad() {
        var input: InputStream? = null
        try {
            input = ResourceUtil.getResourceAsStream(javaClass, "PropertyMap.properties")
            PropertyMapJvm.load(m, input)
        } catch (e: MissingResourceException) {
            fail("Unable to find PropertyMap.properties")
        } catch (e: IOException) {
            fail("Unable to read PropertyMap.properties")
        } finally {
            IOUtil.close(input)
        }
    }

    @Test
    fun testRead() {
        var input: InputStream? = null
        try {
            input = ResourceUtil.getResourceAsStream(javaClass, "PropertyMap.properties")
            PropertyMapJvm.load(m, input)
        } catch (e: MissingResourceException) {
            fail("Unable to find PropertyMap.properties")
        } catch (e: IOException) {
            fail("Unable to read PropertyMap.properties")
        } finally {
            IOUtil.close(input)
        }
        assertEquals(1, m.size, "Only one element was in the file")
        assertEquals("I am", m.get("Here"), "Test that the load worked")
    }

    @Test
    fun testSave() {
        m.put("Here", "Am I")
        val uri: URI = CWProject.instance().getWritableURI("test", FileUtil.EXTENSION_PROPERTIES)!!
        var out: OutputStream? = null
        try {
            out = NetUtilJvm.getOutputStream(uri)
            PropertyMapJvm.store(m, out, "Test data can be deleted at any time")
        } catch (e: IOException) {
            fail("Unable to save test.properties")
        } finally {
            IOUtil.close(out)
        }
    }

    @Test
    fun testReload() {
        assertEquals(0, m.size, "The map is empty")

        var input: InputStream? = null
        val uri: URI = CWProject.instance().getWritableURI("test", FileUtil.EXTENSION_PROPERTIES)!!
        try {
            input = NetUtilJvm.getInputStream(uri)
            PropertyMapJvm.load(m, input)
        } catch (e: IOException) {
            fail("Unable to reload test.properties")
        } finally {
            IOUtil.close(input)
        }
        assertEquals(1, m.size, "Only one element was in the file")
        assertEquals("Am I", m.get("Here"), "Test that the save and reload worked")
    }

    private lateinit var m: PropertyMap
}
