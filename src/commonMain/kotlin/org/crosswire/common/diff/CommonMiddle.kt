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
 * © CrossWire Bible Society, 2007 - 2016
 *
 */
package org.crosswire.common.diff

/**
 * A CommonMiddle represents an overlap between a baseline/source text and a
 * changed/target text.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
open class CommonMiddle(
    /**
     * A CommonMiddle represents an overlap between a baseline/source text and a
     * changed/target text.
     *
     * @param sourcePrefix
     *            The text before the commonality form the source
     * @param sourceSuffix
     *            The text after the commonality form the source
     * @param targetPrefix
     *            The text before the commonality form the target
     * @param targetSuffix
     *            The text after the commonality form the target
     * @param commonality
     *            The text in common
     */
    private val sourcePrefix: String?,
    private val sourceSuffix: String?,
    private val targetPrefix: String?,
    private val targetSuffix: String?,
    private val commonality: String?,
) {
    /**
     * @return the source start
     */
    open fun getSourcePrefix(): String? = sourcePrefix

    /**
     * @return the target start
     */
    open fun getTargetPrefix(): String? = targetPrefix

    /**
     * @return the commonality
     */
    open fun getCommonality(): String? = commonality

    /**
     * @return the source end
     */
    open fun getSourceSuffix(): String? = sourceSuffix

    /**
     * @return the target end
     */
    open fun getTargetSuffix(): String? = targetSuffix

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append(sourcePrefix)
        buf.append(',')
        buf.append(sourceSuffix)
        buf.append(',')
        buf.append(targetPrefix)
        buf.append(',')
        buf.append(targetSuffix)
        buf.append(',')
        buf.append(commonality)
        return buf.toString()
    }

    override fun hashCode(): Int {
        var result = 31 + (sourcePrefix?.hashCode() ?: 0)
        result = 31 * result + (sourceSuffix?.hashCode() ?: 0)
        result = 31 * result + (targetPrefix?.hashCode() ?: 0)
        result = 31 * result + (targetSuffix?.hashCode() ?: 0)
        return 31 * result + (commonality?.hashCode() ?: 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || this::class != other::class) {
            return false
        }

        other as CommonMiddle

        // Java dereferenced the fields here (NPE on null), hence !!
        return sourcePrefix!! == other.sourcePrefix && sourceSuffix!! == other.sourceSuffix && targetPrefix!! == other.targetPrefix &&
            targetSuffix!! == other.targetSuffix && commonality!! == other.commonality
    }
}
