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
/**
 * A collection of various XML utility classes.
 */
package org.crosswire.common.xml

import okio.IOException
import org.crosswire.common.util.isJavaWhitespace
import org.crosswire.common.util.PropertyMap
import org.crosswire.common.util.jswordAssert
import fi.sykero.kdom.Document
import fi.sykero.kdom.JDOMException
import org.crosswire.common.util.LoggerFactory
import fi.sykero.kdom.sax.Attributes
import fi.sykero.kdom.sax.ContentHandler
import fi.sykero.kdom.sax.SAXException

/**
 * Utilities for working with SAX XML parsing.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 * @author DM Smith
 */
// An object, not a companion: getDocument() calls ResourceUtil.getResourceAsStream(String), which identifies the
// calling class from the stack, so the caller must be XMLUtil itself (not XMLUtil$Companion). Being an object also
// makes the constructor private, as the Java utility class had.
object XMLUtil {
    /**
     * Get and load an XML file from the classpath and a few other places into a
     * JDOM Document object.
     * 
     * @param subject
     *            The name of the desired resource (without any extension)
     * @return The requested resource
     * @throws IOException
     *             if there is a problem reading the file
     * @throws JDOMException
     *             If the resource is not valid XML
     *
     * Note: this reads a classpath resource. The embedded resources are no longer on the main classpath, so it
     * cannot find them; it has no callers and is left for K5 (read via KswordResources then).
     */
    @JvmStatic
    @Throws(JDOMException::class, IOException::class)
    fun getDocument(subject: String?): Document {
        return loadValidatedResource(subject)
    }

    /**
     * Serialize a SAXEventProvider into an XML String
     * 
     * @param provider
     *            The source of SAX events
     * @return a serialized string
     * @throws SAXException
     */
    @JvmStatic
    @Throws(SAXException::class)
    fun writeToString(provider: SAXEventProvider): String {
        val ser: ContentHandler = PrettySerializingContentHandler()
        provider.provideSAXEvents(ser)
        return ser.toString()
    }

    /**
     * Get the full name of the attribute, including the namespace if any.
     * 
     * @param attrs
     *            the collection of attributes
     * @param index
     *            the index of the desired attribute
     * @return the requested attribute
     */
    @JvmStatic
    fun getAttributeName(attrs: Attributes, index: Int): String? {
        val qName = attrs.getQName(index)
        if (qName != null) {
            return qName
        }
        return attrs.getLocalName(index)
    }

    /**
     * Show the attributes of an element as debug
     * @param attrs
     */
    @JvmStatic
    fun debugSAXAttributes(attrs: Attributes) {
        for (i in 0 until attrs.length) {
            log.debug("attr[{}]: {}={}", i.toString(), attrs.getQName(i), attrs.getValue(i))
        }
    }

    /**
     * Normalizes the given string
     * @param s
     * @return the escaped string
     */
    @JvmStatic
    fun escape(s: String?): String? {
        if (s == null) {
            return s
        }
        val len = s.length
        val str = StringBuilder(len)

        for (i in 0 until len) {
            val ch = s[i]
            when (ch) {
                '<' -> str.append("&lt;")

                '>' -> str.append("&gt;")

                '&' -> str.append("&amp;")

                '"' -> str.append("&quot;")

                else -> str.append(ch)
            }
        }

        return str.toString()
    }

