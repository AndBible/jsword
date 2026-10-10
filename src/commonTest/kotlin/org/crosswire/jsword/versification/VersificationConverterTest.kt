package org.crosswire.jsword.versification

import org.crosswire.jsword.versification.*
import org.crosswire.jsword.versification.system.*
import kotlin.test.*
import org.crosswire.common.util.KLocale
import org.crosswire.common.util.MissingResourceException
import okio.IOException

import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.system.SystemKJVA
import org.crosswire.jsword.versification.system.Versifications



/**
 * @author Martin Denham [mjdenham at gmail dot com]
 */
class VersificationConverterTest {

    private lateinit var versificationConverter: VersificationConverter

    private var SEGOND_JOHN_3_16: Verse = Verse(TestData.SEGOND, BibleBook.JOHN, 3, 16)

    @BeforeTest
    fun setup() {
        versificationConverter = VersificationConverter()
    }

    @Test
    fun isConvertibleTo(){
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.KJV))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.NRSV))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.GERMAN))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.LUTHER))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.KJVA))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.SYNODAL))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.SYNODAL_PROT))
        assertEquals(true, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.VULGATE))
        // contain no NT books
        assertEquals(false, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.MT))
        assertEquals(false, versificationConverter.isConvertibleTo(SEGOND_JOHN_3_16, TestData.LENINGRAD))
    }

    @Test
    fun testConvert(){
        // Try to convert all verses in all versifications to KJVA
        var iterator: Iterator<String> = Versifications.instance().iterator()
        var lastKjvOrdinal: Int = TestData.KJVA.getLastVerse().ordinal

        while (iterator.hasNext()) {
            var v11n: Versification = Versifications.instance().getVersification(iterator.next())!!
            for (i in 0..v11n.getLastVerse().ordinal) {
                var v: Verse = Verse(v11n, i)
                var vKjv: Verse = versificationConverter.convert(v, TestData.KJVA)
                var newOrdinal: Int = vKjv.ordinal
                assertTrue(newOrdinal >= 0 && newOrdinal <= lastKjvOrdinal, "valid ordinal " + newOrdinal)
            }
        }
    }
    @Test
    fun testKJVAOrdinalRange(){
        // Make sure that KJVA versification range does not change without notice.

        var KJVA: Versification = Versifications.instance().getVersification(SystemKJVA.V11N_NAME)!!
        var lastVerse: Verse = KJVA.getLastVerse()
        var lastKjvOrdinal: Int = lastVerse.ordinal

        assertEquals(38272, lastKjvOrdinal)
        assertEquals("Rev.22.21", lastVerse.getOsisID())
        assertEquals(0, KJVA.allVerses.start.ordinal)
        assertEquals(38272, KJVA.allVerses.end.ordinal)
    }
}
