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
 * The System package defines each of the known Versifications.
 * The only useful class is Versifications.
 *
 * Each Versification consists of 5 static fields:
 * 1. **V11N_NAME** - The name of the Versification
 * 2. **BOOKS_OT** - An ordered list of Old Testament Bible Books.
 * 3. **BOOKS_NT** - An ordered list of New Testament Bible Books.
 * 4. **LAST_VERSE_OT** - The highest verse number given by Old Testament book and chapter.
 * 5. **LAST_VERSE_NT** - The highest verse number given by New Testament book and chapter.
 */
package org.crosswire.jsword.versification.system

import org.crosswire.jsword.versification.Versification

/**
 * The Versifications class manages the creation of Versifications as needed.
 * It delays the construction of the Versification until getVersification(String name) is called.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class Versifications private constructor() {

    /**
     * The set of v11n names.
     */
    private val known: MutableSet<String> = HashSet<String>().apply {
        add(SystemCalvin.V11N_NAME)
        add(SystemCatholic.V11N_NAME)
        add(SystemCatholic2.V11N_NAME)
        add(SystemDarbyFR.V11N_NAME)
        add(SystemGerman.V11N_NAME)
        add(SystemKJV.V11N_NAME)
        add(SystemKJVA.V11N_NAME)
        add(SystemLeningrad.V11N_NAME)
        add(SystemLuther.V11N_NAME)
        add(SystemLXX.V11N_NAME)
        add(SystemMT.V11N_NAME)
        add(SystemNRSV.V11N_NAME)
        add(SystemNRSVA.V11N_NAME)
        add(SystemOrthodox.V11N_NAME)
        add(SystemSegond.V11N_NAME)
        add(SystemSynodal.V11N_NAME)
        add(SystemSynodalProt.V11N_NAME)
        add(SystemVulg.V11N_NAME)
    }

    /**
     * The map of instantiated Versifications, given by their names.
     */
    private val fluffed: MutableMap<String, Versification> = HashMap()

    /**
     * Get the default Versification.
     *
     * @return the default Versification.
     * @deprecated Use [getVersification(String)] instead.
     */
    @Deprecated("Use getVersification(String) instead.")
    @Synchronized
    fun getDefaultVersification(): Versification? {
        return getVersification(DEFAULT_V11N)
    }

    /**
     * Get the Versification by its name. If name is null then return the default Versification.
     *
     * @param name the name of the Versification
     * @return the Versification or null if it is not known.
     */
    @Synchronized
    fun getVersification(name: String?): Versification? {
        val actual: String = name ?: DEFAULT_V11N

        // This class delays the building of a Versification to when it is
        // actually needed.
        var rs = fluffed[actual]
        if (rs == null) {
            rs = fluff(actual)
            if (rs != null) {
                fluffed[actual] = rs
            }
        }

        return rs
    }

    /**
     * Determine whether the named Versification is known.
     *
     * @param name the name of the Versification
     * @return true when the Versification is available for use
     */
    @Synchronized
    fun isDefined(name: String?): Boolean {
        return name == null || known.contains(name)
    }

    private fun fluff(name: String?): Versification? {
        // Keep KJV at the top as it is the most common
        if (name == null || SystemKJV.V11N_NAME == name) {
            return SystemKJV()
        }

        // then in alphabetical order, to ease the developer checking we have them all
        if (SystemCalvin.V11N_NAME == name) {
            return SystemCalvin()
        }
        if (SystemCatholic.V11N_NAME == name) {
            return SystemCatholic()
        }
        if (SystemCatholic2.V11N_NAME == name) {
            return SystemCatholic2()
        }
        if (SystemDarbyFR.V11N_NAME == name) {
            return SystemDarbyFR()
        }
        if (SystemGerman.V11N_NAME == name) {
            return SystemGerman()
        }
        if (SystemKJVA.V11N_NAME == name) {
            return SystemKJVA()
        }
        if (SystemLeningrad.V11N_NAME == name) {
            return SystemLeningrad()
        }
        if (SystemLuther.V11N_NAME == name) {
            return SystemLuther()
        }
        if (SystemLXX.V11N_NAME == name) {
            return SystemLXX()
        }
        if (SystemMT.V11N_NAME == name) {
            return SystemMT()
        }
        if (SystemNRSV.V11N_NAME == name) {
            return SystemNRSV()
        }
        if (SystemNRSVA.V11N_NAME == name) {
            return SystemNRSVA()
        }
        if (SystemOrthodox.V11N_NAME == name) {
            return SystemOrthodox()
        }
        if (SystemSegond.V11N_NAME == name) {
            return SystemSegond()
        }
        if (SystemSynodal.V11N_NAME == name) {
            return SystemSynodal()
        }
        if (SystemSynodalProt.V11N_NAME == name) {
            return SystemSynodalProt()
        }
        if (SystemVulg.V11N_NAME == name) {
            return SystemVulg()
        }

        return null
    }

    /**
     * Add a Versification that is not predefined by JSword.
     *
     * @param rs the Versification to register
     */
    @Synchronized
    fun register(rs: Versification) {
        fluffed[rs.name] = rs
        known.add(rs.name)
    }

    /**
     * Get an iterator over all known versifications.
     *
     * @return an iterator of versification names.
     */
    fun iterator(): Iterator<String> {
        return known.iterator()
    }

    /**
     * @return number of versifications
     */
    fun size(): Int {
        return known.size
    }

    companion object {
        /**
         * The default Versification for JSword is the KJV.
         * This is subject to change at any time.
         */
        const val DEFAULT_V11N: String = SystemKJV.V11N_NAME

        /**
         * Get the singleton instance of Versifications.
         *
         * @return the singleton
         */
        @JvmStatic
        fun instance(): Versifications {
            return singleton
        }

        private val singleton = Versifications()
    }
}
