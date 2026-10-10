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


import org.crosswire.jsword.passage.OsisParser
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.RangedPassage
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.system.SystemCatholic
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.Versifications

/**
 * Some tests based on the principles outlined in the Javadoc of {@link VersificationToKJVMapper }
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Chris Burrell
 */
class VersificationToKJVMapperTest {
    private var properties: FileVersificationMapping = FileVersificationMapping()
    private lateinit var mapper: VersificationToKJVMapper
    private var osisParser: OsisParser = OsisParser()

    @Test
    fun testSimpleMapping() {
        addProperty("Gen.1.1", "Gen.1.2")
        init()
        assertEquals("Gen.1.2", map("Gen.1.1"))
        assertEquals("Gen.1.1", unmap("Gen.1.2"))
    }

    @Test
    fun testTwoLeftMappings() {
        addProperty("Gen.1.1", "Gen.1.1")
        addProperty("Gen.1.2", "Gen.1.1")
        init()
        //map always go to 1.1
        assertEquals("Gen.1.1", map("Gen.1.1"))
        assertEquals("Gen.1.1", map("Gen.1.2"))
        assertEquals("Gen.1.1-Gen.1.2", unmap("Gen.1.1"))
    }

    @Test
    fun testLeftRangeMappings() {
        addProperty("Gen.1.1-Gen.1.2", "Gen.1.1")
        init()
        assertEquals("Gen.1.1", map("Gen.1.1"))
        assertEquals("Gen.1.1", map("Gen.1.2"))
        assertEquals("Gen.1.1-Gen.1.2", unmap("Gen.1.1"))
    }

    @Test
    fun testTwoRightMappings() {
        addProperty("Gen.1.1", "Gen.1.1")
        addProperty("Gen.1.1", "Gen.1.2")
        init()
        assertEquals("Gen.1.1-Gen.1.2", map("Gen.1.1"))
    }

    @Test
    fun testRightRangeMappings() {
        addProperty("Gen.1.1", "Gen.1.1-Gen.1.2")
        init()
        assertEquals("Gen.1.1-Gen.1.2", map("Gen.1.1"))
        assertEquals("Gen.1.1", unmap("Gen.1.1"))
        assertEquals("Gen.1.1", unmap("Gen.1.2"))
    }


    @Test
    fun testMissingMapping() {
        init()
        assertEquals("Gen.1.1", map("Gen.1.1"))
        assertEquals("Gen.1.1", unmap("Gen.1.1"))
    }

    @Test
    fun testRangeToRange() {
        addProperty("Gen.1.1-Gen.1.3", "Gen.1.2-Gen.1.4")
        init()
        assertEquals("Gen.1.2", map("Gen.1.1"))
        assertEquals("Gen.1.3", map("Gen.1.2"))
        assertEquals("Gen.1.4", map("Gen.1.3"))
        assertEquals("Gen.1.1", unmap("Gen.1.2"))
        assertEquals("Gen.1.2", unmap("Gen.1.3"))
        assertEquals("Gen.1.3", unmap("Gen.1.4"))
    }

    @Test
    fun testMappingWithPositiveOffset() {
        addProperty("Gen.1.1-Gen.1.2", "+1")
        init()
        assertEquals("Gen.1.2", map("Gen.1.1"))
        assertEquals("Gen.1.3", map("Gen.1.2"))
        assertEquals("Gen.1.1", unmap("Gen.1.2"))
        assertEquals("Gen.1.2", unmap("Gen.1.3"))
    }

    @Test
    fun testPartsAreReturned() {
        addProperty("Gen.1.1", "Gen.1.3!a")
        addProperty("Gen.1.2", "Gen.1.3!b")
        init()
        assertEquals("Gen.1.3!a", mapToQualifiedKey("Gen.1.1"))
        assertEquals("Gen.1.3!b", mapToQualifiedKey("Gen.1.2"))
        assertEquals("Gen.1.3", map("Gen.1.1"))
        assertEquals("Gen.1.3", map("Gen.1.2"))
        assertEquals("Gen.1.1", unmap("Gen.1.3!a"))
        assertEquals("Gen.1.2", unmap("Gen.1.3!b"))
        assertEquals("Gen.1.1-Gen.1.2", unmap("Gen.1.3"))
    }

