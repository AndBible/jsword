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
 * © CrossWire Bible Society, 2011 - 2016
 *
 */
package org.crosswire.common.util

import kotlin.jvm.JvmField

/**
 * Version is an immutable representation of dotted "number" consisting of 1 to 4 parts.
 *
 * Here is the grammar for version strings:
 *
 * <pre>
 * version ::= major('.'minor('.'micro('.'nano)?)?)?
 * major ::= [0-9]+
 * minor ::= [0-9]+
 * micro ::= [0-9]+
 * nano  ::= [0-9]+
 * </pre>
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class Version : Comparable<Version> {
    /**
     * Created a version identifier from the specified string.
     *
     * @param version String representation of the version identifier.
     * @throws IllegalArgumentException If `version` is improperly
     *         formatted.
     */
    constructor(version: String?) {
        if (version == null) {
            throw IllegalArgumentException("Null version not allowed.")
        }
        this.original = version
        this.parts = intArrayOf(-1, -1, -1, -1)
        val matcher = VERSION_PATTERN.matchEntire(this.original)
        if (matcher != null) {
            val count = matcher.groups.size - 1
            for (i in 1..count) {
                val part: String = matcher.groups[i]?.value ?: break
                parts[i - 1] = part.toInt()
            }
        } else {
            throw IllegalArgumentException("invalid: $version")
        }
    }

    /**
     * Returns the original string representation of this version identifier.
     *
     * @return The original string representation of this version identifier.
     */
    override fun toString(): String {
        return original
    }

    override fun hashCode(): Int {
        return original.hashCode()
    }

    /**
     * Compares this `Version` object to another object.
     *
     * A version is considered to be equal to another version if all the
     * parts are equal.
     *
     * @param object The `Version` object to be compared.
     * @return true if the two objects are equal.
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Version) {
            return false
        }

        val that: Version = other
        if (that === this) {
            return true
        }

        for (i in parts.indices) {
            if (parts[i] != that.parts[i]) {
                return false
            }
        }
        return true
    }

    /**
     * Compares this `Version` object to another object.
     *
     * The comparison considers each of the parts (major, minor, micro, nano) in turn,
     * comparing like with like. At any point the comparison is not equal, a result is
     * known.
     *
     * @param other The `Version` object to be compared.
     * @return A negative integer, zero, or a positive integer if this object is
     *         less than, equal to, or greater than the specified
     *         `Version` object.
     * @throws ClassCastException If the specified object is not a `Version`.
     */
    override fun compareTo(other: Version): Int {
        if (other === this) {
            return 0
        }

        for (i in parts.indices) {
            val result = parts[i] - other.parts[i]
            if (result != 0) {
                return result
            }
        }
        return 0
    }

    private val original: String
    private val parts: IntArray

    companion object {
        @JvmField
        val VERSION_PATTERN: Regex = Regex("^(\\d+)(?:.(\\d+))?(?:.(\\d+))?(?:.(\\d+))?$")
    }
}
