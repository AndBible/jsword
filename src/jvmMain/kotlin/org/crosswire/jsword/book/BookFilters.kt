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

import org.crosswire.common.util.splitLikeJava

/**
 * Some common implementations of BookFilter.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class BookFilters
/**
 * Ensure we can't be created
 */
private constructor() {
    /**
     * Filter for all books
     */
    private class AllBookFilter : BookFilter {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BookFilter#test(org.crosswire.jsword.book.Book)
         */
        override fun test(book: Book): Boolean {
            return true
        }
    }

    /**
     * Filter for books by category
     */
    private class BookCategoryFilter(private var category: BookCategory) : BookFilter {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BookFilter#test(org.crosswire.jsword.book.Book)
         */
        override fun test(book: Book): Boolean {
            return book.bookCategory == category && !book.isLocked
        }
    }

    /**
     * Filter for books by category
     */
    private class NotBookCategoryFilter(private var category: BookCategory) : BookFilter {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BookFilter#test(org.crosswire.jsword.book.Book)
         */
        override fun test(book: Book): Boolean {
            return book.bookCategory != category && !book.isLocked
        }
    }

    /**
     * Filter for books by feature
     */
    open class BookFeatureFilter(private var feature: FeatureType) : BookFilter {
        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BookFilter#test(org.crosswire.jsword.book.Book)
         */
        override fun test(book: Book): Boolean {
            return book.hasFeature(feature) && !book.isLocked
        }
    }

    /**
     * Custom Filter
     */
    private class CustomBookFilter
    /**
     * Ctor
     *
     * @param match
     *            The match spec.
     * @see BookFilters#getCustom(String)
     */
    (match: String) : BookFilter {
        private var tests: Array<Test>

        init {
            val cache: MutableList<Test> = ArrayList()
            val filters = match.splitLikeJava(Regex(";"))
            for (i in filters.indices) {
                cache.add(Test(filters[i]))
            }

            tests = cache.toTypedArray()
        }

        /* (non-Javadoc)
         * @see org.crosswire.jsword.book.BookFilter#test(org.crosswire.jsword.book.Book)
         */
        override fun test(book: Book): Boolean {
            for (i in tests.indices) {
                val test = tests[i]
                if ("initials".equals(test.property, ignoreCase = true)) {
                    if (test.result != book.initials) {
                        return false
                    }
                    continue
                }
                val property: Any = book.getProperty(test.property) ?: return false
                val result = property.toString()
                if (result.indexOf('\n') > -1) {
                    // There's more than one value.
                    if ((result.contains(test.result)) != test.expected) {
                        return false
                    }
                } else if ((test.result == result) != test.expected) {
                    return false
                }
            }

            return true
        }

        /**
         * A helper class
         */
        open class Test {
            constructor(filter: String) {
                val parts = filter.splitLikeJava(Regex("="))
                if (parts.size != 2 || parts[0].length == 0 || parts[1].length == 0) {
                    throw IllegalArgumentException("Filter format is 'property=value' or property!=value, given: " + filter)
                }
                property = parts[0]
                result = parts[1]
                val len = property.length
                this.expected = (property[len - 1] != '!')
                if (!expected) {
                    property = property.substring(0, len - 1)
                }
            }

            constructor(property: String, result: String, expected: Boolean) {
                this.property = property
                this.result = result
                this.expected = expected
            }

            constructor(property: String, result: String) : this(property, result, true)

            var property: String
            var result: String
            var expected: Boolean
        }
    }

    companion object {
        /**
         * A simple default filter that returns everything
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getAll(): BookFilter {
            return AllBookFilter()
        }

        /**
         * A filter that accepts everything that implements Bible or Commentary,
         * when commentaries are listed with Bibles.
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getBibles(): BookFilter {
            if (isCommentariesWithBibles) {
                return either(BookCategoryFilter(BookCategory.BIBLE), BookCategoryFilter(BookCategory.COMMENTARY))
            }
            return BookCategoryFilter(BookCategory.BIBLE)
        }

        /**
         * A filter that accepts everything that implements Bible.
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getOnlyBibles(): BookFilter {
            return BookCategoryFilter(BookCategory.BIBLE)
        }

        /**
         * A filter that accepts everything that's not a Bible or a Commentary, when
         * commentaries are listed with Bibles.
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getNonBibles(): BookFilter {
            if (isCommentariesWithBibles) {
                return both(NotBookCategoryFilter(BookCategory.BIBLE), NotBookCategoryFilter(BookCategory.COMMENTARY))
            }
            return NotBookCategoryFilter(BookCategory.BIBLE)
        }

        /**
         * A filter that accepts everything that implements Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getDictionaries(): BookFilter {
            return BookCategoryFilter(BookCategory.DICTIONARY)
        }

        /**
         * A filter that accepts everything that implements Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getGlossaries(): BookFilter {
            return BookCategoryFilter(BookCategory.GLOSSARY)
        }

        /**
         * A filter that accepts everything that implements DailyDevotionals
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getDailyDevotionals(): BookFilter {
            return BookCategoryFilter(BookCategory.DAILY_DEVOTIONS)
        }

        /**
         * A filter that accepts everything that implements Commentary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getCommentaries(): BookFilter {
            return BookCategoryFilter(BookCategory.COMMENTARY)
        }

        /**
         * A filter that accepts everything that implements GeneralBook
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getGeneralBooks(): BookFilter {
            return BookCategoryFilter(BookCategory.GENERAL_BOOK)
        }

        /**
         * A filter that accepts everything that implements Maps
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getMaps(): BookFilter {
            return BookCategoryFilter(BookCategory.MAPS)
        }

        /**
         * A filter that accepts everything that implements Other
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getOther(): BookFilter {
            return BookCategoryFilter(BookCategory.OTHER)
        }

        /**
         * A filter that accepts everything that is a Greek Definition Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getGreekDefinitions(): BookFilter {
            return BookFeatureFilter(FeatureType.GREEK_DEFINITIONS)
        }

        /**
         * A filter that accepts everything that is a Greek Parse/Morphology
         * Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getGreekParse(): BookFilter {
            return BookFeatureFilter(FeatureType.GREEK_PARSE)
        }

        /**
         * A filter that accepts everything that is a Hebrew Definition Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getHebrewDefinitions(): BookFilter {
            return BookFeatureFilter(FeatureType.HEBREW_DEFINITIONS)
        }

        /**
         * A filter that accepts everything that is a Hebrew Parse/Morphology
         * Dictionary
         *
         * @return the desired filter
         */
        @JvmStatic
        fun getHebrewParse(): BookFilter {
            return BookFeatureFilter(FeatureType.HEBREW_PARSE)
        }

        /**
         * Whether the Bible filters include commentaries (true if commentaries
         * should be returned together with Bibles). Initially false.
         */
        @JvmStatic
        var isCommentariesWithBibles: Boolean = false

        /**
         * A filter that accepts Books that match two criteria.
         *
         * @param b1 the first filter criteria
         * @param b2 the second filter criteria
         * @return the desired filter
         */
        @JvmStatic
        fun both(b1: BookFilter, b2: BookFilter): BookFilter {
            return object : BookFilter {
                override fun test(book: Book): Boolean {
                    return b1.test(book) && b2.test(book)
                }
            }
        }

        /**
         * A filter that accepts Books that match either of two criteria.
         *
         * @param b1 the first filter criteria
         * @param b2 the second filter criteria
         * @return the desired filter
         */
        @JvmStatic
        fun either(b1: BookFilter, b2: BookFilter): BookFilter {
            return object : BookFilter {
                override fun test(book: Book): Boolean {
                    return b1.test(book) || b2.test(book)
                }
            }
        }

        /**
         * A filter that accepts Books that match by book driver.
         *
         * @param driver the driver to match
         * @return the desired filter
         */
        @JvmStatic
        fun getBooksByDriver(driver: BookDriver?): BookFilter {
            return object : BookFilter {
                override fun test(book: Book): Boolean {
                    return book.driver === driver
                }
            }
        }

        /**
         * A simple default filter that returns everything. The match parameter is a
         * set of name value pairs like this: <br></br>
         * `initials=ESV;type=Bible;driverName=Sword`<br></br>
         * Before the = there must be the name of a property on Book and after the
         * value to match (.toString()) is called on the results of the getter.
         *
         * @param match
         *            a ; separated list of properties (of Book) to match
         * @return the desired filter
         * @see Book
         */
        @JvmStatic
        fun getCustom(match: String): BookFilter {
            return CustomBookFilter(match)
        }
    }
}
