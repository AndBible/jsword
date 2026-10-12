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
package org.crosswire.jsword.book

import java.net.URI

import org.crosswire.common.util.Language
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.common.kdom.Document

/**
 * A BookMetaData represents a method of translating the Bible. All Books with
 * the same BookMetaData should return identical text for any call to
 * <code>Bible.getText(VerseRange)</code>. The implication of this is that there
 * may be many instances of the Version "NIV", as there are several different
 * versions of the NIV - Original American-English, Anglicised, and Inclusive
 * Language editions at least.
 * 
 * <p>
 * BookMetaData like Strings must be compared using <code>.equals()</code>
 * instead of ==. A Bible must have the ability to handle a book unknown to
 * JSword. So Books must be able to add versions to the system, and the system
 * must cope with books that already exist.
 * </p>
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
interface BookMetaData : Comparable<BookMetaData> {
    /**
     * The name of the book, for example "King James Version" or
     * "Bible in Basic English" or "Greek". This method should not
     * return null or a blank string.
     *
     * @return The name of this book
     */
    val name: String

    /**
     * With which Charset is this Book encoded?
     *
     * @return the encoding of the Book
     */
    val bookCharset: String?

    /**
     * How this Book organizes it's keys.
     *
     * @return the organization of keys of this Book
     */
    val keyType: KeyType?

    /**
     * What category of content is this, a Bible or a reference work like a
     * Dictionary or Commentary.
     *
     * @return The category of book
     */
    val bookCategory: BookCategory

    /**
     * Accessor for the driver that runs this Book. Note this method should only
     * be used to delete() Books. Everything else you should want to do to a
     * Book should be available in other ways.
     *
     * @return the driver for the book.
     */
    val driver: BookDriver?

    /**
     * The language of the book.
     *
     * @return the book's language
     */
    /**
     * Set the language for this book.
     *
     * @param language
     *            the book's language
     */
    var language: Language

    /**
     * The initials of this book - how people familiar with this book will know
     * it, for example "NIV", "KJV".
     *
     * @return The book's initials
     */
    val abbreviation: String

    /**
     * The internal name of this book.
     *
     * @return The book's internal name
     */
    val initials: String

    /**
     * Calculated field: Get an OSIS identifier for the OsisText.setOsisIDWork()
     * and the Work.setOsisWork() methods. The response will generally be of the
     * form [Bible][Dict..].getInitials
     *
     * @return The osis id of this book
     */
    val osisID: String

    /**
     * Indicate whether this book is supported by JSword. Since the expectation
     * is that all books are supported, abstract implementations should return
     * true and let specific implementations return false if they cannot support
     * the book.
     *
     * @return true if the book is supported
     */
    val isSupported: Boolean

    /**
     * Indicate whether this book is enciphered. Since the expectation is that
     * most books are unenciphered, abstract implementations should return false
     * and let specific implementations return true otherwise.
     *
     * @return true if the book is enciphered
     */
    val isEnciphered: Boolean

    /**
     * Indicate whether this book is enciphered and without a key. Since the
     * expectation is that most books are unenciphered, abstract implementations
     * should return false and let specific implementations return true
     * otherwise.
     *
     * @return true if the book is locked
     */
    val isLocked: Boolean

    /**
     * Unlocks a book with the given key.
     *
     * @param unlockKey
     *            the key to try
     * @return true if the unlock key worked.
     */
    fun unlock(unlockKey: String?): Boolean

    fun resetLock()

    /**
     * Gets the unlock key for the module.
     *
     * @return the unlock key, if any, null otherwise.
     */
    val unlockKey: String?

    /**
     * Indicate whether this book is questionable. A book may be deemed
     * questionable if it's quality or content has not been confirmed. Since the
     * expectation is that all books are not questionable, abstract
     * implementations should return false and let specific implementations
     * return true if the book is questionable.
     *
     * @return true if the book is questionable
     */
    val isQuestionable: Boolean

    /**
     * Calculated field: The name of the name, which could be helpful to
     * distinguish similar Books available through 2 BookDrivers.
     *
     * @return The driver name
     */
    val driverName: String?

    /**
     * Return the orientation of the script of the Book. If a book contains more
     * than one script, it refers to the dominate script of the book. This will
     * be used to present Arabic and Hebrew in their proper orientation. Note:
     * some languages have multiple scripts which don't have the same
     * directionality.
     *
     * @return true if the orientation for the dominate script is LeftToRight.
     */
    val isLeftToRight: Boolean

    /**
     * Return whether the feature is supported by the book.
     *
     * @param feature the feature in question
     * @return true if the book supports the feature
     */
    fun hasFeature(feature: FeatureType?): Boolean

    /**
     * Get the base URI for library of this module.
     *
     * @return the base URI or null if there is none
     */
    /**
     * Set the base URI for library of this module.
     *
     * @param library
     *            the base URI or null if there is none
     * @throws BookException  indicates missing data files
     */
    @set:Throws(BookException::class)
    var library: URI?

    /**
     * Get the base URI for relative URIs in the document.
     *
     * @return the base URI or null if there is none
     */
    /**
     * Set the base URI for relative URIs in the document.
     *
     * @param library
     *            the base URI or null if there is none
     */
    var location: URI?

    /**
     * If this BookMetaData is partially loaded, reload it fully.
     * If it is fully loaded, don't do it again.
     *
     * @throws BookException when a problem is encountered loading the file
     */
    @Throws(BookException::class)
    fun reload()

    /**
     * Get a list of all the properties available to do with this Book. The
     * returned Properties will be read-only so any attempts to alter it will
     * fail.
     *
     * @return the read-only properties for this book
     */
    val propertyKeys: Set<String>?

    /**
     * Get the property or null.
     *
     * @param key
     *            the key of the property.
     * @return the value of the property
     */
    fun getProperty(key: String?): String?

    /**
     * Get the values or null.
     *
     * @param key
     *            the key of the property.
     * @return the values of the property
     */
    fun getValues(key: String?): Collection<String>?

    /**
     * Store a transient property.
     *
     * @param key
     *            the key of the property to set
     * @param value
     *            the value of the property
     */
    fun setProperty(key: String?, value: String?)

    /**
     * Save to shared storage.
     *
     * @param key
     *            the key of the property to set
     * @param value
     *            the value of the property
     */
    fun putProperty(key: String?, value: String?)

    /**
     * Saves an entry to a particular configuration file.
     *
     * @param key the entry that we are saving
     * @param value the value of the entry
     * @param forFrontend when `true` save to front end storage, else in shared storage
     */
    fun putProperty(key: String?, value: String?, forFrontend: Boolean)

    /**
     * Has anyone generated a search index for this Book?
     *
     * @return the status for the index of this book.
     * @see org.crosswire.jsword.index.IndexManager
     */
    /**
     * This method does not alter the index status, however it is for Indexers
     * that are responsible for indexing and have changed the status themselves.
     *
     * @param status the status for the index of this book
     * @see org.crosswire.jsword.index.IndexManager
     */
    var indexStatus: IndexStatus

    /**
     * Get an OSIS representation of information concerning this Book.
     *
     * @return the OSIS representation of information about this book.
     */
    fun toOSIS(): Document

    companion object {
        /**
         * The key for the type in the properties map
         */
        const val KEY_CATEGORY = "Category"

        /**
         * The key for the book in the properties map
         */
        const val KEY_BOOK = "Book"

        /**
         * The key for the driver in the properties map
         */
        const val KEY_DRIVER = "Driver"

        /**
         * The key for the name in the properties map
         */
        const val KEY_NAME = "Description"

        /**
         * The key for the language in the properties map
         */
        const val KEY_LANG = "Lang"

        /**
         * The key for the language in the properties map
         */
        const val KEY_LANGUAGE = "Language"

        /**
         * The key for the font in the properties map
         */
        const val KEY_FONT = "Font"

        /**
         * The key for the Versification property.
         */
        const val KEY_VERSIFICATION = "Versification"

        const val KEY_BOOKLIST = "BookList"

        const val KEY_SCOPE = "Scope"
    }
}
