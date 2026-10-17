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

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JUnit Test.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class PassageBinaryJvmTest {
    @Test
    fun testBinary() {
        var buffer: ByteArray
        var index: IntArray

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(1, toBinary(buffer, 0, 0, 0))
        assertEquals(0, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 0))
        assertEquals(1, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(1, toBinary(buffer, 0, 0, 1))
        assertEquals(0, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 1))
        assertEquals(1, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(1, toBinary(buffer, 0, 0, 255))
        assertEquals(0, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 255))
        assertEquals(1, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(2, toBinary(buffer, 0, 0, 256))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 256))
        assertEquals(2, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(2, toBinary(buffer, 0, 0, 65535))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 65535))
        assertEquals(2, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(3, toBinary(buffer, 0, 0, 65536))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 65536))
        assertEquals(3, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(3, toBinary(buffer, 0, 0, 16777215))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 16777215))
        assertEquals(3, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(4, toBinary(buffer, 0, 0, 16777216))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 16777216))
        assertEquals(4, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(4, toBinary(buffer, 0, 0, 2147483647))
        assertEquals(0, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(0, fromBinary(buffer, index, 2147483647))
        assertEquals(4, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(0, 0, 0, 0)
        assertEquals(1, toBinary(buffer, 0, 1, 1))
        assertEquals(1, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(1, fromBinary(buffer, index, 1))
        assertEquals(1, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(0, 0, 0, 0)
        assertEquals(1, toBinary(buffer, 0, 255, 255))
        assertEquals(-1, buffer[0].toInt())
        assertEquals(0, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(255, fromBinary(buffer, index, 255))
        assertEquals(1, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(0, 0, 0, 0)
        assertEquals(2, toBinary(buffer, 0, 65535, 65535))
        assertEquals(-1, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(0, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(65535, fromBinary(buffer, index, 65535))
        assertEquals(2, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(0, 0, 0, 0)
        assertEquals(3, toBinary(buffer, 0, 16777215, 16777215))
        assertEquals(-1, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(0, buffer[3].toInt())
        assertEquals(16777215, fromBinary(buffer, index, 16777215))
        assertEquals(3, index[0])

        index = intArrayOf(0)
        buffer = byteArrayOf(0, 0, 0, 0)
        assertEquals(4, toBinary(buffer, 0, 2147483647, 2147483647))
        assertEquals(127, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(-1, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(2147483647, fromBinary(buffer, index, 2147483647))
        assertEquals(4, index[0])

        index = intArrayOf(2)
        buffer = byteArrayOf(-1, -1, -1, -1)
        assertEquals(1, toBinary(buffer, 2, 10, 11))
        assertEquals(-1, buffer[0].toInt())
        assertEquals(-1, buffer[1].toInt())
        assertEquals(10, buffer[2].toInt())
        assertEquals(-1, buffer[3].toInt())
        assertEquals(10, fromBinary(buffer, index, 11))
        assertEquals(3, index[0])

        for (i in 0 until 2147400000 step 10000) {
            index = intArrayOf(0)
            buffer = byteArrayOf(0, 0, 0, 0)
            assertEquals(4, toBinary(buffer, 0, i, 2147483647))
            assertEquals(i, fromBinary(buffer, index, 2147483647))
            assertEquals(4, index[0])
        }
    }

}

/*
 * toBinary and fromBinary are protected static on PassageKeyFactory. Java tests in the same
 * package could call them; Kotlin cannot, so they are reached reflectively here.
 */
private val toBinaryMethod = PassageKeyFactory::class.java
    .getDeclaredMethod("toBinary", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
    .apply { isAccessible = true }

private val fromBinaryMethod = PassageKeyFactory::class.java
    .getDeclaredMethod("fromBinary", ByteArray::class.java, IntArray::class.java, Int::class.javaPrimitiveType)
    .apply { isAccessible = true }

private fun toBinary(buffer: ByteArray, index: Int, number: Int, max: Int): Int =
    toBinaryMethod.invoke(null, buffer, index, number, max) as Int

private fun fromBinary(buffer: ByteArray, index: IntArray, max: Int): Int =
    fromBinaryMethod.invoke(null, buffer, index, max) as Int
