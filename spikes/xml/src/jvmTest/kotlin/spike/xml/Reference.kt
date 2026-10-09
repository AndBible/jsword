package spike.xml

import java.io.File
import java.io.StringReader
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.Books
import org.jdom2.CDATA
import org.jdom2.Content
import org.jdom2.Element
import org.jdom2.JDOMException
import org.jdom2.Namespace
import org.jdom2.Text
import org.jdom2.filter.Filters
import org.jdom2.input.SAXBuilder
import org.jdom2.xpath.XPathFactory

/** One parser input: [group] e.g. "osis:KJV", [key] the module key (or fixture name), [xml] the exact string handed to the parser. */
class XmlInput(val group: String, val key: String, val xml: String)

/** Reference = JDOM2 + SAX exactly as JSword uses it today. */
object Reference {
    const val KEY_CAP = 3000

    private val builder = SAXBuilder().apply { setFeature("http://xml.org/sax/features/external-general-entities", false) }

    /** Canonical form of the jdom tree, or null on JDOMException (what OSISFilter/THMLFilter treat as a parse failure). */
    fun referenceSerialize(xml: String): String? = try {
        canon(builder.build(StringReader(xml)).rootElement)
    } catch (e: JDOMException) {
        null
    } catch (e: java.io.IOException) {
        null
    }

    fun canon(e: Element): String = StringBuilder().also { write(e, it) }.toString()

    private fun write(e: Element, sb: StringBuilder) {
        sb.append(Canon.open(e.qualifiedName, e.attributes.map { it.qualifiedName to it.value }))
        val text = StringBuilder()
        fun flush() { if (text.isNotEmpty()) { sb.append(Canon.escText(text.toString())); text.setLength(0) } }
        for (c: Content in e.content) {
            when (c) {
                is Element -> { flush(); write(c, sb) }
                is Text -> text.append(c.text) // CDATA extends Text
                else -> {} // comments, PIs
            }
        }
        flush()
        sb.append("</").append(e.qualifiedName).append('>')
    }

    // ---- module inputs --------------------------------------------------------------------------------

    class ModConf(val initials: String, val modDrv: String?, val sourceType: String?)

    fun confs(): List<ModConf> {
        val dir = File(System.getProperty("user.home"), ".sword/mods.d")
        return (dir.listFiles { f -> f.name.endsWith(".conf") } ?: emptyArray()).sortedBy { it.name }.mapNotNull { f ->
            val lines = f.readLines(Charsets.UTF_8)
            val init = lines.firstOrNull { it.startsWith("[") }?.trim()?.removePrefix("[")?.removeSuffix("]") ?: return@mapNotNull null
            fun prop(k: String) = lines.firstOrNull { it.startsWith("$k=") }?.substringAfter('=')?.trim()
            ModConf(init, prop("ModDrv"), prop("SourceType"))
        }
    }

    /** JSword defaults to OSIS when SourceType is absent (SwordBookMetaData.getSourceType) for text modules. */
    fun isOsis(c: ModConf) = c.sourceType.equals("OSIS", true) || (c.sourceType == null && c.modDrv.equals("zText", true))
    fun isThml(c: ModConf) = c.sourceType.equals("ThML", true)

    /** Evenly spaced sample of at most [KEY_CAP] keys over the whole module (deviation from "first 3000": covers all books). */
    private fun rawTexts(book: Book): List<Pair<String, String>> {
        val all = book.getGlobalKeyList().iterator().asSequence().toList()
        val stride = maxOf(1, (all.size + KEY_CAP - 1) / KEY_CAP)
        val out = ArrayList<Pair<String, String>>()
        for (i in all.indices step stride) {
            val k = all[i]
            try {
                out.add(k.getOsisID() to book.getRawText(k))
            } catch (e: Exception) {
                // harness: unreadable key, skip
            }
        }
        return out
    }

    /** Raw-sample sizes per module id, filled as a side effect (module id -> total keys, sampled). */
    val sampleInfo = LinkedHashMap<String, String>()

    fun osisInputs(): List<XmlInput> = confs().filter(::isOsis).flatMap { c ->
        val book = Books.installed().getBook(c.initials) ?: return@flatMap emptyList()
        val total = book.getGlobalKeyList().getCardinality()
        val raws = rawTexts(book)
        sampleInfo["osis:${c.initials}"] = "$total keys, sampled ${raws.size}"
        // OSISFilter.parse(): "<xxx>" + plain + "</xxx>" (the div milestone rewrite is commented out at HEAD).
        raws.map { (k, t) -> XmlInput("osis:${c.initials}", k, "<xxx>$t</xxx>") }
    }

    private fun wrapThml(s: String?) = "<root>$s</root>"

