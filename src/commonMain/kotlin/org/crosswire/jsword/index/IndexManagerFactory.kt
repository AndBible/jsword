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
package org.crosswire.jsword.index

import kotlin.jvm.JvmStatic
import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory


/**
 * A Factory class for IndexManagers.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class IndexManagerFactory
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Create a new IndexManager.
         * 
         * @return the singleton index manager
         */
        @JvmStatic
        fun getIndexManager(): IndexManager {
            return instance ?: throw SearchUnsupportedException()
        }

        /** Test seam: runs [block] with [manager] as the singleton and restores the previous one afterwards. */
        internal fun <T> withIndexManagerForTest(manager: IndexManager, block: () -> T): T {
            val saved = instance
            instance = manager
            try { return block() } finally { instance = saved }
        }

        /**
         * The singleton
         */
        private var instance: IndexManager? = null

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(IndexManagerFactory::class)

        /**
         * Setup the instance
         */
        init {
            try {
                instance = PluginRegistry.default(IndexManager::class)?.create() ?: throw SearchUnsupportedException()
            } catch (e: Exception) {
                log.error("createIndexManager failed", e)
            }
        }
    }
}
