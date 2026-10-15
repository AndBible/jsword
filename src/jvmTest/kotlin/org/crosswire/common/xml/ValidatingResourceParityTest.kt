package org.crosswire.common.xml

import kotlin.test.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

/** Local-only comparison of the legacy validating loader's observable tree and failures. */
class ValidatingResourceParityTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun compare(xml: String) {
        val file = tmp.newFile("input-${System.nanoTime()}.xml").apply { writeText(xml) }
        val old = runCatching { org.jdom2.input.SAXBuilder(org.jdom2.input.sax.XMLReaders.DTDVALIDATING).build(file.inputStream()) }
        // Use the same caller-relative resource loader as production, via a disposable classloader URL.
        val loader = Thread.currentThread().contextClassLoader
        val urlLoader = java.net.URLClassLoader(arrayOf(tmp.root.toURI().toURL()), loader)
        Thread.currentThread().contextClassLoader = urlLoader
        try {
            val actual = runCatching { XMLUtil.getDocument(file.nameWithoutExtension) }
            if (old.isSuccess) {
                val expected = old.getOrThrow()
                val current = actual.getOrThrow()
                assertEquals(org.jdom2.output.XMLOutputter().outputString(expected), fi.sykero.kdom.output.XMLOutputter().outputString(current))
                assertEquals(expected.rootElement.additionalNamespaces.map { it.prefix to it.uri }, current.rootElement.additionalNamespaces.map { it.prefix to it.uri })
                assertEquals(expected.rootElement.content.map { it.javaClass.simpleName to it.value }, current.rootElement.content.map { it.javaClass.simpleName to it.value })
                assertEquals(expected.docType?.internalSubset, current.docType?.internalSubset)
                assertEquals(expected.rootElement.attributes.map { "${it.qualifiedName}:${it.attributeType}:${it.isSpecified}" }, current.rootElement.attributes.map { "${it.qualifiedName}:${it.attributeType}:${it.isSpecified}" })
            } else {
                val expected = old.exceptionOrNull() as org.jdom2.input.JDOMParseException
                val current = actual.exceptionOrNull() as fi.sykero.kdom.input.JDOMParseException
                assertEquals(expected.message, current.message)
                assertEquals(expected.lineNumber, current.lineNumber)
                assertEquals(expected.columnNumber, current.columnNumber)
                assertEquals(expected.publicId, current.publicId)
                assertEquals(expected.systemId, current.systemId)
                assertEquals(expected.partialDocument?.hasRootElement(), current.partialDocument?.hasRootElement())
                assertEquals(expected.partialDocument?.let { org.jdom2.output.XMLOutputter().outputString(it) }, current.partialDocument?.let { fi.sykero.kdom.output.XMLOutputter().outputString(it) })
                assertEquals(expected.cause?.message, current.cause?.message)
            }
        } finally {
            Thread.currentThread().contextClassLoader = loader
            urlLoader.close()
        }
    }

    @Test fun emptyCdataRetainsLegacyBufferedNodeType() = compare("<!DOCTYPE doc [<!ELEMENT doc (#PCDATA)>]><doc>a<![CDATA[]]></doc>")
    @Test fun emptyCommentsAreSuppressed() = compare("<!DOCTYPE doc [<!ELEMENT doc EMPTY>]><!----><doc><!----></doc><!---->")
    @Test fun sameUriAliasNamespaceIsAdditional() = compare("<!DOCTYPE doc [<!ELEMENT doc EMPTY><!ATTLIST doc xmlns CDATA #IMPLIED xmlns:p CDATA #IMPLIED>]><doc xmlns='urn:same' xmlns:p='urn:same'/>")
    @Test fun ownNamespaceIsNotAdditional() = compare("<!DOCTYPE doc [<!ELEMENT doc EMPTY><!ATTLIST doc xmlns CDATA #IMPLIED xmlns:p CDATA #IMPLIED>]><doc xmlns='urn:own' xmlns:p='urn:extra'/>")
    @Test fun dtdPiNotationAndUnparsedDeclarations() = compare("""<!DOCTYPE doc [<!ELEMENT doc EMPTY><?kept data?><!NOTATION gif SYSTEM "image/gif"><!ENTITY logo SYSTEM "logo.gif" NDATA gif>]><doc/>""")

    @Test fun orderedAugmentedAttributes() = compare("""<!DOCTYPE doc [<!ELEMENT doc EMPTY><!ATTLIST doc z CDATA #IMPLIED a CDATA #IMPLIED xmlns:p CDATA #IMPLIED p:n CDATA #IMPLIED d NMTOKEN "default">]><doc z="1" a="2" xmlns:p="urn:p" p:n="3"/>""")
    @Test fun failuresBeforeAndAfterRoot() {
        compare("<bad")
        compare("<!DOCTYPE doc [<!ELEMENT doc EMPTY>]><doc><other/></doc>")
        compare("<!DOCTYPE doc [<!ELEMENT doc (#PCDATA)>]><doc>broken</wrong>")
    }
    @Test fun localExternalDtdAndEntity() {
        val entity = tmp.newFile("entity.xml").apply { writeText("<item/><item xml:base=\"real\"/>") }
        val declarations = tmp.newFile("nested.dtd").apply { writeText("<!ELEMENT doc (item*)><!ELEMENT item EMPTY><!ATTLIST item xml:base CDATA #IMPLIED><!ATTLIST doc z CDATA 'default'><!ENTITY ext SYSTEM '${entity.toURI()}'>") }
        val dtd = tmp.newFile("main.dtd").apply { writeText("<!ENTITY % nested SYSTEM 'nested.dtd'>%nested;") }
        compare("<!DOCTYPE doc SYSTEM '${dtd.toURI()}'><doc>&ext;</doc>")
        declarations.writeText("<!ELEMENT doc EMPTY>broken")
        compare("<!DOCTYPE doc SYSTEM '${dtd.toURI()}'><doc/>")
    }

    @Test fun callbackExceptionChain() {
        val original = fi.sykero.kdom.sax.SAXException("boom")
        val thrown = assertFailsWith<fi.sykero.kdom.sax.SAXException> {
            JDOMSAXEventProvider(fi.sykero.kdom.Document(fi.sykero.kdom.Element("r"))).provideSAXEvents(object : fi.sykero.kdom.sax.DefaultHandler() {
                override fun startElement(uri: String, localName: String, qName: String, atts: fi.sykero.kdom.sax.Attributes) { throw original }
            })
        }
        assertNotSame(original, thrown)
        assertEquals("Encountered a SAX exception processing the Document: ", thrown.message)
        assertTrue(thrown.cause is fi.sykero.kdom.JDOMException)
        assertSame(original, thrown.cause!!.cause)
    }
}
