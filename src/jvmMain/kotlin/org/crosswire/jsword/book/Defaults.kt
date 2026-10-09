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

import org.crosswire.common.config.ChoiceFactory
import org.crosswire.common.util.jswordAssert
import java.util.TreeMap

/**
 * Handles the current default Books.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class Defaults
/**
 * Prevent construction.
 */
private constructor() {
    companion object {
        /**
         * Determine whether the getBible should return the current Bible or the
         * user's chosen default.
         *
         * @return true if the bible tracks the user's selection
         */
        @JvmStatic
        fun isCurrentBible(): Boolean {
            return trackBible
        }

        /**
         * Establish whether the getBible should return the current Bible or the
         * user's chosen default.
         *
         * @param current whether getBible tracks the current Bible
         */
        @JvmStatic
        fun setCurrentBible(current: Boolean) {
            trackBible = current
        }

        /**
         * If the user has chosen to remember the book (by type) then set the
         * current book for that type.
         *
         * @param book the current Book
         */
        @JvmStatic
        fun setCurrentBook(book: Book) {
            val type = book.bookCategory
            if (type == BookCategory.BIBLE && isCurrentBible()) {
                currentBible = book
            }
        }

        /**
         * Get the current Bible, if set, else the default Bible.
         *
         * @return the current Bible
         */
        @JvmStatic
        fun getCurrentBible(): Book? {
            if (currentBible == null) {
                return bibleDeft.default
            }
            return currentBible
        }

        /**
         * Set the default Bible.
         *
         * @param book the default Bible
         */
        @JvmStatic
        fun setBible(book: Book?) {
            bibleDeft.setDefault(book)
        }

        /**
         * Unset the default Bible.
         */
        @JvmStatic
        protected fun unsetBible() {
            bibleDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default Bible
         */
        @JvmStatic
        fun getBible(): Book? {
            return bibleDeft.default
        }

        /**
         * @return the name of the default Bible
         */
        @JvmStatic
        fun getBibleByName(): String? {
            return bibleDeft.defaultName
        }

        /**
         * Set the default Bible by name.
         *
         * @param name the name of the default Bible
         */
        @JvmStatic
        fun setBibleByName(name: String?) {
            bibleDeft.setDefaultByName(name)
        }

        /**
         * Set the default commentary.
         *
         * @param book the default commentary
         */
        @JvmStatic
        fun setCommentary(book: Book?) {
            commentaryDeft.setDefault(book)
        }

        /**
         * Unset the default commentary.
         */
        @JvmStatic
        protected fun unsetCommentary() {
            commentaryDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default commentary
         */
        @JvmStatic
        fun getCommentary(): Book? {
            return commentaryDeft.default
        }

        /**
         * @return the name of the default commentary
         */
        @JvmStatic
        fun getCommentaryByName(): String? {
            return commentaryDeft.defaultName
        }

        /**
         * Set the default commentary by name.
         *
         * @param name the default commentary's name
         */
        @JvmStatic
        fun setCommentaryByName(name: String?) {
            commentaryDeft.setDefaultByName(name)
        }

        /**
         * Set the default dictionary.
         *
         * @param book the default dictionary
         */
        @JvmStatic
        fun setDictionary(book: Book?) {
            dictionaryDeft.setDefault(book)
        }

        /**
         * Unset the default dictionary.
         */
        @JvmStatic
        protected fun unsetDictionary() {
            dictionaryDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default dictionary
         */
        @JvmStatic
        fun getDictionary(): Book? {
            return dictionaryDeft.default
        }

        /**
         * @return the name of the default dictionary
         */
        @JvmStatic
        fun getDictionaryByName(): String? {
            return dictionaryDeft.defaultName
        }

        /**
         * Set the default dictionary by name.
         *
         * @param name the name of the default dictionary
         */
        @JvmStatic
        fun setDictionaryByName(name: String?) {
            dictionaryDeft.setDefaultByName(name)
        }

        /**
         * Set the default daily devotional.
         *
         * @param book the default daily devotional
         */
        @JvmStatic
        fun setDailyDevotional(book: Book?) {
            dictionaryDeft.setDefault(book)
        }

        /**
         * Unset the default daily devotional.
         */
        @JvmStatic
        protected fun unsetDailyDevotional() {
            dailyDevotionalDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default daily devotional
         */
        @JvmStatic
        fun getDailyDevotional(): Book? {
            return dailyDevotionalDeft.default
        }

        /**
         * @return the name of the default daily devotional
         */
        @JvmStatic
        fun getDailyDevotionalByName(): String? {
            return dailyDevotionalDeft.defaultName
        }

        /**
         * Set the default daily devotional by name.
         *
         * @param name the name of the default daily devotional
         */
        @JvmStatic
        fun setDailyDevotionalByName(name: String?) {
            dailyDevotionalDeft.setDefaultByName(name)
        }

        /**
         * Set the default Greek Strong's Numbers dictionary.
         *
         * @param book the default Greek Strong's Numbers dictionary.
         */
        @JvmStatic
        fun setGreekDefinitions(book: Book?) {
            greekDefinitionsDeft.setDefault(book)
        }

        /**
         * Unset the default Greek Strong's Numbers dictionary.
         */
        @JvmStatic
        protected fun unsetGreekDefinitions() {
            greekDefinitionsDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default Greek Strong's Numbers dictionary
         */
        @JvmStatic
        fun getGreekDefinitions(): Book? {
            return greekDefinitionsDeft.default
        }

        /**
         * @return the name of the default Greek Strong's Numbers dictionary
         */
        @JvmStatic
        fun getGreekDefinitionsByName(): String? {
            return greekDefinitionsDeft.defaultName
        }

        /**
         * Set the default Greek Strong's Numbers dictionary by name.
         *
         * @param name the name of the default Greek Strong's Numbers dictionary
         */
        @JvmStatic
        fun setGreekDefinitionsByName(name: String?) {
            greekDefinitionsDeft.setDefaultByName(name)
        }

        /**
         * Set the default Hebrew Strong's Numbers dictionary.
         *
         * @param book the default Hebrew Strong's Numbers dictionary.
         */
        @JvmStatic
        fun setHebrewDefinitions(book: Book?) {
            hebrewDefinitionsDeft.setDefault(book)
        }

        /**
         * Unset the default Hebrew Strong's Numbers dictionary.
         */
        @JvmStatic
        protected fun unsetHebrewDefinitions() {
            hebrewDefinitionsDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default Hebrew Strong's Numbers dictionary
         */
        @JvmStatic
        fun getHebrewDefinitions(): Book? {
            return hebrewDefinitionsDeft.default
        }

        /**
         * @return the name of the default Hebrew Strong's Numbers dictionary
         */
        @JvmStatic
        fun getHebrewDefinitionsByName(): String? {
            return hebrewDefinitionsDeft.defaultName
        }

        /**
         * Set the default Hebrew Strong's Numbers dictionary by name.
         *
         * @param name the name of the default Hebrew Strong's Numbers dictionary
         */
        @JvmStatic
        fun setHebrewDefinitionsByName(name: String?) {
            hebrewDefinitionsDeft.setDefaultByName(name)
        }

        /**
         * Set the default Greek morphology dictionary.
         *
         * @param book the default Greek morphology dictionary.
         */
        @JvmStatic
        fun setGreekParse(book: Book?) {
            greekParseDeft.setDefault(book)
        }

        /**
         * Unset the default Greek morphology dictionary.
         */
        @JvmStatic
        protected fun unsetGreekParse() {
            greekParseDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default Greek morphology dictionary
         */
        @JvmStatic
        fun getGreekParse(): Book? {
            return greekParseDeft.default
        }

        /**
         * Set the default Greek morphology dictionary by name.
         *
         * @return the name of the default Greek morphology dictionary
         */
        @JvmStatic
        fun getGreekParseByName(): String? {
            return greekParseDeft.defaultName
        }

        /**
         * @param name the name of the default Greek morphology dictionary
         */
        @JvmStatic
        fun setGreekParseByName(name: String?) {
            greekParseDeft.setDefaultByName(name)
        }

        /**
         * Set the default Hebrew morphology dictionary by name.
         *
         * @param book the default Hebrew morphology dictionary by name.
         */
        @JvmStatic
        fun setHebrewParse(book: Book?) {
            hebrewParseDeft.setDefault(book)
        }

        /**
         * Unset the default Hebrew morphology dictionary.
         */
        @JvmStatic
        protected fun unsetHebrewParse() {
            hebrewParseDeft.unsetDefaultForPackage()
        }

        /**
         * @return the default Hebrew morphology dictionary
         */
        @JvmStatic
        fun getHebrewParse(): Book? {
            return hebrewParseDeft.default
        }

        /**
         * @return the name of the default Hebrew morphology dictionary
         */
        @JvmStatic
        fun getHebrewParseByName(): String? {
            return hebrewParseDeft.defaultName
        }

        /**
         * Set the default Hebrew morphology dictionary by name.
         *
         * @param name the default Hebrew morphology dictionary by name.
         */
        @JvmStatic
        fun setHebrewParseByName(name: String?) {
            hebrewParseDeft.setDefaultByName(name)
        }

        /**
         * @return the default Bible
         */
        @JvmStatic
        protected fun getDefaultBible(): DefaultBook {
            return bibleDeft
        }

        /**
         * @return the default commentary
         */
        @JvmStatic
        protected fun getDefaultCommentary(): DefaultBook {
            return commentaryDeft
        }

        /**
         * @return the default dictionary
         */
        @JvmStatic
        protected fun getDefaultDictionary(): DefaultBook {
            return dictionaryDeft
        }

        /**
         * @return the default daily devotional
         */
        @JvmStatic
        protected fun getDefaultDailyDevotional(): DefaultBook {
            return dailyDevotionalDeft
        }

        /**
         * @return the default Greek Strong's Numbers dictionary
         */
        @JvmStatic
        protected fun getDefaultGreekDefinitions(): DefaultBook {
            return greekDefinitionsDeft
        }

        /**
         * @return the default Hebrew Strong's Numbers dictionary
         */
        @JvmStatic
        protected fun getDefaultHebrewDefinitions(): DefaultBook {
            return hebrewDefinitionsDeft
        }

        /**
         * @return the default Greek morphology dictionary
         */
        @JvmStatic
        protected fun getDefaultGreekParse(): DefaultBook {
            return greekParseDeft
        }

        /**
         * @return the default Hebrew morphology dictionary
         */
        @JvmStatic
        protected fun getDefaultHebrewParse(): DefaultBook {
            return hebrewParseDeft
        }


        /**
         * Create book lists for every type of book.
         */
        @JvmStatic
        fun refreshBooks() {
            // Create the array of Bibles
            val bnames = getBookMap(BookFilters.getOnlyBibles())
            ChoiceFactory.getDataMap().put(BIBLE_KEY, bnames)

            // Create the array of Commentaries
            val cnames = getBookMap(BookFilters.getCommentaries())
            ChoiceFactory.getDataMap().put(COMMENTARY_KEY, cnames)

            // Create the array of Dictionaries
            val dnames = getBookMap(BookFilters.getDictionaries())
            ChoiceFactory.getDataMap().put(DICTIONARY_KEY, dnames)

            // Create the array of DailyDevotionals
            val rnames = getBookMap(BookFilters.getDailyDevotionals())
            ChoiceFactory.getDataMap().put(DAILY_DEVOTIONALS_KEY, rnames)

            // Create the array of Dictionaries
            val greekDef = getBookMap(BookFilters.getGreekDefinitions())
            ChoiceFactory.getDataMap().put(GREEKDEF_KEY, greekDef)

            // Create the array of Dictionaries
            val hebrewDef = getBookMap(BookFilters.getHebrewDefinitions())
            ChoiceFactory.getDataMap().put(HEBREWDEF_KEY, hebrewDef)

            // Create the array of Dictionaries
            val greekParse = getBookMap(BookFilters.getGreekParse())
            ChoiceFactory.getDataMap().put(GREEKPARSE_KEY, greekParse)

            // Create the array of Dictionaries
            val hebrewParse = getBookMap(BookFilters.getHebrewParse())
            ChoiceFactory.getDataMap().put(HEBREWPARSE_KEY, hebrewParse)
        }

        /**
         * Go through all of the current books checking to see if we need to replace
         * the current defaults with one of these.
         */
        @JvmStatic
        protected fun checkAllPreferable() {
            for (book in Books.installed().getBooks()) {
                checkPreferable(book)
            }
        }

        /**
         * Determine whether this Book become the default. It should, only if there
         * is not one.
         *
         * @param book the book to check
         */
        @JvmStatic
        protected fun checkPreferable(book: Book?) {
            jswordAssert(book != null) { "" }

            bibleDeft.setDefaultConditionally(book)
            commentaryDeft.setDefaultConditionally(book)
            dictionaryDeft.setDefaultConditionally(book)
            dailyDevotionalDeft.setDefaultConditionally(book)
            greekDefinitionsDeft.setDefaultConditionally(book)
            greekParseDeft.setDefaultConditionally(book)
            hebrewDefinitionsDeft.setDefaultConditionally(book)
            hebrewParseDeft.setDefaultConditionally(book)
        }

        /**
         * Convert a filter into an array of names of Books that pass the filter.
         *
         * @param filter the filter to locate matching books
         * @return matching books
         */
        private fun getBookMap(filter: BookFilter): MutableMap<Book, String> {
            val books: MutableMap<Book, String> = TreeMap(BookComparators.getDefault())

            for (book in Books.installed().getBooks(filter)) {
                books.put(book, book.name)
            }

            return books
        }

        private const val BIBLE_KEY = "bible-names"
        private const val COMMENTARY_KEY = "commentary-names"
        private const val DICTIONARY_KEY = "dictionary-names"
        private const val DAILY_DEVOTIONALS_KEY = "daily-devotional-names"
        private const val GREEKDEF_KEY = "greekdef-names"
        private const val HEBREWDEF_KEY = "hebrewdef-names"
        private const val GREEKPARSE_KEY = "greekparse-names"
        private const val HEBREWPARSE_KEY = "hebrewparse-names"

        /**
         * Indicates whether the last book of each type is used next time.
         */
        private var trackBible = true

        /**
         * The current bible being tracked.
         */
        private var currentBible: Book? = null

        /**
         * The default Bible
         */
        private val bibleDeft = DefaultBook(Books.installed(), BookFilters.getOnlyBibles())

        /**
         * The default Commentary
         */
        private val commentaryDeft = DefaultBook(Books.installed(), BookFilters.getCommentaries())

        /**
         * The default DailyDevotional
         */
        private val dailyDevotionalDeft = DefaultBook(Books.installed(), BookFilters.getDailyDevotionals())

        /**
         * The default Dictionary
         */
        private val dictionaryDeft = DefaultBook(Books.installed(), BookFilters.getDictionaries())

        /**
         * The default Greek Parse Dictionary.
         */
        private val greekParseDeft = DefaultBook(Books.installed(), BookFilters.getGreekParse())

        /**
         * The default Hebrew Parse Dictionary.
         */
        private val hebrewParseDeft = DefaultBook(Books.installed(), BookFilters.getHebrewParse())

        /**
         * The default Greek Definitions Dictionary.
         */
        private val greekDefinitionsDeft = DefaultBook(Books.installed(), BookFilters.getGreekDefinitions())

        /**
         * The default Hebrew Definitions Dictionary.
         */
        private val hebrewDefinitionsDeft = DefaultBook(Books.installed(), BookFilters.getHebrewDefinitions())

        /**
         * Register with Books so we know how to provide valid defaults
         */
        init {
            Books.installed().addBooksListener(DefaultsBookListener())
            checkAllPreferable()
        }
    }

    /**
     * To keep us up to date with changes in the available Books
     */
    private class DefaultsBookListener : BooksListener {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BooksListener#bookAdded(org.crosswire.jsword.book.BooksEvent)
         */
        override fun bookAdded(ev: BooksEvent) {
            val book = ev.book
            checkPreferable(book)
            refreshBooks()
        }

        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BooksListener#bookRemoved(org.crosswire.jsword.book.BooksEvent)
         */
        override fun bookRemoved(ev: BooksEvent) {
            val book = ev.book

            getDefaultBible().unsetDefaultConditionallyForPackage(book)
            getDefaultCommentary().unsetDefaultConditionallyForPackage(book)
            getDefaultDailyDevotional().unsetDefaultConditionallyForPackage(book)
            getDefaultDictionary().unsetDefaultConditionallyForPackage(book)
            getDefaultGreekDefinitions().unsetDefaultConditionallyForPackage(book)
            getDefaultGreekParse().unsetDefaultConditionallyForPackage(book)
            getDefaultHebrewDefinitions().unsetDefaultConditionallyForPackage(book)
            getDefaultHebrewParse().unsetDefaultConditionallyForPackage(book)
        }
    }
}
