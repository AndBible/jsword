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

import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.util.MissingResourceException
import fi.sykero.kdom.AttributeType
import fi.sykero.kdom.Document
import fi.sykero.kdom.Element
import fi.sykero.kdom.JDOMException
import fi.sykero.kdom.Namespace
import fi.sykero.kdom.ProcessingInstruction
import fi.sykero.kdom.sax.AttributesImpl
import fi.sykero.kdom.sax.ContentHandler
import fi.sykero.kdom.sax.SAXException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Characterization of the SAX serializers, the SAX providers and [XMLUtil]:
 * pins the behaviour of the Java classes before they are converted to Kotlin.
 * Expected values were derived by reading the Java, not by observing it.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
class XMLSerializationCharacterizationTest {

    /** A pretty handler that exposes the protected decorateIndent, as Java's same-package access did. */
    private class IndentProbe : PrettySerializingContentHandler(FormatType.CLASSIC_INDENT) {
        fun indent(level: Int): String = decorateIndent(level)
    }

    /** Attributes with a nullable qualified name, for the SAX interface contract. */
    private class NullableQNameAttributes : fi.sykero.kdom.sax.Attributes {
        private val uris = arrayOf("u", "", "")
        private val locals = arrayOf("local", "bare", "empty")
        private val names = arrayOf<String?>("p:local", null, "")
        private val values = arrayOf("1", "2", "3")

        override val length: Int get() = 3
        override fun getURI(index: Int): String? = if (index >= 0 && index < 3) uris[index] else null
        override fun getLocalName(index: Int): String? = if (index >= 0 && index < 3) locals[index] else null
        override fun getQName(index: Int): String? = if (index >= 0 && index < 3) names[index] else null
        override fun getType(index: Int): String? = if (index >= 0 && index < 3) "CDATA" else null
        override fun getValue(index: Int): String? = if (index >= 0 && index < 3) values[index] else null
        override fun getIndex(uri: String, localName: String): Int {
            for (i in 0 until 3) if (uris[i] == uri && locals[i] == localName) return i
            return -1
        }
        override fun getIndex(qName: String): Int {
            for (i in 0 until 3) if (names[i] == qName) return i
            return -1
        }
        override fun getType(uri: String, localName: String): String? = getType(getIndex(uri, localName))
        override fun getType(qName: String): String? = getType(getIndex(qName))
        override fun getValue(uri: String, localName: String): String? = getValue(getIndex(uri, localName))
        override fun getValue(qName: String): String? = getValue(getIndex(qName))
    }

    private fun attrs(vararg namesAndValues: String): AttributesImpl {
        val attrs = AttributesImpl()
        var i = 0
        while (i < namesAndValues.size) {
            attrs.addAttribute("", namesAndValues[i], namesAndValues[i], "CDATA", namesAndValues[i + 1])
            i += 2
        }
        return attrs
    }

    /** `<a x="1"><b>t</b><c/></a>` as SAX events, with no namespaces. */
    private fun feedSimple(handler: ContentHandler) {
        handler.startDocument()
        handler.startElement("", "a", "a", attrs("x", "1"))
        handler.startElement("", "b", "b", attrs())
        handler.characters("t".toCharArray(), 0, 1)
        handler.endElement("", "b", "b")
        handler.startElement("", "c", "c", attrs())
        handler.endElement("", "c", "c")
        handler.endElement("", "a", "a")
        handler.endDocument()
    }

    /**
     * A tree with attributes (one with a quote), a processing instruction, text
     * with &lt;, &amp;, quotes and non-ASCII characters, an empty element and nesting.
     */
    private fun tree(): Document {
        val root = Element("root")
        root.setAttribute("a", "1")
        root.setAttribute("b", "x\"y")
        root.addContent(ProcessingInstruction("pi", "d"))
        root.addContent(Element("child").setText("t < & \"q\" " + UNICODE))
        root.addContent(Element("empty"))
        val n = Element("n")
        n.addContent(Element("m").setAttribute("k", "v").setText("deep"))
        root.addContent(n)
        return Document(root)
    }

