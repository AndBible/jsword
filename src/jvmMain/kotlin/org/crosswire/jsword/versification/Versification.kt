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
/**
 * <p>
 * The core responsibility of the versification package is to understand verse references.
 * It standardizes on OSIS book names and on a KJV versification. Each book in the KJV is
 * numbered sequentially and each verse of the KJV is numbered sequentially.
 * </p>
 *
 * <p>
 * The primary abilities of this package are to flexibly understand Bible book names
 * as might be found in a reference work or supplied by a user, to convert between
 * these and OSIS book names and KJV book number.
 * </p>
 *
 * <p>
 * One can also request information concerning books of the Bible: The number of chapters
 * or verses in a book, the number of verses in a particular chapter, the ordinal position
 * of a verse in the KJV and so forth.
 * </p>
 */
package org.crosswire.jsword.versification

import java.io.PrintStream
import java.util.Locale

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.JSMsg
import org.crosswire.jsword.JSOtherMsg
import org.crosswire.jsword.book.ReferenceSystem
import org.crosswire.jsword.passage.NoSuchVerseException
import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseRange

/**
 * A named Versification defines
 * the order of BibleBooks by Testament,
 * the number of chapters in each BibleBook,
 * the number of verses in each chapter.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Versification
/**
 * Construct a Versification.
 * 
 * @param name
 *            The name of this reference system
 * @param booksOT
 *            An ordered list of books in this reference system. The list
 *            should not include INTRO_BIBLE, or INTRO_OT.
 * @param booksNT
 *            An ordered list of books in this reference system. The list
 *            should not include INTRO_NT.
 * @param lastVerseOT
 *            For each book in booksOT, this has an array with one entry for
 *            each chapter whose value is the highest numbered verse in that
 *            chapter. Do not include chapter 0.
 * @param lastVerseNT
 *            For each book in booksNT, this has an array with one entry for
 *            each chapter whose value is the highest numbered verse in that
 *            chapter. Do not include chapter 0.
 */
