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

import java.util.StringTokenizer
import java.util.regex.Pattern
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class StringUtilTest {

    @Test
    fun testGetInitials() {
        assertEquals("CoE", StringUtil.getInitials("Church of England"))
        assertEquals("JDC", StringUtil.getInitials("Java DataBase Connectivity"))
        assertEquals("", StringUtil.getInitials(""))
    }

    @Test
    fun testCreateTitle() {
        assertEquals("One Two", StringUtil.createTitle("OneTwo"))
        assertEquals("One Two", StringUtil.createTitle("one_two"))
        assertEquals("ONe TWo", StringUtil.createTitle("ONeTWo"))
        assertEquals("One Two", StringUtil.createTitle("One_Two"))
        assertEquals("One Two", StringUtil.createTitle("One _Two"))
        assertEquals("One Two", StringUtil.createTitle("one  _Two"))
    }

    @Test
    fun testSplitWhitespace() {
        assertContentEquals(arrayOf<String>(), StringUtil.split(null), "split null on ws")
        assertContentEquals(arrayOf<String>(), StringUtil.split(""), "split empty on ws")
        assertContentEquals(arrayOf("abc", "def"), StringUtil.split("abc def"), "split parts on middle ws")
        assertContentEquals(arrayOf("abc", "def"), StringUtil.split("abc  def"), "split parts on many ws")
        assertContentEquals(arrayOf("abc"), StringUtil.split(" abc "), "split parts on surrounding ws")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.split("a\tb\nc"), "split parts on tab and new line ws")
    }

    @Test
    fun testSplitWhitespaceMax() {
        assertContentEquals(arrayOf<String>(), StringUtil.split(null, 1), "split null on ws")
        assertContentEquals(arrayOf<String>(), StringUtil.split("", 1), "split empty on ws")
        assertContentEquals(arrayOf("abc"), StringUtil.split("abc def", 1), "split parts on middle ws")
        assertContentEquals(arrayOf("abc"), StringUtil.split("abc  def", 1), "split parts on many ws")
        assertContentEquals(arrayOf("abc"), StringUtil.split(" abc ", 1), "split parts on surrounding ws")
        assertContentEquals(arrayOf("a", "b"), StringUtil.split("a\tb\nc", 2), "split parts on tab and new line ws")
    }

    @Test
    fun testSplitChar() {
        assertContentEquals(arrayOf<String>(), StringUtil.split(null, '.'), "split null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.split("", '.'), "split empty on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.split("a.b.c", '.'), "split parts on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.split("a..b.c", '.'), "split adjacent on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.split(".a..b.c", '.'), "split adjacent leading .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.split(".a..b.c.", '.'), "split adjacent trailing .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.split("a:b:c", '.'), "split nothing on .")
    }

    @Test
    fun testSplitAllChar() {
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.'), "splitAll null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.'), "splitAll empty on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.splitAll("a.b.c", '.'), "splitAll parts on .")
        assertContentEquals(arrayOf("a", "", "b", "c"), StringUtil.splitAll("a..b.c", '.'), "splitAll adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.'), "splitAll nothing on .")
    }

    @Test
    fun testSplitAllCharMax() {
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.', 0), "splitAll all null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.', 0), "splitAll all empty on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.splitAll("a.b.c", '.', 0), "splitAll all parts on .")
        assertContentEquals(arrayOf("a", "", "b", "c"), StringUtil.splitAll("a..b.c", '.', 0), "splitAll all adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.', 0), "splitAll all nothing on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.', Int.MAX_VALUE), "splitAll max null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.', Int.MAX_VALUE), "splitAll max empty on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.splitAll("a.b.c", '.', Int.MAX_VALUE), "splitAll max parts on .")
        assertContentEquals(arrayOf("a", "", "b", "c"), StringUtil.splitAll("a..b.c", '.', Int.MAX_VALUE), "splitAll max adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.', Int.MAX_VALUE), "splitAll max nothing on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.', 3), "splitAll 3 null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.', 3), "splitAll 3 empty on .")
        assertContentEquals(arrayOf("a", "b", "c"), StringUtil.splitAll("a.b.c", '.', 3), "splitAll 3 parts on .")
        assertContentEquals(arrayOf("a", "", "b"), StringUtil.splitAll("a..b.c", '.', 3), "splitAll 3 adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.', 3), "splitAll 3 nothing on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.', 2), "splitAll 2 null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.', 2), "splitAll 2 empty on .")
        assertContentEquals(arrayOf("a", "b"), StringUtil.splitAll("a.b.c", '.', 2), "splitAll 2 parts on .")
        assertContentEquals(arrayOf("a", ""), StringUtil.splitAll("a..b.c", '.', 2), "splitAll 2 adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.', 2), "splitAll 2 nothing on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll(null, '.', 1), "splitAll 1 null on .")
        assertContentEquals(arrayOf<String>(), StringUtil.splitAll("", '.', 1), "splitAll 1 empty on .")
        assertContentEquals(arrayOf("a"), StringUtil.splitAll("a.b.c", '.', 1), "splitAll 1 parts on .")
        assertContentEquals(arrayOf("a"), StringUtil.splitAll("a..b.c", '.', 1), "splitAll 1 adjacent on .")
        assertContentEquals(arrayOf("a:b:c"), StringUtil.splitAll("a:b:c", '.', 1), "splitAll 1 nothing on .")
    }

    @Ignore
    @Test
    fun bench() {

        val string = "a/b/c/d/e/f/g/h/i/j/asd/asdas/dasdjasodjoa/sjd/oajs/djoasjd/as/odj/jaowdj/oajw/odj/aojwd/oja/owjd/oja/wjdoja/wdj/awjdojaw/odj/oawjd/oja/wjdoawjdojaw/d/dff"
        val n = 1000000

        var start: Long

        for (t in 1..3) {
            println("Run #$t")

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                Pattern.compile("/").split(string, 0)
            }
            println("Java 6 string.split: " + (System.currentTimeMillis() - start))

            val splitter = Pattern.compile("/")
            start = System.currentTimeMillis()
            for (i in 0 until n) {
                splitter.split(string)
            }
            println("Pattern.split: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                val s = StringTokenizer(string, "/")
                while (s.hasMoreElements()) {
                    s.nextElement()
                }
            }
            println("StringTokenizer: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                // Java's String.split(regex) is translated to splitLikeJava (rules section 4)
                string.splitLikeJava(Regex("/"))
            }
            println("Java 7 string.split: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                StringUtil.splitAll(string, '/')
            }
            println("StringUtil.splitAll: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                StringUtil.splitAll(string, '/', Int.MAX_VALUE)
            }
            println("StringUtil.splitAll max: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                StringUtil.split(string, '/')
            }
            println("StringUtil.split char: " + (System.currentTimeMillis() - start))

            start = System.currentTimeMillis()
            for (i in 0 until n) {
                StringUtil.split(string, '/', Int.MAX_VALUE)
            }
            println("StringUtil.split char max: " + (System.currentTimeMillis() - start))

            println("")
        }
    }
}
