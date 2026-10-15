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
package org.crosswire.jsword.book.basic

import org.crosswire.common.util.Language
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.KeyType
import org.crosswire.jsword.index.IndexStatus
import fi.sykero.kdom.Document
import org.crosswire.common.util.KUri

/**
 * An implementation of the Property Change methods from BookMetaData.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */

/**
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br></br>
 * The copyright to this program is held by its authors.
 */
abstract class AbstractBookMetaData : BookMetaData {

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getKeyType()
     */
    override val keyType: KeyType?
        get() = KeyType.LIST

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getDriver()
     */
    override val driver: BookDriver?
        get() = driverValue

    private var driverValue: BookDriver? = null

    /**
     * @param driver The driver to set.
     */
    open fun setDriver(driver: BookDriver?) {
        this.driverValue = driver
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getDriverName()
     */
    override val driverName: String?
        get() {
            if (driver == null) {
                return null
            }

            return driver!!.getDriverName()
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#hasFeature(org.crosswire.jsword.book.FeatureType)
     */
    override fun hasFeature(feature: FeatureType?): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getOsisID()
     */
    override val osisID: String
        get() = bookCategory.getName() + '.' + initials

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isSupported()
     */
    override val isSupported: Boolean
        get() = true

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isEnciphered()
     */
    override val isEnciphered: Boolean
        get() = false

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isLocked()
     */
    override val isLocked: Boolean
        get() = false

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#unlock(java.lang.String)
     */
    override fun unlock(unlockKey: String?): Boolean {
        return false
    }

    override fun resetLock() {}

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getUnlockKey()
     */
    override val unlockKey: String?
        get() = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isQuestionable()
     */
    override val isQuestionable: Boolean
        get() = false

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getLanguage()
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#setLanguage(org.crosswire.common.util.Language)
     */
    override var language: Language
        get() = languageValue!!
        set(language) {
            languageValue = language
        }
    private var languageValue: Language? = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getLibrary()
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#setLibrary(org.crosswire.common.util.KUri)
     */
    // Native Throws excludes setters; retain the JVM checked-exception contract.
    @Suppress("WRONG_ANNOTATION_TARGET", "WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET")
    @set:Throws(BookException::class)
    override var library: KUri? = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#setLocation(org.crosswire.common.util.KUri)
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getLocation()
     */
    override var location: KUri? = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getIndexStatus()
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#setIndexStatus(org.crosswire.jsword.index.IndexStatus)
     */
    override var indexStatus: IndexStatus = IndexStatus.UNDONE

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#reload()
     */
    @Throws(BookException::class)
    override fun reload() {
        // over ride this if partial loads are allowed
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#putProperty(java.lang.String, java.lang.String)
     */
    override fun putProperty(key: String?, value: String?) {
        putProperty(key, value, false)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#toOSIS()
     */
    override fun toOSIS(): Document {
        throw UnsupportedOperationException("If you want to use this, implement it.")
    }

    override fun equals(other: Any?): Boolean {
        // Since this can not be null
        if (other == null) {
            return false
        }

        // We might consider checking for equality against all BookMetaDatas?
        // However currently we don't.

        // Check that that is the same as this
        // Don't use instanceof since that breaks inheritance
        if (other::class != this::class) {
            return false
        }

        // The real bit ...
        val that = other as BookMetaData

        return bookCategory == that.bookCategory && name == that.name && initials == that.initials
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    /*
     * The sort order should be based on initials rather than name because name often begins with general words like 'The ...'
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: BookMetaData): Int {
        var result = this.bookCategory.compareTo(other.bookCategory)
        if (result == 0) {
            // module names normally (always?) use English characters so ignore case should be fine
            result = this.abbreviation.compareTo(other.abbreviation, ignoreCase = true)
        }
        if (result == 0) {
            result = this.initials.compareTo(other.initials)
        }
        if (result == 0) {
            result = this.name.compareTo(other.name)
        }
        return result
    }

    override fun toString(): String {
        val internal = initials
        val abbreviation = abbreviation
        if (internal == abbreviation) {
            return internal
        }
        val buf = StringBuilder(internal)
        buf.append('(')
        buf.append(abbreviation)
        buf.append(')')
        return buf.toString()
    }
}
