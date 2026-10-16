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
 * \u00A9 CrossWire Bible Society, 2005 - 2016
 */
package org.crosswire.jsword.book.basic

import fi.sykero.kdom.Content
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.IndexStatusEvent
import org.crosswire.jsword.index.IndexStatusListener
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.versification.system.Versifications
import org.junit.Assume
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Characterization of [AbstractBook] (through the installed KJV and
 * StrongsGreek, and through a minimal subclass built in the test) and of
 * [DefaultBookMetaData].
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class AbstractBookCharacterizationTest {
    private fun installed(initials: String): Book {
        val book = Books.installed().getBook(initials)
        Assume.assumeNotNull(book)
        return book!!
    }

    /** Minimal concrete AbstractBook: delegation is what is under test. */
    private class StubBook(bmd: BookMetaData, private val globalKeys: Key?, private val osisFails: Boolean) :
        AbstractBook(bmd, null) {
        val osisCalls = AtomicInteger()

        override fun getOsis(key: Key?, p: RawTextToXmlProcessor?): List<Content> {
            osisCalls.incrementAndGet()
            if (osisFails) {
                throw BookException("cannot read")
            }
            return nullOf()
        }

        override fun getGlobalKeyList(): Key = globalKeys ?: nullOf()

        override fun getValidKey(name: String?): Key = nullOf()

        override fun getKey(name: String?): Key = nullOf()

        override fun createEmptyKeyList(): Key = nullOf()

        override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> =
            nullOf()

        override fun contains(key: Key?): Boolean = false

        override fun getRawText(key: Key?): String = nullOf()

        override fun isWritable(): Boolean = false

        override fun setRawText(key: Key?, rawData: String?) {
        }

        override fun setAliasKey(alias: Key?, source: Key?) {
        }
    }

    /** A metadata whose unlock() succeeds and which records resetLock(). */
    private class UnlockableMetaData : DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE) {
        val resets = AtomicInteger()

        override fun unlock(unlockKey: String?): Boolean {
            return true
        }

        override fun resetLock() {
            resets.incrementAndGet()
        }
    }

    private fun emptyKeyList(): Key {
        return PassageKeyFactory.instance().createEmptyKeyList(Versifications.instance().getVersification("KJV"))
    }

    // ---- installed books ----

    @Test
    fun testEqualsHashCodeCompareToOnInstalledBooks() {
        val kjv = installed("KJV")
        val strongs = installed("StrongsGreek")
        val esv = installed("ESV2011")

        assertTrue(kjv == kjv)
        assertEquals(0, kjv.compareTo(kjv))
        assertEquals(kjv.bookMetaData.hashCode(), kjv.hashCode())
        assertFalse(kjv.equals(null))

        // Different concrete classes (SwordBook vs SwordDictionary) are never equal.
        assertFalse(kjv == strongs)
        assertFalse(strongs == kjv)
        // Same class, different metadata (initials/name differ) are not equal.
        assertFalse(kjv == esv)

        // compareTo is the metadata compareTo: Bible category sorts before Dictionary,
        // then abbreviation ignoring case ("ESV2011" < "KJV").
        assertTrue(kjv.compareTo(strongs) < 0)
        assertTrue(strongs.compareTo(kjv) > 0)
        assertTrue(esv.compareTo(kjv) < 0)
        assertTrue(kjv.compareTo(esv) > 0)
    }

    @Test
    fun testToStringAndMetadataDelegation() {
        val kjv = installed("KJV")
        val meta = kjv.bookMetaData
        assertEquals(meta.toString(), kjv.toString())
        assertTrue(kjv.toString().startsWith("KJV"))
        assertSame(meta, kjv.bookMetaData)
        assertEquals(meta.initials, kjv.initials)
        assertEquals(meta.name, kjv.name)
        assertEquals(meta.abbreviation, kjv.abbreviation)
        assertEquals(meta.osisID, kjv.osisID)
        assertEquals(meta.language, kjv.language)
        assertEquals(meta.bookCategory, kjv.bookCategory)
        assertEquals(meta.driver, kjv.driver)
        assertEquals(meta.driverName, kjv.driverName)
        assertEquals(meta.propertyKeys, kjv.propertyKeys)
    }

    @Test
    fun testPropertyAndFlagsOnInstalledBooks() {
        for (initials in arrayOf("KJV", "StrongsGreek")) {
            val book = installed(initials)
            assertEquals("en", book.getProperty("Lang"), initials)
            assertFalse(book.isEnciphered, initials)
            assertFalse(book.isLocked, initials)
            assertNull(book.unlockKey, initials)
            assertTrue(book.isSupported, initials)
            assertNull(book.getProperty("NoSuchPropertyKey"), initials)
            assertSame(book, (book as AbstractBook).book, initials)
            assertTrue(book.isLeftToRight, initials)
        }
        assertEquals(BookCategory.BIBLE, installed("KJV").bookCategory)
        assertEquals(BookCategory.DICTIONARY, installed("StrongsGreek").bookCategory)
    }

    @Test
    fun testMatch() {
        val kjv = installed("KJV")
        assertFalse(kjv.match(null))
        assertTrue(kjv.match("KJV"))
        assertTrue(kjv.match("kjv"))
        assertTrue(kjv.match(kjv.name))
        assertTrue(kjv.match(kjv.name.uppercase(Locale.getDefault())))
        // startsWith the initials
        assertTrue(kjv.match("KJV extra"))
        assertFalse(kjv.match("XYZ"))
        assertFalse(kjv.match(""))
    }

    @Test
    fun testGetValidKeyOnPassageBook() {
        val kjv = installed("KJV")
        val key = kjv.getValidKey("Gen 1:1")
        assertEquals("Genesis 1:1", key.getName())
        assertEquals(1, key.getCardinality())
        assertTrue(kjv.contains(key))

        // Unparsable text gives an empty key list, not an exception.
        val bad = kjv.getValidKey("nonsense")
        assertNotNull(bad)
        assertEquals(0, bad.getCardinality())
        assertEquals("", bad.getName())
    }

    @Test
    fun testScopeIsGlobalKeyList() {
        val kjv = installed("KJV")
        assertSame(kjv.getGlobalKeyList(), kjv.scope)
    }

    // ---- DefaultBookMetaData ----

    @Test
    fun testDefaultBookMetaData() {
        val meta = DefaultBookMetaData(null, "Test Book", BookCategory.COMMENTARY)
        assertEquals("Test Book", meta.name)
        // initials are derived from the words of the name; abbreviation is the same
        assertEquals("TB", meta.initials)
        assertEquals("TB", meta.abbreviation)
        assertEquals("TB", meta.toString())
        assertEquals(BookCategory.COMMENTARY, meta.bookCategory)
        assertEquals("Test Book".hashCode(), meta.hashCode())

        // setName and setBookCategory write properties
        assertEquals("Test Book", meta.getProperty(BookMetaData.KEY_NAME))
        assertEquals(BookCategory.COMMENTARY.toString(), meta.getProperty(BookMetaData.KEY_CATEGORY))
        assertEquals(2, meta.propertyKeys!!.size)
        assertTrue(meta.propertyKeys!!.contains(BookMetaData.KEY_NAME))
        assertTrue(meta.propertyKeys!!.contains(BookMetaData.KEY_CATEGORY))

        // missing key is null; the language key is answered from the language object
        assertNull(meta.getProperty("missing"))
        assertNull(meta.getValues("missing"))
        assertEquals(meta.language.name, meta.getProperty(BookMetaData.KEY_LANGUAGE))

        meta.putProperty("custom", "v")
        assertEquals("v", meta.getProperty("custom"))
        assertEquals(java.util.Collections.singletonList("v"), java.util.ArrayList<String>(meta.getValues("custom")!!))
        meta.putProperty("custom2", "w", true)
        assertEquals("w", meta.getProperty("custom2"))
        assertEquals(4, meta.propertyKeys!!.size)

        // the key set is a read-only view
        assertFailsWith<UnsupportedOperationException> {
            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
            val view = meta.propertyKeys!! as java.util.Set<String>
            view.add("x")
        }
    }

    @Test
    fun testDefaultBookMetaDataDefaultsAndEquality() {
        val a = DefaultBookMetaData(null, "Test Book", null)
        // null category falls back to BIBLE
        assertEquals(BookCategory.BIBLE, a.bookCategory)
        assertEquals(BookCategory.BIBLE.getName() + ".TB", a.osisID)
        assertTrue(a.isSupported)
        assertFalse(a.isEnciphered)
        assertFalse(a.isLocked)
        assertFalse(a.unlock("x"))
        assertNull(a.unlockKey)
        assertEquals(IndexStatus.UNDONE, a.indexStatus)
        assertEquals("UTF-8", a.bookCharset)

        val b = DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE)
        val c = DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE)
        assertEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals<Any?>(a, null)
        assertEquals(0, a.compareTo(b))

        a.setInitials(null)
        assertEquals("TB", a.initials)
    }

    // ---- AbstractBook behaviour through a stub ----

    @Test
    fun testStubDelegationAndPropertyPut() {
        val meta = DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE)
        val book = StubBook(meta, null, false)
        assertSame(meta, book.bookMetaData)
        assertNull(book.driver)
        assertNull(book.driverName)
        assertNull(book.backend)
        assertSame(book, book.book)
        assertEquals("TB", book.initials)
        assertEquals("TB", book.toString())

        book.putProperty("k", "v")
        assertEquals("v", meta.getProperty("k"))
        book.putProperty("k2", "v2", true)
        assertEquals("v2", book.getProperty("k2"))

        val other: BookMetaData = DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE)
        book.bookMetaData = other
        assertSame(other, book.bookMetaData)
        assertEquals("OB", book.initials)

        // equals requires the same runtime class and equal metadata
        val twin = StubBook(DefaultBookMetaData(null, "Other Book", BookCategory.BIBLE), null, false)
        assertEquals(book, twin)
        assertEquals(book.hashCode(), twin.hashCode())
        assertEquals(0, book.compareTo(twin))
    }

    @Test
    fun testIndexStatusFiresOnlyOnChange() {
        val meta = DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE)
        val book = StubBook(meta, null, false)
        val events = AtomicInteger()
        val last = arrayOfNulls<IndexStatus>(1)
        val listener = object : IndexStatusListener {
            override fun statusChanged(ev: IndexStatusEvent?) {
                events.incrementAndGet()
                last[0] = ev!!.indexStatus
            }
        }
        book.addIndexStatusListener(listener)

        assertEquals(IndexStatus.UNDONE, book.indexStatus)
        book.indexStatus = IndexStatus.UNDONE
        assertEquals(0, events.get())
        book.indexStatus = IndexStatus.DONE
        assertEquals(1, events.get())
        assertEquals(IndexStatus.DONE, last[0])
        assertEquals(IndexStatus.DONE, meta.indexStatus)

        book.removeIndexStatusListener(listener)
        book.indexStatus = IndexStatus.UNDONE
        assertEquals(1, events.get())
        assertEquals(IndexStatus.UNDONE, book.indexStatus)
    }

    @Test
    fun testUnlockWhenMetadataRefuses() {
        val book = StubBook(DefaultBookMetaData(null, "Test Book", BookCategory.BIBLE), null, false)
        assertFalse(book.unlock("x"))
        assertEquals(0, book.osisCalls.get())
    }

    @Test
    fun testUnlockWithNullGlobalKeyListIsAssumedValid() {
        val meta = UnlockableMetaData()
        val book = StubBook(meta, null, false)
        assertTrue(book.unlock("x"))
        assertEquals(0, meta.resets.get())
        assertEquals(0, book.osisCalls.get())
    }

    @Test
    fun testUnlockTriesToReadAndResetsOnFailure() {
        val meta = UnlockableMetaData()
        val failing = StubBook(meta, emptyKeyList(), true)
        assertFalse(failing.unlock("x"))
        assertEquals(1, failing.osisCalls.get())
        assertEquals(1, meta.resets.get())

        val meta2 = UnlockableMetaData()
        val working = StubBook(meta2, emptyKeyList(), false)
        assertTrue(working.unlock("x"))
        assertEquals(1, working.osisCalls.get())
        assertEquals(0, meta2.resets.get())
    }
}

/**
 * Java returned null from these stub overrides. Kotlin's non-null return types are
 * bypassed by generic erasure, so the null paths stay reachable as they were in Java.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> nullOf(): T = null as T
