package org.crosswire.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KUriPartsTest {
    @Test fun fileUriWithSpaceAndUmlaut() {
        val p = KUriParts.parse("file:/sdcard/My%20Bibles/%C3%A4/mods.d")!!
        assertEquals("file", p.scheme)
        assertNull(p.host)
        assertEquals("/sdcard/My%20Bibles/%C3%A4/mods.d", p.rawPath)
        assertEquals("/sdcard/My Bibles/ä/mods.d", KUriParts.decode(p.rawPath!!))
    }

    @Test fun httpsWithPortQueryFragment() {
        val p = KUriParts.parse("https://user@crosswire.org:8443/ftpmirror/pub?x=1#frag")!!
        assertEquals("https", p.scheme)
        assertEquals("user", p.rawUserInfo)
        assertEquals("crosswire.org", p.host)
        assertEquals(8443, p.port)
        assertEquals("/ftpmirror/pub", p.rawPath)
        assertEquals("x=1", p.rawQuery)
        assertEquals("frag", p.rawFragment)
    }

    @Test fun noPortIsMinusOne() = assertEquals(-1, KUriParts.parse("https://a.org/x")!!.port)

    @Test fun quotePathEncodesSpaceButNotNonAscii() =
        // java.net.URI's multi-argument constructors quote illegal ASCII but keep "other" (non-ASCII) characters
        assertEquals("/My%20Bibles/ä", KUriParts.quotePath("/My Bibles/ä"))

    @Test fun relativeHasNoScheme() = assertNull(KUriParts.parse("mods.d/kjv.conf")!!.scheme)

    @Test fun illegalCharacterIsAnError() = assertNull(KUriParts.parse("file:/a b"))

    @Test fun opaqueSchemeSpecificPartHasNoHierarchicalComponents() {
        val p = KUriParts.parse("jar:file:/a.jar!/x?version=1#entry")!!
        assertEquals("jar", p.scheme)
        assertEquals("file:/a.jar!/x?version=1", p.rawSsp)
        assertNull(p.rawAuthority)
        assertNull(p.rawPath)
        assertNull(p.rawQuery)
        assertEquals("entry", p.rawFragment)
    }

    @Test fun malformedEscapesAreRejected() {
        for (s in listOf("file:/%", "file:/%0", "file:/%gg")) {
            assertNull(KUriParts.parse(s), s)
        }
    }

    @Test fun decodeKeepsLiteralPlusAndReplacesInvalidUtf8() {
        assertEquals("/a+b+c", KUriParts.decode("/a%2Bb+c"))
        assertEquals("/�(", KUriParts.decode("/%C3%28"))
    }
}
