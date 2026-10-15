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
 * \u00A9 CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.xml;

import java.io.StringWriter;
import java.util.MissingResourceException;

import fi.sykero.kdom.Document;
import fi.sykero.kdom.Element;
import fi.sykero.kdom.JDOMException;
import fi.sykero.kdom.ProcessingInstruction;
import org.junit.Assert;
import org.junit.Test;
import fi.sykero.kdom.sax.ContentHandler;
import fi.sykero.kdom.sax.SAXException;
import fi.sykero.kdom.sax.AttributesImpl;

/**
 * Characterization of the SAX serializers, the SAX providers and {@link XMLUtil}:
 * pins the behaviour of the Java classes before they are converted to Kotlin.
 * Expected values were derived by reading the Java, not by observing it.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class XMLSerializationCharacterizationTest {
    /** \u00E9 and the euro sign, written as escapes so that the source stays ASCII. */
    private static final String UNICODE = "\u00E9\u20AC";

    private static AttributesImpl attrs(String... namesAndValues) {
        AttributesImpl attrs = new AttributesImpl();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            attrs.addAttribute("", namesAndValues[i], namesAndValues[i], "CDATA", namesAndValues[i + 1]);
        }
        return attrs;
    }

    /** {@code <a x="1"><b>t</b><c/></a>} as SAX events, with no namespaces. */
    private static void feedSimple(ContentHandler handler) throws SAXException {
        handler.startDocument();
        handler.startElement("", "a", "a", attrs("x", "1"));
        handler.startElement("", "b", "b", attrs());
        handler.characters("t".toCharArray(), 0, 1);
        handler.endElement("", "b", "b");
        handler.startElement("", "c", "c", attrs());
        handler.endElement("", "c", "c");
        handler.endElement("", "a", "a");
        handler.endDocument();
    }

    /**
     * A tree with attributes (one with a quote), a processing instruction, text
     * with &lt;, &amp;, quotes and non-ASCII characters, an empty element and nesting.
     */
    private static Document tree() {
        Element root = new Element("root");
        root.setAttribute("a", "1");
        root.setAttribute("b", "x\"y");
        root.addContent(new ProcessingInstruction("pi", "d"));
        root.addContent(new Element("child").setText("t < & \"q\" " + UNICODE));
        root.addContent(new Element("empty"));
        Element n = new Element("n");
        n.addContent(new Element("m").setAttribute("k", "v").setText("deep"));
        root.addContent(n);
        return new Document(root);
    }

    private static String serialize(ContentHandler handler) throws SAXException {
        new JDOMSAXEventProvider(tree()).provideSAXEvents(handler);
        return handler.toString();
    }

    @Test
    public void testPrettyRequiresNonNullSaxNames() {
        PrettySerializingContentHandler handler = new PrettySerializingContentHandler();
        Assert.assertThrows(NullPointerException.class, () -> handler.startPrefixMapping(null, "u"));
        Assert.assertThrows(NullPointerException.class, () -> handler.startPrefixMapping("p", null));
        Assert.assertThrows(NullPointerException.class, () -> handler.endPrefixMapping(null));
        Assert.assertThrows(NullPointerException.class, () -> handler.skippedEntity(null));
        // Locator remains optional in the target SAX contract.
        handler.setDocumentLocator(null);
        Assert.assertEquals("", handler.toString());
    }

    @Test
    public void testPrettyAsIs() throws SAXException {
        // Attribute values are escaped and single quoted; text is not escaped; the quote inside is not a problem
        Assert.assertEquals("<root a='1' b='x&quot;y'><!pi d!><child>t < & \"q\" " + UNICODE + "</child><empty></empty><n><m k='v'>deep</m></n></root>",
                serialize(new PrettySerializingContentHandler()));
        Assert.assertEquals(serialize(new PrettySerializingContentHandler()), serialize(new PrettySerializingContentHandler(FormatType.AS_IS)));
    }

    @Test
    public void testPrettyFormats() throws SAXException {
        // Derived from the pending-end-tag state machine: ANALYSIS breaks before each '>' of a tag
        // end, CLASSIC breaks before each tag start and each end tag; _INDENT adds one tab per depth.
        PrettySerializingContentHandler handler = new PrettySerializingContentHandler(FormatType.AS_IS);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'><b>t</b><c></c></a>", handler.toString());

        handler = new PrettySerializingContentHandler(FormatType.ANALYSIS);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'\n><b\n>t</b\n><c\n></c\n></a\n>", handler.toString());

        handler = new PrettySerializingContentHandler(FormatType.ANALYSIS_INDENT);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'\n\t><b\n\t\t>t</b\n\t><c\n></c\n\t></a\n>", handler.toString());

        handler = new PrettySerializingContentHandler(FormatType.CLASSIC);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'>\n<b>\nt\n</b>\n<c>\n</c>\n</a>", handler.toString());

        handler = new PrettySerializingContentHandler(FormatType.CLASSIC_INDENT);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'>\n\t<b>\n\t\tt\n\t</b>\n\t<c>\n\t</c>\n</a>", handler.toString());
    }

    @Test
    public void testPrettyCharactersAndProcessingInstructions() throws SAXException {
        // Adjacent characters() calls (entities in the source) are joined: only the first closes the tag
        PrettySerializingContentHandler handler = new PrettySerializingContentHandler(FormatType.ANALYSIS);
        handler.startElement("", "p", "p", attrs());
        handler.characters("a".toCharArray(), 0, 1);
        handler.characters("b".toCharArray(), 0, 1);
        handler.endElement("", "p", "p");
        Assert.assertEquals("<p\n>ab</p\n>", handler.toString());

        // ignorableWhitespace is treated as characters
        handler = new PrettySerializingContentHandler(FormatType.AS_IS);
        handler.startElement("", "p", "p", attrs());
        handler.ignorableWhitespace(" \n".toCharArray(), 0, 2);
        handler.endElement("", "p", "p");
        Assert.assertEquals("<p> \n</p>", handler.toString());

        // A processing instruction: "<!target data!>", and a newline only when multiline
        handler = new PrettySerializingContentHandler(FormatType.AS_IS);
        handler.processingInstruction("pi", "data");
        Assert.assertEquals("<!pi data!>", handler.toString());
        // (handlePending() also breaks the line before it in the classic formats)
        handler = new PrettySerializingContentHandler(FormatType.CLASSIC);
        handler.processingInstruction("pi", "data");
        Assert.assertEquals("\n<!pi data!>\n", handler.toString());

        // An apostrophe in an attribute value is not escaped, so the single quoting breaks
        handler = new PrettySerializingContentHandler();
        handler.startElement("", "p", "p", attrs("t", "it's <&>"));
        handler.endElement("", "p", "p");
        Assert.assertEquals("<p t='it's &lt;&amp;&gt;'></p>", handler.toString());

        // Pretty writes the local name (not the qualified name) of an element, the qualified name of an attribute
        handler = new PrettySerializingContentHandler();
        AttributesImpl prefixed = new AttributesImpl();
        prefixed.addAttribute("u", "al", "p:al", "CDATA", "1");
        handler.startElement("u", "local", "p:local", prefixed);
        handler.endElement("u", "local", "p:local");
        Assert.assertEquals("<local p:al='1'></local>", handler.toString());
    }

    @Test
    public void testPrettyWritesToTheGivenWriter() throws SAXException {
        StringWriter writer = new StringWriter();
        PrettySerializingContentHandler handler = new PrettySerializingContentHandler(FormatType.AS_IS, writer);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'><b>t</b><c></c></a>", writer.toString());
        Assert.assertEquals(writer.toString(), handler.toString());

        // A null writer means a private StringWriter
        handler = new PrettySerializingContentHandler(FormatType.AS_IS, null);
        feedSimple(handler);
        Assert.assertEquals("<a x='1'><b>t</b><c></c></a>", handler.toString());
    }

    @Test
    public void testPrettyIndentIsLimitedToThirtyLevels() {
        PrettySerializingContentHandler handler = new PrettySerializingContentHandler(FormatType.CLASSIC_INDENT);
        Assert.assertEquals("", handler.decorateIndent(0));
        Assert.assertEquals("\t\t\t", handler.decorateIndent(3));
        Assert.assertEquals(30, handler.decorateIndent(30).length());
        try {
            handler.decorateIndent(31);
            Assert.fail("the tab table has 30 entries");
        } catch (StringIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testFormatType() {
        Object[][] expected = {
                // multiline, indented, analytic, classic
                {FormatType.AS_IS, false, false, false, false},
                {FormatType.ANALYSIS, true, false, true, false},
                {FormatType.CLASSIC, true, false, false, true},
                {FormatType.ANALYSIS_INDENT, true, true, true, false},
                {FormatType.CLASSIC_INDENT, true, true, false, true},
        };
        Assert.assertEquals(expected.length, FormatType.values().length);
        for (Object[] row : expected) {
            FormatType type = (FormatType) row[0];
            Assert.assertEquals(type.name(), row[1], type.isMultiline());
            Assert.assertEquals(type.name(), row[2], type.isIndented());
            Assert.assertEquals(type.name(), row[3], type.isAnalytic());
            Assert.assertEquals(type.name(), row[4], type.isClassic());
        }
    }

    @Test
    public void testWriteToString() throws SAXException {
        // writeToString is a PrettySerializingContentHandler with AS_IS
        Assert.assertEquals(serialize(new PrettySerializingContentHandler()), XMLUtil.writeToString(new JDOMSAXEventProvider(tree())));
        Assert.assertEquals("<root a='1' b='x&quot;y'><!pi d!><child>t < & \"q\" " + UNICODE + "</child><empty></empty><n><m k='v'>deep</m></n></root>",
                XMLUtil.writeToString(new JDOMSAXEventProvider(tree())));
    }

    private static final class NullableQNameAttributes implements fi.sykero.kdom.sax.Attributes {
        private final String[] uris = {"u", "", ""};
        private final String[] locals = {"local", "bare", "empty"};
        private final String[] names = {"p:local", null, ""};
        private final String[] values = {"1", "2", "3"};
        public int getLength() { return 3; }
        public String getURI(int i) { return i >= 0 && i < 3 ? uris[i] : null; }
        public String getLocalName(int i) { return i >= 0 && i < 3 ? locals[i] : null; }
        public String getQName(int i) { return i >= 0 && i < 3 ? names[i] : null; }
        public String getType(int i) { return i >= 0 && i < 3 ? "CDATA" : null; }
        public String getValue(int i) { return i >= 0 && i < 3 ? values[i] : null; }
        public int getIndex(String uri, String local) {
            for (int i = 0; i < 3; i++) if (uris[i].equals(uri) && locals[i].equals(local)) return i;
            return -1;
        }
        public int getIndex(String name) {
            for (int i = 0; i < 3; i++) if (java.util.Objects.equals(names[i], name)) return i;
            return -1;
        }
        public String getType(String uri, String local) { return getType(getIndex(uri, local)); }
        public String getType(String name) { return getType(getIndex(name)); }
        public String getValue(String uri, String local) { return getValue(getIndex(uri, local)); }
        public String getValue(String name) { return getValue(getIndex(name)); }
    }

    @Test
    public void testAttributeName() {
        // Builders only accept nonnull names; exercise the nullable SAX interface contract directly.
        fi.sykero.kdom.sax.Attributes attrs = new NullableQNameAttributes();
        Assert.assertEquals("p:local", XMLUtil.getAttributeName(attrs, 0));
        Assert.assertEquals("bare", XMLUtil.getAttributeName(attrs, 1));
        // Only null falls back; an empty qualified name is used as it is
        Assert.assertEquals("", XMLUtil.getAttributeName(attrs, 2));
        // Debug logging must cope with a missing qualified name
        XMLUtil.debugSAXAttributes(attrs);
    }

    @Test
    public void testEscape() {
        Assert.assertNull(XMLUtil.escape(null));
        Assert.assertEquals("", XMLUtil.escape(""));
        Assert.assertEquals("a&lt;b&amp;c&gt;&quot;d'", XMLUtil.escape("a<b&c>\"d'"));
        // Not recursive: an existing entity is escaped too
        Assert.assertEquals("&amp;amp;", XMLUtil.escape("&amp;"));
        Assert.assertEquals(UNICODE, XMLUtil.escape(UNICODE));
    }

    @Test
    public void testCleanAllEntities() {
        // &nbsp; becomes U+00A0, &amp; stays, unknown &foo; becomes a space, numeric references stay
        // (also the hex one with decimal digits), and the final bare & is escaped
        Assert.assertEquals("\u00A0&amp; &#65;&#x42;&amp;", XMLUtil.cleanAllEntities("&nbsp;&amp;&foo;&#65;&#x42;&"));
        // The numeric pattern only allows 2-4 decimal digits: letters in a hex reference are not accepted
        Assert.assertEquals("&amp;#xAB;", XMLUtil.cleanAllEntities("&#xAB;"));
        Assert.assertEquals("&amp;#1;", XMLUtil.cleanAllEntities("&#1;"));
    }

    @Test
    public void testCleanAllCharacters() {
        Assert.assertEquals("", XMLUtil.cleanAllCharacters(""));
        Assert.assertEquals("a\tb\r\nc" + UNICODE, XMLUtil.cleanAllCharacters("a\tb\r\nc" + UNICODE));
        Assert.assertEquals("a b c d", XMLUtil.cleanAllCharacters("a\u0001b\uFFFEc\uFFFFd"));
        // The range ends at U+FFFD
        Assert.assertEquals("\uD7FF\uE000\uFFFD", XMLUtil.cleanAllCharacters("\uD7FF\uE000\uFFFD"));
        Assert.assertEquals(" ", XMLUtil.cleanAllCharacters("\u0000"));
        Assert.assertEquals(" ", XMLUtil.cleanAllCharacters("\u0008"));
        // A supplementary character is one code point for the regex and not in the allowed set: one space
        Assert.assertEquals("a b", XMLUtil.cleanAllCharacters("a\uD83D\uDE00b"));
        // A lone surrogate is replaced too
        Assert.assertEquals("a b", XMLUtil.cleanAllCharacters("a\ud83db"));
        try {
            XMLUtil.cleanAllCharacters(null);
            Assert.fail("null input");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testRecloseTags() {
        Assert.assertEquals("<a><b></b></a>", XMLUtil.recloseTags("<a><b></a>"));
        // Closing tags at the end are stripped (with trailing whitespace) and re-created
        Assert.assertEquals("<a>x</a>", XMLUtil.recloseTags("<a>x</a>"));
        Assert.assertEquals("<a>x</a>", XMLUtil.recloseTags("<a>x</a> \n"));
        Assert.assertEquals("", XMLUtil.recloseTags(""));
        Assert.assertEquals("plain", XMLUtil.recloseTags("plain"));
        // Self-closing tags are not open (also with whitespace before the slash's '>')
        Assert.assertEquals("<a/>", XMLUtil.recloseTags("<a/>"));
        Assert.assertEquals("<a/><b></b>", XMLUtil.recloseTags("<a/><b>"));
        // Mismatched or unbalanced input cannot be repaired
        Assert.assertNull(XMLUtil.recloseTags("<a>x</b>y"));
        Assert.assertNull(XMLUtil.recloseTags("a</x>b"));
        Assert.assertNull(XMLUtil.recloseTags("<a"));
        // A stray </l> or </lg> gets its opening tag put in front, once
        Assert.assertEquals("<l>text</l>more", XMLUtil.recloseTags("text</l>more"));
        Assert.assertEquals("<lg></lg>z", XMLUtil.recloseTags("</lg>z"));
        Assert.assertNull(XMLUtil.recloseTags("</l>x</l>y"));
    }

    @Test
    public void testCloseEmptyTags() {
        Assert.assertNull(XMLUtil.closeEmptyTags(null));
        Assert.assertEquals("<br/>", XMLUtil.closeEmptyTags("<br>"));
        Assert.assertEquals("<br/>", XMLUtil.closeEmptyTags("<br/>"));
        Assert.assertEquals("<img src=\"a\"/>x<hr />", XMLUtil.closeEmptyTags("<img src=\"a\">x<hr >"));
        Assert.assertEquals("<p>x</p>", XMLUtil.closeEmptyTags("<p>x</p>"));
        // The name only has to start with one of the three
        Assert.assertEquals("<bra/>", XMLUtil.closeEmptyTags("<bra>"));
    }

    @Test
    public void testCleanAllTags() {
        Assert.assertNull(XMLUtil.cleanAllTags(null));
        // Each tag becomes one space; an unterminated tag runs to the end of the text
        Assert.assertEquals(" x  ", XMLUtil.cleanAllTags("<a>x</a><b"));
        Assert.assertEquals("plain", XMLUtil.cleanAllTags("plain"));
        // A tag ends at '>' or at the first later space after which the text since the FIRST space has no '='
        Assert.assertEquals(" x>y", XMLUtil.cleanAllTags("<a word x>y"));
        // ... and since that start is never moved on, one earlier attribute keeps the tag open until '>'
        Assert.assertEquals(" x", XMLUtil.cleanAllTags("<a b=\"1\" word y>x"));
    }

    @Test
    public void testGetDocumentPreservesDtdAugmentationAndContentOrder() throws Exception {
        Document doc = XMLUtil.getDocument("XMLSerializationAugmentedDoc");
        Element root = doc.getRootElement();
        Assert.assertEquals("first", root.getAttributeValue("mode"));
        Assert.assertFalse(root.getAttribute("mode").isSpecified());
        Assert.assertEquals(fi.sykero.kdom.AttributeType.NMTOKEN, root.getAttribute("mode").getAttributeType());
        Element item = root.getChildren().get(0);
        Assert.assertEquals("urn:test", item.getNamespaceURI());
        Assert.assertEquals("one", item.getAttributeValue("id", fi.sykero.kdom.Namespace.getNamespace("p", "urn:test")));
        Assert.assertEquals("expanded", item.getText());
        Assert.assertEquals("Text,Element,CDATA,Comment,ProcessingInstruction,Text", root.getContent().stream()
                .map(node -> node.getClass().getSimpleName()).collect(java.util.stream.Collectors.joining(",")));
        Assert.assertEquals("start", root.getContent(0).getValue());
        Assert.assertEquals("<raw>", root.getContent(2).getValue());
        Assert.assertEquals("end", root.getContent(5).getValue());
        Assert.assertEquals("doc", doc.getDocType().getElementName());
        Assert.assertEquals("", doc.getDocType().getInternalSubset());
    }

    @Test
    public void testGetDocument() throws Exception {
        // The resource is found next to XMLUtil (in its package), validated against its DTD
        Document doc = XMLUtil.getDocument("XMLSerializationCharacterizationDoc");
        Assert.assertEquals("doc", doc.getRootElement().getName());
        Assert.assertEquals(2, doc.getRootElement().getChildren("item").size());
        Assert.assertEquals("2", doc.getRootElement().getChildren("item").get(1).getAttributeValue("id"));
        Assert.assertEquals("one", doc.getRootElement().getChildren("item").get(0).getText());

        try {
            XMLUtil.getDocument("XMLSerializationCharacterizationInvalidDoc");
            Assert.fail("violates the DTD");
        } catch (JDOMException e) {
            // expected
        }

        try {
            XMLUtil.getDocument("XMLSerializationCharacterizationMissing");
            Assert.fail("no such resource");
        } catch (MissingResourceException e) {
            Assert.assertEquals("XMLSerializationCharacterizationMissing.xml", e.getKey());
        }
    }

}
