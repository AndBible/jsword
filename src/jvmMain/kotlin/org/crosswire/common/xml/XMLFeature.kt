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
package org.crosswire.common.xml

import org.crosswire.common.util.jswordAssert

/**
 * Wraps an XML Feature. The "known" set of XML Features is found in
 * XMLFeatureSet.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author DM Smith
 */
enum class XMLFeature
/**
 * Construct a feature for xml, setting the initial state
 * 
 * @param control the identifier for an XML feature
 * @param initialState whether that feature defaults to on or off
 */
(control: String, initialState: Boolean) {
    /** Namespaces feature id  */
    NAMESPACES("http://xml.org/sax/features/namespaces"),

    /** Namespace prefixes feature id  */
    NAMESPACE_PREFIX("http://xml.org/sax/features/namespace-prefixes"),

    /** Validation feature id  */
    VALIDATION("http://xml.org/sax/features/validation"),

    /** Schema validation feature id  */
    SCHEMA_VALIDATION("http://apache.org/xml/features/validation/schema"),

    /** Schema full checking feature id  */
    SCHEMA_FULL_CHECKING("http://apache.org/xml/features/validation/schema-full-checking"),

    /** Validate schema annotations feature id  */
    VALIDATE_ANNOTATIONS("http://apache.org/xml/features/validate-annotations"),

    /** Dynamic validation feature id  */
    DYNAMIC_VALIDATION("http://apache.org/xml/features/validation/dynamic"),

    /** Load external DTD feature id  */
    LOAD_EXTERNAL_DTD("http://apache.org/xml/features/nonvalidating/load-external-dtd"),

    /** XInclude feature id  */
    XINCLUDE("http://apache.org/xml/features/xinclude"),

    /** XInclude fixup base URIs feature id  */
    XINCLUDE_FIXUP_BASE_URIS("http://apache.org/xml/features/xinclude/fixup-base-uris", true),

    /** XInclude fixup language feature id  */
    XINCLUDE_FIXUP_LANGUAGE("http://apache.org/xml/features/xinclude/fixup-language", true);

    /**
     * Construct a feature for xml, setting the initial state set to false.
     * 
     * @param control the identifier for an XML feature that is initially off
     */
    constructor(control: String) : this(control, false)

    /**
     * @return the control associated with this feature
     */
    fun getControl(): String {
        return control
    }

    /**
     * What state should the feature be set to.
     * 
     * @return the state of the feature
     */
    fun getState(): Boolean {
        return state
    }

    override fun toString(): String {
        return (if (state) "on  " else "off ") + control
    }

    private var control: String
    private var state: Boolean

    init {
        this.control = control
        this.state = initialState
    }

    companion object {
        /**
         * Lookup method to convert from a String
         * 
         * @param name the name of the control feature
         * @return the XML Feature
         */
        @JvmStatic
        fun fromString(name: String?): XMLFeature? {
            for (o in entries) {
                if (o.control.equals(name, ignoreCase = true)) {
                    return o
                }
            }
            // cannot get here
            jswordAssert(false) { "" }
            return null
        }
    }
}
