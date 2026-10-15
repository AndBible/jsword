package org.crosswire.jsword.book.filter

import fi.sykero.kdom.Element

internal actual fun filterOsisFactory(): FilterOsisFactory = factory
private val factory = FilterOsisFactory()

internal actual open class FilterOsisFactory {
    actual open fun createAbbr(): Element = Element("abbr")
    actual open fun createCell(): Element = Element("cell")
    actual open fun createDiv(): Element = Element("div")
    actual open fun createFigure(): Element = Element("figure")
    actual open fun createForeign(): Element = Element("foreign")
    actual open fun createHI(): Element = Element("hi")
    actual open fun createItem(): Element = Element("item")
    actual open fun createL(): Element = Element("l")
    actual open fun createLB(): Element = Element("lb")
    actual open fun createLG(): Element = Element("lg")
    actual open fun createList(): Element = Element("list")
    actual open fun createName(): Element = Element("name")
    actual open fun createNote(): Element = Element("note")
    actual open fun createP(): Element = Element("p")
    actual open fun createQ(): Element = Element("q")
    actual open fun createReference(): Element = Element("reference")
    actual open fun createRow(): Element = Element("row")
    actual open fun createSeg(): Element = Element("seg")
    actual open fun createTable(): Element = Element("table")
    actual open fun createTitle(): Element = Element("title")
    actual open fun createW(): Element = Element("w")
}
