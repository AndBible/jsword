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
 * © CrossWire Bible Society, 2026
 *
 */
package org.crosswire.common.xml;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.ProcessingInstruction;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Characterization of the SAX serializers, the SAX providers and {@link XMLUtil}:
 * pins the behaviour of the Java classes before they are converted to Kotlin.
 * Expected values were derived by reading the Java, not by observing it.
 *
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 */
public class XMLSerializationCharacterizationTest {
    /** é and the euro sign, written as escapes so that the source stays ASCII. */
    private static final String UNICODE = "é€";

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
    public void testSerializingContentHandler() throws SAXException {
        // Attribute values and text are written as they are: no escaping at all
        Assert.assertEquals("<root a=\"1\" b=\"x\"y\"><!pi d!><child>t < & \"q\" " + UNICODE + "</child><empty></empty><n><m k=\"v\">deep</m></n></root>",
                serialize(new SerializingContentHandler(false)));
        Assert.assertEquals(serialize(new SerializingContentHandler(false)), serialize(new SerializingContentHandler()));

        // With newlines: one after the document start, each start tag, each end tag and each PI, none after text
        Assert.assertEquals("\n<root a=\"1\" b=\"x\"y\">\n<!pi d!>\n<child>\nt < & \"q\" " + UNICODE + "</child>\n<empty>\n</empty>\n<n>\n<m k=\"v\">\ndeep</m>\n</n>\n</root>\n",
                serialize(new SerializingContentHandler(true)));
    }

