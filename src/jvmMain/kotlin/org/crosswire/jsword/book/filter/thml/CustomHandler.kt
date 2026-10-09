/**
 * Distribution License:
 * JSword is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License, version 2.1 or later
 * as published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The License is available on the internet at:
 *      http://www.gnu.org/copyleft/lgpl.html
 * or by writing to:
 *      Free Software Foundation, Inc.
 *      59 Temple Place - Suite 330
 *      Boston, MA 02111-1307, USA
 *
 * © CrossWire Bible Society, 2005 - 2016
 *
 */
package org.crosswire.jsword.book.filter.thml

import java.util.HashMap
import java.util.LinkedList
import java.util.Locale

import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.DataPolice
import org.crosswire.jsword.passage.Key
import org.jdom2.Content
import org.jdom2.Element
import org.jdom2.Text
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

/**
 * To convert SAX events into OSIS events.
 * 
 * <p>
 * This is based upon the THML reference page:
 * <a href="http://www.ccel.org/ThML/ThML1.04.htm">http://www.ccel.org/ThML/ThML1.04.htm</a>
 * to work out what the tags meant.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class CustomHandler
/**
 * Simple ctor
 * 
 * @param book the book
 * @param key the key
 */
(
    /**
     * The book being parsed.
     */
    private var book: Book?,
    /**
     * The book being parsed.
     */
    private var key: Key?
) : DefaultHandler() {
    /**
     * When the document is parsed, this is the last element popped off the
     * stack.
     */
    private var rootElementValue: Element? = null

    /**
     * The stack of elements that we have created
     */
    private var stack: LinkedList<Content?> = LinkedList()

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Throws(SAXException::class)
    override fun startElement(uri: String?, localname: String?, qname: String?, attrs: Attributes?) {
        var ele: Element? = null

        // If we are looking at the root element
        // then the stack is empty
        if (!stack.isEmpty()) {
            val top: Any? = stack.peekFirst()

            // If the element and its descendants are to be ignored
            // then there is a null element on the stack
            if (top == null) {
                return
            }

            // It might be a text element
            if (top is Element) {
                ele = top
            }
        }

        val t = getTag(localname, qname)

        if (t != null) {
            stack.addFirst(t.processTag(book, key, ele, attrs))
        }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    override fun characters(data: CharArray, offset: Int, length: Int) {
        // what we are adding
        val text = String(data, offset, length)

        if (stack.isEmpty()) {
            stack.addFirst(Text(text))
            return
        }

        // What we are adding to
        val top: Content? = stack.peekFirst()

        // If the element and its descendants are to be ignored
        // then there is a null element on the stack
        if (top == null) {
            return
        }

        if (top is Text) {
            top.append(text)
            return
        }

        if (top is Element) {
            val current: Element = top

            val size = current.contentSize

            // If the last element in the list is a string then we should add
            // this string on to the end of it rather than add a new list item
            // because (probably as an artifact of the HTML/XSL transform we get
            // a space inserted in the output even when 2 calls to this method
            // split a word.
            if (size > 0) {
                val last: Content = current.getContent(size - 1)
                if (last is Text) {
                    last.append(text)
                    return
                }
            }
            current.addContent(Text(text))
        }
    }

    /* (non-Javadoc)
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    override fun endElement(uri: String?, localname: String?, qname: String?) {
        if (stack.isEmpty()) {
            return
        }
        // When we are done processing an element we need to remove
        // it from the stack so that nothing more is attached to it.
        val top: Content? = stack.removeFirst()
        if (top is Element) {
            val finished: Element = top
            val t = getTag(localname, qname)

            if (t != null) {
                t.processContent(book, key, finished)
            }

            // If it was the last element then it was the root element
            // so save it
            if (stack.isEmpty()) {
                rootElementValue = finished
            }
        }
    }

    /**
     * @return the root element
     */
    open val rootElement: Element?
        get() = rootElementValue

    private fun getTag(localname: String?, qname: String?): Tag? {
        // sometimes qname is empty e.g. on Android 2.1
        val name: String? = if (qname != null && qname.length > 0) {
            qname
        } else {
            localname
        }

        var t = TAG_MAP[name]

        // Some of the THML books are broken in that they use uppercase
        // element names, which the spec disallows, but we might as well
        // look out for them
        if (t == null) {
            t = TAG_MAP[name!!.lowercase(Locale.ENGLISH)]

            if (t == null) {
                DataPolice.report(book, key, "Unknown thml element: " + localname + " qname=" + name)

                // Report on it only once and make sure the content is output.
                t = AnonymousTag(name)
                TAG_MAP[name] = t
                return t
            }

            // DataPolice.report(book, key, "Wrong case used in thml element: " + name);
        }
        return t
    }

    private companion object {
        /**
         * The known tag types
         */
        private val TAG_MAP: MutableMap<String?, Tag> = HashMap()

        init {
            /*
             * ThML is based upon Voyager XHTML and all Voyager elements are
             * allowed. However not all elements make sense.
             */
            val tags = arrayOf<Tag>(
                // The following are defined in Voyager xhtml 4.0
                ATag(),
                AbbrTag(),
                AliasTag("acronym", AbbrTag()),
                AnonymousTag("address"),
                SkipTag("applet"),
                SkipTag("area"),
                BTag(),
                SkipTag("base"),
                SkipTag("basefont"),
                IgnoreTag("bdo"),
                BigTag(),
                BlockquoteTag(),
                IgnoreTag("body"),
                BrTag(),
                SkipTag("button"),
                AnonymousTag("caption"),
                CenterTag(),
                AnonymousTag("cite"),
                AnonymousTag("code"),
                SkipTag("col"),
                SkipTag("colgroup"),
                AliasTag("dd", LiTag()),
                AnonymousTag("del"),
                AnonymousTag("dfn"),
                DivTag(),
                AliasTag("dl", UlTag()),
                AliasTag("dt", LiTag()),
                AliasTag("em", ITag()),
                IgnoreTag("fieldset"),
                FontTag(),
                SkipTag("form"),
                SkipTag("frame"),
                SkipTag("frameset"),
                AliasTag("h1", HTag(1)),
                AliasTag("h2", HTag(2)),
                AliasTag("h3", HTag(3)),
                AliasTag("h4", HTag(4)),
                AliasTag("h5", HTag(5)),
                AliasTag("h6", HTag(6)),
                SkipTag("head"),
                HrTag(),
                IgnoreTag("html"),
                IgnoreTag("frameset"),
                ITag(),
                SkipTag("iframe"),
                ImgTag(),
                SkipTag("input"),
                AnonymousTag("ins"),
                AnonymousTag("kbd"),
                AnonymousTag("label"),
                AnonymousTag("legend"),
                LiTag(),
                SkipTag("link"),
                SkipTag("map"),
                SkipTag("meta"),
                SkipTag("noscript"),
                SkipTag("object"),
                OlTag(),
                SkipTag("optgroup"),
                SkipTag("option"),
                PTag(),
                SkipTag("param"),
                IgnoreTag("pre"),
                QTag(),
                RootTag(),
                STag(),
                AnonymousTag("samp"),
                SkipTag("script"),
                SkipTag("select"),
                SmallTag(),
                IgnoreTag("span"),
                AliasTag("strong", BTag()),
                SkipTag("style"),
                SubTag(),
                SupTag(),
                SyncTag(),
                TableTag(),
                IgnoreTag("tbody"),
                TdTag(),
                IgnoreTag("tfoot"),
                SkipTag("textarea"),
                SkipTag("title"),
                IgnoreTag("thead"),
                ThTag(),
                TrTag(),
                TtTag(),
                UTag(),
                UlTag(),
                AnonymousTag("var"),

                // ThML adds the following to Voyager
                // Note: hymn.mod is not here nor are additional head&DC
                // elements
                AnonymousTag("added"),
                AnonymousTag("attr"),
                AnonymousTag("argument"),
                CitationTag(),
                AnonymousTag("date"),
                AnonymousTag("deleted"),
                AnonymousTag("def"),
                AliasTag("div1", DivTag(1)),
                AliasTag("div2", DivTag(2)),
                AliasTag("div3", DivTag(3)),
                AliasTag("div4", DivTag(4)),
                AliasTag("div5", DivTag(5)),
                AliasTag("div6", DivTag(6)),
                ForeignTag(),
                AnonymousTag("index"),
                AnonymousTag("insertIndex"),
                AnonymousTag("glossary"),
                NoteTag(),
                NameTag(),
                PbTag(),
                AnonymousTag("scripCom"),
                AnonymousTag("scripContext"),
                ScripRefTag(),
                ScriptureTag(),
                TermTag(),
                AnonymousTag("unclear"),
                VerseTag(),
            )
            for (i in tags.indices) {
                val t = tags[i]
                val tagName = t.tagName
                TAG_MAP[tagName] = t
            }
        }
    }
}
