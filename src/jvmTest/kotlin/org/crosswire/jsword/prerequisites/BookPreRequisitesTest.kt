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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.prerequisites

import kotlinx.coroutines.runBlocking
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.install.InstallException
import org.crosswire.jsword.book.install.InstallManager
import org.crosswire.jsword.book.install.Installer
import org.slf4j.LoggerFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * JUnit Test.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class BookPreRequisitesTest {
    @BeforeTest
    fun setUp() {
        installedBooks = Books.installed()
    }

    @Test
    fun testInstallBook() {
        for (ii in BOOKS.indices) {
            try {
                if (installedBooks.getBook(BOOKS[ii]) == null) {
                    if (underTest == null) {
                        underTest = InstallManager().getInstaller("CrossWire")
                    }
                    val installer = underTest!!
                    LOGGER.info("Installing [{}]... Please wait...", BOOKS[ii])
                    runBlocking { installer.reloadBookList() }
                    val book = installer.getBook(BOOKS[ii])
                    runBlocking { installer.install(book!!) }
                }
            } catch (ex: InstallException) {
                ex.printStackTrace()
                fail(ex.message)
            } catch (ex: Exception) {
                throw AssertionError(ex)
            }
        }
    }

    private var underTest: Installer? = null
    private lateinit var installedBooks: Books

    private companion object {
        /**
         * All books referenced in these unit tests that can be retrieved from the Crosswire repository.
         * Currently missing : ESV2011, MHCC, ot1nt2
         * Tests that reference a currently missing book are automatically skipped unless the book was installed externally.
         */
        private val BOOKS = arrayOf("KJV", "StrongsHebrew", "StrongsGreek", "Josephus", "Nave", "Geneva", "TurNTB")
        private val LOGGER = LoggerFactory.getLogger(BookPreRequisitesTest::class.java)
    }
}