    @Test
    fun testExtraUnmappedVersesSingle() {
        addProperty("Dan.3.32", "?StoryOfThreeYoungMen.1.1")
        init()
        assertEquals("?StoryOfThreeYoungMen.1.1", mapToQualifiedKey("Dan.3.32"))
        assertEquals("Dan.3.32", mapper.unmap(QualifiedKey("?StoryOfThreeYoungMen.1.1"))!!.getOsisRef())
    }

    @Test
    fun testExtraUnmappedVersesWithRange() {
        addProperty("Dan.3.31-Dan.3.68", "?StoryOfThreeYoungMen")
        init()
        assertEquals("?StoryOfThreeYoungMen", mapToQualifiedKey("Dan.3.31"))
        assertEquals("?StoryOfThreeYoungMen", mapToQualifiedKey("Dan.3.32"))
        assertEquals("?StoryOfThreeYoungMen", mapToQualifiedKey("Dan.3.45"))
        assertEquals("Dan.3.31-Dan.3.68", mapper.unmap(QualifiedKey("?StoryOfThreeYoungMen"))!!.getOsisRef())
    }

    @Test
    fun testAbsentVerses() {
        addProperty("?", "Gen.1.1")
        init()
        assertEquals("", unmap("Gen.1.1"))
    }

    @Test
    fun testAbsentVersesWithRange() {
        addProperty("?", "Gen.1.1-Gen.1.3")
        init()
        assertEquals("", unmap("Gen.1.1"))
        assertEquals("", unmap("Gen.1.2"))
        assertEquals("", unmap("Gen.1.3"))
        assertEquals("Gen.1.4", unmap("Gen.1.4"))
    }

    /**
     * Initialises the object under test.
     */
    private fun init() {
        mapper = VersificationToKJVMapper(Versifications.instance().getVersification(SystemCatholic.V11N_NAME), properties)
    }

    /**
     * Helper method to add a property to our mappings
     *
     * @param left  the left key
     * @param right the kjv key
     */
    private fun addProperty(left: String, right: String) {
        properties.addProperty(left, right)
    }

    fun map(key: String): String {
        var vr: VerseRange = osisParser.parseOsisRef(NON_KJV, key)!!
        var range: QualifiedKey = QualifiedKey(vr)
        var qualifiedKeys: List<QualifiedKey> = mapper.map(range)
        var vk: Passage = RangedPassage(KJV)
        for (qualifiedKey in qualifiedKeys) {
            //we may bits in here, that don't exist in the KJV
            if (qualifiedKey.key != null) {
                vk.addAll(qualifiedKey.key!!.whole)
            }
        }

        return vk.getOsisRef()
    }

    fun unmap(kjvVerse: String): String {
        return mapper.unmap(QualifiedKey(osisParser.parseOsisRef(KJV, kjvVerse)))!!.getOsisRef()
    }

    fun mapToQualifiedKey(verseKey: String): String {
        var vk: VerseRange = osisParser.parseOsisRef(NON_KJV, verseKey)!!
        var qualifiedVerse: QualifiedKey = QualifiedKey(vk)
        var qualifiedKeys: List<QualifiedKey> = mapper.map(qualifiedVerse)

        var representation: StringBuilder = StringBuilder(128)
        var len: Int = qualifiedKeys.size
        for (i in 0 until len) {
            var qk: QualifiedKey = qualifiedKeys[i]
            if (i > 0) {
                representation.append(' ')
            }
            representation.append(qk)
        }
        return representation.toString()
    }

    private var KJV: Versification = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)!!
    private var NON_KJV: Versification = Versifications.instance().getVersification(SystemCatholic.V11N_NAME)!!
}
