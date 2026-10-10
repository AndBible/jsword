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

import org.crosswire.jsword.versification.Versification
import org.crosswire.jsword.versification.system.Versifications

/**
 * .
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
class KeyUtil
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Walk through a tree visiting the nodes and branches in the tree
         *
         * @param key
         *            The node tree to walk through
         * @param visitor
         *            The visitor to notify whenever a node is found
         */
        @JvmStatic
        fun visit(key: Key?, visitor: KeyVisitor?) {
            for (subkey in key!!) {
                if (subkey.canHaveChildren()) {
                    visitor!!.visitBranch(subkey)
                    visit(subkey, visitor)
                } else {
                    visitor!!.visitLeaf(subkey)
                }
            }
        }

        /**
         * Cast a Key to a Verse. Only those keys that are a Verse or can
         * contain Verses (i.e. Passage and VerseRange) may be cast to one.
         * Verse containers (i.e. Passage and VerseRange) return their
         * first verse.
         *
         * @param key The key to cast
         * @return The key cast to a Verse
         * @throws ClassCastException
         */
        @JvmStatic
        fun getVerse(key: Key?): Verse {
            if (key is Verse) {
                return key
            }

            if (key is VerseRange) {
                return key.start
            }

            if (key is Passage) {
                return key.getVerseAt(0)
            }

            throw ClassCastException("Expected key to be a Verse, VerseRange or Passage")
        }

        /**
         * Cast a Key to a Passage. Only those keys that are a Passage or can
         * be held by a Passage (i.e. Verse and VerseRange) may be cast to one.
         * If you pass a null key into this method, you get a null Passage out.
         *
         * @param key The key to cast
         * @return The key cast to a Passage
         * @throws ClassCastException
         */
        @JvmStatic
        fun getPassage(key: Key?): Passage? {
            if (key == null) {
                return null
            }

            if (key is Passage) {
                return key
            }

            if (key is VerseKey<*>) {
                val ref = PassageKeyFactory.instance().createEmptyKeyList(key.versification)
                ref.addAll(key)
                return ref as Passage
            }

            throw ClassCastException("Expected key to be a Verse, VerseRange or Passage")
        }

        /**
         * Get the versification for the key or the default versification.
         *
         * @param key the key that should provide for the Versification
         * @return the versification for the key
         */
        @JvmStatic
        fun getVersification(key: Key?): Versification {
            if (key is VerseKey<*>) {
                return key.versification
            }
            return Versifications.instance().getVersification(Versifications.DEFAULT_V11N)!!
        }
    }
}
