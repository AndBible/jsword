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
package org.crosswire.jsword.book.filter

import org.crosswire.common.util.PluginRegistry
import org.crosswire.common.util.Logger
import org.crosswire.common.util.LoggerFactory


/**
 * A simple container for all the known SourceFilters.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
class SourceFilterFactory
/**
 * Prevent instantiation
 */
private constructor() {
    companion object {
        /**
         * Find a filter given a lookup string. If lookup is null or the filter is
         * not found then the default filter will be used.
         * 
         * @param lookup the lookup string for the filter
         * @return the matching filter
         */
        @JvmStatic
        fun getFilter(lookup: String?): SourceFilter {
            var reply = filters[lookup!!.lowercase()]

            if (reply == null) {
                reply = deft
            }

            return reply!!.clone()
        }

        /**
         * Find a filter given a lookup string
         * 
         * @return the default filter
         */
        @JvmStatic
        fun getDefaultFilter(): SourceFilter {
            return deft!!.clone()
        }

        /**
         * Add to our list of known filters
         * 
         * @param name 
         * @param instance 
         */
        @JvmStatic
        fun addFilter(name: String?, instance: SourceFilter?) {
            filters[name!!.lowercase()] = instance
        }

        /**
         * The lookup table of filters
         */
        private var filters: MutableMap<String, SourceFilter?> = HashMap()

        /**
         * The log stream
         */
        private val log: Logger = LoggerFactory.getLogger(SourceFilterFactory::class)

        /**
         * The lookup table of filters
         */
        @Volatile
        private var deft: SourceFilter? = null

        /**
         * Populate the lookup table of filters and the default from the properties
         * file.
         */
        init {
            val entries = PluginRegistry.entries(SourceFilter::class)

            // the default value
            try {
                deft = PluginRegistry.default(SourceFilter::class)!!.create()
            } catch (e: Exception) {
                log.error("Failed to get default filter, will attempt to use first", e)
            }

            // the lookup table
            var instance: SourceFilter? = null
            for (entry in entries) {
                if (entry.name == PluginRegistry.DEFAULT) continue
                try {
                    instance = entry.create()
                    addFilter(entry.name, instance)
                } catch (e: Exception) {
                    log.error("Failed to add filter", e)
                }
            }

            // if the default didn't work then make a stab at an answer
            if (deft == null) {
                deft = instance
            }
        }
    }
}
