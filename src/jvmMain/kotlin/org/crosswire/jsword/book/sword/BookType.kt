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

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookException
import org.crosswire.jsword.book.KeyType
import org.crosswire.jsword.book.sword.state.RawBackendState
import org.crosswire.jsword.book.sword.state.RawLDBackendState

import java.util.ArrayList
import java.util.Arrays

/**
 * Data about book types.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
abstract class BookType
/**
 * Simple ctor
 */
(
    /**
     * The name of the BookType
     */
    private val name: String,
    /**
     * The category of this book
     */
    open val bookCategory: BookCategory,
    /**
     * Get the way this type of Book organizes it's keys.
     */
    open val keyType: KeyType
) {
    /**
     * Given a SwordBookMetaData determine whether this BookType will work for
     * it.
     *
     * @param sbmd
     *            the BookMetaData that this BookType works upon
     * @return true if this is a usable BookType
     */
    @Suppress("SENSELESS_COMPARISON")
    open fun isSupported(sbmd: SwordBookMetaData?): Boolean {
        return bookCategory != null && sbmd != null
    }

    /**
     * Create a Book appropriate for the BookMetaData
     *
     * @param sbmd the book metadata
     * @return a book for that metadata
     * @throws BookException
     */
    @Throws(BookException::class)
    open fun createBook(sbmd: SwordBookMetaData): Book {
        return getBook(sbmd, getBackend(sbmd))
    }

    /**
     * Create a Book with the given backend
     */
    protected open fun getBook(sbmd: SwordBookMetaData, backend: Backend<*>): Book =
        createSwordBook(this, sbmd, backend)

    /**
     * Create a the appropriate backend for this type of book
     */
    @Throws(BookException::class)
    protected abstract fun getBackend(sbmd: SwordBookMetaData): Backend<*>

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    override fun toString(): String {
        return name
    }

    companion object {
        /**
         * Uncompressed Bibles
         */
        private val rawText: BookType = object : BookType("RawText", BookCategory.BIBLE, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawBackend<RawBackendState>(sbmd, 2)
            }
        }

        /**
         * Compressed Bibles
         */
        private val zText: BookType = object : BookType("zText", BookCategory.BIBLE, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                val blockType = BlockType.fromString(sbmd.getProperty(SwordBookMetaData.KEY_BLOCK_TYPE))
                return ZVerseBackend(sbmd, blockType, 2)
            }
        }

        /**
         * Compressed Bibles
         */
        private val zText4: BookType = object : BookType("zText4", BookCategory.BIBLE, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                val blockType = BlockType.fromString(sbmd.getProperty(SwordBookMetaData.KEY_BLOCK_TYPE))
                return ZVerseBackend(sbmd, blockType, 4)
            }
        }

        /**
         * Uncompressed Commentaries
         */
        private val rawCom: BookType = object : BookType("RawCom", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawBackend<RawBackendState>(sbmd, 2)
            }
        }

        private val rawCom4: BookType = object : BookType("RawCom4", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawBackend<RawBackendState>(sbmd, 4)
            }
        }

        /**
         * Compressed Commentaries
         */
        private val zCom: BookType = object : BookType("zCom", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                val blockType = BlockType.fromString(sbmd.getProperty(SwordBookMetaData.KEY_BLOCK_TYPE))
                return ZVerseBackend(sbmd, blockType, 2)
            }
        }

        /**
         * Compressed Commentaries
         */
        private val zCom4: BookType = object : BookType("zCom4", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                val blockType = BlockType.fromString(sbmd.getProperty(SwordBookMetaData.KEY_BLOCK_TYPE))
                return ZVerseBackend(sbmd, blockType, 4)
            }
        }

        /**
         * Uncompresses HREF Commentaries
         */
        private val hrefCom: BookType = object : BookType("HREFCom", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawBackend<RawBackendState>(sbmd, 2)
            }
        }

        /**
         * Uncompressed Commentaries
         */
        private val rawFiles: BookType = object : BookType("RawFiles", BookCategory.COMMENTARY, KeyType.VERSE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawFileBackend(sbmd, 2)
            }
        }

        /**
         * 2-Byte Index Uncompressed Dictionaries
         */
        private val rawLd: BookType = object : BookType("RawLD", BookCategory.DICTIONARY, KeyType.LIST) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawLDBackend<RawLDBackendState>(sbmd, 2)
            }
        }

        /**
         * 4-Byte Index Uncompressed Dictionaries
         */
        private val rawLd4: BookType = object : BookType("RawLD4", BookCategory.DICTIONARY, KeyType.LIST) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return RawLDBackend<RawLDBackendState>(sbmd, 4)
            }
        }

        /**
         * Compressed Dictionaries
         */
        private val zLd: BookType = object : BookType("zLD", BookCategory.DICTIONARY, KeyType.LIST) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return ZLDBackend(sbmd)
            }
        }

        /**
         * Generic Books
         */
        private val rawGenBook: BookType = object : BookType("RawGenBook", BookCategory.GENERAL_BOOK, KeyType.TREE) {
            @Throws(BookException::class)
            override fun getBackend(sbmd: SwordBookMetaData): Backend<*> {
                return GenBookBackend(sbmd)
            }
        }

        private val supportedBookTypes = ArrayList<BookType>(Arrays.asList(
            rawText, zText, zText4, rawCom, rawCom4, zCom, zCom4, hrefCom, rawFiles, rawLd,
            rawLd4, zLd, rawGenBook)
        )

        @JvmStatic
        fun addSupportedBookType(b: BookType) {
            supportedBookTypes.add(b)
        }

        /**
         * Lookup method to convert from a String
         *
         * @param name the string representation of a book type
         * @return the matching book type
         */
        @JvmStatic
        fun fromString(name: String?): BookType? {
            for (v in supportedBookTypes) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }
            return null
        }
    }
}
