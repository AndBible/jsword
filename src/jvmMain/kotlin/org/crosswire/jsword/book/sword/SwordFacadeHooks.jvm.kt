package org.crosswire.jsword.book.sword

import fi.sykero.kdom.Document
import org.crosswire.common.xml.XMLUtil
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory
import org.crosswire.jsword.book.Books
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.SourceFilter
import org.crosswire.jsword.book.filter.SourceFilterFactory
import fi.sykero.kdom.Element
import java.util.Arrays

/** K4 facade boundaries, JVM-only until their parameter types move in Task 16. */
internal actual fun swordMetaDataToOsis(md: SwordBookMetaData): Document {
    val knownKeys: MutableList<String> = ArrayList(md.osisConfig.keys)
    val factory = OSISUtil.factory()
    val table = factory.createTable()
    var row: Element? = toRow(factory, "Initials", md.initials)
    table.addContent(row!!)
    // Each key gets one row.
    for (key in SwordBookMetaData.OSIS_INFO) {
        knownKeys.remove(key)
        row = toRow(md, factory, key)
        if (row != null) {
            table.addContent(row)
        }
    }
    // Output the rest in the order that they are in the conf
    // however, don't show those that should be hidden.
    val hide = Arrays.asList(*SwordBookMetaData.HIDDEN)
    for (key in knownKeys) {
        if (hide.contains(key)) {
            continue
        }
        row = toRow(md, factory, key)
        if (row != null) {
            table.addContent(row)
        }
    }
    return Document(table)
}

internal actual fun sourceFilterFor(sourceType: String?): SourceFilter = SourceFilterFactory.getFilter(sourceType)

internal actual fun createSwordBook(type: BookType, sbmd: SwordBookMetaData, backend: Backend<*>): Book =
    when (type.toString()) {
        "RawLD", "RawLD4", "zLD" -> if (sbmd.bookCategory == BookCategory.DAILY_DEVOTIONS) {
            SwordDailyDevotion(sbmd, backend)
        } else {
            SwordDictionary(sbmd, backend)
        }
        "RawGenBook" -> SwordGenBook(sbmd, backend)
        "RawText", "zText", "zText4", "RawCom", "RawCom4", "zCom", "zCom4", "HREFCom", "RawFiles" -> SwordBook(sbmd, backend)
        else -> throw UnsupportedOperationException("Unknown book type: $type")
    }

internal actual fun registerSwordDriver() = Books.installed().registerDriver(SwordBookDriver.instance())

private fun toRow(factory: OSISUtil.OSISFactory, key: String, value: String): Element {
    val nameEle = toKeyCell(factory, key)

    val valueElement = factory.createCell()
    valueElement.addContent(value)

    // Each key gets one row.
    val rowEle = factory.createRow()
    rowEle.addContent(nameEle)
    rowEle.addContent(valueElement)
    return rowEle
}

private fun toRow(md: SwordBookMetaData, factory: OSISUtil.OSISFactory, key: String): Element? {
    val size = md.osisConfig.size(key)
    if (size == 0) {
        return null
    }

    // See if it is a predefined type
    val type = ConfigEntryType.fromString(key)
    val nameEle = toKeyCell(factory, key)

    val valueElement = factory.createCell()
    for (j in 0 until size) {
        if (j > 0) {
            valueElement.addContent(factory.createLB())
        }

        var text = md.osisConfig.get(key, j)
        if (type != null && !type.isText() && type.isAllowed(text)) {
            text = type.convert(text)!!.toString()
        }
        text = XMLUtil.escape(text)
        if (type != null && type.allowsRTF()) {
            valueElement.addContent(OSISUtil.rtfToOsis(text!!))
        } else {
            valueElement.addContent(text)
        }
    }

    // Each key gets one row.
    val rowEle = factory.createRow()
    rowEle.addContent(nameEle)
    rowEle.addContent(valueElement)

    return rowEle
}

private fun toKeyCell(factory: OSISUtil.OSISFactory, key: String): Element {
    val nameEle = factory.createCell()
    val hiEle = factory.createHI()
    hiEle.setAttribute(OSISUtil.OSIS_ATTR_TYPE, OSISUtil.HI_BOLD)
    nameEle.addContent(hiEle)
    // I18N(DMS): use name to lookup translation.
    hiEle.addContent(key)
    return nameEle
}

