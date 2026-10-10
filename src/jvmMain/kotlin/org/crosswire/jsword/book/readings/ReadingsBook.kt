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
 * An implementation of BookDriver and Dictionary that read provide access to
 * readings planners.
 */
package org.crosswire.jsword.book.readings

import org.crosswire.common.util.toJavaLocale

import java.util.ArrayList
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.MissingResourceException
import org.crosswire.common.util.KswordLocale
import org.crosswire.common.util.ResourceBundles
import java.util.TreeMap

import org.crosswire.common.util.Language
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.FeatureType
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.basic.AbstractBook
import org.crosswire.jsword.book.basic.DefaultBookMetaData
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.internationalisation.LocaleProviderManager
import org.crosswire.jsword.passage.DefaultKeyList
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.PreferredKey
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.SetKeyList
import org.crosswire.jsword.versification.system.Versifications
import org.jdom2.Content
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory

/**
 * A Dictionary that displays daily Readings.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class ReadingsBook
/**
 * Constructor for ReadingsBook.
 * @param driver the driver for this book
 * @param setname the name of this book
 * @param type the type of book
 */
(driver: ReadingsBookDriver?, setname: String?, type: BookCategory?) : AbstractBook(null, null), PreferredKey { // set the book metadata later and no backend
    /**
     * The global key list
     */
    private val global: Key

    /**
     * The store of keys and data
     */
    private val hash: MutableMap<Key, String> = TreeMap<Key, String>()

    init {
        //although we're getting this from the LocaleProviderManager, we're still setting 
        //the language on the metadata once, so won't cope for dynamic changes
        val defaultLocale = LocaleProviderManager.getLocale().toJavaLocale()
        val prop = ResourceBundles.get(setname!!, KswordLocale.current())
            ?: throw MissingResourceException("Can't find bundle for base name $setname, locale $defaultLocale", setname, "")

        // TRANSLATOR: The default name for JSword's Reading plan.
        var bookName = JSMsg.gettext("Readings")
        val title = prop.getString("title")
        if (title != null) {
            bookName = title
        } else {
            log.warn("Missing resource: 'title' while parsing: {}", setname)
        }

        val bmd = DefaultBookMetaData(driver, bookName, type)
        bmd.setInitials(setname)
        bmd.language = Language(defaultLocale.language)
        bookMetaData = bmd

        // Go through the current year
        val greg: java.util.Calendar = java.util.GregorianCalendar()
        greg.set(java.util.Calendar.DAY_OF_MONTH, 1)
        greg.set(java.util.Calendar.MONDAY, java.util.Calendar.JANUARY)
        val currentYear = greg.get(java.util.Calendar.YEAR)

        while (greg.get(java.util.Calendar.YEAR) == currentYear) {
            val internalKey = ReadingsKey.external2internal(greg)
            var readings = ""

            val found = prop.getString(internalKey)
            if (found != null) {
                readings = found
                hash.put(ReadingsKey(greg.time), readings)
            } else {
                log.warn("Missing resource: {} while parsing: {}", internalKey, setname)
            }

            greg.add(java.util.Calendar.DATE, 1)
        }

        global = SetKeyList(hash.keys, name)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.PreferredKey#getPreferred()
     */
    override fun getPreferred(): Key {
        return ReadingsKey(Date())
    }

    @Throws(BookException::class)
    override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> {
        if (key !is ReadingsKey) {
            // TRANSLATOR: Error condition: Indicates that something could not be found in the book. {0} is a placeholder for the unknown key.
            throw BookException(JSMsg.gettext("Key not found {0}", key!!.getName()))
        }

        // TODO(DMS): make the iterator be demand driven
        val content: MutableList<Content> = ArrayList<Content>()

        val title = OSISUtil.factory().createTitle()
        title.addContent(key.getName())
        content.add(title)

        val readings = hash.get(key)
        if (readings == null) {
            // TRANSLATOR: Error condition: Indicates that something could not be found in the book. {0} is a placeholder for the unknown key.
            throw BookException(JSMsg.gettext("Key not found {0}", key.getName()))
        }

        try {
            // AV11N(DMS): Is this right?
            // At this point and time, ReadingsBook is a verse list from the KJV.
            // Should store the v11n in the ReadingsBook
            val keyf = PassageKeyFactory.instance()
            val ref = keyf.getKey(Versifications.instance().getVersification("KJV"), readings)

            val list = OSISUtil.factory().createList()
            content.add(list)

            val it = ref.rangeIterator(RestrictionType.NONE)
            while (it.hasNext()) {
                val range: Key = it.next()

                val reading = OSISUtil.factory().createReference()
                reading.setAttribute(OSISUtil.OSIS_ATTR_REF, range.getOsisRef())
                reading.addContent(range.getName())

                val item = OSISUtil.factory().createItem()
                item.addContent(reading)
                list.addContent(item)
            }
        } catch (ex: NoSuchKeyException) {
            content.add(OSISUtil.factory().createText(JSOtherMsg.lookupText("Failed to parse {0}", readings)))
        }

        return content.iterator()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#contains(org.crosswire.jsword.passage.Key)
     */
    override fun contains(key: Key?): Boolean {
        return false
    }

    /** Returns an empty string
     * (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getRawText(org.crosswire.jsword.passage.Key)
     */
    @Throws(BookException::class)
    override fun getRawText(key: Key?): String {
        return ""
    }

    /**
     * Returns an empty list
     */
    @Throws(BookException::class)
    public override fun getOsis(key: Key?, noOpRawTextProcessor: RawTextToXmlProcessor?): List<Content> {
        return Collections.emptyList()
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isWritable()
     */
    override fun isWritable(): Boolean {
        return false
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
     * @see org.crosswire.jsword.book.Book#getValidKey(java.lang.String)
     */
    override fun getValidKey(name: String?): Key {
        try {
            return getKey(name)
        } catch (e: NoSuchKeyException) {
            return createEmptyKeyList()
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getKey(java.lang.String)
     */
    @Throws(NoSuchKeyException::class)
    override fun getKey(name: String?): Key {
        val reply = DefaultKeyList()
        reply.addAll(ReadingsKey(name, name, global))
        return reply
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getGlobalKeyList()
     */
    override fun getGlobalKeyList(): Key {
        return global
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#createEmptyKeyList()
     */
    override fun createEmptyKeyList(): Key {
        return DefaultKeyList()
    }

    override fun hasFeature(feature: FeatureType?): Boolean {
        return feature == FeatureType.DAILY_DEVOTIONS
    }

    private companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(ReadingsBook::class)
    }
}
