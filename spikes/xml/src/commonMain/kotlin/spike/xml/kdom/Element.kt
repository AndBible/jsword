package spike.xml.kdom

class Element(name: String, namespace: Namespace = Namespace.NO_NAMESPACE) : Content(), Parent {
    var name: String = name
    var namespace: Namespace = namespace
    private val attrs = ArrayList<Attribute>()
    private val items = ArrayList<Content>()
    private val extraNamespaces = ArrayList<Namespace>()

    constructor(name: String, uri: String) : this(name, Namespace.getNamespace("", uri))
    constructor(name: String, prefix: String, uri: String) : this(name, Namespace.getNamespace(prefix, uri))

    fun getNamespacePrefix() = namespace.prefix
    fun getNamespaceURI() = namespace.uri
    val qualifiedName get() = if (namespace.prefix.isEmpty()) name else "${namespace.prefix}:$name"

    // ---- namespaces ----------------------------------------------------------------------------------
    val additionalNamespaces: List<Namespace> get() = extraNamespaces
    fun addNamespaceDeclaration(ns: Namespace): Boolean {
        if (ns == namespace || extraNamespaces.any { it.prefix == ns.prefix }) return false
        return extraNamespaces.add(ns)
    }

    // ---- attributes ----------------------------------------------------------------------------------
    val attributes: List<Attribute> get() = attrs
    fun hasAttributes() = attrs.isNotEmpty()
    fun getAttribute(name: String, ns: Namespace = Namespace.NO_NAMESPACE) = attrs.firstOrNull { it.name == name && it.namespace == ns }
    fun getAttributeValue(name: String, ns: Namespace = Namespace.NO_NAMESPACE): String? = getAttribute(name, ns)?.value
    fun getAttributeValue(name: String, default: String): String = getAttribute(name)?.value ?: default

    fun setAttribute(a: Attribute): Element {
        a.detach()
        val i = attrs.indexOfFirst { it.name == a.name && it.namespace == a.namespace }
        if (i >= 0) { attrs[i].parent = null; attrs[i] = a } else attrs.add(a)
        a.parent = this
        return this
    }
    fun setAttribute(name: String, value: String, ns: Namespace = Namespace.NO_NAMESPACE): Element {
        val ex = getAttribute(name, ns)
        if (ex != null) ex.value = value else setAttribute(Attribute(name, value, ns))
        return this
    }
    fun removeAttribute(name: String, ns: Namespace = Namespace.NO_NAMESPACE): Boolean {
        val a = getAttribute(name, ns) ?: return false
        attrs.remove(a); a.parent = null
        return true
    }

    // ---- content -------------------------------------------------------------------------------------
    override val content: List<Content> get() = items
    val contentSize get() = items.size

    override fun addContent(c: Content): Element = addContent(items.size, c)
    fun addContent(s: String): Element = addContent(Text(s))
    fun addContent(index: Int, c: Content): Element {
        check(c.parent == null) { "The Content already has an existing parent" }
        check(!isSelfOrDescendantOf(c)) { "Cannot add an ancestor as content" }
        items.add(index, c); c.parent = this
        return this
    }
    fun addContent(cs: Collection<Content>): Element { cs.toList().forEach { it.detach(); addContent(it) }; return this }

    override fun removeContent(c: Content): Boolean {
        val r = items.remove(c)
        if (r) c.parent = null
        return r
    }
    /** Removes and returns all content. */
    fun removeContent(): List<Content> { val old = items.toList(); old.forEach { it.parent = null }; items.clear(); return old }

    /** True if [c] is this element or one of its ancestors (adding it would create a cycle). */
    private fun isSelfOrDescendantOf(c: Content): Boolean {
        var p: Parent? = this
        while (p != null) { if (p === c) return true; p = (p as? Content)?.parent }
        return false
    }

    fun setText(s: String?): Element {
        removeContent()
        if (!s.isNullOrEmpty()) addContent(Text(s))
        return this
    }

    // ---- children ------------------------------------------------------------------------------------
    /** Snapshot, not a live view (JDOM returns a live filtered list). */
    fun getChildren(): List<Element> = items.filterIsInstance<Element>()
    fun getChildren(name: String, ns: Namespace = Namespace.NO_NAMESPACE): List<Element> =
        items.filterIsInstance<Element>().filter { it.name == name && it.namespace == ns }
    fun getChild(name: String, ns: Namespace = Namespace.NO_NAMESPACE): Element? =
        items.firstOrNull { it is Element && it.name == name && it.namespace == ns } as Element?
    fun getChildText(name: String, ns: Namespace = Namespace.NO_NAMESPACE): String? = getChild(name, ns)?.getText()
    fun removeChild(name: String, ns: Namespace = Namespace.NO_NAMESPACE): Boolean = getChild(name, ns)?.let { removeContent(it) } ?: false
    fun removeChildren(name: String, ns: Namespace = Namespace.NO_NAMESPACE): Boolean =
        getChildren(name, ns).map { removeContent(it) }.any { it }

    /** Depth-first, document order, all descendant content. */
    fun getDescendants(): Sequence<Content> = sequence {
        for (c in items) { yield(c); if (c is Element) yieldAll(c.getDescendants()) }
    }

    // ---- text ----------------------------------------------------------------------------------------
    /** Concatenation of the element's own text and CDATA children (not descendants). */
    fun getText(): String = items.filterIsInstance<Text>().joinToString("") { it.text }
    fun getTextTrim(): String = getText().trim { isXmlWs(it) }
    fun getTextNormalize(): String = normalizeString(getText())
    /** Concatenated text of all descendants. */
    override fun getValue(): String = buildString { items.forEach { if (it is Text) append(it.text) else if (it is Element) append(it.getValue()) } }

    // ---- tree ----------------------------------------------------------------------------------------
    /** Deep copy, detached. */
    override fun clone(): Element {
        val e = Element(name, namespace)
        extraNamespaces.forEach { e.extraNamespaces.add(it) }
        attrs.forEach { e.setAttribute(it.clone()) }
        items.forEach { e.addContent(it.clone()) }
        return e
    }
    override fun toString() = "[Element: <$qualifiedName/>]"
}
