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

    @Test fun loneSurrogatesBecomeReplacementCharOnEveryPlatform() {
        val fffd = SearchTokenCodec.encode("\uFFFD")
        assertEquals("xefbfbd", fffd)
        assertEquals("x61efbfbd62", SearchTokenCodec.encode("a\uD800b"))
        assertEquals("x61efbfbd62", SearchTokenCodec.encode("a\uDC00b"))
        assertEquals("xefbfbd", SearchTokenCodec.encode("\uD800")) // trailing lone high
        assertEquals("xefbfbdefbfbd", SearchTokenCodec.encode("\uDC00\uD800")) // low then high: both lone
        assertEquals("xefbfbdf09d849e", SearchTokenCodec.encode("\uD800\uD834\uDD1E")) // lone high, then a valid pair
    }

    @Test fun validSurrogatePairStaysIntact() {
        assertEquals("xf09d849e", SearchTokenCodec.encode("\uD834\uDD1E"))
        assertEquals("\uD834\uDD1E", SearchTokenCodec.decode("xf09d849e"))
    }

    @Test fun decodeRejectsMalformedUtf8EvenWhenReplacementIsValid() {
        assertNull(SearchTokenCodec.decode("xc328"))
        assertEquals("\uFFFD", SearchTokenCodec.decode("xefbfbd"))
    }
}
