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
package org.crosswire.jsword.index.search

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import org.crosswire.jsword.index.query.QueryDecorator
import org.crosswire.jsword.index.query.QueryDecoratorFactory

/**
 * An Enumeration of the possible types of Searches.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class SearchType
/**
 * @param name
 *            The name of the BookCategory
 */
(
    /**
     * The name of the BookCategory
     */
    private val displayName: String
) {
    /**
     * Find the words in the specified order.
     */
    PHRASE("Phrase") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decoratePhrase(queryWords)
        }
    },

    /**
     * Find all the words regardless of position.
     */
    ALL_WORDS("All") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateAllWords(queryWords)
        }
    },

    /**
     * Find any of these words
     */
    ANY_WORDS("Any") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateAnyWords(queryWords)
        }
    },

    /**
     * Find verses not containing these words. Note this may require being added
     * after words being sought.
     */
    NOT_WORDS("Not") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateNotWords(queryWords)
        }
    },

    /**
     * Find verses with words that start with the these beginnings.
     */
    START_WORDS("Start") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateStartWords(queryWords)
        }
    },

    /**
     * Find verses with words spelled something like
     */
    SPELL_WORDS("Spell") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateSpellWords(queryWords)
        }
    },

    /**
     * Find verses in this range
     */
    RANGE("Range") {
        override fun decorate(queryWords: String?): String {
            return SEARCH_SYNTAX.decorateRange(queryWords)
        }
    };

    /**
     * Decorate a string with the given type of decoration.
     * @param queryWords 
     * @return the decorated query
     */
    abstract fun decorate(queryWords: String?): String

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    override fun toString(): String {
        return displayName
    }

    companion object {
        /**
         * Lookup method to convert from a String
         * 
         * @param name 
         * @return the matching search type
         */
        @JvmStatic
        fun fromString(name: String?): SearchType {
            for (v in entries) {
                if (v.displayName.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            throw ClassCastException("Not a valid search type")
        }

        @JvmField
        protected val SEARCH_SYNTAX: QueryDecorator = QueryDecoratorFactory.getSearchSyntax()
    }
}
