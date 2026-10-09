package spike.xml.ksoup

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.CDataNode
import com.fleeksoft.ksoup.nodes.Comment
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.parser.Parser
import spike.xml.Canon
import spike.xml.XmlArm

/** S1: ksoup (jsoup port) in XML mode. Rejection = the parser tracked at least one error. */
class KsoupArm : XmlArm {
    override val name = "ksoup"

    private fun parse(xml: String): Pair<Element?, Int> {
        val parser = Parser.xmlParser().setTrackErrors(50)
        val doc = Ksoup.parse(xml, parser)
        return doc.children().firstOrNull() to parser.getErrors().size
    }

    override fun parseAndSerialize(xml: String): String? {
        val (root, errors) = parse(xml)
        if (root == null || errors > 0) return null
        return canon(root)
    }

    private fun canon(e: Element): String {
        val sb = StringBuilder()
        write(e, sb)
        return sb.toString()
    }

    private fun write(n: Node, sb: StringBuilder) {
        when (n) {
            is Element -> {
                sb.append(Canon.open(n.tagName(), n.attributes().map { it.key to it.value }))
                for (c in n.childNodes()) write(c, sb)
                sb.append("</").append(n.tagName()).append('>')
            }
            is Comment -> {}
            is TextNode -> sb.append(Canon.escText(n.getWholeText())) // CDataNode extends TextNode
            else -> {} // XmlDeclaration, DocumentType, ...
        }
    }

    private fun descendants(e: Element): List<Element> = e.getAllElements().filter { it !== e }

    private fun textNodes(e: Element, out: MutableList<TextNode>, inNote: Boolean) {
        for (c in e.childNodes()) {
            when (c) {
                is Element -> textNodes(c, out, inNote || c.tagName() == "note")
                is TextNode -> if (!inNote) out.add(c)
                else -> {}
            }
        }
    }

    override fun epubQueries(docs: Map<String, String>): Map<String, List<String>> {
        val res = LinkedHashMap<String, List<String>>()
        fun sel(doc: String, id: String, css: String) {
            val root = parse(docs.getValue(doc)).first
            res["$doc/$id"] = root?.select(css)?.map { canon(it) } ?: emptyList()
        }
        sel("container.xml", "rootfile", "rootfile")
        for (id in listOf("manifestItem" to "manifest > item",
            "tocByMedia" to "manifest > item[media-type=application/x-dtbncx+xml]",
            "tocById" to "manifest > item[id=toc]",
            "tocByNav" to "manifest > item[properties=nav]",
            "spineItemref" to "spine > itemref",
            "dcTitle" to "dc|title", "dcCreator" to "dc|creator", "dcLanguage" to "dc|language")) {
            sel("content.opf", id.first, id.second)
        }
        sel("toc.ncx", "navContent", "navPoint > content")
        run {
            val root = parse(docs.getValue("toc.ncx")).first
            res["toc.ncx/navLabel"] = root?.select("navPoint > content")?.flatMap { c ->
                c.parent()!!.select("> navLabel > text").map { canon(it) }
            } ?: emptyList()
        }
        for (d in listOf("ch1.xhtml", "ch2.xhtml", "ch3.xhtml")) {
            sel(d, "img", "img"); sel(d, "a", "a"); sel(d, "body", "body"); sel(d, "bva", "BVA")
            sel(d, "withId", "[id]")
            sel(d, "svgImage", "svg|image[xlink:href]")
            val root = parse(docs.getValue(d)).first
            for (ord in listOf(1, 3, 5, 99)) {
                res["$d/split$ord"] = if (root == null) emptyList() else {
                    val hit = root.getAllElements().firstOrNull { e ->
                        descendants(e).any { it.tagName() == "BVA" && it.attr("ordinal") == "$ord" } &&
                            (e.tagName() == "p" || (e.parent()?.children()?.any { it !== e && it.tagName() == "p" } == true))
                    }
                    listOfNotNull(hit?.let { canon(it) })
                }
            }
        }
        for (d in listOf("ch1.xhtml", "frag.xml")) {
            val root = parse(docs.getValue(d)).first
            val out = ArrayList<TextNode>()
            if (root != null) textNodes(root, out, false)
            res["$d/textNoNote"] = out.map { Canon.escText(it.getWholeText()) }
        }
        return res
    }
}
