package spike.xml

import nl.adaptivity.xmlutil.XmlException
import spike.xml.kdom.*

/** S2: jdom2 core port (kdom) fed by xmlutil's pull parser. [generic] picks the KtXml reader over the platform reader. */
class KdomArm(private val generic: Boolean = true) : XmlArm {
    override val name = if (generic) "kdom-xmlutil" else "kdom-xmlutil-platform"

    private fun parse(xml: String): Document? = try {
        XmlUtilBuilder.build(xml, generic)
    } catch (e: XmlException) {
        null
    } catch (e: KdomParseException) {
        null
    }

    override fun parseAndSerialize(xml: String): String? = parse(xml)?.rootElement?.let { canon(it) }

    fun canon(e: Element): String = StringBuilder().also { write(e, it) }.toString()

    private fun write(e: Element, sb: StringBuilder) {
        sb.append(Canon.open(e.qualifiedName, e.attributes.map { it.qualifiedName to it.value }))
        val text = StringBuilder()
        fun flush() { if (text.isNotEmpty()) { sb.append(Canon.escText(text.toString())); text.setLength(0) } }
        for (c in e.content) when (c) {
            is Element -> { flush(); write(c, sb) }
            is Text -> text.append(c.text)
            else -> {}
        }
        flush()
        sb.append("</").append(e.qualifiedName).append('>')
    }

    private val xhtml = Namespace.getNamespace("ns", "http://www.w3.org/1999/xhtml")
    private val svg = Namespace.getNamespace("svg", "http://www.w3.org/2000/svg")
    private val xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink")
    private val dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/")
    private val opf = Namespace.getNamespace("ns", "http://www.idpf.org/2007/opf")
    private val container = Namespace.getNamespace("ns", "urn:oasis:names:tc:opendocument:xmlns:container")
    private val ncx = Namespace.getNamespace("ns", "http://www.daisy.org/z3986/2005/ncx/")

    /** Each XPath of EpubBackendState / EpubOptimization / SwordContentFacade.addAnchors, in the order of Reference.referenceEpub. */
    override fun epubQueries(docs: Map<String, String>): Map<String, List<String>> {
        val res = LinkedHashMap<String, List<String>>()
        fun root(d: String) = parse(docs.getValue(d))!!.rootElement   // a failed parse must fail the experiment
        fun put(doc: String, id: String, hits: Sequence<Element>) { res["$doc/$id"] = hits.map { canon(it) }.toList() }

        root("container.xml").let { put("container.xml", "rootfile", it.descendantsOrSelf("rootfile", container)) }
        root("content.opf").let { r ->
            val items = r.descendantsOrSelf("manifest", opf).flatMap { it.children("item", opf) }  // //ns:manifest/ns:item
            put("content.opf", "manifestItem", items)
            put("content.opf", "tocByMedia", items.filter { it.attrIs("media-type", "application/x-dtbncx+xml") })
            put("content.opf", "tocById", items.filter { it.attrIs("id", "toc") })
            put("content.opf", "tocByNav", items.filter { it.attrIs("properties", "nav") })
            put("content.opf", "spineItemref", r.descendantsOrSelf("spine", opf).flatMap { it.children("itemref", opf) })
            for (k in listOf("title", "creator", "language")) put("content.opf", "dc" + k.replaceFirstChar { it.uppercase() }, r.descendantsOrSelf(k, dc))
        }
        root("toc.ncx").let { r ->
            val contents = r.descendantsOrSelf("navPoint", ncx).flatMap { it.children("content", ncx) }.toList()
            put("toc.ncx", "navContent", contents.asSequence())
            put("toc.ncx", "navLabel", contents.asSequence().flatMap { c -> // ../ns:navLabel/ns:text
                c.parentElement()?.children("navLabel", ncx)?.flatMap { it.children("text", ncx) }.orEmpty() })
        }
        for (d in listOf("ch1.xhtml", "ch2.xhtml", "ch3.xhtml")) {
            val r = root(d)
            put(d, "img", r.descendantsOrSelf("img", xhtml)); put(d, "a", r.descendantsOrSelf("a", xhtml))
            put(d, "body", r.descendantsOrSelf("body", xhtml)); put(d, "bva", r.descendantsOrSelf("BVA", xhtml))
            put(d, "withId", r.descendantsOrSelf(null).filter { it.attrIs("id") })              // //*[@id]
            put(d, "svgImage", r.descendantsOrSelf("image", svg).filter { it.attrIs("href", null, xlink) })
            for (ord in listOf(1, 3, 5, 99)) {
                val hit = r.descendantsOrSelf().firstOrNull { e ->
                    e.descendants("BVA", xhtml).any { it.attrIs("ordinal", "$ord") } &&
                        (e.followingSiblings().any { it.matches("p", xhtml) } || e.precedingSiblings().any { it.matches("p", xhtml) } || e.matches("p", xhtml))
                }
                put(d, "split$ord", listOfNotNull(hit).asSequence())
            }
        }
        for (d in listOf("ch1.xhtml", "frag.xml")) {
            res["$d/textNoNote"] = root(d).textNodes(excludeAncestor = "note").map { Canon.escText(it.text) }
        }
        return res
    }
}
