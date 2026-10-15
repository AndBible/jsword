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
package org.crosswire.jsword.book.sword

import org.crosswire.common.activate.Activator
import org.crosswire.common.activate.Lock
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.basic.AbstractBook
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.ReadOnlyKeyList
import org.crosswire.jsword.passage.VerseRange
import fi.sykero.kdom.Content

/**
 * A Sword version of a generic book.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class SwordGenBook
/**
 * Construct an SwordGenBook given the BookMetaData and the AbstractBackend.
 *
 * @param sbmd the metadata that describes the book
 * @param backend the means by which the resource is accessed
 */
(sbmd: SwordBookMetaData, backend: Backend<*>?) : AbstractBook(sbmd, backend) {
    init {
        if (backend == null) {
            throw IllegalArgumentException("AbstractBackend must not be null.")
        }
    }

    /**
     * The filter to use to convert to OSIS.
     */
    @JvmField
    protected var filter: SourceFilter = sbmd.filter

    /**
     * The global key list
     */
    private var global: Key? = null

    /**
     * Are we active.
     * Volatile so that a reader thread sees the latest value without
     * needing to enter the synchronized block (cf. checkActive()).
     */
    @Volatile
    private var active = false

    /**
     * So we can quickly find a Key given the text for the key
     */
    private var map: MutableMap<String, Key>? = null

    /**
     * So we can implement getIndex() easily
     */
    private var set: Key? = null

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#activate(org.crosswire.common.activate.Lock)
     */
    @Synchronized
    final override fun activate(lock: Lock?) {
        super.activate(lock)

        val newSet = backend!!.readIndex()
        val newMap = HashMap<String, Key>()
        for (key in newSet!!) {
            newMap.put(key.getOsisRef(), key)
        }

        // Publish fully-populated state before flipping active=true so that
        // a concurrent reader observing active=true is guaranteed to see a
        // non-null, fully-populated map.
        set = newSet
        map = newMap
        global = ReadOnlyKeyList(newSet, false)
        active = true

        // We don't need to activate the backend because it should be capable
        // of doing it for itself.
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#deactivate(org.crosswire.common.activate.Lock)
     */
    @Synchronized
    final override fun deactivate(lock: Lock?) {
        super.deactivate(lock)

        // Flip active=false BEFORE nulling the fields so that a concurrent
        // reader passing checkActive() (active==true) is guaranteed to still
        // see non-null map/set/global. Otherwise a race between deactivate
        // and getKey() can produce NPE on map.get().
        active = false

        map = null
        set = null
        global = null
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getOsisIterator(org.crosswire.jsword.passage.Key, boolean, boolean)
     */
    @Throws(BookException::class)
    override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> {
        checkActive()

        jswordAssert(key != null) { "key must not be null" }

        return backend!!.readToOsis(key, object : RawTextToXmlProcessor {
            override fun preRange(range: VerseRange?, partialDom: MutableList<Content>?) {
                // no - op
            }

            override fun postVerse(verse: Key?, partialDom: MutableList<Content>?, rawText: String?) {
                partialDom!!.addAll(filter.toOSIS(this@SwordGenBook, verse, rawText))
            }

            override fun init(partialDom: MutableList<Content>?) {
                // no-op
            }
        }).iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        return backend!!.getRawText(key)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return backend!!.contains(key)
    }

    /*
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBook#getOsis(org.crosswire.jsword.passage.Key, org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor)
     */
    @Throws(BookException::class)
    public override fun getOsis(key: Key?, noOpRawTextProcessor: RawTextToXmlProcessor?): MutableList<Content> {
        checkActive()

        jswordAssert(key != null) { "key must not be null" }

        return backend!!.readToOsis(key, noOpRawTextProcessor)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isWritable()
     */
    override fun isWritable(): Boolean {
        return backend!!.isWritable
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setRawText(org.crosswire.jsword.passage.Key, java.lang.String)
     */
    @Throws(BookException::class)
    override fun setRawText(key: Key?, rawData: String?) {
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#setAliasKey(org.crosswire.jsword.passage.Key, org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun setAliasKey(alias: Key?, source: Key?) {
        throw BookException(JSOtherMsg.lookupText("This Book is read-only."))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getGlobalKeyList()
     */
    override fun getGlobalKeyList(): Key {
        checkActive()

        // Snapshot the field into a local reference. A concurrent deactivate()
        // may null the global field after checkActive() has returned, so
        // returning the field directly could hand back null to the caller. If
        // we lost that race, force a re-activation and re-snapshot before
        // falling back to an empty key list.
        var localGlobal = global
        if (localGlobal == null) {
            Activator.activate(this)
            localGlobal = global
        }
        if (localGlobal == null) {
            return createEmptyKeyList()
        }

        return localGlobal
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getValidKey(java.lang.String)
     */
    override fun getValidKey(name: String?): Key {
        try {
            return getKey(name)
        } catch (e: NoSuchKeyException) {
            return createEmptyKeyList()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#getKey(java.lang.String)
     */
    @Throws(NoSuchKeyException::class)
    override fun getKey(name: String?): Key {
        val text = name
        checkActive()

        // Snapshot the map into a local reference. A concurrent deactivate()
        // may null the map field after checkActive() has returned, so working
        // off the field directly could hit map.get() on null. If we lost that
        // race, force a re-activation and re-snapshot before giving up.
        var localMap = map
        if (localMap == null) {
            Activator.activate(this)
            localMap = map
        }
        if (localMap == null) {
            throw NoSuchKeyException(JSMsg.gettext("No entry for '{0}' in {1}.", text, initials))
        }

        val key = localMap[text]
        if (key != null) {
            return key
        }

        // First check for keys that match ignoring case
        for (keyName in localMap.keys) {
            if (keyName.equals(text, ignoreCase = true)) {
                return localMap[keyName]!!
            }
        }

        // Next keys that start with the given text
        for (keyName in localMap.keys) {
            if (keyName.startsWith(text!!)) {
                return localMap[keyName]!!
            }
        }

        // Next try keys that contain the given text
        for (keyName in localMap.keys) {
            if (keyName.indexOf(text!!) != -1) {
                return localMap[keyName]!!
            }
        }

        // TRANSLATOR: Error condition: Indicates that something could not be
        // found in the book.
        // {0} is a placeholder for the unknown key.
        // {1} is the short name of the book
        throw NoSuchKeyException(JSMsg.gettext("No entry for '{0}' in {1}.", text, initials))
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#createEmptyKeyList()
     */
    override fun createEmptyKeyList(): Key {
        return DefaultKeyList()
    }

    /**
     * Helper method so we can quickly activate ourselves on access
     */
    private fun checkActive() {
        if (!active) {
            Activator.activate(this)
        }
    }
}