    /**
     * For each entity in the input that is not allowed in XML, replace the
     * entity with its unicode equivalent or remove it. For each instance of a
     * bare &, replace it with &amp;<br>
     * XML only allows 4 entities: &amp;amp;, &amp;quot;, &amp;lt; and &amp;gt;.
     * 
     * @param broken
     *            the string to handle entities
     * @return the string with entities appropriately fixed up
     */
    @JvmStatic
    fun cleanAllEntities(broken: String?): String? {
        if (broken == null) {
            return null
        }

        var working: String = broken
        var cleanfrom = 0

        while (true) {
            var amp = working.indexOf('&', cleanfrom)

            // If there are no more amps then we are done
            if (amp == -1) {
                break
            }

            // Skip references of the kind &#ddd;
            if (validCharacterEntityPattern.containsMatchIn(working.substring(amp))) {
                cleanfrom = working.indexOf(';', amp) + 1
                continue
            }

            var i = amp + 1
            while (true) {
                // if we are at the end of the string then just escape the '&';
                if (i >= working.length) {
                    // String entity = working.substring(amp);
                    // String replace = guessEntity(entity);
                    // DataPolice.report("replacing unterminated entity: '" +
                    // entity + "' with: '" + replace + "'");

                    return working.substring(0, amp) + "&amp;" + working.substring(amp + 1)
                }

                // if we have come to a ; then we have an entity
                // If it is something that xml can't handle then replace it.
                val c = working[i]
                if (c == ';') {
                    val entity = working.substring(amp, i + 1)
                    val replace = handleEntity(entity)
                    // log.warn("replacing entity: '{}' with: '{}'", entity, replace);

                    working = working.substring(0, amp) + replace + working.substring(i + 1)
                    break
                }

                // Did we end an entity without finding a closing ;
                // Then treat it as an '&' that needs to be replaced with &amp;
                if (!c.isLetterOrDigit()) {
                    // String entity = working.substring(amp, i);
                    // String replace = "&amp;" + working.substring(amp + 1, i);
                    // log.warn("replacing invalid entity: '{}' with: '{}': {}", entity, replace, broken);

                    working = working.substring(0, amp) + "&amp;" + working.substring(amp + 1)
                    amp = i + 4 // account for the 4 extra characters
                    break
                }

                i++
            }

            cleanfrom = amp + 1
        }

        return working
    }

    /**
     * Remove all invalid characters in the input, replacing them with a space. XML has stringent
     * requirements as to which characters are or are not allowed. The set of
     * allowable characters are:<br>
     * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]<br>
     * Note: Java handles to \uFFFF
     * 
     * @param broken
     *            the string to be cleaned
     * @return the cleaned string
     */
    @JvmStatic
    fun cleanAllCharacters(broken: String): String {
        return invalidCharacterPattern.replace(broken, " ")
    }

    /**
     * Strip all closing tags from the end of the XML fragment, and then
     * re-close all tags that are open at the end of the string.
     *
     * @param broken
     *            the string to be cleaned.
     * @return cleaned string, or {@code null} if the string could not be
     *         cleaned due to more broken XML
     */
    @JvmStatic
    fun recloseTags(broken: String): String? {
        var result = broken
        // remove closing tags from the end
        while (Regex(".*</[a-zA-Z]+>[ \t\r\n]*").matches(result)) {
            result = result.substring(0, result.lastIndexOf('<'))
        }
        // close tags again
        val openTags: MutableList<String> = ArrayList()
        val matches = Regex("</?[a-zA-Z]+").findAll(result)
        var lTagFound = false
        var lgTagFound = false
        for (m in matches) {
            val match = m.value
            if (match.startsWith("</")) {
                if (openTags.size == 0 && match == "</l" && !lTagFound) {
                    return recloseTags("<l>" + broken)
                }
                if (openTags.size == 0 && match == "</lg" && !lgTagFound) {
                    return recloseTags("<lg>" + broken)
                }
                if (openTags.size == 0)
                    return null
                val lastTag = openTags.removeAt(openTags.size - 1)
                if (match != "</" + lastTag)
                    return null
            } else {
                var closePos = result.indexOf('>', m.range.last + 1)
                if (closePos == -1)
                    return null
                while (result[closePos - 1].isJavaWhitespace()) closePos--
                if (result[closePos - 1] != '/') {
                    if (match == "<l") lTagFound = true
                    if (match == "<lg") lgTagFound = true
                    openTags.add(match.substring(1))
                }
            }
        }
        openTags.reverse()
        for (openTag in openTags) {
            result += "</" + openTag + ">"
        }
        return result
    }

