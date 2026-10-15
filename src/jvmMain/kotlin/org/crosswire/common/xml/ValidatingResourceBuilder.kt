package org.crosswire.common.xml

import fi.sykero.kdom.*
import fi.sykero.kdom.input.JDOMParseException
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import org.xml.sax.ext.Attributes2
import org.xml.sax.ext.DefaultHandler2

/** JVM-only legacy resource validation seam; ordered SAX events avoid DOM augmentation/fixups. */
internal fun buildValidatedResource(input: InputStream): Document {
    val tree = ValidatingResourceHandler()
    val reader = try {
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = true
        }.newSAXParser().xmlReader
    } catch (e: SAXException) {
        throw JDOMException("Unable to create a new XMLReader instance", e)
    } catch (e: javax.xml.parsers.ParserConfigurationException) {
        throw JDOMException("Unable to create a new XMLReader instance", e)
    } catch (e: Exception) {
        throw JDOMException("It was not possible to configure a suitable XMLReader to support DTDVALIDATING", e)
    }
    // Configuration errors are not parse errors; lexical reporting is optional in legacy SAXBuilder.
    reader.contentHandler = tree
    reader.errorHandler = tree
    reader.dtdHandler = tree
        for (property in listOf("http://xml.org/sax/properties/lexical-handler", "http://xml.org/sax/handlers/LexicalHandler")) {
            try { reader.setProperty(property, tree); break }
            catch (_: org.xml.sax.SAXNotSupportedException) {}
            catch (_: org.xml.sax.SAXNotRecognizedException) {}
        }
    val entityFeature = "http://xml.org/sax/features/external-general-entities"
    try { reader.setFeature(entityFeature, true) }
    catch (e: org.xml.sax.SAXNotSupportedException) {
        throw JDOMException("$entityFeature feature $entityFeature not supported for SAX driver ${reader.javaClass.name}")
    }
    catch (e: org.xml.sax.SAXNotRecognizedException) {
        throw JDOMException("$entityFeature feature $entityFeature not recognized for SAX driver ${reader.javaClass.name}")
    }
    try {
        input.use { reader.parse(org.xml.sax.InputSource(it)) }
        return tree.document
    } catch (e: SAXParseException) {
        val where = e.systemId?.let { " of document $it" } ?: ""
        throw JDOMParseException("Error on line ${e.lineNumber}$where: ${e.message}", e,
            tree.document.takeIf { it.hasRootElement() }, e.publicId, e.systemId, e.lineNumber, e.columnNumber)
    } catch (e: SAXException) {
        throw JDOMParseException("Error in building: ${e.message}", e, tree.document)
    }
}

private class ValidatingResourceHandler : DefaultHandler2() {
    val document = Document()
    private var current: Element? = null
    private val namespaces = mutableListOf<Namespace>()
    private val text = StringBuilder()
    private var cdata = false
    private var previousCdata = false
    private val internalSubset = StringBuilder()
    private var inInternalSubset = false
    private var inDtd = false

    override fun error(e: SAXParseException) { throw e }
    override fun fatalError(e: SAXParseException) { throw e }
    override fun startPrefixMapping(prefix: String, uri: String) { namespaces.add(Namespace.getNamespace(prefix, uri)) }
    override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
        flush()
        val element = Element(localName, Namespace.getNamespace(qName.substringBefore(':', ""), uri))
        namespaces.filter { it.prefix != element.namespace.prefix || it.uri != element.namespace.uri }.forEach { element.addNamespaceDeclaration(it) }
        namespaces.clear()
        for (i in 0 until attrs.length) {
            val qualified = attrs.getQName(i)
            if (qualified == "xmlns" || qualified.startsWith("xmlns:")) continue
            val attribute = Attribute(attrs.getLocalName(i), attrs.getValue(i), AttributeType.getAttributeType(attrs.getType(i)),
                Namespace.getNamespace(qualified.substringBefore(':', ""), attrs.getURI(i)))
            if (attrs is Attributes2) attribute.isSpecified = attrs.isSpecified(i)
            element.setAttribute(attribute)
        }
        add(element)
        current = element
    }
    override fun endElement(uri: String, localName: String, qName: String) { flush(); current = current!!.parentElement }
    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (length == 0 && !cdata) return
        if (previousCdata != cdata) flush()
        text.append(ch, start, length)
    }
    override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) { characters(ch, start, length) }
    override fun processingInstruction(target: String, data: String) { flush(); add(ProcessingInstruction(target, data)) }
    override fun comment(ch: CharArray, start: Int, length: Int) { flush(); if (!inDtd && length > 0) add(Comment(String(ch, start, length))) }
    override fun startCDATA() { cdata = true }
    override fun endCDATA() { previousCdata = true; flush(); previousCdata = false; cdata = false }
    override fun startDTD(name: String, publicId: String?, systemId: String?) {
        flush(); inDtd = true; inInternalSubset = true
        document.addContent(DocType(name, publicId, systemId).apply { internalSubset = "" })
    }
    override fun endDTD() { document.docType!!.internalSubset = internalSubset.toString(); inDtd = false; inInternalSubset = false }
    override fun endEntity(name: String) { if (name == "[dtd]") inInternalSubset = true }
    override fun notationDecl(name: String, publicId: String?, systemId: String?) {
        if (inInternalSubset) { internalSubset.append("  <!NOTATION ").append(name); externalId(publicId, systemId); internalSubset.append(">\n") }
    }
    override fun unparsedEntityDecl(name: String, publicId: String?, systemId: String?, notationName: String) {
        if (inInternalSubset) { internalSubset.append("  <!ENTITY ").append(name); externalId(publicId, systemId); internalSubset.append(" NDATA ").append(notationName).append(">\n") }
    }
    private fun externalId(publicId: String?, systemId: String?) {
        if (publicId != null) internalSubset.append(" PUBLIC ").append('"').append(publicId).append('"')
        if (systemId != null) { internalSubset.append(if (publicId == null) " SYSTEM " else " "); internalSubset.append('"').append(systemId).append('"') }
    }
    override fun endDocument() { flush() }
    private fun add(node: Content) { current?.addContent(node) ?: document.addContent(node) }
    private fun flush() {
        if (text.isNotEmpty() || cdata) {
            if (previousCdata) add(CDATA(text.toString())) else if (text.isNotEmpty()) add(Text(text.toString()))
        }
        previousCdata = cdata
        text.setLength(0)
    }
}
