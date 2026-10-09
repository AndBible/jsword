package spike.xml.kdom

/** The element, then all its descendant elements, in document order (XPath `descendant-or-self::*`). */
fun Element.descendantsOrSelf(): Sequence<Element> = sequenceOf(this) + descendantElements()

fun Element.descendantElements(): Sequence<Element> = getDescendants().filterIsInstance<Element>()

/** `//name` relative to this element incl. itself (XPath `descendant-or-self::ns:name`); [name] null = any (`*`). */
fun Element.descendantsOrSelf(name: String?, ns: Namespace? = null): Sequence<Element> =
    descendantsOrSelf().filter { it.matches(name, ns) }

/** `.//name` (descendants only). */
fun Element.descendants(name: String?, ns: Namespace? = null): Sequence<Element> = descendantElements().filter { it.matches(name, ns) }

/** `ns:name` children; null name or ns = wildcard. */
fun Element.children(name: String? = null, ns: Namespace? = null): List<Element> = getChildren().filter { it.matches(name, ns) }

fun Element.matches(name: String?, ns: Namespace?) = (name == null || this.name == name) && (ns == null || namespace == ns)

/** `[@name='value']` (value null: `[@name]`). */
fun Element.attrIs(name: String, value: String? = null, ns: Namespace = Namespace.NO_NAMESPACE): Boolean =
    getAttributeValue(name, ns).let { it != null && (value == null || it == value) }

/** `..`: for the document root there is none. */
fun Element.parentElement(): Element? = getParentElement()

fun Element.followingSiblings(): List<Element> {
    val sibs = getParentElement()?.getChildren() ?: return emptyList()
    return sibs.drop(sibs.indexOfFirst { it === this } + 1)
}

fun Element.precedingSiblings(): List<Element> {
    val sibs = getParentElement()?.getChildren() ?: return emptyList()
    return sibs.take(sibs.indexOfFirst { it === this })
}

fun Element.ancestors(): Sequence<Element> = generateSequence(getParentElement()) { it.getParentElement() }

/**
 * `.//text()[not(ancestor::<excludeAncestor>)]`: descendant Text/CDATA nodes with no ancestor element named
 * [excludeAncestor] in no namespace (same literal-name semantics as an unprefixed XPath name test).
 */
fun Element.textNodes(excludeAncestor: String? = null): List<Text> =
    getDescendants().filterIsInstance<Text>().filter { t ->
        excludeAncestor == null || (t.getParentElement()?.let { p -> (sequenceOf(p) + p.ancestors()).none { it.matches(excludeAncestor, Namespace.NO_NAMESPACE) } } ?: true)
    }.toList()