    /**
     * Common HTML tags such as &lt;br&gt;,&lt;hr&gt; and &lt;img&gt; may be
     * left open causing XML parsing to fail. This method closes these tags.
     * 
     * @param broken
     *            the string to be cleaned
     * @return the cleaned string
     */
    @JvmStatic
    fun closeEmptyTags(broken: String?): String? {
        if (broken == null) {
            return null
        }

        return openHTMLTagPattern.replace(broken, "<\$1\$2/>")
    }

    /**
     * XML parse failed, so we can try getting rid of all the tags and having
     * another go. We define a tag to start at a &lt; and end at the end of the
     * next word (where a word is what comes in between spaces) that does not
     * contain an = sign, or at a >, whichever is earlier.
     * @param broken
     * @return the string without any tags
     */
    @JvmStatic
    fun cleanAllTags(broken: String?): String? {
        if (broken == null) {
            return null
        }

        var working: String = broken

        allTags@ while (true) {
            val lt = working.indexOf('<')

            // If there are no more amps then we are done
            if (lt == -1) {
                break@allTags
            }

            // loop to find the end of this tag
            var i = lt
            var startattr = -1

            singletag@ while (true) {
                i++

                // the tag can't exist past the end of the string
                if (i >= working.length) {
                    // go back one so we can safely chop
                    i--
                    break@singletag
                }

                val c = working[i]

                // normal end of tag
                if (c == '>') {
                    break@singletag
                }

                // we declare end-of-tag if this 'word' is not an attribute
                if (c == ' ') {
                    if (startattr == -1) {
                        // NOTE(joe): should we skip over consecutive spaces?
                        startattr = i
                    } else {
                        // so we've already had a space indicating start of
                        // attribute, so this must be the beginning of the next
                        // NOTE(joe): no - spaces can exist in attr values
                        val value = working.substring(startattr, i)
                        if (value.indexOf('=') == -1) {
                            // this 'attribute' does not contain an equals so
                            // we call it a word and end the parse
                            break@singletag
                        }
                    }
                }
            }

            // So we have the end of the tag, delete it, but leave a space in it's place
            // DataPolice.report("discarding tag: " + working.substring(lt, i + 1));
            working = working.substring(0, lt) + " " + working.substring(i + 1)
        }

        return working
    }

    /**
     * Replace entity with its unicode equivalent, if it is not a valid XML
     * entity. Otherwise strip it out. XML only allows 4 entities: &amp;amp;,
     * &amp;quot;, &amp;lt; and &amp;gt;.
     * 
     * @param entity
     *            the entity to be replaced
     * @return the substitution for the entity, either itself, the unicode
     *         equivalent or an empty string.
     */
    private fun handleEntity(entity: String): String {
        if (goodEntities.contains(entity)) {
            return entity
        }

        val replace = badEntities.get(entity)
        if (replace != null) {
            return replace
        }

        // replace unknown entities with a space
        return " "
    }

    // Map entities to their unicode equivalent
    private val goodEntities: MutableSet<String> = HashSet()
    private val badEntities = PropertyMap()

