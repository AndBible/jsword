package org.crosswire.common.xml

import java.io.ByteArrayInputStream
import javax.xml.parsers.*
import org.xml.sax.*
import org.xml.sax.helpers.XMLFilterImpl
import org.xml.sax.ext.LexicalHandler
import kotlin.test.*
import org.junit.Test

/** Provider injection is process-global; restore the exact property in finally. */
class ValidatingProviderParityTest {
    @Test fun creationAndNonparseFailuresAndLexicalFallback() {
        val key = "javax.xml.parsers.SAXParserFactory"
        val old = System.getProperty(key)
        try {
            // Initialize the reference's cached factory before injecting, then restore its exact field.
            val getSupplier = org.jdom2.input.sax.XMLReaders::class.java.getDeclaredMethod("getSupplier").apply { isAccessible = true }
            val supplier = getSupplier.invoke(org.jdom2.input.sax.XMLReaders.DTDVALIDATING)
            val cached = supplier.javaClass.getDeclaredField("factory").apply { isAccessible = true }
            val originalFactory = cached.get(supplier)
            cached.set(supplier, FixtureSaxFactory())
            try {
            System.setProperty(key, FixtureSaxFactory::class.java.name)
            for (mode in listOf("creationSax", "creationConfig", "creationRuntime", "configuration", "nonparse", "alternate", "unsupported", "partialCdata")) {
                FixtureSaxFactory.mode = mode
                val reference = runCatching {
                    org.jdom2.input.SAXBuilder(org.jdom2.input.sax.XMLReaders.DTDVALIDATING).build(ByteArrayInputStream(byteArrayOf()))
                }
                val current = runCatching { buildValidatedResource(ByteArrayInputStream(byteArrayOf())) }
                if (reference.isFailure) {
                    val expected = reference.exceptionOrNull()!!
                    val actual = current.exceptionOrNull()!!
                    assertEquals(expected.javaClass.simpleName, actual.javaClass.simpleName, mode)
                    assertEquals(expected.message, actual.message, mode)
                    assertEquals(expected.cause?.javaClass?.name, actual.cause?.javaClass?.name, mode)
                    assertEquals(expected.cause?.message, actual.cause?.message, mode)
                    if (expected is org.jdom2.input.JDOMParseException) {
                        actual as fi.sykero.kdom.input.JDOMParseException
                        assertEquals(expected.partialDocument?.let { org.jdom2.output.XMLOutputter().outputString(it) }, actual.partialDocument?.let { fi.sykero.kdom.output.XMLOutputter().outputString(it) }, mode)
                    }
                } else {
                    assertEquals(org.jdom2.output.XMLOutputter().outputString(reference.getOrThrow()), fi.sykero.kdom.output.XMLOutputter().outputString(current.getOrThrow()), mode)
                }
            }
            } finally { cached.set(supplier, originalFactory) }
        } finally {
            if (old == null) System.clearProperty(key) else System.setProperty(key, old)
        }
    }
}

class FixtureSaxFactory : SAXParserFactory() {
    companion object { var mode = "" }
    override fun setFeature(name: String, value: Boolean) {}
    override fun getFeature(name: String) = false
    override fun newSAXParser(): SAXParser {
        if (mode == "creationSax") throw SAXException("creation")
        if (mode == "creationConfig") throw ParserConfigurationException("configuration")
        if (mode == "creationRuntime") throw IllegalStateException("creation-runtime")
        return object : SAXParser() {
            private val reader = object : XMLFilterImpl() {
                private var lexical: LexicalHandler? = null
                override fun setFeature(name: String, value: Boolean) { if (mode == "configuration") throw SAXNotSupportedException(name) }
                override fun getFeature(name: String) = false
                override fun setProperty(name: String, value: Any?) {
                    if (mode == "unsupported" || (mode == "alternate" && name == "http://xml.org/sax/properties/lexical-handler")) throw SAXNotRecognizedException(name)
                    lexical = value as? LexicalHandler
                }
                override fun parse(input: InputSource) {
                    if (mode == "nonparse") throw SAXException("before root")
                    contentHandler.startDocument()
                    contentHandler.startElement("", "doc", "doc", org.xml.sax.helpers.AttributesImpl())
                    contentHandler.characters(charArrayOf('a'), 0, 1)
                    lexical?.startCDATA()
                    if (mode == "partialCdata") throw SAXParseException("partial", null, null, 2, 3)
                    lexical?.endCDATA()
                    lexical?.comment(charArrayOf('x'), 0, 1)
                    contentHandler.endElement("", "doc", "doc")
                    contentHandler.endDocument()
                }
            }
            override fun getParser(): Parser = throw UnsupportedOperationException()
            override fun getXMLReader(): XMLReader = reader
            override fun isNamespaceAware() = true
            override fun isValidating() = true
            override fun setProperty(name: String, value: Any?) = reader.setProperty(name, value)
            override fun getProperty(name: String): Any? = null
        }
    }
}
