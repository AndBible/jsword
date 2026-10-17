/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
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
package org.crosswire.common.activate

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests that the Activator tracks activation per instance rather than per
 * equal value.
 *
 * Books compare equal by (category, name, initials), so a replacement instance
 * for the same module equals the instance it replaced. Activation state lives
 * in the instance, so tracking by value would skip activating the replacement
 * and leave it permanently unusable.
 */
class ActivatorTest {

    /**
     * Activatable whose equality is by name, mimicking AbstractBook.
     */
    private class NamedActivatable(private val name: String) : Activatable {
        var activateCount = 0
        var deactivateCount = 0

        override fun activate(lock: Lock?) {
            activateCount++
        }

        override fun deactivate(lock: Lock?) {
            deactivateCount++
        }

        fun isActivated(): Boolean = activateCount > deactivateCount

        override fun equals(other: Any?): Boolean {
            if (other !is NamedActivatable) {
                return false
            }
            return name == other.name
        }

        override fun hashCode(): Int = name.hashCode()
    }

    @BeforeTest
    fun setUp() {
        Activator.deactivateAll()
    }

    @AfterTest
    fun tearDown() {
        Activator.deactivateAll()
    }

    @Test
    fun testActivatesEachInstanceEvenWhenTheyCompareEqual() {
        val original = NamedActivatable("KJV")
        val replacement = NamedActivatable("KJV")
        assertEquals(original, replacement, "precondition: the two instances compare equal")

        Activator.activate(original)
        Activator.activate(replacement)

        assertEquals(1, original.activateCount)
        assertEquals(
            1, replacement.activateCount, "replacement instance must be activated in its own right")
    }

    @Test
    fun testReplacementIsActivatedAfterTheOriginalIsDeactivated() {
        val original = NamedActivatable("KJV")
        Activator.activate(original)
        Activator.deactivate(original)

        val replacement = NamedActivatable("KJV")
        Activator.activate(replacement)

        assertTrue(replacement.isActivated())
    }

    @Test
    fun testDeactivatingOneInstanceLeavesAnEqualInstanceActive() {
        val first = NamedActivatable("KJV")
        val second = NamedActivatable("KJV")
        Activator.activate(first)
        Activator.activate(second)

        Activator.deactivate(first)

        assertFalse(first.isActivated())
        assertTrue(
            second.isActivated(), "deactivating one instance must not deactivate an equal one")
        assertEquals(0, second.deactivateCount)
    }

    @Test
    fun testActivateIsNotRepeatedForTheSameInstance() {
        val subject = NamedActivatable("KJV")

        Activator.activate(subject)
        Activator.activate(subject)

        assertEquals(1, subject.activateCount)
    }

    @Test
    fun testDeactivateAllDeactivatesEveryTrackedInstance() {
        val first = NamedActivatable("KJV")
        val second = NamedActivatable("KJV")
        val other = NamedActivatable("ESV")
        Activator.activate(first)
        Activator.activate(second)
        Activator.activate(other)

        Activator.deactivateAll()

        assertFalse(first.isActivated())
        assertFalse(second.isActivated())
        assertFalse(other.isActivated())
    }

    @Test
    fun testDeactivateOfAnUntrackedInstanceIsANoOp() {
        val activated = NamedActivatable("KJV")
        val neverActivated = NamedActivatable("KJV")
        Activator.activate(activated)

        Activator.deactivate(neverActivated)

        assertEquals(0, neverActivated.deactivateCount)
        assertTrue(activated.isActivated())
    }
}