    private fun serialize(handler: ContentHandler): String {
        JDOMSAXEventProvider(tree()).provideSAXEvents(handler)
        return handler.toString()
    }

    /**
     * Calls a public method with the given arguments, null included, through reflection, as a Java
     * caller would. Returns the exception the method threw, or null (Kotlin cannot pass null to a
     * non-null parameter directly, so the Java-ABI behaviour is reached this way).
     */
    private fun thrownByJavaCall(target: Any, name: String, vararg args: Any?): Throwable? {
        val method = target.javaClass.methods.single { it.name == name && it.parameterCount == args.size }
        return try {
            method.invoke(target, *args)
            null
        } catch (e: InvocationTargetException) {
            e.cause
        }
    }

    @Test
    fun testPrettyRequiresNonNullSaxNames() {
        val handler = PrettySerializingContentHandler()
        assertIs<NullPointerException>(thrownByJavaCall(handler, "startPrefixMapping", null, "u"))
        assertIs<NullPointerException>(thrownByJavaCall(handler, "startPrefixMapping", "p", null))
        assertIs<NullPointerException>(thrownByJavaCall(handler, "endPrefixMapping", null))
        assertIs<NullPointerException>(thrownByJavaCall(handler, "skippedEntity", null))
        // Locator remains optional in the target SAX contract.
        handler.setDocumentLocator(null)
        assertEquals("", handler.toString())
    }

    @Test
    fun testPrettyAsIs() {
        // Attribute values are escaped and single quoted; text is not escaped; the quote inside is not a problem
        assertEquals("<root a='1' b='x&quot;y'><!pi d!><child>t < & \"q\" " + UNICODE + "</child><empty></empty><n><m k='v'>deep</m></n></root>",
                serialize(PrettySerializingContentHandler()))
        assertEquals(serialize(PrettySerializingContentHandler()), serialize(PrettySerializingContentHandler(FormatType.AS_IS)))
    }

    @Test
    fun testPrettyFormats() {
        // Derived from the pending-end-tag state machine: ANALYSIS breaks before each '>' of a tag
        // end, CLASSIC breaks before each tag start and each end tag; _INDENT adds one tab per depth.
        var handler = PrettySerializingContentHandler(FormatType.AS_IS)
        feedSimple(handler)
        assertEquals("<a x='1'><b>t</b><c></c></a>", handler.toString())

        handler = PrettySerializingContentHandler(FormatType.ANALYSIS)
        feedSimple(handler)
        assertEquals("<a x='1'\n><b\n>t</b\n><c\n></c\n></a\n>", handler.toString())

        handler = PrettySerializingContentHandler(FormatType.ANALYSIS_INDENT)
        feedSimple(handler)
        assertEquals("<a x='1'\n\t><b\n\t\t>t</b\n\t><c\n></c\n\t></a\n>", handler.toString())

        handler = PrettySerializingContentHandler(FormatType.CLASSIC)
        feedSimple(handler)
        assertEquals("<a x='1'>\n<b>\nt\n</b>\n<c>\n</c>\n</a>", handler.toString())

        handler = PrettySerializingContentHandler(FormatType.CLASSIC_INDENT)
        feedSimple(handler)
        assertEquals("<a x='1'>\n\t<b>\n\t\tt\n\t</b>\n\t<c>\n\t</c>\n</a>", handler.toString())
    }

