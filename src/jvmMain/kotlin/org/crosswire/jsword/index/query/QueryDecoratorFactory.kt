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
package org.crosswire.jsword.index.query

import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory


/**
 * A Factory class for QueryDecorator.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class QueryDecoratorFactory
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Create a new QueryDecorator.
         * 
         * @return a new query decorator
         */
        @JvmStatic
        fun getSearchSyntax(): QueryDecorator {
            return instance!!
        }

        /**
         * The singleton
         */
        private var instance: QueryDecorator? = null

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(QueryDecoratorFactory::class)

        /**
         * Setup the instance
         */
        init {
            try {
                instance = PluginRegistry.default(QueryDecorator::class)!!.create()
            } catch (e: Exception) {
                log.error("create QueryDecorator failed", e)
            }
        }
    }
}
