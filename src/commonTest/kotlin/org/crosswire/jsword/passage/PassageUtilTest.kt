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
package org.crosswire.jsword.passage

import kotlin.test.*

import org.crosswire.jsword.book.CaseType
import org.crosswire.jsword.versification.BookName
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class PassageUtilTest {
    /**
     * How we create Passages
     */
    private var keyf: PassageKeyFactory = PassageKeyFactory.instance()
    /** Control the output of names */
    private var storedCase: CaseType? = null
    private var fullName: Boolean = false
    private var persist: Boolean = false
    private lateinit var v11n: Versification

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#setUp()
     */
    @BeforeTest
    fun setUp(){
        storedCase = BookName.getDefaultCase()
        BookName.setCase(CaseType.SENTENCE)
        fullName = BookName.isFullBookName()
        BookName.setFullBookName(false)
        persist = PassageUtil.isPersistentNaming()
        // AV11N(DMS): Update test to test all V11Ns
        v11n = Versifications.instance().getVersification("KJV")!!
    }

    /*
     * (non-Javadoc)
     *
     * @see junit.framework.TestCase#tearDown()
     */
    @AfterTest
    fun tearDown(){
        BookName.setCase(storedCase)
        BookName.setFullBookName(fullName)
        PassageUtil.setPersistentNaming(persist)
    }

    @Test
    fun testOther() {
        // Need to add:
        /*
         * log("PassageUtil.[s|g]etBlurRestriction()")
         * log("PassageUtil.isValidBlurRestriction()")
         * log("PassageUtil.getBlurRestrictions()")
         * log("PassageUtil.isValidCase()"); log("PassageUtil.getCases()")
         * log("PassageUtil.isValidAccuracy()"); Should there be getAccuracies()
         */
    }

    @Test
    fun testBinaryRepresentation(){
        var genC1V135: Passage = keyf.getKey(v11n, "Gen 1:1, Gen 1:3, Gen 1:5")
        var exo2aTo3b: Passage = keyf.getKey(v11n, "Exo 2:1-10, Exo 3:1-11")
        var gentoRev: Passage = keyf.getKey(v11n, "Gen 1:1-Rev 22:21")
        var hard: Passage = (keyf.createEmptyKeyList(v11n) as Passage)
        var empty: Passage = (keyf.createEmptyKeyList(v11n) as Passage)

        for (i in 1 until v11n.maximumOrdinal() step 10) {
            hard.add(v11n.decodeOrdinal(i))
        }

        var temp: ByteArray = PassageKeyFactory.toBinaryRepresentation(genC1V135)
        var genC1V135Copy: Passage = PassageKeyFactory.fromBinaryRepresentation(temp)
        assertEquals(genC1V135Copy, genC1V135)
        temp = PassageKeyFactory.toBinaryRepresentation(exo2aTo3b)
        var exo2aTo3bCopy: Passage = PassageKeyFactory.fromBinaryRepresentation(temp)
        assertEquals(exo2aTo3bCopy, exo2aTo3b)
        temp = PassageKeyFactory.toBinaryRepresentation(gentoRev)
        var genToRevCopy: Passage = PassageKeyFactory.fromBinaryRepresentation(temp)
        assertEquals(genToRevCopy, gentoRev)
        temp = PassageKeyFactory.toBinaryRepresentation(hard)
        var hardCopy: Passage = PassageKeyFactory.fromBinaryRepresentation(temp)
        assertEquals(hardCopy, hard)
        temp = PassageKeyFactory.toBinaryRepresentation(empty)
        var emptyCopy: Passage = PassageKeyFactory.fromBinaryRepresentation(temp)
        assertEquals(emptyCopy, empty)
    }
}
