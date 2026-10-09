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
/**
 * The Basic package contains default and abstract implementations of the main
 * Book classes.
 */
package org.crosswire.jsword.book.basic

import org.crosswire.common.activate.Lock
import org.crosswire.common.util.Language
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.sword.AbstractBackend
import org.crosswire.jsword.book.sword.Backend
import org.crosswire.jsword.book.sword.processing.NoOpRawTextProcessor
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.index.IndexStatus
import org.crosswire.jsword.index.IndexStatusEvent
import org.crosswire.jsword.index.IndexStatusListener
import org.crosswire.jsword.index.search.DefaultSearchRequest
import org.crosswire.jsword.index.search.SearchRequest
import org.crosswire.jsword.index.search.Searcher
import org.crosswire.jsword.index.search.SearcherFactory
import org.crosswire.jsword.passage.Key
import org.jdom2.Content
import org.jdom2.Document
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AbstractBook implements a few of the more generic methods of Book. This class
 * does a lot of work in helping make search easier, and implementing some basic
 * write methods.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
abstract class AbstractBook
/**
 * Construct an AbstractBook given the BookMetaData and the AbstractBackend.
 * 
 * @param bmd the metadata that describes the book
 * @param backend the means by which the resource is accessed
 */
(
    /**
     * The meta data for this book
     */
    private var bmd: BookMetaData?,
    /**
     * To read the data from the Book
     */
    private val backendValue: Backend<*>?
) : Book {
    /**
     * How do we perform searches
     */
    private var searcher: Searcher? = null

    /**
     * The list of property change listeners
     */
    private var listeners: MutableList<IndexStatusListener>? = CopyOnWriteArrayList()

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getScope()
     */
    override val scope: Key?
        get() = getGlobalKeyList()

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getBookMetaData()
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setBookMetaData(org.crosswire.jsword.book.BookMetaData)
     */
    final override var bookMetaData: BookMetaData
        get() = bmd!!
        set(bmd) {
            this.bmd = bmd
        }

    val backend: Backend<*>?
        get() = backendValue

    /* (non-Javadoc)
     * @see org.crosswire.common.activate.Activatable#activate(org.crosswire.common.activate.Lock)
     */
    override fun activate(lock: Lock?) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.common.activate.Activatable#deactivate(org.crosswire.common.activate.Lock)
     */
    override fun deactivate(lock: Lock?) {
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#find(java.lang.String)
     */
    @Throws(BookException::class)
    override fun find(request: String?): Key? {
        return find(DefaultSearchRequest(request))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#find(org.crosswire.jsword.index.search.SearchRequest)
     */
    @Throws(BookException::class)
    override fun find(request: SearchRequest?): Key? {
        if (searcher == null) {
            try {
                searcher = SearcherFactory.createSearcher(this)
            } catch (ex: InstantiationException) {
                throw BookException(JSOtherMsg.lookupText("Failed to initialize the search index"), ex)
            }
        }

        return searcher!!.search(request)
    }

    override fun resetSearcher() {
        searcher = null
    }

    /**
     * Get this book.
     * 
     * @return this book
     */
    open val book: Book
        get() = this

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getDriver()
     */
    override val driver: BookDriver?
        get() = if (bmd == null) null else bmd!!.driver

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getDriverName()
     */
    override val driverName: String?
        get() = if (bmd == null) null else bmd!!.driverName

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#match(java.lang.String)
     */
    override fun match(name: String?): Boolean {
        if (name == null) {
            return false
        }
        if (name == initials) {
            return true
        }
        if (name == this.name) {
            return true
        }
        if (name.equals(initials, ignoreCase = true)) {
            return true
        }
        if (name.equals(this.name, ignoreCase = true)) {
            return true
        }
        if (name.startsWith(initials)) {
            return true
        }
        if (name.startsWith(this.name)) {
            return true
        }
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getIndexStatus()
     */
    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setIndexStatus(org.crosswire.jsword.index.IndexStatus)
     */
    override var indexStatus: IndexStatus
        get() = bmd!!.indexStatus
        set(newStatus) {
            val oldStatus = bmd!!.indexStatus
            bmd!!.indexStatus = newStatus
            firePropertyChange(oldStatus, newStatus)
        }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getAbbreviation()
     */
    override val abbreviation: String
        get() = bmd!!.abbreviation

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getInitials()
     */
    override val initials: String
        get() = bmd!!.initials

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getLanguage()
     */
    override val language: Language
        get() = bmd!!.language

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getName()
     */
    override val name: String
        get() = bmd!!.name

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getOsisID()
     */
    override val osisID: String
        get() = bmd!!.osisID

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getPropertyKeys()
     */
    override val propertyKeys: Set<String>?
        get() = bmd!!.propertyKeys

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getProperty(java.lang.String)
     */
    override fun getProperty(key: String?): String? {
        return bmd!!.getProperty(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#putProperty(java.lang.String, java.lang.Object)
     */
    override fun putProperty(key: String?, value: String?) {
        bmd!!.putProperty(key, value, false)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#putProperty(java.lang.String, java.lang.String, boolean)
     */
    override fun putProperty(key: String?, value: String?, forFrontend: Boolean) {
        bmd!!.putProperty(key, value, forFrontend)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getBookCategory()
     */
    override val bookCategory: BookCategory
        get() = bmd!!.bookCategory

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isLeftToRight()
     */
    override val isLeftToRight: Boolean
        get() = bmd!!.isLeftToRight

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isSupported()
     */
    override val isSupported: Boolean
        get() = bmd!!.isSupported

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isEnciphered()
     */
    override val isEnciphered: Boolean
        get() = bmd!!.isEnciphered

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isLocked()
     */
    override val isLocked: Boolean
        get() = bmd!!.isLocked

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#unlock(java.lang.String)
     */
    override fun unlock(unlockKey: String?): Boolean {
        val state = bmd!!.unlock(unlockKey)
        if (state) {
            // Double check.
            if (isUnlockKeyValid()) {
                return true
            } else {
                bmd!!.resetLock()
                return false
            }
        }
        return false
    }

    /**
     * This is a heuristic that tries out the key.
     * 
     * @return true if there were no exceptions in reading the enciphered
     * module.
     */
    private fun isUnlockKeyValid(): Boolean {
        try {
            val keys: Key? = getGlobalKeyList()
            var key = keys
            if (keys == null) {
                // weird key == null, assume it is valid
                return true
            }

            if (keys.getCardinality() > 0) {
                key = keys.get(0)
            }
            if (backendValue is AbstractBackend<*>) {
                backendValue.setIgnoreReadErrors(false)
            }
            getOsis(key, NoOpRawTextProcessor())
            return true
        } catch (ex: BookException) {
            return false
        } finally {
            if (backendValue is AbstractBackend<*>) {
                backendValue.setIgnoreReadErrors(true)
            }
        }
    }

    @Throws(BookException::class)
    protected abstract fun getOsis(key: Key?, noOpRawTextProcessor: RawTextToXmlProcessor?): List<Content>

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getUnlockKey()
     */
    override val unlockKey: String?
        get() = bmd!!.unlockKey

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isQuestionable()
     */
    override val isQuestionable: Boolean
        get() = bmd!!.isQuestionable

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#hasFeature(org.crosswire.jsword.book.FeatureType)
     */
    override fun hasFeature(feature: FeatureType?): Boolean {
        return bmd!!.hasFeature(feature)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#addIndexStatusListener(org.crosswire.jsword.index.IndexStatusListener)
     */
    override fun addIndexStatusListener(li: IndexStatusListener?) {
        if (listeners == null) {
            listeners = CopyOnWriteArrayList()
        }
        listeners!!.add(li!!)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#removeIndexStatusListener(org.crosswire.jsword.index.IndexStatusListener)
     */
    override fun removeIndexStatusListener(li: IndexStatusListener?) {
        if (listeners == null) {
            return
        }

        listeners!!.remove(li)
    }

    /**
     * Reports bound property changes. If `oldValue` and
     * `newValue` are not equal and the
     * `PropertyChangeEvent` listener list isn't empty, then fire a
     * `PropertyChange` event to each listener.
     * 
     * @param oldStatus
     * the old value of the property (as an Object)
     * @param newStatus
     * the new value of the property (as an Object)
     */
    protected open fun firePropertyChange(oldStatus: IndexStatus?, newStatus: IndexStatus?) {
        if (oldStatus != null && newStatus != null && oldStatus == newStatus) {
            return
        }

        val ev = IndexStatusEvent(this, newStatus)
        for (listener in listeners!!) {
            listener.statusChanged(ev)
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#toOSIS()
     */
    override fun toOSIS(): Document {
        return bmd!!.toOSIS()
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(other: Any?): Boolean {
        // Since this can not be null
        if (other == null) {
            return false
        }

        // We might consider checking for equality against all Books?
        // However currently we don't.

        // Check that that is the same as this
        // Don't use instanceof since that breaks inheritance
        if (other.javaClass != this.javaClass) {
            return false
        }

        // The real bit ...
        val that = other as Book

        return bmd!! == that.bookMetaData
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        return bmd!!.hashCode()
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: Book): Int {
        return this.bmd!!.compareTo(other.bookMetaData)
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return bmd!!.toString()
    }
}
