package org.crosswire.ksword.k5

import org.crosswire.common.xml.xmlCharacters
import org.crosswire.jsword.book.OSISUtil
import org.crosswire.jsword.book.filter.FilterOsis
import org.junit.Test
import org.junit.Assert.*

class FilterPortableSeamTest {
    @Test fun portableVocabularyMatchesUnmovedJvmFacade() {
        assertEquals("ATTRIBUTE_CELL_COLS", OSISUtil.ATTRIBUTE_CELL_COLS, FilterOsis.ATTRIBUTE_CELL_COLS)
        assertEquals("ATTRIBUTE_CELL_ROWS", OSISUtil.ATTRIBUTE_CELL_ROWS, FilterOsis.ATTRIBUTE_CELL_ROWS)
        assertEquals("ATTRIBUTE_FIGURE_SRC", OSISUtil.ATTRIBUTE_FIGURE_SRC, FilterOsis.ATTRIBUTE_FIGURE_SRC)
        assertEquals("ATTRIBUTE_Q_WHO", OSISUtil.ATTRIBUTE_Q_WHO, FilterOsis.ATTRIBUTE_Q_WHO)
        assertEquals("ATTRIBUTE_TABLE_BORDER", OSISUtil.ATTRIBUTE_TABLE_BORDER, FilterOsis.ATTRIBUTE_TABLE_BORDER)
        assertEquals("ATTRIBUTE_TABLE_ROLE", OSISUtil.ATTRIBUTE_TABLE_ROLE, FilterOsis.ATTRIBUTE_TABLE_ROLE)
        assertEquals("ATTRIBUTE_W_LEMMA", OSISUtil.ATTRIBUTE_W_LEMMA, FilterOsis.ATTRIBUTE_W_LEMMA)
        assertEquals("ATTRIBUTE_W_MORPH", OSISUtil.ATTRIBUTE_W_MORPH, FilterOsis.ATTRIBUTE_W_MORPH)
        assertEquals("HI_BOLD", OSISUtil.HI_BOLD, FilterOsis.HI_BOLD)
        assertEquals("HI_ITALIC", OSISUtil.HI_ITALIC, FilterOsis.HI_ITALIC)
        assertEquals("HI_LINETHROUGH", OSISUtil.HI_LINETHROUGH, FilterOsis.HI_LINETHROUGH)
        assertEquals("HI_SUB", OSISUtil.HI_SUB, FilterOsis.HI_SUB)
        assertEquals("HI_SUPER", OSISUtil.HI_SUPER, FilterOsis.HI_SUPER)
        assertEquals("HI_UNDERLINE", OSISUtil.HI_UNDERLINE, FilterOsis.HI_UNDERLINE)
        assertEquals("HI_X_BIG", OSISUtil.HI_X_BIG, FilterOsis.HI_X_BIG)
        assertEquals("HI_X_SMALL", OSISUtil.HI_X_SMALL, FilterOsis.HI_X_SMALL)
        assertEquals("HI_X_TT", OSISUtil.HI_X_TT, FilterOsis.HI_X_TT)
        assertEquals("LEMMA_MISC", OSISUtil.LEMMA_MISC, FilterOsis.LEMMA_MISC)
        assertEquals("LEMMA_STRONGS", OSISUtil.LEMMA_STRONGS, FilterOsis.LEMMA_STRONGS)
        assertEquals("LIST_ORDERED", OSISUtil.LIST_ORDERED, FilterOsis.LIST_ORDERED)
        assertEquals("LIST_UNORDERED", OSISUtil.LIST_UNORDERED, FilterOsis.LIST_UNORDERED)
        assertEquals("MORPH_ROBINSONS", OSISUtil.MORPH_ROBINSONS, FilterOsis.MORPH_ROBINSONS)
        assertEquals("MORPH_STRONGS", OSISUtil.MORPH_STRONGS, FilterOsis.MORPH_STRONGS)
        assertEquals("NOTETYPE_STUDY", OSISUtil.NOTETYPE_STUDY, FilterOsis.NOTETYPE_STUDY)
        assertEquals("OSIS_ATTR_CANONICAL", OSISUtil.OSIS_ATTR_CANONICAL, FilterOsis.OSIS_ATTR_CANONICAL)
        assertEquals("OSIS_ATTR_LANG", OSISUtil.OSIS_ATTR_LANG, FilterOsis.OSIS_ATTR_LANG)
        assertEquals("OSIS_ATTR_LEVEL", OSISUtil.OSIS_ATTR_LEVEL, FilterOsis.OSIS_ATTR_LEVEL)
        assertEquals("OSIS_ATTR_OSISID", OSISUtil.OSIS_ATTR_OSISID, FilterOsis.OSIS_ATTR_OSISID)
        assertEquals("OSIS_ATTR_REF", OSISUtil.OSIS_ATTR_REF, FilterOsis.OSIS_ATTR_REF)
        assertEquals("OSIS_ATTR_SUBTYPE", OSISUtil.OSIS_ATTR_SUBTYPE, FilterOsis.OSIS_ATTR_SUBTYPE)
        assertEquals("OSIS_ATTR_TYPE", OSISUtil.OSIS_ATTR_TYPE, FilterOsis.OSIS_ATTR_TYPE)
        assertEquals("OSIS_ELEMENT_W", OSISUtil.OSIS_ELEMENT_W, FilterOsis.OSIS_ELEMENT_W)
        assertEquals("Q_BLOCK", OSISUtil.Q_BLOCK, FilterOsis.Q_BLOCK)
        assertEquals("SEG_CENTER", OSISUtil.SEG_CENTER, FilterOsis.SEG_CENTER)
        assertEquals("SEG_COLORPREFIX", OSISUtil.SEG_COLORPREFIX, FilterOsis.SEG_COLORPREFIX)
        assertEquals("SEG_JUSTIFYLEFT", OSISUtil.SEG_JUSTIFYLEFT, FilterOsis.SEG_JUSTIFYLEFT)
        assertEquals("SEG_JUSTIFYRIGHT", OSISUtil.SEG_JUSTIFYRIGHT, FilterOsis.SEG_JUSTIFYRIGHT)
        assertEquals("SEG_SIZEPREFIX", OSISUtil.SEG_SIZEPREFIX, FilterOsis.SEG_SIZEPREFIX)
        assertEquals("TABLE_ROLE_LABEL", OSISUtil.TABLE_ROLE_LABEL, FilterOsis.TABLE_ROLE_LABEL)
        assertEquals("TYPE_X_PREFIX", OSISUtil.TYPE_X_PREFIX, FilterOsis.TYPE_X_PREFIX)
        assertEquals("VARIANT_CLASS", OSISUtil.VARIANT_CLASS, FilterOsis.VARIANT_CLASS)
        assertEquals("VARIANT_TYPE", OSISUtil.VARIANT_TYPE, FilterOsis.VARIANT_TYPE)
    }

    @Test fun callbackSlicesRetainJvmConstructorFailureContract() {
        val chars = charArrayOf('a', '\u0000', '\uD800', 'z')
        for ((offset, length) in listOf(0 to 4, 1 to 2, 4 to 0, -1 to 1, 0 to -1,
            3 to 2, Int.MAX_VALUE to 1, 1 to Int.MAX_VALUE)) {
            val expected = runCatching { String(chars, offset, length) }
            val actual = runCatching { xmlCharacters(chars, offset, length) }
            assertEquals(expected.getOrNull(), actual.getOrNull())
            assertEquals(expected.exceptionOrNull()?.javaClass, actual.exceptionOrNull()?.javaClass)
            assertEquals(expected.exceptionOrNull()?.message, actual.exceptionOrNull()?.message)
        }
    }
}
