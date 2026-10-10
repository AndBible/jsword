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

/**
 * JUnit test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class TreeKeyTest {

    private lateinit var pilgrimPart1: TreeKey
    private lateinit var pilgrimPart1FirstStage: TreeKey
    private lateinit var pilgrimPart1FirstStageClone: TreeKey
    private lateinit var pilgrimPart2FirstStage: TreeKey

    @BeforeTest
    fun setUp(){
        pilgrimPart1 = TreeKey("Part 1")
        pilgrimPart1FirstStage = TreeKey("First Stage", pilgrimPart1)
        pilgrimPart1FirstStageClone = TreeKey("First Stage", TreeKey("Part 1"))
        pilgrimPart2FirstStage = TreeKey("First Stage", TreeKey("Part 2"))
    }

    @Test
    fun testEquals(){
        assertTrue(pilgrimPart1FirstStage.equals(pilgrimPart1FirstStageClone))
        assertFalse(pilgrimPart1FirstStage.equals(pilgrimPart2FirstStage))
        assertFalse(pilgrimPart1FirstStage.equals(null))
        assertFalse(pilgrimPart1FirstStage.equals(pilgrimPart1))
        assertTrue(pilgrimPart1FirstStage.getParent()!!.equals(pilgrimPart1))
    }

}
