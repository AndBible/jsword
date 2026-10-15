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
package org.crosswire.jsword.book.basic

import org.crosswire.common.util.Language
import org.crosswire.common.util.StringUtil
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.BookDriver
import org.crosswire.jsword.book.BookMetaData
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.passage.VerseKey
import fi.sykero.kdom.Document
import fi.sykero.kdom.Element

/**
 * DefaultBookMetaData is an implementation of the of the BookMetaData
 * interface. A less complete implementation design for inheritance is available
 * in AbstractBookMetaData where the complexity is in the setup rather than the
 * inheritance. DefaultBookMetaData is probably the preferred implementation.
 * 
 * @see gnu.lgpl.License The GNU Lesser General Public License for details.
 * @author Joe Walker
 */
open class DefaultBookMetaData
/**
 * Ctor with some default values. A call to setBook() is still required
 * after this ctor is called
 * @param driver the driver for this book
 * @param name the name of this book
 * @param type the type of this book
 */
(driver: BookDriver?, name: String?, type: BookCategory?) : AbstractBookMetaData() {
    private var type: BookCategory? = null
    private var nameValue: String? = null
    private var initialsValue: String? = null
    private val props: MutableMap<String, String?> = HashMap()

    init {
        setDriver(driver)
        setName(name)
        setBookCategory(type)
        language = Language.DEFAULT_LANG // Default language
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getBookCharset()
     */
    override val bookCharset: String?
        get() = DEFAULT_CHARSET

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getPropertyKeys()
     */
    override val propertyKeys: Set<String>?
        get() = UnmodifiableKeyView(props.keys)

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getProperty(java.lang.String)
     */
    override fun getProperty(key: String?): String? {
        if (BookMetaData.KEY_LANGUAGE == key) {
            return language.name
        }
        return props[key]
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getValues(java.lang.String)
     */
    override fun getValues(key: String?): Collection<String>? {
        val value = props[key]
        if (value != null) {
            val values: MutableCollection<String> = ArrayList()
            values.add(value)
            return values
        } else {
            return null
        }
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#setProperty(java.lang.String, java.lang.String)
     */
    override fun setProperty(key: String?, value: String?) {
        props[key!!] = value
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#putProperty(java.lang.String, java.lang.String)
     */
    override fun putProperty(key: String?, value: String?) {
        setProperty(key, value)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#putProperty(java.lang.String, java.lang.String, boolean)
     */
    override fun putProperty(key: String?, value: String?, forFrontend: Boolean) {
        setProperty(key, value)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getBookCategory()
     */
    override val bookCategory: BookCategory
        get() = type!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getName()
     */
    override val name: String
        get() = nameValue!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getAbbreviation()
     */
    override val abbreviation: String
        get() = initialsValue!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#getInitials()
     */
    override val initials: String
        get() = initialsValue!!

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.BookMetaData#isLeftToRight()
     */
    override val isLeftToRight: Boolean
        get() = language.isLeftToRight

    /**
     * See note on setName() for side effect on setInitials(). If a value of
     * null is used then the initials are defaulted using the name
     * 
     * @see DefaultBookMetaData.setName
     * @param initials
     * The initials to set.
     */
    open fun setInitials(initials: String?) {
        if (initials == null) {
            if (nameValue == null) {
                this.initialsValue = ""
            } else {
                this.initialsValue = StringUtil.getInitials(nameValue)
            }
        } else {
            this.initialsValue = initials
        }
    }

    /**
     * Setting the name also sets some default initials, so if you wish to set
     * some specific initials then it should be done after setting the name.
     * 
     * @see DefaultBookMetaData.setInitials
     * @param name
     * The name to set.
     */
    open fun setName(name: String?) {
        this.nameValue = name

        putProperty(BookMetaData.KEY_NAME, this.nameValue)

        setInitials(StringUtil.getInitials(name))
    }

    /**
     * @param aType
     * The type to set.
     */
    open fun setBookCategory(aType: BookCategory?) {
        var t = aType
        if (t == null) {
            t = BookCategory.BIBLE
        }
        type = t

        putProperty(BookMetaData.KEY_CATEGORY, type.toString())
    }

    /**
     * @param typestr
     * The string version of the type to set.
     */
    open fun setType(typestr: String?) {
        var newType: BookCategory? = null
        if (typestr != null) {
            newType = BookCategory.fromString(typestr)
        }

        setBookCategory(newType)
    }

    /* (non-Javadoc)
     * @see org.crosswire.jsword.book.basic.AbstractBookMetaData#toOSIS()
     */
    override fun toOSIS(): Document {
        val factory = OSISUtil.factory()
        val ele = factory.createTable()
        addRow(factory, ele, "Initials", initials)
        addRow(factory, ele, "Description", name)
        addRow(factory, ele, "Key", bookCategory.toString())
        addRow(factory, ele, "Language", language.name)
        return Document(ele)
    }

    /**
     * @return The scope
     */
    open val scope: VerseKey<*>
        get() {
            // The following method is only available for Sword books
            throw UnsupportedOperationException()
        }

    private fun addRow(factory: OSISUtil.OSISFactory, table: Element, key: String, value: String?) {
        if (value == null) {
            return
        }

        val rowEle = factory.createRow()

        val nameEle = factory.createCell()
        val hiEle = factory.createHI()
        hiEle.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_BOLD)
        nameEle.addContent(hiEle)
        val valueElement = factory.createCell()
        rowEle.addContent(nameEle)
        rowEle.addContent(valueElement)

        // I18N(DMS): use name to lookup translation.
        hiEle.addContent(key)

        val expandedValue = XMLUtil.escape(value)
        valueElement.addContent(expandedValue)

        table.addContent(rowEle)
    }

    /**
     * Live read-only view of the property keys, the replacement of `Collections.unmodifiableSet` (rule 26): it
     * reflects later changes of the map, and neither the set nor its iterator can remove.
     */
    private class UnmodifiableKeyView(private val keys: Set<String>) : AbstractSet<String>() {
        override val size: Int get() = keys.size

        override fun contains(element: String): Boolean = keys.contains(element)

        override fun iterator(): Iterator<String> {
            val source = keys.iterator()
            return object : Iterator<String> {
                override fun hasNext(): Boolean = source.hasNext()
                override fun next(): String = source.next()
            }
        }
    }

    private companion object {
        private const val DEFAULT_CHARSET = "UTF-8"
    }
}