    @Test
    fun testPrettyCharactersAndProcessingInstructions() {
        // Adjacent characters() calls (entities in the source) are joined: only the first closes the tag
        var handler = PrettySerializingContentHandler(FormatType.ANALYSIS)
        handler.startElement("", "p", "p", attrs())
        handler.characters("a".toCharArray(), 0, 1)
        handler.characters("b".toCharArray(), 0, 1)
        handler.endElement("", "p", "p")
        assertEquals("<p\n>ab</p\n>", handler.toString())

        // ignorableWhitespace is treated as characters
        handler = PrettySerializingContentHandler(FormatType.AS_IS)
        handler.startElement("", "p", "p", attrs())
        handler.ignorableWhitespace(" \n".toCharArray(), 0, 2)
        handler.endElement("", "p", "p")
        assertEquals("<p> \n</p>", handler.toString())

        // A processing instruction: "<!target data!>", and a newline only when multiline
        handler = PrettySerializingContentHandler(FormatType.AS_IS)
        handler.processingInstruction("pi", "data")
        assertEquals("<!pi data!>", handler.toString())
        // (handlePending() also breaks the line before it in the classic formats)
        handler = PrettySerializingContentHandler(FormatType.CLASSIC)
        handler.processingInstruction("pi", "data")
        assertEquals("\n<!pi data!>\n", handler.toString())

        // An apostrophe in an attribute value is not escaped, so the single quoting breaks
        handler = PrettySerializingContentHandler()
        handler.startElement("", "p", "p", attrs("t", "it's <&>"))
        handler.endElement("", "p", "p")
        assertEquals("<p t='it's &lt;&amp;&gt;'></p>", handler.toString())

        // Pretty writes the local name (not the qualified name) of an element, the qualified name of an attribute
        handler = PrettySerializingContentHandler()
        val prefixed = AttributesImpl()
        prefixed.addAttribute("u", "al", "p:al", "CDATA", "1")
        handler.startElement("u", "local", "p:local", prefixed)
        handler.endElement("u", "local", "p:local")
        assertEquals("<local p:al='1'></local>", handler.toString())
    }

    @Test
    fun testPrettyWritesToTheGivenWriter() {
        val writer = StringWriter()
        var handler = PrettySerializingContentHandler(FormatType.AS_IS, writer)
        feedSimple(handler)
        assertEquals("<a x='1'><b>t</b><c></c></a>", writer.toString())
        assertEquals(writer.toString(), handler.toString())

        // A null writer means a private StringWriter
        handler = PrettySerializingContentHandler(FormatType.AS_IS, null)
        feedSimple(handler)
        assertEquals("<a x='1'><b>t</b><c></c></a>", handler.toString())
    }

    @Test
    fun testPrettyIndentIsLimitedToThirtyLevels() {
        val handler = IndentProbe()
        assertEquals("", handler.indent(0))
        assertEquals("\t\t\t", handler.indent(3))
        assertEquals(30, handler.indent(30).length)
        assertFailsWith<StringIndexOutOfBoundsException>("the tab table has 30 entries") {
            handler.indent(31)
        }
    }

    @Test
    fun testFormatType() {
        val expected: Array<Array<Any>> = arrayOf(
                // multiline, indented, analytic, classic
                arrayOf(FormatType.AS_IS, false, false, false, false),
                arrayOf(FormatType.ANALYSIS, true, false, true, false),
                arrayOf(FormatType.CLASSIC, true, false, false, true),
                arrayOf(FormatType.ANALYSIS_INDENT, true, true, true, false),
                arrayOf(FormatType.CLASSIC_INDENT, true, true, false, true),
        )
        assertEquals(expected.size, FormatType.values().size)
        for (row in expected) {
            val type = row[0] as FormatType
            assertEquals(row[1], type.isMultiline, type.name)
            assertEquals(row[2], type.isIndented, type.name)
            assertEquals(row[3], type.isAnalytic, type.name)
            assertEquals(row[4], type.isClassic, type.name)
        }
    }

    @Test
    fun testWriteToString() {
        // writeToString is a PrettySerializingContentHandler with AS_IS
        assertEquals(serialize(PrettySerializingContentHandler()), XMLUtil.writeToString(JDOMSAXEventProvider(tree())))
        assertEquals("<root a='1' b='x&quot;y'><!pi d!><child>t < & \"q\" " + UNICODE + "</child><empty></empty><n><m k='v'>deep</m></n></root>",
                XMLUtil.writeToString(JDOMSAXEventProvider(tree())))
    }

