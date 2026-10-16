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
package org.crosswire.jsword.book.sword

import fi.sykero.kdom.output.Format
import fi.sykero.kdom.output.XMLOutputter
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.versification.BookName
import org.junit.Assume
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test simple functionality across multiple backends, to ensure that all types
 * of backing drivers are working correctly
 *
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BackendTest {
    @Test
    fun testKeyBackendsHaveNoJavaSerializationHooks() {
        for (type in arrayOf<Class<*>>(AbstractKeyBackend::class.java, RawLDBackend::class.java, ZLDBackend::class.java)) {
            assertTrue(Key::class.java.isAssignableFrom(type))
            assertFalse(java.io.Serializable::class.java.isAssignableFrom(type))
            for (field in type.declaredFields) {
                assertFalse("serialVersionUID" == field.name, type.name)
            }
            for (method in type.declaredMethods) {
                assertFalse("readObject" == method.name
                        || "writeObject" == method.name || "readResolve" == method.name, type.name)
            }
        }
    }

    /**
     * Z Text
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendKJVTextZ() {
        val version = "KJV"
        val reference = "Romans 1:1-3"
        val assertion = arrayOf(
                "<w lemma=\"strong:G3588 strong:G5207",
                " morph=\"robinson:T-GSM robinson:N-GSM\" src=\"2 3\">Son</w>"
        )

        backendTest(version, reference, *assertion)
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendKJV() {
        val version = "KJV"
        val reference = "Romans 1:1-3"
        backendTest(version, reference, "separated", "unto", "the gospel", "of", "God")
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendFullChapter() {
        val version = "KJV"
        val reference = "Romans 1"

        backendTest(version, reference, " <title type=\"x-gen\">Romans 1</title>", "<verse osisID=\"Rom.1.1\" verseOrdinal", "<verse osisID=\"Rom.1.32\" verseOrdinal",
                "separated", "unto", "the gospel", "of", "God", "but", "have pleasure in", "them that", "do them")
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackend1AndFollowing() {
        val version = "KJV"
        val reference = "Romans 1:1-ff"

        backendTest(version, reference, "<title type=\"x-gen\">Romans 1</title>", "<verse osisID=\"Rom.1.1\" verseOrdinal", "<verse osisID=\"Rom.1.32\" verseOrdinal",
                 "separated", "unto", "the gospel", "of", "God", "but", "have pleasure in", "them that", "do them")
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendCrossChapterBoundary() {
        val version = "KJV"
        val reference = "Rom 1:32-Rom:2:2"

        val xml = backendTest(version, reference, "<verse osisID=\"Rom.1.32\" verseOrdinal", "<title type=\"x-gen\">Romans 2:0-2</title>", "<verse osisID=\"Rom.2.1\" verseOrdinal",
                "<verse osisID=\"Rom.2.2\" verseOrdinal")

        assertFalse(xml.contains("<verse osisID=\"Rom.1.31\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.2.3\" verseOrdinal"))

    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendMiddleChapter() {
        val version = "KJV"
        val reference = "Rom 1:2-1:31"

        val xml = backendTest(version, reference, "<verse osisID=\"Rom.1.2\" verseOrdinal", "<verse osisID=\"Rom.1.31\" verseOrdinal")

        assertFalse(xml.contains("<verse osisID=\"Rom.1.1\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.1.32\" verseOrdinal"))

    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendMiddleToEnd() {
        val version = "KJV"
        val reference = "Rom 1:5-ff"

        val xml = backendTest(version, reference, "<verse osisID=\"Rom.1.5\" verseOrdinal", "<verse osisID=\"Rom.1.31\" verseOrdinal", "<verse osisID=\"Rom.1.32\" verseOrdinal")

        assertFalse(xml.contains("<verse osisID=\"Rom.1.1\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.1.4\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.2.0\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.2.1\" verseOrdinal"))
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackend2ChaptersStartMiddle() {
        val version = "KJV"
        val reference = "Rom 1:5-3:14"

        val xml = backendTest(version, reference, "<verse osisID=\"Rom.1.5\" verseOrdinal",
                "<verse osisID=\"Rom.2.1\" verseOrdinal", "<verse osisID=\"Rom.2.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.1\" verseOrdinal", "<verse osisID=\"Rom.3.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.14\" verseOrdinal"
                )

        assertFalse(xml.contains("<verse osisID=\"Rom.1.4\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.3.15\" verseOrdinal"))
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackend2ChaptersStartVerse1() {
        val version = "KJV"
        val reference = "Rom 1:1-3:14"

        val xml = backendTest(version, reference,
                "<verse osisID=\"Rom.1.1\" verseOrdinal", "<verse osisID=\"Rom.1.2\" verseOrdinal",
                "<verse osisID=\"Rom.1.5\" verseOrdinal",
                "<verse osisID=\"Rom.2.1\" verseOrdinal", "<verse osisID=\"Rom.2.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.1\" verseOrdinal", "<verse osisID=\"Rom.3.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.14\" verseOrdinal"
                )

        assertFalse(xml.contains("<verse osisID=\"Rom.1.0\" verseOrdinal"))
        assertFalse(xml.contains("<verse osisID=\"Rom.3.15\" verseOrdinal"))
    }

    /**
     * Z Text - cos it's important
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackend2ChaptersStartVerse0() {
        val version = "KJV"
        val reference = "Rom 1:0-3:14"

        val xml = backendTest(version, reference,
                "<verse osisID=\"Rom.1.2\" verseOrdinal", "<verse osisID=\"Rom.1.2\" verseOrdinal",
                "<verse osisID=\"Rom.1.5\" verseOrdinal",
                "<verse osisID=\"Rom.2.1\" verseOrdinal", "<verse osisID=\"Rom.2.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.1\" verseOrdinal", "<verse osisID=\"Rom.3.10\" verseOrdinal",
                "<verse osisID=\"Rom.3.14\" verseOrdinal"
                )

        assertFalse(xml.contains("<verse osisID=\"Rom.3.15\" verseOrdinal"))
    }

    /**
     * Commentaries Raw
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendMHCCCommentsRaw() {
        val version = "MHCC"
        val reference = "Romans 1:1-3"
        val assertion = "Father of all believers, and coming to them through the Lord"

        backendTest(version, reference, assertion)
    }

    /**
     * Comments Z
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendGenevaCommentsZ() {
        val version = "Geneva"
        val reference = "Romans 1:1-3"
        val assertion = "(3) By declaring the sum of the doctrine of the Gospel, he stirs up the Romans"

        backendTest(version, reference, assertion)
    }

    /**
     * Lexicon/Dictionary RawLd
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendStrongsGreekRawLd() {
        val version = "StrongsGreek"
        val reference = "G3588"

        backendTest(version, reference, "3588", "ho")
    }

    /**
     * Lexicon/Dictionary RawLd
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendNaveZld() {
        val version = "Nave"
        val reference = "AARON"
        val assertion = "Marriage of"

        backendTest(version, reference, assertion)
    }

    /**
     * Lexicon/Dictionary RawLd
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendJosephus() {
        val version = "Josephus"
        val assertion = "calamity of our whole nation; for those that were fond of"

        val currentBook = Books.installed().getBook(version)
        if (currentBook == null) {
            return
        }

        val key = currentBook.getKey("The Life of Flavius Josephus").get(5)!!
        backendTest(currentBook, key, assertion)
    }

    /**
     * Lexicon/Dictionary RawLd
     *
     * @throws Exception
     *             an uncaught, failing, exception
     */
    @Test
    @Throws(Exception::class)
    fun testBackendOt1Nt2Devotional() {
        val version = "ot1nt2"
        val assertion = "Genesis 1-2"

        val book = Books.installed().getBook(version)
        if (book == null) {
            return
        }
        backendTest(book, book.getGlobalKeyList().get(0)!!, assertion)
    }

    @Throws(NoSuchKeyException::class, BookException::class)
    private fun backendTest(version: String, reference: String, vararg assertion: String): String {
        val currentBook = Books.installed().getBook(version)

        Assume.assumeTrue("Book $version is installed", currentBook != null)

        return backendTest(currentBook!!, currentBook.getKey(reference), *assertion)
    }

    @Throws(BookException::class)
    private fun backendTest(currentBook: Book, key: Key, vararg assertions: String): String {
        //order in test can be inconsistent and it seems on a linux build server the full book name is sometimes not used...
        BookName.setFullBookName(true)

        val bookData = BookData(currentBook, key)
        val osisFragment = bookData.osisFragment

        val xmlOutputter = XMLOutputter(Format.getPrettyFormat())
        val xml = xmlOutputter.outputString(osisFragment)
        log.debug("For book {} with key {}, Got {}", currentBook.initials, key.getName(), xml)

        for (s in assertions) {
            assertTrue(xml.contains(s), s)
        }
        return xml
    }

    companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(BackendTest::class.java)
    }
}