    @Test
    public void testSerializingContentHandlerDirectEvents() throws SAXException {
        SerializingContentHandler handler = new SerializingContentHandler();
        Assert.assertEquals("", handler.toString());

        // characters() and ignorableWhitespace() take (start, length) of the array
        handler.characters("xxabcxx".toCharArray(), 2, 3);
        handler.ignorableWhitespace("12  34".toCharArray(), 2, 2);
        handler.characters("zz".toCharArray(), 1, 0);
        Assert.assertEquals("abc  ", handler.toString());

        // A missing qualified name falls back to the local name, for elements and attributes
        AttributesImpl noQName = new AttributesImpl();
        noQName.addAttribute("", "loc", null, "CDATA", "v");
        handler.startElement("", "local", null, noQName);
        handler.endElement("", "local", null);
        Assert.assertEquals("abc  <local loc=\"v\"></local>", handler.toString());

        // Ignored events
        handler.startPrefixMapping("p", "u");
        handler.endPrefixMapping("p");
        handler.skippedEntity("e");
        handler.setDocumentLocator(null);
        Assert.assertEquals("abc  <local loc=\"v\"></local>", handler.toString());
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
    public void testHTMLSerializingContentHandler() throws SAXException {
        HTMLSerializingContentHandler handler = new HTMLSerializingContentHandler();
        String prologStart = "<html><head><style type='text/css'>\nFONT.tag    {";
        handler.startDocument();
        Assert.assertTrue(handler.toString(), handler.toString().startsWith(prologStart));
        Assert.assertTrue(handler.toString(), handler.toString().endsWith("FONT.text   { font-family:courier new, monospaced; background:#FFFF99; }\n</style></head><body>\n"));
        String prolog = handler.toString();

        // The markup characters of the tags are escaped; names, values and text are wrapped in fonts
        handler.startElement("", "p", "p", attrs("id", "1"));
        handler.characters("a&b".toCharArray(), 0, 3);
        handler.endElement("", "p", "p");
        handler.endDocument();
        Assert.assertEquals(prolog
                + "&lt;<font class='tag'>p</font> <font class='attr'>id</font>='<font class='value'>1</font>'&gt;"
                + "<font class='text'>a&amp;b</font>"
                + "&lt;/<font class='tag'>p</font>&gt;"
                + "</body></head>", handler.toString());

        // Newlines in text become <br>
        handler = new HTMLSerializingContentHandler(FormatType.AS_IS);
        handler.startElement("", "p", "p", attrs());
        handler.characters("a\nb".toCharArray(), 0, 3);
        Assert.assertEquals("&lt;<font class='tag'>p</font>&gt;<font class='text'>a<br>b</font>", handler.toString());
    }

    @Test
    public void testHTMLSerializingContentHandlerFormatted() throws SAXException {
        String tagA = "<font class='tag'>a</font>";
        String tagB = "<font class='tag'>b</font>";
        String indent1 = "<font class='indent'>&nbsp;&nbsp;&nbsp;&nbsp;</font>";
        String indent0 = "<font class='indent'></font>";

        StringWriter writer = new StringWriter();
        HTMLSerializingContentHandler handler = new HTMLSerializingContentHandler(FormatType.CLASSIC_INDENT, writer);
        handler.startElement("", "a", "a", attrs());
        handler.startElement("", "b", "b", attrs());
        handler.endElement("", "b", "b");
        handler.endElement("", "a", "a");
        // The newline is <br> and the tabs of the indent become four non-breaking spaces each
        Assert.assertEquals("&lt;" + tagA + "&gt;<br>" + indent1 + "&lt;" + tagB + "&gt;<br>" + indent1 + "&lt;/" + tagB + "&gt;<br>" + indent0 + "&lt;/" + tagA + "&gt;",
                writer.toString());

        handler = new HTMLSerializingContentHandler(FormatType.CLASSIC);
        handler.processingInstruction("pi", "x<y");
        Assert.assertEquals("<br>&lt;!pi <font class='text'>x&lt;y</font>!&gt;<br>", handler.toString());
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

    @Test
    public void testStringSAXEventProvider() throws Exception {
        SerializingContentHandler handler = new SerializingContentHandler();
        // The entity is decoded by the parser and written back raw; the empty element gets an end tag
        new StringSAXEventProvider("<r a='1'><x>t&amp;u</x><e/></r>").provideSAXEvents(handler);
        Assert.assertEquals("<r a=\"1\"><x>t&u</x><e></e></r>", handler.toString());

        // The parse happens when events are requested, not when the provider is built
        StringSAXEventProvider broken = new StringSAXEventProvider("<r><x></r>");
        try {
            broken.provideSAXEvents(new SerializingContentHandler());
            Assert.fail("not well formed");
        } catch (SAXParseException e) {
            // expected
        }
    }

    @Test
    public void testSAXEventProviderXMLReader() throws Exception {
        Element root = new Element("r");
        root.addContent(new Element("x"));
        SAXEventProviderXMLReader reader = new SAXEventProviderXMLReader(new JDOMSAXEventProvider(new Document(root)));

        SerializingContentHandler handler = new SerializingContentHandler();
        Assert.assertNull(reader.getContentHandler());
        reader.setContentHandler(handler);
        Assert.assertSame(handler, reader.getContentHandler());
        reader.parse(new SAXEventProviderInputSource());
        Assert.assertEquals("<r><x></x></r>", handler.toString());

        // Anything else than the dedicated input source is refused
        try {
            reader.parse(new InputSource());
            Assert.fail("plain input source");
        } catch (SAXException e) {
            Assert.assertEquals("SAXEventProviderInputSource required", e.getMessage());
        }
        try {
            reader.parse("file:///some.xml");
            Assert.fail("system id");
        } catch (SAXException e) {
            Assert.assertEquals("SAXEventProviderInputSource required", e.getMessage());
        }

        // Features and properties are not supported, the handlers are only stored
        Assert.assertFalse(reader.getFeature("anything"));
        reader.setFeature("anything", true);
        Assert.assertFalse(reader.getFeature("anything"));
        Assert.assertNull(reader.getProperty("anything"));
        reader.setProperty("anything", "x");
        Assert.assertNull(reader.getProperty("anything"));
        Assert.assertNull(reader.getErrorHandler());
        Assert.assertNull(reader.getDTDHandler());
        Assert.assertNull(reader.getEntityResolver());
        XMLHandlerAdapter adapter = new XMLHandlerAdapter();
        reader.setErrorHandler(adapter);
        reader.setDTDHandler(adapter);
        reader.setEntityResolver(adapter);
        Assert.assertSame(adapter, reader.getErrorHandler());
        Assert.assertSame(adapter, reader.getDTDHandler());
        Assert.assertSame(adapter, reader.getEntityResolver());
    }

    @Test
    public void testAttributeName() {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("u", "local", "p:local", "CDATA", "1");
        attrs.addAttribute("", "bare", null, "CDATA", "2");
        attrs.addAttribute("", "empty", "", "CDATA", "3");
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
        Assert.assertEquals(" &amp; &#65;&#x42;&amp;", XMLUtil.cleanAllEntities("&nbsp;&amp;&foo;&#65;&#x42;&"));
        // The numeric pattern only allows 2-4 decimal digits: letters in a hex reference are not accepted
        Assert.assertEquals("&amp;#xAB;", XMLUtil.cleanAllEntities("&#xAB;"));
        Assert.assertEquals("&amp;#1;", XMLUtil.cleanAllEntities("&#1;"));
    }

    @Test
    public void testCleanAllCharacters() {
        Assert.assertEquals("", XMLUtil.cleanAllCharacters(""));
        Assert.assertEquals("a\tb\r\nc" + UNICODE, XMLUtil.cleanAllCharacters("a\tb\r\nc" + UNICODE));
        Assert.assertEquals("a b c d", XMLUtil.cleanAllCharacters("a\u0001b￾c￿d"));
        // The range ends at U+FFFD
        Assert.assertEquals("퟿�", XMLUtil.cleanAllCharacters("퟿�"));
        Assert.assertEquals(" ", XMLUtil.cleanAllCharacters("\u0000"));
        Assert.assertEquals(" ", XMLUtil.cleanAllCharacters("\u0008"));
        // A supplementary character is one code point for the regex and not in the allowed set: one space
        Assert.assertEquals("a b", XMLUtil.cleanAllCharacters("a😀b"));
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

    @Test
    public void testRedLetterText() {
        RedLetterText rlt = new RedLetterText();
        Assert.assertFalse(rlt.isRLT());
        // Leaving below zero does nothing
        rlt.leave();
        Assert.assertFalse(rlt.isRLT());
        rlt.enter();
        rlt.enter();
        Assert.assertTrue(rlt.isRLT());
        rlt.leave();
        Assert.assertTrue(rlt.isRLT());
        rlt.leave();
        Assert.assertFalse(rlt.isRLT());
        rlt.leave();
        rlt.enter();
        Assert.assertTrue(rlt.isRLT());
    }

    @Test
    public void testXMLFeature() {
        Assert.assertEquals(11, XMLFeature.values().length);
        Assert.assertEquals("http://xml.org/sax/features/validation", XMLFeature.VALIDATION.getControl());
        Assert.assertFalse(XMLFeature.VALIDATION.getState());
        Assert.assertTrue(XMLFeature.XINCLUDE_FIXUP_BASE_URIS.getState());
        Assert.assertTrue(XMLFeature.XINCLUDE_FIXUP_LANGUAGE.getState());
        Assert.assertFalse(XMLFeature.NAMESPACES.getState());
        Assert.assertEquals("off http://xml.org/sax/features/validation", XMLFeature.VALIDATION.toString());
        Assert.assertEquals("on  http://apache.org/xml/features/xinclude/fixup-language", XMLFeature.XINCLUDE_FIXUP_LANGUAGE.toString());

        // The lookup is by control string, ignoring case
        Assert.assertSame(XMLFeature.SCHEMA_VALIDATION, XMLFeature.fromString("http://apache.org/xml/features/validation/schema"));
        Assert.assertSame(XMLFeature.NAMESPACE_PREFIX, XMLFeature.fromString("HTTP://XML.ORG/SAX/FEATURES/NAMESPACE-PREFIXES"));
    }

    /** An XMLReader that records the features set on it, and rejects one of them. */
    private static XMLReader recordingReader(final List<String> calls, final String rejected) {
        return (XMLReader) Proxy.newProxyInstance(XMLSerializationCharacterizationTest.class.getClassLoader(), new Class<?>[] {XMLReader.class},
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("setFeature".equals(method.getName())) {
                            if (args[0].equals(rejected)) {
                                throw new SAXNotRecognizedException("no " + rejected);
                            }
                            calls.add(args[0] + "=" + args[1]);
                        }
                        return null;
                    }
                });
    }

    @Test
    public void testXMLFeatureSet() {
        XMLFeatureSet set = new XMLFeatureSet();
        // The features are kept ordered by their short option name: dv f n np s v va xb xd xi xl.
        // Defaults: n, xd, xb and xl on, the others off.
        List<String> calls = new ArrayList<String>();
        set.setFeatures(recordingReader(calls, null));
        Assert.assertEquals("[http://apache.org/xml/features/validation/dynamic=false, "
                + "http://apache.org/xml/features/validation/schema-full-checking=false, "
                + "http://xml.org/sax/features/namespaces=true, "
                + "http://xml.org/sax/features/namespace-prefixes=false, "
                + "http://apache.org/xml/features/validation/schema=false, "
                + "http://xml.org/sax/features/validation=false, "
                + "http://apache.org/xml/features/validate-annotations=false, "
                + "http://apache.org/xml/features/xinclude/fixup-base-uris=true, "
                + "http://apache.org/xml/features/nonvalidating/load-external-dtd=true, "
                + "http://apache.org/xml/features/xinclude=false, "
                + "http://apache.org/xml/features/xinclude/fixup-language=true]", calls.toString());

        // Upper case turns an option off, lower case on; unknown options and non-options are ignored
        set.setFeatureStates(new String[] {"-XD", "-v", "junk", "-zz", "-S", "-xi"});
        set.setFeatureState(XMLFeature.NAMESPACES, false);
        calls.clear();
        set.setFeatures(recordingReader(calls, null));
        Assert.assertTrue(calls.toString(), calls.contains("http://apache.org/xml/features/nonvalidating/load-external-dtd=false"));
        Assert.assertTrue(calls.toString(), calls.contains("http://xml.org/sax/features/validation=true"));
        Assert.assertTrue(calls.toString(), calls.contains("http://apache.org/xml/features/validation/schema=false"));
        Assert.assertTrue(calls.toString(), calls.contains("http://apache.org/xml/features/xinclude=true"));
        Assert.assertTrue(calls.toString(), calls.contains("http://xml.org/sax/features/namespaces=false"));
        Assert.assertEquals(11, calls.size());

        // toString lists the features' own initial states, not the set's: it never changes
        StringBuilder expected = new StringBuilder("\n");
        String[] lines = {"off http://apache.org/xml/features/validation/dynamic", "off http://apache.org/xml/features/validation/schema-full-checking",
                "off http://xml.org/sax/features/namespaces", "off http://xml.org/sax/features/namespace-prefixes",
                "off http://apache.org/xml/features/validation/schema", "off http://xml.org/sax/features/validation",
                "off http://apache.org/xml/features/validate-annotations", "on  http://apache.org/xml/features/xinclude/fixup-base-uris",
                "off http://apache.org/xml/features/nonvalidating/load-external-dtd", "off http://apache.org/xml/features/xinclude",
                "on  http://apache.org/xml/features/xinclude/fixup-language"};
        for (String line : lines) {
            expected.append(line).append('\n');
        }
        Assert.assertEquals(expected.toString(), set.toString());
        Assert.assertEquals(expected.toString(), new XMLFeatureSet().toString());

        // A parser that does not know a feature only gets a warning on System.err
        PrintStream oldErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
        try {
            calls.clear();
            set.setFeatures(recordingReader(calls, "http://xml.org/sax/features/validation"));
        } finally {
            System.setErr(oldErr);
        }
        Assert.assertEquals(10, calls.size());
        Assert.assertEquals("warning: Parser does not recognize feature (http://xml.org/sax/features/validation)" + System.lineSeparator(), captured.toString());
    }

    @Test
    public void testXMLHandlerAdapterReportsErrorsOnStdErr() throws Exception {
        XMLHandlerAdapter adapter = new XMLHandlerAdapter();
        // The other callbacks do nothing and resolveEntity declines
        Assert.assertNull(adapter.resolveEntity("p", "s"));
        adapter.startDocument();
        adapter.characters(new char[0], 0, 0);
        adapter.endDocument();

        PrintStream oldErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
        try {
            adapter.warning(new SAXParseException("careful", null, "file:///a/b/c.xml", 3, 4));
            adapter.error(new SAXParseException("oops", null, null, 5, 6));
            adapter.fatalError(new SAXParseException("dead", null, "plain.xml", -1, -1));
        } finally {
            System.setErr(oldErr);
        }
        String nl = System.lineSeparator();
        // The system id is cut to the part after its last slash; a missing one is left out
        Assert.assertEquals("[Warning] c.xml:3:4: careful" + nl + "[Error] :5:6: oops" + nl + "[Fatal Error] plain.xml:-1:-1: dead" + nl, captured.toString());
    }
}
