package spike.xml.kdom

/** Subset of org.jdom2.output.Format: indent, line separator, text mode, declaration and empty-element switches. */
class Format private constructor() {
    enum class TextMode { PRESERVE, TRIM, NORMALIZE }

    var indent: String? = null
    var lineSeparator: String? = "\r\n"
    var textMode = TextMode.PRESERVE
    var omitDeclaration = false
    var omitEncoding = false
    var encoding = "UTF-8"
    var expandEmptyElements = false

    fun setIndent(s: String?) = apply { indent = s }
    fun setLineSeparator(s: String?) = apply { lineSeparator = if (s == "") null else s }
    fun setTextMode(m: TextMode) = apply { textMode = m }
    fun setOmitDeclaration(b: Boolean) = apply { omitDeclaration = b }
    fun setExpandEmptyElements(b: Boolean) = apply { expandEmptyElements = b }

    companion object {
        fun getRawFormat() = Format()
        fun getPrettyFormat() = Format().setIndent("  ").setTextMode(TextMode.TRIM)
        fun getCompactFormat() = Format().setTextMode(TextMode.NORMALIZE)
    }
}

/** Serializer producing the same strings as org.jdom2.output.XMLOutputter for elements, text, CDATA and namespaces. */
class XMLOutputter(private val format: Format = Format.getRawFormat()) {
    private companion object { val INITIAL_SCOPE = mapOf("" to "", "xml" to Namespace.XML_NAMESPACE.uri) }

    fun outputString(e: Element): String = StringBuilder().also { printElement(it, e, 0, format.textMode, INITIAL_SCOPE) }.toString()

    fun outputString(d: Document): String {
        val out = StringBuilder()
        if (!format.omitDeclaration) {
            out.append("<?xml version=\"1.0\"")
            if (!format.omitEncoding) out.append(" encoding=\"").append(format.encoding).append('"')
            out.append("?>").append(format.lineSeparator ?: "")
        }
        val root = d.content.filterIsInstance<Element>().firstOrNull()
        if (root != null) {
            printElement(out, root, 0, format.textMode, INITIAL_SCOPE)
            format.lineSeparator?.let { out.append(it) }
        }
        return out.toString()
    }

    private fun pad(depth: Int, mode: Format.TextMode): String {
        val ind = format.indent
        if (ind == null || mode == Format.TextMode.PRESERVE) return ""
        return (format.lineSeparator ?: "") + ind.repeat(depth)
    }

    private fun escText(s: String, sb: StringBuilder) {
        for (c in s) when (c) {
            '&' -> sb.append("&amp;"); '<' -> sb.append("&lt;"); '>' -> sb.append("&gt;"); '\r' -> sb.append("&#xD;")
            '\n' -> sb.append(format.lineSeparator ?: "\n")
            else -> sb.append(c)
        }
    }

    private fun escAttr(s: String, sb: StringBuilder) {
        for (c in s) when (c) {
            '&' -> sb.append("&amp;"); '<' -> sb.append("&lt;"); '>' -> sb.append("&gt;"); '"' -> sb.append("&quot;")
            '\t' -> sb.append("&#x9;"); '\n' -> sb.append("&#xA;"); '\r' -> sb.append("&#xD;")
            else -> sb.append(c)
        }
    }

    /** One formatted piece of an element's content: an already-serialized text run, or a child element. */
    private class Item(val raw: String?, val element: Element?)

    private fun textPiece(t: Text, s: String, sb: StringBuilder) {
        if (t is CDATA) sb.append("<![CDATA[").append(s).append("]]>") else escText(s, sb)
    }

    private fun items(content: List<Content>, mode: Format.TextMode): List<Item> {
        val res = ArrayList<Item>()
        var i = 0
        while (i < content.size) {
            val c = content[i]
            if (c is Element) { res.add(Item(null, c)); i++; continue }
            var j = i
            while (j < content.size && content[j] is Text) j++
            val run = content.subList(i, j).map { it as Text }
            i = j
            val sb = StringBuilder()
            when (mode) {
                Format.TextMode.PRESERVE -> run.forEach { textPiece(it, it.text, sb) }
                Format.TextMode.TRIM -> {
                    val ts = run.dropWhile { isAllXmlWs(it.text) }.dropLastWhile { isAllXmlWs(it.text) }
                    ts.forEachIndexed { k, t ->
                        var s = t.text
                        if (k == 0) s = s.trimStart(::isXmlWs)
                        if (k == ts.lastIndex) s = s.trimEnd(::isXmlWs)
                        textPiece(t, s, sb)
                    }
                }
                Format.TextMode.NORMALIZE -> {
                    var between = false; var needSpace = false
                    for (t in run) {
                        if (isAllXmlWs(t.text)) { if (between && t.text.isNotEmpty()) needSpace = true; continue }
                        if (between && (needSpace || isXmlWs(t.text.first()))) sb.append(' ')
                        textPiece(t, normalizeString(t.text), sb)
                        between = true; needSpace = isXmlWs(t.text.last())
                    }
                }
            }
            // PRESERVE keeps the run even if empty-looking; trimmed modes drop empty runs
            if (sb.isNotEmpty() || (mode == Format.TextMode.PRESERVE && run.any { it.text.isNotEmpty() })) res.add(Item(sb.toString(), null))
        }
        return res
    }

    private fun printElement(out: StringBuilder, e: Element, depth: Int, parentMode: Format.TextMode, scope: Map<String, String>) {
        out.append('<').append(e.qualifiedName)
        val added = ArrayList<Namespace>()
        val newScope = HashMap(scope)
        fun consider(ns: Namespace) {
            if ((newScope[ns.prefix] ?: "") != ns.uri) { added.add(ns); newScope[ns.prefix] = ns.uri }
        }
        consider(e.namespace)
        e.additionalNamespaces.forEach { if (it != e.namespace) consider(it) }
        e.attributes.forEach { if (it.namespace != Namespace.NO_NAMESPACE && it.namespace != e.namespace) consider(it.namespace) }
        for (ns in added) {
            out.append(" xmlns")
            if (ns.prefix.isNotEmpty()) out.append(':').append(ns.prefix)
            out.append("=\""); escAttr(ns.uri, out); out.append('"')
        }
        for (a in e.attributes) { out.append(' ').append(a.qualifiedName).append("=\""); escAttr(a.value, out); out.append('"') }

        fun empty() { if (format.expandEmptyElements) out.append("></").append(e.qualifiedName).append('>') else out.append(" />") }
        if (e.content.isEmpty()) { empty(); return }

        var mode = parentMode
        when (e.getAttributeValue("space", Namespace.XML_NAMESPACE)) {
            "default" -> mode = format.textMode
            "preserve" -> mode = Format.TextMode.PRESERVE
        }
        val its = items(e.content, mode)
        if (its.isEmpty()) { empty(); return }
        out.append('>')
        val allText = its.size == 1 && its[0].raw != null
        if (!allText) out.append(pad(depth + 1, mode))
        its.forEachIndexed { k, it ->
            if (k > 0) out.append(pad(depth + 1, mode))
            if (it.raw != null) out.append(it.raw) else printElement(out, it.element!!, depth + 1, mode, newScope)
        }
        if (!allText) out.append(pad(depth, mode))
        out.append("</").append(e.qualifiedName).append('>')
    }
}
