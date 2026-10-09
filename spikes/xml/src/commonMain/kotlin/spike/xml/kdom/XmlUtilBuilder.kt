package spike.xml.kdom

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlStreaming

/** Thrown where SAX would raise a JDOMException (malformed input the xmlutil reader did not reject itself). */
class KdomParseException(msg: String) : Exception(msg)

/**
 * Builds a [Document] from xmlutil pull-parser events (stand-in for org.jdom2.input.SAXBuilder).
 * [generic] = true uses the platform-independent KtXml reader (what iOS would use); false uses the platform
 * default (StAX on the JVM).
 */
object XmlUtilBuilder {
    /** @throws XmlException or [KdomParseException] on malformed input. */
    fun build(xml: String, generic: Boolean = true): Document {
        if (xml.isBlank()) throw KdomParseException("empty input") // xmlutil throws IllegalArgumentException here
        val r: XmlReader = if (generic) XmlStreaming.newGenericReader(xml) else XmlStreaming.newReader(xml)
        try {
            return read(r)
        } finally {
            r.close()
        }
    }

    private fun read(r: XmlReader): Document {
        val doc = Document()
        var cur: Parent = doc
        val text = StringBuilder()
        fun flush() {
            if (text.isNotEmpty()) {
                if (cur is Element) cur.addContent(Text(text.toString()))
                text.setLength(0)
            }
        }
        while (r.hasNext()) {
            when (r.next()) {
                EventType.START_ELEMENT -> {
                    flush()
                    val ns = Namespace.getNamespace(r.prefix, r.namespaceURI)
                    val e = Element(r.localName, ns)
                    for (d in r.namespaceDecls) {
                        val n = Namespace.getNamespace(d.prefix, d.namespaceURI)
                        if (n.prefix != ns.prefix) e.addNamespaceDeclaration(n)
                    }
                    for (i in 0 until r.attributeCount) {
                        val an = Namespace.getNamespace(r.getAttributePrefix(i), r.getAttributeNamespace(i))
                        if (e.getAttribute(r.getAttributeLocalName(i), an) != null) throw KdomParseException("duplicate attribute") // xmlutil keeps the last
                        e.setAttribute(r.getAttributeLocalName(i), r.getAttributeValue(i), an)
                    }
                    if (cur is Document && doc.hasRootElement()) throw KdomParseException("second root element")
                    cur.addContent(e)
                    cur = e
                }
                EventType.END_ELEMENT -> {
                    flush()
                    cur = (cur as? Element)?.parent ?: throw KdomParseException("unbalanced end tag")
                }
                EventType.TEXT, EventType.IGNORABLE_WHITESPACE ->
                    if (cur is Element) text.append(r.text)
                    else if (r.text.any { !isXmlWs(it) }) throw KdomParseException("text outside the root element")
                EventType.ENTITY_REF -> if (cur is Element && r.text.isNotEmpty()) text.append(r.text) else throw KdomParseException("entity outside root")
                EventType.CDSECT -> if (cur is Element) { flush(); cur.addContent(CDATA(r.text)) }
                else -> {} // comments, PIs, doctype, document start/end: not modelled
            }
        }
        if (cur !is Document) throw KdomParseException("unclosed element at end of input")
        if (!doc.hasRootElement()) throw KdomParseException("no root element")
        return doc
    }
}
