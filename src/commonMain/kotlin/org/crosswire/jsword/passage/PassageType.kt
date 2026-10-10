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
package org.crosswire.jsword.passage

import kotlin.jvm.JvmStatic

import org.crosswire.common.util.jswordAssert
import org.crosswire.jsword.versification.Versification

/**
 * Types of Passage optimizations.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class PassageType {
    /**
     * Optimize the Passage for speed
     */
    SPEED {
        @Throws(NoSuchVerseException::class)
        override fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage {
            if (passage == null || passage.length == 0) {
                return createEmptyPassage(v11n)
            }
            return RocketPassage(v11n, passage, basis)
        }

        override fun createEmptyPassage(v11n: Versification?): Passage {
            return RocketPassage(v11n)
        }
    },

    /**
     * Optimize the Passage for write speed
     */
    WRITE_SPEED {
        @Throws(NoSuchVerseException::class)
        override fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage {
            if (passage == null || passage.length == 0) {
                return createEmptyPassage(v11n)
            }
            return BitwisePassage(v11n, passage, basis)
        }

        override fun createEmptyPassage(v11n: Versification?): Passage {
            return BitwisePassage(v11n)
        }
    },

    /**
     * Optimize the Passage for size
     */
    SIZE {
        @Throws(NoSuchVerseException::class)
        override fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage {
            if (passage == null || passage.length == 0) {
                return createEmptyPassage(v11n)
            }
            return DistinctPassage(v11n, passage, basis)
        }

        override fun createEmptyPassage(v11n: Versification?): Passage {
            return DistinctPassage(v11n)
        }
    },

    /**
     * Optimize the Passage for a mix
     */
    MIX {
        @Throws(NoSuchVerseException::class)
        override fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage {
            if (passage == null || passage.length == 0) {
                return createEmptyPassage(v11n)
            }
            return RangedPassage(v11n, passage, basis)
        }

        override fun createEmptyPassage(v11n: Versification?): Passage {
            return RangedPassage(v11n)
        }
    },

    /**
     * Optimize the Passage for tally operations
     */
    TALLY {
        @Throws(NoSuchVerseException::class)
        override fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage {
            if (passage == null || passage.length == 0) {
                return createEmptyPassage(v11n)
            }
            return PassageTally(v11n, passage, basis)
        }

        override fun createEmptyPassage(v11n: Versification?): Passage {
            return PassageTally(v11n)
        }
    };

    /**
     * Create an optimized passage
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @param passage
     * @param basis
     * @return the optimized passage
     * @throws NoSuchVerseException
     */
    @Throws(NoSuchVerseException::class)
    abstract fun createPassage(v11n: Versification?, passage: String?, basis: Key?): Passage

    @Throws(NoSuchVerseException::class)
    fun createPassage(v11n: Versification?, passage: String?): Passage {
        return createPassage(v11n, passage, null)
    }

    /**
     * Create an empty, optimized passage
     *
     * @param v11n
     *            the versification to which this reference pertains
     * @return the optimized, empty passage
     */
    abstract fun createEmptyPassage(v11n: Versification?): Passage

    companion object {
        /**
         * Lookup method to convert from a String
         * @param name
         * @return the matching Passage type
         */
        @JvmStatic
        fun fromString(name: String?): PassageType? {
            for (v in entries) {
                if (v.name.equals(name, ignoreCase = true)) {
                    return v
                }
            }

            // cannot get here
            jswordAssert(false) { "" }
            return null
        }

        /**
         * Lookup method to convert from an integer
         *
         * @param i
         * @return the Passage type from its ordinal value
         */
        @JvmStatic
        fun fromInteger(i: Int): PassageType {
            for (v in entries) {
                if (v.ordinal == i) {
                    return v
                }
            }

            // on error return SPEED
            return SPEED
        }

        /**
         * Lookup method to convert from an integer
         * @param type
         * @return the ordinal value for the passage type
         */
        @JvmStatic
        fun toInteger(type: PassageType?): Int {
            return type!!.ordinal
        }
    }
}