    init {
        // pre-defined XML entities
        goodEntities.add("&quot;") // quotation mark
        goodEntities.add("&amp;") // ampersand
        goodEntities.add("&lt;") // less-than sign
        goodEntities.add("&gt;") // greater-than sign

        // misc entities
        badEntities["&euro;"] = "\u20AC" // euro
        badEntities["&lsquo;"] = "\u2018" // left single quotation mark
        badEntities["&rsquo;"] = "\u2019" // right single quotation mark

        // Latin 1 entities
        badEntities["&nbsp;"] = "\u00A0" // no-break space
        badEntities["&iexcl;"] = "\u00A1" // inverted exclamation mark
        badEntities["&cent;"] = "\u00A2" // cent sign
        badEntities["&pound;"] = "\u00A3" // pound sign
        badEntities["&curren;"] = "\u00A4" // currency sign
        badEntities["&yen;"] = "\u00A5" // yen sign
        badEntities["&brvbar;"] = "\u00A6" // broken vertical bar
        badEntities["&sect;"] = "\u00A7" // section sign
        badEntities["&uml;"] = "\u00A8" // diaeresis
        badEntities["&copy;"] = "\u00A9" // copyright sign
        badEntities["&ordf;"] = "\u00AA" // feminine ordinal indicator
        badEntities["&laquo;"] = "\u00AB" // left-pointing double angle quotation mark
        badEntities["&not;"] = "\u00AC" // not sign
        badEntities["&shy;"] = "\u00AD" // soft hyphen
        badEntities["&reg;"] = "\u00AE" // registered sign
        badEntities["&macr;"] = "\u00AF" // macron
        badEntities["&deg;"] = "\u00B0" // degree sign
        badEntities["&plusmn;"] = "\u00B1" // plus-minus sign
        badEntities["&sup2;"] = "\u00B2" // superscript two
        badEntities["&sup3;"] = "\u00B3" // superscript three
        badEntities["&acute;"] = "\u00B4" // acute accent
        badEntities["&micro;"] = "\u00B5" // micro sign
        badEntities["&para;"] = "\u00B6" // pilcrow sign
        badEntities["&middot;"] = "\u00B7" // middle dot
        badEntities["&cedil;"] = "\u00B8" // cedilla
        badEntities["&sup1;"] = "\u00B9" // superscript one
        badEntities["&ordm;"] = "\u00BA" // masculine ordinal indicator
        badEntities["&raquo;"] = "\u00BB" // right-pointing double angle quotation mark
        badEntities["&frac14;"] = "\u00BC" // vulgar fraction one quarter
        badEntities["&frac12;"] = "\u00BD" // vulgar fraction one half
        badEntities["&frac34;"] = "\u00BE" // vulgar fraction three quarters
        badEntities["&iquest;"] = "\u00BF" // inverted question mark
        badEntities["&Agrave;"] = "\u00C0" // latin capital letter A with grave
        badEntities["&Aacute;"] = "\u00C1" // latin capital letter A with acute
        badEntities["&Acirc;"] = "\u00C2" // latin capital letter A with circumflex
        badEntities["&Atilde;"] = "\u00C3" // latin capital letter A with tilde
        badEntities["&Auml;"] = "\u00C4" // latin capital letter A with diaeresis
        badEntities["&Aring;"] = "\u00C5" // latin capital letter A with ring above
        badEntities["&AElig;"] = "\u00C6" // latin capital letter AE
        badEntities["&Ccedil;"] = "\u00C7" // latin capital letter C with cedilla
        badEntities["&Egrave;"] = "\u00C8" // latin capital letter E with grave
        badEntities["&Eacute;"] = "\u00C9" // latin capital letter E with acute
        badEntities["&Ecirc;"] = "\u00CA" // latin capital letter E with circumflex
        badEntities["&Euml;"] = "\u00CB" // latin capital letter E with diaeresis
        badEntities["&Igrave;"] = "\u00CC" // latin capital letter I with grave
        badEntities["&Iacute;"] = "\u00CD" // latin capital letter I with acute
        badEntities["&Icirc;"] = "\u00CE" // latin capital letter I with circumflex
        badEntities["&Iuml;"] = "\u00CF" // latin capital letter I with diaeresis
        badEntities["&ETH;"] = "\u00D0" // latin capital letter ETH
        badEntities["&Ntilde;"] = "\u00D1" // latin capital letter N with tilde
        badEntities["&Ograve;"] = "\u00D2" // latin capital letter O with grave
        badEntities["&Oacute;"] = "\u00D3" // latin capital letter O with acute
        badEntities["&Ocirc;"] = "\u00D4" // latin capital letter O with circumflex
        badEntities["&Otilde;"] = "\u00D5" // latin capital letter O with tilde
        badEntities["&Ouml;"] = "\u00D6" // latin capital letter O with diaeresis
        badEntities["&times;"] = "\u00D7" // multiplication sign
        badEntities["&Oslash;"] = "\u00D8" // latin capital letter O with stroke
        badEntities["&Ugrave;"] = "\u00D9" // latin capital letter U with grave
        badEntities["&Uacute;"] = "\u00DA" // latin capital letter U with acute
        badEntities["&Ucirc;"] = "\u00DB" // latin capital letter U with circumflex
        badEntities["&Uuml;"] = "\u00DC" // latin capital letter U with diaeresis
        badEntities["&Yacute;"] = "\u00DD" // latin capital letter Y with acute
        badEntities["&THORN;"] = "\u00DE" // latin capital letter THORN
        badEntities["&szlig;"] = "\u00DF" // latin small letter sharp s
        badEntities["&agrave;"] = "\u00E0" // latin small letter a with grave
        badEntities["&aacute;"] = "\u00E1" // latin small letter a with acute
        badEntities["&acirc;"] = "\u00E2" // latin small letter a with circumflex
        badEntities["&atilde;"] = "\u00E3" // latin small letter a with tilde
        badEntities["&auml;"] = "\u00E4" // latin small letter a with diaeresis
        badEntities["&aring;"] = "\u00E5" // latin small letter a with ring above
        badEntities["&aelig;"] = "\u00E6" // latin small letter ae
        badEntities["&ccedil;"] = "\u00E7" // latin small letter c with cedilla
        badEntities["&egrave;"] = "\u00E8" // latin small letter e with grave
        badEntities["&eacute;"] = "\u00E9" // latin small letter e with acute
        badEntities["&ecirc;"] = "\u00EA" // latin small letter e with circumflex
        badEntities["&euml;"] = "\u00EB" // latin small letter e with diaeresis
        badEntities["&igrave;"] = "\u00EC" // latin small letter i with grave
        badEntities["&iacute;"] = "\u00ED" // latin small letter i with acute
        badEntities["&icirc;"] = "\u00EE" // latin small letter i with circumflex
        badEntities["&iuml;"] = "\u00EF" // latin small letter i with diaeresis
        badEntities["&eth;"] = "\u00F0" // latin small letter eth
        badEntities["&ntilde;"] = "\u00F1" // latin small letter n with tilde
        badEntities["&ograve;"] = "\u00F2" // latin small letter o with grave
        badEntities["&oacute;"] = "\u00F3" // latin small letter o with acute
        badEntities["&ocirc;"] = "\u00F4" // latin small letter o with circumflex
        badEntities["&otilde;"] = "\u00F5" // latin small letter o with tilde
        badEntities["&ouml;"] = "\u00F6" // latin small letter o with diaeresis
        badEntities["&divide;"] = "\u00F7" // division sign
        badEntities["&oslash;"] = "\u00F8" // latin small letter o with stroke
        badEntities["&ugrave;"] = "\u00F9" // latin small letter u with grave
        badEntities["&uacute;"] = "\u00FA" // latin small letter u with acute
        badEntities["&ucirc;"] = "\u00FB" // latin small letter u with circumflex
        badEntities["&uuml;"] = "\u00FC" // latin small letter u with diaeresis
        badEntities["&yacute;"] = "\u00FD" // latin small letter y with acute
        badEntities["&thorn;"] = "\u00FE" // latin small letter thorn
        badEntities["&yuml;"] = "\u00FF" // latin small letter y with diaeresis
    }

    /**
     * Pattern for numeric entities.
     */
    private val validCharacterEntityPattern = Regex("^&#x?\\d{2,4};")

    /**
     * Pattern that negates the allowable XML 4 byte unicode characters. Valid
     * are: #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
     * [#x10000-#x10FFFF]
     */
    private val invalidCharacterPattern = Regex("[^\t\r\n\u0020-\uD7FF\uE000-\uFFFD]")

    /**
     * Pattern that matches open &lt;br&gt;,&lt;hr&gt; and &lt;img&gt; tags.
     */
    private val openHTMLTagPattern = Regex("<(img|hr|br)([^>]*)(?<!/)>")

    /**
     * The log stream
     */
    private val log = LoggerFactory.getLogger(XMLUtil::class)
}