(
    /**
     * Get the OSIS name for this Versification.
     * @return the OSIS name of the Versification
     */
    open val name: String,
    booksOT: Array<BibleBook>,
    booksNT: Array<BibleBook>,
    lastVerseOT: Array<IntArray>,
    lastVerseNT: Array<IntArray>
) : ReferenceSystem {
    /** The ordered list of books in this versification.  */
    private var bookList: BibleBookList

    /** The last ordinal number of the Old Testament  */
    private var otMaxOrdinal: Int = 0

    /** The last ordinal number of the New Testament and the maximum ordinal number of this Reference System  */
    private var ntMaxOrdinal: Int = 0

    /** Constant for the max verse number in each chapter  */
    private var lastVerse: Array<IntArray>

    /**
     * Constant for the ordinal number of the first verse in each chapter.
     */
    private var chapterStarts: Array<IntArray>

    /**
     * The number of chapters in the OT, not counting chapter 0
     */
    private var otChapterCount: Int = 0

    /**
     * The number of chapters in the NT, not counting chapter 0
     */
    private var ntChapterCount: Int = 0

    /**
     * The Java no-arg constructor left every field null; the fields are non-null in Kotlin, so this
     * builds an empty versification (INTRO_BIBLE only) named "". Nothing in jsword or :app calls it.
     */
    constructor() : this("", emptyArray(), emptyArray(), emptyArray(), emptyArray())

    init {
        // Copy the books into an aggregated BibleBook array
        // including INTRO_BIBLE and INTRO_OT/INTRO_NT for non-null book lists
        var bookCount = 1 // Always include the INTRO_BIBLE
        if (booksOT.size > 0) {
            bookCount += booksOT.size + 1 // All of the OT books and INTRO_OT
        }

        val ntStart = bookCount
        if (booksNT.size > 0) {
            bookCount += booksNT.size + 1 // All of the NT books and INTRO_NT
        }
        @Suppress("UNCHECKED_CAST")
        val books = arrayOfNulls<BibleBook>(bookCount) as Array<BibleBook>
        books[0] = BibleBook.INTRO_BIBLE
        if (booksOT.size > 0) {
            books[1] = BibleBook.INTRO_OT
            System.arraycopy(booksOT, 0, books, 2, booksOT.size)
        }

        if (booksNT.size > 0) {
            books[ntStart] = BibleBook.INTRO_NT
            System.arraycopy(booksNT, 0, books, ntStart + 1, booksNT.size)
        }

        this.bookList = BibleBookList(books)

        var ordinal = 0

        // Create an independent copy of lastVerse.
        @Suppress("UNCHECKED_CAST")
        this.lastVerse = arrayOfNulls<IntArray>(bookCount) as Array<IntArray>
        var bookIndex = 0

        // Add in the bible introduction
        var chapters = IntArray(1)
        chapters[0] = 0
        this.lastVerse[bookIndex++] = chapters

        // Now append the OT info
        if (lastVerseOT.size > 0) {
            // Add in the testament intro
            chapters = IntArray(1)
            chapters[0] = 0
            this.lastVerse[bookIndex++] = chapters
            // then all the testament info
            for (i in lastVerseOT.indices) {
                val src = lastVerseOT[i]
                this.otChapterCount += src.size
                // Add one as the location for chapter 0.
                val dest = IntArray(src.size + 1)
                this.lastVerse[bookIndex++] = dest
                // The last verse of chapter 0 is 0
                dest[0] = 0
                // copy the last verse array for the chapter
                System.arraycopy(src, 0, dest, 1, src.size)
            }
        }

        // Now append the NT info
        if (lastVerseNT.size > 0) {
            // Add in the testament intro
            chapters = IntArray(1)
            chapters[0] = 0
            this.lastVerse[bookIndex++] = chapters
            // then all the testament info
            for (i in lastVerseNT.indices) {
                val src = lastVerseNT[i]
                this.ntChapterCount += src.size
                // Add one as the location for chapter 0.
                val dest = IntArray(src.size + 1)
                this.lastVerse[bookIndex++] = dest
                // The last verse of chapter 0 is 0
                dest[0] = 0
                // copy the last verse array for the chapter
                System.arraycopy(src, 0, dest, 1, src.size)
            }
        }

        // Initialize chapterStarts to be a parallel array to lastVerse,
        // but with chapter starts
        @Suppress("UNCHECKED_CAST")
        this.chapterStarts = arrayOfNulls<IntArray>(bookCount) as Array<IntArray>
        bookIndex = 0
        while (bookIndex < bookCount) {

            // Remember where the OT ends
            if (bookList.getBook(bookIndex) == BibleBook.INTRO_NT) {
                // This is not reached for a v11n without a NT.
                this.otMaxOrdinal = ordinal - 1
            }

            // Save off the chapter starts
            val src = this.lastVerse[bookIndex]
            val numChapters = src.size
            val dest = IntArray(numChapters)
            this.chapterStarts[bookIndex] = dest
            for (chapterIndex in 0 until numChapters) {
                // Save off the chapter start
                dest[chapterIndex] = ordinal

                // Set ordinal to the start of the next chapter or book introduction.
                // The number of verses in each chapter, when including verse 0,
                // is one more that the largest numbered verse in the chapter.
                ordinal += src[chapterIndex] + 1
            }
            bookIndex++
        }

        // Remember where the NT ends
        this.ntMaxOrdinal = ordinal - 1

        // The MT v11n has no NT, so at this point otMaxOrdinal == 0
        if (booksNT.size == 0) {
            this.otMaxOrdinal = this.ntMaxOrdinal
        }
        //        Versification.dump(System.out, this.osisName, this.bookList, this.lastVerse);
        //        Versification.dump(System.out, this.osisName, this.bookList, this.chapterStarts);
    }

    /**
     * Does this Versification contain the BibleBook.
     *
     * @param book
     * @return true if it is present.
     */
    open fun containsBook(book: BibleBook?): Boolean {
        return bookList.contains(book)
    }

    /**
     * Get the BibleBook by its position in this Versification.
     * If the position is negative, return the first book.
     * If the position is greater than the last, return the last book.
     *
     * @param ordinal
     * @return the indicated book
     */
    open fun getBook(ordinal: Int): BibleBook {
        return bookList.getBook(ordinal)
    }

    /**
     * Get a book from its name.
     *
     * @param find
     *            The string to identify
     * @return The BibleBook, On error null
     */
    open fun getBook(find: String?): BibleBook? {
        val book = BibleNames.instance().getBook(find)
        if (containsBook(book)) {
            return book
        }
        return null
    }

    /**
     * Get the number of books in this Versification.
     * @return the number of books
     */
    open val bookCount: Int
        get() = bookList.bookCount

    /**
     * The number of books between two verses includes
     * the books of the two verses and everything in between.
     * 
     * @param start
     *            The first Verse in the range
     * @param end The last Verse in the range
     * @return The number of books. Always &gt;= 1.
     */
    open fun getBookCount(start: Verse?, end: Verse?): Int {
        val startBook = bookList.getOrdinal(start!!.book)
        val endBook = bookList.getOrdinal(end!!.book)

        return endBook - startBook + 1
    }

    /**
     * Return the first book in the list.
     *
     * @return the first book in the list
     */
    open val firstBook: BibleBook
        get() = bookList.firstBook

    /**
     * Return the first book in the list.
     *
     * @return the first book in the list
     */
    open val lastBook: BibleBook
        get() = bookList.lastBook

    /**
     * Given a BibleBook, get the next BibleBook in this Versification. If it is the last book, return null.
     * @param book A BibleBook in the Versification
     * @return the previous BibleBook or null.
     */
    open fun getNextBook(book: BibleBook?): BibleBook? {
        return bookList.getNextBook(book)
    }

    /**
     * Given a BibleBook, get the previous BibleBook in this Versification. If it is the first book, return null.
     * @param book A BibleBook in the Versification
     * @return the previous BibleBook or null.
     */
    open fun getPreviousBook(book: BibleBook?): BibleBook? {
        return bookList.getPreviousBook(book)
    }

    /**
     * Get the BibleBooks in this Versification.
     *
     * @return an Iterator over the books
     */
    open val bookIterator: Iterator<BibleBook>
        get() = bookList.iterator()

    /**
     * Get the BookName.
     *
     * @param book the desired book
     * @return The requested BookName
     */
    open fun getBookName(book: BibleBook?): BookName? {
        return BibleNames.instance().getBookName(book)
    }

    /**
     * Get the preferred name of a book. Altered by the case setting (see
     * setBookCase() and isFullBookName())
     *
     * @param book the desired book
     * @return The full name of the book
     */
    open fun getPreferredName(book: BibleBook?): String {
        return BibleNames.instance().getPreferredName(book)
    }

    /**
     * Get the preferred name of a book in specified locale. Altered by the case setting (see
     * setBookCase() and isFullBookName())
     *
     * @param book the desired book
     * @param locale the desired locale for book name. If left null, returns with default locale
     * @return The full name of the book or null if not in this versification
     */
    open fun getPreferredNameInLocale(book: BibleBook?, locale: Locale?): String? {
        if (containsBook(book)) {
            if (locale != null) {
                return BibleNames.instance().getPreferredNameInLocale(book, locale)
            }
            return BibleNames.instance().getPreferredName(book)
        }
        return null
    }

    /**
     * Get the full name of a book (e.g. "Genesis"). Altered by the case setting
     * (see setBookCase())
     *
     * @param book the book of the Bible
     * @return The full name of the book
     */
    open fun getLongName(book: BibleBook?): String {
        return BibleNames.instance().getLongName(book)
    }

    /**
     * Get the short name of a book (e.g. "Gen"). Altered by the case setting
     * (see setBookCase())
     *
     * @param book the book of the Bible
     * @return The short name of the book
     */
    open fun getShortName(book: BibleBook?): String {
        return BibleNames.instance().getShortName(book)
    }

    /**
     * Is the given string a valid book name. If this method returns true then
     * getBook() will return a BibleBook and not null.
     *
     * @param find
     *            The string to identify
     * @return true when the book name is recognized
     */
    open fun isBook(find: String?): Boolean {
        return getBook(find) != null
    }

    /**
     * Get the last valid chapter number for a book.
     *
     * @param book
     *            The book part of the reference.
     * @return The last valid chapter number for a book.
     */
    open fun getLastChapter(book: BibleBook?): Int {
        // This is faster than doing the check explicitly, unless
        // The exception is actually thrown, then it is a lot slower
        // I'd like to think that the norm is to get it right
        try {
            return lastVerse[bookList.getOrdinal(book)].size - 1
        } catch (ex: NullPointerException) {
            return 0
        } catch (ex: ArrayIndexOutOfBoundsException) {
            return 0
        }
    }

    /**
     * Get the last valid verse number for a chapter.
     *
     * @param book
     *            The book part of the reference.
     * @param chapter
     *            The current chapter
     * @return The last valid verse number for a chapter
     */
    open fun getLastVerse(book: BibleBook?, chapter: Int): Int {
        // This is faster than doing the check explicitly, unless
        // The exception is actually thrown, then it is a lot slower
        // I'd like to think that the norm is to get it right
        try {
            return lastVerse[bookList.getOrdinal(book)][chapter]
        } catch (ex: NullPointerException) {
            return 0
        } catch (ex: ArrayIndexOutOfBoundsException) {
            return 0
        }
    }

    open fun getLastVerse(): Verse {
        val lastBook = this.lastBook
        val lastChapter = getLastChapter(lastBook)
        val lastVerse = getLastVerse(lastBook, lastChapter)
        return Verse(this, lastBook, lastChapter, lastVerse)
    }

    /**
     * Get the number of chapters in the Bible not counting Chapter 0.
     * which is the one per book and one for the INTRO_BIBLE, INTRO_OT and INTRO_NT;
     * 
     * @return the number of chapters in this Versification
     */
    open val chapterCount: Int
        get() = otChapterCount + ntChapterCount

    /**
     * Get the number of chapters in the OT not counting Chapter 0.
     * which is the one per book and one for the INTRO_OT.
     * 
     * @return the number of chapters in the OT of this versification
     */
    open fun getOTChapterCount(): Int {
        return otChapterCount
    }

    /**
     * Get the number of chapters in the NT not counting Chapter 0.
     * which is the one per book and one for the INTRO_NT.
     * 
     * @return the number of chapters in the OT of this versification
     */
    open fun getNTChapterCount(): Int {
        return ntChapterCount
    }
    /**
     * Get a VerseRange encompassing this Versification.
     * 
     * @return a VerseRange for the whole versification
     */
    open val allVerses: VerseRange
        get() {
            val first = Verse(this, bookList.firstBook, 0, 0)
            val last = getLastVerse()
            return VerseRange(this, first, last)
        }

    /**
     * An introduction is a Verse that has a verse number of 0.
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isIntro(verse: Verse?): Boolean {
        val v = verse!!.verse
        return v == 0
    }

    /**
     * A book introduction is an introduction
     * that has a chapter of 0.
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isBookIntro(verse: Verse?): Boolean {
        return 0 == verse!!.chapter && isIntro(verse)
    }

    /**
     * A chapter introduction is an introduction
     * that has a chapter other than 0
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isChapterIntro(verse: Verse?): Boolean {
        return 0 != verse!!.chapter && isIntro(verse)
    }

    /**
     * The start of a chapter is indicated by
     * a verse number of 0 or 1
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isStartOfChapter(verse: Verse?): Boolean {
        val v = verse!!.verse
        return v <= 1
    }

    /**
     * The end of the chapter is indicated by
     * the verse number matching the last in the chapter.
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isEndOfChapter(verse: Verse?): Boolean {
        val b = verse!!.book
        val v = verse.verse
        val c = verse.chapter
        return v == getLastVerse(b, c)
    }

    /**
     * The start of a book is indicated by
     * a chapter number of 0 or 1 and
     * a verse number of 0 or 1.
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isStartOfBook(verse: Verse?): Boolean {
        val v = verse!!.verse
        val c = verse.chapter
        return v <= 1 && c <= 1
    }

    /**
     * The end of the book is indicated by
     * the chapter number matching the last chapter
     * in the book and the verse number matching
     * the last verse in the chapter.
     *
     * @param verse the verse to test
     * @return true or false ...
     */
    open fun isEndOfBook(verse: Verse?): Boolean {
        val b = verse!!.book
        val v = verse.verse
        val c = verse.chapter
        return v == getLastVerse(b, c) && c == getLastChapter(b)
    }

    /**
     * Two verses are in the same chapter if both
     * the book and chapter agree.
     *
     * @param first
     *            The verse to compare to
     * @param second
     *            The verse to compare to
     * @return true or false ...
     */
    open fun isSameChapter(first: Verse?, second: Verse?): Boolean {
        return first!!.book === second!!.book && first.chapter == second.chapter
    }

    /**
     * Two verse are adjacent if one immediately follows the other,
     * even across book boundaries. Introductions are considered
     * as having "zero width" in this determination. That is the
     * last verse in a chapter or book is adjacent every verse that
     * follows up to and including verse 1 of the next chapter in
     * the versification.
     * <br>
     * For example:<br>
     * The last verse in the Old Testament is adjacent to:
     * <ul>
     * <li>Intro.NT - the New Testament introduction</li>
     * <li>Matt 0:0 - the book introduction</li>
     * <li>Matt 1:0 - the chapter introduction</li>
     * <li>Matt 1:1 - the first verse of Matt</li>
     * </ul>
     * Note: the verses can be in any order.
     *
     * @param first
     *            The verse to compare to
     * @param second
     *            The verse to compare to
     * @return true or false ...
     */
    open fun isAdjacentChapter(first: Verse?, second: Verse?): Boolean {
        val before = min(first, second)
        val after = max(first, second)
        if (isSameBook(first, second)) {
            return after!!.chapter - before!!.chapter == 1
        }
        // The earlier verse has to be the  last chapter
        return (isAdjacentBook(before, after)
                && getLastChapter(before!!.book) == before.chapter
                && after!!.chapter <= 1)
    }

    /**
     * Two verses are in the same book
     * when they have the same book.
     *
     * @param first
     *            The verse to compare to
     * @param second
     *            The verse to compare to
     * @return true or false ...
     */
    open fun isSameBook(first: Verse?, second: Verse?): Boolean {
        return first!!.book === second!!.book
    }

    /**
     * Two verses are in adjacent books if one book
     * follows the other in this versification.
     * Note: the verses can be in any order.
     *
     * @param first
     *            The verse to compare to
     * @param second
     *            The verse to compare to
     * @return true or false ...
     */
    open fun isAdjacentBook(first: Verse?, second: Verse?): Boolean {
        return Math.abs(bookList.getOrdinal(second!!.book) - bookList.getOrdinal(first!!.book)) == 1
    }

    /**
     * Is this verse adjacent to another verse
     *
     * @param first
     *            The first verse in the comparison
     * @param second
     *            The second verse in the comparison
     * @return true if the verses are adjacent.
     */
    open fun isAdjacentVerse(first: Verse?, second: Verse?): Boolean {
        val before = min(first, second)
        val after = max(first, second)
        if (isSameChapter(first, second)) {
            return after!!.verse - before!!.verse == 1
        }
        // The earlier verse has to be the last verse in the chapter
        return (isAdjacentChapter(before, after)
                && getLastVerse(before!!.book, before.chapter) == before.verse
                && after!!.verse <= 1)
    }

    /**
     * How many verses are there in between the 2 Verses. The answer is -ve if
     * start is bigger than end. The answer is inclusive of start and exclusive
     * of end, so that <code>distance(gen11, gen12) == 1</code>
     *
     * @param start
     *            The first Verse in the range
     * @param end The last Verse in the range
     * @return The count of verses between this and that.
     */
    open fun distance(start: Verse?, end: Verse?): Int {
        return end!!.ordinal - start!!.ordinal
    }

    /**
     * Determine the earlier of the two verses.
     * If first == second then return first.
     * 
     * @param first the first verse to compare
     * @param second the second verse to compare
     * @return The earlier of the two verses
     */
    open fun min(first: Verse?, second: Verse?): Verse? {
        return if (first!!.ordinal <= second!!.ordinal) first else second
    }

    /**
     * Determine the later of the two verses.
     * If first == second then return first.
     * 
     * @param first the first verse to compare
     * @param second the second verse to compare
     * @return The later of the two verses
     */
    open fun max(first: Verse?, second: Verse?): Verse? {
        return if (first!!.ordinal > second!!.ordinal) first else second
    }

    /**
     * Get the verse n down from here this Verse.
     *
     * @param verse
     *            The verse to use as a start
     * @param n
     *            The number to count down by
     * @return The new Verse
     */
    open fun subtract(verse: Verse?, n: Int): Verse? {
        // Check to see if we are blurring 
        if (n == 0) {
            return verse
        }
        val newVerse = verse!!.verse - n
        // Try the simple case of the verse being in the same chapter
        if (newVerse >= 0) {
            return Verse(verse.versification, verse.book, verse.chapter, newVerse)
        }
        return decodeOrdinal(verse.ordinal - n)
    }

    /**
     * Get the verse that is a verses on from the one we've got.
     *
     * @param verse
     *            The verse to use as a start
     * @return The new verse or null if there is no next verse
     */
    open fun next(verse: Verse?): Verse? {
        // Cannot increment past the end.
        if (verse!!.ordinal == ntMaxOrdinal) {
            return null
        }

        var nextBook: BibleBook? = verse.book
        var nextChapter = verse.chapter
        var nextVerse = verse.verse + 1
        if (nextVerse > getLastVerse(nextBook, nextChapter)) {
            // Go to an introduction.
            nextVerse = 0
            // of the next chapter
            nextChapter += 1
            // check to see that the chapter is valid for the book
            if (nextChapter > getLastChapter(nextBook)) {
                // To to an introduction
                nextChapter = 0
                // of the next book
                nextBook = bookList.getNextBook(verse.book)
            }
        }

        // nextBook is null when we try to increment past the last verse
        // The test at the beginning is designed to prevent that
        if (nextBook == null) {
            jswordAssert(false) { "" }
            return null
        }

        return Verse(this, nextBook, nextChapter, nextVerse)
    }

    /**
     * Get the verse that is a few verses on from the one we've got.
     *
     * @param verse
     *            The verse to use as a start
     * @param n
     *            the number of verses later than the one we're one
     * @return The new verse
     */
    open fun add(verse: Verse?, n: Int): Verse? {
        // Check to see if we are blurring 
        if (n == 0) {
            return verse
        }
        val newVerse = verse!!.verse + n
        // Try the simple case of the verse being in the same chapter
        if (newVerse <= getLastVerse(verse.book, verse.chapter)) {
            return Verse(verse.versification, verse.book, verse.chapter, newVerse)
        }
        return decodeOrdinal(verse.ordinal + n)
    }

    /**
     * The number of chapters between two verses includes
     * the chapters of the two verses and everything in between.
     * 
     * @param start
     *            The first Verse in the range
     * @param end The last Verse in the range
     * @return The number of chapters. Always &gt;= 1.
     */
    open fun getChapterCount(start: Verse?, end: Verse?): Int {
        var startBook: BibleBook? = start!!.book
        val startChap = start.chapter
        var endBook: BibleBook? = end!!.book
        val endChap = end.chapter

        if (startBook == endBook) {
            return endChap - startChap + 1
        }

        // So we are going to have to count up chapters from start to end
        var total = getLastChapter(startBook) - startChap
        startBook = bookList.getNextBook(startBook)
        endBook = bookList.getPreviousBook(endBook)
        var b = startBook
        while (b != endBook) {
            total += getLastChapter(b)
            b = bookList.getNextBook(b)
        }
        total += endChap

        return total
    }

    /**
     * The maximum number of verses in the Bible, including module, testament, book and chapter introductions.
     *
     * @return the number of addressable verses in this versification.
     */
    open fun maximumOrdinal(): Int {
        // This is the same as the last ordinal in the Reference System.
        return ntMaxOrdinal
    }

    /**
     * Where does this verse come in the Bible. The value that this returns should be treated as opaque, useful for a bit set.
     * The introductions to the Book, OT/NT Testaments, Bible books and chapters are included here.
     * <ul>
     * <li>0 - INTRO_BIBLE 0:0 - The Book introduction</li>
     * <li>1 - INTRO_OT 0:0 - The OT Testament introduction</li>
     * <li>2 - Gen 0:0 - The introduction to the book of Genesis</li>
     * <li>3 - Gen 1:0 - The introduction to Genesis chapter 1</li>
     * <li>4 - Gen 1:1</li>
     * <li>...</li>
     * <li>35 - Gen 1:31</li>
     * <li>36 - Gen 2:0 - The introduction to Genesis chapter 2</li>
     * <li>37 - Gen 2:1</li>
     * <li>...</li>
     * <li>n - last verse in the OT</li>
     * <li>n + 1 - INTRO_NT, 0, 0 - The New Testament introduction</li>
     * <li>n + 2 - Matt 0:0 - The introduction to Matt</li>
     * <li>n + 3 - Matt 1:0 - The introduction to Matt 1</li>
     * <li>n + 4 - Matt 1:1</li>
     * <li>...</li>
     * </ul>
     * 
     * If the verse is not in this versification, return 0.
     *
     * @param verse
     *            The verse to convert
     * @return The ordinal number of verses
    */
    open fun getOrdinal(verse: Verse?): Int {
        try {
            return chapterStarts[bookList.getOrdinal(verse!!.book)][verse.chapter] + verse.verse
        } catch (e: ArrayIndexOutOfBoundsException) {
            return 0
        }
    }

    /**
     * Get the ordinal position of the BibleBook in this Versification.
     * Note: OT and NT intros are treated as books.
     * 
     * @param book
     * @return the position of the BibleBook
     */
    open fun getOrdinal(book: BibleBook?): Int {
        return bookList.getOrdinal(book)
    }
    /**
     * Determine the ordinal value for this versification given the
     * ordinal value in a testament. If the ordinal is out of bounds it
     * is constrained to be within the boundaries of the testament.
     * This unwinds getTestamentOrdinal.
     * 
     * @param testament the testament in which the ordinal value pertains
     * @param testamentOrdinal the ordinal value within the testament
     * @return the ordinal value for the versification as a whole
     */
    open fun getOrdinal(testament: Testament?, testamentOrdinal: Int): Int {
        var ordinal = if (testamentOrdinal >= 0) testamentOrdinal else 0
        if (Testament.NEW == testament) {
            ordinal = otMaxOrdinal + testamentOrdinal
            return if (ordinal <= ntMaxOrdinal) ordinal else ntMaxOrdinal
        }
        return if (ordinal <= otMaxOrdinal) ordinal else otMaxOrdinal
    }

    /**
     * Where does this verse come in the Bible. The value that this returns should be treated as opaque, useful for a bit set.
     * The introductions to the Book, OT/NT Testaments, Bible books and chapters are included here.
     * <ul>
     * <li>0 - INTRO_BIBLE 0:0 - The Book introduction</li>
     * <li>1 - INTRO_OT 0:0 - The OT Testament introduction</li>
     * <li>2 - Gen 0:0 - The introduction to the book of Genesis</li>
     * <li>3 - Gen 1:0 - The introduction to Genesis chapter 1</li>
     * <li>4 - Gen 1:1</li>
     * <li>...</li>
     * <li>35 - Gen 1:31</li>
     * <li>36 - Gen 2:0 - The introduction to Genesis chapter 2</li>
     * <li>37 - Genesis 2:1</li>
     * <li>...</li>
     * <li>n - last verse in the OT</li>
     * <li>0 - INTRO_NT, 0, 0 - The New Testament introduction</li>
     * <li>1 - Matt 0:0 - The introduction to Matt</li>
     * <li>2 - Matt 1:0 - The introduction to Matt 1</li>
     * <li>3 - Matt 1:1</li>
     * <li>...</li>
     * </ul>
     *
     * @param ordinal
     *            The ordinal number of the verse to convert
     * @return The ordinal number of the Verse within its Testament
     */
    open fun getTestamentOrdinal(ordinal: Int): Int {
        val ntOrdinal = otMaxOrdinal + 1
        if (ordinal >= ntOrdinal) {
            return ordinal - ntOrdinal + 1
        }
        return ordinal
    }

    /**
     * Get the testament of a given verse
     * @param ordinal the ordinal position of the verse in the whole Bible
     * @return the testament in which that verse is found
     */
    open fun getTestament(ordinal: Int): Testament {
        if (ordinal > otMaxOrdinal) {
            // This is an NT verse
            return Testament.NEW
        }
        // This is an OT verse
        return Testament.OLD
    }

    /**
     * Give the count of verses in the testament or the whole Bible.
     *
     * @param testament The testament to count. If null, then all testaments.
     * @return the number of verses in the testament
     */
    open fun getCount(testament: Testament?): Int {
        val total = ntMaxOrdinal + 1
        if (testament == null) {
            return total
        }

        val otCount = otMaxOrdinal + 1
        if (testament == Testament.OLD) {
            return otCount
        }

        return total - otCount
    }

    /**
     * Where does this verse come in the Bible. This will unwind the value returned by getOrdinal(Verse).
     * If the ordinal value is less than 0 or greater than the last verse in this Versification,
     * then constrain it to the first or last verse in this Versification.
     *
     * @param ordinal
     *            The ordinal number of the verse
     * @return A Verse
     */
    open fun decodeOrdinal(ordinal: Int): Verse {
        var ord = ordinal

        if (ord < 0) {
            ord = 0
        } else if (ord > ntMaxOrdinal) {
            ord = ntMaxOrdinal
        }

        // Handle three special cases
        // Book/Module introduction
        if (ord == 0) {
            return Verse(this, BibleBook.INTRO_BIBLE, 0, 0)
        }

        // OT introduction
        if (ord == 1) {
            return Verse(this, BibleBook.INTRO_OT, 0, 0)
        }

        // NT introduction
        if (ord == otMaxOrdinal + 1) {
            return Verse(this, BibleBook.INTRO_NT, 0, 0)
        }

        // To find the book, do a binary search in chapterStarts against chapter 0
        var low = 0
        var high = chapterStarts.size
        var match = -1

        while (high - low > 1) {
            // use >>> to keep mid always in range
            val mid = (low + high) ushr 1

            // Compare the for the item at "mid"
            val cmp = chapterStarts[mid][0] - ord
            if (cmp < 0) {
                low = mid
            } else if (cmp > 0) {
                high = mid
            } else {
                match = mid
                break
            }
        }

        // If we didn't have an exact match then use the low value
        val bookIndex = if (match >= 0) match else low
        val book = bookList.getBook(bookIndex)

        // To find the chapter, do a binary search in chapterStarts against bookIndex
        low = 0
        high = chapterStarts[bookIndex].size
        match = -1

        while (high - low > 1) {
            // use >>> to keep mid always in range
            val mid = (low + high) ushr 1

            // Compare the for the item at "mid"
            val cmp = chapterStarts[bookIndex][mid] - ord
            if (cmp < 0) {
                low = mid
            } else if (cmp > 0) {
                high = mid
            } else {
                match = mid
                break
            }
        }

        // If we didn't have an exact match then use the low value
        val chapterIndex = if (match >= 0) match else low
        val verse = if (chapterIndex == 0) 0 else ord - chapterStarts[bookIndex][chapterIndex]
        return Verse(this, book, chapterIndex, verse)
    }

    /**
     * Does the following represent a real verse?. It is code like this that
     * makes me wonder if I18 is done well/worth doing. All this code does is
     * check if the numbers are valid, but the exception handling code is huge
     * :(
     *
     * @param book
     *            The book part of the reference.
     * @param chapter
     *            The chapter part of the reference.
     * @param verse
     *            The verse part of the reference.
     * @exception NoSuchVerseException
     *                If the reference is illegal
     */
    @Throws(NoSuchVerseException::class)
    open fun validate(book: BibleBook?, chapter: Int, verse: Int) {
        validate(book, chapter, verse, false)
    }

    /**
     * Does the following represent a real verse?. It is code like this that
     * makes me wonder if I18 is done well/worth doing. All this code does is
     * check if the numbers are valid, but the exception handling code is huge
     * :(
     *
     * @param book
     *            The book part of the reference.
     * @param chapter
     *            The chapter part of the reference.
     * @param verse
     *            The verse part of the reference.
     * @param silent
     *            true to indicate we do not want to throw an exception
     * @return true if validation was succesful
     * @exception NoSuchVerseException
     *                If the reference is illegal and silent was false
     */
    @Throws(NoSuchVerseException::class)
    open fun validate(book: BibleBook?, chapter: Int, verse: Int, silent: Boolean): Boolean {
        // Check the book
        if (book == null) {
            if (silent) {
                return false
            }
            // TRANSLATOR: The user did not supply a book for a verse reference.
            throw NoSuchVerseException(JSOtherMsg.lookupText("Book must not be null"))
        }

        if (!bookList.contains(book)) {
            if (silent) {
                return false
            }
            // TRANSLATOR: The user supplied a book which does not exist in this versification.
            throw NoSuchVerseException(JSOtherMsg.lookupText("Book must be present in the versification"))
        }

        // Check the chapter
        val maxChapter = getLastChapter(book)
        if (chapter < 0 || chapter > maxChapter) {
            if (silent) {
                return false
            }
            // TRANSLATOR: The user supplied a chapter that was out of bounds. This tells them what is allowed.
            // {0} is the lowest value that is allowed. This is always 0.
            // {1,number,integer} is the place holder for the highest chapter number in the book. The format is special in that it will present it in the user's preferred format.
            // {2} is a placeholder for the Bible book name.
            // {3,number,integer} is a placeholder for the chapter number that the user gave.
            throw NoSuchVerseException(JSMsg.gettext("Chapter should be between {0} and {1,number,integer} for {2} (given {3,number,integer}).",
                    0, maxChapter, getPreferredName(book), chapter
            ))
        }

        // Check the verse
        val maxVerse = getLastVerse(book, chapter)
        if (verse < 0 || verse > maxVerse) {
            if (silent) {
                return false
            }
            // TRANSLATOR: The user supplied a verse number that was out of bounds. This tells them what is allowed.
            // {0} is the lowest value that is allowed. This is always 0.
            // {1,number,integer} is the place holder for the highest verse number in the chapter. The format is special in that it will present it in the user's preferred format.
            // {2} is a placeholder for the Bible book name.
            // {3,number,integer} is a placeholder for the chapter number that the user gave.
            // {4,number,integer} is a placeholder for the verse number that the user gave.
            throw NoSuchVerseException(JSMsg.gettext("Verse should be between {0} and {1,number,integer} for {2} {3,number,integer} (given {4,number,integer}).",
                    0, maxVerse, getPreferredName(book), chapter, verse
            ))
        }
        return true
    }

    /**
     * Fix up these verses so that they are as valid a possible. This is
     * currently done so that we can say "Gen 1:1" + 31 = "Gen 1:32" and
     * "Gen 1:32".patch() is "Gen 2:1".
     * <p>
     * There is another patch system that allows us to use large numbers to mean
     * "the end of" so "Gen 1:32".otherPatch() gives "Gen 1:31". This could be
     * useful to allow the user to enter things like "Gen 1:99" meaning the end
     * of the chapter. Or "Isa 99:1" to mean the last chapter in Isaiah verse 1
     * or even "Rev 99:99" to mean the last verse in the Bible.
     * <p>
     * However I have not implemented this because I've used a different
     * convention: "Gen 1:$" (OLB compatible) or "Gen 1:ff" (common commentary
     * usage) to mean the end of the chapter - So the functionality is there
     * anyway.
     * <p>
     * I think that getting into the habit of typing "Gen 1:99" is bad. It could
     * be the source of surprises "Psa 119:99" is not what you'd might expect,
     * and neither is "Psa 99:1" is you wanted the last chapter in Psalms -
     * expecting us to type "Psa 999:1" seems like we're getting silly.
     * <p>
     * However despite this maybe we should provide the functionality anyway.
     *
     * @param book the book to obtain
     * @param chapter the supposed chapter
     * @param verse the supposed verse
     * @return The resultant verse.
     */
    open fun patch(book: BibleBook?, chapter: Int, verse: Int): Verse {
        var patchedBook = book
        var patchedChapter = chapter
        var patchedVerse = verse

        // If the book is null,
        // then patch to the first book in the reference system
        if (patchedBook == null) {
            patchedBook = bookList.firstBook
        }
        // If they are too small
        if (patchedChapter < 0) {
            patchedChapter = 0
        }
        if (patchedVerse < 0) {
            patchedVerse = 0
        }

        // Goal is to start in the current book and go forward that number of chapters
        // which might cause one to land in a later book.
        // For each book, the chapters number from 0 to n, where n is the last chapter number.
        // So if we want Genesis 53, then that would be 3 chapters into Exodus,
        // which would be chapter 2.
        while (patchedBook != null && patchedChapter > getLastChapter(patchedBook)) {
            patchedChapter -= getLastChapter(patchedBook) + 1
            patchedBook = bookList.getNextBook(patchedBook)
        }

        // At this point we have a valid chapter.
        // Now we do the same for the verses.
        // For each book, the chapters number from 0 to n, where n is the last chapter number.
        // So if we want Genesis 49:36, then that would be 3 verses into Genesis 50,
        // which would be verse 50:2.     
        while (patchedBook != null && patchedVerse > getLastVerse(patchedBook, patchedChapter)) {
            patchedVerse -= getLastVerse(patchedBook, patchedChapter) + 1
            patchedChapter += 1

            if (patchedChapter > getLastChapter(patchedBook)) {
                patchedChapter -= getLastChapter(patchedBook) + 1
                patchedBook = bookList.getNextBook(patchedBook)
            }
        }

        // If we have gone beyond the last book
        // then return the last chapter and verse in the last book
        if (patchedBook == null) {
            patchedBook = bookList.lastBook
            patchedChapter = getLastChapter(patchedBook)
            patchedVerse = getLastVerse(patchedBook, patchedChapter)
        }

        return Verse(this, patchedBook, patchedChapter, patchedVerse)
    }

    companion object {
        @JvmStatic
        fun dump(out: PrintStream?, name: String?, bookList: BibleBookList?, array: Array<IntArray>?) {
            var vstr1: String
            var vstr2: String
            var count: Int
            out!!.println("    private final int[][] " + name + " =")
            out.println("    {")
            // Output an array just like lastVerse, indexed by book and chapter,
            // that accumulates verse counts for offsets,
            // having a sentinel at the end.
            val bookCount = array!!.size
            for (bookIndex in 0 until bookCount) {
                count = 0
                out.print("        // ")
                if (bookIndex < bookList!!.bookCount) {
                    val book = bookList.getBook(bookIndex)
                    out.println(book.osis)
                } else {
                    out.println("Sentinel")
                }
                out.print("        { ")

                val numChapters = array[bookIndex].size
                for (chapterIndex in 0 until numChapters) {

                    // Pretty print with 10 items per line
                    if (count++ % 10 == 0) {
                        out.println()
                        out.print("            ")
                    }

                    // Output the offset for the chapter introduction
                    // This is referenced with a verse number of 0
                    vstr1 = "     " + array[bookIndex][chapterIndex]
                    vstr2 = vstr1.substring(vstr1.length - 5)
                    out.print("$vstr2, ")
                }
                out.println()
                out.println("        },")
            }
            out.println("    };")
        }

        @JvmStatic
        fun optimize(out: PrintStream?, bookList: BibleBookList?, lastVerse: Array<IntArray>?) {
            var vstr1: String
            var vstr2: String
            var count: Int
            var ordinal = 0
            out!!.println("    private final int[][] chapterStarts =")
            out.println("    {")
            // Output an array just like lastVerse, indexed by book and chapter,
            // that accumulates verse counts for offsets,
            // having a sentinel at the end.
            var bookIndex = 0
            var ntStartOrdinal = 0
            var book: BibleBook? = bookList!!.getBook(0)
            while (book != null) {
                count = 0
                out.print("        // ")
                out.println(book.osis)
                out.print("        { ")

                // Remember where the NT Starts
                if (book == BibleBook.INTRO_NT) {
                    ntStartOrdinal = ordinal
                }

                val numChapters = lastVerse!![bookIndex].size
                for (chapterIndex in 0 until numChapters) {

                    // Pretty print with 10 items per line
                    if (count++ % 10 == 0) {
                        out.println()
                        out.print("            ")
                    }

                    // Output the offset for the chapter introduction
                    // This is referenced with a verse number of 0
                    vstr1 = "     $ordinal"
                    vstr2 = vstr1.substring(vstr1.length - 5)
                    out.print("$vstr2, ")
                    // Set ordinal to the start of the next chapter or book introduction
                    val versesInChapter = lastVerse[bookIndex][chapterIndex] + 1
                    ordinal += versesInChapter
                }
                out.println()
                out.println("        },")
                bookIndex++
                book = bookList.getNextBook(book)
            }

            // Output a sentinel value:
            // It is a book of one chapter starting with what would be the ordinal of the next chapter's introduction.
            vstr1 = "     $ordinal"
            vstr2 = vstr1.substring(vstr1.length - 5)
            out.println("        // Sentinel")
            out.println("        { ")
            out.println("            $vstr2, ")
            out.println("        },")
            out.println("    };")
            out.println()
            out.println("    /** The last ordinal number of the Old Testament */")
            out.println("    private int otMaxOrdinal = " + (ntStartOrdinal - 1) + ";")
            out.println("    /** The last ordinal number of the New Testament and the maximum ordinal number of this Reference System */")
            out.println("    private int ntMaxOrdinal = " + (ordinal - 1) + ";")
        }
    }
}
