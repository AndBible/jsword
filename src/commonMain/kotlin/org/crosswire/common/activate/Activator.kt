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
package org.crosswire.common.activate

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.jvm.JvmStatic

/**
 * Manager for instances of Activatable.
 *
 * Activator should be used to manage all activate()ions and deactivate()ions so
 * that it can keep a track of exactly what is active and what can be
 * deactivated is save memory.
 *
 * Package overview: code to allow classes to be started-up and then shutdown
 * later to conserve resources.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class Activator
/**
 * Prevent instantiation
 */
private constructor() {
    // singleton - no set-up needed

    companion object {
        /**
         * Check that a subject is activated and call activate() if not.
         *
         * Synchronized so the contains-check and the activate()/add() form a
         * single atomic operation. Otherwise a concurrent [deactivate] could
         * leave the `activated` set desynced from the subject's own state,
         * causing a reactivation to be wrongly skipped (see JSword issue: NPE in
         * SwordGenBook.getKey when a book is (de)activated on one thread while read
         * on another).
         *
         * @param subject
         *            The thing to activate
         */
        @JvmStatic
        fun activate(subject: Activatable?) {
            synchronized(activationLock) {
                if (subject != null && activated.none { it === subject }) {
                    subject.activate(lock)
                    activated.add(subject)
                }
            }
        }

        /**
         * If we need to tighten things up a bit we can save memory with this
         *
         * @param amount the amount by which to to reduce memory
         */
        @JvmStatic
        fun reduceMemoryUsage(amount: Kill) {
            amount.reduceMemoryUsage()
        }

        /**
         * Deactivate an Activatable object. It is safe to activate() something and
         * then forget to deactivate() it since we keep a track of activated objects
         * and will automatically deactivate() when needed, so this method should
         * only be used when we are sure that something will not be needed again.
         *
         * @param subject
         *            The thing to deactivate
         */
        @JvmStatic
        fun deactivate(subject: Activatable?) {
            synchronized(activationLock) {
                if (subject != null && activated.any { it === subject }) {
                    subject.deactivate(lock)
                    activated.removeAll { it === subject }
                }
            }
        }

        @JvmStatic
        fun deactivateAll() {
            synchronized(activationLock) {
                // Deactivate directly (not via deactivate()) and clear afterwards so we
                // don't remove entries while iterating the set.
                for (item in activated) {
                    item.deactivate(lock)
                }
                activated.clear()
            }
        }

        /**
         * The list of things that we have activated.
         *
         * Identity-based on purpose. Activation state lives in the instance
         * (e.g. SwordGenBook's key map), so it must be tracked per instance, not
         * per equal value. Book equality is by (category, name, initials), so a
         * replacement instance for the same module compares equal to the one it
         * replaced. With a value-based set, activating the replacement would be
         * skipped as "already activated" and its key map would stay null, making
         * every getKey() throw NoSuchKeyException and getGlobalKeyList() return an
         * empty list until something forced a deactivate/activate cycle.
         */
        private val activated = mutableListOf<Activatable>()
        private val activationLock = SynchronizedObject()

        /**
         * The object we use to prevent others from
         */
        private var lock: Lock = Lock()
    }
}
