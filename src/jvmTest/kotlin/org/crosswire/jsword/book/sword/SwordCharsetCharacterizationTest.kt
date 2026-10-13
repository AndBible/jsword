package org.crosswire.jsword.book.sword

import org.junit.Assert.*
import org.junit.Test

class SwordCharsetCharacterizationTest {
    @Test fun decodingPreservesFallbackMutationAndBounds() {
        val utf8 = byteArrayOf(0x78, 0xc3.toByte(), 0xa4.toByte(), 0x79)
        assertEquals("ä", SwordUtil.decode("key", utf8, 1, 2, "UTF-8"))
        assertEquals("ä", SwordUtil.decode("key", utf8, 1, 2, "not-a-charset"))
        assertEquals("", SwordUtil.decode("key", utf8, 0, 9, null))
        assertThrows(NullPointerException::class.java) { SwordUtil.decode("key", utf8, null) }
        assertThrows(IndexOutOfBoundsException::class.java) { SwordUtil.decode("key", utf8, -1, 2, "UTF-8") }
        val dirty = byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x01, 0x09)
        assertEquals(" € \t", SwordUtil.decode("key", dirty, "WINDOWS-1252"))
        assertArrayEquals(byteArrayOf(0x20, 0x80.toByte(), 0x20, 0x09), dirty)
        assertEquals("�", SwordUtil.decode("key", byteArrayOf(0xc3.toByte()), "UTF-8"))
    }

    @Test fun dataEntryUsesSameDecodingForKeysLinksAndClonedPayload() {
        val bytes = "ä\\\r\n@LINK ä\n".toByteArray(Charsets.UTF_8)
        val entry = DataEntry("entry", bytes, "not-a-charset")
        bytes.fill(0)
        assertEquals("ä", entry.getKey())
        assertTrue(entry.isLinkEntry())
        assertEquals("ä", entry.getLinkTarget())
        assertEquals("ä\\\r\n@LINK ä", entry.getRawText(null))
    }
}