    @Test
    fun testAttributeName() {
        // Builders only accept nonnull names; exercise the nullable SAX interface contract directly.
        val attrs: fi.sykero.kdom.sax.Attributes = NullableQNameAttributes()
        assertEquals("p:local", XMLUtil.getAttributeName(attrs, 0))
        assertEquals("bare", XMLUtil.getAttributeName(attrs, 1))
        // Only null falls back; an empty qualified name is used as it is
        assertEquals("", XMLUtil.getAttributeName(attrs, 2))
        // Debug logging must cope with a missing qualified name
        XMLUtil.debugSAXAttributes(attrs)
    }

    @Test
    fun testEscape() {
        assertNull(XMLUtil.escape(null))
        assertEquals("", XMLUtil.escape(""))
        assertEquals("a&lt;b&amp;c&gt;&quot;d'", XMLUtil.escape("a<b&c>\"d'"))
        // Not recursive: an existing entity is escaped too
        assertEquals("&amp;amp;", XMLUtil.escape("&amp;"))
        assertEquals(UNICODE, XMLUtil.escape(UNICODE))
    }

    @Test
    fun testCleanAllEntities() {
        // &nbsp; becomes U+00A0, &amp; stays, unknown &foo; becomes a space, numeric references stay
        // (also the hex one with decimal digits), and the final bare & is escaped
        assertEquals("\u00A0&amp; &#65;&#x42;&amp;", XMLUtil.cleanAllEntities("&nbsp;&amp;&foo;&#65;&#x42;&"))
        // The numeric pattern only allows 2-4 decimal digits: letters in a hex reference are not accepted
        assertEquals("&amp;#xAB;", XMLUtil.cleanAllEntities("&#xAB;"))
        assertEquals("&amp;#1;", XMLUtil.cleanAllEntities("&#1;"))
    }

    @Test
    fun testCleanAllCharacters() {
        assertEquals("", XMLUtil.cleanAllCharacters(""))
        assertEquals("a\tb\r\nc" + UNICODE, XMLUtil.cleanAllCharacters("a\tb\r\nc" + UNICODE))
        assertEquals("a b c d", XMLUtil.cleanAllCharacters("a\u0001b\uFFFEc\uFFFFd"))
        // The range ends at U+FFFD
        assertEquals("\uD7FF\uE000\uFFFD", XMLUtil.cleanAllCharacters("\uD7FF\uE000\uFFFD"))
        assertEquals(" ", XMLUtil.cleanAllCharacters("\u0000"))
        assertEquals(" ", XMLUtil.cleanAllCharacters("\u0008"))
        // A supplementary character is one code point for the regex and not in the allowed set: one space
        assertEquals("a b", XMLUtil.cleanAllCharacters("a😀b"))
        // A lone surrogate is replaced too
        assertEquals("a b", XMLUtil.cleanAllCharacters("a\ud83db"))
        // expected NullPointerException (null argument, through reflection)
        assertIs<NullPointerException>(thrownByJavaCall(XMLUtil, "cleanAllCharacters", null))
    }

    @Test
    fun testRecloseTags() {
        assertEquals("<a><b></b></a>", XMLUtil.recloseTags("<a><b></a>"))
        // Closing tags at the end are stripped (with trailing whitespace) and re-created
        assertEquals("<a>x</a>", XMLUtil.recloseTags("<a>x</a>"))
        assertEquals("<a>x</a>", XMLUtil.recloseTags("<a>x</a> \n"))
        assertEquals("", XMLUtil.recloseTags(""))
        assertEquals("plain", XMLUtil.recloseTags("plain"))
        // Self-closing tags are not open (also with whitespace before the slash's '>')
        assertEquals("<a/>", XMLUtil.recloseTags("<a/>"))
        assertEquals("<a/><b></b>", XMLUtil.recloseTags("<a/><b>"))
        // Mismatched or unbalanced input cannot be repaired
        assertNull(XMLUtil.recloseTags("<a>x</b>y"))
        assertNull(XMLUtil.recloseTags("a</x>b"))
        assertNull(XMLUtil.recloseTags("<a"))
        // A stray </l> or </lg> gets its opening tag put in front, once
        assertEquals("<l>text</l>more", XMLUtil.recloseTags("text</l>more"))
        assertEquals("<lg></lg>z", XMLUtil.recloseTags("</lg>z"))
        assertNull(XMLUtil.recloseTags("</l>x</l>y"))
    }

