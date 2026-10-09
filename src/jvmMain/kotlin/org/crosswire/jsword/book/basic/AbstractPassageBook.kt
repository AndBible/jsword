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

import org.crosswire.common.util.StringUtil
import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.book.BookData
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.sword.Backend
import org.crosswire.jsword.book.sword.processing.RawTextToXmlProcessor
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.NoSuchKeyException
import org.crosswire.jsword.passage.PassageKeyFactory
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseKey
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.BibleBook
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.VersificationsMapper
import org.crosswire.jsword.versification.system.Versifications
import org.jdom2.Content
import org.jdom2.Element
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory
import java.util.HashSet
import java.util.LinkedHashSet

/**
 * An abstract implementation of Book that lets implementors just concentrate on
 * reading book data.
 *
 * @author Joe Walker
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br></br>
 * The copyright to this program is held by its authors.
 */
abstract class AbstractPassageBook
/**
 * Construct an AbstractPassageBook given the BookMetaData and the AbstractBackend.
 *
 * @param bmd     the metadata that describes the book
 * @param backend the means by which the resource is accessed
 */
(bmd: BookMetaData, backend: Backend<*>?) : AbstractBook(bmd, backend) {
    /**
     * The name of the versification or null
     */
    private val versificationName: String?

    /**
     * Versification system, created lazily, so use getter
     */
    private var versificationSystem: Versification? = null

    /**
     * Our key manager
     */
    private val keyf: PassageKeyFactory

    /**
     * lazy of cache of bible books contained in the Book
     */
    @Volatile
    private var bibleBooksCache: Set<BibleBook>? = null

    init {
        keyf = PassageKeyFactory.instance()
        this.versificationName = bmd.getProperty(BookMetaData.KEY_VERSIFICATION)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#getOsisIterator(org.crosswire.jsword.passage.Key, boolean, boolean)
     */
    @Throws(BookException::class)
    override fun getOsisIterator(key: Key?, allowEmpty: Boolean, allowGenTitles: Boolean): MutableIterator<Content> {
        // Note: allowEmpty indicates parallel view
        // TODO(DMS): make the iterator be demand driven
        val filter = getFilter()

        // For all the ranges in this Passage
        //TODO(CJB): I'd prefer to do the key mapping in KeyUtil, and pass in our current versification.
        //we could remove the method that doesn't support the versification parameter.
        //but that has far reaching consequences.
        val ref = VersificationsMapper.instance().map(KeyUtil.getPassage(key), this.versification)

        // Generated titles are shown when
        // there are 2 or more ranges or
        // empty are not allowed and generated titles are allowed
        val showTitles = ref.hasRanges(RestrictionType.CHAPTER) || (!allowEmpty && allowGenTitles)

        val processor: RawTextToXmlProcessor = object : RawTextToXmlProcessor {
            // track previous text to exclude duplicates caused by merged verses
            private var previousVerseText: String? = ""

            override fun preRange(range: VerseRange?, partialDom: MutableList<Content>?) {
                if (showTitles) {
                    val title = OSISUtil.factory().createGeneratedTitle()
                    title.addContent(range!!.getName())
                    partialDom!!.add(title)
                }
            }

            override fun postVerse(verse: Key?, partialDom: MutableList<Content>?, rawText: String?) {
                // If the verse is empty or repeated then we shouldn't add the verse
                if ((allowEmpty || rawText!!.length > 0) && previousVerseText != rawText) {
                    val osisContent = filter.toOSIS(this@AbstractPassageBook, verse, rawText)
                    addOSIS(verse, partialDom, osisContent)
                }
                previousVerseText = rawText
            }

            override fun init(partialDom: MutableList<Content>?) {
                // no-op
            }
        }

        // getOsis returns a read-only List type for Kotlin implementers; every implementation returns a java.util.List
        return (getOsis(ref, processor) as MutableList<Content>).iterator()
    }

    /**
     * Add the OSIS elements to the div element. Note, this assumes that the
     * data is fully marked up.
     *
     * @param key         The key being added
     * @param div         The div element to which the key's OSIS representation is
     * being added
     * @param osisContent The OSIS representation of the key being added.
     */
    open fun addOSIS(key: Key?, div: Element?, osisContent: MutableList<Content>?) {
        jswordAssert(key != null) { "Assertion failed" }
        div!!.addContent(osisContent)
    }

    /**
     * Add the OSIS elements to the content list. Note, this assumes that the
     * data is fully marked up.
     *
     * @param key         The key being added
     * @param content     The list to which the key's OSIS representation is being added
     * @param osisContent The OSIS representation of the key being added.
     */
    open fun addOSIS(key: Key?, content: MutableList<Content>?, osisContent: MutableList<Content>?) {
        jswordAssert(key != null) { "Assertion failed" }
        content!!.addAll(osisContent!!)
    }

    /**
     * What filter should be used to filter data in the format produced by this
     * Book?. In some ways this method is more suited to BookMetaData however we
     * do not have a specialization of BookMetaData to fit AbstractPassageBook
     * and it doesn't like any higher in the hierarchy at the moment so I will
     * leave this here.
     */
    protected abstract fun getFilter(): SourceFilter

    /**
     * For when we want to add writing functionality. This does not work.
     *
     * @param key
     * @param bdata
     * @throws BookException
     */
    @Throws(BookException::class)
    open fun setDocument(key: Key?, bdata: BookData?) {
        // For all of the sections
        for (nextElem in OSISUtil.getFragment(bdata!!.osisFragment)) {
            if (nextElem is Element) {
                val div = nextElem

                // For all of the Verses in the section
                for (data in div.content) {
                    if (data is Element) {
                        val overse = data
                        val text = OSISUtil.getPlainText(overse)

                        setRawText(key, text)
                    } else {
                        log.error("Ignoring non OSIS/Verse content of DIV.")
                    }
                }
            } else {
                log.error("Ignoring non OSIS/Verse content of DIV.")
            }
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.Book#isWritable()
     */
    override fun isWritable(): Boolean {
        return false
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.passage.KeyFactory#createEmptyKeyList()
     */
    final override fun createEmptyKeyList(): Key {
        return keyf.createEmptyKeyList(Versifications.instance().getVersification(versificationName))
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
    final override fun getKey(name: String?): Key {
        return PassageKeyFactory.instance().getKey(Versifications.instance().getVersification(versificationName), name)
    }

    open val versification: Versification
        get() {
            if (this.versificationSystem == null) {
                this.versificationSystem = Versifications.instance().getVersification(bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION))
            }
            return versificationSystem!!
        }

    /**
     * This implementation lazily inits, saves to the JSword conf file and also caches the book list for future use.
     *
     * @return the list of Bible books contained in the module
     */
    open val bibleBooks: Set<BibleBook>
        get() {
            if (bibleBooksCache == null) {
                synchronized(this) {
                    if (bibleBooksCache == null) {
                        bibleBooksCache = getBibleBooksInternal()
                    }
                }
            }

            return bibleBooksCache!!
        }

    /**
     * Obtains the set of bible books from the internal configuration file, creating it if required.
     * @return the bible books relevant to this module.
     */
    private fun getBibleBooksInternal(): Set<BibleBook> {
        val list = this.bookMetaData.getProperty(BookMetaData.KEY_BOOKLIST)
        val books: Set<BibleBook>
        if (list == null) {
            //calculate and store
            books = calculateBibleBookList()
            val listOfBooks = toString(books)
            this.putProperty(BookMetaData.KEY_BOOKLIST, listOfBooks)
        } else {
            //iterate through each item and get the books as a bible books
            books = fromString(list)
        }

        return books
    }

    private fun fromString(list: String): Set<BibleBook> {
        // fromExactOSIS can return null for an unknown OSIS id; Java stored it in the set as is
        val books: MutableSet<BibleBook?> = LinkedHashSet(list.length / 2)
        val bookOsis = StringUtil.split(list, ' ')
        for (s in bookOsis) {
            books.add(BibleBook.fromExactOSIS(s))
        }
        @Suppress("UNCHECKED_CAST")
        return books as Set<BibleBook>
    }

    private fun toString(books: Set<BibleBook>): String {
        val sb = StringBuilder(books.size * 8)
        val iterator = books.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            sb.append(b.osis)
            if (iterator.hasNext()) {
                sb.append(' ')
            }
        }
        return sb.toString()
    }

    /**
     * Iterate all books checking if document contains a verse from the book
     */
    private fun calculateBibleBookList(): Set<BibleBook> {
        val bookMetaData = this.bookMetaData
        val scope = this.scope as VerseKey<*>?
        if (scope == null) {
            return HashSet()
        }

        val bookList: MutableSet<BibleBook> = LinkedHashSet()

        // iterate over all book possible in this document
        val v11n = Versifications.instance().getVersification(bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION))
        val v11nBookIterator = v11n!!.bookIterator

        while (v11nBookIterator.hasNext()) {
            val bibleBook = v11nBookIterator.next()
            // test some random verses - normally ch1 v 1 is sufficient - but we don't want to miss any
            if (scope.contains(Verse(v11n, bibleBook, 1, 1))
                || scope.contains(Verse(v11n, bibleBook, 1, 2))
            ) {
                bookList.add(bibleBook)
            }
        }

        return bookList
    }

    private companion object {
        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(AbstractPassageBook::class)
    }
}
