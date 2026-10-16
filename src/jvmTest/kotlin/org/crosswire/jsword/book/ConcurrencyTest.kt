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
 * © CrossWire Bible Society, 2012 - 2016
 *
 */
package org.crosswire.jsword.book

import org.crosswire.jsword.passage.NoSuchKeyException
import kotlin.test.Test
import kotlin.test.fail

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class ConcurrencyTest {

    @Test
    fun testConcurrencyIssueOnBookData() {
        val names = arrayOf("KJV", "KJVA")
        val ref = "Rom.1.1"

        val r1 = Runnable {
            val b0 = Books.installed().getBook(names[0])!!
            try {
                val bd1 = BookData(b0, b0.getKey(ref))
                bd1.getSAXEventProvider()
            } catch (e: NoSuchKeyException) {
                System.err.println("A jsword error during test")
                e.printStackTrace()
                fail("JSword bug has occured")
            } catch (e: BookException) {
                System.err.println("A jsword error during test")
                e.printStackTrace()
                fail("JSword bug has occured")
            }
        }

        val r2 = Runnable {
            val b0 = Books.installed().getBook(names[1])!!
            try {
                val bd1 = BookData(b0, b0.getKey(ref))
                bd1.getSAXEventProvider()
            } catch (e: NoSuchKeyException) {
                System.err.println("A jsword error during test")
                e.printStackTrace()
                fail("JSword bug has occured")
            } catch (e: BookException) {
                System.err.println("A jsword error during test")
                e.printStackTrace()
                fail("JSword bug has occured")
            }
        }

        var ii = 0
        while (ii++ < 50) {
            val t1 = Thread(r1)
            val t2 = Thread(r2)
            t1.start()
            t2.start()

            t1.join()
            t2.join()
        }
    }
}
