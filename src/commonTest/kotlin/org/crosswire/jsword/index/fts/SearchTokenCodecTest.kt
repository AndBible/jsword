package org.crosswire.jsword.index.fts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchTokenCodecTest {
    @Test fun encodesAsciiAsLowercaseHex() = assertEquals("x6c6f7665", SearchTokenCodec.encode("love"))

    @Test fun roundTrips() {
        for (t in listOf("love", "αγάπη", "אהבה", "愛", "𝄞😀", "", "ä", "a b")) {
            assertEquals(t, SearchTokenCodec.decode(SearchTokenCodec.encode(t)), t)
        }
    }

    @Test fun emptyTermIsJustX() {
        assertEquals("x", SearchTokenCodec.encode(""))
        assertEquals("", SearchTokenCodec.decode("x"))
    }

    @Test fun rejectsMalformed() {
        assertNull(SearchTokenCodec.decode("x6"))
        assertNull(SearchTokenCodec.decode("q00"))
        assertNull(SearchTokenCodec.decode(""))
        assertNull(SearchTokenCodec.decode("g"))
        assertNull(SearchTokenCodec.decode("x6C"))
        assertNull(SearchTokenCodec.decode("xzz"))
        assertNull(SearchTokenCodec.decode("xff"))
    }
}