    @Test
    fun testCloseEmptyTags() {
        assertNull(XMLUtil.closeEmptyTags(null))
        assertEquals("<br/>", XMLUtil.closeEmptyTags("<br>"))
        assertEquals("<br/>", XMLUtil.closeEmptyTags("<br/>"))
        assertEquals("<img src=\"a\"/>x<hr />", XMLUtil.closeEmptyTags("<img src=\"a\">x<hr >"))
        assertEquals("<p>x</p>", XMLUtil.closeEmptyTags("<p>x</p>"))
        // The name only has to start with one of the three
        assertEquals("<bra/>", XMLUtil.closeEmptyTags("<bra>"))
    }

    @Test
    fun testCleanAllTags() {
        assertNull(XMLUtil.cleanAllTags(null))
        // Each tag becomes one space; an unterminated tag runs to the end of the text
        assertEquals(" x  ", XMLUtil.cleanAllTags("<a>x</a><b"))
        assertEquals("plain", XMLUtil.cleanAllTags("plain"))
        // A tag ends at '>' or at the first later space after which the text since the FIRST space has no '='
        assertEquals(" x>y", XMLUtil.cleanAllTags("<a word x>y"))
        // ... and since that start is never moved on, one earlier attribute keeps the tag open until '>'
        assertEquals(" x", XMLUtil.cleanAllTags("<a b=\"1\" word y>x"))
    }

    @Test
    fun testGetDocumentPreservesDtdAugmentationAndContentOrder() {
        val doc = XMLUtil.getDocument("XMLSerializationAugmentedDoc")
        val root = doc.rootElement
        assertEquals("first", root.getAttributeValue("mode"))
        assertFalse(root.getAttribute("mode")!!.isSpecified)
        assertEquals(AttributeType.NMTOKEN, root.getAttribute("mode")!!.attributeType)
        val item = root.children[0]
        assertEquals("urn:test", item.namespaceURI)
        assertEquals("one", item.getAttributeValue("id", Namespace.getNamespace("p", "urn:test")))
        assertEquals("expanded", item.text)
        assertEquals("Text,Element,CDATA,Comment,ProcessingInstruction,Text",
                root.content.joinToString(",") { it.javaClass.simpleName })
        assertEquals("start", root.getContent(0).value)
        assertEquals("<raw>", root.getContent(2).value)
        assertEquals("end", root.getContent(5).value)
        assertEquals("doc", doc.docType!!.elementName)
        assertEquals("", doc.docType!!.internalSubset)
    }

    @Test
    fun testGetDocument() {
        // The resource is found next to XMLUtil (in its package), validated against its DTD
        val doc = XMLUtil.getDocument("XMLSerializationCharacterizationDoc")
        assertEquals("doc", doc.rootElement.name)
        assertEquals(2, doc.rootElement.getChildren("item").size)
        assertEquals("2", doc.rootElement.getChildren("item")[1].getAttributeValue("id"))
        assertEquals("one", doc.rootElement.getChildren("item")[0].text)

        assertFailsWith<JDOMException>("violates the DTD") {
            XMLUtil.getDocument("XMLSerializationCharacterizationInvalidDoc")
        }

        val e = assertFailsWith<MissingResourceException>("no such resource") {
            XMLUtil.getDocument("XMLSerializationCharacterizationMissing")
        }
        assertEquals("XMLSerializationCharacterizationMissing.xml", e.key)
    }

    private companion object {
        /** é and the euro sign, written as escapes so that the source stays ASCII. */
        const val UNICODE = "\u00E9\u20AC"
    }
}
