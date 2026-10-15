package org.crosswire.jsword.book.filter

import fi.sykero.kdom.Element

/** The element-construction subset of the JVM facade needed by portable source filters. */
internal object FilterOsis {
    const val ATTRIBUTE_CELL_COLS = "cols"
    const val ATTRIBUTE_CELL_ROWS = "rows"
    const val ATTRIBUTE_FIGURE_SRC = "src"
    const val ATTRIBUTE_Q_WHO = "who"
    const val ATTRIBUTE_TABLE_BORDER = "border"
    const val ATTRIBUTE_TABLE_ROLE = "role"
    const val ATTRIBUTE_W_LEMMA = "lemma"
    const val ATTRIBUTE_W_MORPH = "morph"
    const val HI_BOLD = "bold"
    const val HI_ITALIC = "italic"
    const val HI_LINETHROUGH = "line-through"
    const val HI_SUB = "sub"
    const val HI_SUPER = "super"
    const val HI_UNDERLINE = "underline"
    const val HI_X_BIG = "x-big"
    const val HI_X_SMALL = "x-small"
    const val HI_X_TT = "x-tt"
    const val LEMMA_MISC = "lemma:"
    const val LEMMA_STRONGS = "strong:"
    const val LIST_ORDERED = "x-ordered"
    const val LIST_UNORDERED = "x-unordered"
    const val MORPH_ROBINSONS = "robinson:"
    const val MORPH_STRONGS = "x-StrongsMorph:T"
    const val NOTETYPE_STUDY = "x-StudyNote"
    const val OSIS_ATTR_CANONICAL = "canonical"
    const val OSIS_ATTR_LANG = "lang"
    const val OSIS_ATTR_LEVEL = "level"
    const val OSIS_ATTR_OSISID = "osisID"
    const val OSIS_ATTR_REF = "osisRef"
    const val OSIS_ATTR_SUBTYPE = "subType"
    const val OSIS_ATTR_TYPE = "type"
    const val OSIS_ELEMENT_W = "w"
    const val Q_BLOCK = "blockquote"
    const val SEG_CENTER = "text-align: center;"
    const val SEG_COLORPREFIX = "color: "
    const val SEG_JUSTIFYLEFT = "text-align: left;"
    const val SEG_JUSTIFYRIGHT = "text-align: right;"
    const val SEG_SIZEPREFIX = "font-size: "
    const val TABLE_ROLE_LABEL = "label"
    const val TYPE_X_PREFIX = "x-"
    const val VARIANT_CLASS = "x-"
    const val VARIANT_TYPE = "x-variant"
    fun factory(): FilterOsisFactory = filterOsisFactory()
}

internal expect fun filterOsisFactory(): FilterOsisFactory

internal expect open class FilterOsisFactory {
    open fun createAbbr(): Element
    open fun createCell(): Element
    open fun createDiv(): Element
    open fun createFigure(): Element
    open fun createForeign(): Element
    open fun createHI(): Element
    open fun createItem(): Element
    open fun createL(): Element
    open fun createLB(): Element
    open fun createLG(): Element
    open fun createList(): Element
    open fun createName(): Element
    open fun createNote(): Element
    open fun createP(): Element
    open fun createQ(): Element
    open fun createReference(): Element
    open fun createRow(): Element
    open fun createSeg(): Element
    open fun createTable(): Element
    open fun createTitle(): Element
    open fun createW(): Element
}
