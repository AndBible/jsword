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
 * © CrossWire Bible Society, 2013 - 2016
 *
 */
package org.crosswire.jsword.versification

import org.crosswire.jsword.passage.Verse
import org.crosswire.jsword.passage.VerseKey
import org.crosswire.jsword.passage.VerseRange
import org.crosswire.jsword.versification.system.Versifications

/**
 * A QualifiedKey represents the various left and right sides of a map entry.
 * <p>
 * The QualifiedKey is Qualified:
 * </p>
 * <ul>
 * <li><strong>DEFAULT</strong> - This QualifiedKey is either a Verse or a VerseRange.</li>
 * <li><strong>ABSENT_IN_KJV</strong> - This QualifiedKey has a section name for what is absent in the KJV (the right hand of the map entry).</li>
 * <li><strong>ABSENT_IN_LEFT</strong> - This QualifiedKey has no other content.</li>
 * </ul>
 * <p>
 * The mapping can indicate a part of a verse. This is an internal implementation detail of the Versification mapping code.
 * Here it is used to distinguish one QualifiedKey from another in equality tests and in containers.
 * </p>
 *
 * @author Chris Burrell
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.<br>
 * The copyright to this program is held by its authors.
 */
class QualifiedKey {
    /**
     * A Qualifier indicates whether the verse is numbered the same in both the KJV and the other, is missing in the KJV or the other.
     *
     * Java had this enum package-private; it is the return type of the public [absentType], so it is public here (was package-private).
     */
    enum class Qualifier {
        /**
         * The DEFAULT Qualifier indicates a Verse or a VerseRange.
         */
        DEFAULT {
            override fun getDescription(q: QualifiedKey?): String {
                return ""
            }
        },
        /**
         * The ABSENT_IN_LEFT Qualifier indicates that the left side of the map has no equivalent on the right (KJV).
         */
        ABSENT_IN_LEFT {
            override fun getDescription(q: QualifiedKey?): String {
                return "Absent in Left"
            }
        },
        /**
         * The ABSENT_IN_KJV Qualifier indicates that the right (KJV) side of the map has no equivalent on the left.
         */
        ABSENT_IN_KJV {
            override fun getDescription(q: QualifiedKey?): String {
                return if (q != null && q.sectionName != null) q.sectionName!! else "Missing section name"
            }
        };

        /**
         * @param q the QualifiedKey that this describes
         * @return The description for the qualified key
         */
        abstract fun getDescription(q: QualifiedKey?): String
    }

    private var qualifiedKey: VerseKey<*>? = null
    private var wholeKey: VerseKey<*>? = null

    /**
     * @return the name (any name) of the section represented within the KJV
     */
    var sectionName: String? = null
        private set

    /**
     * @return the type of the unknown qualifier
     */
    val absentType: Qualifier

    /**
     * Construct a QualifiedKey from a Verse.
     * Java had this constructor protected; a Kotlin protected constructor is not visible to the
     * package's other classes, so it is public (was protected).
     *
     * @param key the verse from which to create this QualifiedKey
     */
    constructor(key: Verse?) {
        setKey(key)
        this.absentType = Qualifier.DEFAULT
    }

    /**
     * Construct a QualifiedKey from a Verse.
     *
     * @param key the verse range from which to create this QualifiedKey
     */
    constructor(key: VerseRange?) {
        setKey(key)
        this.absentType = Qualifier.DEFAULT
    }

    /**
     * @param sectionName with a given section name, we assume absent in KJV
     */
    constructor(sectionName: String?) {
        this.sectionName = sectionName
        this.absentType = Qualifier.ABSENT_IN_KJV
    }

    /**
     * Constructs the QualifiedKey with the ABSENT_IN_LEFT qualifier.
     * This really means that there are no fields in this QualifiedKey.
     */
    constructor() {
        this.absentType = Qualifier.ABSENT_IN_LEFT
    }

    /**
     * @return * The internal key which is either a Verse or VerseRange
     */
    val key: VerseKey<*>?
        get() = wholeKey

    /**
     * @return * The internal key cast as a Verse
     * @throws ClassCastException
     */
    val verse: Verse?
        get() = wholeKey as Verse?

    /**
     * A QualifiedKey is whole if it does not split part of a reference.
     *
     * @return whether this QualifiedKey has a whole reference
     */
    val isWhole: Boolean
        get() {
            // If the reference is null, then it cannot be part or whole.
            // But we say it is whole because the calls to this are really testing
            // to see if it is a part.
            val qualifiedKey = this.qualifiedKey
            return qualifiedKey == null || qualifiedKey.isWhole
        }

    /**
     * Convert this QualifiedKey from one Versification to another.
     * This is a potentially dangerous operation that does no mapping
     * from one versification to another. Use it only when it is known
     * to be safe.
     *
     * @param target The target versification
     * @return The reversified QualifiedKey
     */
    fun reversify(target: Versification?): QualifiedKey {
        // Only if it has a qualified key can it be reversified
        val qualifiedKey = this.qualifiedKey ?: return this

        val reversifiedKey = qualifiedKey.reversify(target)
        if (reversifiedKey != null) {
            return create(reversifiedKey)
        }

        if (target!!.name == Versifications.DEFAULT_V11N) {
            //then we're absent in KJV
            return QualifiedKey(qualifiedKey.osisID)
        }
        return QualifiedKey()
    }

    override fun toString(): String {
        val buf = StringBuilder()
        if (wholeKey != null) {
            buf.append(qualifiedKey!!.osisRef)
        }
        val desc = absentType.getDescription(this)
        if (desc.length > 0) {
            if (buf.length > 0) {
                buf.append(": ")
            }
            buf.append(absentType.getDescription(this))
        }
        return buf.toString()
    }

    override fun hashCode(): Int {
        // Use a prime number in case one of the values is not around
        return ((this.qualifiedKey?.hashCode() ?: 17)
                + this.absentType.ordinal
                + (this.sectionName?.hashCode() ?: 19))
    }

    override fun equals(other: Any?): Boolean {
        if (other is QualifiedKey) {
            return (this.absentType === other.absentType
                    && bothNullOrEqual(this.sectionName, other.sectionName)
                    && bothNullOrEqual(this.qualifiedKey, other.qualifiedKey))
        }
        return false
    }

    /**
     * Allow override of the key, particular useful if we're constructing in 2 stages like the offset mechanism
     *
     * @param key the new key
     */
    private fun setKey(key: Verse?) {
        this.qualifiedKey = key
        this.wholeKey = key!!.whole
    }

    /**
     * Allow override of the key, particular useful if we're constructing in 2 stages like the offset mechanism
     *
     * @param key the new key
     */
    private fun setKey(key: VerseRange?) {
        if (key!!.cardinality == 1) {
            this.qualifiedKey = key.start
        } else {
            this.qualifiedKey = key
        }
        this.wholeKey = this.qualifiedKey!!.whole
    }

    companion object {
        /**
         * Create a QualifiedKey from a Verse or a VerseRange.
         *
         * @param k the Verse or VerseRange
         * @return the created QualifiedKey
         * @throws ClassCastException
         */
        @JvmStatic
        fun create(k: VerseKey<*>?): QualifiedKey {
            return if (k is Verse) QualifiedKey(k) else QualifiedKey(k as VerseRange?)
        }

        /**
         * Determine whether two objects are equal, allowing nulls
         *
         * @param x
         * @param y
         * @return true if both are null or the two are equal
         */
        private fun bothNullOrEqual(x: Any?, y: Any?): Boolean {
            return x === y || (x != null && x == y)
        }
    }
}
