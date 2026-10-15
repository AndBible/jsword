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

import org.crosswire.common.diff.Diff
import org.crosswire.common.diff.DiffCleanup
import org.crosswire.common.diff.Difference
import org.crosswire.common.util.jswordAssert
import org.crosswire.common.xml.JDOMSAXEventProvider
import org.crosswire.common.xml.SAXEventProvider
import org.crosswire.jsword.passage.Key
import org.crosswire.jsword.passage.KeyUtil
import org.crosswire.jsword.passage.Passage
import org.crosswire.jsword.passage.RestrictionType
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.VersificationsMapper
import org.crosswire.jsword.versification.system.Versifications
import fi.sykero.kdom.Content
import fi.sykero.kdom.Document
import fi.sykero.kdom.Element
import fi.sykero.kdom.Namespace
import fi.sykero.kdom.Text


/**
 * BookData is the assembler of the OSIS that is returned by the filters. As
 * such it puts that into an OSIS document. When several books are supplied, it
 * gets the data from each and puts it into a parallel or interlinear view.
 * Note: it is critical that all the books are able to understand the same key.
 * That does not mean that each has to have content for each key. Missing keys
 * are represented by empty cells.
 *
 * @author Joe Walker
 * @author DM Smith
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
open class BookData : BookProvider {
    /**
     * What key was used to create this data
     */
    private var keyField: Key?

    /**
     * The books to which the key should be applied.
     */
    private var books: Array<Book?>?

    /**
     * Whether the Books should be compared.
     */
    private var comparingBooks: Boolean = false

    /**
     * The complete OSIS container for the element
     */
    private var osisRoot: Element? = null

    /**
     * Just the element
     */
    private var fragment: Element? = null

    private var unaccenter: UnAccenter? = null

    /**
     * Create a BookData.
     * 
     * @param book the Book to which the data belongs
     * @param key the Key specifying the data
     */
    constructor(book: Book?, key: Key?) {
        jswordAssert(book != null) { "book" }
        jswordAssert(key != null) { "key" }

        this.keyField = key

        books = arrayOfNulls<Book>(1)
        books!![0] = book
    }

    /**
     * Create BookData for multiple books.
     * 
     * @param books the set of Books to which the data belongs
     * @param key the Key specifying the data
     * @param compare when true each pair of adjacent books is to be compared
     */
    constructor(books: Array<Book?>, key: Key?, compare: Boolean) {
        jswordAssert(books.size > 0) { "books" }
        jswordAssert(key != null) { "key" }

        this.books = books.copyOf()
        this.keyField = key
        this.comparingBooks = compare
    }

    /**
     * Accessor for the root OSIS element
     * 
     * @return the root of the OSIS document representing this data
     * @throws BookException if there is any problem with this request
     */
    // Native Throws excludes getters; retain the JVM checked-exception contract.
    @Suppress("WRONG_ANNOTATION_TARGET", "WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET")
    @get:Throws(BookException::class)
    open val osis: Element
        get() {
            if (osisRoot == null) {
                // TODO(DMS): Determine the proper representation of the OSISWork
                // name for multiple books.
                osisRoot = OSISUtil.createOsisFramework(firstBook!!.bookMetaData)
                val text = osisRoot!!.getChild(OSISUtil.OSIS_ELEMENT_OSISTEXT)
                val div = osisFragment
                text!!.addContent(div)
            }

            return osisRoot!!
        }

    /**
     * Accessor for the requested data in OSIS format.
     * 
     * @return the fragment of the OSIS document representing this data
     * @throws BookException if there is any problem with this request
     */
    // Native Throws excludes getters; retain the JVM checked-exception contract.
    @Suppress("WRONG_ANNOTATION_TARGET", "WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET")
    @get:Throws(BookException::class)
    open val osisFragment: Element
        get() {
            if (fragment == null) {
                fragment = getOsisContent(true)
            }

            return fragment!!
        }

    /**
     * Accessor for the root OSIS element
     * 
     * @param allowGenTitles whether to generate titles
     * @return the root of the document
     * @throws BookException if there is any problem with this request
     */
    @Throws(BookException::class)
    open fun getOsisFragment(allowGenTitles: Boolean): Element {
        if (fragment == null) {
            fragment = getOsisContent(allowGenTitles)
        }

        return fragment!!
    }

    /**
     * Output the current data as a SAX stream.
     *
     * @return A way of posting SAX events
     * @throws BookException if there is any problem with this request
     */
    @Throws(BookException::class)
    open fun getSAXEventProvider(): SAXEventProvider {
        // If the fragment is already in a document, then use that.
        val frag = osisFragment
        var doc: Document? = frag.document
        if (doc == null) {
            doc = Document(frag)
        }
        return JDOMSAXEventProvider(doc)
    }

    /**
     * Who created this data.
     *
     * @return Returns the book.
     */
    @Suppress("UNCHECKED_CAST")
    override fun getBooks(): Array<Book> {
        return (if (books == null) null else books!!.copyOf()) as Array<Book>
    }

    /**
     * Get the first book.
     * 
     * @return the first or only book
     */
    override val firstBook: Book?
        get() = if (books != null && books!!.size > 0) books!![0] else null

    /**
     * The key used to obtain data from one or more books.
     *
     * @return Returns the key.
     */
    open val key: Key?
        get() = keyField

    /**
     * @return whether the books should be compared.
     */
    open val isComparingBooks: Boolean
        get() = comparingBooks

    @Throws(BookException::class)
    private fun getOsisContent(allowGenTitles: Boolean): Element {
        val books = this.books!!
        val div = OSISUtil.factory().createDiv()

        if (books.size == 1) {
            val iter = books[0]!!.getOsisIterator(keyField, false, allowGenTitles)
            while (iter.hasNext()) {
                val content = iter.next()
                div.addContent(content)
            }
        } else {
            val table = OSISUtil.factory().createTable()
            var row = OSISUtil.factory().createRow()
            var cell = OSISUtil.factory().createCell()

            table.addContent(row)

            val iters = arrayOfNulls<Iterator<Content>>(books.size)
            val passages = arrayOfNulls<Passage>(books.size)
            val showDiffs = BooleanArray(books.size - 1)
            var doDiffs = false

            //iterate through a first time mapping out our data. This enables us to detect a difference in number
            //of ranges later on and flag it to the user...
            val ommittedVerses = BooleanArray(books.size)
            var numRangesInMasterPassage = 0
            for (i in books.indices) {
                //although the osis iterator now caters for keys in different versifications
                //we are going to want to analyse the resulting key, so let's do the conversion up-front
                passages[i] = VersificationsMapper.instance().map(KeyUtil.getPassage(keyField), getVersification(i))

                //iterator takes care of versification differences here...
                iters[i] = books[i]!!.getOsisIterator(passages[i], true, true)

                if (i == 0) {
                    //we never omit a verse for the first passage, since we're going to output everything based on that.
                    ommittedVerses[i] = false
                    numRangesInMasterPassage = passages[i]!!.countRanges(RestrictionType.NONE)
                } else {
                    // basically, if we end up with more ranges than we started with, then we're omitting a verse
                    //somewhere along the lines.
                    ommittedVerses[i] = passages[i]!!.countRanges(RestrictionType.NONE) > numRangesInMasterPassage
                }
            }


            //now read the content and map it out
            val booksContents = arrayOfNulls<VerseContentMap>(books.size)
            for (i in books.indices) {
                doDiffs = doDiffs or addHeaderAndSetShowDiffsState(row, showDiffs, i, ommittedVerses[i])
                booksContents[i] = keyIteratorContentByVerse(
                    getVersification(i),
                    iters[i]!!)
            }

            var cellCount = 0
            var rowCount = 0

            //we iterate through the first book's contents, and match the verses from all the other ones
            for (verseContent in booksContents[0]!!.entries) {
                cellCount = 0
                row = OSISUtil.factory().createRow()
                var firstText: String? = ""

                for (i in books.indices) {
                    val book = books[i]!!
                    cell = OSISUtil.factory().createCell()
                    var lang = book.language
                    if (lang != null) {
                        cell.setAttribute(OSISUtil.OSIS_ATTR_LANG, lang.code, Namespace.XML_NAMESPACE)
                    }

                    row.addContent(cell)

                    val newText = StringBuilder(if (doDiffs) 32 else 0)

                    //get the contents from the mapped verse - key might be null if we had content outside of a verse.
                    //might be a no-op if it's in the same versification.
                    val verseInRelavantBookContents: Key? = VersificationsMapper.instance().mapVerse(verseContent.key, getVersification(i))

                    //key might have several child keys, ie. a verse mapping to a range, or list of verses
                    val passageOfInterest = KeyUtil.getPassage(verseInRelavantBookContents)
                    val passageKeys = passageOfInterest!!.iterator()
                    while (passageKeys.hasNext()) {
                        val singleKey = passageKeys.next()
                        //TODO(CJB): for performance, we probably want to avoid the instanceof, so either change the
                        //method signature, or cast directly and be optimistic
                        if (singleKey !is Verse) {
                            throw UnsupportedOperationException("Iterating through a passage gives non-verses")
                        }

                        var xmlContent: MutableList<Content>? = booksContents[i]!![singleKey]

                        //if the book simply did not contain that reference (say Greek book, with Gen.1 as a reference)
                        //then we end up with a key that doesn't exist in the map. Therefore, we need to cope for this.
                        if (xmlContent == null) {
                            xmlContent = ArrayList<Content>(0)
                        }

                        addText(doDiffs, newText, xmlContent)

                        if (doDiffs) {
                            var thisText: String? = newText.toString()
                            if (unaccenter != null) {
                                thisText = unaccenter!!.unaccent(thisText)
                            }

                            if (i > 0 && showDiffs[i - 1]) {
                                val diffs = Diff(firstText!!, thisText!!, false).compare()
                                DiffCleanup.cleanupSemantic(diffs)
                                cell.addContent(OSISUtil.diffToOsis(diffs))

                                // Since we used that cell create another
                                cell = OSISUtil.factory().createCell()
                                lang = book.language
                                cell.setAttribute(OSISUtil.OSIS_ATTR_LANG, lang.code, Namespace.XML_NAMESPACE)
                                row.addContent(cell)
                            }
                            if (i == 0) {
                                firstText = thisText
                            }
                        }

                        //TODO(CJB): wrong location - we should record the keys in a set and notify
                        //when there is a problem
                        //this should be outside of the loop?
                        addContentSafely(cell, xmlContent)
                        cellCount++
                    }
                }

                if (cellCount == 0) {
                    break
                }

                table.addContent(row)
                rowCount++
            }
            if (rowCount > 0) {
                div.addContent(table)
            }
        }

        return div
    }

    /**
     * JDOM will throw an exception if we try and add the content to multiple parents.
     * As a result, we take the opportunity to add it safely, and add a note indicating
     * this content appears twice.
     *
     * @param cell the element to be added
     * @param xmlContent the collector of content
     */
    private fun addContentSafely(cell: Element, xmlContent: List<Content>) {
        var note: Element? = null
        for (c in xmlContent) {
            if (c.parent == null) {
                cell.addContent(c)
            } else if (note != null) {
                note.addContent(c.clone())
            } else {
                //we're in the situation where we have added this already.
                //add note. In this case, we wrap the content that has already been applied.
                note = appendVersificationNotice(cell, "duplicate")
                note.addContent(c.clone())
            }
        }
    }

    /**
     * Creates a notice element.
     *
     * @param parent the parent to which the notice is added
     * @param notice the notice fragment to be applied to the sub-type
     * @return the new element
     */
    private fun appendVersificationNotice(parent: Element, notice: String): Element {
        val note = OSISUtil.factory().createDiv()
        note.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.GENERATED_CONTENT)
        note.setAttribute(OSISUtil.OSIS_ATTR_SUBTYPE, OSISUtil.TYPE_X_PREFIX + notice)
        parent.addContent(note)
        return note
    }

    /**
     * @param i the current position in the array of books
     * @return the versification of the book.
     */
    private fun getVersification(i: Int): Versification {
        return Versifications.instance().getVersification(
            books!![i]!!.bookMetaData.getProperty(BookMetaData.KEY_VERSIFICATION))!!
    }


    /**
     * We iterate through the content, making sure we key together those bits that belong together.
     * And separating out each verse.
     *
     * @param v11n the versification for the content
     * @param iter the iterator of OSIS content
     * @return the verse content for the book
     * @throws BookException if there is any problem with this request
     */
    @Throws(BookException::class)
    private fun keyIteratorContentByVerse(v11n: Versification, iter: Iterator<Content>): VerseContentMap {
        val contentsByOsisID = VerseContentMap()

        //we will be using this map later to track whi ch keys have been catered for in the order calculation
        var currentVerse: Verse? = null
        var content: Content

        var contents: MutableList<Content> = ArrayList()
        while (iter.hasNext()) {
            content = iter.next()
            if (content is Element && OSISUtil.OSIS_ELEMENT_VERSE == content.name) {
                if (currentVerse != null) {
                    contentsByOsisID[currentVerse] = contents
                    contents = ArrayList()
                }

                currentVerse = OSISUtil.getVerse(v11n, content)

                //if we still have stuff in here, then let's assign it to the previous verse (i.e.
                //we might have come across content that legitimately sits in verse 0 for example).
                //of perhaps we've somehow come across previous content. Either way, it clearly doesn't
                //belong to the current verse.
                if (contents.size > 0) {
                    val previousVerse = Verse(currentVerse!!.versification, currentVerse.ordinal - 1)
                    contentsByOsisID[previousVerse] = contents
                    contents = ArrayList()
                }
            }

            contents.add(content)
        }

        //now append what's left into the last verse
        if (currentVerse != null) {
            contentsByOsisID[currentVerse] = contents
        }

        return contentsByOsisID
    }

    /**
     * @param row           our current OSIS row
     * @param showDiffs     the array of states as to whether we are showing diffs for this column
     * @param i             our current place in the state
     * @param ommittedVerse true to indicate this column will be ommiting a verse
     * @return true if we are doing diffs
     */
    private fun addHeaderAndSetShowDiffsState(row: Element, showDiffs: BooleanArray, i: Int, ommittedVerse: Boolean): Boolean {
        var doDiffs = false
        val book = books!![i]!!
        var cell = OSISUtil.factory().createHeaderCell()

        if (i > 0) {
            val firstBook = books!![0]!!
            val category = book.bookCategory

            val prevCategory = firstBook.bookCategory
            val prevName = firstBook.initials
            showDiffs[i - 1] = comparingBooks && BookCategory.BIBLE == category && category == prevCategory
                    && book.language == firstBook.language && book.initials != prevName

            if (showDiffs[i - 1]) {
                doDiffs = true
                val buf = StringBuilder(firstBook.initials)
                buf.append(" ==> ")
                buf.append(book.initials)

                cell.addContent(OSISUtil.factory().createText(buf.toString()))
                row.addContent(cell)
                cell = OSISUtil.factory().createHeaderCell()
            }
        }

        val text = OSISUtil.factory().createText(book.initials)
        if (ommittedVerse) {
            val notice = this.appendVersificationNotice(cell, "omitted-verses")
            notice.addContent(text)
        } else {
            cell.addContent(text)
        }
        row.addContent(cell)
        return doDiffs
    }

    /**
     * Loops around contents and calls addText for a single element
     *
     * @param doDiffs  true for calculating differences
     * @param newText  the newText buffer used to compare one portion of text to another
     * @param contents the contents to be added
     */
    private fun addText(doDiffs: Boolean, newText: StringBuilder, contents: List<Content>) {
        for (c in contents) {
            addText(doDiffs, newText, c)
        }
    }

    /**
     * Adds the text to the diff buffer
     *
     * @param doDiffs true for calculating differences
     * @param newText the newText buffer used to compare one portion of text to another
     * @param content the content element to be added
     */
    private fun addText(doDiffs: Boolean, newText: StringBuilder, content: Content) {
        if (doDiffs) {
            // if we already have content, let's add a space to avoid chaining words together
            if (newText.length != 0) {
                newText.append(' ')
            }

            if (content is Element) {
                newText.append(OSISUtil.getCanonicalText(content))
            } else if (content is Text) {
                newText.append(content.text)
            }
        }
    }

    /**
     * @param unaccenter the unaccenter to set
     */
    open fun setUnaccenter(unaccenter: UnAccenter?) {
        this.unaccenter = unaccenter
    }

    /**
     * A temporary holder for a map that links each verse ID to its set of OSIS elements.
     * Used purely to avoid having too many generic/array notations entangled in the code
     */
}

/**
 * Verse contents of one book, replacing a `TreeMap<Verse, MutableList<Content>>` (K5b rule 26). `Verse.compareTo`
 * compares only the ordinal, so the map is keyed by ordinal. All keys of one instance come from one versification
 * (`keyIteratorContentByVerse` builds one per book), which makes ordinal-only keying equivalent to the TreeMap.
 * As in `TreeMap.put`, a second put for an equal ordinal replaces the value but keeps the first key object.
 */
internal class VerseContentMap {
    /** A verse with its content, like a `Map.Entry`. */
    class Entry(val key: Verse, var value: MutableList<Content>)

    private val byOrdinal = HashMap<Int, Entry>()

    operator fun set(key: Verse, value: MutableList<Content>) {
        val existing = byOrdinal[key.ordinal]
        if (existing != null) existing.value = value else byOrdinal[key.ordinal] = Entry(key, value)
    }

    operator fun get(key: Verse): MutableList<Content>? = byOrdinal[key.ordinal]?.value

    /** The entries in ascending ordinal order (a snapshot). */
    val entries: List<Entry>
        get() = byOrdinal.values.sortedBy { it.key.ordinal }

    val size: Int get() = byOrdinal.size
}
