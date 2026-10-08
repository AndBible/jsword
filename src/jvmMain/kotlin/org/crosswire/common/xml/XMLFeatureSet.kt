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
/*
 * This is inspired by DocumentChecker.
 * @author Andy Clark, IBM
 * @author Arnaud Le Hors, IBM
 *
 * Copyright 2000-2002,2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.crosswire.common.xml

import java.util.Locale
import java.util.TreeMap
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.XMLReader

/**
 * A set of useful XML Features
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
class XMLFeatureSet
/**
 * An XMLFeatureSet with default settings.
 */
{
    /**
     * Set the state of an XMLFeture in this set.
     * 
     * @param feature the XMLFeature to set
     * @param state whether the feature is on or off
     */
    fun setFeatureState(feature: XMLFeature, state: Boolean) {
        features.get(states.get(feature)!!)!!.setState(state)
    }

    /**
     * Allow for XMLFeatures to be set from command line.
     * 
     * @param argv the specification of XMLFeatures to turn on or off
     * @see .printUsage See printUsage for details
     */
    fun setFeatureStates(argv: Array<String>) {
        // process arguments
        for (i in argv.indices) {
            val arg = argv[i]
            if (arg[0] == '-') {
                val option = arg.substring(1)
                val key = option.lowercase(Locale.ENGLISH)
                val feature = features.get(key)
                if (feature != null) {
                    feature.setState(option == key)
                }
            }
        }
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append('\n')
        for (state in features.values) {
            buf.append(state.getFeature().toString()).append('\n')
        }
        return buf.toString()
    }

    /**
     * Prints the usage.
     */
    fun printUsage() {
        System.err.println("XML Feature Set options:")
        System.err.println("  -n  | -N    Turn on/off namespace processing.")
        System.err.println("  -np | -NP   Turn on/off namespace prefixes.")
        System.err.println("              NOTE: Requires use of -n.")
        System.err.println("  -v  | -V    Turn on/off validation.")
        System.err.println("  -xd | -XD   Turn on/off loading of external DTDs.")
        System.err.println("              NOTE: Always on when -v in use and not supported by all parsers.")
        System.err.println("  -s  | -S    Turn on/off Schema validation support.")
        System.err.println("              NOTE: Not supported by all parsers.")
        System.err.println("  -f  | -F    Turn on/off Schema full checking.")
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.")
        System.err.println("  -va | -VA   Turn on/off validation of schema annotations.")
        System.err.println("              NOTE: Requires use of -s and not supported by all parsers.")
        System.err.println("  -dv | -DV   Turn on/off dynamic validation.")
        System.err.println("              NOTE: Not supported by all parsers.")
        System.err.println("  -xi | -XI   Turn on/off XInclude processing.")
        System.err.println("              NOTE: Not supported by all parsers.")
        System.err.println("  -xb | -XB   Turn on/off base URI fixup during XInclude processing.")
        System.err.println("              NOTE: Requires use of -xi and not supported by all parsers.")
        System.err.println("  -xl | -XL   Turn on/off language fixup during XInclude processing.")
        System.err.println("              NOTE: Requires use of -xi and not supported by all parsers.")
    }

    /**
     * Set this XMLFeatureSets state onto an XMLReader.
     * 
     * @param parser the XMLReader
     */
    fun setFeatures(parser: XMLReader) {
        for (state in features.values) {
            state.setFeature(parser)
        }
    }

    /**
     * A holder of the boolean state for a feature.
     */
    private class XMLFeatureState
    /**
     * Bind a state to an XMLFeature.
     * 
     * @param feature the XMLFeature
     * @param state whether that XMLFeature is on or off
     */
    (feature: XMLFeature, state: Boolean) {
        /**
         * An XMLFeature that is turned off.
         * @param feature the XMLFeature
         */
        constructor(feature: XMLFeature) : this(feature, false)

        /**
         * @return Returns the feature.
         */
        fun getFeature(): XMLFeature {
            return feature
        }

        /**
         * Set the new state
         * 
         * @param newState whether the feature is on or off
         */
        fun setState(newState: Boolean) {
            state = newState
        }

        /**
         * Set the control state on the parser.
         * 
         * @param parser The parser on which to set this feature
         */
        fun setFeature(parser: XMLReader) {
            val control = feature.getControl()
            try {
                parser.setFeature(control, state)
            } catch (e: SAXNotRecognizedException) {
                System.err.println("warning: Parser does not recognize feature ($control)")
            } catch (e: SAXNotSupportedException) {
                System.err.println("warning: Parser does not support feature ($control)")
            }
        }

        private var state: Boolean = state
        private var feature: XMLFeature = feature
    }

    private var features: MutableMap<String, XMLFeatureState> = TreeMap<String, XMLFeatureState>()
    private var states: MutableMap<XMLFeature, String> = TreeMap<XMLFeature, String>()

    init {
        features.put("n", XMLFeatureState(XMLFeature.NAMESPACES, true))
        features.put("np", XMLFeatureState(XMLFeature.NAMESPACE_PREFIX))
        features.put("v", XMLFeatureState(XMLFeature.VALIDATION))
        features.put("xd", XMLFeatureState(XMLFeature.LOAD_EXTERNAL_DTD, true))
        features.put("s", XMLFeatureState(XMLFeature.SCHEMA_VALIDATION))
        features.put("f", XMLFeatureState(XMLFeature.SCHEMA_FULL_CHECKING))
        features.put("va", XMLFeatureState(XMLFeature.VALIDATE_ANNOTATIONS))
        features.put("dv", XMLFeatureState(XMLFeature.DYNAMIC_VALIDATION))
        features.put("xi", XMLFeatureState(XMLFeature.XINCLUDE))
        features.put("xb", XMLFeatureState(XMLFeature.XINCLUDE_FIXUP_BASE_URIS, true))
        features.put("xl", XMLFeatureState(XMLFeature.XINCLUDE_FIXUP_LANGUAGE, true))

        for (entry in features.entries) {
            states.put(entry.value.getFeature(), entry.key)
        }
    }
}
