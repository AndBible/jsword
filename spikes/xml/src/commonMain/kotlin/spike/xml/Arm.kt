package spike.xml

interface XmlArm {
    val name: String
    /** Parses [xml]; returns its canonical serialization, or null where the parser rejects the input. */
    fun parseAndSerialize(xml: String): String?
    /** Runs the EPUB queries on the fixture's documents; returns, per query id, the selected elements' canonical forms. */
    fun epubQueries(docs: Map<String, String>): Map<String, List<String>>
}

/**
 * Canonical form helpers shared by all arms and the reference.
 * Elements: `<qname a="v" ...>children</qname>` (attributes sorted by qualified name, never self-closing),
 * text escaped minimally (& < >), adjacent text/CDATA merged, no declaration/doctype/comments/PIs,
 * xmlns / xmlns:* attributes dropped (namespace declarations are not attributes in JDOM; prefixes are kept as written).
 */
object Canon {
    fun escText(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    fun escAttr(s: String): String = escText(s).replace("\"", "&quot;")

    fun isNsDecl(name: String) = name == "xmlns" || name.startsWith("xmlns:")

    fun open(qname: String, attrs: List<Pair<String, String>>): String {
        val sb = StringBuilder("<").append(qname)
        for ((k, v) in attrs.filter { !isNsDecl(it.first) }.sortedBy { it.first }) {
            sb.append(' ').append(k).append("=\"").append(escAttr(v)).append('"')
        }
        return sb.append('>').toString()
    }
}
