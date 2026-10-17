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
 * © CrossWire Bible Society, 2009 - 2016
 *
 */
package org.crosswire.jsword.book.sword

import org.crosswire.common.util.CWProject
import org.crosswire.common.util.IniSection
import org.crosswire.common.util.IniSectionJvm
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.MetaDataLocator
import org.junit.BeforeClass
import java.io.File
import java.io.IOException
import java.net.URI
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * A Raw File format that allows for each verse to have it's own storage.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author mbergmann
 * @author DM Smith
 */
class SwordBookMetaDataTest {
    private val configFile = File("testconfig.conf")
    private var swordBookMetaData: SwordBookMetaData? = null

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {
        // required for setting ui properties
        CWProject.instance().setFrontendName("jsword-app")

        swordBookMetaData = createTestSwordBookMetaData()
    }

    @AfterTest
    @Throws(Exception::class)
    fun tearDown() {
        configFile.delete()
    }

    @Test
    fun testFrontEndSwordBookMetadataSetup() {
        // From the code: "The write location supersedes the read location" so don't worry about getReadableFrontendProjectDir (which is null)
        assertNotNull(CWProject.instance().writableFrontendProjectDir)
    }

    @Test
    fun testPropertiesAccessors() {
        assertNotNull(swordBookMetaData)
        assertEquals("MyNewBook", swordBookMetaData!!.name)
        assertEquals("TestBook", swordBookMetaData!!.initials)
        assertNotNull(swordBookMetaData!!.language)
        assertEquals("de", swordBookMetaData!!.language.code)
    }

    @Test
    fun testPutThenGetProperty() {
        val swordBookMetaData = swordBookMetaData!!
        // simple jsword property
        swordBookMetaData.putProperty("jswordkey", "jswordvalue")
        assertEquals("jswordvalue", swordBookMetaData.getProperty("jswordkey"))

        // UI property
        swordBookMetaData.putProperty("uikey", "uivalue", true)
        assertEquals("uivalue", swordBookMetaData.getProperty("uikey"))

        // Transient property
        swordBookMetaData.putProperty("transientkey", "transientvalue", MetaDataLocator.TRANSIENT)
        assertEquals("transientvalue", swordBookMetaData.getProperty("transientkey"))
    }

    @Test
    @Throws(Exception::class)
    fun testPropertyPersistence() {
        val swordBookMetaData = swordBookMetaData!!
        swordBookMetaData.putProperty("jswordkey", "jswordvalue")
        swordBookMetaData.putProperty("uikey", "uivalue", true)
        swordBookMetaData.putProperty("transientkey", "transientvalue", MetaDataLocator.TRANSIENT)

        // jsword and ui properties should be persisted, and therefore restored into another SwordBookMetaData but not transient properties
        val anotherSwordBookMetaData = createTestSwordBookMetaData()

        assertEquals("jswordvalue", anotherSwordBookMetaData.getProperty("jswordkey"))
        assertEquals("uivalue", anotherSwordBookMetaData.getProperty("uikey"))
        assertEquals(null, anotherSwordBookMetaData.getProperty("transientkey"))
    }

    private fun createTestSwordBookMetaData(): SwordBookMetaData {
        val modName = "TestBook"
        val table = IniSection(modName)
        table.add(BookMetaData.KEY_LANG, "de")
        table.add(SwordBookMetaData.KEY_DESCRIPTION, "MyNewBook")
        table.add(SwordBookMetaData.KEY_MOD_DRV, "RawFiles")
        table.add(SwordBookMetaData.KEY_DATA_PATH, "test")
        table.add(SwordBookMetaData.KEY_ENCODING, "UTF-8")
        try {
            IniSectionJvm.save(table, configFile, "UTF-8")
        } catch (e: IOException) {
            println(e.message)
        }

        swordBookMetaData = SwordBookMetaDataJvm.create(configFile, URI("file:///tmp"))

        return swordBookMetaData!!
    }

    companion object {
        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun classSetup() {
            // CWProject is a static that has probably already been initialised without a FrontEnd so forcefully reset it
            val resetMethod = CWProject::class.java.getDeclaredMethod("reset")
            resetMethod.isAccessible = true
            resetMethod.invoke(CWProject::class.java)
        }
    }
}