    /**
     * THMLFilter chain: cleanAllEntities -> parse; on failure cleanAllCharacters -> parse; closeEmptyTags -> parse;
     * cleanAllTags(clean) -> parse. Later stage inputs are generated only where the reference rejects the earlier stage.
     */
    fun thmlInputs(): List<XmlInput> = confs().filter(::isThml).flatMap { c ->
        val book = Books.installed().getBook(c.initials) ?: return@flatMap emptyList()
        val total = book.getGlobalKeyList().getCardinality()
        val raws = rawTexts(book)
        sampleInfo["thml:${c.initials}"] = "$total keys, sampled ${raws.size}"
        val out = ArrayList<XmlInput>()
        for ((k, t) in raws) {
            val clean = XMLUtil.cleanAllEntities(t)
            val p = wrapThml(clean)
            out.add(XmlInput("thml.primary:${c.initials}", k, p))
            if (referenceSerialize(p) != null) continue
            val text = XMLUtil.cleanAllCharacters(clean!!)
            val s2 = wrapThml(text)
            out.add(XmlInput("thml.chars:${c.initials}", k, s2))
            if (referenceSerialize(s2) != null) continue
            val s3 = wrapThml(XMLUtil.closeEmptyTags(text))
            out.add(XmlInput("thml.closeEmpty:${c.initials}", k, s3))
            if (referenceSerialize(s3) != null) continue
            out.add(XmlInput("thml.cleanTags:${c.initials}", k, wrapThml(XMLUtil.cleanAllTags(text))))
        }
        out
    }

    // ---- EPUB -----------------------------------------------------------------------------------------

    val DOC_NAMES = listOf("container.xml", "content.opf", "toc.ncx", "ch1.xhtml", "ch2.xhtml", "ch3.xhtml", "frag.xml")
    private val PATHS = mapOf(
        "container.xml" to "META-INF/container.xml", "content.opf" to "OEBPS/content.opf", "toc.ncx" to "OEBPS/toc.ncx",
        "ch1.xhtml" to "OEBPS/ch1.xhtml", "ch2.xhtml" to "OEBPS/ch2.xhtml", "ch3.xhtml" to "OEBPS/ch3.xhtml", "frag.xml" to "OEBPS/frag.xml")

    fun epubDocs(): Map<String, String> = DOC_NAMES.associateWith { n ->
        Reference::class.java.getResourceAsStream("/epub/${PATHS.getValue(n)}")!!.readBytes().toString(Charsets.UTF_8)
    }

    private val xhtml = Namespace.getNamespace("ns", "http://www.w3.org/1999/xhtml")
    private val svg = Namespace.getNamespace("svg", "http://www.w3.org/2000/svg")
    private val xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink")
    private val dc = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/")
    private val opf = Namespace.getNamespace("ns", "http://www.idpf.org/2007/opf")
    private val container = Namespace.getNamespace("ns", "urn:oasis:names:tc:opendocument:xmlns:container")
    private val ncx = Namespace.getNamespace("ns", "http://www.daisy.org/z3986/2005/ncx/")

    /** The XPath expressions and namespaces of EpubBackendState.kt / EpubOptimization.kt / SwordContentFacade.addAnchors. */
    fun referenceEpub(docs: Map<String, String>): Map<String, List<String>> {
        val res = LinkedHashMap<String, List<String>>()
        fun parse(d: String) = builder.build(StringReader(docs.getValue(d)))
        fun q(doc: String, id: String, expr: String, vararg ns: Namespace) {
            val xp = XPathFactory.instance().compile(expr, Filters.element(), null, *ns)
            res["$doc/$id"] = xp.evaluate(parse(doc)).map { canon(it) }
        }
        q("container.xml", "rootfile", "//ns:rootfile", container)
        q("content.opf", "manifestItem", "//ns:manifest/ns:item", opf)
        q("content.opf", "tocByMedia", "//ns:manifest/ns:item[@media-type='application/x-dtbncx+xml']", opf)
        q("content.opf", "tocById", "//ns:manifest/ns:item[@id='toc']", opf)
        q("content.opf", "tocByNav", "//ns:manifest/ns:item[@properties='nav']", opf)
        q("content.opf", "spineItemref", "//ns:spine/ns:itemref", opf)
        for (k in listOf("title", "creator", "language")) q("content.opf", "dc" + k.replaceFirstChar { it.uppercase() }, "//dc:$k", dc)
        q("toc.ncx", "navContent", "//ns:navPoint/ns:content", ncx)
        run {
            val doc = parse("toc.ncx")
            val contents = XPathFactory.instance().compile("//ns:navPoint/ns:content", Filters.element(), null, ncx).evaluate(doc)
            val rel = XPathFactory.instance().compile("../ns:navLabel/ns:text", Filters.element(), null, ncx)
            res["toc.ncx/navLabel"] = contents.flatMap { c -> rel.evaluate(c).map { canon(it) } }
        }
        for (d in listOf("ch1.xhtml", "ch2.xhtml", "ch3.xhtml")) {
            q(d, "img", "//ns:img", xhtml); q(d, "a", "//ns:a", xhtml); q(d, "body", "//ns:body", xhtml)
            q(d, "bva", "//ns:BVA", xhtml); q(d, "withId", "//*[@id]", xhtml)
            q(d, "svgImage", "//svg:image[@xlink:href]", svg, xlink)
            for (ord in listOf(1, 3, 5, 99)) {
                val xp = XPathFactory.instance().compile(
                    "//*[descendant::ns:BVA[@ordinal='$ord'] and (following-sibling::ns:p or preceding-sibling::ns:p or self::ns:p)]",
                    Filters.element(), null, xhtml)
                res["$d/split$ord"] = listOfNotNull(xp.evaluateFirst(parse(d))?.let { canon(it) })
            }
        }
        for (d in listOf("ch1.xhtml", "frag.xml")) {
            val xp = XPathFactory.instance().compile(".//text()[not(ancestor::note)]", Filters.text())
            res["$d/textNoNote"] = xp.evaluate(parse(d).rootElement).map { Canon.escText(it.text) }
        }
        return res
    }
}
