package spike.xml.kdom

/*
 * Spike port of a subset of JDOM 2.0.6.1 (https://www.jdom.org/), Copyright (C) 2000-2012 Jason Hunter & Brett McLaughlin.
 * JDOM is distributed under a BSD-style licence with an attribution clause: source redistributions must retain the
 * copyright notice, the conditions and the disclaimer; binary redistributions must reproduce them in the documentation
 * or other materials; the name "JDOM" may not be used to endorse or name derived products (a port must not be called
 * JDOM); and an acknowledgement ("This product includes software developed by the JDOM Project (http://www.jdom.org/)")
 * is requested but not required. A production port would carry the full JDOM LICENSE text in this package.
 */

/** Characters JDOM's Verifier treats as XML whitespace. */
internal fun isXmlWs(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r'

internal fun isAllXmlWs(s: String) = s.all(::isXmlWs)

/** Trims and collapses internal XML whitespace runs to one space (JDOM Text.normalizeString). */
internal fun normalizeString(s: String): String {
    val sb = StringBuilder()
    var pendingSpace = false
    for (c in s) {
        if (isXmlWs(c)) pendingSpace = sb.isNotEmpty() else {
            if (pendingSpace) { sb.append(' '); pendingSpace = false }
            sb.append(c)
        }
    }
    return sb.toString()
}

/** Anything that has content: [Element] and [Document]. */
interface Parent {
    val content: List<Content>
    fun addContent(c: Content): Parent
    fun removeContent(c: Content): Boolean
    fun indexOf(c: Content): Int = content.indexOf(c)
    fun getParentElementOrNull(): Element? = this as? Element
}

/** Base of everything that can sit in a [Parent]'s content list. */
abstract class Content {
    var parent: Parent? = null
        internal set

    fun getParentElement(): Element? = parent as? Element
    fun getDocument(): Document? {
        var p = parent
        while (p is Element) p = p.parent
        return p as? Document
    }

    /** Removes this node from its parent; returns this. */
    fun detach(): Content { parent?.removeContent(this); return this }

    abstract fun getValue(): String
    abstract fun clone(): Content
}

/** XML namespace; like JDOM, equality is by URI only. */
class Namespace private constructor(val prefix: String, val uri: String) {
    override fun equals(other: Any?) = other is Namespace && other.uri == uri
    override fun hashCode() = uri.hashCode()
    override fun toString() = "[Namespace: prefix \"$prefix\" is mapped to URI \"$uri\"]"

    companion object {
        val NO_NAMESPACE = Namespace("", "")
        val XML_NAMESPACE = Namespace("xml", "http://www.w3.org/XML/1998/namespace")

        fun getNamespace(prefix: String, uri: String): Namespace {
            if (uri.isEmpty() && prefix.isEmpty()) return NO_NAMESPACE
            if (uri == XML_NAMESPACE.uri) return XML_NAMESPACE
            return Namespace(prefix, uri)
        }
        fun getNamespace(uri: String) = getNamespace("", uri)
    }
}

class Attribute(val name: String, var value: String, val namespace: Namespace = Namespace.NO_NAMESPACE) {
    var parent: Element? = null
        internal set
    val qualifiedName get() = if (namespace.prefix.isEmpty()) name else "${namespace.prefix}:$name"
    fun clone() = Attribute(name, value, namespace)
    fun detach(): Attribute { parent?.removeAttribute(name, namespace); return this }
    override fun toString() = "[Attribute: $qualifiedName=\"$value\"]"
}

open class Text(text: String) : Content() {
    var text: String = text
    open fun append(s: String): Text { text += s; return this }
    fun getTextTrim() = text.trim { isXmlWs(it) }
    fun getTextNormalize() = normalizeString(text)
    override fun getValue() = text
    override fun clone(): Content = Text(text)
    override fun toString() = "[Text: $text]"
}

class CDATA(text: String) : Text(text) {
    override fun clone(): Content = CDATA(text)
    override fun toString() = "[CDATA: $text]"
}

class Document() : Parent {
    private val items = ArrayList<Content>()
    override val content: List<Content> get() = items
    constructor(root: Element) : this() { setRootElement(root) }

    val rootElement: Element get() = items.filterIsInstance<Element>().firstOrNull() ?: error("Root element not set")
    fun hasRootElement() = items.any { it is Element }

    fun setRootElement(e: Element): Document {
        e.detach()
        val i = items.indexOfFirst { it is Element }
        if (i >= 0) { items[i].parent = null; items[i] = e } else items.add(e)
        e.parent = this
        return this
    }

    override fun addContent(c: Content): Parent {
        check(c.parent == null) { "Content already has a parent" }
        check(c !is Element || !hasRootElement()) { "Document already has a root element" }
        check(c !is Text || isAllXmlWs(c.text)) { "A document cannot contain non-whitespace text" }
        items.add(c); c.parent = this
        return this
    }

    override fun removeContent(c: Content): Boolean {
        val r = items.remove(c)
        if (r) c.parent = null
        return r
    }

    fun clone(): Document = Document().also { d -> items.forEach { d.addContent(it.clone()) } }
}
