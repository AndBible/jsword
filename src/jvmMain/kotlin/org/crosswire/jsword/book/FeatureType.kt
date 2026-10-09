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

import org.crosswire.common.util.jswordAssert

/**
 * An Enumeration of the possible Features a Book may have.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
enum class FeatureType
/**
 * @param name
 *            The name of the FeatureType
 * @param alias
 *            The alias of the FeatureType
 */
(
    /**
     * The name of the FeatureType
     */
    private val featureName: String,
    /**
     * The alias of the FeatureType
     */
    val alias: String = ""
) {
    /**
     * The book is one of Greek Definitions. AKA, Strong's.
     */
    GREEK_DEFINITIONS("GreekDef"),

    /**
     * The book is one of Greek word parsings. AKA, Robinson.
     */
    GREEK_PARSE("GreekParse"),

    /**
     * The book is one of Hebrew Definitions. AKA, Strong's.
     */
    HEBREW_DEFINITIONS("HebrewDef"),

    /**
     * The book is one of Hebrew word parsings. AKA, ???.
     */
    HEBREW_PARSE("HebrewParse"),

    /**
     * The book is one of Daily Devotions.
     */
    DAILY_DEVOTIONS("DailyDevotions"),

    /**
     * The book is glossary of translations from one language to another.
     */
    GLOSSARY("Glossary"),

    /**
     * The book contains Strong's Numbers.
     * The alias is used to match GlobalOptionFilters.
     */
    STRONGS_NUMBERS("StrongsNumbers", "Strongs"),

    /**
     * The book contains footnotes
     */
    FOOTNOTES("Footnotes"),

    /**
     * The book contains Scripture cross references
     */
    SCRIPTURE_REFERENCES("Scripref"),

    /**
     * The book marks the Word's of Christ
     */
    WORDS_OF_CHRIST("RedLetterWords"),

    /**
     * The book contains Morphology info
     */
    MORPHOLOGY("Morph"),

    /**
     * The book contains Headings
     */
    HEADINGS("Headings"),

    /**
     * The book contains Headings
     */
    GLOSSES("Glosses", "Ruby");

    /* (non-Javadoc)
     * @see java.lang.Enum#toString()
     */
    override fun toString(): String {
        return featureName
    }

    companion object {
        /**
         * Lookup method to convert from a String
         *
         * @param name the name of a FeatureType
         * @return the matching FeatureType
         */
        @JvmStatic
        fun fromString(name: String?): FeatureType? {
            for (v in values()) {
                if (v.featureName.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }
    }
}
